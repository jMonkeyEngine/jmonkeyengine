#include "util.h"
#include "com_jme3_audio_android_AndroidALC.h"
#include "AL/alc.h"
#include "AL/alext.h"

static jboolean created = JNI_FALSE;

/* InitAL opens the default device and sets up a context using default
 * attributes, making the program ready to call OpenAL functions. */
static int InitAL()
{
    ALCdevice *device = NULL;
    ALCcontext *ctx = NULL;

    /* Open and initialize a device with default settings */
    device = alcOpenDevice(NULL);
    
    if(device == NULL)
    {
        fprintf(stderr, "Could not open a device!\n");
        goto cleanup;
    }

    ctx = alcCreateContext(device, NULL);
    
    if (ctx == NULL)
    {
        fprintf(stderr, "Could not create context!\n");
        goto cleanup;
    }
    
    if (!alcMakeContextCurrent(ctx)) 
    {
        fprintf(stderr, "Could not make context current!\n");
        goto cleanup;
    }

    return 0;
    
cleanup:
    if (ctx != NULL) alcDestroyContext(ctx);
    if (device != NULL) alcCloseDevice(device);
    return 1;
}

/* CloseAL closes the device belonging to the current context, and destroys the
 * context. */
static void CloseAL()
{
    ALCdevice *device;
    ALCcontext *ctx;

    ctx = alcGetCurrentContext();
    
    if (ctx == NULL) 
    {
        return;
    }

    device = alcGetContextsDevice(ctx);
    
    if (device == NULL) 
    {
        return;
    }

    if(!alcMakeContextCurrent(NULL)) {
        return;
    }

    alcDestroyContext(ctx);
    alcCloseDevice(device);
}

static ALCdevice* GetALCDevice()
{
    ALCdevice *device;
    ALCcontext *ctx;

    ctx = alcGetCurrentContext();
    
    if (ctx != NULL) 
    {
        device = alcGetContextsDevice(ctx);
        
        if (device != NULL)
        {
            return device;
        }
    }
    
    return NULL;
}

JNIEXPORT jboolean JNICALL Java_com_jme3_audio_android_AndroidALC_isCreated
  (JNIEnv* env, jobject obj)
{
    return created;
}


JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidALC_createALC
  (JNIEnv* env, jobject obj)
{
    created = (InitAL() == 0);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidALC_destroyALC
  (JNIEnv* env, jobject obj)
{
    CloseAL();
    created = JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_com_jme3_audio_android_AndroidALC_alcGetString
  (JNIEnv* env, jobject obj, jint param)
{
    ALCdevice* device = GetALCDevice();
    if (device == NULL) return NULL;
    return (*env)->NewStringUTF(env, alcGetString(device, param));
}

JNIEXPORT jboolean JNICALL Java_com_jme3_audio_android_AndroidALC_alcIsExtensionPresent
  (JNIEnv* env, jobject obj, jstring extension)
{
    ALCdevice* device = GetALCDevice();
    
    if (device == NULL) return JNI_FALSE;
    
    const char* strExtension = (*env)->GetStringUTFChars(env, extension, NULL);
    
    if (strExtension == NULL)
    {
        return JNI_FALSE;
    }
    
    jboolean result = alcIsExtensionPresent(device, strExtension);
    
    (*env)->ReleaseStringUTFChars(env, extension, strExtension);
    
    return result;
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidALC_alcGetInteger
  (JNIEnv* env, jobject obj, jint param, jobject buffer, jint bufferSize)
{
    ALCdevice* device = GetALCDevice();
    
    if (device == NULL) return;

    ALCint* pBuffers = (ALCint*) (*env)->GetDirectBufferAddress(env, buffer);

    alcGetIntegerv(device, (ALCenum)param, (ALCsizei)bufferSize, pBuffers);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidALC_alcDevicePauseSOFT
  (JNIEnv* env, jobject obj)
{
    ALCdevice* device = GetALCDevice();
    
    if (device == NULL) return;
    
    alcDevicePauseSOFT(device);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_android_AndroidALC_alcDeviceResumeSOFT
  (JNIEnv* env, jobject obj)
{
    ALCdevice* device = GetALCDevice();
    
    if (device == NULL) return;
    
    alcDeviceResumeSOFT(device);
}