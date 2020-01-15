#include "../headers/com_jme3_audio_android_AndroidAL.h"
#include "AL/al.h"
#include "AL/alext.h"

JNIEXPORT jstring JNICALL Java_com_jme3_audio_android_AndroidAL_alGetString
  (JNIEnv* env, jobject obj, jint param)
{
    return (*env)->NewStringUTF(env, alGetString(param));
}

JNIEXPORT jint JNICALL Java_com_jme3_audio_android_AndroidAL_alGenSources
  (JNIEnv *env, jobject obj)
{
    ALuint source;
    alGenSources(1, &source);
    return source;
}

JNIEXPORT jint JNICALL Java_com_jme3_audio_android_AndroidAL_alGetError
  (JNIEnv *env, jobject obj)
{
    return alGetError();
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alDeleteSources
  (JNIEnv* env, jobject obj, jint numSources, jobject intbufSources)
{
    ALuint* pIntBufSources = (ALuint*) (*env)->GetDirectBufferAddress(env, intbufSources);
    alDeleteSources((ALsizei)numSources, pIntBufSources);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alGenBuffers
  (JNIEnv* env, jobject obj, jint numBuffers, jobject intbufBuffers)
{
    ALuint* pIntBufBuffers = (ALuint*) (*env)->GetDirectBufferAddress(env, intbufBuffers);
    alGenBuffers((ALsizei)numBuffers, pIntBufBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alDeleteBuffers
  (JNIEnv* env, jobject obj, jint numBuffers, jobject intbufBuffers)
{
    ALuint* pIntBufBuffers = (ALuint*) (*env)->GetDirectBufferAddress(env, intbufBuffers);
    alDeleteBuffers((ALsizei)numBuffers, pIntBufBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alSourceStop
  (JNIEnv *env, jobject obj, jint source)
{
    alSourceStop((ALuint)source);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alSourcei
  (JNIEnv *env, jobject obj, jint source, jint param, jint value)
{
    alSourcei((ALuint)source, (ALenum)param, (ALint)value);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alBufferData
  (JNIEnv* env, jobject obj, jint buffer, jint format, jobject bufferData, jint bufferSize, jint frequency)
{
    ALuint* pBufferData = (ALuint*) (*env)->GetDirectBufferAddress(env, bufferData);
    alBufferData((ALuint)buffer, (ALenum)format, pBufferData, (ALsizei)bufferSize, (ALsizei)frequency);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alSourcePlay
  (JNIEnv *env, jobject obj, jint source)
{
    alSourcePlay((ALuint)source);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alSourcePause
  (JNIEnv *env, jobject obj, jint source)
{
    alSourcePause((ALuint)source);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alSourcef
  (JNIEnv *env, jobject obj, jint source, jint param, jfloat value)
{
    alSourcef((ALuint)source, (ALenum)param, (ALfloat)value);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alSource3f
  (JNIEnv *env, jobject obj, jint source, jint param, jfloat value1, jfloat value2, jfloat value3)
{
    alSource3f((ALuint)source, (ALenum)param, (ALfloat)value1, (ALfloat)value2, (ALfloat)value3);
}

JNIEXPORT jint JNICALL Java_com_jme3_audio_android_AndroidAL_alGetSourcei
  (JNIEnv *env, jobject obj, jint source, jint param)
{
    ALint result;
    alGetSourcei((ALuint)source, (ALenum)param, &result);
    return (jint)result;
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alSourceUnqueueBuffers
  (JNIEnv* env, jobject obj, jint source, jint numBuffers, jobject buffers)
{
    ALuint* pBuffers = (ALuint*) (*env)->GetDirectBufferAddress(env, buffers);
    alSourceUnqueueBuffers((ALuint)source, (ALsizei)numBuffers, pBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alSourceQueueBuffers
  (JNIEnv* env, jobject obj, jint source, jint numBuffers, jobject buffers)
{
    ALuint* pBuffers = (ALuint*) (*env)->GetDirectBufferAddress(env, buffers);
    alSourceQueueBuffers((ALuint)source, (ALsizei)numBuffers, pBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alListener
  (JNIEnv* env, jobject obj, jint param, jobject bufferData)
{
    ALfloat* pBufferData = (ALfloat*) (*env)->GetDirectBufferAddress(env, bufferData);
    alListenerfv((ALenum)param, pBufferData);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alListenerf
  (JNIEnv *env, jobject obj, jint param, jfloat value)
{
    alListenerf((ALenum)param, (ALfloat)value);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alListener3f
  (JNIEnv *env, jobject obj, jint param, jfloat value1, jfloat value2, jfloat value3)
{
    alListener3f((ALenum)param, (ALfloat)value1, (ALfloat)value2, (ALfloat)value3);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidAL_alSource3i
  (JNIEnv *env, jobject obj, jint source, jint param, jint value1, jint value2, jint value3)
{
    alSource3i((ALuint)source, (ALenum)param, (ALint)value1, (ALint)value2, (ALint)value3);
}
