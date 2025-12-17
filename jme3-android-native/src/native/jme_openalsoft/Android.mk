# jni/Android.mk

LOCAL_PATH := $(call my-dir)

# require the path to cmake-build-<ABI>
ifndef OPENALSOFT_BUILD_ROOT
$(error OPENALSOFT_BUILD_ROOT not set! pass it via ndk-build OPENALSOFT_BUILD_ROOT=/path/to/cmake-build-root)
endif

# assemble the path to this ABI's .a
OPENAL_PREBUILT_DIR := $(OPENALSOFT_BUILD_ROOT)/cmake-build-$(TARGET_ARCH_ABI)

# -----------------------------------------------------------------------------
# 1) prebuilt static library
include $(CLEAR_VARS)
LOCAL_MODULE := openalsoft_prebuilt
LOCAL_SRC_FILES := $(OPENAL_PREBUILT_DIR)/libopenal.a
LOCAL_EXPORT_C_INCLUDES := $(OPENALSOFT_BUILD_ROOT)/include
include $(PREBUILT_STATIC_LIBRARY)

# -----------------------------------------------------------------------------
# 2) your JNI wrapper
include $(CLEAR_VARS)
LOCAL_MODULE    := openalsoftjme
LOCAL_SRC_FILES := \
    com_jme3_audio_android_AndroidAL.c \
    com_jme3_audio_android_AndroidALC.c \
    com_jme3_audio_android_AndroidEFX.c

LOCAL_C_INCLUDES  += \
    $(LOCAL_PATH) \
    $(LOCAL_PATH)/include \
    $(LOCAL_PATH)/alc \
    $(LOCAL_PATH)/common

LOCAL_CPP_FEATURES          := exceptions rtti
LOCAL_CFLAGS                := -ffast-math \
                               -DAL_ALEXT_PROTOTYPES \
                               -fcommon \
                               -O0 \
                               -DRESTRICT=""

LOCAL_LDLIBS                := -lOpenSLES -llog -Wl,-s -lc++_static -lc++abi
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    LOCAL_LDFLAGS               += "-Wl,-z,max-page-size=16384"
endif
ifeq ($(TARGET_ARCH_ABI),x86_64)
    LOCAL_LDFLAGS               += "-Wl,-z,max-page-size=16384"
endif
LOCAL_STATIC_LIBRARIES      := openalsoft_prebuilt
# (or LOCAL_WHOLE_STATIC_LIBRARIES if you need every object pulled in)

include $(BUILD_SHARED_LIBRARY)

