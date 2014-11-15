package com.jme3.audio.lwjgl;

import com.jme3.audio.openal.ALC;
import java.nio.IntBuffer;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;

public class LwjglALC implements ALC {

    public void createALC() {
        try {
            AL.create();
        } catch (LWJGLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void destroyALC() {
        AL.destroy();
    }

    public boolean isCreated() {
        return AL.isCreated();
    }

    public String alcGetString(int parameter) {
        ALCcontext context = ALC10.alcGetCurrentContext();
        ALCdevice device = ALC10.alcGetContextsDevice(context);
        return ALC10.alcGetString(device, parameter);
    }

    public boolean alcIsExtensionPresent(String extension) {
        ALCcontext context = ALC10.alcGetCurrentContext();
        ALCdevice device = ALC10.alcGetContextsDevice(context);
        return ALC10.alcIsExtensionPresent(device, extension);
    }

    public void alcGetInteger(int param, IntBuffer buffer, int size) {
        if (buffer.position() != 0) throw new AssertionError();
        if (buffer.limit() != size) throw new AssertionError();
        
        ALCcontext context = ALC10.alcGetCurrentContext();
        ALCdevice device = ALC10.alcGetContextsDevice(context);
        ALC10.alcGetInteger(device, param, buffer);
    }

    public void alcDevicePauseSOFT() {
    }

    public void alcDeviceResumeSOFT() {
    }
    
}
