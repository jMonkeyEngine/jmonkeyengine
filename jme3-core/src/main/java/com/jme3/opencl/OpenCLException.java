/*
 * Copyright (c) 2009-2016 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.opencl;

/**
 * Generic OpenCL exception, can be thrown in every method of this package.
 * The error code and its name is reported in the message string as well as the OpenCL call that
 * causes this exception. Please refer to the official OpenCL specification
 * to see what might cause this exception.
 * @author shaman
 */
public class OpenCLException extends RuntimeException {
    private static final long serialVersionUID = 8471229972153694848L;

	private final int errorCode;
	
	/**
	 * Creates a new instance of <code>OpenCLExceptionn</code> without detail
	 * message.
	 */
	public OpenCLException() {
		errorCode = 0;
	}

	/**
	 * Constructs an instance of <code>OpenCLExceptionn</code> with the
	 * specified detail message.
	 *
	 * @param msg the detail message.
	 */
	public OpenCLException(String msg) {
		super(msg);
		errorCode = 0;
	}
	
	public OpenCLException(String msg, int errorCode) {
		super(msg);
		this.errorCode = errorCode;
	}

    /**
     * @return the error code
     */
	public int getErrorCode() {
		return errorCode;
	}

	
}
