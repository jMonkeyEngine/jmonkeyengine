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

import com.jme3.audio.openal.EFX;
import com.jogamp.openal.ALExt;
import com.jogamp.openal.ALFactory;
import java.nio.IntBuffer;

/**
 * Exposes EFX extension for JOAL.
 * 
 * @author Kirill Vainer
 */
public final class JoalEFX implements EFX {
    
    private final ALExt joalAlext;
    
    public JoalEFX() {
        joalAlext = ALFactory.getALExt();
    }

    public void alGenAuxiliaryEffectSlots(int numSlots, IntBuffer buffers) {
        joalAlext.alGenAuxiliaryEffectSlots(numSlots, buffers);
    }

    public void alGenEffects(int numEffects, IntBuffer buffers) {
        joalAlext.alGenEffects(numEffects, buffers);
    }

    public void alEffecti(int effect, int param, int value) {
        joalAlext.alEffecti(effect, param, value);
    }

    public void alAuxiliaryEffectSloti(int effectSlot, int param, int value) {
        joalAlext.alAuxiliaryEffectSloti(effectSlot, param, value);
    }

    public void alDeleteEffects(int numEffects, IntBuffer buffers) {
        joalAlext.alDeleteEffects(numEffects, buffers);
    }

    public void alDeleteAuxiliaryEffectSlots(int numEffectSlots, IntBuffer buffers) {
        joalAlext.alDeleteAuxiliaryEffectSlots(numEffectSlots, buffers);
    }

    public void alGenFilters(int numFilters, IntBuffer buffers) {
        joalAlext.alGenFilters(numFilters, buffers);
    }

    public void alFilteri(int filter, int param, int value) {
        joalAlext.alFilteri(filter, param, value);
    }

    public void alFilterf(int filter, int param, float value) {
        joalAlext.alFilterf(filter, param, value);
    }

    public void alDeleteFilters(int numFilters, IntBuffer buffers) {
        joalAlext.alDeleteFilters(numFilters, buffers);
    }

    public void alEffectf(int effect, int param, float value) {
        joalAlext.alEffectf(effect, param, value);
    }
}
