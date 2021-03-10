TARGET_PLATFORM := android-19

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE     := openalsoftjme

LOCAL_C_INCLUDES += $(LOCAL_PATH) $(LOCAL_PATH)/include \
		    $(LOCAL_PATH)/alc  $(LOCAL_PATH)/common

LOCAL_CPP_FEATURES += exceptions

LOCAL_CFLAGS     := -ffast-math -DAL_BUILD_LIBRARY -DAL_ALEXT_PROTOTYPES -fcommon -O0 -DRESTRICT=""
LOCAL_LDLIBS     := -lOpenSLES -llog -Wl,-s

LOCAL_SRC_FILES  :=   al/auxeffectslot.cpp \
                      al/buffer.cpp \
                      al/effect.cpp \
                      al/effects/autowah.cpp \
                      al/effects/chorus.cpp \
                      al/effects/compressor.cpp \
                      al/effects/convolution.cpp \
                      al/effects/dedicated.cpp \
                      al/effects/distortion.cpp \
                      al/effects/echo.cpp \
                      al/effects/equalizer.cpp \
                      al/effects/fshifter.cpp \
                      al/effects/modulator.cpp \
                      al/effects/null.cpp \
                      al/effects/pshifter.cpp \
                      al/effects/reverb.cpp \
                      al/effects/vmorpher.cpp \
                      al/error.cpp \
                      al/event.cpp \
                      al/extension.cpp \
                      al/filter.cpp \
                      al/listener.cpp \
                      al/source.cpp \
                      al/state.cpp \
                      alc/alc.cpp \
                      alc/alconfig.cpp \
                      alc/alu.cpp \
                      alc/backends/base.cpp \
                      alc/backends/loopback.cpp \
                      alc/backends/null.cpp \
                      alc/backends/opensl.cpp \
                      alc/backends/wave.cpp \
                      alc/bformatdec.cpp \
                      alc/buffer_storage.cpp \
                      alc/converter.cpp \
                      alc/effects/autowah.cpp \
                      alc/effects/chorus.cpp \
                      alc/effects/compressor.cpp \
                      alc/effects/convolution.cpp \
                      alc/effects/dedicated.cpp \
                      alc/effects/distortion.cpp \
                      alc/effects/echo.cpp \
                      alc/effects/equalizer.cpp \
                      alc/effects/fshifter.cpp \
                      alc/effects/modulator.cpp \
                      alc/effects/null.cpp \
                      alc/effects/pshifter.cpp \
                      alc/effects/reverb.cpp \
                      alc/effects/vmorpher.cpp \
                      alc/effectslot.cpp \
                      alc/helpers.cpp \
                      alc/hrtf.cpp \
                      alc/panning.cpp \
                      alc/uiddefs.cpp \
                      alc/voice.cpp \
                      common/alcomplex.cpp \
                      common/alfstream.cpp \
                      common/almalloc.cpp \
                      common/alstring.cpp \
                      common/dynload.cpp \
                      common/polyphase_resampler.cpp \
                      common/ringbuffer.cpp \
                      common/strutils.cpp \
                      common/threads.cpp \
                      core/ambdec.cpp \
                      core/bs2b.cpp \
                      core/bsinc_tables.cpp \
                      core/cpu_caps.cpp \
                      core/devformat.cpp \
                      core/except.cpp \
                      core/filters/biquad.cpp \
                      core/filters/nfc.cpp \
                      core/filters/splitter.cpp \
                      core/fmt_traits.cpp \
                      core/fpu_ctrl.cpp \
                      core/logging.cpp \
                      core/mastering.cpp \
                      core/mixer/mixer_c.cpp \
                      core/uhjfilter.cpp \
                      com_jme3_audio_android_AndroidAL.c \
                      com_jme3_audio_android_AndroidALC.c \
                      com_jme3_audio_android_AndroidEFX.c

include $(BUILD_SHARED_LIBRARY)

#                      Alc/mixer/hrtf_inc.c \

