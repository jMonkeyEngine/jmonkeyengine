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

#include <BulletCollision/CollisionDispatch/btGhostObject.h>

#include "com_jme3_bullet_objects_PhysicsGhostObject.h"
#include "BulletCollision/BroadphaseCollision/btOverlappingPairCache.h"
#include "jmeBulletUtil.h"
#include "jmePhysicsSpace.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    createGhostObject
     * Signature: ()J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_createGhostObject
    (JNIEnv * env, jobject object) {
        jmeClasses::initJavaClasses(env);
        btPairCachingGhostObject* ghost = new btPairCachingGhostObject();
        return reinterpret_cast<jlong>(ghost);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    setGhostFlags
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_setGhostFlags
    (JNIEnv *env, jobject object, jlong objectId) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        ghost->setCollisionFlags(ghost->getCollisionFlags() | btCollisionObject::CF_NO_CONTACT_RESPONSE);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    setPhysicsLocation
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_setPhysicsLocation
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, value, &ghost->getWorldTransform().getOrigin());
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    setPhysicsRotation
     * Signature: (JLcom/jme3/math/Matrix3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_setPhysicsRotation__JLcom_jme3_math_Matrix3f_2
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, value, &ghost->getWorldTransform().getBasis());
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    setPhysicsRotation
     * Signature: (JLcom/jme3/math/Quaternion;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_setPhysicsRotation__JLcom_jme3_math_Quaternion_2
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convertQuat(env, value, &ghost->getWorldTransform().getBasis());
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    getPhysicsLocation
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_getPhysicsLocation
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &ghost->getWorldTransform().getOrigin(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    getPhysicsRotation
     * Signature: (JLcom/jme3/math/Quaternion;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_getPhysicsRotation
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convertQuat(env, &ghost->getWorldTransform().getBasis(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    getPhysicsRotationMatrix
     * Signature: (JLcom/jme3/math/Matrix3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_getPhysicsRotationMatrix
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &ghost->getWorldTransform().getBasis(), value);
    }

    class jmeGhostOverlapCallback : public btOverlapCallback {
        JNIEnv* m_env;
        jobject m_object;
    public:
        jmeGhostOverlapCallback(JNIEnv *env, jobject object)
                :m_env(env),
                 m_object(object)
        {
        }
        virtual ~jmeGhostOverlapCallback() {}
        virtual bool    processOverlap(btBroadphasePair& pair)
        {
            btCollisionObject *co1 = (btCollisionObject *)pair.m_pProxy1->m_clientObject;
            jmeUserPointer *up1 = (jmeUserPointer*)co1 -> getUserPointer();
            jobject javaCollisionObject1 = m_env->NewLocalRef(up1->javaCollisionObject);
            m_env->CallVoidMethod(m_object, jmeClasses::PhysicsGhostObject_addOverlappingObject, javaCollisionObject1);
            m_env->DeleteLocalRef(javaCollisionObject1);
            if (m_env->ExceptionCheck()) {
                m_env->Throw(m_env->ExceptionOccurred());
                return false;
            }

            return false;
        }
    };

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    getOverlappingObjects
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_getOverlappingObjects
      (JNIEnv *env, jobject object, jlong objectId) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btHashedOverlappingPairCache * pc = ghost->getOverlappingPairCache();
        jmeGhostOverlapCallback cb(env, object);
        pc -> processAllOverlappingPairs(&cb, NULL);
    }
    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    getOverlappingCount
     * Signature: (J)I
     */
    JNIEXPORT jint JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_getOverlappingCount
    (JNIEnv *env, jobject object, jlong objectId) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return ghost->getNumOverlappingObjects();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    setCcdSweptSphereRadius
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_setCcdSweptSphereRadius
    (JNIEnv *env, jobject object, jlong objectId, jfloat value) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        ghost->setCcdSweptSphereRadius(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    setCcdMotionThreshold
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_setCcdMotionThreshold
    (JNIEnv *env, jobject object, jlong objectId, jfloat value) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        ghost->setCcdMotionThreshold(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    getCcdSweptSphereRadius
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_getCcdSweptSphereRadius
    (JNIEnv *env, jobject object, jlong objectId) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return ghost->getCcdSweptSphereRadius();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    getCcdMotionThreshold
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_getCcdMotionThreshold
    (JNIEnv *env, jobject object, jlong objectId) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return ghost->getCcdMotionThreshold();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsGhostObject
     * Method:    getCcdSquareMotionThreshold
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_getCcdSquareMotionThreshold
    (JNIEnv *env, jobject object, jlong objectId) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return ghost->getCcdSquareMotionThreshold();
    }

#ifdef __cplusplus
}
#endif
