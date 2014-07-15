package org.jenkins.plugins.remotehosts.protocoles;

import hudson.FilePath;
import hudson.model.AbstractBuild;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.jenkins.plugins.remotehosts.exceptions.SSHBuilderException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class SCPChannel {

	public static final Logger LOGGER = Logger.getLogger(SCPChannel.class.getName());

	String hostname;
	String username;
	String password;
	String keyfile;

	int port;
	int serverAliveInterval = 0;

	Boolean pty = Boolean.FALSE;
	transient String resolvedHostname = null;

	/**
	 * 
	 * @param hostname
	 * @param port
	 * @param username
	 * @param password
	 * @param keyfile
	 * @param serverAliveInterval
	 */
	public SCPChannel(String hostname, String port, String username, String password, String keyfile, String serverAliveInterval) {

		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.keyfile = keyfile;

		this.setPort(port);
		this.setServerAliveInterval(serverAliveInterval);
	}

	public void setPort(String port) {
		try {
			this.port = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			this.port = 22;
		}
	}

	public void setServerAliveInterval(String serverAliveInterval) {
		try {
			this.serverAliveInterval = Integer.parseInt(serverAliveInterval);
		} catch (NumberFormatException e) {
			this.serverAliveInterval = 0;
		}
	}


	/**
	 * 
	 * @param sourceFile
	 * @param targetFile
	 * @param build
	 * @param logger
	 * @return 
	 * @throws InterruptedException
	 * @throws SSHBuilderException
	 * @throws IOException 
	 * @throws JSchException 
	 */
	public void executeCommand(String sourceFile, InputStream sourceInputStream, long sourceFileSize, long sourceLastModified, String targetFile, PrintStream logger)
	throws InterruptedException, SSHBuilderException, JSchException, IOException {

		logger.println("[SCP] " + "execute scp " + sourceFile + " " + username + "@" + hostname + ":" + targetFile);
		
		executeTransfert( sourceFile, sourceInputStream, sourceFileSize, sourceLastModified, targetFile);
		
		logger.println("[SCP] " + "exit-status: 0");
	}

	/**
	 * 
	 * @param fis
	 * @param sourceFileName
	 * @param sourceFileSize
	 * @param lastModified 
	 * @param remoteTargetedPath
	 * @return
	 * 
	 * @throws JSchException
	 * @throws IOException
	 */
	private void executeTransfert(String sourceFileName, InputStream fis, long sourceFileSize, long lastModified, String remoteTargetedPath) throws JSchException, IOException {

		Session session = createSession();

		// Open SCP channel over SSH (exec channel with 'scp -t rfile' command remotely)
		
		Channel channel = openScpChannel(remoteTargetedPath, session, false);

		// Get I/O streams for remote scp
		
		OutputStream out = channel.getOutputStream();
		InputStream in = channel.getInputStream();

		// Connect and check connection ack
		
		channelConnection(channel, in);

		// Send protocole header messages for required data and options
		
		sendPreservedFileAccessTimestamp(out, in, lastModified, false);	
		
		sendC0644FileSizeAndName(sourceFileName, sourceFileSize, out, in);

		// Finally, send the file stream, check the transaction ack and disconnect
		
		copyStreamAndCloseQuietly(fis, in, out);
		
		IOUtils.closeQuietly(in);
		
		channel.disconnect();
		session.disconnect();
	}

	/**
	 * 
	 * @return
	 * @throws JSchException
	 */
	private Session createSession() throws JSchException {

		JSch jsch = new JSch();

		Session session = jsch.getSession(username, hostname, port);
		
		if (this.keyfile != null && this.keyfile.length() > 0) 
		{
			jsch.addIdentity(this.keyfile, this.password);
		}
		else {
			session.setPassword(password);
		}

		UserInfo ui = new SSHUserInfo(password);
		
		session.setUserInfo(ui);
		session.setServerAliveInterval(serverAliveInterval);

		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.connect();

		return session;
	}


	/**
	 * 
	 * @param targetFile
	 * @param session
	 * @param ptimestamp
	 * @return
	 * @throws JSchException
	 */
	private Channel openScpChannel(String targetFile, Session session, boolean ptimestamp) throws JSchException {
		
		String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + targetFile;

		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);
		
		return channel;
	}
	
	/**
	 * 
	 * @param channel
	 * @param in
	 * @throws JSchException
	 * @throws IOException
	 */
	private void channelConnection(Channel channel, InputStream in) throws JSchException, IOException {
		
		channel.connect();

		if (checkAck(in) != 0) {
			throw new IOException("SCP channel connection error");
		}
	}

	/**
	 * 
	 * @param fileInput
	 * @param in2 
	 * @param out
	 * @throws IOException
	 */
	public static void copyStreamAndCloseQuietly(InputStream fileInput, InputStream in, OutputStream out) throws IOException {
 
		try {
			IOUtils.copy(fileInput, out);
			
		} finally {
			IOUtils.closeQuietly(fileInput);
			IOUtils.closeQuietly(out);
		}
	}

	private void sendPreservedFileAccessTimestamp(OutputStream out, InputStream in, long lastModified, boolean ptimestamp) throws IOException {

		if (ptimestamp) {
			
			String command = "T" + (lastModified / 1000) + " 0";
			// The access time should be sent here, but it is not accessible
			command += (" " + (lastModified / 1000) + " 0\n");
			
			out.write(command.getBytes());
			out.flush();
			
			if (checkAck(in) != 0) {
				throw new IOException("SCP channel error on preserve file access timestamp option");
			}
		}
	}

	/**
	 * Send "C0644 filesize filename", where filename should not include '/'
	 * 
	 * 
	 * @param sourceFileName
	 * @param filesize
	 * @param out
	 * @param in
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void sendC0644FileSizeAndName(String sourceFileName, long filesize, OutputStream out, InputStream in) throws IOException {

		String command = "C0644 " + filesize + " ";

		if (sourceFileName.lastIndexOf('/') > 0) {
			command += (sourceFileName.substring(sourceFileName.lastIndexOf('/') + 1) + "\n");
		} else {
			command += (sourceFileName + "\n");
		}

		out.write(command.getBytes());
		out.flush();

		if (checkAck(in) != 0) {
			throw new IOException("SCP channel error on C0644 file size and name");
		}
	}


	/**
	 * Check Ack and get return code : 0 for success, 1 error, 2 for fatal error
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	static int checkAck(InputStream in) throws IOException {
		
		int b = in.read();

		if (b == 1 || b == 2) {
			
			String message = "";
			
			try {  
				message += ((String)IOUtils.readLines(in).get(0)); 
			}
			catch(Exception e)
			{ 
				message += "Unexpected error";
			}
			
			throw new IOException(message);
		}
		return b;
	}
}