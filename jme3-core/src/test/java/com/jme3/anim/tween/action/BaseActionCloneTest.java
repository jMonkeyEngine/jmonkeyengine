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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.ArmatureMask;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests cloned composite actions through normal scene and composer APIs. */
public class BaseActionCloneTest {

    private static final float TOLERANCE = 0.00001f;

    @Test
    public void originalSequenceAndDirectClipCloneControls() {
        Node source = model();
        sequence(source);
        source.updateLogicalState(0.5f);
        assertEquals(5, x(source), TOLERANCE);

        Node copy = source.clone(false);
        composer(copy).setCurrentAction("a");
        copy.updateLogicalState(0.75f);
        assertEquals(5, x(source), TOLERANCE);
        assertEquals(7.5f, x(copy), TOLERANCE);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 0.5, 1.5})
    public void cloneBeforeDuringAndAfterFirstClip(double time) {
        Node source = model();
        sequence(source);
        if (time > 0) {
            source.updateLogicalState((float) time);
        }
        final float originalPosition = x(source);
        Node copy = source.clone(false);

        // Cloning deliberately clears the layer's current action; select it again.
        assertNull(composer(copy).getCurrentAction());
        composer(copy).setCurrentAction("sequence");
        composer(copy).setTime(time);
        float step = time == 0 ? 0.5f : 0.25f;
        copy.updateLogicalState(step);

        float expected = time < 1 ? (float) (10 * (time + step))
                : (float) (20 * (time - 1 + step));
        assertEquals(originalPosition, x(source), TOLERANCE);
        assertEquals(expected, x(copy), TOLERANCE);
        assertNotSame(composer(source).getAction("sequence"), composer(copy).getAction("sequence"));
    }

    @Test
    public void restartingCloneDoesNotResetOriginalSequenceCursor() {
        Node source = model();
        sequence(source);
        source.updateLogicalState(1.5f);
        Node copy = source.clone(false);
        composer(copy).setCurrentAction("sequence");
        copy.updateLogicalState(0.5f);
        assertEquals(10, x(source), TOLERANCE);
        assertEquals(5, x(copy), TOLERANCE);

        source.updateLogicalState(0.25f);
        assertEquals(15, x(source), TOLERANCE);
        assertEquals(5, x(copy), TOLERANCE);
        copy.updateLogicalState(0.25f);
        assertEquals(15, x(source), TOLERANCE);
        assertEquals(7.5f, x(copy), TOLERANCE);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("wrappers")
    public void wrappedSequenceAnimatesClonedTarget(String name, UnaryOperator<Tween> wrapper,
            float time, double expected) {
        Node source = model();
        AnimComposer composer = composer(source);
        composer.actionSequence("wrapped", wrapper.apply(composer.action("a")));
        Node copy = source.clone(false);
        composer(copy).setCurrentAction("wrapped");
        // Sample after the default clip transition to isolate tween ownership.
        copy.updateLogicalState(time);
        assertEquals(0, x(source), TOLERANCE);
        assertEquals(expected, x(copy), TOLERANCE);
    }

    private static Stream<Arguments> wrappers() {
        return Stream.of(
                wrapper("sequence", tween -> Tweens.sequence(tween), 0.75f, 7.5),
                wrapper("parallel", tween -> Tweens.parallel(tween), 0.75f, 7.5),
                wrapper("stretch", tween -> Tweens.stretch(2, tween), 1.5f, 7.5),
                wrapper("loop count", tween -> Tweens.loopCount(2, tween), 0.75f, 7.5),
                wrapper("loop duration", tween -> Tweens.loopDuration(2.5, tween), 0.75f, 7.5),
                wrapper("sine", tween -> Tweens.sineStep(tween), 0.75f, 5 * (1 + Math.sqrt(0.5))),
                wrapper("smooth", tween -> Tweens.smoothStep(tween), 0.75f, 8.4375),
                wrapper("invert", tween -> Tweens.invert(tween), 0.25f, 7.5),
                wrapper("cycle", tween -> Tweens.cycle(tween), 0.75f, 7.5),
                wrapper("nested", tween -> Tweens.sequence(Tweens.parallel(
                        Tweens.stretch(1, Tweens.loopCount(2, Tweens.invert(
                                Tweens.smoothStep(tween)))))), 0.25f, 5));
    }

    private static Arguments wrapper(String name, UnaryOperator<Tween> wrapper, float time, double expected) {
        return Arguments.of(name, wrapper, time, expected);
    }

    @Test
    public void nestedNamedSequencesUseClonedActions() {
        Node source = model();
        AnimComposer composer = composer(source);
        composer.actionSequence("nested", sequence(source), composer.action("a"));
        Node copy = source.clone(false);
        composer(copy).setCurrentAction("nested");
        copy.updateLogicalState(0.5f);
        assertEquals(0, x(source), TOLERANCE);
        assertEquals(5, x(copy), TOLERANCE);
    }

    @Test
    public void maskPropagationUsesTheSameClonedActionGraph() {
        Node source = model();
        sequence(source);
        Node copy = source.clone(false);
        BaseAction copied = (BaseAction) composer(copy).getAction("sequence");
        ArmatureMask mask = new ArmatureMask();
        copied.setMask(mask);
        assertSame(mask, composer(copy).getAction("a").getMask());
        assertSame(mask, composer(copy).getAction("b").getMask());
        assertNull(composer(source).getAction("a").getMask());
        assertNull(composer(source).getAction("b").getMask());

        copied.setMaskPropagationEnabled(false);
        copied.setMask(null);
        assertSame(mask, composer(copy).getAction("a").getMask());
    }

    private static BaseAction sequence(Node model) {
        AnimComposer composer = composer(model);
        BaseAction action = composer.actionSequence("sequence", composer.action("a"), composer.action("b"));
        composer.setCurrentAction("sequence");
        return action;
    }

    private static Node model() {
        Node model = new Node("model");
        Node target = new Node("target");
        model.attachChild(target);
        AnimComposer composer = new AnimComposer();
        model.addControl(composer);
        addClip(composer, target, "a", 10);
        addClip(composer, target, "b", 20);
        return model;
    }

    private static void addClip(AnimComposer composer, Node target, String name, float end) {
        AnimClip clip = new AnimClip(name);
        clip.setTracks(new AnimTrack<?>[] {new TransformTrack(target, new float[] {0, 1},
                new Vector3f[] {new Vector3f(), new Vector3f(end, 0, 0)}, null, null)});
        composer.addAnimClip(clip);
    }

    private static AnimComposer composer(Node model) {
        return model.getControl(AnimComposer.class);
    }

    private static float x(Node model) {
        return model.getChild("target").getLocalTranslation().x;
    }
}
