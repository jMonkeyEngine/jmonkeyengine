package com.jme3.audio.ios;

import com.jme3.audio.openal.ALC;
import java.nio.IntBuffer;

public final class IosALC implements ALC {
    
    public IosALC() {
    }

    @Override
    public native void createALC();

    @Override
    public native void destroyALC();

    @Override
    public native boolean isCreated();

    @Override
    public native String alcGetString(int parameter);
    
    @Override
    public native boolean alcIsExtensionPresent(String extension);
    
    @Override
    public native void alcGetInteger(int param, IntBuffer buffer, int size);
    
    @Override
    public native void alcDevicePauseSOFT();
    
    @Override
    public native void alcDeviceResumeSOFT();
}
