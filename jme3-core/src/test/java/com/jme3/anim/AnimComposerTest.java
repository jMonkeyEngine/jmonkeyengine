/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package com.jme3.anim;

import com.jme3.anim.tween.action.Action;
import com.jme3.util.clone.Cloner;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Remy Van Doosselaer
 */
public class AnimComposerTest {

    @Test
    public void testGetAnimClips() {
        AnimComposer composer = new AnimComposer();

        Assert.assertNotNull(composer.getAnimClips());
        Assert.assertEquals(0, composer.getAnimClips().size());
    }

    @Test
    public void testGetAnimClipsNames() {
        AnimComposer composer = new AnimComposer();

        Assert.assertNotNull(composer.getAnimClipsNames());
        Assert.assertEquals(0, composer.getAnimClipsNames().size());
    }

    @Test
    public void testMakeLayer() {
        AnimComposer composer = new AnimComposer();

        final String layerName = "TestLayer";

        composer.makeLayer(layerName, null);

        final Set<String> layers = new TreeSet<>();
        layers.add("Default");
        layers.add(layerName);

        Assert.assertNotNull(composer.getLayer(layerName));
        Assert.assertEquals(layers, composer.getLayerNames());
    }

    @Test
    public void testMakeAction() {
        AnimComposer composer = new AnimComposer();

        final String animName = "TestClip";

        final AnimClip anim = new AnimClip(animName);
        composer.addAnimClip(anim);

        final Action action = composer.makeAction(animName);

        Assert.assertNotNull(action);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAnimClipsIsNotModifiable() {
        AnimComposer composer = new AnimComposer();

        composer.getAnimClips().add(new AnimClip("test"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAnimClipsNamesIsNotModifiable() {
        AnimComposer composer = new AnimComposer();

        composer.getAnimClipsNames().add("test");
    }

    @Test
    public void testHasDefaultLayer() {
        AnimComposer composer = new AnimComposer();

        AnimLayer defaultLayer = composer.getLayer("Default");
        Assert.assertNotNull(defaultLayer);
    }

    @Test
    /**
     * https://github.com/jMonkeyEngine/jmonkeyengine/issues/2341
     *
     */
    public void testMissingDefaultLayerIssue2341() {
        AnimComposer composer = new AnimComposer();
        composer.removeLayer(AnimComposer.DEFAULT_LAYER);

        AnimComposer clone = (AnimComposer) composer.jmeClone();
        clone.cloneFields(new Cloner(), composer);
        Assert.assertNotNull(clone.getLayer(AnimComposer.DEFAULT_LAYER));
    }

}
