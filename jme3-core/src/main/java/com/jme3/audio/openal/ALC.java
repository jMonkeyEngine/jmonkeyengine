package com.jme3.audio.openal;

import java.nio.IntBuffer;

public interface ALC {

    /**
     * No error
     */
    public static final int ALC_NO_ERROR = 0;

    /**
     * No device
     */
    public static final int ALC_INVALID_DEVICE = 0xA001;

    /**
     * invalid context ID
     */
    public static final int ALC_INVALID_CONTEXT = 0xA002;

    /**
     * bad enum
     */
    public static final int ALC_INVALID_ENUM = 0xA003;

    /**
     * bad value
     */
    public static final int ALC_INVALID_VALUE = 0xA004;

    /**
     * Out of memory.
     */
    public static final int ALC_OUT_OF_MEMORY = 0xA005;

    /**
     * The Specifier string for default device
     */
    public static final int ALC_DEFAULT_DEVICE_SPECIFIER = 0x1004;
    public static final int ALC_DEVICE_SPECIFIER = 0x1005;
    public static final int ALC_EXTENSIONS = 0x1006;

    public static final int ALC_MAJOR_VERSION = 0x1000;
    public static final int ALC_MINOR_VERSION = 0x1001;

    public static final int ALC_ATTRIBUTES_SIZE = 0x1002;
    public static final int ALC_ALL_ATTRIBUTES = 0x1003;

    /**
     * Capture extension
     */
    public static final int ALC_CAPTURE_DEVICE_SPECIFIER = 0x310;
    public static final int ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER = 0x311;
    public static final int ALC_CAPTURE_SAMPLES = 0x312;

    /**
     * ALC_ENUMERATE_ALL_EXT enums
     */
    public static final int ALC_DEFAULT_ALL_DEVICES_SPECIFIER = 0x1012;
    public static final int ALC_ALL_DEVICES_SPECIFIER = 0x1013;

    //public static ALCCapabilities createCapabilities(long device);
    public static final int ALC_CONNECTED = 0x313;

    /**
     * Creates an AL context.
     */
    public void createALC();

    /**
     * Destroys an AL context.
     */
    public void destroyALC();

    /**
     * Checks of creating an AL context.
     *
     * @return true if an AL context is created.
     */
    public boolean isCreated();

    /**
     * Obtains string value(s) from ALC.
     *
     * @param parameter the information to query. One of:
     *  {@link #ALC_DEFAULT_DEVICE_SPECIFIER DEFAULT_DEVICE_SPECIFIER}
     *  {@link #ALC_DEVICE_SPECIFIER DEVICE_SPECIFIER}
     *  {@link #ALC_EXTENSIONS EXTENSIONS}
     *  {@link #ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER CAPTURE_DEFAULT_DEVICE_SPECIFIER}
     *  {@link #ALC_CAPTURE_DEVICE_SPECIFIER CAPTURE_DEVICE_SPECIFIER}
     * @return the parameter value
     */
    public String alcGetString(int parameter);

    /**
     * Verifies that a given extension is available for the current context and the device it is associated with.
     *
     * <p>Invalid and unsupported string tokens return ALC_FALSE. A {@code NULL} deviceHandle is acceptable. {@code extName} is not case sensitive – the implementation
     * will convert the name to all upper-case internally (and will express extension names in upper-case).</p>
     *
     * @param extension the extension name.
     * @return true if the extension is available, otherwise false
     */
    public boolean alcIsExtensionPresent(String extension);

    /**
     * Obtains integer value(s) from ALC.
     *
     * @param param  the information to query. One of:
     *  {@link #ALC_MAJOR_VERSION MAJOR_VERSION}
     *  {@link #ALC_MINOR_VERSION MINOR_VERSION}
     *  {@link #ALC_ATTRIBUTES_SIZE ATTRIBUTES_SIZE}
     *  {@link #ALC_ALL_ATTRIBUTES ALL_ATTRIBUTES}
     *  {@link #ALC_CAPTURE_SAMPLES CAPTURE_SAMPLES}
     * @param buffer the destination buffer.
     * @param size   the buffer size.
     */
    public void alcGetInteger(int param, IntBuffer buffer, int size);

    /**
     * Pauses a playback device.
     *
     * <p>When paused, no contexts associated with the device will be processed or updated. Playing sources will not produce sound, have their offsets
     * incremented, or process any more buffers, until the device is resumed. Pausing a device that is already paused is a legal no-op.</p>
     */
    public void alcDevicePauseSOFT();

    /**
     * Resumes playback of a paused device.
     *
     * <p>This will restart processing on the device -- sources will resume playing sound as normal. Resuming playback on a device that is not paused is a legal
     * no-op.</p>
     *
     * <p>These functions are not reference counted. alcDeviceResumeSOFT only needs to be called once to resume playback, regardless of how many times
     * {@link #alcDevicePauseSOFT DevicePauseSOFT} was called.</p>
     */
    public void alcDeviceResumeSOFT();
}
