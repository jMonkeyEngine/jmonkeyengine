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
#include "com_jme3_bullet_objects_infos_RigidBodyMotionState.h"
#include "jmeBulletUtil.h"
#include "jmeMotionState.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_objects_infos_RigidBodyMotionState
     * Method:    createMotionState
     * Signature: ()J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_infos_RigidBodyMotionState_createMotionState
    (JNIEnv *env, jobject object) {
        jmeClasses::initJavaClasses(env);
        jmeMotionState* motionState = new jmeMotionState();
        return reinterpret_cast<jlong>(motionState);
    }

    /*
     * Class:     com_jme3_bullet_objects_infos_RigidBodyMotionState
     * Method:    applyTransform
     * Signature: (JLcom/jme3/math/Vector3f;Lcom/jme3/math/Quaternion;)Z
     */
    JNIEXPORT jboolean JNICALL Java_com_jme3_bullet_objects_infos_RigidBodyMotionState_applyTransform
    (JNIEnv *env, jobject object, jlong stateId, jobject location, jobject rotation) {
        jmeMotionState* motionState = reinterpret_cast<jmeMotionState*>(stateId);
        if (motionState == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return false;
        }
        return motionState->applyTransform(env, location, rotation);
    }

    /*
     * Class:     com_jme3_bullet_objects_infos_RigidBodyMotionState
     * Method:    getWorldLocation
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_infos_RigidBodyMotionState_getWorldLocation
    (JNIEnv *env, jobject object, jlong stateId, jobject value) {
        jmeMotionState* motionState = reinterpret_cast<jmeMotionState*>(stateId);
        if (motionState == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &motionState->worldTransform.getOrigin(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_infos_RigidBodyMotionState
     * Method:    getWorldRotation
     * Signature: (JLcom/jme3/math/Matrix3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_infos_RigidBodyMotionState_getWorldRotation
    (JNIEnv *env, jobject object, jlong stateId, jobject value) {
        jmeMotionState* motionState = reinterpret_cast<jmeMotionState*>(stateId);
        if (motionState == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &motionState->worldTransform.getBasis(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_infos_RigidBodyMotionState
     * Method:    getWorldRotationQuat
     * Signature: (JLcom/jme3/math/Quaternion;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_infos_RigidBodyMotionState_getWorldRotationQuat
    (JNIEnv *env, jobject object, jlong stateId, jobject value) {
        jmeMotionState* motionState = reinterpret_cast<jmeMotionState*>(stateId);
        if (motionState == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convertQuat(env, &motionState->worldTransform.getBasis(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_infos_RigidBodyMotionState
     * Method:    finalizeNative
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_infos_RigidBodyMotionState_finalizeNative
    (JNIEnv *env, jobject object, jlong stateId) {
        jmeMotionState* motionState = reinterpret_cast<jmeMotionState*>(stateId);
        if (motionState == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        delete(motionState);
    }

#ifdef __cplusplus
}
#endif
