package org.jenkins.plugins.remotehosts.utils;

import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.filters.StringInputStream;

public class JenkinsUtils {

	/**
	 * Private constructor preventing utility class instantiation
	 */
	private JenkinsUtils() {
		super();
	}
	

	/**
	 * 
	 * @param sourceFile
	 * @param build
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws FileNotFoundException
	 */
	public static FilePath getWorkspaceFile(String sourceFile, AbstractBuild<?, ?> build) throws IOException, InterruptedException, FileNotFoundException {
		
		FilePath slaveWorkspaceFile = build.getWorkspace().child(sourceFile);
		
		if (!slaveWorkspaceFile.exists()) {
			throw new FileNotFoundException("File not found: " + slaveWorkspaceFile.getRemote());
		}
		
		if (slaveWorkspaceFile.isDirectory()) {
			throw new IOException("The given file is a directory: " + slaveWorkspaceFile.getRemote());
		}
		
		return slaveWorkspaceFile;
	}
	
	
	/**
	 * 
	 * @param fileName
	 * @param content
	 * @param build
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void writeFileOnWorkspace(String fileName, String content, AbstractBuild<?, ?> build) throws IOException, InterruptedException {
		
		FilePath slaveWorkspaceFile = build.getWorkspace().child(fileName);
		
		Util.copyStreamAndClose(new StringInputStream(content), slaveWorkspaceFile.write());
	}


}
