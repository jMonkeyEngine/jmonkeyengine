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
package com.jme3.audio.lwjgl;

import com.jme3.audio.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALContext;

import java.nio.IntBuffer;

import static org.lwjgl.openal.ALC10.alcGetContextsDevice;
import static org.lwjgl.openal.ALC10.alcGetCurrentContext;

public class LwjglALC implements ALC {

    private ALContext context;

    public void createALC() {
        context = ALContext.create();
    }

    public void destroyALC() {
        if (context != null) {
            context.destroy();
        }
    }

    public boolean isCreated() {
        return context != null;
    }

    public String alcGetString(final int parameter) {
        final long context = alcGetCurrentContext();
        final long device = alcGetContextsDevice(context);
        return ALC10.alcGetString(device, parameter);
    }

    public boolean alcIsExtensionPresent(final String extension) {
        final long context = alcGetCurrentContext();
        final long device = alcGetContextsDevice(context);
        return ALC10.alcIsExtensionPresent(device, extension);
    }

    public void alcGetInteger(final int param, final IntBuffer buffer, final int size) {
        if (buffer.position() != 0) throw new AssertionError();
        if (buffer.limit() != size) throw new AssertionError();

        final long context = alcGetCurrentContext();
        final long device = alcGetContextsDevice(context);
        final int value = ALC10.alcGetInteger(device, param);
        //buffer.put(value);
    }

    public void alcDevicePauseSOFT() {
    }

    public void alcDeviceResumeSOFT() {
    }

}
