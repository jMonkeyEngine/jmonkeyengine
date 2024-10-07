/*
 * Copyright (c) 2009-2022 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @file com_jme3_util_AndroidNativeBufferAllocator.c
 * @author pavl_g.
 * @brief Creates and releases direct byte buffers for {com.jme3.util.AndroidNativeBufferAllocator}.
 * @date 2022-05-17.
 * @note
 * Find more at :
 * - JNI Direct byte buffers : https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/functions.html#NewDirectByteBuffer.
 * - JNI Get Direct byte buffer : https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/functions.html#GetDirectBufferAddress.
 * - GNU Basic allocation : https://www.gnu.org/software/libc/manual/html_node/Basic-Allocation.html.
 * - GNU Allocating Cleared Space : https://www.gnu.org/software/libc/manual/html_node/Allocating-Cleared-Space.html.
 * - GNU No Memory error : https://www.gnu.org/software/libc/manual/html_node/Error-Codes.html#index-ENOMEM.
 * - GNU Freeing memory : https://www.gnu.org/software/libc/manual/html_node/Freeing-after-Malloc.html.
 * - Android logging : https://developer.android.com/ndk/reference/group/logging.
 * - Android logging example : https://github.com/android/ndk-samples/blob/7a8ff4c5529fce6ec4c5796efbe773f5d0e569cc/hello-libs/app/src/main/cpp/hello-libs.cpp#L25-L26.
 */

#include "headers/com_jme3_util_AndroidNativeBufferAllocator.h"
#include <stdlib.h>
#include <stdbool.h>
#include <errno.h>

#ifndef NDEBUG
#include <android/log.h>
#define LOG(LOG_ID, ...) __android_log_print(LOG_ID, \
                    "AndroidNativeBufferAllocator", ##__VA_ARGS__);
#else
#define LOG(...)
#endif

bool isDeviceOutOfMemory(void*);

/**
 * @brief Tests if the device is out of memory.
 *
 * @return true if the buffer to allocate is a NULL pointer and the errno is ENOMEM (Error-no-memory).
 * @return false otherwise.
 */
bool isDeviceOutOfMemory(void* buffer) {
    return buffer == NULL && errno == ENOMEM;
}

JNIEXPORT void JNICALL Java_com_jme3_util_AndroidNativeBufferAllocator_releaseDirectByteBuffer
(JNIEnv * env, jobject object, jobject bufferObject)
{
    void* buffer = (*env)->GetDirectBufferAddress(env, bufferObject);
    // deallocates the buffer pointer
    free(buffer);
    // log the destruction by mem address
    LOG(ANDROID_LOG_INFO, "Buffer released (mem_address, size) -> (%p, %lu)", buffer, sizeof(buffer));
    // avoid accessing this memory space by resetting the memory address
    buffer = NULL;
    LOG(ANDROID_LOG_INFO, "Buffer mem_address formatted (mem_address, size) -> (%p, %u)", buffer, sizeof(buffer));
}

JNIEXPORT jobject JNICALL Java_com_jme3_util_AndroidNativeBufferAllocator_createDirectByteBuffer
(JNIEnv * env, jobject object, jlong size)
{
    void* buffer = calloc(1, size);
    if (isDeviceOutOfMemory(buffer)) {
       LOG(ANDROID_LOG_FATAL, "Device is out of memory exiting with %u", errno);
       exit(errno);
    } else {
       LOG(ANDROID_LOG_INFO, "Buffer created successfully (mem_address, size) -> (%p %lli)", buffer, size);
    }
    return (*env)->NewDirectByteBuffer(env, buffer, size);
}