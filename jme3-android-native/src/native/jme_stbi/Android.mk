TARGET_PLATFORM := android-9

LOCAL_PATH := $(call my-dir)
	
include $(CLEAR_VARS)

LOCAL_MODULE    := stbijme
	
LOCAL_C_INCLUDES  += $(LOCAL_PATH)

LOCAL_CFLAGS := -std=c99
LOCAL_LDLIBS := -lz -llog -Wl,-s
	
LOCAL_SRC_FILES := com_jme3_texture_plugins_AndroidNativeImageLoader.c

include $(BUILD_SHARED_LIBRARY)
