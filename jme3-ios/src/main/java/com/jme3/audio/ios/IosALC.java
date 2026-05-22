package com.jme3.audio.ios;

import com.jme3.audio.openal.ALC;

import java.nio.IntBuffer;

public final class IosALC implements ALC {
    public IosALC() {
    }

    @Override
    public void createALC() {
        org.ngengine.libjglios.openal.ios.IosALC.createALC();
    }

    @Override
    public void destroyALC() {
        org.ngengine.libjglios.openal.ios.IosALC.destroyALC();
    }

    @Override
    public boolean isCreated() {
        return org.ngengine.libjglios.openal.ios.IosALC.isCreated();
    }

    @Override
    public String alcGetString(int parameter) {
        return org.ngengine.libjglios.openal.ios.IosALC.alcGetString(parameter);
    }

    @Override
    public boolean alcIsExtensionPresent(String extension) {
        return org.ngengine.libjglios.openal.ios.IosALC.alcIsExtensionPresent(extension);
    }

    @Override
    public void alcGetInteger(int param, IntBuffer buffer, int size) {
        org.ngengine.libjglios.openal.ios.IosALC.alcGetInteger(param, buffer, size);
    }

    @Override
    public void alcDevicePauseSOFT() {
        org.ngengine.libjglios.openal.ios.IosALC.alcDevicePauseSOFT();
    }

    @Override
    public void alcDeviceResumeSOFT() {
        org.ngengine.libjglios.openal.ios.IosALC.alcDeviceResumeSOFT();
    }
}
