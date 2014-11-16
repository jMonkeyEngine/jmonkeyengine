package com.jme3.audio.lwjgl;

import com.jme3.audio.openal.EFX;
import java.nio.IntBuffer;
import org.lwjgl.openal.EFX10;

public class LwjglEFX implements EFX {

    public void alGenAuxiliaryEffectSlots(int numSlots, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numSlots) throw new AssertionError();
        EFX10.alGenAuxiliaryEffectSlots(buffers);
    }

    public void alGenEffects(int numEffects, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffects) throw new AssertionError();
        EFX10.alGenEffects(buffers);
    }

    public void alEffecti(int effect, int param, int value) {
        EFX10.alEffecti(effect, param, value);
    }

    public void alAuxiliaryEffectSloti(int effectSlot, int param, int value) {
        EFX10.alAuxiliaryEffectSloti(effectSlot, param, value);
    }

    public void alDeleteEffects(int numEffects, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffects) throw new AssertionError();
        EFX10.alDeleteEffects(buffers);
    }

    public void alDeleteAuxiliaryEffectSlots(int numEffectSlots, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffectSlots) throw new AssertionError();
        EFX10.alDeleteAuxiliaryEffectSlots(buffers);
    }

    public void alGenFilters(int numFilters, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numFilters) throw new AssertionError();
        EFX10.alGenFilters(buffers);
    }

    public void alFilteri(int filter, int param, int value) {
        EFX10.alFilteri(filter, param, value);
    }

    public void alFilterf(int filter, int param, float value) {
        EFX10.alFilterf(filter, param, value);
    }

    public void alDeleteFilters(int numFilters, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numFilters) throw new AssertionError();
        EFX10.alDeleteFilters(buffers);
    }

    public void alEffectf(int effect, int param, float value) {
        EFX10.alEffectf(effect, param, value);
    }
    
}
