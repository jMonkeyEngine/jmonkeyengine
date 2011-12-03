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
#include "com_jme3_bullet_PhysicsSpace.h"
#include "jmePhysicsSpace.h"
#include "jmeBulletUtil.h"

/**
 * Author: Normen Hansen
 */
#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    createPhysicsSpace
     * Signature: (FFFFFFI)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_PhysicsSpace_createPhysicsSpace
    (JNIEnv * env, jobject object, jfloat minX, jfloat minY, jfloat minZ, jfloat maxX, jfloat maxY, jfloat maxZ, jint broadphase, jboolean threading) {
        jmeClasses::initJavaClasses(env);
        jmePhysicsSpace* space = new jmePhysicsSpace(env, object);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space has not been created.");
            return 0;
        }
        space->createPhysicsSpace(minX, minY, minZ, maxX, maxY, maxZ, broadphase, threading);
        return reinterpret_cast<jlong>(space);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    stepSimulation
     * Signature: (JFIF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_stepSimulation
    (JNIEnv * env, jobject object, jlong spaceId, jfloat tpf, jint maxSteps, jfloat accuracy) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        space->stepSimulation(tpf, maxSteps, accuracy);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    addCollisionObject
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_addCollisionObject
    (JNIEnv * env, jobject object, jlong spaceId, jlong objectId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btCollisionObject* collisionObject = reinterpret_cast<btCollisionObject*>(objectId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (collisionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The collision object does not exist.");
            return;
        }
        jmeUserPointer *userPointer = (jmeUserPointer*)collisionObject->getUserPointer();
        userPointer -> space = space;

        space->getDynamicsWorld()->addCollisionObject(collisionObject);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    removeCollisionObject
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_removeCollisionObject
    (JNIEnv * env, jobject object, jlong spaceId, jlong objectId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btCollisionObject* collisionObject = reinterpret_cast<btCollisionObject*>(objectId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (collisionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The collision object does not exist.");
            return;
        }
        space->getDynamicsWorld()->removeCollisionObject(collisionObject);
        jmeUserPointer *userPointer = (jmeUserPointer*)collisionObject->getUserPointer();
        userPointer -> space = NULL;
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    addRigidBody
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_addRigidBody
    (JNIEnv * env, jobject object, jlong spaceId, jlong rigidBodyId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btRigidBody* collisionObject = reinterpret_cast<btRigidBody*>(rigidBodyId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (collisionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The collision object does not exist.");
            return;
        }
        jmeUserPointer *userPointer = (jmeUserPointer*)collisionObject->getUserPointer();
        userPointer -> space = space;
        space->getDynamicsWorld()->addRigidBody(collisionObject);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    removeRigidBody
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_removeRigidBody
    (JNIEnv * env, jobject object, jlong spaceId, jlong rigidBodyId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btRigidBody* collisionObject = reinterpret_cast<btRigidBody*>(rigidBodyId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (collisionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The collision object does not exist.");
            return;
        }
        jmeUserPointer *userPointer = (jmeUserPointer*)collisionObject->getUserPointer();
        userPointer -> space = NULL;
        space->getDynamicsWorld()->removeRigidBody(collisionObject);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    addCharacterObject
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_addCharacterObject
    (JNIEnv * env, jobject object, jlong spaceId, jlong objectId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btCollisionObject* collisionObject = reinterpret_cast<btCollisionObject*>(objectId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (collisionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The collision object does not exist.");
            return;
        }
        jmeUserPointer *userPointer = (jmeUserPointer*)collisionObject->getUserPointer();
        userPointer -> space = space;
        space->getDynamicsWorld()->addCollisionObject(collisionObject,
                btBroadphaseProxy::CharacterFilter,
                btBroadphaseProxy::StaticFilter | btBroadphaseProxy::DefaultFilter
        );
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    removeCharacterObject
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_removeCharacterObject
    (JNIEnv * env, jobject object, jlong spaceId, jlong objectId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btCollisionObject* collisionObject = reinterpret_cast<btCollisionObject*>(objectId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (collisionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The collision object does not exist.");
            return;
        }
        jmeUserPointer *userPointer = (jmeUserPointer*)collisionObject->getUserPointer();
        userPointer -> space = NULL;
        space->getDynamicsWorld()->removeCollisionObject(collisionObject);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    addAction
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_addAction
    (JNIEnv * env, jobject object, jlong spaceId, jlong objectId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btActionInterface* actionObject = reinterpret_cast<btActionInterface*>(objectId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (actionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The action object does not exist.");
            return;
        }
        space->getDynamicsWorld()->addAction(actionObject);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    removeAction
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_removeAction
    (JNIEnv * env, jobject object, jlong spaceId, jlong objectId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btActionInterface* actionObject = reinterpret_cast<btActionInterface*>(objectId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (actionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The action object does not exist.");
            return;
        }
        space->getDynamicsWorld()->removeAction(actionObject);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    addVehicle
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_addVehicle
    (JNIEnv * env, jobject object, jlong spaceId, jlong objectId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btActionInterface* actionObject = reinterpret_cast<btActionInterface*>(objectId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (actionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The vehicle object does not exist.");
            return;
        }
        space->getDynamicsWorld()->addVehicle(actionObject);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    removeVehicle
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_removeVehicle
    (JNIEnv * env, jobject object, jlong spaceId, jlong objectId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btActionInterface* actionObject = reinterpret_cast<btActionInterface*>(objectId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (actionObject == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The action object does not exist.");
            return;
        }
        space->getDynamicsWorld()->removeVehicle(actionObject);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    addConstraint
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_addConstraint
    (JNIEnv * env, jobject object, jlong spaceId, jlong objectId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btTypedConstraint* constraint = reinterpret_cast<btTypedConstraint*>(objectId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (constraint == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The constraint object does not exist.");
            return;
        }
        space->getDynamicsWorld()->addConstraint(constraint);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    removeConstraint
     * Signature: (JJ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_removeConstraint
    (JNIEnv * env, jobject object, jlong spaceId, jlong objectId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        btTypedConstraint* constraint = reinterpret_cast<btTypedConstraint*>(objectId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        if (constraint == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The constraint object does not exist.");
            return;
        }
        space->getDynamicsWorld()->removeConstraint(constraint);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    setGravity
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_setGravity
    (JNIEnv * env, jobject object, jlong spaceId, jobject vector) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }
        btVector3 gravity = btVector3();
        jmeBulletUtil::convert(env, vector, &gravity);
        space->getDynamicsWorld()->setGravity(gravity);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    initNativePhysics
     * Signature: ()V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_initNativePhysics
    (JNIEnv * env, jclass clazz) {
        jmeClasses::initJavaClasses(env);
    }

    /*
     * Class:     com_jme3_bullet_PhysicsSpace
     * Method:    finalizeNative
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_finalizeNative
    (JNIEnv * env, jobject object, jlong spaceId) {
        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*>(spaceId);
        if (space == NULL) {
            return;
        }
        delete(space);
    }
    
    JNIEXPORT void JNICALL Java_com_jme3_bullet_PhysicsSpace_rayTest_1native
    (JNIEnv * env, jobject object, jobject to, jobject from, jlong spaceId, jobject resultlist) {

        jmePhysicsSpace* space = reinterpret_cast<jmePhysicsSpace*> (spaceId);
        if (space == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The physics space does not exist.");
            return;
        }

        struct AllRayResultCallback : public btCollisionWorld::RayResultCallback {

            AllRayResultCallback(const btVector3& rayFromWorld, const btVector3 & rayToWorld) : m_rayFromWorld(rayFromWorld), m_rayToWorld(rayToWorld) {
            }
            jobject resultlist;
            JNIEnv* env;
            btVector3 m_rayFromWorld; //used to calculate hitPointWorld from hitFraction
            btVector3 m_rayToWorld;

            btVector3 m_hitNormalWorld;
            btVector3 m_hitPointWorld;

            virtual btScalar addSingleResult(btCollisionWorld::LocalRayResult& rayResult, bool normalInWorldSpace) {
                if (normalInWorldSpace) {
                    m_hitNormalWorld = rayResult.m_hitNormalLocal;
                } else {
                    m_hitNormalWorld = m_collisionObject->getWorldTransform().getBasis() * rayResult.m_hitNormalLocal;
                }
                m_hitPointWorld.setInterpolate3(m_rayFromWorld, m_rayToWorld, rayResult.m_hitFraction);

                jmeBulletUtil::addResult(env, resultlist, m_hitNormalWorld, m_hitPointWorld, rayResult.m_hitFraction, rayResult.m_collisionObject);

                return 1.f;
            }
        };

        btVector3 native_to = btVector3();
        jmeBulletUtil::convert(env, to, &native_to);

        btVector3 native_from = btVector3();
        jmeBulletUtil::convert(env, from, &native_from);

        AllRayResultCallback resultCallback(native_from, native_to);
        resultCallback.env = env;
        resultCallback.resultlist = resultlist;
        space->getDynamicsWorld()->rayTest(native_from, native_to, resultCallback);
        return;
    }

#ifdef __cplusplus
}
#endif
