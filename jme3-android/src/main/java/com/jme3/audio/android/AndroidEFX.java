package com.jme3.audio.android;

import com.jme3.audio.openal.EFX;
import java.nio.IntBuffer;

public class AndroidEFX implements EFX {

    public AndroidEFX() {
    }

    @Override
    public native void alGenAuxiliaryEffectSlots(int numSlots, IntBuffer buffers);

    @Override
    public native void alGenEffects(int numEffects, IntBuffer buffers);

    @Override
    public native void alEffecti(int effect, int param, int value);

    @Override
    public native void alAuxiliaryEffectSloti(int effectSlot, int param, int value);

    @Override
    public native void alDeleteEffects(int numEffects, IntBuffer buffers);

    @Override
    public native void alDeleteAuxiliaryEffectSlots(int numEffectSlots, IntBuffer buffers);

    @Override
    public native void alGenFilters(int numFilters, IntBuffer buffers);

    @Override
    public native void alFilteri(int filter, int param, int value);

    @Override
    public native void alFilterf(int filter, int param, float value);

    @Override
    public native void alDeleteFilters(int numFilters, IntBuffer buffers);

    @Override
    public native void alEffectf(int effect, int param, float value);
}
