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
#include "com_jme3_bullet_collision_shapes_GImpactCollisionShape.h"
#include "jmeBulletUtil.h"
#include <BulletCollision/Gimpact/btGImpactShape.h>

#ifdef __cplusplus
extern "C" {
#endif

    /*
     * Class:     com_jme3_bullet_collision_shapes_GImpactCollisionShape
     * Method:    createShape
     * Signature: (J)J
     */
    JNIEXPORT jlong JNICALL Java_com_jme3_bullet_collision_shapes_GImpactCollisionShape_createShape
    (JNIEnv * env, jobject object, jlong meshId) {
        jmeClasses::initJavaClasses(env);
        btTriangleIndexVertexArray* array = reinterpret_cast<btTriangleIndexVertexArray*>(meshId);
        btGImpactMeshShape* shape = new btGImpactMeshShape(array);
        shape->updateBound();
        return reinterpret_cast<jlong>(shape);
    }

    /*
     * Class:     com_jme3_bullet_collision_shapes_GImpactCollisionShape
     * Method:    recalcAabb
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_shapes_GImpactCollisionShape_recalcAabb
    (JNIEnv *env, jobject object, jlong shapeId) {
        btGImpactMeshShape *pShape
                = reinterpret_cast<btGImpactMeshShape *> (shapeId);
        pShape->updateBound();
    }

    /*
     * Class:     com_jme3_bullet_collision_shapes_GImpactCollisionShape
     * Method:    finalizeNative
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL Java_com_jme3_bullet_collision_shapes_GImpactCollisionShape_finalizeNative
    (JNIEnv * env, jobject object, jlong meshId) {
        btTriangleIndexVertexArray* array = reinterpret_cast<btTriangleIndexVertexArray*> (meshId);
        delete(array);
    }

#ifdef __cplusplus
}
#endif
