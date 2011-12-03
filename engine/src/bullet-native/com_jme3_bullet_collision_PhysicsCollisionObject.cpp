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
#include "com_jme3_bullet_collision_PhysicsCollisionObject.h"
#include "jmeBulletUtil.h"
#include "jmePhysicsSpace.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_collision_PhysicsCollisionObject
     * Method:    attachCollisionShape
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionObject_attachCollisionShape
    (JNIEnv * env, jobject object, jlong objectId, jlong shapeId) {
        btCollisionObject* collisionObject = reinterpret_cast<btCollisionObject*>(objectId);
        if (collisionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/IllegalStateException");
            env->ThrowNew(newExc, "The collision object does not exist.");
            return;
        }
        btCollisionShape* collisionShape = reinterpret_cast<btCollisionShape*>(shapeId);
        if (collisionShape == NULL) {
            jclass newExc = env->FindClass("java/lang/IllegalStateException");
            env->ThrowNew(newExc, "The collision shape does not exist.");
            return;
        }
        collisionObject->setCollisionShape(collisionShape);
    }

    /*
     * Class:     com_jme3_bullet_collision_PhysicsCollisionObject
     * Method:    finalizeNative
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionObject_finalizeNative
    (JNIEnv * env, jobject object, jlong objectId) {
        btCollisionObject* collisionObject = reinterpret_cast<btCollisionObject*>(objectId);
        if (collisionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        if (collisionObject -> getUserPointer() != NULL){
            jmeUserPointer *userPointer = (jmeUserPointer*)collisionObject->getUserPointer();
            delete(userPointer);
        }
        delete(collisionObject);
    }
    /*
     * Class:     com_jme3_bullet_collision_PhysicsCollisionObject
     * Method:    initUserPointer
     * Signature: (JII)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionObject_initUserPointer
      (JNIEnv *env, jobject object, jlong objectId, jint group, jint groups) {
        btCollisionObject* collisionObject = reinterpret_cast<btCollisionObject*>(objectId);
        if (collisionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeUserPointer *userPointer = (jmeUserPointer*)collisionObject->getUserPointer();
        if (userPointer != NULL) {
//            delete(userPointer);
        }
        userPointer = new jmeUserPointer();
        userPointer -> javaCollisionObject = env->NewWeakGlobalRef(object);
        userPointer -> group = group;
        userPointer -> groups = groups;
        userPointer -> space = NULL;
        collisionObject -> setUserPointer(userPointer);
    }
    /*
     * Class:     com_jme3_bullet_collision_PhysicsCollisionObject
     * Method:    setCollisionGroup
     * Signature: (JI)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionObject_setCollisionGroup
      (JNIEnv *env, jobject object, jlong objectId, jint group) {
        btCollisionObject* collisionObject = reinterpret_cast<btCollisionObject*>(objectId);
        if (collisionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeUserPointer *userPointer = (jmeUserPointer*)collisionObject->getUserPointer();
        if (userPointer != NULL){
            userPointer -> group = group;
        }
    }
    /*
     * Class:     com_jme3_bullet_collision_PhysicsCollisionObject
     * Method:    setCollideWithGroups
     * Signature: (JI)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionObject_setCollideWithGroups
      (JNIEnv *env, jobject object, jlong objectId, jint groups) {
        btCollisionObject* collisionObject = reinterpret_cast<btCollisionObject*>(objectId);
        if (collisionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeUserPointer *userPointer = (jmeUserPointer*)collisionObject->getUserPointer();
        if (userPointer != NULL){
            userPointer -> groups = groups;
        }
    }

#ifdef __cplusplus
}
#endif
