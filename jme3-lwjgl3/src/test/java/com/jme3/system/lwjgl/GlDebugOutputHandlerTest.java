/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system.lwjgl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.system.MemoryUtil;

class GlDebugOutputHandlerTest {
    @ParameterizedTest
    @ValueSource(strings = {"Invalid texture operation", "Shader message: café, éclair"})
    @ResourceLock(Resources.SYSTEM_ERR)
    void logsMessageBytesWithinTheCallbackRange(String message) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] guard = "DO_NOT_LOG_PAST_LENGTH".getBytes(StandardCharsets.UTF_8);
        ByteBuffer nativeMessage = MemoryUtil.memAlloc(bytes.length + guard.length);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previous = System.err;
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            // Exercise the Java callback body without allocating a native upcall.
            LwjglGLDebugOutputHandler callback = mock(LwjglGLDebugOutputHandler.class, CALLS_REAL_METHODS);
            nativeMessage.put(bytes).put(guard).flip();
            System.setErr(capture);
            callback.invoke(ARBDebugOutput.GL_DEBUG_SOURCE_APPLICATION_ARB,
                    ARBDebugOutput.GL_DEBUG_TYPE_ERROR_ARB, 42,
                    ARBDebugOutput.GL_DEBUG_SEVERITY_HIGH_ARB, bytes.length,
                    MemoryUtil.memAddress(nativeMessage), 0L);
        } finally {
            System.setErr(previous);
            MemoryUtil.memFree(nativeMessage);
        }
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Message: " + message), text);
        assertTrue(text.contains("Source: APPLICATION"), text);
        assertTrue(text.contains("Severity: HIGH"), text);
        assertFalse(text.contains("DO_NOT_LOG_PAST_LENGTH"), text);
    }
}
