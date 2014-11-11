#include <unistd.h>
#include <stdlib.h>
#include "Tremor/ivorbisfile.h"

#include "com_jme3_audio_plugins_NativeVorbisFile.h"

#ifndef NDEBUG
#include <android/log.h>
#include <stdio.h>
#define LOGI(fmt, ...) __android_log_print(ANDROID_LOG_INFO, \
                       "NativeVorbisFile", fmt, ##__VA_ARGS__);
#else
#error We are building in release mode, arent we?
#define LOGI(fmt, ...)
#endif

typedef struct
{
    JNIEnv* env;
    int fd;
}
FileDescWrapper;

static size_t FileDesc_read(void *ptr, size_t size, size_t nmemb, void *datasource)
{
    FileDescWrapper* wrapper = (FileDescWrapper*)datasource;
    size_t totalRead = read(wrapper->fd, ptr, size * nmemb);
    
    LOGI("read(%zu) = %zu", size * nmemb, totalRead);
    
    return totalRead;
}

// off64_t lseek64(int fd, off64_t offset, int whence); 
static int FileDesc_seek(void *datasource, ogg_int64_t offset, int whence)
{
    FileDescWrapper* wrapper = (FileDescWrapper*)datasource;
    int result = lseek64(wrapper->fd, offset, whence);
    
    char* whenceStr;
    switch (whence) {
        case SEEK_CUR: whenceStr = "SEEK_CUR"; break;
        case SEEK_END: whenceStr = "SEEK_END"; break;
        case SEEK_SET: whenceStr = "SEEK_SET"; break;
        default: whenceStr = "unknown"; break;
    }
    LOGI("seek(%lld, %s) = %d", offset, whenceStr, result);
}

static int FileDesc_close(void *datasource)
{
    FileDescWrapper* wrapper = (FileDescWrapper*)datasource;
    
    LOGI("close");
    
    return close(wrapper->fd);
}

static long FileDesc_tell(void *datasource)
{
    FileDescWrapper* wrapper = (FileDescWrapper*)datasource;
    long result = lseek64(wrapper->fd, 0, SEEK_CUR);
    
    LOGI("tell = %ld", result);
    
    return result;
}

static ov_callbacks FileDescCallbacks = {
    FileDesc_read,
    FileDesc_seek,
    FileDesc_close,
    FileDesc_tell
};

static void throwIOException(JNIEnv* env, const char* message)
{
    jclass ioExClazz = (*env)->FindClass(env, "java/io/IOException");
    (*env)->ThrowNew(env, ioExClazz, message);
}

static jfieldID nvf_field_ovf;
static jfieldID nvf_field_seekable;
static jfieldID nvf_field_channels;
static jfieldID nvf_field_sampleRate;
static jfieldID nvf_field_bitRate;
static jfieldID nvf_field_totalBytes;
static jfieldID nvf_field_duration;

JNIEXPORT void JNICALL Java_com_jme3_audio_plugins_NativeVorbisFile_nativeInit
  (JNIEnv *env, jclass clazz)
{
    LOGI("nativeInit");
    
    nvf_field_ovf = (*env)->GetFieldID(env, clazz, "ovf", "Ljava/nio/ByteBuffer;");;
    nvf_field_seekable = (*env)->GetFieldID(env, clazz, "seekable", "Z");
    nvf_field_channels = (*env)->GetFieldID(env, clazz, "channels", "I");
    nvf_field_sampleRate = (*env)->GetFieldID(env, clazz, "sampleRate", "I");
    nvf_field_bitRate = (*env)->GetFieldID(env, clazz, "bitRate", "I");
    nvf_field_totalBytes = (*env)->GetFieldID(env, clazz, "totalBytes", "I");
    nvf_field_duration = (*env)->GetFieldID(env, clazz, "duration", "F");
}

JNIEXPORT void JNICALL Java_com_jme3_audio_plugins_NativeVorbisFile_open
  (JNIEnv *env, jobject nvf, jint fd)
{
    LOGI("open: %d", fd)
            
    OggVorbis_File* ovf = (OggVorbis_File*) malloc(sizeof(OggVorbis_File));
    
    FileDescWrapper* wrapper = (FileDescWrapper*) malloc(sizeof(FileDescWrapper));
    wrapper->fd = fd;
    wrapper->env = env; // NOTE: every java call has to update this
    
    int result = ov_open_callbacks((void*)wrapper, ovf, NULL, 0, FileDescCallbacks);
    
    if (result != 0)
    {
        LOGI("ov_open fail");
        
        free(ovf);
        free(wrapper);
    
        char err[512];
        sprintf(err, "ov_open failed: %d", result);
        throwIOException(env, err);
        
        return;
    }
    
    LOGI("ov_open OK");
    jobject ovfBuf = (*env)->NewDirectByteBuffer(env, ovf, sizeof(OggVorbis_File));
    
    vorbis_info* info = ov_info(ovf, -1);
    jint total_bytes = ov_pcm_total(ovf, -1);
    jboolean seekable = ov_seekable(ovf) != 0;
    jfloat duration = (jfloat) ov_time_total(ovf, -1);
    
    (*env)->SetObjectField(env, nvf, nvf_field_ovf, ovfBuf);
    (*env)->SetBooleanField(env, nvf, nvf_field_seekable, seekable);
    (*env)->SetIntField(env, nvf, nvf_field_channels, info->channels);
    (*env)->SetIntField(env, nvf, nvf_field_sampleRate, info->rate);
    (*env)->SetIntField(env, nvf, nvf_field_bitRate, info->bitrate_nominal);
    (*env)->SetIntField(env, nvf, nvf_field_totalBytes, total_bytes);
    (*env)->SetFloatField(env, nvf, nvf_field_duration, duration);
}

JNIEXPORT void JNICALL Java_com_jme3_audio_plugins_NativeVorbisFile_seekTime
  (JNIEnv *env, jobject nvf, jdouble time)
{
    
}

JNIEXPORT jint JNICALL Java_com_jme3_audio_plugins_NativeVorbisFile_read
  (JNIEnv *env, jobject nvf, jbyteArray buf, jint off, jint len)
{
    return 0;
}

JNIEXPORT void JNICALL Java_com_jme3_audio_plugins_NativeVorbisFile_readFully
  (JNIEnv *env, jobject nvf, jobject buf)
{
    
}

JNIEXPORT void JNICALL Java_com_jme3_audio_plugins_NativeVorbisFile_close
  (JNIEnv *env, jobject nvf)
{
    LOGI("close");
    
    jobject ovfBuf = (*env)->GetObjectField(env, nvf, nvf_field_ovf);
    OggVorbis_File* ovf = (OggVorbis_File*) (*env)->GetDirectBufferAddress(env, ovfBuf);
    FileDescWrapper* wrapper = (FileDescWrapper*) ovf->datasource;
    free(wrapper);
    free(ovf);
    (*env)->SetObjectField(env, nvf, nvf_field_ovf, NULL);
}