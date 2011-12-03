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
#include "jmeMotionState.h"
#include "jmeBulletUtil.h"

/**
 * Author: Normen Hansen
 */

jmeMotionState::jmeMotionState() {
    trans = new btTransform();
    trans -> setIdentity();
    worldTransform = *trans;
    dirty = true;
}

void jmeMotionState::getWorldTransform(btTransform& worldTrans) const {
    worldTrans = worldTransform;
}

void jmeMotionState::setWorldTransform(const btTransform& worldTrans) {
    worldTransform = worldTrans;
    dirty = true;
}

void jmeMotionState::setKinematicTransform(const btTransform& worldTrans) {
    worldTransform = worldTrans;
    dirty = true;
}

void jmeMotionState::setKinematicLocation(JNIEnv* env, jobject location) {
    jmeBulletUtil::convert(env, location, &worldTransform.getOrigin());
    dirty = true;
}

void jmeMotionState::setKinematicRotation(JNIEnv* env, jobject rotation) {
    jmeBulletUtil::convert(env, rotation, &worldTransform.getBasis());
    dirty = true;
}

void jmeMotionState::setKinematicRotationQuat(JNIEnv* env, jobject rotation) {
    jmeBulletUtil::convertQuat(env, rotation, &worldTransform.getBasis());
    dirty = true;
}

bool jmeMotionState::applyTransform(JNIEnv* env, jobject location, jobject rotation) {
    if (dirty) {
        //        fprintf(stdout, "Apply world translation\n");
        //        fflush(stdout);
        jmeBulletUtil::convert(env, &worldTransform.getOrigin(), location);
        jmeBulletUtil::convertQuat(env, &worldTransform.getBasis(), rotation);
        dirty = false;
        return true;
    }
    return false;
}

jmeMotionState::~jmeMotionState() {
    free(trans);
}
