#import <stdlib.h>
#define __LP64__ 1
#import <jni.h>
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#import <OpenGLES/ES3/gl.h>
#import <OpenGLES/ES3/glext.h>

/**
 * Author: Kostyantyn Hushchyn, Jesus Oliver
 */

#ifndef JNIEXPORT
#define JNIEXPORT __attribute__ ((visibility("default"))) \
  __attribute__ ((used))
#endif

#ifndef _Included_JmeIosGLES
#define _Included_JmeIosGLES
#endif

#define glBindVertexArray glBindVertexArrayOES

static int initialized = 0;

static jclass bufferClass = (jclass)0;
static jclass byteBufferClass = (jclass)0;
static jclass shortBufferClass = (jclass)0;
static jclass intBufferClass = (jclass)0;
static jclass floatBufferClass = (jclass)0;
static jfieldID positionID;
static jfieldID limitID;


static void
nativeClassInit(JNIEnv *e);

static int
allowIndirectBuffers(JNIEnv *e);

static void *
getDirectBufferPointer(JNIEnv *e, jobject buffer);

static void *
getPointer(JNIEnv *e, jobject buffer, jarray *array, jint *remaining, jint *offset);

static void
releasePointer(JNIEnv *e, jarray array, void *data, jboolean commit);

static void
jniThrowException(JNIEnv *e, const char* type, const char* message);

static jint
getBufferElementSize(JNIEnv *e, jobject buffer);

static int getNeededCount(GLint pname);

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glActiveTexture(JNIEnv* e, jobject c, jint texture) {
    glActiveTexture(
        (GLenum)texture
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glAttachShader(JNIEnv* e, jobject c, jint program, jint shader) {
    glAttachShader(
        (GLuint)program,
        (GLuint)shader
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glBindBuffer(JNIEnv* e, jobject c, jint target, jint buffer) {
    glBindBuffer(
        (GLenum)target,
        (GLuint)buffer
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glBindFramebuffer(JNIEnv* e, jobject c, jint target, jint framebuffer) {
    glBindFramebuffer(
        (GLenum)target,
        (GLuint)framebuffer
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glBindRenderbuffer(JNIEnv* e, jobject c, jint target, jint renderbuffer) {
    glBindRenderbuffer(
        (GLenum)target,
        (GLuint)renderbuffer
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glBindTexture(JNIEnv* e, jobject c, jint target, jint texture) {
    glBindTexture(
        (GLenum)target,
        (GLuint)texture
    );
}

 // TODO: Investigate this
 /*
JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glBindVertexArray(JNIEnv* e, jobject c, jint array) {
	glBindVertexArray(array);
}
*/

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glBlendFunc(JNIEnv* e, jobject c, jint sfactor, jint dfactor) {
    glBlendFunc(
        (GLenum)sfactor,
        (GLenum)dfactor
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glBufferData(JNIEnv* e, jobject c, jint target, jint size, jobject data_buf, jint usage) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *data = (GLvoid *) 0;

    if (data_buf) {
        data = (GLvoid *)getPointer(e, data_buf, &_array, &_remaining, &_bufferOffset);
        if (_remaining < size) {
            _exception = 1;
            _exceptionType = "java/lang/IllegalArgumentException";
            _exceptionMessage = "remaining() < size < needed";
            goto exit;
        }
    }
    if (data_buf && data == NULL) {
        char * _dataBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        data = (GLvoid *) (_dataBase + _bufferOffset);
    }
    glBufferData(
        (GLenum)target,
        (GLsizeiptr)size,
        (GLvoid *)data,
        (GLenum)usage
    );

exit:
    if (_array) {
        releasePointer(e, _array, data, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glBufferData2(JNIEnv* e, jobject c, jint target, jint size, jbyteArray data, jint offset, jint usage) {
	jbyte *dataNative = (*e)->GetByteArrayElements(e, data, NULL);
	
    glBufferData(
        (GLenum)target,
        (GLsizeiptr)size,
        (GLvoid *)dataNative,
        (GLenum)(usage + offset)
    );
	
	(*e)->ReleaseByteArrayElements(e, data, dataNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glBufferSubData(JNIEnv* e, jobject c, jint target, jint offset, jint size, jobject data_buf) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *data = (GLvoid *) 0;

    data = (GLvoid *)getPointer(e, data_buf, &_array, &_remaining, &_bufferOffset);
    if (_remaining < size) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "remaining() < size < needed";
        goto exit;
    }
    if (data == NULL) {
        char * _dataBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        data = (GLvoid *) (_dataBase + _bufferOffset);
    }
    glBufferSubData(
        (GLenum)target,
        (GLintptr)offset,
        (GLsizeiptr)size,
        (GLvoid *)data
    );

exit:
    if (_array) {
        releasePointer(e, _array, data, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glBufferSubData2(JNIEnv* e, jobject c, jint target, jint offset, jint size, jbyteArray data, jint dataoffset) {
	jbyte *dataNative = (*e)->GetByteArrayElements(e, data, NULL);
	
    glBufferSubData(
        (GLenum)target,
        (GLintptr)offset,
        (GLsizeiptr)size,
        (GLvoid *)(dataNative + dataoffset)
    );
	
	(*e)->ReleaseByteArrayElements(e, data, dataNative, 0);
}

JNIEXPORT jint JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glCheckFramebufferStatus(JNIEnv* e, jobject c, jint target) {
    GLenum _returnValue;
    _returnValue = glCheckFramebufferStatus(
        (GLenum)target
    );
    return (jint)_returnValue;
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glClear(JNIEnv* e, jobject c, jint mask) {
    glClear(
        (GLbitfield)mask
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glClearColor(JNIEnv* e, jobject c, jfloat red, jfloat green, jfloat blue, jfloat alpha) {
    glClearColor(
        (GLclampf)red,
        (GLclampf)green,
        (GLclampf)blue,
        (GLclampf)alpha
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glColorMask(JNIEnv* e, jobject c, jboolean red, jboolean green, jboolean blue, jboolean alpha) {
    glColorMask(
        (GLboolean)red,
        (GLboolean)green,
        (GLboolean)blue,
        (GLboolean)alpha
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glCompileShader(JNIEnv* e, jobject c, jint shader) {
    glCompileShader(
        (GLuint)shader
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glCompressedTexImage2D(JNIEnv* e, jobject c, jint target, jint level, jint internalformat, jint width, jint height, jint border, jint imageSize, jobject pixels_buf) {
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *pixels = (GLvoid *) 0;

    if (pixels_buf) {
        pixels = (GLvoid *)getPointer(e, pixels_buf, &_array, &_remaining, &_bufferOffset);
    }
    if (pixels_buf && pixels == NULL) {
        char * _pixelsBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        pixels = (GLvoid *) (_pixelsBase + _bufferOffset);
    }
    glCompressedTexImage2D(
        (GLenum)target,
        (GLint)level,
        (GLenum)internalformat,
        (GLsizei)width,
        (GLsizei)height,
        (GLint)border,
        (GLsizei)imageSize,
        (GLvoid *)pixels
    );
    if (_array) {
        releasePointer(e, _array, pixels, JNI_FALSE);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glCompressedTexSubImage2D(JNIEnv* e, jobject c, jint target, jint level, jint xoffset, jint yoffset, jint width, jint height, jint format, jint imageSize, jobject pixels_buf) {
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *pixels = (GLvoid *) 0;

    if (pixels_buf) {
        pixels = (GLvoid *)getPointer(e, pixels_buf, &_array, &_remaining, &_bufferOffset);
    }
    if (pixels_buf && pixels == NULL) {
        char * _pixelsBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        pixels = (GLvoid *) (_pixelsBase + _bufferOffset);
    }
    glCompressedTexSubImage2D(
        (GLenum)target,
        (GLint)level,
        (GLint)xoffset,
        (GLint)yoffset,
        (GLsizei)width,
        (GLsizei)height,
        (GLenum)format,
        (GLsizei)imageSize,
        (GLvoid *)pixels
    );
    if (_array) {
        releasePointer(e, _array, pixels, JNI_FALSE);
    }
}

JNIEXPORT jint JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glCreateProgram(JNIEnv* e, jobject c) {
    GLuint _returnValue;
    _returnValue = glCreateProgram();
    return (jint)_returnValue;
}

JNIEXPORT jint JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glCreateShader(JNIEnv* e, jobject c, jint shaderType) {
    GLuint _returnValue;
    _returnValue = glCreateShader(
        (GLenum)shaderType
    );
    return (jint)_returnValue;
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glCullFace(JNIEnv* e, jobject c, jint mode) {
    glCullFace(
        (GLenum)mode
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDeleteBuffers(JNIEnv* e, jobject c, jint n, jintArray buffers, jint offset) {
	jint *buffersNative = (*e)->GetIntArrayElements(e, buffers, NULL);
	
    glDeleteBuffers(
        (GLsizei)n,
        (GLuint *)buffersNative
    );
	
	(*e)->ReleaseIntArrayElements(e, buffers, buffersNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDeleteFramebuffers(JNIEnv* e, jobject c, jint n, jintArray framebuffers, jint offset) {
	jint *buffersNative = (*e)->GetIntArrayElements(e, framebuffers, NULL);
	
    glDeleteFramebuffers(
        (GLsizei)n,
        (GLuint *)buffersNative
    );
	
	(*e)->ReleaseIntArrayElements(e, framebuffers, buffersNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDeleteProgram(JNIEnv* e, jobject c, jint program) {
    glDeleteProgram(
        (GLuint)program
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDeleteRenderbuffers(JNIEnv* e, jobject c, jint n, jintArray renderbuffers, jint offset) {
	jint *buffersNative = (*e)->GetIntArrayElements(e, renderbuffers, NULL);
	
    glDeleteRenderbuffers(
        (GLsizei)n,
        (GLuint *)buffersNative
    );
	
	(*e)->ReleaseIntArrayElements(e, renderbuffers, buffersNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDeleteShader(JNIEnv* e, jobject c, jint shader) {
    glDeleteShader(
        (GLuint)shader
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDeleteTextures(JNIEnv* e, jobject c, jint n, jintArray textures_ref, jint offset) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    GLuint *textures_base = (GLuint *) 0;
    jint _remaining;
    GLuint *textures = (GLuint *) 0;

    if (!textures_ref) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "textures == null";
        goto exit;
    }
    if (offset < 0) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "offset < 0";
        goto exit;
    }
    _remaining = (*e)->GetArrayLength(e, textures_ref) - offset;
    if (_remaining < n) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "length - offset < n < needed";
        goto exit;
    }
    textures_base = (GLuint *)
        (*e)->GetPrimitiveArrayCritical(e, textures_ref, (jboolean *)0);
    textures = textures_base + offset;

    glDeleteTextures(
        (GLsizei)n,
        (GLuint *)textures
    );

exit:
    if (textures_base) {
        (*e)->ReleasePrimitiveArrayCritical(e, textures_ref, textures_base,
            JNI_ABORT);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDepthFunc(JNIEnv* e, jobject c, jint func) {
    glDepthFunc(
        (GLenum)func
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDepthMask(JNIEnv* e, jobject c, jboolean flag) {
    glDepthMask(
        (GLboolean)flag
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDepthRangef(JNIEnv* e, jobject c, jfloat zNear, jfloat zFar) {
    glDepthRangef(
        (GLclampf)zNear,
        (GLclampf)zFar
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDetachShader(JNIEnv* e, jobject c, jint program, jint shader) {
    glDetachShader(
        (GLuint)program,
        (GLuint)shader
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDisable(JNIEnv* e, jobject c, jint cap) {
    glDisable(
        (GLenum)cap
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDisableVertexAttribArray(JNIEnv* e, jobject c, jint index) {
    glDisableVertexAttribArray(
        (GLuint)index
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDrawArrays(JNIEnv* e, jobject c, jint mode, jint first, jint count) {
    glDrawArrays(
        (GLenum)mode,
        (GLint)first,
        (GLsizei)count
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDrawElements(JNIEnv* e, jobject c, jint mode, jint count, jint type, jobject indices_buf) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *indices = (GLvoid *) 0;

    indices = (GLvoid *)getPointer(e, indices_buf, &_array, &_remaining, &_bufferOffset);
    if (_remaining < count) {
        _exception = 1;
        _exceptionType = "java/lang/ArrayIndexOutOfBoundsException";
        _exceptionMessage = "remaining() < count < needed";
        goto exit;
    }
    if (indices == NULL) {
        char * _indicesBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        indices = (GLvoid *) (_indicesBase + _bufferOffset);
    }
    glDrawElements(
        (GLenum)mode,
        (GLsizei)count,
        (GLenum)type,
        (GLvoid *)indices
    );

exit:
    if (_array) {
        releasePointer(e, _array, indices, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDrawElements2(JNIEnv* e, jobject c, jint mode, jint count, jint type, jbyteArray indices, jint offset) {
	jbyte *indicesNative = (*e)->GetByteArrayElements(e, indices, NULL);
	
    glDrawElements(
        (GLenum)mode,
        (GLsizei)count,
        (GLenum)type,
        (GLvoid *)(indicesNative + offset)
    );
	
	(*e)->ReleaseByteArrayElements(e, indices, indicesNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glDrawElementsIndex(JNIEnv* e, jobject c, jint mode, jint count, jint type, jint offset) {
    glDrawElements(
        (GLenum)mode,
        (GLsizei)count,
        (GLenum)type,
        (GLvoid *)offset
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glEnable(JNIEnv* e, jobject c, jint cap) {
    glEnable(
        (GLenum)cap
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glEnableVertexAttribArray(JNIEnv* e, jobject c, jint index) {
    glEnableVertexAttribArray(
        (GLuint)index
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glFramebufferRenderbuffer(JNIEnv* e, jobject c, jint target, jint attachment, jint renderbuffertarget, jint renderbuffer) {
    glFramebufferRenderbuffer(
        (GLenum)target,
        (GLenum)attachment,
        (GLenum)renderbuffertarget,
        (GLuint)renderbuffer
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glFramebufferTexture2D(JNIEnv* e, jobject c, jint target, jint attachment, jint textarget, jint texture, jint level) {
    glFramebufferTexture2D(
        (GLenum)target,
        (GLenum)attachment,
        (GLenum)textarget,
        (GLuint)texture,
        (GLint)level
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGenBuffers(JNIEnv* e, jobject c, jint n, jintArray buffers_ref, jint offset) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    GLuint *buffers_base = (GLuint *) 0;
    jint _remaining;
    GLuint *buffers = (GLuint *) 0;

    if (!buffers_ref) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "buffers == null";
        goto exit;
    }
    if (offset < 0) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "offset < 0";
        goto exit;
    }
    _remaining = (*e)->GetArrayLength(e, buffers_ref) - offset;
    if (_remaining < n) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "length - offset < n < needed";
        goto exit;
    }
    buffers_base = (GLuint *)
        (*e)->GetPrimitiveArrayCritical(e, buffers_ref, (jboolean *)0);
    buffers = buffers_base + offset;

    glGenBuffers(
        (GLsizei)n,
        (GLuint *)buffers
    );

exit:
    if (buffers_base) {
        (*e)->ReleasePrimitiveArrayCritical(e, buffers_ref, buffers_base,
            _exception ? JNI_ABORT: 0);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGenFramebuffers(JNIEnv* e, jobject c, jint n, jintArray framebuffers_ref, jint offset) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    GLuint *buffers_base = (GLuint *) 0;
    jint _remaining;
    GLuint *buffers = (GLuint *) 0;

    if (!framebuffers_ref) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "buffers == null";
        goto exit;
    }
    if (offset < 0) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "offset < 0";
        goto exit;
    }
    _remaining = (*e)->GetArrayLength(e, framebuffers_ref) - offset;
    if (_remaining < n) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "length - offset < n < needed";
        goto exit;
    }
    buffers_base = (GLuint *)
        (*e)->GetPrimitiveArrayCritical(e, framebuffers_ref, (jboolean *)0);
    buffers = buffers_base + offset;

    glGenFramebuffers(
        (GLsizei)n,
        (GLuint *)buffers
    );

exit:
    if (buffers_base) {
        (*e)->ReleasePrimitiveArrayCritical(e, framebuffers_ref, buffers_base,
            _exception ? JNI_ABORT: 0);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGenRenderbuffers(JNIEnv* e, jobject c, jint n, jintArray renderbuffers_ref, jint offset) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    GLuint *buffers_base = (GLuint *) 0;
    jint _remaining;
    GLuint *buffers = (GLuint *) 0;

    if (!renderbuffers_ref) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "buffers == null";
        goto exit;
    }
    if (offset < 0) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "offset < 0";
        goto exit;
    }
    _remaining = (*e)->GetArrayLength(e, renderbuffers_ref) - offset;
    if (_remaining < n) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "length - offset < n < needed";
        goto exit;
    }
    buffers_base = (GLuint *)
        (*e)->GetPrimitiveArrayCritical(e, renderbuffers_ref, (jboolean *)0);
    buffers = buffers_base + offset;

    glGenRenderbuffers(
        (GLsizei)n,
        (GLuint *)buffers
    );

exit:
    if (buffers_base) {
        (*e)->ReleasePrimitiveArrayCritical(e, renderbuffers_ref, buffers_base,
            _exception ? JNI_ABORT: 0);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGenTextures(JNIEnv* e, jobject c, jint n, jintArray textures_ref, jint offset) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    GLuint *buffers_base = (GLuint *) 0;
    jint _remaining;
    GLuint *buffers = (GLuint *) 0;

    if (!textures_ref) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "buffers == null";
        goto exit;
    }
    if (offset < 0) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "offset < 0";
        goto exit;
    }
    _remaining = (*e)->GetArrayLength(e, textures_ref) - offset;
    if (_remaining < n) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "length - offset < n < needed";
        goto exit;
    }
    buffers_base = (GLuint *)
        (*e)->GetPrimitiveArrayCritical(e, textures_ref, (jboolean *)0);
    buffers = buffers_base + offset;

    glGenTextures(
        (GLsizei)n,
        (GLuint *)buffers
    );

exit:
    if (buffers_base) {
        (*e)->ReleasePrimitiveArrayCritical(e, textures_ref, buffers_base,
            _exception ? JNI_ABORT: 0);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGenerateMipmap(JNIEnv* e, jobject c, jint target) {
    glGenerateMipmap(
        (GLenum)target
    );
}

JNIEXPORT jint JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGetAttribLocation(JNIEnv* e, jobject c, jint program, jstring name) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    GLint _returnValue = 0;
    const char* _nativename = 0;

    if (!name) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "name == null";
        goto exit;
    }
    _nativename = (*e)->GetStringUTFChars(e, name, 0);

    _returnValue = glGetAttribLocation(
        (GLuint)program,
        (char *)_nativename
    );

exit:
    if (_nativename) {
        (*e)->ReleaseStringUTFChars(e, name, _nativename);
    }

    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
    return (jint)_returnValue;
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGetBoolean(JNIEnv* e, jobject c, jint pname, jobject params_buf) {
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *params = (GLvoid *) 0;

    if (params_buf) {
        params = (GLvoid *)getPointer(e, params_buf, &_array, &_remaining, &_bufferOffset);
    }
    if (params_buf && params == NULL) {
        char * _paramsBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        params = (GLvoid *) (_paramsBase + _bufferOffset);
    }
      
    glGetBooleanv(
        (GLenum) pname,
        (GLboolean *) params
    );
}


JNIEXPORT jint JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGetError(JNIEnv* e, jobject c) {
    GLenum _returnValue;
    _returnValue = glGetError();
    return (jint)_returnValue;
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGetFramebufferAttachmentParameteriv(JNIEnv* e, jobject c, jint target, jint attachment, jint pname, jintArray params_ref, jint offset) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    GLint *params_base = (GLint *) 0;
    jint _remaining;
    GLint *params = (GLint *) 0;

    if (!params_ref) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "params == null";
        goto exit;
    }
    if (offset < 0) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "offset < 0";
        goto exit;
    }
    _remaining = (*e)->GetArrayLength(e, params_ref) - offset;
    params_base = (GLint *)
        (*e)->GetPrimitiveArrayCritical(e, params_ref, (jboolean *)0);
    params = params_base + offset;

    glGetFramebufferAttachmentParameteriv(
        (GLenum)target,
        (GLenum)attachment,
        (GLenum)pname,
        (GLint *)params
    );

exit:
    if (params_base) {
        (*e)->ReleasePrimitiveArrayCritical(e, params_ref, params_base,
            _exception ? JNI_ABORT: 0);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGetIntegerv(JNIEnv* e, jobject c, jint pname, jintArray params_ref, jint offset) {
    jint _exception = 0;
    const char * _exceptionType;
    const char * _exceptionMessage;
    GLint *params_base = (GLint *) 0;
    jint _remaining;
    GLint *params = (GLint *) 0;
    int _needed = 0;

    if (!params_ref) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "params == null";
        goto exit;
    }
    if (offset < 0) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "offset < 0";
        goto exit;
    }
    _remaining = (*e)->GetArrayLength(e, params_ref) - offset;
    _needed = getNeededCount(pname);
    // if we didn't find this pname, we just assume the user passed
    // an array of the right size -- this might happen with extensions
    // or if we forget an enum here.
    if (_remaining < _needed) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "length - offset < needed";
        goto exit;
    }
    params_base = (GLint *)
        (*e)->GetPrimitiveArrayCritical(e, params_ref, (jboolean *)0);
    params = params_base + offset;

    glGetIntegerv(
        (GLenum)pname,
        (GLint *)params
    );

exit:
    if (params_base) {
        (*e)->ReleasePrimitiveArrayCritical(e, params_ref, params_base,
            _exception ? JNI_ABORT: 0);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT jstring JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGetProgramInfoLog(JNIEnv* e, jobject c, jint program) {
	GLsizei size = 0;
	glGetProgramiv((GLuint)program, GL_INFO_LOG_LENGTH, &size);
	
	GLchar *infoLog;

	if (!size) {
		return  (*e)->NewStringUTF(e, "");
	}
	
	infoLog = malloc(sizeof(GLchar) * size);
    if (infoLog == NULL) {
        jniThrowException(e, "java/lang/IllegalArgumentException", "out of memory");
        return NULL;
    }
	
	glGetProgramInfoLog((GLuint)program, size, NULL, infoLog);
	jstring log = (*e)->NewStringUTF(e, infoLog);
	free(infoLog);

	return log; 
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGetProgramiv(JNIEnv* e, jobject c, jint program, jint pname, jintArray params_ref, jint offset) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    GLint *params_base = (GLint *) 0;
    jint _remaining;
    GLint *params = (GLint *) 0;

    if (!params_ref) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "params == null";
        goto exit;
    }
    if (offset < 0) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "offset < 0";
        goto exit;
    }
    _remaining = (*e)->GetArrayLength(e, params_ref) - offset;
    if (_remaining < 1) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "length - offset < 1 < needed";
        goto exit;
    }
    params_base = (GLint *)
        (*e)->GetPrimitiveArrayCritical(e, params_ref, (jboolean *)0);
    params = params_base + offset;

    glGetProgramiv(
        (GLuint)program,
        (GLenum)pname,
        (GLint *)params
    );

exit:
    if (params_base) {
        (*e)->ReleasePrimitiveArrayCritical(e, params_ref, params_base,
            _exception ? JNI_ABORT: 0);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT jstring JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGetShaderInfoLog(JNIEnv* e, jobject c, jint shader) {
	GLsizei size = 0;
	glGetShaderiv((GLuint)shader, GL_INFO_LOG_LENGTH, &size);
	
	GLchar *infoLog;

	if (!size) {
		return  (*e)->NewStringUTF(e, "");
	}
	
	infoLog = malloc(sizeof(GLchar) * size);
    if (infoLog == NULL) {
        jniThrowException(e, "java/lang/IllegalArgumentException", "out of memory");
        return NULL;
    }
	
	glGetShaderInfoLog((GLuint)shader, size, NULL, infoLog);
	jstring log = (*e)->NewStringUTF(e, infoLog);
	free(infoLog);

	return log; 
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGetShaderiv(JNIEnv* e, jobject c, jint shader, jint pname, jintArray params_ref, jint offset) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    GLint *params_base = (GLint *) 0;
    jint _remaining;
    GLint *params = (GLint *) 0;

    if (!params_ref) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "params == null";
        goto exit;
    }
    if (offset < 0) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "offset < 0";
        goto exit;
    }
    _remaining = (*e)->GetArrayLength(e, params_ref) - offset;
    if (_remaining < 1) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "length - offset < 1 < needed";
        goto exit;
    }
    params_base = (GLint *)
        (*e)->GetPrimitiveArrayCritical(e, params_ref, (jboolean *)0);
    params = params_base + offset;

    glGetShaderiv(
        (GLuint)shader,
        (GLenum)pname,
        (GLint *)params
    );

exit:
    if (params_base) {
        (*e)->ReleasePrimitiveArrayCritical(e, params_ref, params_base,
            _exception ? JNI_ABORT: 0);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT jstring JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGetString(JNIEnv* e, jobject c, jint name) {
	const GLubyte* value = glGetString((GLenum) name);

	return (*e)->NewStringUTF(e, (const char*)value); 
}

JNIEXPORT jint JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glGetUniformLocation(JNIEnv* e, jobject c, jint program, jstring name) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    GLint _returnValue = 0;
    const char* _nativename = 0;

    if (!name) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "name == null";
        goto exit;
    }
    _nativename = (*e)->GetStringUTFChars(e, name, 0);

    _returnValue = glGetUniformLocation(
        (GLuint)program,
        (char *)_nativename
    );

exit:
    if (_nativename) {
        (*e)->ReleaseStringUTFChars(e, name, _nativename);
    }

    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
    return (jint)_returnValue;
}

JNIEXPORT jboolean JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glIsEnabled(JNIEnv* e, jobject c, jint cap) {
    GLboolean _returnValue;
    _returnValue = glIsEnabled(
        (GLenum)cap
    );
    return (jboolean)_returnValue;
}

JNIEXPORT jboolean JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glIsFramebuffer(JNIEnv* e, jobject c, jint framebuffer) {
    GLboolean _returnValue;
    _returnValue = glIsFramebuffer(
        (GLuint)framebuffer
    );
    return (jboolean)_returnValue;
}

JNIEXPORT jboolean JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glIsRenderbuffer(JNIEnv* e, jobject c, jint renderbuffer) {
    GLboolean _returnValue;
    _returnValue = glIsRenderbuffer(
        (GLuint)renderbuffer
    );
    return (jboolean)_returnValue;
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glLineWidth(JNIEnv* e, jobject c, jfloat width) {
    glLineWidth(
        (GLfloat)width
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glLinkProgram(JNIEnv* e, jobject c, jint program) {
    glLinkProgram(
        (GLuint)program
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glPixelStorei(JNIEnv* e, jobject c, jint pname, jint param) {
    glPixelStorei(
        (GLenum)pname,
        (GLint)param
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glPolygonOffset(JNIEnv* e, jobject c, jfloat factor, jfloat units) {
    glPolygonOffset(
        (GLfloat)factor,
        (GLfloat)units
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glReadPixels(JNIEnv* e, jobject c, jint vpX, jint vpY, jint vpW, jint vpH, jint format, jint type, jobject pixels_buf) {
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *pixels = (GLvoid *) 0;

    pixels = (GLvoid *)getPointer(e, pixels_buf, &_array, &_remaining, &_bufferOffset);
    if (pixels == NULL) {
        char * _pixelsBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        pixels = (GLvoid *) (_pixelsBase + _bufferOffset);
    }
    glReadPixels(
        (GLint)vpX,
        (GLint)vpY,
        (GLsizei)vpW,
        (GLsizei)vpH,
        (GLenum)format,
        (GLenum)type,
        (GLvoid *)pixels
    );
    if (_array) {
        releasePointer(e, _array, pixels, JNI_TRUE);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glReadPixels2(JNIEnv* e, jobject c, jint vpX, jint vpY, jint vpW, jint vpH, jint format, jint type, jintArray pixels, jint offset, jint size) {
	GLint* bufferNative = malloc(size);
	
    glReadPixels(
        (GLint)vpX,
        (GLint)vpY,
        (GLsizei)vpW,
        (GLsizei)vpH,
        (GLenum)format,
        (GLenum)type,
        (GLvoid *)bufferNative
    );
	
	(*e)->SetIntArrayRegion(e, pixels, offset, size, bufferNative);
	
	free(bufferNative);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glRenderbufferStorage(JNIEnv* e, jobject c, jint target, jint internalformat, jint width, jint height) {
    glRenderbufferStorage(
        (GLenum)target,
        (GLenum)internalformat,
        (GLsizei)width,
        (GLsizei)height
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glScissor(JNIEnv* e, jobject c, jint x, jint y, jint width, jint height) {
    glScissor(
        (GLint)x,
        (GLint)y,
        (GLsizei)width,
        (GLsizei)height
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glShaderSource(JNIEnv* e, jobject c, jint shader, jstring string) {
	const char *stringNative = (*e)->GetStringUTFChars(e, string, NULL);
	glShaderSource(shader, 1, &stringNative, NULL);
	//jsize stringLen = (*e)->GetStringUTFLength(e, string);
	//const char** code = { stringNative };
	//const GLint* length = { stringLen };
	
	printf("upload shader source: %s", stringNative);

	//glShaderSource(shader, 1, code, length);
	
	(*e)->ReleaseStringUTFChars(e, string, stringNative);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glStencilFuncSeparate(JNIEnv* e, jobject c, jint face, jint func, jint ref, jint mask) {
    glStencilFuncSeparate(
        (GLenum) face,
        (GLenum) func,
        (GLint) ref,
        (GLuint) mask
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glStencilOpSeparate(JNIEnv* e, jobject c, jint face, jint sfail, jint dpfail, jint dppass) {
    glStencilOpSeparate(
        (GLenum) face,
        (GLenum) sfail,
        (GLenum) dpfail,
        (GLenum) dppass
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glTexImage2D(JNIEnv* e, jobject c, jint target, jint level, jint internalformat, jint width, jint height, jint border, jint format, jint type, jobject pixels_buf) {
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *pixels = (GLvoid *) 0;

    if (pixels_buf) {
        pixels = (GLvoid *)getPointer(e, pixels_buf, &_array, &_remaining, &_bufferOffset);
    }
    if (pixels_buf && pixels == NULL) {
        char * _pixelsBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        pixels = (GLvoid *) (_pixelsBase + _bufferOffset);
    }
    glTexImage2D(
        (GLenum)target,
        (GLint)level,
        (GLint)internalformat,
        (GLsizei)width,
        (GLsizei)height,
        (GLint)border,
        (GLenum)format,
        (GLenum)type,
        (GLvoid *)pixels
    );
    if (_array) {
        releasePointer(e, _array, pixels, JNI_FALSE);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glTexParameteri(JNIEnv* e, jobject c, jint target, jint pname, jint param) {
    glTexParameteri(
        (GLenum)target,
        (GLenum)pname,
        (GLint)param
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glTexParameterf(JNIEnv* e, jobject c, jint target, jint pname, jfloat param) {
    glTexParameterf(
        (GLenum)target,
        (GLenum)pname,
        (GLfloat)param
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glTexSubImage2D(JNIEnv* e, jobject c, jint target, jint level, jint xoffset, jint yoffset, jint width, jint height, jint format, jint type, jobject pixels_buf) {
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *pixels = (GLvoid *) 0;

    if (pixels_buf) {
        pixels = (GLvoid *)getPointer(e, pixels_buf, &_array, &_remaining, &_bufferOffset);
    }
    if (pixels_buf && pixels == NULL) {
        char * _pixelsBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        pixels = (GLvoid *) (_pixelsBase + _bufferOffset);
    }
    glTexSubImage2D(
        (GLenum)target,
        (GLint)level,
        (GLint)xoffset,
        (GLint)yoffset,
        (GLsizei)width,
        (GLsizei)height,
        (GLenum)format,
        (GLenum)type,
        (GLvoid *)pixels
    );
    if (_array) {
        releasePointer(e, _array, pixels, JNI_FALSE);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform1f(JNIEnv* e, jobject c, jint location, jfloat x) {
    glUniform1f(
        (GLint)location,
        (GLfloat)x
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform1fv(JNIEnv* e, jobject c, jint location, jint count, jobject v_buf) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLfloat *v = (GLfloat *) 0;

    v = (GLfloat *)getPointer(e, v_buf, &_array, &_remaining, &_bufferOffset);
    if (_remaining < count) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "remaining() < count < needed";
        goto exit;
    }
    if (v == NULL) {
        char * _vBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        v = (GLfloat *) (_vBase + _bufferOffset);
    }
    glUniform1fv(
        (GLint)location,
        (GLsizei)count,
        (GLfloat *)v
    );

exit:
    if (_array) {
        releasePointer(e, _array, v, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform1fv2(JNIEnv* e, jobject c, jint location, jint count, jfloatArray v, jint offset) {
	jfloat *vNative = (*e)->GetFloatArrayElements(e, v, NULL);
	
    glUniform1fv(
        (GLint)location,
        (GLsizei)count,
        (GLfloat *)(vNative + offset * sizeof(GLfloat))
    );
	
	(*e)->ReleaseFloatArrayElements(e, v, vNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform1i(JNIEnv* e, jobject c, jint location, jint x) {
    glUniform1i(
        (GLint)location,
        (GLint)x
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform1iv(JNIEnv* e, jobject c, jint location, jint count, jobject v_buf) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLint *v = (GLint *) 0;

    v = (GLint *)getPointer(e, v_buf, &_array, &_remaining, &_bufferOffset);
    if (_remaining < count) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "remaining() < count < needed";
        goto exit;
    }
    if (v == NULL) {
        char * _vBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        v = (GLint *) (_vBase + _bufferOffset);
    }
    glUniform1iv(
        (GLint)location,
        (GLsizei)count,
        (GLint *)v
    );

exit:
    if (_array) {
        releasePointer(e, _array, v, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform1iv2(JNIEnv* e, jobject c, jint location, jint count, jintArray v, jint offset) {
	jint *vNative = (*e)->GetIntArrayElements(e, v, NULL);
	
    glUniform1iv(
        (GLint)location,
        (GLsizei)count,
        (GLint *)(vNative + offset * sizeof(GLint))
    );
	glUniform1iv(location, count, vNative + offset);
	
	(*e)->ReleaseIntArrayElements(e, v, vNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform2f(JNIEnv* e, jobject c, jint location, jfloat x, jfloat y) {
    glUniform2f(
        (GLint)location,
        (GLfloat)x,
        (GLfloat)y
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform2fv(JNIEnv* e, jobject c, jint location, jint count, jobject v_buf) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLfloat *v = (GLfloat *) 0;

    v = (GLfloat *)getPointer(e, v_buf, &_array, &_remaining, &_bufferOffset);
    if (_remaining < count*2) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "remaining() < count*2 < needed";
        goto exit;
    }
    if (v == NULL) {
        char * _vBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        v = (GLfloat *) (_vBase + _bufferOffset);
    }
    glUniform2fv(
        (GLint)location,
        (GLsizei)count,
        (GLfloat *)v
    );

exit:
    if (_array) {
        releasePointer(e, _array, v, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform2fv2(JNIEnv* e, jobject c, jint location, jint count, jfloatArray v, jint offset) {
	jfloat *vNative = (*e)->GetFloatArrayElements(e, v, NULL);
	
    glUniform2fv(
        (GLint)location,
        (GLsizei)count,
        (GLfloat *)(vNative + offset * sizeof(GLfloat))
    );
	
	(*e)->ReleaseFloatArrayElements(e, v, vNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform3f(JNIEnv* e, jobject c, jint location, jfloat x, jfloat y, jfloat z) {
    glUniform3f(
        (GLint)location,
        (GLfloat)x,
        (GLfloat)y,
        (GLfloat)z
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform3fv(JNIEnv* e, jobject c, jint location, jint count, jobject v_buf) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLfloat *v = (GLfloat *) 0;

    v = (GLfloat *)getPointer(e, v_buf, &_array, &_remaining, &_bufferOffset);
    if (_remaining < count * 3) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "remaining() < count*3 < needed";
        goto exit;
    }
    if (v == NULL) {
        char * _vBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        v = (GLfloat *) (_vBase + _bufferOffset);
    }
    glUniform3fv(
        (GLint)location,
        (GLsizei)count,
        (GLfloat *)v
    );

exit:
    if (_array) {
        releasePointer(e, _array, v, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform3fv2(JNIEnv* e, jobject c, jint location, jint count, jfloatArray v, jint offset) {
	jfloat *vNative = (*e)->GetFloatArrayElements(e, v, NULL);
	
    glUniform3fv(
        (GLint)location,
        (GLsizei)count,
        (GLfloat *)(vNative + offset * sizeof(GLfloat))
    );
	
	(*e)->ReleaseFloatArrayElements(e, v, vNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform4f(JNIEnv* e, jobject c, jint location, jfloat x, jfloat y, jfloat z, jfloat w) {
    glUniform4f(
        (GLint)location,
        (GLfloat)x,
        (GLfloat)y,
        (GLfloat)z,
        (GLfloat)w
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform4fv(JNIEnv* e, jobject c, jint location, jint count, jobject v_buf) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLfloat *v = (GLfloat *) 0;

    v = (GLfloat *)getPointer(e, v_buf, &_array, &_remaining, &_bufferOffset);
    if (_remaining < count * 4) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "remaining() < count*4 < needed";
        goto exit;
    }
    if (v == NULL) {
        char * _vBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        v = (GLfloat *) (_vBase + _bufferOffset);
    }
    glUniform4fv(
        (GLint)location,
        (GLsizei)count,
        (GLfloat *)v
    );

exit:
    if (_array) {
        releasePointer(e, _array, v, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniform4fv2(JNIEnv* e, jobject c, jint location, jint count, jfloatArray v, jint offset) {
	jfloat *vNative = (*e)->GetFloatArrayElements(e, v, NULL);
	
    glUniform4fv(
        (GLint)location,
        (GLsizei)count,
        (GLfloat *)(vNative + offset * sizeof(GLfloat))
    );
	
	(*e)->ReleaseFloatArrayElements(e, v, vNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniformMatrix3fv(JNIEnv* e, jobject c, jint location, jint count, jboolean transpose, jobject value_buf) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLfloat *value = (GLfloat *) 0;

    value = (GLfloat *)getPointer(e, value_buf, &_array, &_remaining, &_bufferOffset);
    if (_remaining < count*9) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "remaining() < count*9 < needed";
        goto exit;
    }
    if (value == NULL) {
        char * _valueBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        value = (GLfloat *) (_valueBase + _bufferOffset);
    }
    glUniformMatrix3fv(
        (GLint)location,
        (GLsizei)count,
        (GLboolean)transpose,
        (GLfloat *)value
    );

exit:
    if (_array) {
        releasePointer(e, _array, value, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniformMatrix3fv2(JNIEnv* e, jobject c, jint location, jint count, jboolean transpose, jfloatArray value, jint offset) {
	jfloat *vNative = (*e)->GetFloatArrayElements(e, value, NULL);
	
    glUniformMatrix3fv(
        (GLint)location,
        (GLsizei)count,
        (GLboolean)transpose,
        (GLfloat *)(vNative + offset * sizeof(GLfloat))
    );
	
	(*e)->ReleaseFloatArrayElements(e, value, vNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniformMatrix4fv(JNIEnv* e, jobject c, jint location, jint count, jboolean transpose, jobject value_buf) {
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLfloat *value = (GLfloat *) 0;

    value = (GLfloat *)getPointer(e, value_buf, &_array, &_remaining, &_bufferOffset);
    if (_remaining < count * 16) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "remaining() < count*16 < needed";
        goto exit;
    }
    if (value == NULL) {
        char * _valueBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        value = (GLfloat *) (_valueBase + _bufferOffset);
    }
    glUniformMatrix4fv(
        (GLint)location,
        (GLsizei)count,
        (GLboolean)transpose,
        (GLfloat *)value
    );

exit:
    if (_array) {
        releasePointer(e, _array, value, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUniformMatrix4fv2(JNIEnv* e, jobject c, jint location, jint count, jboolean transpose, jfloatArray value, jint offset) {
	jfloat *vNative = (*e)->GetFloatArrayElements(e, value, NULL);
	
    glUniformMatrix4fv(
        (GLint)location,
        (GLsizei)count,
        (GLboolean)transpose,
        (GLfloat *)(vNative + offset * sizeof(GLfloat))
    );
	
	(*e)->ReleaseFloatArrayElements(e, value, vNative, 0);
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glUseProgram(JNIEnv* e, jobject c, jint program) {
    glUseProgram(
        (GLuint)program
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glVertexAttribPointer(JNIEnv* e, jobject c, jint indx, jint size, jint type, jboolean normalized, jint stride, jobject buffer) {
    GLvoid *ptr = (GLvoid *) 0;

    if (buffer) {
        ptr = (GLvoid *) getDirectBufferPointer(e, buffer);
        if (!ptr) {
            return;
        }
    }
    glVertexAttribPointer(
        (GLuint)indx,
        (GLint)size,
        (GLenum)type,
        (GLboolean)normalized,
        (GLsizei)stride,
        (GLvoid *)ptr
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glVertexAttribPointer2(JNIEnv* e, jclass c, jint indx, jint size, jint type, jboolean normalized, jint stride, jint offset) {
	
    glVertexAttribPointer(
        (GLuint)indx,
        (GLint)size,
        (GLenum)type,
        (GLboolean)normalized,
        (GLsizei)stride,
        (GLvoid *)(offset)
    );
}

JNIEXPORT void JNICALL
Java_com_jme3_renderer_ios_JmeIosGLES_glViewport(JNIEnv* e, jobject c, jint x, jint y, jint width, jint height) {
    glViewport(
        (GLint)x,
        (GLint)y,
        (GLsizei)width,
        (GLsizei)height
    );
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glBeginQuery(JNIEnv* e, jobject c, jint target, jint query) {
    glBeginQuery(
        (GLint) target,
        (GLint) query
    );
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glEndQuery(JNIEnv* e, jobject c, jint target)
{
    glEndQuery((GLint)target);
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glGenQueries(JNIEnv* e, jobject c, jint count, jobject v_buf)
{
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLint *v = (GLint *) 0;

    v = (GLint *)getPointer(e, v_buf, &_array, &_remaining, &_bufferOffset);
    if (_remaining < count) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "remaining() < count < needed";
        goto exit;
    }
    if (v == NULL) {
        char * _vBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        v = (GLint *) (_vBase + _bufferOffset);
    }
    glGenQueries(
        (GLsizei)count,
        (GLint *)v
    );

exit:
    if (_array) {
        releasePointer(e, _array, v, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glGetQueryObjectuiv(JNIEnv* e, jobject c, jint query, jint pname, jintArray params_ref)
{
    jint _exception = 0;
    const char * _exceptionType;
    const char * _exceptionMessage;
    GLint *params_base = (GLint *) 0;
    jint _remaining;
    GLint *params = (GLint *) 0;
    int _needed = 0;

    if (!params_ref) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "params == null";
        goto exit;
    }

    _remaining = (*e)->GetArrayLength(e, params_ref);
    _needed = getNeededCount(pname);
    // if we didn't find this pname, we just assume the user passed
    // an array of the right size -- this might happen with extensions
    // or if we forget an enum here.
    if (_remaining < _needed) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "length < needed";
        goto exit;
    }
    params_base = (GLint *)
        (*e)->GetPrimitiveArrayCritical(e, params_ref, (jboolean *)0);
    params = params_base;

    glGetQueryObjectuiv(
        (GLint)query,
        (GLenum)pname,
        (GLint *)params
    );

exit:
    if (params_base) {
        (*e)->ReleasePrimitiveArrayCritical(e, params_ref, params_base,
            _exception ? JNI_ABORT: 0);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glGetQueryiv(JNIEnv* e, jobject c, jint target, jint pname, jintArray params_ref)
{
    jint _exception = 0;
    const char * _exceptionType;
    const char * _exceptionMessage;
    GLint *params_base = (GLint *) 0;
    jint _remaining;
    GLint *params = (GLint *) 0;
    int _needed = 0;

    if (!params_ref) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "params == null";
        goto exit;
    }

    _remaining = (*e)->GetArrayLength(e, params_ref);
    _needed = getNeededCount(pname);
    // if we didn't find this pname, we just assume the user passed
    // an array of the right size -- this might happen with extensions
    // or if we forget an enum here.
    if (_remaining < _needed) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "length < needed";
        goto exit;
    }
    params_base = (GLint *)
        (*e)->GetPrimitiveArrayCritical(e, params_ref, (jboolean *)0);
    params = params_base;

    glGetQueryiv(
        (GLenum)target,
        (GLenum)pname,
        (GLint *)params
    );

exit:
    if (params_base) {
        (*e)->ReleasePrimitiveArrayCritical(e, params_ref, params_base,
            _exception ? JNI_ABORT: 0);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glBlitFramebuffer(JNIEnv* e, jobject c, jint srcX0, jint srcY0, jint srcX1, jint srcY1, jint dstX0, jint dstY0, jint dstX1, jint dstY1, jint mask, jint filter)
{
    glBlitFramebuffer( 	
        (GLint) srcX0,
        (GLint) srcY0,
        (GLint) srcX1,
        (GLint) srcY1,
        (GLint) dstX0,
        (GLint) dstY0,
        (GLint) dstX1,
        (GLint) dstY1,
        (GLbitfield) mask,
        (GLenum) filter
    );
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glDrawArraysInstanced(JNIEnv* e, jobject c, jint mode, jint first, jint count, jint primcount)
{
    glDrawArraysInstanced(
        (GLenum) mode,
        (GLint) first,
        (GLsizei) count,
        (GLsizei) primcount
    );
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glDrawBuffers(JNIEnv* e, jobject c, jint count, jobject v_buf)
{
    jint _exception = 0;
    const char * _exceptionType = NULL;
    const char * _exceptionMessage = NULL;
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLint *v = (GLint *) 0;

    v = (GLint *)getPointer(e, v_buf, &_array, &_remaining, &_bufferOffset);
    if (_remaining < count) {
        _exception = 1;
        _exceptionType = "java/lang/IllegalArgumentException";
        _exceptionMessage = "remaining() < count < needed";
        goto exit;
    }
    if (v == NULL) {
        char * _vBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        v = (GLint *) (_vBase + _bufferOffset);
    }
    glDrawBuffers(
        (GLsizei)count,
        (GLint *)v
    );

exit:
    if (_array) {
        releasePointer(e, _array, v, JNI_FALSE);
    }
    if (_exception) {
        jniThrowException(e, _exceptionType, _exceptionMessage);
    }
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glDrawElementsInstanced(JNIEnv* e, jobject c, jint mode, jint count, jint type, jlong indices, jint primcount)
{
    glDrawElementsInstanced(
        (GLenum) mode,
        (GLsizei) count,
        (GLenum) type,
        (const void *) indices,
        (GLsizei) primcount
    );
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glVertexAttribDivisor(JNIEnv* e, jobject c, jint index, jint divisor)
{
    glVertexAttribDivisor(
        (GLint) index,
        (GLint) divisor
    );
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glFramebufferTextureLayer(JNIEnv* e, jobject c, jint target, jint attachment, jint texture, jint level, jint layer)
{
    glFramebufferTextureLayer(
        (GLenum) target,
        (GLenum) attachment,
        (GLuint) texture,
        (GLint) level,
        (GLint) layer
    );
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glReadBuffer(JNIEnv* e, jobject c, jint src)
{
    glReadBuffer((GLenum) src);
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glCompressedTexImage3D(JNIEnv* e, jobject c, jint target, jint level, jint internalFormat, jint width, jint height, jint depth, jint border, jint imageSize, jobject pixels_buf)
{
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *pixels = (GLvoid *) 0;

    if (pixels_buf) {
        pixels = (GLvoid *)getPointer(e, pixels_buf, &_array, &_remaining, &_bufferOffset);
    }
    if (pixels_buf && pixels == NULL) {
        char * _pixelsBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        pixels = (GLvoid *) (_pixelsBase + _bufferOffset);
    }
        
    glCompressedTexImage3D(
        (GLenum) target,
        (GLint) level,
        (GLenum) internalFormat,
        (GLsizei) width,
        (GLsizei) height,
        (GLsizei) depth,
        (GLint) border,
        (GLsizei) imageSize,
        (GLvoid *)pixels
    );

    if (_array) {
        releasePointer(e, _array, pixels, JNI_FALSE);
    }
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glCompressedTexSubImage3D(JNIEnv* e, jobject c, jint target, jint level, jint xoffset, jint yoffset, jint zoffset, jint width, jint height, jint depth, jint format, jint imageSize, jobject pixels_buf)
{
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *pixels = (GLvoid *) 0;

    if (pixels_buf) {
        pixels = (GLvoid *)getPointer(e, pixels_buf, &_array, &_remaining, &_bufferOffset);
    }
    if (pixels_buf && pixels == NULL) {
        char * _pixelsBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        pixels = (GLvoid *) (_pixelsBase + _bufferOffset);
    }
        
    glCompressedTexSubImage3D(
        (GLenum) target,
        (GLint) level,
        (GLint) xoffset,
        (GLint) yoffset,
        (GLint) zoffset,
        (GLsizei) width,
        (GLsizei) height,
        (GLsizei) depth,
        (GLenum) format,
        (GLsizei) imageSize,
        (GLvoid *)pixels
    );

    if (_array) {
        releasePointer(e, _array, pixels, JNI_FALSE);
    }
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glTexImage3D(JNIEnv* e, jobject c, jint target, jint level, jint internalFormat, jint width, jint height, jint depth, jint border, jint format, jint type, jobject pixels_buf)
{
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *pixels = (GLvoid *) 0;

    if (pixels_buf) {
        pixels = (GLvoid *)getPointer(e, pixels_buf, &_array, &_remaining, &_bufferOffset);
    }
    if (pixels_buf && pixels == NULL) {
        char * _pixelsBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        pixels = (GLvoid *) (_pixelsBase + _bufferOffset);
    }
        
    glTexImage3D(
        (GLenum) target,
        (GLint) level,
        (GLint) internalFormat,
        (GLsizei) width,
        (GLsizei) height,
        (GLsizei) depth,
        (GLint) border,
        (GLenum) format,
        (GLenum) type,
        (GLvoid *)pixels
    );

    if (_array) {
        releasePointer(e, _array, pixels, JNI_FALSE);
    }
}

JNIEXPORT void JNICALL 
Java_com_jme3_renderer_ios_JmeIosGLES_glTexSubImage3D(JNIEnv* e, jobject c, jint target, jint level, jint xoffset, jint yoffset, jint zoffset, jint width, jint height, jint depth, jint format, jint type, jobject pixels_buf)
{
    jarray _array = (jarray) 0;
    jint _bufferOffset = (jint) 0;
    jint _remaining;
    GLvoid *pixels = (GLvoid *) 0;

    if (pixels_buf) {
        pixels = (GLvoid *)getPointer(e, pixels_buf, &_array, &_remaining, &_bufferOffset);
    }
    if (pixels_buf && pixels == NULL) {
        char * _pixelsBase = (char *)(*e)->GetPrimitiveArrayCritical(e, _array, (jboolean *) 0);
        pixels = (GLvoid *) (_pixelsBase + _bufferOffset);
    }
        
    glTexSubImage3D(
        (GLenum) target,
        (GLint) level,
        (GLint) xoffset,
        (GLint) yoffset,
        (GLint) zoffset,
        (GLsizei) width,
        (GLsizei) height,
        (GLsizei) depth,
        (GLenum) format,
        (GLenum) type,
        (GLvoid *)pixels
    );

    if (_array) {
        releasePointer(e, _array, pixels, JNI_FALSE);
    }
}


static int
allowIndirectBuffers(JNIEnv *e) {
    return 0;
}

static void *
getDirectBufferPointer(JNIEnv *e, jobject buffer) {
    if (!buffer) {
        return NULL;
    }
    
    if (!initialized) {
    	nativeClassInit(e);
    }

    void* buf = (*e)->GetDirectBufferAddress(e, buffer);
    if (buf) {
        jint position = (*e)->GetIntField(e, buffer, positionID);
        jint elementSizeShift = getBufferElementSize(e, buffer);
        buf = ((char*) buf) + (position << elementSizeShift);
    } else {
            jniThrowException(e, "java/lang/IllegalArgumentException",
                              "Must use a native order direct Buffer");
    }
    return buf;
}

static void *
getPointer(JNIEnv *e, jobject buffer, jarray *array, jint *remaining, jint *offset) {
    jint position;
    jint limit;
    jint elementSizeShift;
    jlong pointer;
    
    if (!buffer) {
        return NULL;
    }
    
    if (!initialized) {
    	nativeClassInit(e);
    }

    position = (*e)->GetIntField(e, buffer, positionID);
    limit = (*e)->GetIntField(e, buffer, limitID);
    elementSizeShift = getBufferElementSize(e, buffer);
    
    array = (void*) NULL;
    *remaining = (limit - position) << elementSizeShift; 
    *offset = position;
    
    return getDirectBufferPointer(e, buffer);
}


static void
nativeClassInit(JNIEnv *e) {
    if (!byteBufferClass) {
    	jclass byteBufferClassLocal = (*e)->FindClass(e, "java/nio/ByteBuffer");
    	byteBufferClass = (jclass) (*e)->NewGlobalRef(e, byteBufferClassLocal);
    }
    
    if (!shortBufferClass) {
    	jclass shortBufferClassLocal = (*e)->FindClass(e, "java/nio/ShortBuffer");
    	shortBufferClass = (jclass) (*e)->NewGlobalRef(e, shortBufferClassLocal);
    }
    
    if (!intBufferClass) {
    	jclass intBufferClassLocal = (*e)->FindClass(e, "java/nio/IntBuffer");
    	intBufferClass = (jclass) (*e)->NewGlobalRef(e, intBufferClassLocal);
    }
    
    if (!floatBufferClass) {
    	jclass floatBufferClassLocal = (*e)->FindClass(e, "java/nio/FloatBuffer");
    	floatBufferClass = (jclass) (*e)->NewGlobalRef(e, floatBufferClassLocal);
    }
    
    if (!bufferClass) {
        jclass bufferClassLocal = (*e)->FindClass(e, "java/nio/Buffer");
        bufferClass = (jclass) (*e)->NewGlobalRef(e, bufferClassLocal);
    }

    if (!positionID && bufferClass) {
	    positionID = (*e)->GetFieldID(e, bufferClass, "position", "I");
	}

    if (!limitID && bufferClass) {
	    limitID = (*e)->GetFieldID(e, bufferClass, "limit", "I");
	}

	initialized = floatBufferClass && bufferClass && shortBufferClass && byteBufferClass
			&& intBufferClass && positionID && limitID;
			
	printf("Initializion of java.nio.Buffer access functionality %s\n", initialized ? "succeeded" : "failed");
}

static void
releasePointer(JNIEnv *e, jarray array, void *data, jboolean commit) {
    (*e)->ReleasePrimitiveArrayCritical(e, array, data,
					   commit ? 0 : JNI_ABORT);
}

static void
jniThrowException(JNIEnv *e, const char* type, const char* message) {
	jclass excCls = (*e)->FindClass(e, type);
	if (excCls != 0) {
    	(*e)->ThrowNew(e, excCls, message);
    }
}

static jint
getBufferElementSize(JNIEnv *e, jobject buffer) {
    if (!buffer) {
        return 0;
    }

	if ((*e)->IsInstanceOf(e, buffer, floatBufferClass) == JNI_TRUE) {
		return 2;
	} else if ((*e)->IsInstanceOf(e, buffer, intBufferClass) == JNI_TRUE) {
		return 2;
	} else if ((*e)->IsInstanceOf(e, buffer, shortBufferClass) == JNI_TRUE) {
		return 1;
	}
	
	//TODO: check other buffer types
	return 0;
}

static int getNeededCount(GLint pname) {
    int needed = 1;
#ifdef GL_ES_VERSION_2_0
    // GLES 2.x pnames
    switch (pname) {
        case GL_ALIASED_LINE_WIDTH_RANGE:
        case GL_ALIASED_POINT_SIZE_RANGE:
            needed = 2;
            break;

        case GL_BLEND_COLOR:
        case GL_COLOR_CLEAR_VALUE:
        case GL_COLOR_WRITEMASK:
        case GL_SCISSOR_BOX:
        case GL_VIEWPORT:
            needed = 4;
            break;

        case GL_COMPRESSED_TEXTURE_FORMATS:
            glGetIntegerv(GL_NUM_COMPRESSED_TEXTURE_FORMATS, &needed);
            break;

        case GL_SHADER_BINARY_FORMATS:
            glGetIntegerv(GL_NUM_SHADER_BINARY_FORMATS, &needed);
            break;
    }
#endif

#ifdef GL_VERSION_ES_CM_1_1
    // GLES 1.x pnames
    switch (pname) {
        case GL_ALIASED_LINE_WIDTH_RANGE:
        case GL_ALIASED_POINT_SIZE_RANGE:
        case GL_DEPTH_RANGE:
        case GL_SMOOTH_LINE_WIDTH_RANGE:
        case GL_SMOOTH_POINT_SIZE_RANGE:
            needed = 2;
            break;

        case GL_CURRENT_NORMAL:
        case GL_POINT_DISTANCE_ATTENUATION:
            needed = 3;
            break;

        case GL_COLOR_CLEAR_VALUE:
        case GL_COLOR_WRITEMASK:
        case GL_CURRENT_COLOR:
        case GL_CURRENT_TEXTURE_COORDS:
        case GL_FOG_COLOR:
        case GL_LIGHT_MODEL_AMBIENT:
        case GL_SCISSOR_BOX:
        case GL_VIEWPORT:
            needed = 4;
            break;

        case GL_MODELVIEW_MATRIX:
        case GL_PROJECTION_MATRIX:
        case GL_TEXTURE_MATRIX:
            needed = 16;
            break;

        case GL_COMPRESSED_TEXTURE_FORMATS:
            glGetIntegerv(GL_NUM_COMPRESSED_TEXTURE_FORMATS, &needed);
            break;
    }
#endif
    return needed;
}
