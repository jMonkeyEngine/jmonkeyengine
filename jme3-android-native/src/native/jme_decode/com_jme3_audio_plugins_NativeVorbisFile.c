#include <unistd.h>
#include <stdlib.h>
#include <errno.h>

#include "Tremor/ivorbisfile.h"

#include "com_jme3_audio_plugins_NativeVorbisFile.h"

#ifndef NDEBUG
#include <android/log.h>
#include <stdio.h>
#define LOGI(fmt, ...) __android_log_print(ANDROID_LOG_INFO, \
                       "NativeVorbisFile", fmt, ##__VA_ARGS__);
#else
// #error We are building in release mode, arent we?
#define LOGI(fmt, ...)
#endif

typedef struct
{
    JNIEnv* env;
    int fd;
    int start;
    int end;
    int current;
}
FileDescWrapper;

// size_t read (int fd, void *buf, size_t nbytes)
static size_t FileDesc_read(void *ptr, size_t size, size_t nmemb, void *datasource)
{
    FileDescWrapper* wrapper = (FileDescWrapper*)datasource;
    
    int req_size = size * nmemb;
    int remaining = wrapper->end - wrapper->current;
    int to_read = remaining < req_size ? remaining : req_size;
    
    if (to_read <= 0) 
    {
        return 0;
    }
    
    size_t total_read = read(wrapper->fd, ptr, to_read);
    
    if (total_read > 0)
    {
        wrapper->current += total_read;
    }
    
    LOGI("FD read(%d) = %zu", to_read, total_read);
    
    return total_read;
}

// off64_t lseek64(int fd, off64_t offset, int whence); 
static int FileDesc_seek(void *datasource, ogg_int64_t offset, int whence)
{
    FileDescWrapper* wrapper = (FileDescWrapper*)datasource;
    
    int actual_offset;
    
    switch (whence)
    {
        case SEEK_SET:
            // set the offset relative to start location.
            actual_offset = wrapper->start + offset;
            break;
        case SEEK_END:
            // seek from the end of the file.
            // offset needs to be negative in this case.
            actual_offset = wrapper->end + offset;
            break;
        case SEEK_CUR:
            // seek relative to current position.
            actual_offset = wrapper->current + offset;
            break;
        default:
            // invalid whence.
            errno = EINVAL;
            return (off_t)-1;
    }
    
    if (actual_offset < wrapper->start || 
        actual_offset > wrapper->end)
    {
        // actual offset should be within our acceptable range.
        errno = EINVAL;
        return (off_t)-1;
    }
    
    int result = lseek64(wrapper->fd, actual_offset, SEEK_SET);
    
    LOGI("FD seek(%d) = %d", actual_offset, result);
    
    if (result < 0)
    {
        // failed, errno should have been set by lseek.
        return (off_t)-1;
    }
    
    if (result != actual_offset)
    {
        // did not seek the expected amount. something wrong here.
        errno = EINVAL;
        return (off_t)-1;
    }
    
    // seek succeeded.
    // update current position
    wrapper->current = actual_offset;
}

static int FileDesc_close(void *datasource)
{
    FileDescWrapper* wrapper = (FileDescWrapper*)datasource;
    
    LOGI("FD close");
    
    return close(wrapper->fd);
}

static long FileDesc_tell(void *datasource)
{
    FileDescWrapper* wrapper = (FileDescWrapper*)datasource;
    long result = lseek64(wrapper->fd, 0, SEEK_CUR);
    
    LOGI("FD tell = %ld", result);
    
    if (wrapper->current != result)
    {
        // Not sure how to deal with this.
        LOGI("PROBLEM: stored offset does not match actual: %d != %ld", 
             wrapper->current, result);
    }
    
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
  (JNIEnv *env, jobject nvf, jint fd, jlong off, jlong len)
{
    LOGI("open: fd = %d, off = %lld, len = %lld", fd, off, len);
    
    OggVorbis_File* ovf = (OggVorbis_File*) malloc(sizeof(OggVorbis_File));
    
    FileDescWrapper* wrapper = (FileDescWrapper*) malloc(sizeof(FileDescWrapper));
    wrapper->fd = fd;
    wrapper->env = env; // NOTE: every java call has to update this
    wrapper->start = off;
    wrapper->current = off;
    wrapper->end = off + len;
    
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
    
    // total # of bytes = total samples * bytes per sample * channels
    int total_samples = ov_pcm_total(ovf, -1);
    jint total_bytes = total_samples * 2 * info->channels;
    
    jboolean seekable = ov_seekable(ovf) != 0;
    
    // duration = millis / 1000
    long timeMillis = ov_time_total(ovf, -1);
    double timeSeconds = ((double)timeMillis) / 1000.0;
    jfloat duration = (jfloat) timeSeconds;
    
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
    jobject nvfBuf = (*env)->GetObjectField(env, nvf, nvf_field_ovf);
    OggVorbis_File* ovf = (OggVorbis_File*) (*env)->GetDirectBufferAddress(env, nvfBuf);
    FileDescWrapper* wrapper = (FileDescWrapper*) ovf->datasource;
    wrapper->env = env;
    
    LOGI("ov_time_seek(%f)", (double)time);
    
    int result = ov_time_seek(ovf, (double)time);
    
    if (result != 0)
    {
        char err[512];
        sprintf(err, "ov_time_seek failed: %d", result);
        throwIOException(env, err);
    }
}

JNIEXPORT jint JNICALL Java_com_jme3_audio_plugins_NativeVorbisFile_read
  (JNIEnv *env, jobject nvf, jbyteArray buf, jint off, jint len)
{
    int bitstream = -1;
    jobject nvfBuf = (*env)->GetObjectField(env, nvf, nvf_field_ovf);
    OggVorbis_File* ovf = (OggVorbis_File*) (*env)->GetDirectBufferAddress(env, nvfBuf);
    FileDescWrapper* wrapper = (FileDescWrapper*) ovf->datasource;
    wrapper->env = env;
    
    char nativeBuf[len];
    
    long result = ov_read(ovf, (void*) nativeBuf, sizeof(nativeBuf), &bitstream);
    
    LOGI("ov_read(%d) = %ld", len, result);
    
    if (result == 0)
    {
        return (jint)-1; // EOF
    }
    else if (result < 0)
    {
        char err[512];
        sprintf(err, "ov_read failed: %ld", result);
        throwIOException(env, err);
        return 0;
    }
    
    jbyte* javaBuf = (*env)->GetPrimitiveArrayCritical(env, buf, 0);
    
    if (javaBuf == NULL)
    {
        throwIOException(env, "Failed to acquire array elements");
        return 0;
    }
    
    memcpy(&javaBuf[off], nativeBuf, result);
    
    (*env)->ReleasePrimitiveArrayCritical(env, buf, javaBuf, 0);
    
    return result;
}

JNIEXPORT void JNICALL Java_com_jme3_audio_plugins_NativeVorbisFile_readFully
  (JNIEnv *env, jobject nvf, jobject buf)
{
    int bitstream = -1;
    jobject nvfBuf = (*env)->GetObjectField(env, nvf, nvf_field_ovf);
    OggVorbis_File* ovf = (OggVorbis_File*) (*env)->GetDirectBufferAddress(env, nvfBuf);
    FileDescWrapper* wrapper = (FileDescWrapper*) ovf->datasource;
    wrapper->env = env;
    
    char err[512];
    void* byteBufferPtr = (*env)->GetDirectBufferAddress(env, buf);
    jlong byteBufferCap = (*env)->GetDirectBufferCapacity(env, buf);
    
    int offset     = 0;
    int remaining  = byteBufferCap;
    
    while (remaining > 0)
    {
        long result = ov_read(ovf, byteBufferPtr + offset, remaining, &bitstream);

        LOGI("ov_read(%d, %d) = %ld", offset, remaining, result);
        
        if (result == 0)
        {
            sprintf(err, "premature EOF. expected %lld bytes, got %d.", 
                    byteBufferCap, offset);
            
            throwIOException(env, err);
            return;
        }
        else if (result < 0)
        {
            sprintf(err, "ov_read failed: %ld", result);
            throwIOException(env, err);
            return;
        }
        
        remaining  -= result;
        offset     += result;
    }
}

JNIEXPORT void JNICALL Java_com_jme3_audio_plugins_NativeVorbisFile_close
  (JNIEnv *env, jobject nvf)
{
    LOGI("close");
    
    jobject ovfBuf = (*env)->GetObjectField(env, nvf, nvf_field_ovf);
    OggVorbis_File* ovf = (OggVorbis_File*) (*env)->GetDirectBufferAddress(env, ovfBuf);
    FileDescWrapper* wrapper = (FileDescWrapper*) ovf->datasource;
    wrapper->env = env;
    
    ov_clear(ovf);
    
    free(wrapper);
    free(ovf);
    (*env)->SetObjectField(env, nvf, nvf_field_ovf, NULL);
}