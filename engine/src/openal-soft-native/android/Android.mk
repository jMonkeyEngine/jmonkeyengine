TARGET_PLATFORM := android-9

ROOT_PATH := $(call my-dir)

########################################################################################################

include $(CLEAR_VARS)

LOCAL_MODULE     := openalsoftjme
LOCAL_ARM_MODE   := arm
LOCAL_PATH       := $(ROOT_PATH)
LOCAL_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/include $(LOCAL_PATH)/OpenAL32/Include

LOCAL_CFLAGS     := -ffast-math -DAL_BUILD_LIBRARY -DAL_ALEXT_PROTOTYPES
LOCAL_LDLIBS     := -llog -Wl,-s
LOCAL_LDLIBS    += -lOpenSLES
#  LOCAL_CFLAGS +=   -DPOST_FROYO #-I$(ANDROID_NDK_ROOT)/platforms/android-9/arch-arm/usr/include/
#  LOCAL_LDLIBS += -ldl -L$(ANDROID_NDK_ROOT)/platforms/android-9/arch-arm/usr/lib/

LOCAL_SRC_FILES  := OpenAL32/alAuxEffectSlot.c \
                    OpenAL32/alBuffer.c        \
                    OpenAL32/alEffect.c        \
                    OpenAL32/alError.c         \
                    OpenAL32/alExtension.c     \
                    OpenAL32/alFilter.c        \
                    OpenAL32/alListener.c      \
                    OpenAL32/alSource.c        \
                    OpenAL32/alState.c         \
                    OpenAL32/alThunk.c         \
                    Alc/ALc.c                  \
                    Alc/ALu.c                  \
                    Alc/alcChorus.c            \
                    Alc/alcConfig.c            \
                    Alc/alcDedicated.c         \
                    Alc/alcEcho.c              \
                    Alc/alcFlanger.c           \
                    Alc/alcModulator.c         \
                    Alc/alcReverb.c            \
                    Alc/alcRing.c              \
                    Alc/alcThread.c            \
                    Alc/bs2b.c                 \
                    Alc/helpers.c              \
                    Alc/panning.c              \
                    Alc/hrtf.c                 \
                    Alc/mixer.c                \
                    Alc/mixer_c.c              \
                    Alc/backends/loopback.c    \
                    Alc/backends/null.c        \
                    Alc/backends/opensl.c      \
                    com_jme3_audio_android_AndroidOpenALSoftAudioRenderer.cpp
#                    Alc/backends/alsa.c        \
#                    Alc/backends/android.c     \

include $(BUILD_SHARED_LIBRARY)

