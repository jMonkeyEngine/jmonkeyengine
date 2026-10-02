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
package com.jme3.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderContext.RenderStateCategory;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.texture.Image;
import java.lang.ref.WeakReference;
import org.junit.jupiter.api.Test;

/**
 * Verifies cache-owned validity without changing the public cached field types.
 */
public class RenderContextStateTest {

    @Test
    public void freshContextHasKnownGlDefaults() {
        RenderContext context = new RenderContext();

        assertFreshDefaults(context);
        assertAllValid(context, true);
    }

    @Test
    public void invalidationPreservesCachedValuesButMakesThemUnknown() {
        RenderContext context = new RenderContext();
        setNondefaultDrawState(context);

        context.invalidate();

        assertEquals(RenderState.FaceCullMode.Front, context.cullMode);
        assertTrue(context.depthTestEnabled);
        assertFalse(context.depthWriteEnabled);
        assertFalse(context.colorWriteEnabled);
        assertEquals(RenderState.TestFunction.Never, context.depthFunc);
        assertEquals(RenderState.BlendMode.Custom, context.blendMode);
        assertEquals(RenderState.BlendFunc.Src_Alpha, context.sfactorRGB);
        assertEquals(RenderState.BlendEquation.Subtract, context.blendEquation);
        assertTrue(context.stencilTest);
        assertEquals(RenderState.TestFunction.Equal, context.frontStencilFunction);
        assertTrue(context.polyOffsetEnabled);
        assertEquals(2f, context.polyOffsetFactor);
        assertEquals(3f, context.polyOffsetUnits);
        assertEquals(3f, context.lineWidth);
        assertTrue(context.wireframe);
        assertAllValid(context, false);
    }

    @Test
    public void aPartialWriteValidatesOnlyItsOwnCategory() {
        RenderContext context = new RenderContext();
        context.invalidate();
        context.depthWriteEnabled = true;

        context.setRenderStateValid(RenderStateCategory.DepthWrite);

        for (RenderStateCategory category : RenderStateCategory.values()) {
            assertEquals(category == RenderStateCategory.DepthWrite,
                    context.isRenderStateValid(category), category.toString());
        }
        context.invalidate();
        assertAllValid(context, false);
    }

    @Test
    public void resetRestoresKnownFreshDefaults() {
        RenderContext context = new RenderContext();
        setNondefaultDrawState(context);
        context.invalidate();

        context.reset();

        assertFreshDefaults(context);
        assertAllValid(context, true);
    }

    @Test
    public void invalidationRetainsExistingCleanupForOtherCaches() {
        RenderContext context = new RenderContext();
        context.clipRectEnabled = true;
        context.pointSize = 8f;
        context.boundShaderProgram = 21;
        context.boundFBO = 22;
        context.boundRB = 23;
        context.boundElementArrayVBO = 24;
        context.boundVertexArray = 25;
        context.boundArrayVBO = 26;
        context.boundPixelPackPBO = 27;
        context.numTexturesSet = 3;
        context.boundTextureUnit = 4;
        context.alphaFunc = RenderState.TestFunction.Never;
        context.srgbWriteEnabled = true;
        context.clearColor.set(ColorRGBA.Red);
        context.boundTextures[0] = new WeakReference<Image>(null);
        context.boundAttribs[0] = new WeakReference<VertexBuffer>(null);
        context.textureIndexList.moveToNew(3);
        context.textureIndexList.copyNewToOld();
        context.attribIndexList.moveToNew(2);
        context.attribIndexList.copyNewToOld();
        WeakReference<BufferObject> previousBuffer = new WeakReference<>(null);
        context.boundBO[0] = previousBuffer;

        context.invalidate();

        assertFalse(context.clipRectEnabled);
        assertEquals(1f, context.pointSize);
        assertEquals(0, context.boundShaderProgram);
        assertEquals(0, context.boundFBO);
        assertEquals(0, context.boundRB);
        assertEquals(0, context.boundElementArrayVBO);
        assertEquals(0, context.boundVertexArray);
        assertEquals(0, context.boundArrayVBO);
        assertEquals(0, context.boundPixelPackPBO);
        assertEquals(0, context.numTexturesSet);
        assertEquals(0, context.boundTextureUnit);
        assertEquals(RenderState.TestFunction.Greater, context.alphaFunc);
        assertFalse(context.srgbWriteEnabled);
        assertEquals(new ColorRGBA(0f, 0f, 0f, 0f), context.clearColor);
        assertNull(context.boundTextures[0]);
        assertNull(context.boundAttribs[0]);
        assertEquals(0, context.textureIndexList.oldLen);
        assertEquals(0, context.textureIndexList.newLen);
        assertEquals(0, context.attribIndexList.oldLen);
        assertEquals(0, context.attribIndexList.newLen);
        // The legacy reset did not change boundBO; this revision preserves that scope.
        assertSame(previousBuffer, context.boundBO[0]);
    }

    @Test
    public void constructorDoesNotCallOverridableResetMethods() {
        RenderContext context = new RenderContext() {
            @Override
            public void reset() {
                throw new AssertionError("Constructor called public reset");
            }

            @Override
            public void invalidate() {
                throw new AssertionError("Constructor called public invalidate");
            }
        };

        assertFreshDefaults(context);
        assertAllValid(context, true);
    }

    private static void assertAllValid(RenderContext context, boolean valid) {
        for (RenderStateCategory category : RenderStateCategory.values()) {
            assertEquals(valid, context.isRenderStateValid(category), category.toString());
        }
    }

    private static void assertFreshDefaults(RenderContext context) {
        assertEquals(RenderState.FaceCullMode.Off, context.cullMode);
        assertFalse(context.depthTestEnabled);
        assertTrue(context.depthWriteEnabled);
        assertTrue(context.colorWriteEnabled);
        assertEquals(RenderState.TestFunction.Less, context.depthFunc);
        assertEquals(RenderState.BlendMode.Off, context.blendMode);
        assertEquals(RenderState.BlendFunc.One, context.sfactorRGB);
        assertEquals(RenderState.BlendFunc.Zero, context.dfactorRGB);
        assertEquals(RenderState.BlendFunc.One, context.sfactorAlpha);
        assertEquals(RenderState.BlendFunc.Zero, context.dfactorAlpha);
        assertEquals(RenderState.BlendEquation.Add, context.blendEquation);
        assertFalse(context.stencilTest);
        assertEquals(RenderState.TestFunction.Always, context.frontStencilFunction);
        assertFalse(context.polyOffsetEnabled);
        assertEquals(0f, context.polyOffsetFactor);
        assertEquals(0f, context.polyOffsetUnits);
        assertEquals(1f, context.lineWidth);
        assertFalse(context.wireframe);
    }

    private static void setNondefaultDrawState(RenderContext context) {
        context.cullMode = RenderState.FaceCullMode.Front;
        context.depthTestEnabled = true;
        context.depthWriteEnabled = false;
        context.colorWriteEnabled = false;
        context.depthFunc = RenderState.TestFunction.Never;
        context.blendMode = RenderState.BlendMode.Custom;
        context.sfactorRGB = RenderState.BlendFunc.Src_Alpha;
        context.blendEquation = RenderState.BlendEquation.Subtract;
        context.stencilTest = true;
        context.frontStencilFunction = RenderState.TestFunction.Equal;
        context.polyOffsetEnabled = true;
        context.polyOffsetFactor = 2f;
        context.polyOffsetUnits = 3f;
        context.lineWidth = 3f;
        context.wireframe = true;
    }
}
