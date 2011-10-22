
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
