package com.jme3.audio.ios;

import com.jme3.audio.openal.ALC;
import java.nio.IntBuffer;

public final class IosALC implements ALC {
    
    public IosALC() {
    }

    public native void createALC();

    public native void destroyALC();

    public native boolean isCreated();

    public native String alcGetString(int parameter);
    
    public native boolean alcIsExtensionPresent(String extension);
    
    public native void alcGetInteger(int param, IntBuffer buffer, int size);
    
    public native void alcDevicePauseSOFT();
    
    public native void alcDeviceResumeSOFT();
}
