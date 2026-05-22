package com.jme3.audio.ios;

import com.jme3.audio.openal.EFX;

import java.nio.IntBuffer;

public final class IosEFX implements EFX {
    public IosEFX() {
    }

    @Override
    public void alGenAuxiliaryEffectSlots(int numSlots, IntBuffer buffers) {
        org.ngengine.libjglios.openal.ios.IosEFX.alGenAuxiliaryEffectSlots(numSlots, buffers);
    }

    @Override
    public void alGenEffects(int numEffects, IntBuffer buffers) {
        org.ngengine.libjglios.openal.ios.IosEFX.alGenEffects(numEffects, buffers);
    }

    @Override
    public void alEffecti(int effect, int param, int value) {
        org.ngengine.libjglios.openal.ios.IosEFX.alEffecti(effect, param, value);
    }

    @Override
    public void alAuxiliaryEffectSloti(int effectSlot, int param, int value) {
        org.ngengine.libjglios.openal.ios.IosEFX.alAuxiliaryEffectSloti(effectSlot, param, value);
    }

    @Override
    public void alDeleteEffects(int numEffects, IntBuffer buffers) {
        org.ngengine.libjglios.openal.ios.IosEFX.alDeleteEffects(numEffects, buffers);
    }

    @Override
    public void alDeleteAuxiliaryEffectSlots(int numEffectSlots, IntBuffer buffers) {
        org.ngengine.libjglios.openal.ios.IosEFX.alDeleteAuxiliaryEffectSlots(numEffectSlots, buffers);
    }

    @Override
    public void alGenFilters(int numFilters, IntBuffer buffers) {
        org.ngengine.libjglios.openal.ios.IosEFX.alGenFilters(numFilters, buffers);
    }

    @Override
    public void alFilteri(int filter, int param, int value) {
        org.ngengine.libjglios.openal.ios.IosEFX.alFilteri(filter, param, value);
    }

    @Override
    public void alFilterf(int filter, int param, float value) {
        org.ngengine.libjglios.openal.ios.IosEFX.alFilterf(filter, param, value);
    }

    @Override
    public void alDeleteFilters(int numFilters, IntBuffer buffers) {
        org.ngengine.libjglios.openal.ios.IosEFX.alDeleteFilters(numFilters, buffers);
    }

    @Override
    public void alEffectf(int effect, int param, float value) {
        org.ngengine.libjglios.openal.ios.IosEFX.alEffectf(effect, param, value);
    }
}
