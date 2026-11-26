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
package com.jme3.audio.openal;

import com.jme3.audio.AudioBuffer;
import com.jme3.util.NativeObjectManager;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests that explicitly deleted OpenAL buffers leave no stale manager entry. */
class ALAudioRendererDisposalTest {

    @Test
    void explicitAudioBufferDeletionDoesNotDeleteTheNativeNameAgain() throws ReflectiveOperationException {
        AtomicInteger deletedBufferCount = new AtomicInteger();
        AL al = (AL) Proxy.newProxyInstance(AL.class.getClassLoader(), new Class<?>[]{AL.class},
                (proxy, method, args) -> {
                    if ("alDeleteBuffers".equals(method.getName())) {
                        deletedBufferCount.addAndGet((Integer) args[0]);
                        return null;
                    }
                    throw new AssertionError("Unexpected OpenAL call: " + method.getName());
                });
        ALAudioRenderer renderer = new ALAudioRenderer(al, null, null);
        NativeObjectManager manager = getObjectManager(renderer);

        AudioBuffer buffer = new AudioBuffer();
        buffer.setId(1);
        manager.registerObject(buffer);

        buffer.deleteObject(renderer);
        manager.deleteAllObjects(renderer);

        assertEquals(1, deletedBufferCount.get());
    }

    private static NativeObjectManager getObjectManager(ALAudioRenderer renderer)
            throws ReflectiveOperationException {
        Field field = ALAudioRenderer.class.getDeclaredField("objManager");
        field.setAccessible(true);
        return (NativeObjectManager) field.get(renderer);
    }
}
