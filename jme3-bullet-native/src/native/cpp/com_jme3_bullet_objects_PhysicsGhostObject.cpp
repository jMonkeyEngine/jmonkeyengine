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
        btCollisionObject *m_ghost;
    public:
        jmeGhostOverlapCallback(JNIEnv *env, jobject object, btCollisionObject *ghost)
                :m_env(env),
                 m_object(object),
                 m_ghost(ghost)
        {
        }
        virtual ~jmeGhostOverlapCallback() {}
        virtual bool    processOverlap(btBroadphasePair& pair)
        {
            btCollisionObject *other;
            if(pair.m_pProxy1->m_clientObject == m_ghost){
                other = (btCollisionObject *)pair.m_pProxy0->m_clientObject;
            }else{
                other = (btCollisionObject *)pair.m_pProxy1->m_clientObject;
            }
            jmeUserPointer *up1 = (jmeUserPointer*)other -> getUserPointer();
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
        jmeGhostOverlapCallback cb(env, object, ghost);
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

	JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_rayTest_1native
		(JNIEnv * env, jobject object, jobject from, jobject to, jlong objectId, jobject resultlist, jint flags) {

		btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
		if (ghost == NULL) {
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
				}
				else {
					m_hitNormalWorld = m_collisionObject->getWorldTransform().getBasis() * rayResult.m_hitNormalLocal;
				}
				m_hitPointWorld.setInterpolate3(m_rayFromWorld, m_rayToWorld, rayResult.m_hitFraction);

				jmeBulletUtil::addResult(env, resultlist, &m_hitNormalWorld, &m_hitPointWorld, rayResult.m_hitFraction, rayResult.m_collisionObject);

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
		resultCallback.m_flags = flags;
		ghost->rayTest(native_from, native_to, resultCallback);
		return;
	}



	JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsGhostObject_sweepTest_1native
		(JNIEnv * env, jobject object, jlong shapeId, jobject from, jobject to, jlong objectId, jobject resultlist, jfloat allowedCcdPenetration) {

		btPairCachingGhostObject* ghost = reinterpret_cast<btPairCachingGhostObject*>(objectId);
		if (ghost == NULL) {
			jclass newExc = env->FindClass("java/lang/NullPointerException");
			env->ThrowNew(newExc, "The physics space does not exist.");
			return;
		}

		btCollisionShape* shape = reinterpret_cast<btCollisionShape*> (shapeId);
		if (shape == NULL) {
			jclass newExc = env->FindClass("java/lang/NullPointerException");
			env->ThrowNew(newExc, "The shape does not exist.");
			return;
		}

		struct AllConvexResultCallback : public btCollisionWorld::ConvexResultCallback {

			AllConvexResultCallback(const btTransform& convexFromWorld, const  btTransform & convexToWorld) : m_convexFromWorld(convexFromWorld), m_convexToWorld(convexToWorld) {
			}
			jobject resultlist;
			JNIEnv* env;
			btTransform m_convexFromWorld; //used to calculate hitPointWorld from hitFraction
			btTransform m_convexToWorld;

			btVector3 m_hitNormalWorld;
			btVector3 m_hitPointWorld;

			virtual btScalar addSingleResult(btCollisionWorld::LocalConvexResult& convexResult, bool normalInWorldSpace) {
				if (normalInWorldSpace) {
					m_hitNormalWorld = convexResult.m_hitNormalLocal;
				}
				else {
					m_hitNormalWorld = convexResult.m_hitCollisionObject->getWorldTransform().getBasis() * convexResult.m_hitNormalLocal;
				}
				m_hitPointWorld.setInterpolate3(m_convexFromWorld.getBasis() * m_convexFromWorld.getOrigin(), m_convexToWorld.getBasis() * m_convexToWorld.getOrigin(), convexResult.m_hitFraction);

				jmeBulletUtil::addSweepResult(env, resultlist, &m_hitNormalWorld, &m_hitPointWorld, convexResult.m_hitFraction, convexResult.m_hitCollisionObject);

				return 1.f;
			}
		};

		btTransform native_to = btTransform();
		jmeBulletUtil::convert(env, to, &native_to);

		btTransform native_from = btTransform();
		jmeBulletUtil::convert(env, from, &native_from);

		btScalar native_allowed_ccd_penetration = btScalar(allowedCcdPenetration);

		AllConvexResultCallback resultCallback(native_from, native_to);
		resultCallback.env = env;
		resultCallback.resultlist = resultlist;
		ghost->convexSweepTest((btConvexShape *)shape, native_from, native_to, resultCallback, native_allowed_ccd_penetration);
		return;
	}

#ifdef __cplusplus
}
#endif
