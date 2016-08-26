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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.SOFTPauseDevice;

public class LwjglALC implements com.jme3.audio.openal.ALC {

    private long device;
    private long context;

    public void createALC() {
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);
    }

    public void destroyALC() {
        if (context != 0) {
            ALC10.alcDestroyContext(context);
            context = 0;
        }

        if (device != 0) {
            ALC10.alcCloseDevice(device);
            device = 0;
        }
    }

    public boolean isCreated() {
        return context != 0;
    }

    public String alcGetString(final int parameter) {
        return ALC10.alcGetString(device, parameter);
    }

    public boolean alcIsExtensionPresent(final String extension) {
        return ALC10.alcIsExtensionPresent(device, extension);
    }

    public void alcGetInteger(final int param, final IntBuffer buffer, final int size) {
        if (buffer.position() != 0) {
            throw new AssertionError();
        }
        if (buffer.limit() != size) {
            throw new AssertionError();
        }
        ALC10.alcGetIntegerv(device, param, buffer);
    }

    public void alcDevicePauseSOFT() {
        SOFTPauseDevice.alcDevicePauseSOFT(device);
    }

    public void alcDeviceResumeSOFT() {
        SOFTPauseDevice.alcDeviceResumeSOFT(device);
    }

}
