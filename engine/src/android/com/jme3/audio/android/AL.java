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

    /* ********** */
    /* FROM efx.h */
    /* ********** */

    static final String ALC_EXT_EFX_NAME = "ALC_EXT_EFX";

    static final int ALC_EFX_MAJOR_VERSION = 0x20001;
    static final int ALC_EFX_MINOR_VERSION = 0x20002;
    static final int ALC_MAX_AUXILIARY_SENDS  = 0x20003;


///* Listener properties. */
//#define AL_METERS_PER_UNIT                       0x20004
//
///* Source properties. */
    static final int AL_DIRECT_FILTER = 0x20005;
    static final int AL_AUXILIARY_SEND_FILTER = 0x20006;
//#define AL_AIR_ABSORPTION_FACTOR                 0x20007
//#define AL_ROOM_ROLLOFF_FACTOR                   0x20008
//#define AL_CONE_OUTER_GAINHF                     0x20009
    static final int AL_DIRECT_FILTER_GAINHF_AUTO = 0x2000A;
//#define AL_AUXILIARY_SEND_FILTER_GAIN_AUTO       0x2000B
//#define AL_AUXILIARY_SEND_FILTER_GAINHF_AUTO     0x2000C
//
//
///* Effect properties. */
//
///* Reverb effect parameters */
    static final int AL_REVERB_DENSITY = 0x0001;
    static final int AL_REVERB_DIFFUSION = 0x0002;
    static final int AL_REVERB_GAIN = 0x0003;
    static final int AL_REVERB_GAINHF = 0x0004;
    static final int AL_REVERB_DECAY_TIME = 0x0005;
    static final int AL_REVERB_DECAY_HFRATIO = 0x0006;
    static final int AL_REVERB_REFLECTIONS_GAIN = 0x0007;
    static final int AL_REVERB_REFLECTIONS_DELAY = 0x0008;
    static final int AL_REVERB_LATE_REVERB_GAIN = 0x0009;
    static final int AL_REVERB_LATE_REVERB_DELAY = 0x000A;
    static final int AL_REVERB_AIR_ABSORPTION_GAINHF = 0x000B;
    static final int AL_REVERB_ROOM_ROLLOFF_FACTOR = 0x000C;
    static final int AL_REVERB_DECAY_HFLIMIT = 0x000D;

///* EAX Reverb effect parameters */
//#define AL_EAXREVERB_DENSITY                     0x0001
//#define AL_EAXREVERB_DIFFUSION                   0x0002
//#define AL_EAXREVERB_GAIN                        0x0003
//#define AL_EAXREVERB_GAINHF                      0x0004
//#define AL_EAXREVERB_GAINLF                      0x0005
//#define AL_EAXREVERB_DECAY_TIME                  0x0006
//#define AL_EAXREVERB_DECAY_HFRATIO               0x0007
//#define AL_EAXREVERB_DECAY_LFRATIO               0x0008
//#define AL_EAXREVERB_REFLECTIONS_GAIN            0x0009
//#define AL_EAXREVERB_REFLECTIONS_DELAY           0x000A
//#define AL_EAXREVERB_REFLECTIONS_PAN             0x000B
//#define AL_EAXREVERB_LATE_REVERB_GAIN            0x000C
//#define AL_EAXREVERB_LATE_REVERB_DELAY           0x000D
//#define AL_EAXREVERB_LATE_REVERB_PAN             0x000E
//#define AL_EAXREVERB_ECHO_TIME                   0x000F
//#define AL_EAXREVERB_ECHO_DEPTH                  0x0010
//#define AL_EAXREVERB_MODULATION_TIME             0x0011
//#define AL_EAXREVERB_MODULATION_DEPTH            0x0012
//#define AL_EAXREVERB_AIR_ABSORPTION_GAINHF       0x0013
//#define AL_EAXREVERB_HFREFERENCE                 0x0014
//#define AL_EAXREVERB_LFREFERENCE                 0x0015
//#define AL_EAXREVERB_ROOM_ROLLOFF_FACTOR         0x0016
//#define AL_EAXREVERB_DECAY_HFLIMIT               0x0017
//
///* Chorus effect parameters */
//#define AL_CHORUS_WAVEFORM                       0x0001
//#define AL_CHORUS_PHASE                          0x0002
//#define AL_CHORUS_RATE                           0x0003
//#define AL_CHORUS_DEPTH                          0x0004
//#define AL_CHORUS_FEEDBACK                       0x0005
//#define AL_CHORUS_DELAY                          0x0006
//
///* Distortion effect parameters */
//#define AL_DISTORTION_EDGE                       0x0001
//#define AL_DISTORTION_GAIN                       0x0002
//#define AL_DISTORTION_LOWPASS_CUTOFF             0x0003
//#define AL_DISTORTION_EQCENTER                   0x0004
//#define AL_DISTORTION_EQBANDWIDTH                0x0005
//
///* Echo effect parameters */
//#define AL_ECHO_DELAY                            0x0001
//#define AL_ECHO_LRDELAY                          0x0002
//#define AL_ECHO_DAMPING                          0x0003
//#define AL_ECHO_FEEDBACK                         0x0004
//#define AL_ECHO_SPREAD                           0x0005
//
///* Flanger effect parameters */
//#define AL_FLANGER_WAVEFORM                      0x0001
//#define AL_FLANGER_PHASE                         0x0002
//#define AL_FLANGER_RATE                          0x0003
//#define AL_FLANGER_DEPTH                         0x0004
//#define AL_FLANGER_FEEDBACK                      0x0005
//#define AL_FLANGER_DELAY                         0x0006
//
///* Frequency shifter effect parameters */
//#define AL_FREQUENCY_SHIFTER_FREQUENCY           0x0001
//#define AL_FREQUENCY_SHIFTER_LEFT_DIRECTION      0x0002
//#define AL_FREQUENCY_SHIFTER_RIGHT_DIRECTION     0x0003
//
///* Vocal morpher effect parameters */
//#define AL_VOCAL_MORPHER_PHONEMEA                0x0001
//#define AL_VOCAL_MORPHER_PHONEMEA_COARSE_TUNING  0x0002
//#define AL_VOCAL_MORPHER_PHONEMEB                0x0003
//#define AL_VOCAL_MORPHER_PHONEMEB_COARSE_TUNING  0x0004
//#define AL_VOCAL_MORPHER_WAVEFORM                0x0005
//#define AL_VOCAL_MORPHER_RATE                    0x0006
//
///* Pitchshifter effect parameters */
//#define AL_PITCH_SHIFTER_COARSE_TUNE             0x0001
//#define AL_PITCH_SHIFTER_FINE_TUNE               0x0002
//
///* Ringmodulator effect parameters */
//#define AL_RING_MODULATOR_FREQUENCY              0x0001
//#define AL_RING_MODULATOR_HIGHPASS_CUTOFF        0x0002
//#define AL_RING_MODULATOR_WAVEFORM               0x0003
//
///* Autowah effect parameters */
//#define AL_AUTOWAH_ATTACK_TIME                   0x0001
//#define AL_AUTOWAH_RELEASE_TIME                  0x0002
//#define AL_AUTOWAH_RESONANCE                     0x0003
//#define AL_AUTOWAH_PEAK_GAIN                     0x0004
//
///* Compressor effect parameters */
//#define AL_COMPRESSOR_ONOFF                      0x0001
//
///* Equalizer effect parameters */
//#define AL_EQUALIZER_LOW_GAIN                    0x0001
//#define AL_EQUALIZER_LOW_CUTOFF                  0x0002
//#define AL_EQUALIZER_MID1_GAIN                   0x0003
//#define AL_EQUALIZER_MID1_CENTER                 0x0004
//#define AL_EQUALIZER_MID1_WIDTH                  0x0005
//#define AL_EQUALIZER_MID2_GAIN                   0x0006
//#define AL_EQUALIZER_MID2_CENTER                 0x0007
//#define AL_EQUALIZER_MID2_WIDTH                  0x0008
//#define AL_EQUALIZER_HIGH_GAIN                   0x0009
//#define AL_EQUALIZER_HIGH_CUTOFF                 0x000A
//
///* Effect type */
//#define AL_EFFECT_FIRST_PARAMETER                0x0000
//#define AL_EFFECT_LAST_PARAMETER                 0x8000
    static final int AL_EFFECT_TYPE = 0x8001;
//
///* Effect types, used with the AL_EFFECT_TYPE property */
//#define AL_EFFECT_NULL                           0x0000
    static final int AL_EFFECT_REVERB = 0x0001;
//#define AL_EFFECT_CHORUS                         0x0002
//#define AL_EFFECT_DISTORTION                     0x0003
//#define AL_EFFECT_ECHO                           0x0004
//#define AL_EFFECT_FLANGER                        0x0005
//#define AL_EFFECT_FREQUENCY_SHIFTER              0x0006
//#define AL_EFFECT_VOCAL_MORPHER                  0x0007
//#define AL_EFFECT_PITCH_SHIFTER                  0x0008
//#define AL_EFFECT_RING_MODULATOR                 0x0009
//#define AL_EFFECT_AUTOWAH                        0x000A
//#define AL_EFFECT_COMPRESSOR                     0x000B
//#define AL_EFFECT_EQUALIZER                      0x000C
//#define AL_EFFECT_EAXREVERB                      0x8000
//
///* Auxiliary Effect Slot properties. */
    static final int AL_EFFECTSLOT_EFFECT = 0x0001;
//#define AL_EFFECTSLOT_GAIN                       0x0002
//#define AL_EFFECTSLOT_AUXILIARY_SEND_AUTO        0x0003
//
///* NULL Auxiliary Slot ID to disable a source send. */
//#define AL_EFFECTSLOT_NULL                       0x0000
//
//
///* Filter properties. */
//
///* Lowpass filter parameters */
    static final int AL_LOWPASS_GAIN = 0x0001;
    static final int AL_LOWPASS_GAINHF = 0x0002;
//
///* Highpass filter parameters */
//#define AL_HIGHPASS_GAIN                         0x0001
//#define AL_HIGHPASS_GAINLF                       0x0002
//
///* Bandpass filter parameters */
//#define AL_BANDPASS_GAIN                         0x0001
//#define AL_BANDPASS_GAINLF                       0x0002
//#define AL_BANDPASS_GAINHF                       0x0003
//
///* Filter type */
//#define AL_FILTER_FIRST_PARAMETER                0x0000
//#define AL_FILTER_LAST_PARAMETER                 0x8000
    static final int AL_FILTER_TYPE = 0x8001;
//
///* Filter types, used with the AL_FILTER_TYPE property */
    static final int AL_FILTER_NULL = 0x0000;
    static final int AL_FILTER_LOWPASS = 0x0001;
    static final int AL_FILTER_HIGHPASS = 0x0002;
//#define AL_FILTER_BANDPASS                       0x0003
//
///* Filter ranges and defaults. */
//
///* Lowpass filter */
//#define AL_LOWPASS_MIN_GAIN                      (0.0f)
//#define AL_LOWPASS_MAX_GAIN                      (1.0f)
//#define AL_LOWPASS_DEFAULT_GAIN                  (1.0f)
//
//#define AL_LOWPASS_MIN_GAINHF                    (0.0f)
//#define AL_LOWPASS_MAX_GAINHF                    (1.0f)
//#define AL_LOWPASS_DEFAULT_GAINHF                (1.0f)
//
///* Highpass filter */
//#define AL_HIGHPASS_MIN_GAIN                     (0.0f)
//#define AL_HIGHPASS_MAX_GAIN                     (1.0f)
//#define AL_HIGHPASS_DEFAULT_GAIN                 (1.0f)
//
//#define AL_HIGHPASS_MIN_GAINLF                   (0.0f)
//#define AL_HIGHPASS_MAX_GAINLF                   (1.0f)
//#define AL_HIGHPASS_DEFAULT_GAINLF               (1.0f)
//
///* Bandpass filter */
//#define AL_BANDPASS_MIN_GAIN                     (0.0f)
//#define AL_BANDPASS_MAX_GAIN                     (1.0f)
//#define AL_BANDPASS_DEFAULT_GAIN                 (1.0f)
//
//#define AL_BANDPASS_MIN_GAINHF                   (0.0f)
//#define AL_BANDPASS_MAX_GAINHF                   (1.0f)
//#define AL_BANDPASS_DEFAULT_GAINHF               (1.0f)
//
//#define AL_BANDPASS_MIN_GAINLF                   (0.0f)
//#define AL_BANDPASS_MAX_GAINLF                   (1.0f)
//#define AL_BANDPASS_DEFAULT_GAINLF               (1.0f)
//
//
///* Effect parameter ranges and defaults. */
//
///* Standard reverb effect */
//#define AL_REVERB_MIN_DENSITY                    (0.0f)
//#define AL_REVERB_MAX_DENSITY                    (1.0f)
//#define AL_REVERB_DEFAULT_DENSITY                (1.0f)
//
//#define AL_REVERB_MIN_DIFFUSION                  (0.0f)
//#define AL_REVERB_MAX_DIFFUSION                  (1.0f)
//#define AL_REVERB_DEFAULT_DIFFUSION              (1.0f)
//
//#define AL_REVERB_MIN_GAIN                       (0.0f)
//#define AL_REVERB_MAX_GAIN                       (1.0f)
//#define AL_REVERB_DEFAULT_GAIN                   (0.32f)
//
//#define AL_REVERB_MIN_GAINHF                     (0.0f)
//#define AL_REVERB_MAX_GAINHF                     (1.0f)
//#define AL_REVERB_DEFAULT_GAINHF                 (0.89f)
//
//#define AL_REVERB_MIN_DECAY_TIME                 (0.1f)
//#define AL_REVERB_MAX_DECAY_TIME                 (20.0f)
//#define AL_REVERB_DEFAULT_DECAY_TIME             (1.49f)
//
//#define AL_REVERB_MIN_DECAY_HFRATIO              (0.1f)
//#define AL_REVERB_MAX_DECAY_HFRATIO              (2.0f)
//#define AL_REVERB_DEFAULT_DECAY_HFRATIO          (0.83f)
//
//#define AL_REVERB_MIN_REFLECTIONS_GAIN           (0.0f)
//#define AL_REVERB_MAX_REFLECTIONS_GAIN           (3.16f)
//#define AL_REVERB_DEFAULT_REFLECTIONS_GAIN       (0.05f)
//
//#define AL_REVERB_MIN_REFLECTIONS_DELAY          (0.0f)
//#define AL_REVERB_MAX_REFLECTIONS_DELAY          (0.3f)
//#define AL_REVERB_DEFAULT_REFLECTIONS_DELAY      (0.007f)
//
//#define AL_REVERB_MIN_LATE_REVERB_GAIN           (0.0f)
//#define AL_REVERB_MAX_LATE_REVERB_GAIN           (10.0f)
//#define AL_REVERB_DEFAULT_LATE_REVERB_GAIN       (1.26f)
//
//#define AL_REVERB_MIN_LATE_REVERB_DELAY          (0.0f)
//#define AL_REVERB_MAX_LATE_REVERB_DELAY          (0.1f)
//#define AL_REVERB_DEFAULT_LATE_REVERB_DELAY      (0.011f)
//
//#define AL_REVERB_MIN_AIR_ABSORPTION_GAINHF      (0.892f)
//#define AL_REVERB_MAX_AIR_ABSORPTION_GAINHF      (1.0f)
//#define AL_REVERB_DEFAULT_AIR_ABSORPTION_GAINHF  (0.994f)
//
//#define AL_REVERB_MIN_ROOM_ROLLOFF_FACTOR        (0.0f)
//#define AL_REVERB_MAX_ROOM_ROLLOFF_FACTOR        (10.0f)
//#define AL_REVERB_DEFAULT_ROOM_ROLLOFF_FACTOR    (0.0f)
//
//#define AL_REVERB_MIN_DECAY_HFLIMIT              AL_FALSE
//#define AL_REVERB_MAX_DECAY_HFLIMIT              AL_TRUE
//#define AL_REVERB_DEFAULT_DECAY_HFLIMIT          AL_TRUE
//
///* EAX reverb effect */
//#define AL_EAXREVERB_MIN_DENSITY                 (0.0f)
//#define AL_EAXREVERB_MAX_DENSITY                 (1.0f)
//#define AL_EAXREVERB_DEFAULT_DENSITY             (1.0f)
//
//#define AL_EAXREVERB_MIN_DIFFUSION               (0.0f)
//#define AL_EAXREVERB_MAX_DIFFUSION               (1.0f)
//#define AL_EAXREVERB_DEFAULT_DIFFUSION           (1.0f)
//
//#define AL_EAXREVERB_MIN_GAIN                    (0.0f)
//#define AL_EAXREVERB_MAX_GAIN                    (1.0f)
//#define AL_EAXREVERB_DEFAULT_GAIN                (0.32f)
//
//#define AL_EAXREVERB_MIN_GAINHF                  (0.0f)
//#define AL_EAXREVERB_MAX_GAINHF                  (1.0f)
//#define AL_EAXREVERB_DEFAULT_GAINHF              (0.89f)
//
//#define AL_EAXREVERB_MIN_GAINLF                  (0.0f)
//#define AL_EAXREVERB_MAX_GAINLF                  (1.0f)
//#define AL_EAXREVERB_DEFAULT_GAINLF              (1.0f)
//
//#define AL_EAXREVERB_MIN_DECAY_TIME              (0.1f)
//#define AL_EAXREVERB_MAX_DECAY_TIME              (20.0f)
//#define AL_EAXREVERB_DEFAULT_DECAY_TIME          (1.49f)
//
//#define AL_EAXREVERB_MIN_DECAY_HFRATIO           (0.1f)
//#define AL_EAXREVERB_MAX_DECAY_HFRATIO           (2.0f)
//#define AL_EAXREVERB_DEFAULT_DECAY_HFRATIO       (0.83f)
//
//#define AL_EAXREVERB_MIN_DECAY_LFRATIO           (0.1f)
//#define AL_EAXREVERB_MAX_DECAY_LFRATIO           (2.0f)
//#define AL_EAXREVERB_DEFAULT_DECAY_LFRATIO       (1.0f)
//
//#define AL_EAXREVERB_MIN_REFLECTIONS_GAIN        (0.0f)
//#define AL_EAXREVERB_MAX_REFLECTIONS_GAIN        (3.16f)
//#define AL_EAXREVERB_DEFAULT_REFLECTIONS_GAIN    (0.05f)
//
//#define AL_EAXREVERB_MIN_REFLECTIONS_DELAY       (0.0f)
//#define AL_EAXREVERB_MAX_REFLECTIONS_DELAY       (0.3f)
//#define AL_EAXREVERB_DEFAULT_REFLECTIONS_DELAY   (0.007f)
//
//#define AL_EAXREVERB_DEFAULT_REFLECTIONS_PAN_XYZ (0.0f)
//
//#define AL_EAXREVERB_MIN_LATE_REVERB_GAIN        (0.0f)
//#define AL_EAXREVERB_MAX_LATE_REVERB_GAIN        (10.0f)
//#define AL_EAXREVERB_DEFAULT_LATE_REVERB_GAIN    (1.26f)
//
//#define AL_EAXREVERB_MIN_LATE_REVERB_DELAY       (0.0f)
//#define AL_EAXREVERB_MAX_LATE_REVERB_DELAY       (0.1f)
//#define AL_EAXREVERB_DEFAULT_LATE_REVERB_DELAY   (0.011f)
//
//#define AL_EAXREVERB_DEFAULT_LATE_REVERB_PAN_XYZ (0.0f)
//
//#define AL_EAXREVERB_MIN_ECHO_TIME               (0.075f)
//#define AL_EAXREVERB_MAX_ECHO_TIME               (0.25f)
//#define AL_EAXREVERB_DEFAULT_ECHO_TIME           (0.25f)
//
//#define AL_EAXREVERB_MIN_ECHO_DEPTH              (0.0f)
//#define AL_EAXREVERB_MAX_ECHO_DEPTH              (1.0f)
//#define AL_EAXREVERB_DEFAULT_ECHO_DEPTH          (0.0f)
//
//#define AL_EAXREVERB_MIN_MODULATION_TIME         (0.04f)
//#define AL_EAXREVERB_MAX_MODULATION_TIME         (4.0f)
//#define AL_EAXREVERB_DEFAULT_MODULATION_TIME     (0.25f)
//
//#define AL_EAXREVERB_MIN_MODULATION_DEPTH        (0.0f)
//#define AL_EAXREVERB_MAX_MODULATION_DEPTH        (1.0f)
//#define AL_EAXREVERB_DEFAULT_MODULATION_DEPTH    (0.0f)
//
//#define AL_EAXREVERB_MIN_AIR_ABSORPTION_GAINHF   (0.892f)
//#define AL_EAXREVERB_MAX_AIR_ABSORPTION_GAINHF   (1.0f)
//#define AL_EAXREVERB_DEFAULT_AIR_ABSORPTION_GAINHF (0.994f)
//
//#define AL_EAXREVERB_MIN_HFREFERENCE             (1000.0f)
//#define AL_EAXREVERB_MAX_HFREFERENCE             (20000.0f)
//#define AL_EAXREVERB_DEFAULT_HFREFERENCE         (5000.0f)
//
//#define AL_EAXREVERB_MIN_LFREFERENCE             (20.0f)
//#define AL_EAXREVERB_MAX_LFREFERENCE             (1000.0f)
//#define AL_EAXREVERB_DEFAULT_LFREFERENCE         (250.0f)
//
//#define AL_EAXREVERB_MIN_ROOM_ROLLOFF_FACTOR     (0.0f)
//#define AL_EAXREVERB_MAX_ROOM_ROLLOFF_FACTOR     (10.0f)
//#define AL_EAXREVERB_DEFAULT_ROOM_ROLLOFF_FACTOR (0.0f)
//
//#define AL_EAXREVERB_MIN_DECAY_HFLIMIT           AL_FALSE
//#define AL_EAXREVERB_MAX_DECAY_HFLIMIT           AL_TRUE
//#define AL_EAXREVERB_DEFAULT_DECAY_HFLIMIT       AL_TRUE
//
///* Chorus effect */
//#define AL_CHORUS_WAVEFORM_SINUSOID              (0)
//#define AL_CHORUS_WAVEFORM_TRIANGLE              (1)
//
//#define AL_CHORUS_MIN_WAVEFORM                   (0)
//#define AL_CHORUS_MAX_WAVEFORM                   (1)
//#define AL_CHORUS_DEFAULT_WAVEFORM               (1)
//
//#define AL_CHORUS_MIN_PHASE                      (-180)
//#define AL_CHORUS_MAX_PHASE                      (180)
//#define AL_CHORUS_DEFAULT_PHASE                  (90)
//
//#define AL_CHORUS_MIN_RATE                       (0.0f)
//#define AL_CHORUS_MAX_RATE                       (10.0f)
//#define AL_CHORUS_DEFAULT_RATE                   (1.1f)
//
//#define AL_CHORUS_MIN_DEPTH                      (0.0f)
//#define AL_CHORUS_MAX_DEPTH                      (1.0f)
//#define AL_CHORUS_DEFAULT_DEPTH                  (0.1f)
//
//#define AL_CHORUS_MIN_FEEDBACK                   (-1.0f)
//#define AL_CHORUS_MAX_FEEDBACK                   (1.0f)
//#define AL_CHORUS_DEFAULT_FEEDBACK               (0.25f)
//
//#define AL_CHORUS_MIN_DELAY                      (0.0f)
//#define AL_CHORUS_MAX_DELAY                      (0.016f)
//#define AL_CHORUS_DEFAULT_DELAY                  (0.016f)
//
///* Distortion effect */
//#define AL_DISTORTION_MIN_EDGE                   (0.0f)
//#define AL_DISTORTION_MAX_EDGE                   (1.0f)
//#define AL_DISTORTION_DEFAULT_EDGE               (0.2f)
//
//#define AL_DISTORTION_MIN_GAIN                   (0.01f)
//#define AL_DISTORTION_MAX_GAIN                   (1.0f)
//#define AL_DISTORTION_DEFAULT_GAIN               (0.05f)
//
//#define AL_DISTORTION_MIN_LOWPASS_CUTOFF         (80.0f)
//#define AL_DISTORTION_MAX_LOWPASS_CUTOFF         (24000.0f)
//#define AL_DISTORTION_DEFAULT_LOWPASS_CUTOFF     (8000.0f)
//
//#define AL_DISTORTION_MIN_EQCENTER               (80.0f)
//#define AL_DISTORTION_MAX_EQCENTER               (24000.0f)
//#define AL_DISTORTION_DEFAULT_EQCENTER           (3600.0f)
//
//#define AL_DISTORTION_MIN_EQBANDWIDTH            (80.0f)
//#define AL_DISTORTION_MAX_EQBANDWIDTH            (24000.0f)
//#define AL_DISTORTION_DEFAULT_EQBANDWIDTH        (3600.0f)
//
///* Echo effect */
//#define AL_ECHO_MIN_DELAY                        (0.0f)
//#define AL_ECHO_MAX_DELAY                        (0.207f)
//#define AL_ECHO_DEFAULT_DELAY                    (0.1f)
//
//#define AL_ECHO_MIN_LRDELAY                      (0.0f)
//#define AL_ECHO_MAX_LRDELAY                      (0.404f)
//#define AL_ECHO_DEFAULT_LRDELAY                  (0.1f)
//
//#define AL_ECHO_MIN_DAMPING                      (0.0f)
//#define AL_ECHO_MAX_DAMPING                      (0.99f)
//#define AL_ECHO_DEFAULT_DAMPING                  (0.5f)
//
//#define AL_ECHO_MIN_FEEDBACK                     (0.0f)
//#define AL_ECHO_MAX_FEEDBACK                     (1.0f)
//#define AL_ECHO_DEFAULT_FEEDBACK                 (0.5f)
//
//#define AL_ECHO_MIN_SPREAD                       (-1.0f)
//#define AL_ECHO_MAX_SPREAD                       (1.0f)
//#define AL_ECHO_DEFAULT_SPREAD                   (-1.0f)
//
///* Flanger effect */
//#define AL_FLANGER_WAVEFORM_SINUSOID             (0)
//#define AL_FLANGER_WAVEFORM_TRIANGLE             (1)
//
//#define AL_FLANGER_MIN_WAVEFORM                  (0)
//#define AL_FLANGER_MAX_WAVEFORM                  (1)
//#define AL_FLANGER_DEFAULT_WAVEFORM              (1)
//
//#define AL_FLANGER_MIN_PHASE                     (-180)
//#define AL_FLANGER_MAX_PHASE                     (180)
//#define AL_FLANGER_DEFAULT_PHASE                 (0)
//
//#define AL_FLANGER_MIN_RATE                      (0.0f)
//#define AL_FLANGER_MAX_RATE                      (10.0f)
//#define AL_FLANGER_DEFAULT_RATE                  (0.27f)
//
//#define AL_FLANGER_MIN_DEPTH                     (0.0f)
//#define AL_FLANGER_MAX_DEPTH                     (1.0f)
//#define AL_FLANGER_DEFAULT_DEPTH                 (1.0f)
//
//#define AL_FLANGER_MIN_FEEDBACK                  (-1.0f)
//#define AL_FLANGER_MAX_FEEDBACK                  (1.0f)
//#define AL_FLANGER_DEFAULT_FEEDBACK              (-0.5f)
//
//#define AL_FLANGER_MIN_DELAY                     (0.0f)
//#define AL_FLANGER_MAX_DELAY                     (0.004f)
//#define AL_FLANGER_DEFAULT_DELAY                 (0.002f)
//
///* Frequency shifter effect */
//#define AL_FREQUENCY_SHIFTER_MIN_FREQUENCY       (0.0f)
//#define AL_FREQUENCY_SHIFTER_MAX_FREQUENCY       (24000.0f)
//#define AL_FREQUENCY_SHIFTER_DEFAULT_FREQUENCY   (0.0f)
//
//#define AL_FREQUENCY_SHIFTER_MIN_LEFT_DIRECTION  (0)
//#define AL_FREQUENCY_SHIFTER_MAX_LEFT_DIRECTION  (2)
//#define AL_FREQUENCY_SHIFTER_DEFAULT_LEFT_DIRECTION (0)
//
//#define AL_FREQUENCY_SHIFTER_DIRECTION_DOWN      (0)
//#define AL_FREQUENCY_SHIFTER_DIRECTION_UP        (1)
//#define AL_FREQUENCY_SHIFTER_DIRECTION_OFF       (2)
//
//#define AL_FREQUENCY_SHIFTER_MIN_RIGHT_DIRECTION (0)
//#define AL_FREQUENCY_SHIFTER_MAX_RIGHT_DIRECTION (2)
//#define AL_FREQUENCY_SHIFTER_DEFAULT_RIGHT_DIRECTION (0)
//
///* Vocal morpher effect */
//#define AL_VOCAL_MORPHER_MIN_PHONEMEA            (0)
//#define AL_VOCAL_MORPHER_MAX_PHONEMEA            (29)
//#define AL_VOCAL_MORPHER_DEFAULT_PHONEMEA        (0)
//
//#define AL_VOCAL_MORPHER_MIN_PHONEMEA_COARSE_TUNING (-24)
//#define AL_VOCAL_MORPHER_MAX_PHONEMEA_COARSE_TUNING (24)
//#define AL_VOCAL_MORPHER_DEFAULT_PHONEMEA_COARSE_TUNING (0)
//
//#define AL_VOCAL_MORPHER_MIN_PHONEMEB            (0)
//#define AL_VOCAL_MORPHER_MAX_PHONEMEB            (29)
//#define AL_VOCAL_MORPHER_DEFAULT_PHONEMEB        (10)
//
//#define AL_VOCAL_MORPHER_MIN_PHONEMEB_COARSE_TUNING (-24)
//#define AL_VOCAL_MORPHER_MAX_PHONEMEB_COARSE_TUNING (24)
//#define AL_VOCAL_MORPHER_DEFAULT_PHONEMEB_COARSE_TUNING (0)
//
//#define AL_VOCAL_MORPHER_PHONEME_A               (0)
//#define AL_VOCAL_MORPHER_PHONEME_E               (1)
//#define AL_VOCAL_MORPHER_PHONEME_I               (2)
//#define AL_VOCAL_MORPHER_PHONEME_O               (3)
//#define AL_VOCAL_MORPHER_PHONEME_U               (4)
//#define AL_VOCAL_MORPHER_PHONEME_AA              (5)
//#define AL_VOCAL_MORPHER_PHONEME_AE              (6)
//#define AL_VOCAL_MORPHER_PHONEME_AH              (7)
//#define AL_VOCAL_MORPHER_PHONEME_AO              (8)
//#define AL_VOCAL_MORPHER_PHONEME_EH              (9)
//#define AL_VOCAL_MORPHER_PHONEME_ER              (10)
//#define AL_VOCAL_MORPHER_PHONEME_IH              (11)
//#define AL_VOCAL_MORPHER_PHONEME_IY              (12)
//#define AL_VOCAL_MORPHER_PHONEME_UH              (13)
//#define AL_VOCAL_MORPHER_PHONEME_UW              (14)
//#define AL_VOCAL_MORPHER_PHONEME_B               (15)
//#define AL_VOCAL_MORPHER_PHONEME_D               (16)
//#define AL_VOCAL_MORPHER_PHONEME_F               (17)
//#define AL_VOCAL_MORPHER_PHONEME_G               (18)
//#define AL_VOCAL_MORPHER_PHONEME_J               (19)
//#define AL_VOCAL_MORPHER_PHONEME_K               (20)
//#define AL_VOCAL_MORPHER_PHONEME_L               (21)
//#define AL_VOCAL_MORPHER_PHONEME_M               (22)
//#define AL_VOCAL_MORPHER_PHONEME_N               (23)
//#define AL_VOCAL_MORPHER_PHONEME_P               (24)
//#define AL_VOCAL_MORPHER_PHONEME_R               (25)
//#define AL_VOCAL_MORPHER_PHONEME_S               (26)
//#define AL_VOCAL_MORPHER_PHONEME_T               (27)
//#define AL_VOCAL_MORPHER_PHONEME_V               (28)
//#define AL_VOCAL_MORPHER_PHONEME_Z               (29)
//
//#define AL_VOCAL_MORPHER_WAVEFORM_SINUSOID       (0)
//#define AL_VOCAL_MORPHER_WAVEFORM_TRIANGLE       (1)
//#define AL_VOCAL_MORPHER_WAVEFORM_SAWTOOTH       (2)
//
//#define AL_VOCAL_MORPHER_MIN_WAVEFORM            (0)
//#define AL_VOCAL_MORPHER_MAX_WAVEFORM            (2)
//#define AL_VOCAL_MORPHER_DEFAULT_WAVEFORM        (0)
//
//#define AL_VOCAL_MORPHER_MIN_RATE                (0.0f)
//#define AL_VOCAL_MORPHER_MAX_RATE                (10.0f)
//#define AL_VOCAL_MORPHER_DEFAULT_RATE            (1.41f)
//
///* Pitch shifter effect */
//#define AL_PITCH_SHIFTER_MIN_COARSE_TUNE         (-12)
//#define AL_PITCH_SHIFTER_MAX_COARSE_TUNE         (12)
//#define AL_PITCH_SHIFTER_DEFAULT_COARSE_TUNE     (12)
//
//#define AL_PITCH_SHIFTER_MIN_FINE_TUNE           (-50)
//#define AL_PITCH_SHIFTER_MAX_FINE_TUNE           (50)
//#define AL_PITCH_SHIFTER_DEFAULT_FINE_TUNE       (0)
//
///* Ring modulator effect */
//#define AL_RING_MODULATOR_MIN_FREQUENCY          (0.0f)
//#define AL_RING_MODULATOR_MAX_FREQUENCY          (8000.0f)
//#define AL_RING_MODULATOR_DEFAULT_FREQUENCY      (440.0f)
//
//#define AL_RING_MODULATOR_MIN_HIGHPASS_CUTOFF    (0.0f)
//#define AL_RING_MODULATOR_MAX_HIGHPASS_CUTOFF    (24000.0f)
//#define AL_RING_MODULATOR_DEFAULT_HIGHPASS_CUTOFF (800.0f)
//
//#define AL_RING_MODULATOR_SINUSOID               (0)
//#define AL_RING_MODULATOR_SAWTOOTH               (1)
//#define AL_RING_MODULATOR_SQUARE                 (2)
//
//#define AL_RING_MODULATOR_MIN_WAVEFORM           (0)
//#define AL_RING_MODULATOR_MAX_WAVEFORM           (2)
//#define AL_RING_MODULATOR_DEFAULT_WAVEFORM       (0)
//
///* Autowah effect */
//#define AL_AUTOWAH_MIN_ATTACK_TIME               (0.0001f)
//#define AL_AUTOWAH_MAX_ATTACK_TIME               (1.0f)
//#define AL_AUTOWAH_DEFAULT_ATTACK_TIME           (0.06f)
//
//#define AL_AUTOWAH_MIN_RELEASE_TIME              (0.0001f)
//#define AL_AUTOWAH_MAX_RELEASE_TIME              (1.0f)
//#define AL_AUTOWAH_DEFAULT_RELEASE_TIME          (0.06f)
//
//#define AL_AUTOWAH_MIN_RESONANCE                 (2.0f)
//#define AL_AUTOWAH_MAX_RESONANCE                 (1000.0f)
//#define AL_AUTOWAH_DEFAULT_RESONANCE             (1000.0f)
//
//#define AL_AUTOWAH_MIN_PEAK_GAIN                 (0.00003f)
//#define AL_AUTOWAH_MAX_PEAK_GAIN                 (31621.0f)
//#define AL_AUTOWAH_DEFAULT_PEAK_GAIN             (11.22f)
//
///* Compressor effect */
//#define AL_COMPRESSOR_MIN_ONOFF                  (0)
//#define AL_COMPRESSOR_MAX_ONOFF                  (1)
//#define AL_COMPRESSOR_DEFAULT_ONOFF              (1)
//
///* Equalizer effect */
//#define AL_EQUALIZER_MIN_LOW_GAIN                (0.126f)
//#define AL_EQUALIZER_MAX_LOW_GAIN                (7.943f)
//#define AL_EQUALIZER_DEFAULT_LOW_GAIN            (1.0f)
//
//#define AL_EQUALIZER_MIN_LOW_CUTOFF              (50.0f)
//#define AL_EQUALIZER_MAX_LOW_CUTOFF              (800.0f)
//#define AL_EQUALIZER_DEFAULT_LOW_CUTOFF          (200.0f)
//
//#define AL_EQUALIZER_MIN_MID1_GAIN               (0.126f)
//#define AL_EQUALIZER_MAX_MID1_GAIN               (7.943f)
//#define AL_EQUALIZER_DEFAULT_MID1_GAIN           (1.0f)
//
//#define AL_EQUALIZER_MIN_MID1_CENTER             (200.0f)
//#define AL_EQUALIZER_MAX_MID1_CENTER             (3000.0f)
//#define AL_EQUALIZER_DEFAULT_MID1_CENTER         (500.0f)
//
//#define AL_EQUALIZER_MIN_MID1_WIDTH              (0.01f)
//#define AL_EQUALIZER_MAX_MID1_WIDTH              (1.0f)
//#define AL_EQUALIZER_DEFAULT_MID1_WIDTH          (1.0f)
//
//#define AL_EQUALIZER_MIN_MID2_GAIN               (0.126f)
//#define AL_EQUALIZER_MAX_MID2_GAIN               (7.943f)
//#define AL_EQUALIZER_DEFAULT_MID2_GAIN           (1.0f)
//
//#define AL_EQUALIZER_MIN_MID2_CENTER             (1000.0f)
//#define AL_EQUALIZER_MAX_MID2_CENTER             (8000.0f)
//#define AL_EQUALIZER_DEFAULT_MID2_CENTER         (3000.0f)
//
//#define AL_EQUALIZER_MIN_MID2_WIDTH              (0.01f)
//#define AL_EQUALIZER_MAX_MID2_WIDTH              (1.0f)
//#define AL_EQUALIZER_DEFAULT_MID2_WIDTH          (1.0f)
//
//#define AL_EQUALIZER_MIN_HIGH_GAIN               (0.126f)
//#define AL_EQUALIZER_MAX_HIGH_GAIN               (7.943f)
//#define AL_EQUALIZER_DEFAULT_HIGH_GAIN           (1.0f)
//
//#define AL_EQUALIZER_MIN_HIGH_CUTOFF             (4000.0f)
//#define AL_EQUALIZER_MAX_HIGH_CUTOFF             (16000.0f)
//#define AL_EQUALIZER_DEFAULT_HIGH_CUTOFF         (6000.0f)
//
//
///* Source parameter value ranges and defaults. */
//#define AL_MIN_AIR_ABSORPTION_FACTOR             (0.0f)
//#define AL_MAX_AIR_ABSORPTION_FACTOR             (10.0f)
//#define AL_DEFAULT_AIR_ABSORPTION_FACTOR         (0.0f)
//
//#define AL_MIN_ROOM_ROLLOFF_FACTOR               (0.0f)
//#define AL_MAX_ROOM_ROLLOFF_FACTOR               (10.0f)
//#define AL_DEFAULT_ROOM_ROLLOFF_FACTOR           (0.0f)
//
//#define AL_MIN_CONE_OUTER_GAINHF                 (0.0f)
//#define AL_MAX_CONE_OUTER_GAINHF                 (1.0f)
//#define AL_DEFAULT_CONE_OUTER_GAINHF             (1.0f)
//
//#define AL_MIN_DIRECT_FILTER_GAINHF_AUTO         AL_FALSE
//#define AL_MAX_DIRECT_FILTER_GAINHF_AUTO         AL_TRUE
//#define AL_DEFAULT_DIRECT_FILTER_GAINHF_AUTO     AL_TRUE
//
//#define AL_MIN_AUXILIARY_SEND_FILTER_GAIN_AUTO   AL_FALSE
//#define AL_MAX_AUXILIARY_SEND_FILTER_GAIN_AUTO   AL_TRUE
//#define AL_DEFAULT_AUXILIARY_SEND_FILTER_GAIN_AUTO AL_TRUE
//
//#define AL_MIN_AUXILIARY_SEND_FILTER_GAINHF_AUTO AL_FALSE
//#define AL_MAX_AUXILIARY_SEND_FILTER_GAINHF_AUTO AL_TRUE
//#define AL_DEFAULT_AUXILIARY_SEND_FILTER_GAINHF_AUTO AL_TRUE
//
//
///* Listener parameter value ranges and defaults. */
//#define AL_MIN_METERS_PER_UNIT                   FLT_MIN
//#define AL_MAX_METERS_PER_UNIT                   FLT_MAX
//#define AL_DEFAULT_METERS_PER_UNIT               (1.0f)


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

