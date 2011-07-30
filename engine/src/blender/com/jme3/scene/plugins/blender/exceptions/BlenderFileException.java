/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.scene.plugins.blender.exceptions;

/**
 * This exception is thrown when blend file data is somehow invalid.
 * @author Marcin Roguski
 */
public class BlenderFileException extends Exception {

    private static final long serialVersionUID = 7573482836437866767L;

    /**
     * Constructor. Creates an exception with no description.
     */
    public BlenderFileException() {
    }

    /**
     * Constructor. Creates an exception containing the given message.
     * @param message
     *        the message describing the problem that occured
     */
    public BlenderFileException(String message) {
        super(message);
    }

    /**
     * Constructor. Creates an exception that is based upon other thrown object. It contains the whole stacktrace then.
     * @param throwable
     *        an exception/error that occured
     */
    public BlenderFileException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Constructor. Creates an exception with both a message and stacktrace.
     * @param message
     *        the message describing the problem that occured
     * @param throwable
     *        an exception/error that occured
     */
    public BlenderFileException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
