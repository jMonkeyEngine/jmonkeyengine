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
#include "com_jme3_bullet_joints_ConeJoint.h"
#include "jmeBulletUtil.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_joints_ConeJoint
     * Method:    setLimit
     * Signature: (JFFF)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_ConeJoint_setLimit
    (JNIEnv * env, jobject object, jlong jointId, jfloat swingSpan1, jfloat swingSpan2, jfloat twistSpan) {
        btConeTwistConstraint* joint = reinterpret_cast<btConeTwistConstraint*>(jointId);
        if (joint == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        //TODO: extended setLimit!
        joint->setLimit(swingSpan1, swingSpan2, twistSpan);
    }

    /*
     * Class:     com_jme3_bullet_joints_ConeJoint
     * Method:    setAngularOnly
     * Signature: (JZ)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_ConeJoint_setAngularOnly
    (JNIEnv * env, jobject object, jlong jointId, jboolean angularOnly) {
        btConeTwistConstraint* joint = reinterpret_cast<btConeTwistConstraint*>(jointId);
        if (joint == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return;
        }
        joint->setAngularOnly(angularOnly);
    }

    /*
     * Class:     com_jme3_bullet_joints_ConeJoint
     * Method:    createJoint
     * Signature: (JJLcom/jme3/math/Vector3f;Lcom/jme3/math/Matrix3f;Lcom/jme3/math/Vector3f;Lcom/jme3/math/Matrix3f;)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_joints_ConeJoint_createJoint
    (JNIEnv * env, jobject object, jlong bodyIdA, jlong bodyIdB, jobject pivotA, jobject rotA, jobject pivotB, jobject rotB) {
        jmeClasses::initJavaClasses(env);
        btRigidBody* bodyA = reinterpret_cast<btRigidBody*>(bodyIdA);
        btRigidBody* bodyB = reinterpret_cast<btRigidBody*>(bodyIdB);
        btMatrix3x3 mtx1 = btMatrix3x3();
        btMatrix3x3 mtx2 = btMatrix3x3();
        btTransform transA = btTransform(mtx1);
        jmeBulletUtil::convert(env, pivotA, &transA.getOrigin());
        jmeBulletUtil::convert(env, rotA, &transA.getBasis());
        btTransform transB = btTransform(mtx2);
        jmeBulletUtil::convert(env, pivotB, &transB.getOrigin());
        jmeBulletUtil::convert(env, rotB, &transB.getBasis());
        btConeTwistConstraint* joint = new btConeTwistConstraint(*bodyA, *bodyB, transA, transB);
        return reinterpret_cast<jlong>(joint);
    }

#ifdef __cplusplus
}
#endif
