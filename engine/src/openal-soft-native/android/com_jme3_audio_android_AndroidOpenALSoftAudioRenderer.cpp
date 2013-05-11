#include "com_jme3_audio_android_AndroidOpenALSoftAudioRenderer.h"
#include "AL/alc.h"
#include "AL/al.h"
#include "AL/alext.h"
// for __android_log_print(ANDROID_LOG_INFO, "YourApp", "formatted message");
#include <android/log.h>
#include <jni.h>
#include <stddef.h>
#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <time.h>

#ifdef __cplusplus
extern "C" {
#endif

static jboolean created = JNI_FALSE;

#define BUFFER_COUNT 1
ALuint* buffers[BUFFER_COUNT] = { 0 };
ALuint* source = 0;

int getError() {
    int errorcode = alGetError();
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "getError: %d", errorcode);
    return errorcode;
}

/* InitAL opens the default device and sets up a context using default
 * attributes, making the program ready to call OpenAL functions. */
int InitAL()
{
    ALCdevice *device;
    ALCcontext *ctx;

    /* Open and initialize a device with default settings */
    device = alcOpenDevice(NULL);
    if(!device)
    {
        fprintf(stderr, "Could not open a device!\n");
        return 1;
    }

    ctx = alcCreateContext(device, NULL);
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "NULL: %d", NULL);
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "Created context: %d", ctx);
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "Created context addr: %d", &ctx);
    if(ctx == NULL || alcMakeContextCurrent(ctx) == ALC_FALSE)
    {
        if(ctx != NULL)
            alcDestroyContext(ctx);
        alcCloseDevice(device);
        fprintf(stderr, "Could not set a context!\n");
        return 1;
    }

    printf("Opened \"%s\"\n", alcGetString(device, ALC_DEVICE_SPECIFIER));
    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "Opened %s", alcGetString(device, ALC_DEVICE_SPECIFIER));
    return 0;
}

/* CloseAL closes the device belonging to the current context, and destroys the
 * context. */
void CloseAL()
{
    ALCdevice *device;
    ALCcontext *ctx;
    ALCboolean result;

//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "Getting current context");
    ctx = alcGetCurrentContext();
//    getError();
    if(ctx == NULL){
        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "No context found");
        return;
    }

//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "Getting current context device");
    device = alcGetContextsDevice(ctx);
    if(device == NULL) {
        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "No device found");
        return;
    } else {
//        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alcGetContextsDevice device: %d", device);
//        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alcGetContextsDevice device addr: %d", &device);
    }
//    getError();

//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "Setting context to NULL");
    result = alcMakeContextCurrent(NULL);
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alcMakeContextCurrent returned");
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alcMakeContextCurrent returned with result: %d", result);
    if(!result) {
        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alcMakeContextCurrent failed");
        return;
    }

//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "Destroying context: %d", ctx);
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "Destroying context addr: %d", &ctx);
    alcDestroyContext(ctx);

//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "Closing device");
    result = alcCloseDevice(device);
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alcCloseDevice result: %d", result);
}


JNIEXPORT jboolean JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alIsCreated
  (JNIEnv* env, jclass)
{
    return created;
}


JNIEXPORT jboolean JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alCreate
  (JNIEnv* env, jclass)
{
    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "Starting Audio Engine");

    InitAL();
    created = JNI_TRUE;
    return created;

}

JNIEXPORT jboolean JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alDestroy
  (JNIEnv* env, jclass)
{

//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alDestroy");
    CloseAL();
    created = JNI_FALSE;
    return created;

}

JNIEXPORT jstring JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alcGetString
  (JNIEnv* env, jclass, jint param)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alcGetString for param: %d", param);

    ALCdevice *device;
    ALCcontext *ctx;

    ctx = alcGetCurrentContext();
    if(ctx != NULL) {
        device = alcGetContextsDevice(ctx);
//        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alcGetString param value: %s", alcGetString(device, param));
        return env->NewStringUTF(alcGetString(device, param));
    }
}

JNIEXPORT jstring JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alGetString
  (JNIEnv* env, jclass, jint param)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alGetString for param: %d", param);
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alGetString param value: %s", alGetString(param));
    return env->NewStringUTF(alGetString(param));
}

JNIEXPORT jint JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alGenSources
  (JNIEnv *, jclass)
{
    ALuint source;
    alGenSources(1, &source);
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alGenSources: %d", source);
    return source;
}

JNIEXPORT jint JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alGetError
  (JNIEnv *, jclass)
{
    return getError();
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alDeleteSources
  (JNIEnv* env, jclass, jint numSources, jobject intbufSources)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alDeleteSources numSources: %d", numSources);

    ALuint* pIntBufSources = (ALuint*) env->GetDirectBufferAddress(intbufSources);
    alDeleteSources((ALsizei)numSources, pIntBufSources);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alGenBuffers
  (JNIEnv* env, jclass, jint numBuffers, jobject intbufBuffers)
{
    ALuint* pIntBufBuffers = (ALuint*) env->GetDirectBufferAddress(intbufBuffers);
    alGenBuffers((ALsizei)numBuffers, pIntBufBuffers);
//    for (int i=0; i<numBuffers; i++) {
//        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alGenBuffers[%d]: %d", i, *(pIntBufBuffers+i));
//    }

}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alDeleteBuffers
  (JNIEnv* env, jclass, jint numBuffers, jobject intbufBuffers)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alDeleteBuffers numBuffers: %d", numBuffers);

    ALuint* pIntBufBuffers = (ALuint*) env->GetDirectBufferAddress(intbufBuffers);
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alDeleteBuffers Buffers: %d", *pIntBufBuffers);
//    for (int i=0; i<numBuffers; i++) {
//        if(alIsBuffer(*(pIntBufBuffers+i)) == AL_TRUE) {
//            __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alDeleteBuffers[%d]: %d", i, *(pIntBufBuffers+i));
//            __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alDeleteBuffers buffer is a known buffer");
//        }
//    }
    alDeleteBuffers((ALsizei)numBuffers, pIntBufBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alSourceStop
  (JNIEnv *, jclass, jint source)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alSourceStop for source: %d", source);
    alSourceStop((ALuint)source);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alSourcei
  (JNIEnv *, jclass, jint source, jint param, jint value)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alSourcei for source: %d, param: %d, value: %d", source, param, value);
    alSourcei((ALuint)source, (ALenum)param, (ALint)value);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alBufferData
  (JNIEnv* env, jclass, jint buffer, jint format, jobject bufferData, jint bufferSize, jint frequency)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alBufferData for source: %d, format: %d, size: %d, frequency: %d", buffer, format, bufferSize, frequency);
    ALuint* pBufferData = (ALuint*) env->GetDirectBufferAddress(bufferData);
    alBufferData((ALuint)buffer, (ALenum)format, pBufferData, (ALsizei)bufferSize, (ALsizei)frequency);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alSourcePlay
  (JNIEnv *, jclass, jint source)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alSourcePlay for source: %d", source);
    alSourcePlay((ALuint)source);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alSourcePause
  (JNIEnv *, jclass, jint source)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alSourcePause for source: %d", source);
    alSourcePause((ALuint)source);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alSourcef
  (JNIEnv *, jclass, jint source, jint param, jfloat value)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alSourcef for source: %d, param: %d, value: %f", source, param, value);
    alSourcef((ALuint)source, (ALenum)param, (ALfloat)value);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alSource3f
  (JNIEnv *, jclass, jint source, jint param, jfloat value1, jfloat value2, jfloat value3)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alSource3f for source: %d, param: %d, value1: %f, value2: %f, value3: %f", source, param, value1, value2, value3);
    alSource3f((ALuint)source, (ALenum)param, (ALfloat)value1, (ALfloat)value2, (ALfloat)value3);
}

JNIEXPORT jint JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alGetSourcei
  (JNIEnv *, jclass, jint source, jint param)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alGetSourcei for source: %d, param: %d", source, param);
    ALint result;
    alGetSourcei((ALuint)source, (ALenum)param, &result);
    return (jint)result;
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alSourceUnqueueBuffers
  (JNIEnv* env, jclass, jint source, jint numBuffers, jobject buffers)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alSourceUnqueueBuffers for source: %d, numBuffers: %d", source, numBuffers);
    ALuint* pBuffers = (ALuint*) env->GetDirectBufferAddress(buffers);

//    for (ALuint i=0; i<numBuffers; i++) {
//        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alSourceUnqueueBuffers, checking buffer[%d]: %d", i, *(pBuffers+i));
//        ALboolean isBuffer = alIsBuffer(*(pBuffers+i));
//        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "buffer check result: %d", isBuffer);
//    }
    alSourceUnqueueBuffers((ALuint)source, (ALsizei)numBuffers, pBuffers);
//    for (ALuint i=0; i<numBuffers; i++) {
//        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alSourceUnqueueBuffers[%d]: %d", i, *(pBuffers+i));
//    }
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alSourceQueueBuffers
  (JNIEnv* env, jclass, jint source, jint numBuffers, jobject buffers)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alSourceQueueBuffers for source: %d, numBuffers: %d", source, numBuffers);
    ALuint* pBuffers = (ALuint*) env->GetDirectBufferAddress(buffers);
    alSourceQueueBuffers((ALuint)source, (ALsizei)numBuffers, pBuffers);
//    for (ALuint i=0; i<numBuffers; i++) {
//        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alSourceQueueBuffers[%d]: %d", i, *(pBuffers+i));
//    }
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alListener
  (JNIEnv* env, jclass, jint param, jobject bufferData)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alListener for param: %d", param);
    ALfloat* pBufferData = (ALfloat*) env->GetDirectBufferAddress(bufferData);
    alListenerfv((ALenum)param, pBufferData);
//    getError();
//    for (int i=0; i<4; i++) {
//        __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alListener[%d]: %f", i, *(pBufferData+(i*sizeof(ALfloat))));
//    }

}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alListenerf
  (JNIEnv *, jclass, jint param, jfloat value)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alListenerf for param: %d, value: %f", param, value);
    alListenerf((ALenum)param, (ALfloat)value);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidOpenALSoftAudioRenderer_alListener3f
  (JNIEnv *, jclass, jint param, jfloat value1, jfloat value2, jfloat value3)
{
//    __android_log_print(ANDROID_LOG_INFO, "OpenAL Soft", "alListener3f for param: %d, value1: %f, value2: %f, value3: %f", param, value1, value2, value3);
    alListener3f((ALenum)param, (ALfloat)value1, (ALfloat)value2, (ALfloat)value3);
}





#ifdef __cplusplus
}
#endif
