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
package com.jme3.shadow;

import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.system.NullRenderer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for controls on geometries rendered by shadow maps.
 */
public class ShadowControlRenderTest {

    @Test
    public void testSharedAncestorAndCasterControlsRunOnceForShadowOnlyCasters() {
        Node model = new Node("model");
        CountingControl parentControl = new CountingControl();
        model.addControl(parentControl);

        Geometry first = new Geometry("first", new Mesh());
        CountingControl firstControl = new CountingControl();
        first.addControl(firstControl);
        model.attachChild(first);

        Geometry second = new Geometry("second", new Mesh());
        CountingControl secondControl = new CountingControl();
        second.addControl(secondControl);
        model.attachChild(second);

        GeometryList shadowCasters = new GeometryList(null);
        shadowCasters.add(first);
        shadowCasters.add(second);

        RenderManager renderManager = new RenderManager(new NullRenderer());
        ViewPort viewPort = new ViewPort("main", new Camera(640, 480));
        Camera shadowCamera = new Camera(512, 512);
        renderManager.setCamera(shadowCamera, false);

        // The casters are not in the viewport's scene graph; they are present
        // only in the shadow map's caster list.
        ShadowUtil.runControlRenderForShadowCasters(shadowCasters, renderManager, viewPort);

        assertEquals(1, parentControl.renderCount);
        assertEquals(1, firstControl.renderCount);
        assertEquals(1, secondControl.renderCount);
        assertSame(shadowCamera, firstControl.camera);
        assertSame(viewPort, firstControl.viewPort);
    }

    @Test
    public void testControlsRunForEachShadowPass() {
        Node model = new Node("model");
        CountingControl parentControl = new CountingControl();
        model.addControl(parentControl);

        Geometry caster = new Geometry("caster", new Mesh());
        CountingControl casterControl = new CountingControl();
        caster.addControl(casterControl);
        model.attachChild(caster);

        GeometryList shadowCasters = new GeometryList(null);
        shadowCasters.add(caster);

        RenderManager renderManager = new RenderManager(new NullRenderer());
        ViewPort viewPort = new ViewPort("main", new Camera(640, 480));
        ShadowUtil.runControlRenderForShadowCasters(shadowCasters, renderManager, viewPort);
        ShadowUtil.runControlRenderForShadowCasters(shadowCasters, renderManager, viewPort);

        assertEquals(2, parentControl.renderCount);
        assertEquals(2, casterControl.renderCount);
    }

    @Test
    public void testForcedMaterialAndTechniqueAreRestoredAfterSuccessfulControlRender() {
        CountingControl control = new CountingControl();
        Geometry caster = createCaster(control);
        GeometryList shadowCasters = casterList(caster);
        RenderManager renderManager = new RenderManager(new NullRenderer());
        Material forcedMaterial = new Material();
        String forcedTechnique = "ForcedTechnique";
        renderManager.setForcedMaterial(forcedMaterial);
        renderManager.setForcedTechnique(forcedTechnique);

        ShadowUtil.runControlRenderForShadowCasters(shadowCasters, renderManager,
                new ViewPort("main", new Camera(640, 480)));

        assertEquals(1, control.renderCount);
        assertNull(control.forcedMaterialDuringRender);
        assertNull(control.forcedTechniqueDuringRender);
        assertSame(forcedMaterial, renderManager.getForcedMaterial());
        assertEquals(forcedTechnique, renderManager.getForcedTechnique());
    }

    @Test
    public void testForcedMaterialAndTechniqueAreRestoredWhenControlThrows() {
        Geometry caster = createCaster(new ThrowingControl());
        GeometryList shadowCasters = casterList(caster);
        RenderManager renderManager = new RenderManager(new NullRenderer());
        Material forcedMaterial = new Material();
        String forcedTechnique = "ForcedTechnique";
        renderManager.setForcedMaterial(forcedMaterial);
        renderManager.setForcedTechnique(forcedTechnique);

        assertThrows(IllegalStateException.class, () -> ShadowUtil.runControlRenderForShadowCasters(
                shadowCasters, renderManager, new ViewPort("main", new Camera(640, 480))));

        assertSame(forcedMaterial, renderManager.getForcedMaterial());
        assertEquals(forcedTechnique, renderManager.getForcedTechnique());
    }

    private static Geometry createCaster(AbstractControl control) {
        Geometry caster = new Geometry("caster", new Mesh());
        caster.addControl(control);
        return caster;
    }

    private static GeometryList casterList(Geometry caster) {
        GeometryList shadowCasters = new GeometryList(null);
        shadowCasters.add(caster);
        return shadowCasters;
    }

    private static class CountingControl extends AbstractControl {

        private int renderCount;
        private Camera camera;
        private ViewPort viewPort;
        private Material forcedMaterialDuringRender;
        private String forcedTechniqueDuringRender;

        @Override
        protected void controlUpdate(float tpf) {
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            renderCount++;
            camera = rm.getCurrentCamera();
            viewPort = vp;
            forcedMaterialDuringRender = rm.getForcedMaterial();
            forcedTechniqueDuringRender = rm.getForcedTechnique();
        }

    }

    private static class ThrowingControl extends AbstractControl {

        @Override
        protected void controlUpdate(float tpf) {
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            throw new IllegalStateException("Expected test exception");
        }

    }
}
