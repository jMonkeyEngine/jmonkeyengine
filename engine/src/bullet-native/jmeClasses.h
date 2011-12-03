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
#include <jni.h>

/**
 * Author: Normen Hansen
 */

class jmeClasses {
public:
    static void initJavaClasses(JNIEnv* env);
//    static JNIEnv* env;
    static JavaVM* vm;
    static jclass PhysicsSpace;
    static jmethodID PhysicsSpace_preTick;
    static jmethodID PhysicsSpace_postTick;
    static jmethodID PhysicsSpace_addCollisionEvent;
    static jclass PhysicsGhostObject;
    static jmethodID PhysicsGhostObject_addOverlappingObject;

    static jclass Vector3f;
    static jmethodID Vector3f_set;
    static jmethodID Vector3f_getX;
    static jmethodID Vector3f_getY;
    static jmethodID Vector3f_getZ;
    static jmethodID Vector3f_toArray;
    static jfieldID Vector3f_x;
    static jfieldID Vector3f_y;
    static jfieldID Vector3f_z;
    
    static jclass Quaternion;
    static jmethodID Quaternion_set;
    static jmethodID Quaternion_getX;
    static jmethodID Quaternion_getY;
    static jmethodID Quaternion_getZ;
    static jmethodID Quaternion_getW;
    static jfieldID Quaternion_x;
    static jfieldID Quaternion_y;
    static jfieldID Quaternion_z;
    static jfieldID Quaternion_w;

    static jclass Matrix3f;
    static jmethodID Matrix3f_get;
    static jmethodID Matrix3f_set;
    static jfieldID Matrix3f_m00;
    static jfieldID Matrix3f_m01;
    static jfieldID Matrix3f_m02;
    static jfieldID Matrix3f_m10;
    static jfieldID Matrix3f_m11;
    static jfieldID Matrix3f_m12;
    static jfieldID Matrix3f_m20;
    static jfieldID Matrix3f_m21;
    static jfieldID Matrix3f_m22;

    static jclass PhysicsRay_Class;
    static jmethodID PhysicsRay_newSingleResult;
    static jfieldID PhysicsRay_normalInWorldSpace;
    static jfieldID PhysicsRay_hitfraction;
    static jfieldID PhysicsRay_collisionObject;
    static jclass PhysicsRay_listresult;
    static jmethodID PhysicsRay_addmethod;

    static jclass DebugMeshCallback;
    static jmethodID DebugMeshCallback_addVector;

    static void throwNPE(JNIEnv* env);
private:
    jmeClasses(){};
    ~jmeClasses(){};
};