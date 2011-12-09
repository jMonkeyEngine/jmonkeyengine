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
package com.jme3.cinematic;

import com.jme3.export.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author Nehon
 */
public class TimeLine extends HashMap<Integer, KeyFrame> implements Savable {

    protected int keyFramesPerSeconds = 30;
    protected int lastKeyFrameIndex = 0;

    public TimeLine() {
        super();
    }

    public KeyFrame getKeyFrameAtTime(float time) {
        return get(getKeyFrameIndexFromTime(time));
    }

    public KeyFrame getKeyFrameAtIndex(int keyFrameIndex) {
        return get(keyFrameIndex);
    }

    public void addKeyFrameAtTime(float time, KeyFrame keyFrame) {
        addKeyFrameAtIndex(getKeyFrameIndexFromTime(time), keyFrame);
    }

    public void addKeyFrameAtIndex(int keyFrameIndex, KeyFrame keyFrame) {
        put(keyFrameIndex, keyFrame);
        keyFrame.setIndex(keyFrameIndex);
        if (lastKeyFrameIndex < keyFrameIndex) {
            lastKeyFrameIndex = keyFrameIndex;
        }
    }

    public void removeKeyFrame(int keyFrameIndex) {
        remove(keyFrameIndex);
        if (lastKeyFrameIndex == keyFrameIndex) {
            KeyFrame kf = null;
            for (int i = keyFrameIndex; kf == null && i >= 0; i--) {
                kf = getKeyFrameAtIndex(i);
                lastKeyFrameIndex = i;
            }
        }
    }

    public void removeKeyFrame(float time) {
        removeKeyFrame(getKeyFrameIndexFromTime(time));
    }

    public int getKeyFrameIndexFromTime(float time) {
        return Math.round(time * keyFramesPerSeconds);
    }
    
    public float getKeyFrameTime(KeyFrame keyFrame) {
        return (float)keyFrame.getIndex()/(float)keyFramesPerSeconds;
    }

    public Collection<KeyFrame> getAllKeyFrames() {
        return values();
    }

    public int getLastKeyFrameIndex() {
        return lastKeyFrameIndex;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        ArrayList list = new ArrayList();
        list.addAll(values());
        oc.writeSavableArrayList(list, "keyFrames", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        ArrayList list = ic.readSavableArrayList("keyFrames", null);
        for (Iterator it = list.iterator(); it.hasNext();) {
            KeyFrame keyFrame = (KeyFrame) it.next();
            addKeyFrameAtIndex(keyFrame.getIndex(), keyFrame);
        }
    }
}
