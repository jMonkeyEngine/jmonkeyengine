package com.jme3.audio.lwjgl;

import com.jme3.audio.openal.EFX;
import java.nio.IntBuffer;
import org.lwjgl.openal.EFX10;

public class LwjglEFX implements EFX {

    @Override
    public void alGenAuxiliaryEffectSlots(int numSlots, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numSlots) throw new AssertionError();
        EFX10.alGenAuxiliaryEffectSlots(buffers);
    }

    @Override
    public void alGenEffects(int numEffects, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffects) throw new AssertionError();
        EFX10.alGenEffects(buffers);
    }

    @Override
    public void alEffecti(int effect, int param, int value) {
        EFX10.alEffecti(effect, param, value);
    }

    @Override
    public void alAuxiliaryEffectSloti(int effectSlot, int param, int value) {
        EFX10.alAuxiliaryEffectSloti(effectSlot, param, value);
    }

    @Override
    public void alDeleteEffects(int numEffects, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffects) throw new AssertionError();
        EFX10.alDeleteEffects(buffers);
    }

    @Override
    public void alDeleteAuxiliaryEffectSlots(int numEffectSlots, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numEffectSlots) throw new AssertionError();
        EFX10.alDeleteAuxiliaryEffectSlots(buffers);
    }

    @Override
    public void alGenFilters(int numFilters, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numFilters) throw new AssertionError();
        EFX10.alGenFilters(buffers);
    }

    @Override
    public void alFilteri(int filter, int param, int value) {
        EFX10.alFilteri(filter, param, value);
    }

    @Override
    public void alFilterf(int filter, int param, float value) {
        EFX10.alFilterf(filter, param, value);
    }

    @Override
    public void alDeleteFilters(int numFilters, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numFilters) throw new AssertionError();
        EFX10.alDeleteFilters(buffers);
    }

    @Override
    public void alEffectf(int effect, int param, float value) {
        EFX10.alEffectf(effect, param, value);
    }
    
}
