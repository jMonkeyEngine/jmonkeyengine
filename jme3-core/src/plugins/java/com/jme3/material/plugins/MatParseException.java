/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.material.plugins;

import com.jme3.util.blockparser.Statement;
import java.io.IOException;

/**
 * Custom Exception to report a j3md Material definition file parsing error.
 * This exception reports the line number where the error occurred.
 *
 * @author Nehon
 */
public class MatParseException extends IOException {

    /**
     * creates a MatParseException
     *
     * @param expected the expected value
     * @param got the actual value
     * @param statement the read statement
     */
    public MatParseException(String expected, String got, Statement statement) {
        super("Error On line " + statement.getLineNumber() + " : " + statement.getLine() + "\n->Expected " + (expected == null ? "a statement" : expected) + ", got '" + got + "'!");

    }

    /**
     * creates a MatParseException
     *
     * @param text the error message
     * @param statement the statement where the error occur
     */
    public MatParseException(String text, Statement statement) {
        super("Error On line " + statement.getLineNumber() + " : " + statement.getLine() + "\n->" + text);
    }

    /**
     * creates a MatParseException
     *
     * @param expected the expected value
     * @param got the actual value
     * @param statement the read statement
     * @param cause the embed exception that occurred
     */
    public MatParseException(String expected, String got, Statement statement, Throwable cause) {
        super("Error On line " + statement.getLineNumber() + " : " + statement.getLine() + "\n->Expected " + (expected == null ? "a statement" : expected) + ", got '" + got + "'!", cause);

    }

    /**
     * creates a MatParseException
     *
     * @param text the error message
     * @param statement the statement where the error occur
     * @param cause the embed exception that occurred
     */
    public MatParseException(String text, Statement statement, Throwable cause) {
        super("Error On line " + statement.getLineNumber() + " : " + statement.getLine() + "\n->" + text, cause);
    }
}
