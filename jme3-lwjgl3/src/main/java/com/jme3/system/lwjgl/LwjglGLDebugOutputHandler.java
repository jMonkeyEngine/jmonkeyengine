/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.system.lwjgl;

import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GLDebugMessageARBCallback;

import java.util.HashMap;

class LwjglGLDebugOutputHandler extends GLDebugMessageARBCallback {

    private static final HashMap<Integer, String> constMap = new HashMap<Integer, String>();
    private static final String MESSAGE_FORMAT =
            "[JME3] OpenGL debug message\r\n" +
                    "       ID: %d\r\n" +
                    "       Source: %s\r\n" +
                    "       Type: %s\r\n" +
                    "       Severity: %s\r\n" +
                    "       Message: %s";

    static {
        constMap.put(ARBDebugOutput.GL_DEBUG_SOURCE_API_ARB, "API");
        constMap.put(ARBDebugOutput.GL_DEBUG_SOURCE_APPLICATION_ARB, "APPLICATION");
        constMap.put(ARBDebugOutput.GL_DEBUG_SOURCE_OTHER_ARB, "OTHER");
        constMap.put(ARBDebugOutput.GL_DEBUG_SOURCE_SHADER_COMPILER_ARB, "SHADER_COMPILER");
        constMap.put(ARBDebugOutput.GL_DEBUG_SOURCE_THIRD_PARTY_ARB, "THIRD_PARTY");
        constMap.put(ARBDebugOutput.GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB, "WINDOW_SYSTEM");

        constMap.put(ARBDebugOutput.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB, "DEPRECATED_BEHAVIOR");
        constMap.put(ARBDebugOutput.GL_DEBUG_TYPE_ERROR_ARB, "ERROR");
        constMap.put(ARBDebugOutput.GL_DEBUG_TYPE_OTHER_ARB, "OTHER");
        constMap.put(ARBDebugOutput.GL_DEBUG_TYPE_PERFORMANCE_ARB, "PERFORMANCE");
        constMap.put(ARBDebugOutput.GL_DEBUG_TYPE_PORTABILITY_ARB, "PORTABILITY");
        constMap.put(ARBDebugOutput.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB, "UNDEFINED_BEHAVIOR");

        constMap.put(ARBDebugOutput.GL_DEBUG_SEVERITY_HIGH_ARB, "HIGH");
        constMap.put(ARBDebugOutput.GL_DEBUG_SEVERITY_MEDIUM_ARB, "MEDIUM");
        constMap.put(ARBDebugOutput.GL_DEBUG_SEVERITY_LOW_ARB, "LOW");
    }

    @Override
    public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
        String sourceStr = constMap.get(source);
        String typeStr = constMap.get(type);
        String severityStr = constMap.get(severity);

        System.err.println(String.format(MESSAGE_FORMAT, id, sourceStr, typeStr, severityStr, message));
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void callback(long args) {
        super.callback(args);
    }
}
