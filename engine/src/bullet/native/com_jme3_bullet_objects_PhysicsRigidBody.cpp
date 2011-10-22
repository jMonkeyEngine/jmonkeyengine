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
#include "com_jme3_bullet_objects_PhysicsRigidBody.h"
#include "jmeBulletUtil.h"
#include "jmeMotionState.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    createRigidBody
     * Signature: (FJJ)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_createRigidBody
    (JNIEnv *env, jobject object, jfloat mass, jlong motionstatId, jlong shapeId) {
        jmeClasses::initJavaClasses(env);
        btMotionState* motionState = reinterpret_cast<btMotionState*>(motionstatId);
        btCollisionShape* shape = reinterpret_cast<btCollisionShape*>(shapeId);
        btVector3 localInertia = btVector3();
        shape->calculateLocalInertia(mass, localInertia);
        btRigidBody* body = new btRigidBody(mass, motionState, shape, localInertia);
        body->setUserPointer(NULL);
        return reinterpret_cast<jlong>(body);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    isInWorld
     * Signature: (J)Z
     */
    JNIEXPORT jboolean JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_isInWorld
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return false;
        }
        return body->isInWorld();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setPhysicsLocation
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setPhysicsLocation
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        //        if (body->isStaticOrKinematicObject() || !body->isInWorld())
        ((jmeMotionState*) body->getMotionState())->setKinematicLocation(env, value);
        body->setCenterOfMassTransform(((jmeMotionState*) body->getMotionState())->worldTransform);
        //        else{
        //            btMatrix3x3* mtx = &btMatrix3x3();
        //            btTransform* trans = &btTransform(*mtx);
        //            trans->setBasis(body->getCenterOfMassTransform().getBasis());
        //            jmeBulletUtil::convert(env, value, &trans->getOrigin());
        //            body->setCenterOfMassTransform(*trans);
        //        }
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setPhysicsRotation
     * Signature: (JLcom/jme3/math/Matrix3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setPhysicsRotation__JLcom_jme3_math_Matrix3f_2
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        //        if (body->isStaticOrKinematicObject() || !body->isInWorld())
        ((jmeMotionState*) body->getMotionState())->setKinematicRotation(env, value);
        body->setCenterOfMassTransform(((jmeMotionState*) body->getMotionState())->worldTransform);
        //        else{
        //            btMatrix3x3* mtx = &btMatrix3x3();
        //            btTransform* trans = &btTransform(*mtx);
        //            trans->setOrigin(body->getCenterOfMassTransform().getOrigin());
        //            jmeBulletUtil::convert(env, value, &trans->getBasis());
        //            body->setCenterOfMassTransform(*trans);
        //        }
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setPhysicsRotation
     * Signature: (JLcom/jme3/math/Quaternion;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setPhysicsRotation__JLcom_jme3_math_Quaternion_2
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        //        if (body->isStaticOrKinematicObject() || !body->isInWorld())
        ((jmeMotionState*) body->getMotionState())->setKinematicRotationQuat(env, value);
        body->setCenterOfMassTransform(((jmeMotionState*) body->getMotionState())->worldTransform);
        //        else{
        //            btMatrix3x3* mtx = &btMatrix3x3();
        //            btTransform* trans = &btTransform(*mtx);
        //            trans->setOrigin(body->getCenterOfMassTransform().getOrigin());
        //            jmeBulletUtil::convertQuat(env, value, &trans->getBasis());
        //            body->setCenterOfMassTransform(*trans);
        //        }
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getPhysicsLocation
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getPhysicsLocation
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &body->getWorldTransform().getOrigin(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getPhysicsRotation
     * Signature: (JLcom/jme3/math/Quaternion;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getPhysicsRotation
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convertQuat(env, &body->getWorldTransform().getBasis(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getPhysicsRotationMatrix
     * Signature: (JLcom/jme3/math/Matrix3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getPhysicsRotationMatrix
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &body->getWorldTransform().getBasis(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setKinematic
     * Signature: (JZ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setKinematic
    (JNIEnv *env, jobject object, jlong bodyId, jboolean value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        if (value) {
            body->setCollisionFlags(body->getCollisionFlags() | btCollisionObject::CF_KINEMATIC_OBJECT);
            body->setActivationState(DISABLE_DEACTIVATION);
        } else {
            body->setCollisionFlags(body->getCollisionFlags() & ~btCollisionObject::CF_KINEMATIC_OBJECT);
            body->setActivationState(ACTIVE_TAG);
        }
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setCcdSweptSphereRadius
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setCcdSweptSphereRadius
    (JNIEnv *env, jobject object, jlong bodyId, jfloat value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setCcdSweptSphereRadius(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setCcdMotionThreshold
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setCcdMotionThreshold
    (JNIEnv *env, jobject object, jlong bodyId, jfloat value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setCcdMotionThreshold(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getCcdSweptSphereRadius
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getCcdSweptSphereRadius
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getCcdSweptSphereRadius();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getCcdMotionThreshold
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getCcdMotionThreshold
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getCcdMotionThreshold();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getCcdSquareMotionThreshold
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getCcdSquareMotionThreshold
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getCcdSquareMotionThreshold();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setStatic
     * Signature: (JZ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setStatic
    (JNIEnv *env, jobject object, jlong bodyId, jboolean value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        if (value) {
            body->setCollisionFlags(body->getCollisionFlags() | btCollisionObject::CF_STATIC_OBJECT);
        } else {
            body->setCollisionFlags(body->getCollisionFlags() & ~btCollisionObject::CF_STATIC_OBJECT);
        }
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    updateMassProps
     * Signature: (JJF)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_updateMassProps
    (JNIEnv *env, jobject object, jlong bodyId, jlong shapeId, jfloat mass) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        btCollisionShape* shape = reinterpret_cast<btCollisionShape*>(shapeId);
        btVector3 localInertia = btVector3();
        shape->calculateLocalInertia(mass, localInertia);
        body->setMassProps(mass, localInertia);
        return reinterpret_cast<jlong>(body);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getGravity
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getGravity
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &body->getGravity(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setGravity
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setGravity
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, value, &vec);
        body->setGravity(vec);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getFriction
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getFriction
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getFriction();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setFriction
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setFriction
    (JNIEnv *env, jobject object, jlong bodyId, jfloat value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setFriction(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setDamping
     * Signature: (JFF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setDamping
    (JNIEnv *env, jobject object, jlong bodyId, jfloat value1, jfloat value2) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setDamping(value1, value2);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setAngularDamping
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setAngularDamping
    (JNIEnv *env, jobject object, jlong bodyId, jfloat value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setDamping(body->getAngularDamping(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getLinearDamping
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getLinearDamping
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getLinearDamping();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getAngularDamping
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getAngularDamping
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getAngularDamping();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getRestitution
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getRestitution
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getRestitution();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setRestitution
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setRestitution
    (JNIEnv *env, jobject object, jlong bodyId, jfloat value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setRestitution(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getAngularVelocity
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getAngularVelocity
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &body->getAngularVelocity(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setAngularVelocity
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setAngularVelocity
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, value, &vec);
        body->setAngularVelocity(vec);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getLinearVelocity
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getLinearVelocity
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &body->getLinearVelocity(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setLinearVelocity
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setLinearVelocity
    (JNIEnv *env, jobject object, jlong bodyId, jobject value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, value, &vec);
        body->setLinearVelocity(vec);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    applyForce
     * Signature: (JLcom/jme3/math/Vector3f;Lcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_applyForce
    (JNIEnv *env, jobject object, jlong bodyId, jobject force, jobject location) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec1 = btVector3();
        btVector3 vec2 = btVector3();
        jmeBulletUtil::convert(env, force, &vec1);
        jmeBulletUtil::convert(env, location, &vec2);
        body->applyForce(vec1, vec2);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    applyCentralForce
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_applyCentralForce
    (JNIEnv *env, jobject object, jlong bodyId, jobject force) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec1 = btVector3();
        jmeBulletUtil::convert(env, force, &vec1);
        body->applyCentralForce(vec1);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    applyTorque
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_applyTorque
    (JNIEnv *env, jobject object, jlong bodyId, jobject force) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec1 = btVector3();
        jmeBulletUtil::convert(env, force, &vec1);
        body->applyTorque(vec1);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    applyImpulse
     * Signature: (JLcom/jme3/math/Vector3f;Lcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_applyImpulse
    (JNIEnv *env, jobject object, jlong bodyId, jobject force, jobject location) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec1 = btVector3();
        btVector3 vec2 = btVector3();
        jmeBulletUtil::convert(env, force, &vec1);
        jmeBulletUtil::convert(env, location, &vec2);
        body->applyImpulse(vec1, vec2);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    applyTorqueImpulse
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_applyTorqueImpulse
    (JNIEnv *env, jobject object, jlong bodyId, jobject force) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec1 = btVector3();
        jmeBulletUtil::convert(env, force, &vec1);
        body->applyTorqueImpulse(vec1);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    clearForces
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_clearForces
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->clearForces();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setCollisionShape
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setCollisionShape
    (JNIEnv *env, jobject object, jlong bodyId, jlong shapeId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btCollisionShape* shape = reinterpret_cast<btCollisionShape*>(shapeId);
        body->setCollisionShape(shape);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    activate
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_activate
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->activate(false);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    isActive
     * Signature: (J)Z
     */
    JNIEXPORT jboolean JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_isActive
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return false;
        }
        return body->isActive();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setSleepingThresholds
     * Signature: (JFF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setSleepingThresholds
    (JNIEnv *env, jobject object, jlong bodyId, jfloat linear, jfloat angular) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setSleepingThresholds(linear, angular);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setLinearSleepingThreshold
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setLinearSleepingThreshold
    (JNIEnv *env, jobject object, jlong bodyId, jfloat value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setSleepingThresholds(value, body->getLinearSleepingThreshold());
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setAngularSleepingThreshold
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setAngularSleepingThreshold
    (JNIEnv *env, jobject object, jlong bodyId, jfloat value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        body->setSleepingThresholds(body->getAngularSleepingThreshold(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getLinearSleepingThreshold
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getLinearSleepingThreshold
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getLinearSleepingThreshold();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getAngularSleepingThreshold
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getAngularSleepingThreshold
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getAngularSleepingThreshold();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    getAngularFactor
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_getAngularFactor
    (JNIEnv *env, jobject object, jlong bodyId) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return body->getAngularFactor().getX();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsRigidBody
     * Method:    setAngularFactor
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsRigidBody_setAngularFactor
    (JNIEnv *env, jobject object, jlong bodyId, jfloat value) {
        btRigidBody* body = reinterpret_cast<btRigidBody*>(bodyId);
        if (body == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec1 = btVector3();
        vec1.setX(value);
        vec1.setY(value);
        vec1.setZ(value);
        body->setAngularFactor(vec1);
    }

#ifdef __cplusplus
}
#endif
