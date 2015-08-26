package com.jme3.audio.lwjgl;

import com.jme3.audio.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALContext;

import java.nio.IntBuffer;

import static org.lwjgl.openal.ALC10.alcGetContextsDevice;
import static org.lwjgl.openal.ALC10.alcGetCurrentContext;

public class LwjglALC implements ALC {

    private ALContext context;

    public void createALC() {
        context = ALContext.create();
    }

    public void destroyALC() {
        if (context != null) {
            context.destroy();
        }
    }

    public boolean isCreated() {
        return context != null;
    }

    public String alcGetString(final int parameter) {
        final long context = alcGetCurrentContext();
        final long device = alcGetContextsDevice(context);
        return ALC10.alcGetString(device, parameter);
    }

    public boolean alcIsExtensionPresent(final String extension) {
        final long context = alcGetCurrentContext();
        final long device = alcGetContextsDevice(context);
        return ALC10.alcIsExtensionPresent(device, extension);
    }

    public void alcGetInteger(final int param, final IntBuffer buffer, final int size) {
        if (buffer.position() != 0) throw new AssertionError();
        if (buffer.limit() != size) throw new AssertionError();

        final long context = alcGetCurrentContext();
        final long device = alcGetContextsDevice(context);
        final int value = ALC10.alcGetInteger(device, param);
        //buffer.put(value);
    }

    public void alcDevicePauseSOFT() {
    }

    public void alcDeviceResumeSOFT() {
    }

}
