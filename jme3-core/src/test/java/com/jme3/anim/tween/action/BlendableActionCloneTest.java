/*
 * Copyright (c) 2026 jMonkeyEngine
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
package com.jme3.anim.tween.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.util.clone.Cloner;
import org.junit.jupiter.api.Test;

/**
 * Verify that cloned actions own their animation transition state.
 */
public class BlendableActionCloneTest {
    private static final float TOLERANCE = 1e-6f;

    @Test
    public void unclonedActionUpdatesItsOwnTransition() {
        ClipAction action = createAction(new Node());
        action.interpolate(0.25);
        assertEquals(0.25f, action.getTransitionWeight(), TOLERANCE);
        action.interpolate(0.75);
        assertEquals(0.75f, action.getTransitionWeight(), TOLERANCE);
    }

    @Test
    public void clonedActionUpdatesItsOwnTransition() {
        ClipAction original = createAction(new Node());
        ClipAction clone = Cloner.deepClone(original);
        clone.interpolate(0.25);
        assertEquals(0.25f, clone.getTransitionWeight(), TOLERANCE);
        assertEquals(1f, original.getTransitionWeight(), TOLERANCE);
    }

    @Test
    public void clonedActionDoesNotChangeOriginalTransition() {
        ClipAction original = createAction(new Node());
        original.interpolate(0.25);
        ClipAction clone = Cloner.deepClone(original);
        clone.interpolate(0.75);
        assertEquals(0.25f, original.getTransitionWeight(), TOLERANCE);
        assertEquals(0.75f, clone.getTransitionWeight(), TOLERANCE);
    }

    @Test
    public void clonedActionUsesItsOwnMaximumTransitionWeight() {
        ClipAction original = createAction(new Node());
        original.setMaxTransitionWeight(0.25);
        ClipAction clone = Cloner.deepClone(original);
        clone.setMaxTransitionWeight(0.75);
        clone.interpolate(1.0);
        assertEquals(0.75f, clone.getTransitionWeight(), TOLERANCE);
        original.interpolate(1.0);
        assertEquals(0.25f, original.getTransitionWeight(), TOLERANCE);
    }

    @Test
    public void clonedReverseActionUpdatesItsOwnTransition() {
        ClipAction original = createAction(new Node());
        original.setSpeed(-1.0);
        ClipAction clone = Cloner.deepClone(original);
        clone.interpolate(1.75);
        assertEquals(0.25f, clone.getTransitionWeight(), TOLERANCE);
        assertEquals(1f, original.getTransitionWeight(), TOLERANCE);
    }

    @Test
    public void clonedActionCanDisableItsTransitionIndependently() {
        ClipAction original = createAction(new Node());
        original.interpolate(0.25);
        ClipAction clone = Cloner.deepClone(original);
        clone.setTransitionLength(0.0);
        clone.interpolate(0.0);
        assertEquals(1f, clone.getTransitionWeight(), TOLERANCE);
        assertEquals(0.25f, original.getTransitionWeight(), TOLERANCE);
    }

    @Test
    public void clonedActionPreservesEffectiveTransitionLength() {
        ClipAction original = createAction(new Node());
        original.setTransitionLength(4.0);
        original.interpolate(1.0); // Clamp the internal transition to the two-second clip.
        assertEquals(0.5f, original.getTransitionWeight(), TOLERANCE);
        ClipAction clone = Cloner.deepClone(original);
        clone.setLength(4.0);
        clone.interpolate(0.5);
        assertEquals(0.25f, clone.getTransitionWeight(), TOLERANCE);
        assertEquals(0.5f, original.getTransitionWeight(), TOLERANCE);
    }

    @Test
    public void clonedModelBlendsItsOwnTarget() {
        Node model = new Node("model");
        AnimComposer composer = new AnimComposer();
        model.addControl(composer);
        ClipAction action = createAction(model);
        composer.addAnimClip(action.getAnimClip());
        composer.addAction("move", action);

        Node clone = (Node) model.clone();
        AnimComposer clonedComposer = clone.getControl(AnimComposer.class);
        assertNotSame(composer.getAction("move"), clonedComposer.getAction("move"));
        clonedComposer.setCurrentAction("move");
        clone.updateLogicalState(0.25f);

        // The clip's position is 8; a quarter-second transition should blend to 2.
        assertEquals(2f, clone.getLocalTranslation().x, TOLERANCE);
        assertEquals(0f, model.getLocalTranslation().x, TOLERANCE);
        assertEquals(1f, action.getTransitionWeight(), TOLERANCE);
    }

    private static ClipAction createAction(Node target) {
        TransformTrack track = new TransformTrack(target, new float[]{0f, 2f},
                new Vector3f[]{new Vector3f(8f, 0f, 0f), new Vector3f(8f, 0f, 0f)}, null, null);
        AnimClip clip = new AnimClip("move");
        clip.setTracks(new AnimTrack[]{track});
        ClipAction action = new ClipAction(clip);
        action.setTransitionLength(1.0);
        return action;
    }
}
