/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.util.clone.CloneFunction;
import com.jme3.util.clone.Cloner;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the target and blend-space graph of a cloned blend action.
 */
public class BlendActionCloneTest {
    private static final float TOLERANCE = 0.00001f;

    @Test
    public void originalBlendControl() {
        Node original = model();
        blend(original, 0.5f, "a", "b");
        original.updateLogicalState(0.5f);
        assertEquals(7.5f, x(original), TOLERANCE);
    }

    @Test
    public void directClipCloneControl() {
        Node original = model();
        composer(original).setCurrentAction("a");
        Node copy = original.clone(false);
        composer(copy).setCurrentAction("a");
        copy.updateLogicalState(0.5f);
        assertEquals(5f, x(copy), TOLERANCE);
        assertEquals(0f, x(original), TOLERANCE);
    }

    @Test
    public void defaultTransitionCloneAfterTransitionEnds() {
        Node original = model();
        LinearBlendSpace space = new LinearBlendSpace(0, 1);
        space.setValue(0.5f);
        composer(original).actionBlended("blend", space, "a", "b");
        Node copy = original.clone(false);
        composer(copy).setCurrentAction("blend");
        copy.updateLogicalState(0.5f);
        assertEquals(7.5f, x(copy), TOLERANCE);
        assertEquals(0f, x(original), TOLERANCE);
    }

    @Test
    public void clonedBuilderActionTargetsCopiedSpatial() {
        Node original = model();
        BlendAction originalAction = blend(original, 0.5f, "a", "b");
        Node copy = original.clone(false);
        BlendAction copiedAction = action(copy);
        assertNotSame(originalAction, copiedAction);
        assertEquals(1, copiedAction.getTargets().size());
        assertTrue(copiedAction.getTargets().contains(copy.getChild("target")));
        assertFalse(copiedAction.getTargets().contains(original.getChild("target")));
    }

    @Test
    public void clonedBuilderActionBeforePlayback() {
        Node original = model();
        blend(original, 0.5f, "a", "b");
        Node copy = original.clone(false);
        composer(copy).setCurrentAction("blend");
        copy.updateLogicalState(0.5f);
        assertEquals(7.5f, x(copy), TOLERANCE);
        assertEquals(0f, x(original), TOLERANCE);
        original.updateLogicalState(0.75f);
        assertEquals(11.25f, x(original), TOLERANCE);
        assertEquals(7.5f, x(copy), TOLERANCE);
    }

    @Test
    public void clonedBuilderActionDuringPlayback() {
        Node original = model();
        blend(original, 0.5f, "a", "b");
        original.updateLogicalState(0.5f);
        Node copy = original.clone(false);
        composer(copy).setCurrentAction("blend");
        composer(copy).setTime(0.5);
        copy.updateLogicalState(0.25f);
        assertEquals(11.25f, x(copy), TOLERANCE);
        assertEquals(7.5f, x(original), TOLERANCE);
        original.updateLogicalState(0.125f);
        assertEquals(9.375f, x(original), TOLERANCE);
        assertEquals(11.25f, x(copy), TOLERANCE);
    }

    @Test
    public void copiedSpaceValueAndThreeWaySelectionAreIndependent() {
        Node original = model();
        BlendAction originalAction = blend(original, 0.25f, "a", "b", "c");
        original.updateLogicalState(0.5f);
        Node copy = original.clone(false);
        BlendAction copiedAction = action(copy);
        assertNotSame(originalAction.getBlendSpace(), copiedAction.getBlendSpace());
        copiedAction.getBlendSpace().setValue(0.75f);
        composer(copy).setCurrentAction("blend");
        copy.updateLogicalState(0.5f);
        assertEquals(15f, x(copy), TOLERANCE);
        assertEquals(7.5f, x(original), TOLERANCE);
        original.updateLogicalState(0.25f);
        assertEquals(11.25f, x(original), TOLERANCE);
        assertEquals(15f, x(copy), TOLERANCE);
        originalAction.getBlendSpace().setValue(0f);
        copy.updateLogicalState(0.25f);
        assertEquals(22.5f, x(copy), TOLERANCE);
        assertEquals(11.25f, x(original), TOLERANCE);
    }

    @Test
    public void copiedSpaceHandlesZeroAndFullWeights() {
        Node original = model();
        blend(original, 0.5f, "a", "b");
        Node copy = original.clone(false);
        BlendAction copiedAction = action(copy);
        copiedAction.getBlendSpace().setValue(0f);
        copiedAction.interpolate(0.5);
        assertEquals(5f, x(copy), TOLERANCE);
        copiedAction.getBlendSpace().setValue(1f);
        copiedAction.interpolate(0.75);
        assertEquals(15f, x(copy), TOLERANCE);
        assertEquals(0f, x(original), TOLERANCE);
    }

    @Test
    public void copiedSpeedFactorsAreIndependent() {
        Node original = model();
        BlendAction originalAction = blend(original, 0.5f, "a", "b");
        originalAction.setSpeedFactors(2, 4);
        Node copy = original.clone(false);
        BlendAction copiedAction = action(copy);
        assertNotSame(originalAction.getSpeedFactors(), copiedAction.getSpeedFactors());
        copiedAction.getSpeedFactors()[0] = 6;
        originalAction.interpolate(0.5);
        copiedAction.interpolate(0.5);
        assertEquals(3, originalAction.getSpeed(), TOLERANCE);
        assertEquals(5, copiedAction.getSpeed(), TOLERANCE);
        originalAction.getSpeedFactors()[1] = 8;
        assertEquals(4, copiedAction.getSpeedFactors()[1], TOLERANCE);
    }

    @Test
    public void clearedSpeedFactorsRemainCleared() {
        Node original = model();
        BlendAction originalAction = blend(original, 0.5f, "a", "b");
        originalAction.clearSpeedFactors();
        BlendAction copiedAction = action(original.clone(false));
        assertNull(copiedAction.getSpeedFactors());
        assertEquals(1, copiedAction.getSpeed(), TOLERANCE);
    }

    @Test
    public void targetAccumulatorTransformsAreIndependent() {
        Node original = model();
        BlendAction originalAction = blend(original, 0.5f, "a", "b");
        Node copy = original.clone(false);
        TransformCollector originalCollector = new TransformCollector();
        TransformCollector copiedCollector = new TransformCollector();
        originalAction.setCollectTransformDelegate(originalCollector);
        action(copy).setCollectTransformDelegate(copiedCollector);
        originalAction.interpolate(0.5);
        action(copy).interpolate(0.75);
        assertNotSame(originalCollector.collected, copiedCollector.collected);
        assertEquals(7.5f, originalCollector.collected.getTranslation().x, TOLERANCE);
        assertEquals(11.25f, copiedCollector.collected.getTranslation().x, TOLERANCE);
        originalAction.interpolate(0.25);
        assertEquals(3.75f, originalCollector.collected.getTranslation().x, TOLERANCE);
        assertEquals(11.25f, copiedCollector.collected.getTranslation().x, TOLERANCE);
    }

    @Test
    public void customActionCanRetainNoncloneableTarget() {
        SharedTarget target = new SharedTarget();
        LinearBlendSpace space = new LinearBlendSpace(0, 1);
        space.setValue(0.5f);
        BlendAction original = new BlendAction(space,
                new SharedTargetAction(target, 10), new SharedTargetAction(target, 20));
        original.setTransitionLength(0);
        original.interpolate(0.5);
        assertEquals(7.5f, target.getLocalTransform().getTranslation().x, TOLERANCE);
        BlendAction copy = Cloner.deepClone(original);
        assertSame(target, copy.getTargets().iterator().next());
        copy.interpolate(0.75);
        assertEquals(11.25f, target.getLocalTransform().getTranslation().x, TOLERANCE);
    }

    @Test
    public void plainCustomSpaceRemainsSharedWithoutRebinding() {
        Node original = model();
        CustomBlendSpace space = new CustomBlendSpace();
        BlendAction originalAction = composer(original).actionBlended("blend", space, "a", "b");
        originalAction.setTransitionLength(0);
        space.setValue(0.5f);
        Node copy = original.clone(false);
        assertSame(space, action(copy).getBlendSpace());
        assertSame(originalAction, space.action);
        assertEquals(1, space.bindings);
        originalAction.interpolate(0.5);
        assertEquals(7.5f, x(original), TOLERANCE);
        assertEquals(0f, x(copy), TOLERANCE);
    }

    @Test
    public void registeredCloneFunctionControlsCustomSpace() {
        Node original = model();
        CustomBlendSpace space = new CustomBlendSpace();
        BlendAction originalAction = composer(original).actionBlended("blend", space, "a", "b");
        originalAction.setTransitionLength(0);
        space.setValue(0.5f);
        Cloner cloner = new Cloner();
        cloner.setCloneFunction(CustomBlendSpace.class, new CloneFunction<CustomBlendSpace>() {
            @Override
            public CustomBlendSpace cloneObject(Cloner cloner, CustomBlendSpace original) {
                CustomBlendSpace copy = new CustomBlendSpace();
                copy.value = original.value;
                return copy;
            }

            @Override
            public void cloneFields(Cloner cloner, CustomBlendSpace clone, CustomBlendSpace original) {
                clone.action = cloner.clone(original.action);
            }
        });
        BlendAction copiedAction = cloner.clone(originalAction);
        CustomBlendSpace copiedSpace = (CustomBlendSpace) copiedAction.getBlendSpace();
        assertNotSame(space, copiedSpace);
        assertSame(copiedAction, copiedSpace.action);
        copiedAction.interpolate(0.5);
        HasLocalTransform copiedTarget = copiedAction.getTargets().iterator().next();
        assertEquals(7.5f, copiedTarget.getLocalTransform().getTranslation().x, TOLERANCE);
        assertEquals(0f, x(original), TOLERANCE);
    }

    @Test
    public void preassignedCustomSpaceIsRespected() {
        Node original = model();
        CustomBlendSpace space = new CustomBlendSpace();
        BlendAction originalAction = composer(original).actionBlended("blend", space, "a", "b");
        CustomBlendSpace replacement = new CustomBlendSpace();
        Cloner cloner = new Cloner();
        cloner.setClonedValue(space, replacement);
        BlendAction copiedAction = cloner.clone(originalAction);
        assertSame(replacement, copiedAction.getBlendSpace());
        assertEquals(0, replacement.bindings);
        assertSame(originalAction, space.action);
    }

    @Test
    public void directClonerActionThenSpacePreservesTopology() {
        assertDirectCloneTopology(false);
    }

    @Test
    public void directClonerSpaceThenActionPreservesTopology() {
        assertDirectCloneTopology(true);
    }

    private static void assertDirectCloneTopology(boolean spaceFirst) {
        Node original = model();
        BlendAction originalAction = blend(original, 0.25f, "a", "b", "c");
        LinearBlendSpace originalSpace = (LinearBlendSpace) originalAction.getBlendSpace();
        Cloner cloner = new Cloner();
        LinearBlendSpace copiedSpace = spaceFirst ? cloner.clone(originalSpace) : null;
        BlendAction copiedAction = cloner.clone(originalAction);
        if (!spaceFirst) {
            copiedSpace = cloner.clone(originalSpace);
        }
        assertSame(copiedSpace, copiedAction.getBlendSpace());
        assertNotSame(originalSpace, copiedSpace);
        copiedSpace.setValue(0.75f);
        copiedAction.interpolate(0.5);
        HasLocalTransform copiedTarget = copiedAction.getTargets().iterator().next();
        assertNotSame(original.getChild("target"), copiedTarget);
        assertEquals(15f, copiedTarget.getLocalTransform().getTranslation().x, TOLERANCE);
        assertEquals(0f, x(original), TOLERANCE);
        originalAction.interpolate(0.5);
        assertEquals(7.5f, x(original), TOLERANCE);
        assertEquals(15f, copiedTarget.getLocalTransform().getTranslation().x, TOLERANCE);
    }

    private static Node model() {
        Node model = new Node("model");
        Node target = new Node("target");
        model.attachChild(target);
        AnimComposer composer = new AnimComposer();
        model.addControl(composer);
        addClip(composer, target, "a", 10);
        addClip(composer, target, "b", 20);
        addClip(composer, target, "c", 40);
        return model;
    }

    private static void addClip(AnimComposer composer, Node target, String name, float end) {
        AnimClip clip = new AnimClip(name);
        clip.setTracks(new AnimTrack<?>[]{new TransformTrack(target, new float[]{0, 1},
                new Vector3f[]{new Vector3f(), new Vector3f(end, 0, 0)}, null, null)});
        composer.addAnimClip(clip);
    }

    private static BlendAction blend(Node model, float value, String... clips) {
        LinearBlendSpace space = new LinearBlendSpace(0, 1);
        space.setValue(value);
        BlendAction action = composer(model).actionBlended("blend", space, clips);
        action.setTransitionLength(0);
        composer(model).setCurrentAction("blend");
        return action;
    }

    private static AnimComposer composer(Node model) {
        return model.getControl(AnimComposer.class);
    }

    private static BlendAction action(Node model) {
        return (BlendAction) composer(model).getAction("blend");
    }

    private static float x(Node model) {
        return model.getChild("target").getLocalTranslation().x;
    }

    private static class TransformCollector extends BlendableAction {
        private Transform collected;

        @Override
        public Collection<HasLocalTransform> getTargets() {
            return Collections.emptyList();
        }

        @Override
        public void collectTransform(HasLocalTransform target, Transform transform,
                float weight, BlendableAction source) {
            collected = transform;
        }

        @Override
        public void doInterpolate(double time) {
        }
    }

    private static class SharedTarget implements HasLocalTransform {
        private final Transform transform = new Transform();

        @Override
        public void setLocalTransform(Transform value) {
            transform.set(value);
        }

        @Override
        public Transform getLocalTransform() {
            return transform;
        }

        @Override
        public void write(JmeExporter exporter) {
        }

        @Override
        public void read(JmeImporter importer) {
        }
    }

    private static class SharedTargetAction extends BlendableAction {
        private final HasLocalTransform target;
        private final float end;

        SharedTargetAction(HasLocalTransform target, float end) {
            this.target = target;
            this.end = end;
            setLength(1);
        }

        @Override
        public Collection<HasLocalTransform> getTargets() {
            return Collections.singleton(target);
        }

        @Override
        public void collectTransform(HasLocalTransform target, Transform transform,
                float weight, BlendableAction source) {
            target.setLocalTransform(transform);
        }

        @Override
        public void doInterpolate(double time) {
            Transform transform = new Transform();
            transform.setTranslation((float) time * end, 0, 0);
            if (collectTransformDelegate != null) {
                collectTransformDelegate.collectTransform(target, transform, getWeight(), this);
            } else {
                target.setLocalTransform(transform);
            }
        }
    }

    private static class CustomBlendSpace implements BlendSpace {
        private BlendAction action;
        private float value;
        private int bindings;

        @Override
        public void setBlendAction(BlendAction action) {
            this.action = action;
            ++bindings;
        }

        @Override
        public float getWeight() {
            action.setFirstActiveIndex(0);
            action.setSecondActiveIndex(1);
            return value;
        }

        @Override
        public void setValue(float value) {
            this.value = value;
        }
    }
}
