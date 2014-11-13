TARGET_PLATFORM := android-9

LOCAL_PATH := $(call my-dir)
	
include $(CLEAR_VARS)

LOCAL_MODULE    := decodejme

LOCAL_C_INCLUDES:= \
		$(LOCAL_PATH) \
		$(LOCAL_PATH)/Tremor

LOCAL_CFLAGS := -std=gnu99 -DLIMIT_TO_64kHz
LOCAL_LDLIBS := -lz -llog -Wl,-s
	
ifeq ($(TARGET_ARCH),arm)
LOCAL_CFLAGS+= -D_ARM_ASSEM_
endif

LOCAL_ARM_MODE := arm
	
LOCAL_SRC_FILES := \
		Tremor/bitwise.c \
		Tremor/codebook.c \
		Tremor/dsp.c \
		Tremor/floor0.c \
		Tremor/floor1.c \
		Tremor/floor_lookup.c \
		Tremor/framing.c \
		Tremor/info.c \
		Tremor/mapping0.c \
		Tremor/mdct.c \
		Tremor/misc.c \
		Tremor/res012.c \
		Tremor/vorbisfile.c \
		com_jme3_audio_plugins_NativeVorbisFile.c \
		com_jme3_texture_plugins_AndroidNativeImageLoader.c

include $(BUILD_SHARED_LIBRARY)
