/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
#include "com_jme3_bullet_collision_shapes_CollisionShape.h"
#include "jmeBulletUtil.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_collision_shapes_CollisionShape
     * Method:    getMargin
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_collision_shapes_CollisionShape_getMargin
    (JNIEnv * env, jobject object, jlong shapeId) {
        btCollisionShape* shape = reinterpret_cast<btCollisionShape*>(shapeId);
        if (shape == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return shape->getMargin();
    }

    /*
     * Class:     com_jme3_bullet_collision_shapes_CollisionShape
     * Method:    setLocalScaling
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_shapes_CollisionShape_setLocalScaling
    (JNIEnv * env, jobject object, jlong shapeId, jobject scale) {
        btCollisionShape* shape = reinterpret_cast<btCollisionShape*>(shapeId);
        if (shape == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 scl = btVector3();
        jmeBulletUtil::convert(env, scale, &scl);
        shape->setLocalScaling(scl);
    }

    /*
     * Class:     com_jme3_bullet_collision_shapes_CollisionShape
     * Method:    setMargin
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_shapes_CollisionShape_setMargin
    (JNIEnv * env, jobject object, jlong shapeId, jfloat newMargin) {
        btCollisionShape* shape = reinterpret_cast<btCollisionShape*>(shapeId);
        if (shape == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        shape->setMargin(newMargin);
    }

    /*
     * Class:     com_jme3_bullet_collision_shapes_CollisionShape
     * Method:    finalizeNative
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_shapes_CollisionShape_finalizeNative
    (JNIEnv * env, jobject object, jlong shapeId) {
        btCollisionShape* shape = reinterpret_cast<btCollisionShape*>(shapeId);
        if (shape == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        delete(shape);
    }
#ifdef __cplusplus
}
#endif
