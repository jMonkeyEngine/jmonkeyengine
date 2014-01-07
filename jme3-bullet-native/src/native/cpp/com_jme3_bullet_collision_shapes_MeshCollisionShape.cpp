/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
* Author: Normen Hansen
*/
#include "com_jme3_bullet_collision_shapes_MeshCollisionShape.h"
#include "jmeBulletUtil.h"
#include "BulletCollision/CollisionShapes/btBvhTriangleMeshShape.h"
#include "btBulletDynamicsCommon.h"
#include "BulletCollision/Gimpact/btGImpactShape.h"


#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_collision_shapes_MeshCollisionShape
     * Method:    createShape
     * Signature: (J)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_collision_shapes_MeshCollisionShape_createShape
    (JNIEnv* env, jobject object,jboolean isMemoryEfficient,jboolean buildBVH, jlong arrayId) {
        jmeClasses::initJavaClasses(env);
        btTriangleIndexVertexArray* array = reinterpret_cast<btTriangleIndexVertexArray*>(arrayId);
        btBvhTriangleMeshShape* shape = new btBvhTriangleMeshShape(array, isMemoryEfficient, buildBVH);
        return reinterpret_cast<jlong>(shape);
    }

    JNIEXPORT jbyteArray JNICALL Java_com_jme3_bullet_collision_shapes_MeshCollisionShape_saveBVH(JNIEnv* env, jobject, jlong meshobj){
        btBvhTriangleMeshShape* mesh = reinterpret_cast<btBvhTriangleMeshShape*>(meshobj);
         btOptimizedBvh* bvh = mesh->getOptimizedBvh();
       unsigned int ssize = bvh->calculateSerializeBufferSize();
       char* buffer = (char*)btAlignedAlloc(ssize, 16);
       bool success = bvh->serialize(buffer, ssize, true);
    if(!success){
      jclass newExc = env->FindClass("java/lang/RuntimeException");
      env->ThrowNew(newExc, "Unableto Serialize, native error reported");
    }

         jbyteArray byteArray = env->NewByteArray(ssize);
         env->SetByteArrayRegion(byteArray, 0, ssize , (jbyte*) buffer);
   btAlignedFree(buffer);
         return byteArray;
    };

    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_collision_shapes_MeshCollisionShape_setBVH(JNIEnv* env, jobject,jbyteArray bytearray,jlong meshobj){
        int len = env->GetArrayLength (bytearray);
        void* buffer = btAlignedAlloc(len, 16);
        env->GetByteArrayRegion (bytearray, 0, len, reinterpret_cast<jbyte*>(buffer));

  btOptimizedBvh* bhv = btOptimizedBvh::deSerializeInPlace(buffer, len, true);
  btBvhTriangleMeshShape* mesh = reinterpret_cast<btBvhTriangleMeshShape*>(meshobj);
  mesh->setOptimizedBvh(bhv);
  return reinterpret_cast<jlong>(buffer);
    };

    /*
     * Class:     com_jme3_bullet_collision_shapes_MeshCollisionShape
     * Method:    finalizeNative
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_shapes_MeshCollisionShape_finalizeNative
    (JNIEnv* env, jobject object, jlong arrayId,jlong nativeBVHBuffer){
        btTriangleIndexVertexArray* array = reinterpret_cast<btTriangleIndexVertexArray*>(arrayId);
        delete(array);
  if (nativeBVHBuffer > 0) {
    void* buffer = reinterpret_cast<void*>(nativeBVHBuffer);
    btAlignedFree(buffer);
  }
    }


#ifdef __cplusplus
}
#endif

