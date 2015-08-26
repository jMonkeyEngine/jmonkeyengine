package com.jme3.audio.lwjgl;

import com.jme3.audio.openal.EFX;
import org.lwjgl.openal.EXTEfx;

import java.nio.IntBuffer;

public class LwjglEFX implements EFX {

    public void alGenAuxiliaryEffectSlots(int numSlots, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numSlots) throw new AssertionError();
        EXTEfx.alGenAuxiliaryEffectSlots(buffers);
    }

    public void alGenEffects(int numEffects, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffects) throw new AssertionError();
        EXTEfx.alGenEffects(buffers);
    }

    public void alEffecti(int effect, int param, int value) {
        EXTEfx.alEffecti(effect, param, value);
    }

    public void alAuxiliaryEffectSloti(int effectSlot, int param, int value) {
        EXTEfx.alAuxiliaryEffectSloti(effectSlot, param, value);
    }

    public void alDeleteEffects(int numEffects, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffects) throw new AssertionError();
        EXTEfx.alDeleteEffects(buffers);
    }

    public void alDeleteAuxiliaryEffectSlots(int numEffectSlots, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffectSlots) throw new AssertionError();
        EXTEfx.alDeleteAuxiliaryEffectSlots(buffers);
    }

    public void alGenFilters(int numFilters, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numFilters) throw new AssertionError();
        EXTEfx.alGenFilters(buffers);
    }

    public void alFilteri(int filter, int param, int value) {
        EXTEfx.alFilteri(filter, param, value);
    }

    public void alFilterf(int filter, int param, float value) {
        EXTEfx.alFilterf(filter, param, value);
    }

    public void alDeleteFilters(int numFilters, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numFilters) throw new AssertionError();
        EXTEfx.alDeleteFilters(buffers);
    }

    public void alEffectf(int effect, int param, float value) {
        EXTEfx.alEffectf(effect, param, value);
    }
    
}
