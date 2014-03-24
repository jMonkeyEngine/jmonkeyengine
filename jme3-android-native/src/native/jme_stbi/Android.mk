LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := stbijme
LOCAL_C_INCLUDES  := $(LOCAL_PATH)
LOCAL_CFLAGS += -O2
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_SRC_FILES := $(subst $(LOCAL_PATH)/,, $(wildcard $(LOCAL_PATH)/*.c))

#adds zlib
LOCAL_LDLIBS    += -lz -llog

include $(BUILD_SHARED_LIBRARY)
