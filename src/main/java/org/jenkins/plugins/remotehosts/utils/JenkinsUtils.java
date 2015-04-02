/*
 * The MIT License
 *
 * Copyright (C) 2014-2015 by Lab
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

package org.jenkins.plugins.remotehosts.utils;

import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;

import java.io.FileNotFoundException;
import java.io.IOException;

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
