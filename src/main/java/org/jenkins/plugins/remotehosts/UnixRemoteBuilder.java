/*
 * The MIT License
 *
 * Copyright (C) 2010-2011 by Anthony Robinson (publish-over-ssh-plugin)
 *               2012-2013 by Edmund Wagner    (ssh-plugin)
 *               2014-2015 by Lab              (parameterized-publish-over-ssh-plugin)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkins.plugins.remotehosts;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.jenkins.plugins.remotehosts.exceptions.SSHBuilderException;
import org.jenkins.plugins.remotehosts.protocoles.SCPChannel;
import org.jenkins.plugins.remotehosts.protocoles.SSHChannel;
import org.jenkins.plugins.remotehosts.utils.JenkinsUtils;
import org.jenkins.plugins.remotehosts.utils.ParameterizedUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.jcraft.jsch.JSchException;

public class UnixRemoteBuilder extends Builder {

	public transient static final Logger LOGGER = Logger.getLogger(UnixRemoteBuilder.class.getName());
	
	private String scpCommand;
	private String command;

	private String hostname;
	private String port;
	private String username;
	private String password;
	private String keyfile;
	private String serverAliveInterval = "0";
	private Boolean pty = Boolean.FALSE;

	private transient SSHChannel sshSite;
	
	private transient SCPChannel scpSite;
	
	
	/**
	 * 
	 * @param command
	 * @param scpCommand
	 * @param hostname
	 * @param port
	 * @param username
	 * @param password
	 * @param keyfile
	 * @param serverAliveInterval
	 * @param pty
	 */
	@DataBoundConstructor
	public UnixRemoteBuilder(String command, String scpCommand, String hostname, String port, String username, String password, String keyfile,
			String serverAliveInterval, Boolean pty) {
		
		this.scpCommand = scpCommand;
		this.command = command;
		this.hostname = hostname;
		this.port = port;
		this.username = username;
		this.password = password;
		this.keyfile = keyfile;
		this.serverAliveInterval = serverAliveInterval;
		this.pty = pty;
	}


	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

		try { 
			
			// First initialize the executors and deal with "parameterized values" replacement
			
			Map<String, String> envVars = prepareExecute(build, listener);		
			
			// Execute the SCP command to the remote host to prepare the environment
			
			executeSCP(build, listener, envVars);
			
			// Finally, let the SSH commands be executed on this remote host

			return executeSSH(listener, envVars);
		} 
		catch(Exception e) {
			
			listener.getLogger().println("[ERROR] Secure shell (SSH) execution exception : " + e.getMessage());
			return false;
		}
	}


	/**
	 * 
	 * @param build
	 * @param listener
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private Map<String, String> prepareExecute(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
		
		Map<String, String> envVars = new HashMap<String, String>();
		
		envVars.putAll(build.getEnvironment(listener));
		envVars.putAll(build.getBuildVariables());

		scpSite = new SCPChannel(hostname, String.valueOf(port), username, password, keyfile, String.valueOf(serverAliveInterval));
		sshSite = new SSHChannel(hostname, String.valueOf(port), username, password, keyfile, String.valueOf(serverAliveInterval));

		ParameterizedUtils.reflectiveParameterizedValueReplacer(sshSite, envVars, listener.getLogger());
		ParameterizedUtils.reflectiveParameterizedValueReplacer(scpSite, envVars, listener.getLogger());
		
		return envVars;
	}
	
	
	/**
	 * 
	 * @param build
	 * @param listener
	 * @param envVars
	 * @throws SSHBuilderException
	 * @throws InterruptedException
	 */
	private void executeSCP(AbstractBuild<?, ?> build, BuildListener listener, Map<String, String> envVars) throws SSHBuilderException, InterruptedException {
		
		if (scpSite != null && command != null && command.trim().length() > 0) {
			
			listener.getLogger().printf("executing scp:%n%s%n", scpCommand);
			executeSCPCommands(ParameterizedUtils.replaceParametrizedScript(scpCommand, envVars), build, listener.getLogger());
		}
	}

	
	/**
	 * 
	 * @param listener
	 * @param envVars
	 * @return
	 * @throws InterruptedException
	 */
	private boolean executeSSH(BuildListener listener, Map<String, String> envVars) throws InterruptedException {
		
		if (sshSite != null && command != null && command.trim().length() > 0) {
			
			listener.getLogger().printf("executing script:%n%s%n", command);
			return sshSite.executeCommand(listener.getLogger(), ParameterizedUtils.replace(command, envVars)) == 0;
		}
		
		return true;
	}

	/**
	 * 
	 * @param logger
	 * @param scpCommands
	 * @param build
	 * @throws InterruptedException
	 * @throws FileNotFoundException 
	 * @throws IOException
	 * @throws JSchException
	 */
	public void executeSCPCommands(String scpCommands, AbstractBuild<?, ?> build, PrintStream logger) throws SSHBuilderException, InterruptedException {
		
		try {

			List<String> commands = Arrays.asList(scpCommands.split("\n"));

			for (String command : commands) {
				if(command.trim().length() > 0 && command.contains(" "))
				{
					String sourceFile = command.substring(0, command.indexOf(' '));
					String targetFile = command.substring(command.indexOf(' ') + 1, command.length()).trim();
	
					FilePath sourceFilePath = JenkinsUtils.getWorkspaceFile(sourceFile, build);
	
					scpSite.executeCommand(sourceFile, sourceFilePath.read(), sourceFilePath.length(), sourceFilePath.lastModified(), targetFile, logger);
				}
			}
		} 
		catch (JSchException e) 
		{
			throw new SSHBuilderException("[ERROR] SSH connection error : " + e.getMessage(), e);
		} 
		catch (IOException e) 
		{
			throw new SSHBuilderException("[ERROR] " + e.getMessage(), e);
		}
	}


	/**
	 * 
	 *
	 */
	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Execute parameterized shell script (ssh/scp) on a remote host";
		}

		@Override
		public Builder newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
			return req.bindJSON(clazz, formData);
		}
		
		/**
		 * 
		 * @param keyfile
		 * @return
		 */
		public FormValidation doKeyfileCheck(@QueryParameter String keyfile) {
			keyfile = Util.fixEmpty(keyfile);
			if (keyfile != null) {
				File f = new File(keyfile);
				if (!f.isFile()) {
					return FormValidation.error("keyfile does not exist");
				}
			}

			return FormValidation.ok();
		}
		
		/**
		 * 
		 * @param request
		 * @return
		 */
		public FormValidation doConnectionCheck(StaplerRequest request) {
			
			if (containsParameterizedValue(request)) {
				return FormValidation.warning("Parameterized value(s) detected : connection not tested, parameter(s) will be evaluated at the execution time");
			}
			
			SSHChannel site = new SSHChannel(request.getParameter("hostname"), request.getParameter("port"), request.getParameter("username"), request.getParameter("password"),
											 request.getParameter("keyfile"), request.getParameter("serverAliveInterval"));
			try {
				site.testConnection(System.out);
			} 
			catch (JSchException e) {
				LOGGER.log(Level.SEVERE, e.getMessage());
				return FormValidation.error("Can't connect to server");
			}
			catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.getMessage());
				return FormValidation.error("Can't connect to server");
			}
		
			return FormValidation.respond(Kind.OK, "<font color='green'><b>The connection was successfully established..</b></font>");
		}
		
		/**
		 * 
		 * @param scpCommand
		 * @return
		 */
		public FormValidation doScpCommandCheck(@QueryParameter String scpCommand) {
			
			final String USAGE = "Usage: relativeToWorkspaceSourceFile /absolute/target/path/";
			
			try {

				scpCommand = Util.fixEmpty(scpCommand);
				
				if (scpCommand != null) {
					
					List<String> commands = Arrays.asList(scpCommand.split("\n"));
					
					for (String command : commands) {
	
						String sourceFile=command.substring(0, command.indexOf(' '));
						String targetFile=command.substring(command.indexOf(' ') + 1, command.length()).trim();
		
						
						if (new File(sourceFile).isAbsolute()) {
							return FormValidation.error("Argument '" + sourceFile + "' is not a valid source file description. Please provide a file path relative to the workspace root.\n" + USAGE);
						}
						
//						if (!new File(targetFile).isAbsolute()) {
//							return FormValidation.error("Argument '" + targetFile + "' is not a valid absolute path description for the destination.\n" + USAGE);
//						}
					}
				}
			}
			catch(Exception e){
				return FormValidation.error("Malformed SCP command(s). " + USAGE);
			}

			return FormValidation.ok();
		}

		/**
		 * 
		 * @param request
		 * @param hostname
		 * @return
		 */
		@SuppressWarnings("unchecked")
		private boolean containsParameterizedValue(StaplerRequest request) {
			
		
			for (String iterable_element : ((Set<String>)request.getParameterMap().keySet())) {
				
				if(ParameterizedUtils.containsParameterizedValue(request.getParameter(iterable_element))){
					return true;
				}
			}
			return false;
		}          
		
		

	}


	/**
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}


	/**
	 * @param command the command to set
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	

	/**
	 * @return the scpCommand
	 */
	public String getScpCommand() {
		return scpCommand;
	}


	/**
	 * @param scpCommand the scpCommand to set
	 */
	public void setScpCommand(String scpCommand) {
		this.scpCommand = scpCommand;
	}


	/**
	 * @return the hostname
	 */
	public String getHostname() {
		return hostname;
	}


	/**
	 * @param hostname the hostname to set
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}


	/**
	 * @return the port
	 */
	public String getPort() {
		return port;
	}


	/**
	 * @param port the port to set
	 */
	public void setPort(String port) {
		this.port = port;
	}


	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}


	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}


	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}


	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}


	/**
	 * @return the keyfile
	 */
	public String getKeyfile() {
		return keyfile;
	}


	/**
	 * @param keyfile the keyfile to set
	 */
	public void setKeyfile(String keyfile) {
		this.keyfile = keyfile;
	}


	/**
	 * @return the serverAliveInterval
	 */
	public String getServerAliveInterval() {
		return serverAliveInterval;
	}


	/**
	 * @param serverAliveInterval the serverAliveInterval to set
	 */
	public void setServerAliveInterval(String serverAliveInterval) {
		this.serverAliveInterval = serverAliveInterval;
	}


	/**
	 * @return the pty
	 */
	public Boolean getPty() {
		return pty;
	}


	/**
	 * @param pty the pty to set
	 */
	public void setPty(Boolean pty) {
		this.pty = pty;
	}


	/**
	 * @return the sshSite
	 */
	public SSHChannel getSshSite() {
		return sshSite;
	}


	/**
	 * @param sshSite the sshSite to set
	 */
	public void setSshSite(SSHChannel sshSite) {
		this.sshSite = sshSite;
	}
	
	
}
