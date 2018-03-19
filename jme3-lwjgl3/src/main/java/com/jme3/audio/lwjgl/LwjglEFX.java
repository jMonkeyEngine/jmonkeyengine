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

import com.jme3.audio.openal.EFX;
import org.lwjgl.openal.EXTEfx;

import java.nio.IntBuffer;

/**
 * The LWJGL implementation of {@link EFX}.
 */
public class LwjglEFX implements EFX {

    @Override
    public void alGenAuxiliaryEffectSlots(final int numSlots, final IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numSlots) throw new AssertionError();
        EXTEfx.alGenAuxiliaryEffectSlots(buffers);
    }

    @Override
    public void alGenEffects(final int numEffects, final IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffects) throw new AssertionError();
        EXTEfx.alGenEffects(buffers);
    }

    @Override
    public void alEffecti(final int effect, final int param, final int value) {
        EXTEfx.alEffecti(effect, param, value);
    }

    @Override
    public void alAuxiliaryEffectSloti(final int effectSlot, final int param, final int value) {
        EXTEfx.alAuxiliaryEffectSloti(effectSlot, param, value);
    }

    @Override
    public void alDeleteEffects(final int numEffects, final IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffects) throw new AssertionError();
        EXTEfx.alDeleteEffects(buffers);
    }

    @Override
    public void alDeleteAuxiliaryEffectSlots(final int numEffectSlots, final IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffectSlots) throw new AssertionError();
        EXTEfx.alDeleteAuxiliaryEffectSlots(buffers);
    }

    @Override
    public void alGenFilters(final int numFilters, final IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numFilters) throw new AssertionError();
        EXTEfx.alGenFilters(buffers);
    }

    @Override
    public void alFilteri(final int filter, final int param, final int value) {
        EXTEfx.alFilteri(filter, param, value);
    }

    @Override
    public void alFilterf(final int filter, final int param, final float value) {
        EXTEfx.alFilterf(filter, param, value);
    }

    @Override
    public void alDeleteFilters(final int numFilters, final IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numFilters) throw new AssertionError();
        EXTEfx.alDeleteFilters(buffers);
    }

    @Override
    public void alEffectf(final int effect, final int param, final float value) {
        EXTEfx.alEffectf(effect, param, value);
    }
}
