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

#include "com_jme3_bullet_objects_VehicleWheel.h"
#include "jmeBulletUtil.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_objects_VehicleWheel
     * Method:    getWheelLocation
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_VehicleWheel_getWheelLocation
    (JNIEnv *env, jobject object, jlong vehicleId, jint wheelIndex, jobject out) {
        btRaycastVehicle* vehicle = reinterpret_cast<btRaycastVehicle*>(vehicleId);
        if (vehicle == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &vehicle->getWheelInfo(wheelIndex).m_worldTransform.getOrigin(), out);
    }

    /*
     * Class:     com_jme3_bullet_objects_VehicleWheel
     * Method:    getWheelRotation
     * Signature: (JLcom/jme3/math/Matrix3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_VehicleWheel_getWheelRotation
    (JNIEnv *env, jobject object, jlong vehicleId, jint wheelIndex, jobject out) {
        btRaycastVehicle* vehicle = reinterpret_cast<btRaycastVehicle*>(vehicleId);
        if (vehicle == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &vehicle->getWheelInfo(wheelIndex).m_worldTransform.getBasis(), out);
    }

    /*
     * Class:     com_jme3_bullet_objects_VehicleWheel
     * Method:    applyInfo
     * Signature: (JFFFFFFFFZF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_VehicleWheel_applyInfo
    (JNIEnv *env, jobject object, jlong vehicleId, jint wheelIndex, jfloat suspensionStiffness, jfloat wheelsDampingRelaxation, jfloat wheelsDampingCompression, jfloat frictionSlip, jfloat rollInfluence, jfloat maxSuspensionTravelCm, jfloat maxSuspensionForce, jfloat radius, jboolean frontWheel, jfloat restLength) {
        btRaycastVehicle* vehicle = reinterpret_cast<btRaycastVehicle*>(vehicleId);
        vehicle->getWheelInfo(wheelIndex).m_suspensionStiffness = suspensionStiffness;
        vehicle->getWheelInfo(wheelIndex).m_wheelsDampingRelaxation = wheelsDampingRelaxation;
        vehicle->getWheelInfo(wheelIndex).m_wheelsDampingCompression = wheelsDampingCompression;
        vehicle->getWheelInfo(wheelIndex).m_frictionSlip = frictionSlip;
        vehicle->getWheelInfo(wheelIndex).m_rollInfluence = rollInfluence;
        vehicle->getWheelInfo(wheelIndex).m_maxSuspensionTravelCm = maxSuspensionTravelCm;
        vehicle->getWheelInfo(wheelIndex).m_maxSuspensionForce = maxSuspensionForce;
        vehicle->getWheelInfo(wheelIndex).m_wheelsRadius = radius;
        vehicle->getWheelInfo(wheelIndex).m_bIsFrontWheel = frontWheel;
        vehicle->getWheelInfo(wheelIndex).m_suspensionRestLength1 = restLength;

    }

    /*
     * Class:     com_jme3_bullet_objects_VehicleWheel
     * Method:    getCollisionLocation
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_VehicleWheel_getCollisionLocation
    (JNIEnv *env, jobject object, jlong vehicleId, jint wheelIndex, jobject out) {
        btRaycastVehicle* vehicle = reinterpret_cast<btRaycastVehicle*>(vehicleId);
        if (vehicle == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &vehicle->getWheelInfo(wheelIndex).m_raycastInfo.m_contactPointWS, out);
    }

    /*
     * Class:     com_jme3_bullet_objects_VehicleWheel
     * Method:    getCollisionNormal
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_VehicleWheel_getCollisionNormal
    (JNIEnv *env, jobject object, jlong vehicleId, jint wheelIndex, jobject out) {
        btRaycastVehicle* vehicle = reinterpret_cast<btRaycastVehicle*>(vehicleId);
        if (vehicle == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &vehicle->getWheelInfo(wheelIndex).m_raycastInfo.m_contactNormalWS, out);
    }

    /*
     * Class:     com_jme3_bullet_objects_VehicleWheel
     * Method:    getSkidInfo
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_VehicleWheel_getSkidInfo
    (JNIEnv *env, jobject object, jlong vehicleId, jint wheelIndex) {
        btRaycastVehicle* vehicle = reinterpret_cast<btRaycastVehicle*>(vehicleId);
        if (vehicle == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return vehicle->getWheelInfo(wheelIndex).m_skidInfo;
    }

    /*
     * Class:     com_jme3_bullet_objects_VehicleWheel
     * Method:    getDeltaRotation
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_VehicleWheel_getDeltaRotation
    (JNIEnv *env, jobject object, jlong vehicleId, jint wheelIndex) {
        btRaycastVehicle* vehicle = reinterpret_cast<btRaycastVehicle*>(vehicleId);
        if (vehicle == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return vehicle->getWheelInfo(wheelIndex).m_deltaRotation;
    }

#ifdef __cplusplus
}
#endif
