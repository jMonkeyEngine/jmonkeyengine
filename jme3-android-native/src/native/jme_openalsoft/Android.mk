TARGET_PLATFORM := android-19

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE     := openalsoftjme

LOCAL_C_INCLUDES += $(LOCAL_PATH) $(LOCAL_PATH)/include \
		    $(LOCAL_PATH)/OpenAL32/Include $(LOCAL_PATH)/Alc  $(LOCAL_PATH)/common

LOCAL_CFLAGS     := -std=c99 -ffast-math -DAL_BUILD_LIBRARY -DAL_ALEXT_PROTOTYPES -fcommon -O0
LOCAL_LDLIBS     := -lOpenSLES -llog -Wl,-s

LOCAL_SRC_FILES  :=   Alc/ALc.c \
                      Alc/alconfig.c \
                      Alc/ALu.c \
                      Alc/ambdec.c \
                      Alc/backends/base.c \
                      Alc/backends/loopback.c \
                      Alc/backends/null.c \
                      Alc/backends/opensl.c \
                      Alc/backends/wave.c \
                      Alc/bformatdec.c \
                      Alc/bs2b.c \
                      Alc/converter.c \
                      Alc/effects/autowah.c \
                      Alc/effects/chorus.c \
                      Alc/effects/compressor.c \
                      Alc/effects/dedicated.c \
                      Alc/effects/distortion.c \
                      Alc/effects/echo.c \
                      Alc/effects/equalizer.c \
                      Alc/effects/fshifter.c \
                      Alc/effects/modulator.c \
                      Alc/effects/null.c \
                      Alc/effects/pshifter.c \
                      Alc/effects/reverb.c \
                      Alc/filters/filter.c \
                      Alc/filters/nfc.c \
                      Alc/filters/splitter.c \
                      Alc/helpers.c \
                      Alc/hrtf.c \
                      Alc/mastering.c \
                      Alc/mixer/mixer_c.c \
                      Alc/mixvoice.c \
                      Alc/panning.c \
                      Alc/ringbuffer.c \
                      Alc/uhjfilter.c \
                      common/alcomplex.c \
                      common/almalloc.c \
                      common/atomic.c \
                      common/rwlock.c \
                      common/threads.c \
                      common/uintmap.c \
                      OpenAL32/alAuxEffectSlot.c \
                      OpenAL32/alBuffer.c \
                      OpenAL32/alEffect.c \
                      OpenAL32/alError.c \
                      OpenAL32/alExtension.c \
                      OpenAL32/alFilter.c \
                      OpenAL32/alListener.c \
                      OpenAL32/alSource.c \
                      OpenAL32/alState.c \
                      OpenAL32/event.c \
                      OpenAL32/sample_cvt.c \
                      com_jme3_audio_android_AndroidAL.c \
                      com_jme3_audio_android_AndroidALC.c \
                      com_jme3_audio_android_AndroidEFX.c

include $(BUILD_SHARED_LIBRARY)

#                      Alc/mixer/hrtf_inc.c \

