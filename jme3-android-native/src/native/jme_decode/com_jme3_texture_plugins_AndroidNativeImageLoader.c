#include "../headers/com_jme3_texture_plugins_AndroidNativeImageLoader.h"
#include <assert.h>

#ifndef NDEBUG
#include <android/log.h>
#define LOGI(fmt, ...) __android_log_print(ANDROID_LOG_INFO, \
                       "NativeImageLoader", fmt, ##__VA_ARGS__);
#else
#define LOGI(fmt, ...)
#endif

#define STB_IMAGE_IMPLEMENTATION
#define STBI_NO_STDIO
#define STBI_NO_HDR
#include "STBI/stb_image.h"

typedef struct 
{
    JNIEnv* env;
    jbyteArray tmp;
    int tmpSize;
    jobject isObject;
    jmethodID isReadMethod;
    jmethodID isSkipMethod;
    int isEOF;
    char* errorMsg;
}
JavaInputStreamWrapper;

static void throwIOException(JNIEnv* env, const char* message)
{
    jclass ioExClazz = (*env)->FindClass(env, "java/io/IOException");
    (*env)->ThrowNew(env, ioExClazz, message);
}

static int InputStream_read(void *user, char *nativeData, int nativeSize) {
    JavaInputStreamWrapper* wrapper = (JavaInputStreamWrapper*) user;
    JNIEnv* env = wrapper->env;
    
    if (nativeSize <= 0)
    {
        wrapper->isEOF = 1;
        wrapper->errorMsg = "read() requested negative or zero size";
        return 0;
    }

    jbyteArray tmp = wrapper->tmp;
    jint tmpSize = wrapper->tmpSize;
    jint remaining = nativeSize;
    jint offset = 0;
    
    while (offset < nativeSize)
    {
        // Read data into Java array.
        jint toRead = tmpSize < remaining ? tmpSize : remaining;
        jint read = (*env)->CallIntMethod(env, wrapper->isObject,
                                          wrapper->isReadMethod, 
                                          tmp, (jint)0, (jint)toRead);
        
        // Check IOException
        if ((*env)->ExceptionCheck(env))
        {
            wrapper->isEOF = 1;
            wrapper->errorMsg = NULL;
            return 0;
        }
        
        LOGI("InputStream->read(tmp, 0, %d) = %d", toRead, read);
        
        // Read -1 bytes = EOF. 
        if (read < 0)
        {
            wrapper->isEOF = 1;
            wrapper->errorMsg = NULL;
            break;
        }
        else if (read == 0)
        {
            // Read 0 bytes, give it another try.
            continue;
        }
        
        // Read 1 byte or more.
        
        LOGI("memcpy(native[%d], java, %d)", offset, read);
        
        // Copy contents of Java array to native array.
        jbyte* nativeTmp = (*env)->GetPrimitiveArrayCritical(env, tmp, 0);
        
        if (nativeTmp == NULL)
        {
            wrapper->isEOF = 1;
            wrapper->errorMsg = "Failed to acquire Java array contents";
            return 0;
        }
        
        memcpy(&nativeData[offset], nativeTmp, read);
        
        (*env)->ReleasePrimitiveArrayCritical(env, tmp, nativeTmp, 0);
        
        offset += read;
        remaining -= read;
        
        assert(remaining >= 0);
        assert(offset <= nativeSize);
    }

    return offset;
}

static void InputStream_skip(void *user, int n) {
    JavaInputStreamWrapper* wrapper = (JavaInputStreamWrapper*) user;
    JNIEnv* env = wrapper->env;
    
    if (n < 0)
    {
        wrapper->isEOF = 1;
        wrapper->errorMsg = "Negative seek attempt detected";
        return;
    } 
    else if (n == 0) 
    {
        return;
    }

    // InputStream.skip(n);
    jlong result = (*env)->CallLongMethod(env, wrapper->isObject, 
                                          wrapper->isSkipMethod, (jlong)n);

    LOGI("InputStream->skip(%lld) = %lld", (jlong)n, result);
    
    // IOException
    if ((*env)->ExceptionCheck(env))
    {
        wrapper->isEOF = 1;
        wrapper->errorMsg = NULL;
    }
    else if ((int)result != n)
    {
        wrapper->isEOF = 1;
        wrapper->errorMsg = "Could not skip requested number of bytes";
    }
}

static int InputStream_eof(void *user) {
    JavaInputStreamWrapper* wrapper = (JavaInputStreamWrapper*) user;
    LOGI("InputStream->eof() = %s", wrapper->isEOF ? "true" : "false");
    return wrapper->isEOF;
}

static stbi_io_callbacks JavaInputStreamCallbacks ={
    InputStream_read,
    InputStream_skip,
    InputStream_eof,
};

static JavaInputStreamWrapper createInputStreamWrapper(JNIEnv* env, jobject is, jbyteArray tmpArray)
{
    JavaInputStreamWrapper wrapper;
    jclass inputStreamClass = (*env)->FindClass(env, "java/io/InputStream");
    
    wrapper.env = env;
    wrapper.isObject = is;
    wrapper.isEOF = 0;
    wrapper.errorMsg = NULL;
    wrapper.isReadMethod = (*env)->GetMethodID(env, inputStreamClass, "read", "([BII)I");
    wrapper.isSkipMethod = (*env)->GetMethodID(env, inputStreamClass, "skip", "(J)J");
    wrapper.tmp = (jbyteArray) tmpArray;
    wrapper.tmpSize = (*env)->GetArrayLength(env, tmpArray);
    
    return wrapper;
}

static jobject createJmeImage(JNIEnv* env, int width, int height, int comps, char* data)
{
    // Convert # of components to jME format.
    jclass formatClass = (*env)->FindClass(env, "com/jme3/texture/Image$Format");
    jfieldID formatFieldID;
    
    switch (comps)
    {
        case 1:
            formatFieldID = (*env)->GetStaticFieldID(env, formatClass, 
                                    "Luminance8", "Lcom/jme3/texture/Image$Format;");
            break;
        case 2:
            formatFieldID = (*env)->GetStaticFieldID(env, formatClass, 
                                    "Luminance8Alpha8", "Lcom/jme3/texture/Image$Format;");
            break;
        case 3:
            formatFieldID = (*env)->GetStaticFieldID(env, formatClass, 
                                    "RGB8", "Lcom/jme3/texture/Image$Format;");
            break;
        case 4:
            formatFieldID = (*env)->GetStaticFieldID(env, formatClass, 
                                    "RGBA8", "Lcom/jme3/texture/Image$Format;");
            break;
        default:
            throwIOException(env, "Unrecognized number of components");
            return NULL;
    }
    
    jobject formatVal = (*env)->GetStaticObjectField(env, formatClass, formatFieldID);
    
    // Get colorspace sRGB
    jclass colorSpaceClass = (*env)->FindClass(env, "com/jme3/texture/image/ColorSpace");
    jfieldID sRGBFieldID = (*env)->GetStaticFieldID(env, colorSpaceClass, 
                                    "sRGB", "Lcom/jme3/texture/image/ColorSpace;");
    jobject sRGBVal = (*env)->GetStaticObjectField(env, colorSpaceClass, sRGBFieldID);
    
    int size = width * height * comps;
    
    // Stick it in a ByteBuffer
    jobject directBuffer = (*env)->NewDirectByteBuffer(env, data, size);
    
    if (directBuffer == NULL)
    {
        throwIOException(env, "Failed to allocate ByteBuffer");
        return NULL;
    }
    
    // Create JME image.
    jclass jmeImageClass   = (*env)->FindClass(env, "com/jme3/texture/Image");
    
    // Image(Format format, int width, int height, ByteBuffer data, ColorSpace colorSpace)
    jmethodID newImageMethod = (*env)->GetMethodID(env, jmeImageClass, "<init>", 
                                                   "(Lcom/jme3/texture/Image$Format;IILjava/nio/ByteBuffer;Lcom/jme3/texture/image/ColorSpace;)V");
    
    jobject jmeImage = (*env)->NewObject(env, jmeImageClass, newImageMethod, 
                                         formatVal, (jint)width, (jint)height, 
                                         directBuffer, sRGBVal);
    
    return jmeImage;
}

static void flipImage(int scanline, int height, char* data)
{
    char tmp[scanline];
    
    for (int y = 0; y < height / 2; y++)
    {
        int oppY = height - y - 1;
        int yOff  = y * scanline;
        int oyOff = oppY * scanline;
        // Copy scanline at Y to tmp
        memcpy(tmp, &data[yOff], scanline);
        // Copy data at opposite Y to Y
        memcpy(&data[yOff], &data[oyOff], scanline);
        // Copy tmp to opposite Y
        memcpy(&data[oyOff], tmp, scanline);
    }
}

JNIEXPORT jobject JNICALL Java_com_jme3_texture_plugins_AndroidNativeImageLoader_load
  (JNIEnv * env, jobject thisObj, jobject inputStream, jboolean flipY, jbyteArray tmpArray)
{
    JavaInputStreamWrapper wrapper = createInputStreamWrapper(env, inputStream, tmpArray);
    stbi_uc* imageData;
    int width, height, comps;
    
    LOGI("stbi_load_from_callbacks");
    
    imageData = stbi_load_from_callbacks(&JavaInputStreamCallbacks, &wrapper, &width, &height, &comps, STBI_default);
    
    if ((*env)->ExceptionCheck(env))
    {
        // IOException
        goto problems;
    }
    else if (wrapper.errorMsg != NULL)
    {
        // Misc error
        throwIOException(env, wrapper.errorMsg);
        goto problems;
    }
    else if (imageData == NULL)
    {
        // STBI error
        throwIOException(env, stbi_failure_reason());
        goto problems;
    }
    
    // No IOExceptions or errors encountered. We have image data!
    
    // Maybe we need to flip it.
    LOGI("Flipping image");
    if (flipY) 
    {
        flipImage(width * comps, height, imageData);
    }
    
    // Create the jME3 image.
    LOGI("Creating jME3 image");
    return createJmeImage(env, width, height, comps, imageData);
    
problems:
    if (imageData != NULL)
    {
        stbi_image_free(imageData);
    }

    return NULL;
}
