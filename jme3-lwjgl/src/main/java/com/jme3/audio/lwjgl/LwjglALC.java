package com.jme3.audio.lwjgl;

import com.jme3.audio.openal.ALC;
import java.nio.IntBuffer;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCcontext;
import org.lwjgl.openal.ALCdevice;

public class LwjglALC implements ALC {

    @Override
    public void createALC() {
        int numRetriesRemaining = 4;
        int retryDelayMsec = 100; // start with an 0.1-second delay

        while (true) {
            try {
                AL.create();
                break;

            } catch (LWJGLException exception1) {
                if (numRetriesRemaining < 1) {
                    throw new RuntimeException(exception1);
                }

                // Retry to mitigate JME Issue 1383.
                --numRetriesRemaining;
                System.out.printf("Caught an LWJGLException from AL.create(). "
                        + "Will retry after %d msec, "
                        + "with %d more retr%s remaining.%n",
                        retryDelayMsec,
                        numRetriesRemaining,
                        (numRetriesRemaining == 1) ? "y" : "ies");

                try {
                    Thread.sleep(retryDelayMsec);
                } catch (InterruptedException exception2) {
                }

                // Triple the wait time after each failure.
                retryDelayMsec *= 3;
            }
        }
    }

    @Override
    public void destroyALC() {
        AL.destroy();
    }

    @Override
    public boolean isCreated() {
        return AL.isCreated();
    }

    @Override
    public String alcGetString(int parameter) {
        ALCcontext context = ALC10.alcGetCurrentContext();
        ALCdevice device = ALC10.alcGetContextsDevice(context);
        return ALC10.alcGetString(device, parameter);
    }

    @Override
    public boolean alcIsExtensionPresent(String extension) {
        ALCcontext context = ALC10.alcGetCurrentContext();
        ALCdevice device = ALC10.alcGetContextsDevice(context);
        return ALC10.alcIsExtensionPresent(device, extension);
    }

    @Override
    public void alcGetInteger(int param, IntBuffer buffer, int size) {
        if (buffer.position() != 0) throw new AssertionError();
        if (buffer.limit() != size) throw new AssertionError();
        
        ALCcontext context = ALC10.alcGetCurrentContext();
        ALCdevice device = ALC10.alcGetContextsDevice(context);
        ALC10.alcGetInteger(device, param, buffer);
    }

    @Override
    public void alcDevicePauseSOFT() {
    }

    @Override
    public void alcDeviceResumeSOFT() {
    }
    
}
