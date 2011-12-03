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
#include "jmeClasses.h"
#include <stdio.h>

/**
 * Author: Normen Hansen,Empire Phoenix, Lutherion
 */
//public fields
jclass jmeClasses::PhysicsSpace;
jmethodID jmeClasses::PhysicsSpace_preTick;
jmethodID jmeClasses::PhysicsSpace_postTick;
jmethodID jmeClasses::PhysicsSpace_addCollisionEvent;

jclass jmeClasses::PhysicsGhostObject;
jmethodID jmeClasses::PhysicsGhostObject_addOverlappingObject;

jclass jmeClasses::Vector3f;
jmethodID jmeClasses::Vector3f_set;
jmethodID jmeClasses::Vector3f_toArray;
jmethodID jmeClasses::Vector3f_getX;
jmethodID jmeClasses::Vector3f_getY;
jmethodID jmeClasses::Vector3f_getZ;
jfieldID jmeClasses::Vector3f_x;
jfieldID jmeClasses::Vector3f_y;
jfieldID jmeClasses::Vector3f_z;

jclass jmeClasses::Quaternion;
jmethodID jmeClasses::Quaternion_set;
jmethodID jmeClasses::Quaternion_getX;
jmethodID jmeClasses::Quaternion_getY;
jmethodID jmeClasses::Quaternion_getZ;
jmethodID jmeClasses::Quaternion_getW;
jfieldID jmeClasses::Quaternion_x;
jfieldID jmeClasses::Quaternion_y;
jfieldID jmeClasses::Quaternion_z;
jfieldID jmeClasses::Quaternion_w;

jclass jmeClasses::Matrix3f;
jmethodID jmeClasses::Matrix3f_set;
jmethodID jmeClasses::Matrix3f_get;
jfieldID jmeClasses::Matrix3f_m00;
jfieldID jmeClasses::Matrix3f_m01;
jfieldID jmeClasses::Matrix3f_m02;
jfieldID jmeClasses::Matrix3f_m10;
jfieldID jmeClasses::Matrix3f_m11;
jfieldID jmeClasses::Matrix3f_m12;
jfieldID jmeClasses::Matrix3f_m20;
jfieldID jmeClasses::Matrix3f_m21;
jfieldID jmeClasses::Matrix3f_m22;

jclass jmeClasses::DebugMeshCallback;
jmethodID jmeClasses::DebugMeshCallback_addVector;

jclass jmeClasses::PhysicsRay_Class;
jmethodID jmeClasses::PhysicsRay_newSingleResult;

jfieldID jmeClasses::PhysicsRay_normalInWorldSpace;
jfieldID jmeClasses::PhysicsRay_hitfraction;
jfieldID jmeClasses::PhysicsRay_collisionObject;

jclass jmeClasses::PhysicsRay_listresult;
jmethodID jmeClasses::PhysicsRay_addmethod;

//private fields
//JNIEnv* jmeClasses::env;
JavaVM* jmeClasses::vm;

void jmeClasses::initJavaClasses(JNIEnv* env) {
//    if (env != NULL) {
//        fprintf(stdout, "Check Java VM state\n");
//        fflush(stdout);
//        int res = vm->AttachCurrentThread((void**) &jmeClasses::env, NULL);
//        if (res < 0) {
//            fprintf(stdout, "** ERROR: getting Java env!\n");
//            if (res == JNI_EVERSION) fprintf(stdout, "GetEnv Error because of different JNI Version!\n");
//            fflush(stdout);
//        }
//        return;
//    }
    if(PhysicsSpace!=NULL) return;
    fprintf(stdout, "Bullet-Native: Initializing java classes\n");
    fflush(stdout);
//    jmeClasses::env = env;
    env->GetJavaVM(&vm);

    PhysicsSpace = (jclass)env->NewGlobalRef(env->FindClass("com/jme3/bullet/PhysicsSpace"));
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }

    PhysicsSpace_preTick = env->GetMethodID(PhysicsSpace, "preTick_native", "(F)V");
    PhysicsSpace_postTick = env->GetMethodID(PhysicsSpace, "postTick_native", "(F)V");
    PhysicsSpace_addCollisionEvent = env->GetMethodID(PhysicsSpace, "addCollisionEvent_native","(Lcom/jme3/bullet/collision/PhysicsCollisionObject;Lcom/jme3/bullet/collision/PhysicsCollisionObject;J)V");
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }

    PhysicsGhostObject = (jclass)env->NewGlobalRef(env->FindClass("com/jme3/bullet/objects/PhysicsGhostObject"));
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    PhysicsGhostObject_addOverlappingObject = env->GetMethodID(PhysicsGhostObject, "addOverlappingObject_native","(Lcom/jme3/bullet/collision/PhysicsCollisionObject;)V");
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }

    Vector3f = (jclass)env->NewGlobalRef(env->FindClass("com/jme3/math/Vector3f"));
    Vector3f_set = env->GetMethodID(Vector3f, "set", "(FFF)Lcom/jme3/math/Vector3f;");
    Vector3f_toArray = env->GetMethodID(Vector3f, "toArray", "([F)[F");
    Vector3f_getX = env->GetMethodID(Vector3f, "getX", "()F");
    Vector3f_getY = env->GetMethodID(Vector3f, "getY", "()F");
    Vector3f_getZ = env->GetMethodID(Vector3f, "getZ", "()F");
    Vector3f_x = env->GetFieldID(Vector3f, "x", "F");
    Vector3f_y = env->GetFieldID(Vector3f, "y", "F");
    Vector3f_z = env->GetFieldID(Vector3f, "z", "F");

    Quaternion = (jclass)env->NewGlobalRef(env->FindClass("com/jme3/math/Quaternion"));
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    Quaternion_set = env->GetMethodID(Quaternion, "set", "(FFFF)Lcom/jme3/math/Quaternion;");
    Quaternion_getW = env->GetMethodID(Quaternion, "getW", "()F");
    Quaternion_getX = env->GetMethodID(Quaternion, "getX", "()F");
    Quaternion_getY = env->GetMethodID(Quaternion, "getY", "()F");
    Quaternion_getZ = env->GetMethodID(Quaternion, "getZ", "()F");
    Quaternion_x = env->GetFieldID(Quaternion, "x", "F");
    Quaternion_y = env->GetFieldID(Quaternion, "y", "F");
    Quaternion_z = env->GetFieldID(Quaternion, "z", "F");
    Quaternion_w = env->GetFieldID(Quaternion, "w", "F");

    Matrix3f = (jclass)env->NewGlobalRef(env->FindClass("com/jme3/math/Matrix3f"));
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    Matrix3f_set = env->GetMethodID(Matrix3f, "set", "(IIF)Lcom/jme3/math/Matrix3f;");
    Matrix3f_get = env->GetMethodID(Matrix3f, "get", "(II)F");
    Matrix3f_m00 = env->GetFieldID(Matrix3f, "m00", "F");
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    Matrix3f_m01 = env->GetFieldID(Matrix3f, "m01", "F");
    Matrix3f_m02 = env->GetFieldID(Matrix3f, "m02", "F");
    Matrix3f_m10 = env->GetFieldID(Matrix3f, "m10", "F");
    Matrix3f_m11 = env->GetFieldID(Matrix3f, "m11", "F");
    Matrix3f_m12 = env->GetFieldID(Matrix3f, "m12", "F");
    Matrix3f_m20 = env->GetFieldID(Matrix3f, "m20", "F");
    Matrix3f_m21 = env->GetFieldID(Matrix3f, "m21", "F");
    Matrix3f_m22 = env->GetFieldID(Matrix3f, "m22", "F");

    DebugMeshCallback = (jclass)env->NewGlobalRef(env->FindClass("com/jme3/bullet/util/DebugMeshCallback"));
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }

    DebugMeshCallback_addVector = env->GetMethodID(DebugMeshCallback, "addVector", "(FFFII)V");
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }

    PhysicsRay_Class = (jclass)env->NewGlobalRef(env->FindClass("com/jme3/bullet/collision/PhysicsRayTestResult"));
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }

    PhysicsRay_newSingleResult = env->GetMethodID(PhysicsRay_Class,"<init>","()V");
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }

    PhysicsRay_normalInWorldSpace = env->GetFieldID(PhysicsRay_Class,"hitNormalLocal","Lcom/jme3/math/Vector3f;");
        if (env->ExceptionCheck()) {
            env->Throw(env->ExceptionOccurred());
            return;
        }


    PhysicsRay_hitfraction = env->GetFieldID(PhysicsRay_Class,"hitFraction","F");
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }


    PhysicsRay_collisionObject = env->GetFieldID(PhysicsRay_Class,"collisionObject","Lcom/jme3/bullet/collision/PhysicsCollisionObject;");
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }

    PhysicsRay_listresult = env->FindClass("java/util/List");
    PhysicsRay_listresult = (jclass)env->NewGlobalRef(PhysicsRay_listresult);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }

    PhysicsRay_addmethod = env->GetMethodID(PhysicsRay_listresult,"add","(Ljava/lang/Object;)Z");
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
}

void jmeClasses::throwNPE(JNIEnv* env) {
    if (env == NULL) return;
    jclass newExc = env->FindClass("java/lang/NullPointerException");
    env->ThrowNew(newExc, "");
    return;
}
