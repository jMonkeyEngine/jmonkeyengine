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
#include "com_jme3_bullet_collision_shapes_CompoundCollisionShape.h"
#include "jmeBulletUtil.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
     * Class:     com_jme3_bullet_collision_shapes_CompoundCollisionShape
     * Method:    createShape
     * Signature: ()J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_collision_shapes_CompoundCollisionShape_createShape
    (JNIEnv *env, jobject object) {
        jmeClasses::initJavaClasses(env);
        btCompoundShape* shape = new btCompoundShape();
        return reinterpret_cast<jlong>(shape);
    }

    /*
     * Class:     com_jme3_bullet_collision_shapes_CompoundCollisionShape
     * Method:    addChildShape
     * Signature: (JJLcom/jme3/math/Vector3f;Lcom/jme3/math/Matrix3f;)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_collision_shapes_CompoundCollisionShape_addChildShape
    (JNIEnv *env, jobject object, jlong compoundId, jlong childId, jobject childLocation, jobject childRotation) {
        btCompoundShape* shape = reinterpret_cast<btCompoundShape*>(compoundId);
        if (shape == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        btCollisionShape* child = reinterpret_cast<btCollisionShape*>(childId);
        if (shape == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        btMatrix3x3 mtx = btMatrix3x3();
        btTransform trans = btTransform(mtx);
        jmeBulletUtil::convert(env, childLocation, &trans.getOrigin());
        jmeBulletUtil::convert(env, childRotation, &trans.getBasis());
        shape->addChildShape(trans, child);
        return 0;
    }

    /*
     * Class:     com_jme3_bullet_collision_shapes_CompoundCollisionShape
     * Method:    removeChildShape
     * Signature: (JJ)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_collision_shapes_CompoundCollisionShape_removeChildShape
    (JNIEnv * env, jobject object, jlong compoundId, jlong childId) {
        btCompoundShape* shape = reinterpret_cast<btCompoundShape*>(compoundId);
        if (shape == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        btCollisionShape* child = reinterpret_cast<btCollisionShape*>(childId);
        if (shape == NULL) {
            jclass newExc = env->FindClass("java/lang/NullPointerException");
            env->ThrowNew(newExc, "The native object does not exist.");
            return 0;
        }
        shape->removeChildShape(child);
        return 0;
    }

#ifdef __cplusplus
}
#endif
