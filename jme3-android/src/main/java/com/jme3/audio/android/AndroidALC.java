package com.jme3.audio.android;

import com.jme3.audio.openal.ALC;
import java.nio.IntBuffer;

public final class AndroidALC implements ALC {

    static {
         System.loadLibrary("openalsoftjme");
    }
    
    public AndroidALC() {
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
