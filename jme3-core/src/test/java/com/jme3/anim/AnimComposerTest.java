/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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

}
