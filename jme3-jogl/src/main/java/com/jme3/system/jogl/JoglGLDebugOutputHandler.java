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
package com.jme3.system.jogl;

import java.util.HashMap;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLDebugListener;
import com.jogamp.opengl.GLDebugMessage;

class JoglGLDebugOutputHandler implements GLDebugListener {

    private static final HashMap<Integer, String> constMap = new HashMap<Integer, String>();
    private static final String MESSAGE_FORMAT = 
            "[JME3] OpenGL debug message\r\n" +
            "       ID: %d\r\n" +
            "       Source: %s\r\n" +
            "       Type: %s\r\n" +
            "       Severity: %s\r\n" +
            "       Message: %s";
    
    static {
        constMap.put(GL2ES2.GL_DEBUG_SOURCE_API, "API");
        constMap.put(GL2ES2.GL_DEBUG_SOURCE_APPLICATION, "APPLICATION");
        constMap.put(GL2ES2.GL_DEBUG_SOURCE_OTHER, "OTHER");
        constMap.put(GL2ES2.GL_DEBUG_SOURCE_SHADER_COMPILER, "SHADER_COMPILER");
        constMap.put(GL2ES2.GL_DEBUG_SOURCE_THIRD_PARTY, "THIRD_PARTY");
        constMap.put(GL2ES2.GL_DEBUG_SOURCE_WINDOW_SYSTEM, "WINDOW_SYSTEM");
        
        constMap.put(GL2ES2.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR, "DEPRECATED_BEHAVIOR");
        constMap.put(GL2ES2.GL_DEBUG_TYPE_ERROR, "ERROR");
        constMap.put(GL2ES2.GL_DEBUG_TYPE_OTHER, "OTHER");
        constMap.put(GL2ES2.GL_DEBUG_TYPE_PERFORMANCE, "PERFORMANCE");
        constMap.put(GL2ES2.GL_DEBUG_TYPE_PORTABILITY, "PORTABILITY");
        constMap.put(GL2ES2.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR, "UNDEFINED_BEHAVIOR");
        
        constMap.put(GL2ES2.GL_DEBUG_SEVERITY_HIGH, "HIGH");
        constMap.put(GL2ES2.GL_DEBUG_SEVERITY_MEDIUM, "MEDIUM");
        constMap.put(GL2ES2.GL_DEBUG_SEVERITY_LOW, "LOW");
    }
    
    @Override
	public void messageSent(GLDebugMessage event) {
    	String sourceStr = constMap.get(event.getDbgSource());
        String typeStr = constMap.get(event.getDbgType());
        String severityStr = constMap.get(event.getDbgSeverity());
        
        System.err.println(String.format(MESSAGE_FORMAT, event.getDbgId(), sourceStr, typeStr, severityStr, event.getDbgMsg()));
	}
    
}
