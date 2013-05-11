package com.jme3.audio.android;

/**
 *
 * @author iwgeric
 */
public class AL {



    /* ********** */
    /* FROM ALC.h */
    /* ********** */

//    typedef struct ALCdevice_struct ALCdevice;
//    typedef struct ALCcontext_struct ALCcontext;


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
    static final int ALC_EXT_CAPTURE = 1;
    static final int ALC_CAPTURE_DEVICE_SPECIFIER = 0x310;
    static final int ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER = 0x311;
    static final int ALC_CAPTURE_SAMPLES = 0x312;


    /**
     * ALC_ENUMERATE_ALL_EXT enums
     */
    static final int ALC_ENUMERATE_ALL_EXT = 1;
    static final int ALC_DEFAULT_ALL_DEVICES_SPECIFIER = 0x1012;
    static final int ALC_ALL_DEVICES_SPECIFIER = 0x1013;


    /* ********** */
    /* FROM AL.h */
    /* ********** */

/** Boolean False. */
    static final int AL_FALSE = 0;

/** Boolean True. */
    static final int AL_TRUE = 1;

/* "no distance model" or "no buffer" */
    static final int AL_NONE = 0;

/** Indicate Source has relative coordinates. */
    static final int AL_SOURCE_RELATIVE = 0x202;



/**
 * Directional source, inner cone angle, in degrees.
 * Range:    [0-360]
 * Default:  360
 */
    static final int AL_CONE_INNER_ANGLE = 0x1001;

/**
 * Directional source, outer cone angle, in degrees.
 * Range:    [0-360]
 * Default:  360
 */
    static final int AL_CONE_OUTER_ANGLE = 0x1002;

/**
 * Specify the pitch to be applied at source.
 * Range:   [0.5-2.0]
 * Default: 1.0
 */
    static final int AL_PITCH = 0x1003;

/**
 * Specify the current location in three dimensional space.
 * OpenAL, like OpenGL, uses a right handed coordinate system,
 *  where in a frontal default view X (thumb) points right,
 *  Y points up (index finger), and Z points towards the
 *  viewer/camera (middle finger).
 * To switch from a left handed coordinate system, flip the
 *  sign on the Z coordinate.
 * Listener position is always in the world coordinate system.
 */
    static final int AL_POSITION = 0x1004;

/** Specify the current direction. */
    static final int AL_DIRECTION = 0x1005;

/** Specify the current velocity in three dimensional space. */
    static final int AL_VELOCITY = 0x1006;

/**
 * Indicate whether source is looping.
 * Type: ALboolean?
 * Range:   [AL_TRUE, AL_FALSE]
 * Default: FALSE.
 */
    static final int AL_LOOPING = 0x1007;

/**
 * Indicate the buffer to provide sound samples.
 * Type: ALuint.
 * Range: any valid Buffer id.
 */
    static final int AL_BUFFER = 0x1009;

/**
 * Indicate the gain (volume amplification) applied.
 * Type:   ALfloat.
 * Range:  ]0.0-  ]
 * A value of 1.0 means un-attenuated/unchanged.
 * Each division by 2 equals an attenuation of -6dB.
 * Each multiplicaton with 2 equals an amplification of +6dB.
 * A value of 0.0 is meaningless with respect to a logarithmic
 *  scale; it is interpreted as zero volume - the channel
 *  is effectively disabled.
 */
    static final int AL_GAIN = 0x100A;

/*
 * Indicate minimum source attenuation
 * Type: ALfloat
 * Range:  [0.0 - 1.0]
 *
 * Logarthmic
 */
    static final int AL_MIN_GAIN = 0x100D;

/**
 * Indicate maximum source attenuation
 * Type: ALfloat
 * Range:  [0.0 - 1.0]
 *
 * Logarthmic
 */
    static final int AL_MAX_GAIN = 0x100E;

/**
 * Indicate listener orientation.
 *
 * at/up
 */
    static final int AL_ORIENTATION = 0x100F;

/**
 * Source state information.
 */
    static final int AL_SOURCE_STATE = 0x1010;
    static final int AL_INITIAL = 0x1011;
    static final int AL_PLAYING = 0x1012;
    static final int AL_PAUSED = 0x1013;
    static final int AL_STOPPED = 0x1014;

/**
 * Buffer Queue params
 */
    static final int AL_BUFFERS_QUEUED = 0x1015;
    static final int AL_BUFFERS_PROCESSED = 0x1016;

/**
 * Source buffer position information
 */
    static final int AL_SEC_OFFSET = 0x1024;
    static final int AL_SAMPLE_OFFSET = 0x1025;
    static final int AL_BYTE_OFFSET = 0x1026;

/*
 * Source type (Static, Streaming or undetermined)
 * Source is Static if a Buffer has been attached using AL_BUFFER
 * Source is Streaming if one or more Buffers have been attached using alSourceQueueBuffers
 * Source is undetermined when it has the NULL buffer attached
 */
    static final int AL_SOURCE_TYPE = 0x1027;
    static final int AL_STATIC = 0x1028;
    static final int AL_STREAMING = 0x1029;
    static final int AL_UNDETERMINED = 0x1030;

/** Sound samples: format specifier. */
    static final int AL_FORMAT_MONO8 = 0x1100;
    static final int AL_FORMAT_MONO16 = 0x1101;
    static final int AL_FORMAT_STEREO8 = 0x1102;
    static final int AL_FORMAT_STEREO16 = 0x1103;

/**
 * source specific reference distance
 * Type: ALfloat
 * Range:  0.0 - +inf
 *
 * At 0.0, no distance attenuation occurs.  Default is
 * 1.0.
 */
    static final int AL_REFERENCE_DISTANCE = 0x1020;

/**
 * source specific rolloff factor
 * Type: ALfloat
 * Range:  0.0 - +inf
 *
 */
    static final int AL_ROLLOFF_FACTOR = 0x1021;

/**
 * Directional source, outer cone gain.
 *
 * Default:  0.0
 * Range:    [0.0 - 1.0]
 * Logarithmic
 */
    static final int AL_CONE_OUTER_GAIN = 0x1022;

/**
 * Indicate distance above which sources are not
 * attenuated using the inverse clamped distance model.
 *
 * Default: +inf
 * Type: ALfloat
 * Range:  0.0 - +inf
 */
    static final int AL_MAX_DISTANCE = 0x1023;

/**
 * Sound samples: frequency, in units of Hertz [Hz].
 * This is the number of samples per second. Half of the
 *  sample frequency marks the maximum significant
 *  frequency component.
 */
    static final int AL_FREQUENCY = 0x2001;
    static final int AL_BITS = 0x2002;
    static final int AL_CHANNELS = 0x2003;
    static final int AL_SIZE = 0x2004;

/**
 * Buffer state.
 *
 * Not supported for public use (yet).
 */
    static final int AL_UNUSED = 0x2010;
    static final int AL_PENDING = 0x2011;
    static final int AL_PROCESSED = 0x2012;


/** Errors: No Error. */
    static final int AL_NO_ERROR = 0;

/**
 * Invalid Name paramater passed to AL call.
 */
    static final int AL_INVALID_NAME = 0xA001;

/**
 * Invalid parameter passed to AL call.
 */
    static final int AL_INVALID_ENUM = 0xA002;

/**
 * Invalid enum parameter value.
 */
    static final int AL_INVALID_VALUE = 0xA003;

/**
 * Illegal call.
 */
    static final int AL_INVALID_OPERATION = 0xA004;


/**
 * No mojo.
 */
    static final int AL_OUT_OF_MEMORY = 0xA005;


/** Context strings: Vendor Name. */
    static final int AL_VENDOR = 0xB001;
    static final int AL_VERSION = 0xB002;
    static final int AL_RENDERER = 0xB003;
    static final int AL_EXTENSIONS = 0xB004;

/** Global tweakage. */

/**
 * Doppler scale.  Default 1.0
 */
    static final int AL_DOPPLER_FACTOR = 0xC000;

/**
 * Tweaks speed of propagation.
 */
    static final int AL_DOPPLER_VELOCITY = 0xC001;

/**
 * Speed of Sound in units per second
 */
    static final int AL_SPEED_OF_SOUND = 0xC003;

/**
 * Distance models
 *
 * used in conjunction with DistanceModel
 *
 * implicit: NONE, which disances distance attenuation.
 */
    static final int AL_DISTANCE_MODEL = 0xD000;
    static final int AL_INVERSE_DISTANCE = 0xD001;
    static final int AL_INVERSE_DISTANCE_CLAMPED = 0xD002;
    static final int AL_LINEAR_DISTANCE = 0xD003;
    static final int AL_LINEAR_DISTANCE_CLAMPED = 0xD004;
    static final int AL_EXPONENT_DISTANCE = 0xD005;
    static final int AL_EXPONENT_DISTANCE_CLAMPED = 0xD006;


    public static String GetALErrorMsg(int errorCode) {
        String errorText;
        switch (errorCode) {
            case AL_NO_ERROR:
                errorText = "No Error";
                break;
            case AL_INVALID_NAME:
                errorText = "Invalid Name";
                break;
            case AL_INVALID_ENUM:
                errorText = "Invalid Enum";
                break;
            case AL_INVALID_VALUE:
                errorText = "Invalid Value";
                break;
            case AL_INVALID_OPERATION:
                errorText = "Invalid Operation";
                break;
            case AL_OUT_OF_MEMORY:
                errorText = "Out of Memory";
                break;
            default:
                errorText = "Unknown Error Code: " + String.valueOf(errorCode);
        }
        return errorText;
    }
}

