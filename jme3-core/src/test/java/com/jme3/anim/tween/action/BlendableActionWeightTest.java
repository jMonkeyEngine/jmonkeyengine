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

import com.jme3.anim.util.HasLocalTransform;
import com.jme3.math.Transform;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlendableActionWeightTest {
    @Test
    void setWeightAcceptsRangeEndpoints() {
        TestBlendableAction action = new TestBlendableAction();

        action.setWeight(0f);
        assertEquals(0f, action.getWeight(), 0f);

        action.setWeight(1f);
        assertEquals(1f, action.getWeight(), 0f);
    }

    @Test
    void setWeightRejectsValuesOutsideRangeAndNaN() {
        TestBlendableAction action = new TestBlendableAction();

        for (float weight : new float[]{-Float.MIN_VALUE, Math.nextUp(1f),
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN}) {
            assertThrows(IllegalArgumentException.class, () -> action.setWeight(weight));
            assertEquals(1f, action.getWeight(), 0f);
        }
    }

    @Test
    void setMaxTransitionWeightAcceptsRangeEndpoints() {
        TestBlendableAction action = new TestBlendableAction();

        action.setMaxTransitionWeight(0.0);
        assertEquals(0.0, action.getMaxTransitionWeight(), 0.0);

        action.setMaxTransitionWeight(1.0);
        assertEquals(1.0, action.getMaxTransitionWeight(), 0.0);
    }

    @Test
    void setMaxTransitionWeightRejectsValuesOutsideRangeAndNaN() {
        TestBlendableAction action = new TestBlendableAction();
        action.setMaxTransitionWeight(0.5);

        for (double weight : new double[]{-Double.MIN_VALUE, Math.nextUp(1.0),
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN}) {
            assertThrows(IllegalArgumentException.class,
                    () -> action.setMaxTransitionWeight(weight));
            assertEquals(0.5, action.getMaxTransitionWeight(), 0.0);
        }
    }

    private static class TestBlendableAction extends BlendableAction {
        @Override
        protected void doInterpolate(double t) {
        }

        @Override
        public Collection<HasLocalTransform> getTargets() {
            return Collections.emptyList();
        }

        @Override
        public void collectTransform(HasLocalTransform target, Transform transform,
                float weight, BlendableAction source) {
        }
    }
}
