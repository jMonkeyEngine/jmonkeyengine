/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.material;

import com.jme3.asset.AssetManager;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.NullRenderer;
import com.jme3.system.TestUtil;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify that rendering geometry whose world scale reverses the winding order
 * doesn't corrupt the face cull mode of other geometry. This was issue #2914 at
 * GitHub.
 *
 * <p>Material.updateRenderState() flips the cull mode of backward-normal
 * geometry, but used to do so on the RenderState returned by
 * RenderState.copyMergedTo() -- which, when the material has no additional
 * render state, is the shared technique/default RenderState rather than a
 * per-material copy. The flip then leaked into every other geometry sharing
 * that RenderState, culling their front faces and making them vanish.
 *
 * @author Dieter De Paepe
 */
public class TestIssue2914 {

    private final EnumSet<Caps> caps = EnumSet.of(
            Caps.GLSL100, Caps.GLSL110, Caps.GLSL120,
            Caps.GLSL130, Caps.GLSL140, Caps.GLSL150);

    private FaceCullMode lastAppliedCullMode;

    private final RenderManager renderManager = new RenderManager(new NullRenderer() {
        @Override
        public EnumSet<Caps> getCaps() {
            return caps;
        }

        @Override
        public void applyRenderState(RenderState state) {
            lastAppliedCullMode = state.getFaceCullMode();
        }
    });

    private Geometry geometry(Material material, float scaleX) {
        Geometry geometry = new Geometry("geom", new Box(1, 1, 1));
        geometry.setMaterial(material);
        geometry.setLocalScale(scaleX, 1f, 1f);
        geometry.updateGeometricState();
        return geometry;
    }

    @Test
    public void backwardNormalsDoNotLeakCullModeToOtherGeometry() {
        AssetManager assetManager = TestUtil.createAssetManager();
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        // A mirrored geometry (one negative scale component) has backward normals,
        // so its own cull mode is legitimately flipped from Back to Front.
        material.render(geometry(material, -1f), renderManager);
        assertEquals(FaceCullMode.Front, lastAppliedCullMode,
                "mirrored geometry should have its cull mode flipped to Front");

        // An ordinary geometry rendered afterwards with the same material must
        // still cull Back. Before the fix the flip above mutated a shared
        // RenderState, so this came out Front.
        material.render(geometry(material, 1f), renderManager);
        assertEquals(FaceCullMode.Back, lastAppliedCullMode,
                "unmirrored geometry must still cull Back -- the earlier flip must "
                        + "not leak through a shared RenderState");
    }
}
