/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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

package com.jme3.audio.joal;

import com.jogamp.openal.ALCcontext;
import com.jogamp.openal.ALCdevice;
import com.jogamp.openal.ALFactory;
import com.jogamp.openal.util.ALut;
import java.nio.IntBuffer;

/**
 * Exposes ALC functions via JOAL.
 * 
 * @author Kirill Vainer
 */
public final class JoalALC implements com.jme3.audio.openal.ALC {

    private final com.jogamp.openal.ALC joalAlc;
    
    public JoalALC() {
        joalAlc = ALFactory.getALC();
    }
    
    private ALCdevice getALCDevice() {
        ALCcontext ctx = joalAlc.alcGetCurrentContext();

        if (ctx == null) {
            return null;
        }

        ALCdevice device = joalAlc.alcGetContextsDevice(ctx);

        if (device == null) {
            return null;
        }

        return device;
    }
    
    public void createALC() {
        ALut.alutInit();
        
        /*
        // Get handle to default device.
        ALCdevice device = joalAlc.alcOpenDevice(null);
        if (device == null) {
            throw new ALException("Error opening default OpenAL device");
        }
        
        // Create audio context.
        ALCcontext context = joalAlc.alcCreateContext(device, null);
        if (context == null) {
            throw new ALException("Error creating OpenAL context");
        }
        
        // Set active context.
        if (!joalAlc.alcMakeContextCurrent(context)) {
            throw new ALException("Failed to make OpenAL context current");
        }

        // Check for an error.
        if (joalAlc.alcGetError(device) != com.jogamp.openal.ALC.ALC_NO_ERROR) {
            throw new ALException("Error making OpenAL context current");
        }
        */
    }

    public void destroyALC() {
        /*
        ALCcontext ctx = joalAlc.alcGetCurrentContext();
        
        if (ctx == null) {
            return;
        }
        
        ALCdevice device = joalAlc.alcGetContextsDevice(ctx);
        
        if (device == null) {
            return;
        }
        
        if (!joalAlc.alcMakeContextCurrent(null)) {
            return;
        }
        
        joalAlc.alcDestroyContext(ctx);
        joalAlc.alcCloseDevice(device);
        */
        
        ALut.alutExit();
    }

    public boolean isCreated() {
        return getALCDevice() != null;
    }
    
    public String alcGetString(int parameter) {
        return joalAlc.alcGetString(getALCDevice(), parameter);
    }

    public boolean alcIsExtensionPresent(String extension) {
        return joalAlc.alcIsExtensionPresent(getALCDevice(), extension);
    }

    public void alcGetInteger(int param, IntBuffer buffer, int size) {
        joalAlc.alcGetIntegerv(getALCDevice(), param, size, buffer);
    }

    public void alcDevicePauseSOFT() {
    }

    public void alcDeviceResumeSOFT() {
    }
}
