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

#include "jmeBulletUtil.h"
#include "BulletCollision/NarrowPhaseCollision/btManifoldPoint.h"
#include "com_jme3_bullet_collision_PhysicsCollisionEvent.h"

// Change to trigger build

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getAppliedImpulse
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getAppliedImpulse
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_appliedImpulse;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getAppliedImpulseLateral1
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getAppliedImpulseLateral1
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_appliedImpulseLateral1;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getAppliedImpulseLateral2
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getAppliedImpulseLateral2
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_appliedImpulseLateral2;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getCombinedFriction
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getCombinedFriction
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_combinedFriction;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getCombinedRestitution
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getCombinedRestitution
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_combinedRestitution;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getDistance1
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getDistance1
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_distance1;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getIndex0
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getIndex0
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_index0;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getIndex1
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getIndex1
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_index1;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getLateralFrictionDir1
 * Signature: (JLcom/jme3/math/Vector3f;)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getLateralFrictionDir1
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId, jobject lateralFrictionDir1) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return;
    }
    jmeBulletUtil::convert(env, &mp -> m_lateralFrictionDir1, lateralFrictionDir1);
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getLateralFrictionDir2
 * Signature: (JLcom/jme3/math/Vector3f;)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getLateralFrictionDir2
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId, jobject lateralFrictionDir2) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return;
    }
    jmeBulletUtil::convert(env, &mp -> m_lateralFrictionDir2, lateralFrictionDir2);
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    isLateralFrictionInitialized
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_isLateralFrictionInitialized
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_lateralFrictionInitialized;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getLifeTime
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getLifeTime
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_lifeTime;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getLocalPointA
 * Signature: (JLcom/jme3/math/Vector3f;)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getLocalPointA
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId, jobject localPointA) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return;
    }
    jmeBulletUtil::convert(env, &mp -> m_localPointA, localPointA);
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getLocalPointB
 * Signature: (JLcom/jme3/math/Vector3f;)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getLocalPointB
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId, jobject localPointB) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return;
    }
    jmeBulletUtil::convert(env, &mp -> m_localPointB, localPointB);
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getNormalWorldOnB
 * Signature: (JLcom/jme3/math/Vector3f;)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getNormalWorldOnB
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId, jobject normalWorldOnB) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return;
    }
    jmeBulletUtil::convert(env, &mp -> m_normalWorldOnB, normalWorldOnB);
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getPartId0
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getPartId0
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_partId0;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getPartId1
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getPartId1
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return 0;
    }
    return mp -> m_partId1;
}

/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getPositionWorldOnA
 * Signature: (JLcom/jme3/math/Vector3f;)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getPositionWorldOnA
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId, jobject positionWorldOnA) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return;
    }
    jmeBulletUtil::convert(env, &mp -> m_positionWorldOnA, positionWorldOnA);
}


/*
 * Class:     com_jme3_bullet_collision_PhysicsCollisionEvent
 * Method:    getPositionWorldOnB
 * Signature: (JLcom/jme3/math/Vector3f;)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_PhysicsCollisionEvent_getPositionWorldOnB
  (JNIEnv * env, jobject object, jlong manifoldPointObjectId, jobject positionWorldOnB) {
    btManifoldPoint* mp = reinterpret_cast<btManifoldPoint*>(manifoldPointObjectId);
    if (mp == NULL) {
        jclass newExc = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(newExc, "The manifoldPoint does not exist.");
        return;
    }
    jmeBulletUtil::convert(env, &mp -> m_positionWorldOnB, positionWorldOnB);
}
