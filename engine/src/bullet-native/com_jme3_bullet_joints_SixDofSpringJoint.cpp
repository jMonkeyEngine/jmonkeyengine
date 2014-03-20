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
#include "com_jme3_bullet_joints_SixDofSpringJoint.h"
#include "jmeBulletUtil.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_jme3_bullet_joints_SixDofSpringJoint
 * Method:    enableString
 * Signature: (JIZ)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_SixDofSpringJoint_enableSpring
  (JNIEnv *env, jobject object, jlong jointId, jint index, jboolean onOff) {
    btGeneric6DofSpringConstraint* joint = reinterpret_cast<btGeneric6DofSpringConstraint*>(jointId);
    joint -> enableSpring(index, onOff);
}


/*
 * Class:     com_jme3_bullet_joints_SixDofSpringJoint
 * Method:    setStiffness
 * Signature: (JIF)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_SixDofSpringJoint_setStiffness
  (JNIEnv *env, jobject object, jlong jointId, jint index, jfloat stiffness) {
    btGeneric6DofSpringConstraint* joint = reinterpret_cast<btGeneric6DofSpringConstraint*>(jointId);
    joint -> setStiffness(index, stiffness);
}

/*
 * Class:     com_jme3_bullet_joints_SixDofSpringJoint
 * Method:    setDamping
 * Signature: (JIF)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_SixDofSpringJoint_setDamping
  (JNIEnv *env, jobject object, jlong jointId, jint index, jfloat damping) {
    btGeneric6DofSpringConstraint* joint = reinterpret_cast<btGeneric6DofSpringConstraint*>(jointId);
    joint -> setDamping(index, damping);
}

/*
 * Class:     com_jme3_bullet_joints_SixDofSpringJoint
 * Method:    setEquilibriumPoint
 * Signature: (JIF)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_SixDofSpringJoint_setEquilibriumPoint__J
  (JNIEnv *env, jobject object, jlong jointId) {
    btGeneric6DofSpringConstraint* joint = reinterpret_cast<btGeneric6DofSpringConstraint*>(jointId);
    joint -> setEquilibriumPoint();
}

/*
 * Class:     com_jme3_bullet_joints_SixDofSpringJoint
 * Method:    setEquilibriumPoint
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_joints_SixDofSpringJoint_setEquilibriumPoint__JI
  (JNIEnv *env, jobject object, jlong jointId, jint index) {
    btGeneric6DofSpringConstraint* joint = reinterpret_cast<btGeneric6DofSpringConstraint*>(jointId);
    joint -> setEquilibriumPoint(index);
}




/*
 * Class:     com_jme3_bullet_joints_SixDofSpringJoint
 * Method:    createJoint
 * Signature: (JJLcom/jme3/math/Vector3f;Lcom/jme3/math/Matrix3f;Lcom/jme3/math/Vector3f;Lcom/jme3/math/Matrix3f;Z)J
 */
JNIEXPORT jlong JNICALL Java_com_jme3_bullet_joints_SixDofSpringJoint_createJoint
    (JNIEnv * env, jobject object, jlong bodyIdA, jlong bodyIdB, jobject pivotA, jobject rotA, jobject pivotB, jobject rotB, jboolean useLinearReferenceFrameA) {
        jmeClasses::initJavaClasses(env);
        btRigidBody* bodyA = reinterpret_cast<btRigidBody*>(bodyIdA);
        btRigidBody* bodyB = reinterpret_cast<btRigidBody*>(bodyIdB);
        btTransform transA;
        jmeBulletUtil::convert(env, pivotA, &transA.getOrigin());
        jmeBulletUtil::convert(env, rotA, &transA.getBasis());
        btTransform transB;
        jmeBulletUtil::convert(env, pivotB, &transB.getOrigin());
        jmeBulletUtil::convert(env, rotB, &transB.getBasis());

        btGeneric6DofSpringConstraint* joint = new btGeneric6DofSpringConstraint(*bodyA, *bodyB, transA, transB, useLinearReferenceFrameA);
        return reinterpret_cast<jlong>(joint);
    }

#ifdef __cplusplus
}
#endif
