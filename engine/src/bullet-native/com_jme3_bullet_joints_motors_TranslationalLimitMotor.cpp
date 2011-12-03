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
#include "com_jme3_bullet_joints_motors_TranslationalLimitMotor.h"
#include "jmeBulletUtil.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    getLowerLimit
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_getLowerLimit
    (JNIEnv *env, jobject object, jlong motorId, jobject vector) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &motor->m_lowerLimit, vector);
    }

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    setLowerLimit
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_setLowerLimit
    (JNIEnv *env, jobject object, jlong motorId, jobject vector) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, vector, &motor->m_lowerLimit);
    }

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    getUpperLimit
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_getUpperLimit
    (JNIEnv *env, jobject object, jlong motorId, jobject vector) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &motor->m_upperLimit, vector);
    }

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    setUpperLimit
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_setUpperLimit
    (JNIEnv *env, jobject object, jlong motorId, jobject vector) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, vector, &motor->m_upperLimit);
    }

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    getAccumulatedImpulse
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_getAccumulatedImpulse
    (JNIEnv *env, jobject object, jlong motorId, jobject vector) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &motor->m_accumulatedImpulse, vector);
    }

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    setAccumulatedImpulse
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_setAccumulatedImpulse
    (JNIEnv *env, jobject object, jlong motorId, jobject vector) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, vector, &motor->m_accumulatedImpulse);
    }

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    getLimitSoftness
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_getLetLimitSoftness
    (JNIEnv *env, jobject object, jlong motorId) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return motor->m_limitSoftness;
    }

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    setLimitSoftness
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_setLimitSoftness
    (JNIEnv *env, jobject object, jlong motorId, jfloat value) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        motor->m_limitSoftness = value;
    }

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    getDamping
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_getDamping
    (JNIEnv *env, jobject object, jlong motorId) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return motor->m_damping;
    }

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    setDamping
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_setDamping
    (JNIEnv *env, jobject object, jlong motorId, jfloat value) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        motor->m_damping = value;
    }

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    getRestitution
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_getRestitution
    (JNIEnv *env, jobject object, jlong motorId) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return motor->m_restitution;
    }

    /*
     * Class:     com_jme3_bullet_joints_motors_TranslationalLimitMotor
     * Method:    setRestitution
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_motors_TranslationalLimitMotor_setRestitution
    (JNIEnv *env, jobject object, jlong motorId, jfloat value) {
        btTranslationalLimitMotor* motor = reinterpret_cast<btTranslationalLimitMotor*>(motorId);
        if (motor == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        motor->m_restitution = value;
    }

#ifdef __cplusplus
}
#endif
