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
package com.jme3.anim.tween;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.anim.tween.action.BaseAction;
import com.jme3.util.clone.CloneFunction;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.IdentityCloneFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests playback-state ownership and custom delegate policies when cloning tweens. */
public class TweensCloneTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("wrappers")
    public void optedInDelegateUsesTheSameCloner(String name, UnaryOperator<Tween> wrapper) {
        RecordingTween leaf = new RecordingTween(1);
        Tween source = wrapper.apply(leaf);
        Cloner cloner = new Cloner();
        Tween copy = cloner.clone(source);
        assertNotSame(source, copy);
        RecordingTween copiedLeaf = cloner.clone(leaf);
        copy.interpolate(0.25);
        assertEquals(0, leaf.calls);
        assertEquals(1, copiedLeaf.calls);
        source.interpolate(0.5);
        assertEquals(1, leaf.calls);
        assertEquals(1, copiedLeaf.calls);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("wrappers")
    public void unsupportedCustomDelegateRemainsShared(String name, UnaryOperator<Tween> wrapper) {
        PlainTween leaf = new PlainTween();
        Tween copy = Cloner.deepClone(wrapper.apply(leaf));
        copy.interpolate(0.25);
        assertEquals(1, leaf.calls);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("wrappers")
    public void explicitCustomMappingIsHonored(String name, UnaryOperator<Tween> wrapper) {
        PlainTween leaf = new PlainTween();
        PlainTween replacement = new PlainTween();
        Cloner cloner = new Cloner();
        cloner.setClonedValue(leaf, replacement);
        cloner.clone(wrapper.apply(leaf)).interpolate(0.25);
        assertEquals(0, leaf.calls);
        assertEquals(1, replacement.calls);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("wrappers")
    public void registeredCustomCloneFunctionIsHonored(String name, UnaryOperator<Tween> wrapper) {
        PlainTween leaf = new PlainTween();
        Cloner cloner = new Cloner();
        cloner.setCloneFunction(PlainTween.class, new CloneFunction<PlainTween>() {
            @Override
            public PlainTween cloneObject(Cloner cloner, PlainTween original) {
                return new PlainTween();
            }

            @Override
            public void cloneFields(Cloner cloner, PlainTween clone, PlainTween original) {
            }
        });
        cloner.clone(wrapper.apply(leaf)).interpolate(0.25);
        assertEquals(0, leaf.calls);
        assertEquals(1, cloner.clone(leaf).calls);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("wrappers")
    public void explicitIdentityPolicyCanShareCloneableDelegates(String name, UnaryOperator<Tween> wrapper) {
        RecordingTween leaf = new RecordingTween(1);
        Cloner cloner = new Cloner();
        cloner.setCloneFunction(RecordingTween.class, new IdentityCloneFunction<RecordingTween>());
        cloner.clone(wrapper.apply(leaf)).interpolate(0.25);
        assertEquals(1, leaf.calls);
        assertSame(leaf, cloner.clone(leaf));
    }

    @Test
    public void javaCloneMethodAloneDoesNotOptInCustomTweens() {
        PlainTween leaf = new PlainTween() {
            @Override
            public Object clone() {
                throw new AssertionError("Ordinary Tween.clone() must not be called implicitly");
            }
        };
        Cloner.deepClone(new BaseAction(Tweens.sequence(leaf))).interpolate(0.25);
        assertEquals(1, leaf.calls);
    }

    @Test
    public void optedInCloneFailuresAreNotSilentlyShared() {
        RecordingTween leaf = new RecordingTween(1) {
            @Override
            public AbstractTween jmeClone() {
                throw new IllegalStateException("clone failure");
            }
        };
        assertThrows(IllegalStateException.class, () -> Cloner.deepClone(new BaseAction(leaf)));
        assertThrows(IllegalStateException.class, () -> Cloner.deepClone(Tweens.sequence(leaf)));
    }

    @Test
    public void sharedDelegateArraysAndRepeatedLeavesPreserveTheirAliases() {
        RecordingTween leaf = new RecordingTween(1);
        Tween[] delegates = {leaf, leaf, new PlainTween()};
        Tween source = Tweens.sequence(Tweens.sequence(delegates), Tweens.parallel(delegates));
        Tween[] children = children(Cloner.deepClone(source));
        Tween[] sequence = children(children[0]);
        Tween[] parallel = children(children[1]);
        assertSame(sequence, parallel);
        assertNotSame(delegates, sequence);
        assertSame(sequence[0], sequence[1]);
        assertNotSame(leaf, sequence[0]);
        assertSame(delegates[2], sequence[2]);
        sequence[0] = Tweens.delay(2);
        assertSame(sequence[0], parallel[0]);
        assertSame(leaf, delegates[0]);
    }

    @Test
    public void explicitDelegateArrayMappingIsHonored() {
        Tween[] delegates = {new PlainTween()};
        Tween[] replacement = {new PlainTween()};
        Cloner cloner = new Cloner();
        cloner.setClonedValue(delegates, replacement);
        assertSame(replacement, children(cloner.clone(Tweens.sequence(delegates))));
        assertSame(replacement, children(cloner.clone(Tweens.parallel(delegates))));
    }

    @Test
    public void registeredDelegateArrayPolicyIsHonored() {
        Tween[] delegates = {new PlainTween()};
        Cloner cloner = new Cloner();
        cloner.setCloneFunction(Tween[].class, new IdentityCloneFunction<Tween[]>());
        assertSame(delegates, children(cloner.clone(Tweens.sequence(delegates))));
        assertSame(delegates, children(cloner.clone(Tweens.parallel(delegates))));
    }

    @Test
    public void cycleUsesOneClonedLeafInBothDirections() {
        RecordingTween leaf = new RecordingTween(1);
        Tween[] cycle = children(Cloner.deepClone(Tweens.cycle(leaf)));
        assertNotSame(leaf, cycle[0]);
        assertSame(cycle[0], children(cycle[1])[0]);
    }

    @Test
    public void sequenceCursorIsCopiedAndResetsIndependently() {
        RecordingTween first = new RecordingTween(1);
        RecordingTween second = new RecordingTween(1);
        Tween source = Tweens.sequence(first, second);
        source.interpolate(1.25);
        Cloner cloner = new Cloner();
        Tween copy = cloner.clone(source);
        RecordingTween copiedFirst = cloner.clone(first);
        RecordingTween copiedSecond = cloner.clone(second);
        copy.interpolate(1.5);
        assertEquals(1, copiedFirst.calls);
        assertEquals(0.5, copiedSecond.time);
        assertEquals(0.25, second.time);
        source.interpolate(1.75);
        copy.interpolate(0.25);
        assertEquals(0.75, second.time);
        assertEquals(0.25, copiedFirst.time);
        assertEquals(0.5, copiedSecond.time);
    }

    @Test
    public void parallelDoneFlagsAreCopiedAndResetIndependently() {
        RecordingTween shortLeaf = new RecordingTween(1);
        RecordingTween longLeaf = new RecordingTween(2);
        Tween source = Tweens.parallel(shortLeaf, longLeaf);
        assertTrue(source.interpolate(1.25));
        Cloner cloner = new Cloner();
        Tween copy = cloner.clone(source);
        RecordingTween copiedShort = cloner.clone(shortLeaf);
        RecordingTween copiedLong = cloner.clone(longLeaf);
        assertFalse(copy.interpolate(2));
        assertEquals(1, copiedShort.calls);
        assertEquals(2, copiedLong.calls);
        assertTrue(source.interpolate(1.5));
        assertEquals(2, longLeaf.calls);
        assertTrue(copy.interpolate(0.25));
        assertEquals(2, copiedShort.calls);
        assertTrue(source.interpolate(1.75));
        assertEquals(1, shortLeaf.calls);
        assertEquals(3, longLeaf.calls);
    }

    @Test
    public void loopCursorIsCopiedAndResetsIndependently() {
        RecordingTween leaf = new RecordingTween(1);
        Tween source = Tweens.loopCount(3, leaf);
        source.interpolate(1.25);
        Cloner cloner = new Cloner();
        Tween copy = cloner.clone(source);
        RecordingTween copiedLeaf = cloner.clone(leaf);
        copy.interpolate(1.5);
        assertEquals(3, copiedLeaf.calls);
        assertEquals(0.5, copiedLeaf.time);
        assertEquals(0.25, leaf.time);
        source.interpolate(2.25);
        copy.interpolate(0.75);
        assertEquals(0.25, leaf.time);
        assertEquals(0.75, copiedLeaf.time);
    }

    @Test
    public void callbacksKeepTargetsAndPayloadsSharedEvenWhenMapped() {
        CallbackTarget target = new CallbackTarget();
        CallbackTarget mappedTarget = new CallbackTarget();
        PlainTween payload = new PlainTween();
        PlainTween mappedPayload = new PlainTween();
        Cloner cloner = new Cloner();
        cloner.setClonedValue(target, mappedTarget);
        cloner.setClonedValue(payload, mappedPayload);
        Tween callback = Tweens.sequence(Tweens.callMethod(target, "call", payload),
                Tweens.callTweenMethod(1, target, "interpolate", payload));
        Tween copy = cloner.clone(new BaseAction(callback));
        copy.interpolate(0.25);
        assertEquals(2, target.calls);
        assertSame(payload, target.payload);
        assertEquals(0.25, target.time);
        assertEquals(0, mappedTarget.calls);
        callback.interpolate(0.5);
        assertEquals(4, target.calls);
        assertEquals(0.5, target.time);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("wrappers")
    public void constructorsAlreadyRejectNullDelegates(String name, UnaryOperator<Tween> wrapper) {
        assertThrows(NullPointerException.class, () -> wrapper.apply(null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("containers")
    public void nullAddedAfterConstructionStillFailsAtInterpolation(String name,
            UnaryOperator<Tween> wrapper) {
        Tween container = wrapper.apply(Tweens.delay(1));
        BaseAction source = new BaseAction(container);
        // The caller can mutate the array returned by ContainsTweens after construction.
        children(container)[0] = null;
        BaseAction copy = assertDoesNotThrow(() -> Cloner.deepClone(source));
        assertThrows(NullPointerException.class, () -> copy.interpolate(0.25));
        assertThrows(NullPointerException.class, () -> source.interpolate(0.25));
    }

    @Test
    public void nullMappedBaseActionTweenCanBeClonedAgain() {
        Tween tween = Tweens.delay(1);
        Cloner cloner = new Cloner();
        cloner.setClonedValue(tween, null);
        BaseAction firstCopy = cloner.clone(new BaseAction(tween));
        BaseAction secondCopy = assertDoesNotThrow(() -> Cloner.deepClone(firstCopy));
        assertThrows(NullPointerException.class, () -> firstCopy.interpolate(0.25));
        assertThrows(NullPointerException.class, () -> secondCopy.interpolate(0.25));
    }

    @Test
    public void nullMappedDelegateArrayCanBeClonedAgain() {
        Tween[] delegates = {Tweens.delay(1)};
        Tween sequence = Tweens.sequence(delegates);
        Cloner cloner = new Cloner();
        cloner.setClonedValue(delegates, null);
        Tween firstCopy = cloner.clone(sequence);
        Tween secondCopy = assertDoesNotThrow(() -> Cloner.deepClone(firstCopy));
        assertNull(children(secondCopy));
        assertThrows(NullPointerException.class, () -> secondCopy.interpolate(0.25));
    }

    private static Stream<Arguments> containers() {
        return Stream.of(
                wrapper("sequence", tween -> Tweens.sequence(tween)),
                wrapper("parallel", tween -> Tweens.parallel(tween)),
                wrapper("stretch", tween -> Tweens.stretch(2, tween)),
                wrapper("loop count", tween -> Tweens.loopCount(2, tween)),
                wrapper("loop duration", tween -> Tweens.loopDuration(2.5, tween)),
                wrapper("invert", tween -> Tweens.invert(tween)));
    }

    private static Stream<Arguments> wrappers() {
        return Stream.of(
                wrapper("base action", tween -> new BaseAction(tween)),
                wrapper("sequence", tween -> Tweens.sequence(tween)),
                wrapper("parallel", tween -> Tweens.parallel(tween)),
                wrapper("stretch", tween -> Tweens.stretch(2, tween)),
                wrapper("loop count", tween -> Tweens.loopCount(2, tween)),
                wrapper("loop duration", tween -> Tweens.loopDuration(2.5, tween)),
                wrapper("sine", tween -> Tweens.sineStep(tween)),
                wrapper("smooth", tween -> Tweens.smoothStep(tween)),
                wrapper("invert", tween -> Tweens.invert(tween)));
    }

    private static Arguments wrapper(String name, UnaryOperator<Tween> wrapper) {
        return Arguments.of(name, wrapper);
    }

    private static Tween[] children(Tween tween) {
        return ((ContainsTweens) tween).getTweens();
    }

    private static class PlainTween implements Tween {
        int calls;

        @Override
        public double getLength() {
            return 1;
        }

        @Override
        public boolean interpolate(double time) {
            calls++;
            return time < 1;
        }
    }

    private static class RecordingTween extends AbstractTween {
        int calls;
        double time;

        RecordingTween(double length) {
            super(length);
        }

        @Override
        protected void doInterpolate(double time) {
            calls++;
            this.time = time;
        }
    }

    private static class CallbackTarget {
        int calls;
        Object payload;
        double time;

        public void call(Object payload) {
            calls++;
            this.payload = payload;
        }

        public void interpolate(double time, Object payload) {
            call(payload);
            this.time = time;
        }
    }
}
