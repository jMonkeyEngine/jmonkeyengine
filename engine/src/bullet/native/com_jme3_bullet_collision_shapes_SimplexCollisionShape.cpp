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
#include "com_jme3_bullet_collision_shapes_SimplexCollisionShape.h"
#include "jmeBulletUtil.h"

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_collision_shapes_SimplexCollisionShape
     * Method:    createShape
     * Signature: (Lcom/jme3/math/Vector3f;)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_collision_shapes_SimplexCollisionShape_createShape__Lcom_jme3_math_Vector3f_2
    (JNIEnv *env, jobject object, jobject vector1) {
        jmeClasses::initJavaClasses(env);
        btVector3 vec1 = btVector3();
        jmeBulletUtil::convert(env, vector1, &vec1);
        btBU_Simplex1to4* simplexShape = new btBU_Simplex1to4(vec1);
        return reinterpret_cast<jlong>(simplexShape);
    }

    /*
     * Class:     com_jme3_bullet_collision_shapes_SimplexCollisionShape
     * Method:    createShape
     * Signature: (Lcom/jme3/math/Vector3f;Lcom/jme3/math/Vector3f;)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_collision_shapes_SimplexCollisionShape_createShape__Lcom_jme3_math_Vector3f_2Lcom_jme3_math_Vector3f_2
    (JNIEnv *env, jobject object, jobject vector1, jobject vector2) {
        jmeClasses::initJavaClasses(env);
        btVector3 vec1 = btVector3();
        jmeBulletUtil::convert(env, vector1, &vec1);
        btVector3 vec2 = btVector3();
        jmeBulletUtil::convert(env, vector2, &vec2);
        btBU_Simplex1to4* simplexShape = new btBU_Simplex1to4(vec1, vec2);
        return reinterpret_cast<jlong>(simplexShape);
    }
    /*
     * Class:     com_jme3_bullet_collision_shapes_SimplexCollisionShape
     * Method:    createShape
     * Signature: (Lcom/jme3/math/Vector3f;Lcom/jme3/math/Vector3f;Lcom/jme3/math/Vector3f;)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_collision_shapes_SimplexCollisionShape_createShape__Lcom_jme3_math_Vector3f_2Lcom_jme3_math_Vector3f_2Lcom_jme3_math_Vector3f_2
    (JNIEnv * env, jobject object, jobject vector1, jobject vector2, jobject vector3) {
        jmeClasses::initJavaClasses(env);
        btVector3 vec1 = btVector3();
        jmeBulletUtil::convert(env, vector1, &vec1);
        btVector3 vec2 = btVector3();
        jmeBulletUtil::convert(env, vector2, &vec2);
        btVector3 vec3 = btVector3();
        jmeBulletUtil::convert(env, vector3, &vec3);
        btBU_Simplex1to4* simplexShape = new btBU_Simplex1to4(vec1, vec2, vec3);
        return reinterpret_cast<jlong>(simplexShape);
    }
    /*
     * Class:     com_jme3_bullet_collision_shapes_SimplexCollisionShape
     * Method:    createShape
     * Signature: (Lcom/jme3/math/Vector3f;Lcom/jme3/math/Vector3f;Lcom/jme3/math/Vector3f;Lcom/jme3/math/Vector3f;)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_collision_shapes_SimplexCollisionShape_createShape__Lcom_jme3_math_Vector3f_2Lcom_jme3_math_Vector3f_2Lcom_jme3_math_Vector3f_2Lcom_jme3_math_Vector3f_2
    (JNIEnv * env, jobject object, jobject vector1, jobject vector2, jobject vector3, jobject vector4) {
        jmeClasses::initJavaClasses(env);
        btVector3 vec1 = btVector3();
        jmeBulletUtil::convert(env, vector1, &vec1);
        btVector3 vec2 = btVector3();
        jmeBulletUtil::convert(env, vector2, &vec2);
        btVector3 vec3 = btVector3();
        jmeBulletUtil::convert(env, vector3, &vec3);
        btVector3 vec4 = btVector3();
        jmeBulletUtil::convert(env, vector4, &vec4);
        btBU_Simplex1to4* simplexShape = new btBU_Simplex1to4(vec1, vec2, vec3, vec4);
        return reinterpret_cast<jlong>(simplexShape);
    }
#ifdef __cplusplus
}
#endif
