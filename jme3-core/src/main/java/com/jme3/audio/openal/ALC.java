package com.jme3.audio.openal;

import java.nio.IntBuffer;

public interface ALC {

    /**
     * No error
     */
    static final int ALC_NO_ERROR = 0;

    /**
     * No device
     */
    static final int ALC_INVALID_DEVICE = 0xA001;

    /**
     * invalid context ID
     */
    static final int ALC_INVALID_CONTEXT = 0xA002;

    /**
     * bad enum
     */
    static final int ALC_INVALID_ENUM = 0xA003;

    /**
     * bad value
     */
    static final int ALC_INVALID_VALUE = 0xA004;

    /**
     * Out of memory.
     */
    static final int ALC_OUT_OF_MEMORY = 0xA005;

    /**
     * The Specifier string for default device
     */
    static final int ALC_DEFAULT_DEVICE_SPECIFIER = 0x1004;
    static final int ALC_DEVICE_SPECIFIER = 0x1005;
    static final int ALC_EXTENSIONS = 0x1006;

    static final int ALC_MAJOR_VERSION = 0x1000;
    static final int ALC_MINOR_VERSION = 0x1001;

    static final int ALC_ATTRIBUTES_SIZE = 0x1002;
    static final int ALC_ALL_ATTRIBUTES = 0x1003;

    /**
     * Capture extension
     */
    static final int ALC_CAPTURE_DEVICE_SPECIFIER = 0x310;
    static final int ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER = 0x311;
    static final int ALC_CAPTURE_SAMPLES = 0x312;

    /**
     * ALC_ENUMERATE_ALL_EXT enums
     */
    static final int ALC_DEFAULT_ALL_DEVICES_SPECIFIER = 0x1012;
    static final int ALC_ALL_DEVICES_SPECIFIER = 0x1013;

    //public static ALCCapabilities createCapabilities(long device);
    
    public void createALC();
    public void destroyALC();
    public boolean isCreated();
    public String alcGetString(int parameter);
    public boolean alcIsExtensionPresent(String extension);
    public void alcGetInteger(int param, IntBuffer buffer, int size);
    public void alcDevicePauseSOFT();
    public void alcDeviceResumeSOFT();
}
