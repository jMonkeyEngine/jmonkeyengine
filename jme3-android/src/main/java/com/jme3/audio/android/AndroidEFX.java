package com.jme3.audio.android;

import com.jme3.audio.openal.EFX;
import java.nio.IntBuffer;

public class AndroidEFX implements EFX {

    public AndroidEFX() {
    }

    public native void alGenAuxiliaryEffectSlots(int numSlots, IntBuffer buffers);

    public native void alGenEffects(int numEffects, IntBuffer buffers);

    public native void alEffecti(int effect, int param, int value);

    public native void alAuxiliaryEffectSloti(int effectSlot, int param, int value);

    public native void alDeleteEffects(int numEffects, IntBuffer buffers);

    public native void alDeleteAuxiliaryEffectSlots(int numEffectSlots, IntBuffer buffers);

    public native void alGenFilters(int numFilters, IntBuffer buffers);

    public native void alFilteri(int filter, int param, int value);

    public native void alFilterf(int filter, int param, float value);

    public native void alDeleteFilters(int numFilters, IntBuffer buffers);

    public native void alEffectf(int effect, int param, float value);
}
