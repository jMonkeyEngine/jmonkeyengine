TARGET_PLATFORM := android-9

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE     := openalsoftjme

LOCAL_C_INCLUDES += $(LOCAL_PATH) $(LOCAL_PATH)/include \
		    $(LOCAL_PATH)/OpenAL32/Include $(LOCAL_PATH)/Alc

LOCAL_CFLAGS     := -std=c99 -ffast-math -DAL_BUILD_LIBRARY -DAL_ALEXT_PROTOTYPES
LOCAL_LDLIBS     := -lOpenSLES -llog -Wl,-s

LOCAL_SRC_FILES  :=   Alc/backends/opensl.c \
                      Alc/backends/loopback.c \
                      Alc/backends/wave.c \
                      Alc/backends/base.c \
                      Alc/backends/null.c \
                      Alc/ALc.c \
                      Alc/helpers.c \
                      Alc/bs2b.c \
                      Alc/alcRing.c \
                      Alc/effects/chorus.c \
                      Alc/effects/flanger.c \
                      Alc/effects/dedicated.c \
                      Alc/effects/reverb.c \
                      Alc/effects/distortion.c \
                      Alc/effects/autowah.c \
                      Alc/effects/equalizer.c \
                      Alc/effects/modulator.c \
                      Alc/effects/echo.c \
                      Alc/effects/compressor.c \
                      Alc/effects/null.c \
                      Alc/alcConfig.c \
                      Alc/ALu.c \
                      Alc/mixer_c.c \
                      Alc/panning.c \
                      Alc/hrtf.c \
                      Alc/mixer.c \
                      Alc/midi/soft.c \
                      Alc/midi/sf2load.c \
                      Alc/midi/dummy.c \
                      Alc/midi/fluidsynth.c \
                      Alc/midi/base.c \
                      common/uintmap.c \
                      common/atomic.c \
                      common/threads.c \
                      common/rwlock.c \
                      OpenAL32/alBuffer.c \
                      OpenAL32/alPreset.c \
                      OpenAL32/alListener.c \
                      OpenAL32/alEffect.c \
                      OpenAL32/alExtension.c \
                      OpenAL32/alThunk.c \
                      OpenAL32/alMidi.c \
                      OpenAL32/alSoundfont.c \
                      OpenAL32/alFontsound.c \
                      OpenAL32/alAuxEffectSlot.c \
                      OpenAL32/alError.c \
                      OpenAL32/alFilter.c \
                      OpenAL32/alSource.c \
                      OpenAL32/alState.c \
                      OpenAL32/sample_cvt.c \
		      com_jme3_audio_android_AndroidAL.c \
		      com_jme3_audio_android_AndroidALC.c \
		      com_jme3_audio_android_AndroidEFX.c 

include $(BUILD_SHARED_LIBRARY)

