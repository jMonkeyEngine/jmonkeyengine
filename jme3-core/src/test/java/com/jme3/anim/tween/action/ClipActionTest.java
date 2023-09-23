/*
 * Copyright (c) 2023 jMonkeyEngine
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

import com.jme3.anim.AnimClip;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Test for ClipAction.
 *
 * @author Saichand Chowdary
 */
public class ClipActionTest {

    /**
     * Test to verify setTransitionLength on BlendableAction does not accept negative values.
     */
    @Test
    public void testSetTransitionLength_negativeInput_exceptionThrown() {
        AnimClip animClip = new AnimClip("clip");
        ClipAction clipAction = new ClipAction(animClip);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> clipAction.setTransitionLength(-1));
        assertEquals("transitionLength must be greater than or equal to 0", thrown.getMessage());
    }

    /**
     * Test to verify setTransitionLength on BlendableAction accepts zero.
     */
    @Test
    public void testSetTransitionLength_zeroInput_noExceptionThrown() {
        AnimClip animClip = new AnimClip("clip");
        ClipAction clipAction = new ClipAction(animClip);
        clipAction.setTransitionLength(0);
        assertEquals(0, clipAction.getTransitionLength(), 0);
    }

    /**
     * Test to verify setTransitionLength on BlendableAction accepts positive values.
     */
    @Test
    public void testSetTransitionLength_positiveNumberInput_noExceptionThrown() {
        AnimClip animClip = new AnimClip("clip");
        ClipAction clipAction = new ClipAction(animClip);
        clipAction.setTransitionLength(1.23d);
        assertEquals(1.23d, clipAction.getTransitionLength(), 0);
    }

}
