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

package org.jenkins.plugins.remotehosts.exceptions;

/**
 * Exception wrapper for the SSHBuilder context
 */
public class SSHBuilderException extends Exception {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -6025190974114366352L;

	/**
	 * Default constructor
	 */
	public SSHBuilderException() {
		super();
	}

	/**
	 * Constructs a new SSHBuilderException with the specified detail message.
	 * The cause is not initialized, and may subsequently be initialized by a call to Throwable.initCause(java.lang.Throwable).
	 * 
	 * @param message - the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 */
	public SSHBuilderException(String message) {
		super(message);
	}

	/**
	 * Constructs a new exception with the specified cause and a detail message of (cause==null ? null : cause.toString())
	 * (which typically contains the class and detail message of cause).
	 * This constructor is useful for exceptions that are little more than wrappers for other throwables (for example, PrivilegedActionException).
	 * 
	 * @param cause - the cause (which is saved for later retrieval by the Throwable.getCause() method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public SSHBuilderException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 * Note that the detail message associated with cause is not automatically incorporated in this exception's detail message.

	 * @param message - the detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
	 * @param cause - the cause (which is saved for later retrieval by the Throwable.getCause() method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public SSHBuilderException(String message, Throwable cause) {
		super(message, cause);
	}
}
