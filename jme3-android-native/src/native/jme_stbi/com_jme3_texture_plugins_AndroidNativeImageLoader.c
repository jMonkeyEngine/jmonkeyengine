#include "com_jme3_texture_plugins_AndroidNativeImageLoader.h"
// for __android_log_print(ANDROID_LOG_INFO, "YourApp", "formatted message");
#include <android/log.h>
#include <stddef.h>
#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <time.h>

#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

typedef unsigned int    uint32;


JNIEXPORT jobject JNICALL Java_com_jme3_texture_plugins_AndroidNativeImageLoader_getFailureReason
  (JNIEnv * env, jclass clazz)
{
    return stbi_failure_reason();
}


JNIEXPORT jint JNICALL Java_com_jme3_texture_plugins_AndroidNativeImageLoader_getImageInfo
  (JNIEnv * env, jclass clazz, jobject inBuffer, jint bufSize, jobject outBuffer, jint outSize)
{
    stbi_uc* pInBuffer = (stbi_uc*) (*env)->GetDirectBufferAddress(env, inBuffer);
    stbi_uc* pOutBuffer = (stbi_uc*) (*env)->GetDirectBufferAddress(env, outBuffer);
    uint32 width, height, comp;

    uint32 result = stbi_info_from_memory(pInBuffer, bufSize, &width, &height, &comp);
    if (result == 1) {
        uint32 numBytes = (width) * (height) * (comp);

        __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader", "getImageInfo width: %d", width);
        __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader", "getImageInfo height: %d", height);
        __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader", "getImageInfo comp: %d", comp);
        __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader", "getImageInfo data size: %d", numBytes);

        // each element is a 4 byte int
        if (outSize != 12) {
            return 2;
        }
        memcpy(pOutBuffer+0, &width, 4);
        memcpy(pOutBuffer+4, &height, 4);
        memcpy(pOutBuffer+8, &comp, 4);

        return (jint) 0;
    }

    return 1;
}

JNIEXPORT jint JNICALL Java_com_jme3_texture_plugins_AndroidNativeImageLoader_decodeBuffer
  (JNIEnv * env, jclass clazz, jobject inBuffer, jint inSize, jboolean flipY, jobject outBuffer, jint outSize)
{
    stbi_uc* pInBuffer = (stbi_uc*) (*env)->GetDirectBufferAddress(env, inBuffer);
    stbi_uc* pOutBuffer = (stbi_uc*) (*env)->GetDirectBufferAddress(env, outBuffer);
    uint32 width, height, comp;
    uint32 req_comp = 0;

    stbi_uc* pData = stbi_load_from_memory(pInBuffer, inSize, &width, &height, &comp, req_comp);
    if(pData == NULL) {
        return 1;
    }
    uint32 numBytes = width * height * comp;

    __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader", "decodeBuffer width: %d", width);
    __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader", "decodeBuffer height: %d", height);
    __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader", "decodeBuffer comp: %d", comp);
    __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader", "decodeBuffer data size: %d", numBytes);

    if (numBytes != outSize) {
        return 2;
    }

    int i;
//    for (i=0; i<outSize; i+=4) {
//        __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader",
//                "pData byte[%d] r: %02x, g: %02x, b: %02x, a: %02x",
//                i, *(pData+i), *(pData+i+1), *(pData+i+2), *(pData+i+3));
//    }

   if (!flipY) {
        memcpy(pOutBuffer, pData, outSize);
        stbi_image_free(pData);
        return 0;
    } else {
        uint32 yNew = 0;
        uint32 yOrig = 0;
        // stb_image always outputs in bpp = 8
        uint32 bytesPerLine = (width * comp);
        stbi_uc* newData = (stbi_uc *) malloc(bytesPerLine * height);
        if (newData == NULL) {
            stbi_image_free(pData);
            return 3;
        }

        for (yOrig = 0; yOrig < height; yOrig++){
            yNew = height - yOrig - 1;
//            __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader", "yOrig: %d, yNew: %d, bytes: %d", yOrig, yNew, bytesPerLine);
            memcpy(newData + (yNew * bytesPerLine), pData + (yOrig * bytesPerLine), bytesPerLine);
        }

//        for (i=0; i<outSize; i+=4) {
//            __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader",
//                    "newData byte[%d] r: %02x, g: %02x, b: %02x, a: %02x",
//                    i, *(newData+i), *(newData+i+1), *(newData+i+2), *(newData+i+3));
//        }

        memcpy(pOutBuffer, newData, outSize);

//        for (i=0; i<outSize; i+=4) {
//            __android_log_print(ANDROID_LOG_INFO, "NativeImageLoader",
//                    "pOutBuffer byte[%d] r: %02x, g: %02x, b: %02x, a: %02x",
//                    i, *(pOutBuffer+i), *(pOutBuffer+i+1), *(pOutBuffer+i+2), *(pOutBuffer+i+3));
//        }

        stbi_image_free(pData);
        free(newData);
        return 0;
    }

    return 0;

//    stbi_uc *stbi_load_from_memory(stbi_uc const *buffer, int len, int *x, int *y, int *comp, int req_comp);
}
