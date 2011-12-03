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
#include <math.h>
#include "jmeBulletUtil.h"

/**
 * Author: Normen Hansen,Empire Phoenix, Lutherion
 */
void jmeBulletUtil::convert(JNIEnv* env, jobject in, btVector3* out) {
    if (in == NULL || out == NULL) {
        jmeClasses::throwNPE(env);
    }
    float x = env->GetFloatField(in, jmeClasses::Vector3f_x); //env->CallFloatMethod(in, jmeClasses::Vector3f_getX);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float y = env->GetFloatField(in, jmeClasses::Vector3f_y); //env->CallFloatMethod(in, jmeClasses::Vector3f_getY);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float z = env->GetFloatField(in, jmeClasses::Vector3f_z); //env->CallFloatMethod(in, jmeClasses::Vector3f_getZ);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    out->setX(x);
    out->setY(y);
    out->setZ(z);
}

void jmeBulletUtil::convert(JNIEnv* env, const btVector3* in, jobject out) {
    if (in == NULL || out == NULL) {
        jmeClasses::throwNPE(env);
    }
    float x = in->getX();
    float y = in->getY();
    float z = in->getZ();
    env->SetFloatField(out, jmeClasses::Vector3f_x, x);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Vector3f_y, y);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Vector3f_z, z);
    //    env->CallObjectMethod(out, jmeClasses::Vector3f_set, x, y, z);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
}

void jmeBulletUtil::convert(JNIEnv* env, jobject in, btMatrix3x3* out) {
    if (in == NULL || out == NULL) {
        jmeClasses::throwNPE(env);
    }
    float m00 = env->GetFloatField(in, jmeClasses::Matrix3f_m00);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float m01 = env->GetFloatField(in, jmeClasses::Matrix3f_m01);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float m02 = env->GetFloatField(in, jmeClasses::Matrix3f_m02);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float m10 = env->GetFloatField(in, jmeClasses::Matrix3f_m10);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float m11 = env->GetFloatField(in, jmeClasses::Matrix3f_m11);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float m12 = env->GetFloatField(in, jmeClasses::Matrix3f_m12);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float m20 = env->GetFloatField(in, jmeClasses::Matrix3f_m20);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float m21 = env->GetFloatField(in, jmeClasses::Matrix3f_m21);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float m22 = env->GetFloatField(in, jmeClasses::Matrix3f_m22);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    out->setValue(m00, m01, m02, m10, m11, m12, m20, m21, m22);
}

void jmeBulletUtil::convert(JNIEnv* env, const btMatrix3x3* in, jobject out) {
    if (in == NULL || out == NULL) {
        jmeClasses::throwNPE(env);
    }
    float m00 = in->getRow(0).m_floats[0];
    float m01 = in->getRow(0).m_floats[1];
    float m02 = in->getRow(0).m_floats[2];
    float m10 = in->getRow(1).m_floats[0];
    float m11 = in->getRow(1).m_floats[1];
    float m12 = in->getRow(1).m_floats[2];
    float m20 = in->getRow(2).m_floats[0];
    float m21 = in->getRow(2).m_floats[1];
    float m22 = in->getRow(2).m_floats[2];
    env->SetFloatField(out, jmeClasses::Matrix3f_m00, m00);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Matrix3f_m01, m01);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Matrix3f_m02, m02);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Matrix3f_m10, m10);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Matrix3f_m11, m11);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Matrix3f_m12, m12);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Matrix3f_m20, m20);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Matrix3f_m21, m21);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Matrix3f_m22, m22);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
}

void jmeBulletUtil::convertQuat(JNIEnv* env, jobject in, btMatrix3x3* out) {
    if (in == NULL || out == NULL) {
        jmeClasses::throwNPE(env);
    }
    float x = env->GetFloatField(in, jmeClasses::Quaternion_x);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float y = env->GetFloatField(in, jmeClasses::Quaternion_y);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float z = env->GetFloatField(in, jmeClasses::Quaternion_z);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    float w = env->GetFloatField(in, jmeClasses::Quaternion_w);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }

    float norm = w * w + x * x + y * y + z * z;
    float s = (norm == 1.0) ? 2.0 : (norm > 0.1) ? 2.0 / norm : 0.0;

    // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
    // will be used 2-4 times each.
    float xs = x * s;
    float ys = y * s;
    float zs = z * s;
    float xx = x * xs;
    float xy = x * ys;
    float xz = x * zs;
    float xw = w * xs;
    float yy = y * ys;
    float yz = y * zs;
    float yw = w * ys;
    float zz = z * zs;
    float zw = w * zs;

    // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
    out->setValue(1.0 - (yy + zz), (xy - zw), (xz + yw),
            (xy + zw), 1 - (xx + zz), (yz - xw),
            (xz - yw), (yz + xw), 1.0 - (xx + yy));
}

void jmeBulletUtil::convertQuat(JNIEnv* env, const btMatrix3x3* in, jobject out) {
    if (in == NULL || out == NULL) {
        jmeClasses::throwNPE(env);
    }
    // the trace is the sum of the diagonal elements; see
    // http://mathworld.wolfram.com/MatrixTrace.html
    float t = in->getRow(0).m_floats[0] + in->getRow(1).m_floats[1] + in->getRow(2).m_floats[2];
    float w, x, y, z;
    // we protect the division by s by ensuring that s>=1
    if (t >= 0) { // |w| >= .5
        float s = sqrt(t + 1); // |s|>=1 ...
        w = 0.5f * s;
        s = 0.5f / s; // so this division isn't bad
        x = (in->getRow(2).m_floats[1] - in->getRow(1).m_floats[2]) * s;
        y = (in->getRow(0).m_floats[2] - in->getRow(2).m_floats[0]) * s;
        z = (in->getRow(1).m_floats[0] - in->getRow(0).m_floats[1]) * s;
    } else if ((in->getRow(0).m_floats[0] > in->getRow(1).m_floats[1]) && (in->getRow(0).m_floats[0] > in->getRow(2).m_floats[2])) {
        float s = sqrt(1.0f + in->getRow(0).m_floats[0] - in->getRow(1).m_floats[1] - in->getRow(2).m_floats[2]); // |s|>=1
        x = s * 0.5f; // |x| >= .5
        s = 0.5f / s;
        y = (in->getRow(1).m_floats[0] + in->getRow(0).m_floats[1]) * s;
        z = (in->getRow(0).m_floats[2] + in->getRow(2).m_floats[0]) * s;
        w = (in->getRow(2).m_floats[1] - in->getRow(1).m_floats[2]) * s;
    } else if (in->getRow(1).m_floats[1] > in->getRow(2).m_floats[2]) {
        float s = sqrt(1.0f + in->getRow(1).m_floats[1] - in->getRow(0).m_floats[0] - in->getRow(2).m_floats[2]); // |s|>=1
        y = s * 0.5f; // |y| >= .5
        s = 0.5f / s;
        x = (in->getRow(1).m_floats[0] + in->getRow(0).m_floats[1]) * s;
        z = (in->getRow(2).m_floats[1] + in->getRow(1).m_floats[2]) * s;
        w = (in->getRow(0).m_floats[2] - in->getRow(2).m_floats[0]) * s;
    } else {
        float s = sqrt(1.0f + in->getRow(2).m_floats[2] - in->getRow(0).m_floats[0] - in->getRow(1).m_floats[1]); // |s|>=1
        z = s * 0.5f; // |z| >= .5
        s = 0.5f / s;
        x = (in->getRow(0).m_floats[2] + in->getRow(2).m_floats[0]) * s;
        y = (in->getRow(2).m_floats[1] + in->getRow(1).m_floats[2]) * s;
        w = (in->getRow(1).m_floats[0] - in->getRow(0).m_floats[1]) * s;
    }

    env->SetFloatField(out, jmeClasses::Quaternion_x, x);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Quaternion_y, y);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Quaternion_z, z);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->SetFloatField(out, jmeClasses::Quaternion_w, w);
    //    env->CallObjectMethod(out, jmeClasses::Quaternion_set, x, y, z, w);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
}

void jmeBulletUtil::addResult(JNIEnv* env, jobject resultlist, btVector3 hitnormal, btVector3 m_hitPointWorld, btScalar m_hitFraction, btCollisionObject* hitobject) {

    jobject singleresult = env->AllocObject(jmeClasses::PhysicsRay_Class);
    jobject hitnormalvec = env->AllocObject(jmeClasses::Vector3f);

    convert(env, const_cast<btVector3*> (&hitnormal), hitnormalvec);
    jmeUserPointer *up1 = (jmeUserPointer*) hitobject -> getUserPointer();

    env->SetObjectField(singleresult, jmeClasses::PhysicsRay_normalInWorldSpace, hitnormalvec);
    env->SetFloatField(singleresult, jmeClasses::PhysicsRay_hitfraction, m_hitFraction);

    env->SetObjectField(singleresult, jmeClasses::PhysicsRay_collisionObject, up1->javaCollisionObject);
    env->CallVoidMethod(resultlist, jmeClasses::PhysicsRay_addmethod, singleresult);
    if (env->ExceptionCheck()) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
}
