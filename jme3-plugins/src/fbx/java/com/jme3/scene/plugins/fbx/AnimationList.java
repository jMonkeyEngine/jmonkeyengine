/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.scene.plugins.fbx;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines animations set that will be created while loading FBX scene
 * <p>Animation <code>name</code> is using to access animation via {@link com.jme3.animation.AnimControl}.<br>
 * <code>firstFrame</code> and <code>lastFrame</code> defines animation time interval.<br>
 * Use <code>layerName</code> also to define source animation layer in the case of multiple layers in the scene.<br>
 * Skeletal animations will be created if only scene contain skeletal bones</p>
 */
public class AnimationList {

    List<AnimInverval> list = new ArrayList<>();

    /**
     * Use in the case of multiple animation layers in FBX asset
     *
     * @param name - animation name to access via {@link com.jme3.animation.AnimControl}
     * @param firstFrame the index of the first frame
     * @param lastFrame the index of the last frame
     */
    public void add(String name, int firstFrame, int lastFrame) {
        add(name, null, firstFrame, lastFrame);
    }

    /**
     * Use in the case of multiple animation layers in FBX asset
     *
     * @param name - animation name to access via {@link com.jme3.animation.AnimControl}
     * @param layerName - source layer
     * @param firstFrame the index of the first frame
     * @param lastFrame the index of the last frame
     */
    public void add(String name, String layerName, int firstFrame, int lastFrame) {
        AnimInverval cue = new AnimInverval();
        cue.name = name;
        cue.layerName = layerName;
        cue.firstFrame = firstFrame;
        cue.lastFrame = lastFrame;
        list.add(cue);
    }

    static class AnimInverval {
        String name;
        String layerName;
        int firstFrame;
        int lastFrame;
    }
}
