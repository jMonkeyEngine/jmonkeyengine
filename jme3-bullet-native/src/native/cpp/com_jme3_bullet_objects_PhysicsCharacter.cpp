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

#include "com_jme3_bullet_objects_PhysicsCharacter.h"
#include "jmeBulletUtil.h"
#include "BulletCollision/CollisionDispatch/btGhostObject.h"
#include "BulletDynamics/Character/btKinematicCharacterController.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    createGhostObject
     * Signature: ()J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_createGhostObject
    (JNIEnv * env, jobject object) {
        jmeClasses::initJavaClasses(env);
        btPairCachingGhostObject* ghost = new btPairCachingGhostObject();
        return reinterpret_cast<jlong>(ghost);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setCharacterFlags
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setCharacterFlags
    (JNIEnv *env, jobject object, jlong ghostId) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(ghostId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        ghost->setCollisionFlags(/*ghost->getCollisionFlags() |*/ btCollisionObject::CF_CHARACTER_OBJECT);
        ghost->setCollisionFlags(ghost->getCollisionFlags() & ~btCollisionObject::CF_NO_CONTACT_RESPONSE);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    createCharacterObject
     * Signature: (JJF)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_createCharacterObject
    (JNIEnv *env, jobject object, jlong objectId, jlong shapeId, jfloat stepHeight) {
        btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        //TODO: check convexshape!
        btConvexShape* shape = reinterpret_cast<btConvexShape*>(shapeId);
        btKinematicCharacterController* character = new btKinematicCharacterController(ghost, shape, stepHeight);
        return reinterpret_cast<jlong>(character);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    warp
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_warp
    (JNIEnv *env, jobject object, jlong objectId, jobject vector) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, vector, &vec);
        character->warp(vec);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setWalkDirection
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setWalkDirection
    (JNIEnv *env, jobject object, jlong objectId, jobject vector) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, vector, &vec);
        character->setWalkDirection(vec);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setUp
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setUp
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env,  value, &vec);
        character->setUp(vec);
    }

     /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setAngularVelocity
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setAngularVelocity
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, value, &vec);
        character->setAngularVelocity(vec);
    }


    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getAngularVelocity
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
     
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getAngularVelocity
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 a_vel = character->getAngularVelocity();
        jmeBulletUtil::convert(env, &a_vel, value);
    }


    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setLinearVelocity
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setLinearVelocity
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, value, &vec);
        character->setLinearVelocity(vec);
    }


    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getLinearVelocity
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
     
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getLinearVelocity
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 l_vel = character->getLinearVelocity();
        jmeBulletUtil::convert(env, &l_vel, value);
    }




    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setFallSpeed
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setFallSpeed
    (JNIEnv *env, jobject object, jlong objectId, jfloat value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        character->setFallSpeed(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setJumpSpeed
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setJumpSpeed
    (JNIEnv *env, jobject object, jlong objectId, jfloat value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        character->setJumpSpeed(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setGravity
     * Signature:  (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setGravity
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }

        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, value, &vec);
        character->setGravity(vec);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getGravity
     * Signature:  (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getGravity
    (JNIEnv *env, jobject object, jlong objectId,jobject value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 g = character->getGravity();
        jmeBulletUtil::convert(env, &g, value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setLinearDamping
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setLinearDamping
    (JNIEnv *env, jobject object, jlong objectId,jfloat value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return ;
        }
        character->setLinearDamping(value);
    }


   /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getLinearDamping
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getLinearDamping
    (JNIEnv *env, jobject object, jlong objectId) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return character->getLinearDamping();
    }


      /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setAngularDamping
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setAngularDamping
    (JNIEnv *env, jobject object, jlong objectId,jfloat value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        character->setAngularDamping(value);
    }


   /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getAngularDamping
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getAngularDamping
    (JNIEnv *env, jobject object, jlong objectId) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return character->getAngularDamping();
    }


        /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setStepHeight
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setStepHeight
    (JNIEnv *env, jobject object, jlong objectId,jfloat value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        character->setStepHeight(value);
    }


   /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getStepHeight
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getStepHeight
    (JNIEnv *env, jobject object, jlong objectId) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return character->getStepHeight();
    }


    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setMaxSlope
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setMaxSlope
    (JNIEnv *env, jobject object, jlong objectId, jfloat value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        character->setMaxSlope(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getMaxSlope
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getMaxSlope
    (JNIEnv *env, jobject object, jlong objectId) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return character->getMaxSlope();
    }


    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setMaxPenetrationDepth
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setMaxPenetrationDepth
    (JNIEnv *env, jobject object, jlong objectId, jfloat value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        character->setMaxPenetrationDepth(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getMaxPenetrationDepth
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getMaxPenetrationDepth
    (JNIEnv *env, jobject object, jlong objectId) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return character->getMaxPenetrationDepth();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    onGround
     * Signature: (J)Z
     */
    JNIEXPORT jboolean JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_onGround
    (JNIEnv *env, jobject object, jlong objectId) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return false;
        }
        return character->onGround();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    jump
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_jump
    (JNIEnv *env, jobject object, jlong objectId,jobject value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, value, &vec);
        character->jump(vec);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    applyImpulse
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_applyImpulse
    (JNIEnv *env, jobject object, jlong objectId,jobject value) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        btVector3 vec = btVector3();
        jmeBulletUtil::convert(env, value, &vec);
        character->applyImpulse(vec);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getPhysicsLocation
     * Signature: (JLcom/jme3/math/Vector3f;)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getPhysicsLocation
    (JNIEnv *env, jobject object, jlong objectId, jobject value) {
        btGhostObject* ghost = reinterpret_cast<btGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        jmeBulletUtil::convert(env, &ghost->getWorldTransform().getOrigin(), value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setCcdSweptSphereRadius
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setCcdSweptSphereRadius
    (JNIEnv *env, jobject object, jlong objectId, jfloat value) {
        btGhostObject* ghost = reinterpret_cast<btGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        ghost->setCcdSweptSphereRadius(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    setCcdMotionThreshold
     * Signature: (JF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_setCcdMotionThreshold
    (JNIEnv *env, jobject object, jlong objectId, jfloat value) {
        btGhostObject* ghost = reinterpret_cast<btGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        ghost->setCcdMotionThreshold(value);
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getCcdSweptSphereRadius
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getCcdSweptSphereRadius
    (JNIEnv *env, jobject object, jlong objectId) {
        btGhostObject* ghost = reinterpret_cast<btGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return ghost->getCcdSweptSphereRadius();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getCcdMotionThreshold
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getCcdMotionThreshold
    (JNIEnv *env, jobject object, jlong objectId) {
        btGhostObject* ghost = reinterpret_cast<btGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return ghost->getCcdMotionThreshold();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    getCcdSquareMotionThreshold
     * Signature: (J)F
     */
    JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_getCcdSquareMotionThreshold
    (JNIEnv *env, jobject object, jlong objectId) {
        btGhostObject* ghost = reinterpret_cast<btGhostObject*>(objectId);
        if (ghost == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        return ghost->getCcdSquareMotionThreshold();
    }

    /*
     * Class:     com_jme3_bullet_objects_PhysicsCharacter
     * Method:    finalizeNativeCharacter
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsCharacter_finalizeNativeCharacter
    (JNIEnv *env, jobject object, jlong objectId) {
        btKinematicCharacterController* character = reinterpret_cast<btKinematicCharacterController*>(objectId);
        if (character == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        delete(character);
    }

#ifdef __cplusplus
}
#endif
