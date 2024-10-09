//#include "util.h"
#include "com_jme3_audio_ios_IosEFX.h"
//#include "AL/alext.h"

#include "OpenAL/al.h"
#include "OpenAL/alc.h"
#include "OpenAL/oalMacOSX_OALExtensions.h"

JNIEXPORT void JNICALL Java_com_jme3_audio_ios_IosEFX_alGenAuxiliaryEffectSlots
  (JNIEnv* env, jobject obj, jint numSlots, jobject buffer)
{
    ALuint* pBuffers = (ALuint*) (*env)->GetDirectBufferAddress(env, buffer);
//    alGenAuxiliaryEffectSlots((ALsizei)numSlots, pBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_ios_IosEFX_alGenEffects
  (JNIEnv* env, jobject obj, jint numEffects, jobject buffer)
{
    ALuint* pBuffers = (ALuint*) (*env)->GetDirectBufferAddress(env, buffer);
//    alGenEffects((ALsizei)numEffects, pBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_ios_IosEFX_alEffecti
  (JNIEnv* env, jobject obj, jint effect, jint param, jint value)
{
//    alEffecti((ALuint)effect, (ALenum)param, (ALint)value);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_ios_IosEFX_alAuxiliaryEffectSloti
  (JNIEnv* env, jobject obj, jint effectSlot, jint param, jint value)
{
//    alAuxiliaryEffectSloti((ALuint)effectSlot, (ALenum)param, (ALint)value);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_ios_IosEFX_alDeleteEffects
  (JNIEnv* env, jobject obj, jint numEffects, jobject buffer)
{
    ALuint* pBuffers = (ALuint*) (*env)->GetDirectBufferAddress(env, buffer);
//    alDeleteEffects((ALsizei)numEffects, pBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_ios_IosEFX_alDeleteAuxiliaryEffectSlots
  (JNIEnv* env, jobject obj, jint numEffectSlots, jobject buffer)
{
    ALuint* pBuffers = (ALuint*) (*env)->GetDirectBufferAddress(env, buffer);
//    alDeleteAuxiliaryEffectSlots((ALsizei)numEffectSlots, pBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_ios_IosEFX_alGenFilters
  (JNIEnv* env, jobject obj, jint numFilters, jobject buffer)
{
    ALuint* pBuffers = (ALuint*) (*env)->GetDirectBufferAddress(env, buffer);
//    alGenFilters((ALsizei)numFilters, pBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_ios_IosEFX_alFilteri
  (JNIEnv* env, jobject obj, jint filter, jint param, jint value)
{
//    alFilteri((ALuint)filter, (ALenum)param, (ALint)value);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_ios_IosEFX_alFilterf
  (JNIEnv* env, jobject obj, jint filter, jint param, jfloat value)
{
//    alFilterf((ALuint)filter, (ALenum)param, (ALfloat)value);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_ios_IosEFX_alDeleteFilters
  (JNIEnv* env, jobject obj, jint numFilters, jobject buffer)
{
    ALuint* pBuffers = (ALuint*) (*env)->GetDirectBufferAddress(env, buffer);
//    alDeleteFilters((ALsizei)numFilters, pBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_ios_IosEFX_alEffectf
  (JNIEnv* env, jobject obj, jint effect, jint param, jfloat value)
{
//    alEffectf((ALuint)effect, (ALenum)param, (ALfloat)value);
}
