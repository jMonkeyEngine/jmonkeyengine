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

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import org.junit.jupiter.api.Test;

/**
 * Verifies the distinction between fresh defaults and externally changed bindings.
 */
public class RenderContextBindingTest {

    @Test
    public void bindingInvalidationClearsTextureReferencesWithoutAssumingNativeDefaults() {
        RenderContext context = new RenderContext();
        Image image = new Image();
        context.boundTextures[0] = image.getWeakRef();
        context.boundTextures[3] = image.getWeakRef();
        context.textureIndexList.moveToNew(3);
        context.textureIndexList.copyNewToOld();
        context.boundTextureUnit = 3;
        context.boundFBO = 12;
        context.boundFB = new FrameBuffer(4, 4, 1);

        context.invalidateBindings();

        assertEquals(-1, context.boundTextureUnit);
        assertEquals(12, context.boundFBO);
        assertNull(context.boundFB);
        assertFalse(context.isFrameBufferBindingValid());
        for (int unit = 0; unit < context.boundTextures.length; unit++) {
            assertNull(context.boundTextures[unit]);
        }
        assertEquals(0, context.textureIndexList.oldLen);
        assertEquals(0, context.textureIndexList.newLen);
    }

    @Test
    public void resetReturnsBindingsToKnownFreshDefaults() {
        RenderContext context = new RenderContext();
        context.boundFBO = 12;
        context.boundTextureUnit = 3;
        context.invalidateBindings();
        context.reset();
        assertDefaults(context);
    }

    @Test
    public void framebufferOnlyInvalidationPreservesTextureKnowledgeAndUntrustedName() {
        RenderContext context = new RenderContext();
        FrameBuffer framebuffer = new FrameBuffer(4, 4, 1);
        context.setFrameBufferBinding(framebuffer, 12);
        Image image = new Image();
        context.boundTextures[3] = image.getWeakRef();
        context.boundTextureUnit = 3;
        context.textureIndexList.moveToNew(3);
        context.textureIndexList.copyNewToOld();
        context.textureIndexList.moveToNew(0);

        context.invalidateFrameBufferBinding();

        assertNull(context.boundFB);
        assertFalse(context.isFrameBufferBindingValid());
        assertEquals(12, context.boundFBO);
        assertEquals(3, context.boundTextureUnit);
        assertSame(image.getWeakRef(), context.boundTextures[3]);
        assertEquals(1, context.textureIndexList.oldLen);
        assertEquals(1, context.textureIndexList.newLen);
    }

    private static void assertDefaults(RenderContext context) {
        assertEquals(0, context.boundTextureUnit);
        assertEquals(0, context.boundFBO);
        assertNull(context.boundFB);
        assertTrue(context.isFrameBufferBindingValid());
    }
}
