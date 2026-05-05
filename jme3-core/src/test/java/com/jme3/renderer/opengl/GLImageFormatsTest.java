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
package com.jme3.renderer.opengl;

import com.jme3.renderer.Caps;
import com.jme3.texture.Image;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GLImageFormatsTest {

    @Test
    public void testGles3UsesCoreHalfFloatType() {
        EnumSet<Caps> caps = EnumSet.of(Caps.OpenGLES20, Caps.OpenGLES30,
                Caps.CoreProfile, Caps.FloatTexture, Caps.HalfFloatTexture);

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);

        assertEquals(GLExt.GL_HALF_FLOAT_ARB,
                formats[0][Image.Format.R16F.ordinal()].dataType);
        assertEquals(GLExt.GL_HALF_FLOAT_ARB,
                formats[0][Image.Format.RGBA16F.ordinal()].dataType);
    }

    @Test
    public void testGles3DoesNotExposeDesktopByteOrderFormats() {
        EnumSet<Caps> caps = EnumSet.of(Caps.OpenGLES20, Caps.OpenGLES30,
                Caps.CoreProfile, Caps.Srgb);

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);

        assertNull(formats[0][Image.Format.BGR8.ordinal()]);
        assertNull(formats[0][Image.Format.ABGR8.ordinal()]);
        assertNull(formats[0][Image.Format.ARGB8.ordinal()]);
        assertNull(formats[0][Image.Format.BGRA8.ordinal()]);
        assertNull(formats[1][Image.Format.BGR8.ordinal()]);
        assertNull(formats[1][Image.Format.ABGR8.ordinal()]);
        assertNull(formats[1][Image.Format.ARGB8.ordinal()]);
        assertNull(formats[1][Image.Format.BGRA8.ordinal()]);
    }

    @Test
    public void testGles3LegacyAlphaUsesGlesInternalFormatWhenNoCoreProfile() {
        EnumSet<Caps> caps = EnumSet.of(Caps.OpenGLES20, Caps.OpenGLES30);

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);

        assertEquals(GL.GL_ALPHA,
                formats[0][Image.Format.Alpha8.ordinal()].internalFormat);
        assertEquals(GL.GL_ALPHA,
                formats[0][Image.Format.Alpha8.ordinal()].format);
    }

    @Test
    public void testGles3CoreFormatsRemainMapped() {
        EnumSet<Caps> caps = EnumSet.of(Caps.OpenGLES20, Caps.OpenGLES30,
                Caps.CoreProfile, Caps.Srgb, Caps.FloatTexture,
                Caps.IntegerTexture, Caps.PackedFloatTexture,
                Caps.SharedExponentTexture, Caps.TextureCompressionETC2,
                Caps.Depth24, Caps.FloatDepthBuffer, Caps.PackedDepthStencilBuffer);

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);

        assertNotNull(formats[0][Image.Format.RGB10A2.ordinal()]);
        assertNotNull(formats[0][Image.Format.RGB111110F.ordinal()]);
        assertNotNull(formats[0][Image.Format.RGB9E5.ordinal()]);
        assertNotNull(formats[0][Image.Format.RGBA8UI.ordinal()]);
        assertNotNull(formats[0][Image.Format.ETC2.ordinal()]);
        assertNotNull(formats[0][Image.Format.Depth32F.ordinal()]);
        assertNotNull(formats[0][Image.Format.Depth24Stencil8.ordinal()]);
    }

    @Test
    public void testDepthFormatsFollowExplicitCaps() {
        EnumSet<Caps> caps = EnumSet.of(Caps.OpenGLES20, Caps.OpenGLES30,
                Caps.CoreProfile);

        GLImageFormat[][] formats = GLImageFormats.getFormatsForCaps(caps);

        assertNull(formats[0][Image.Format.Depth24.ordinal()]);
        assertNull(formats[0][Image.Format.Depth32.ordinal()]);
        assertNull(formats[0][Image.Format.Depth32F.ordinal()]);
        assertNull(formats[0][Image.Format.Depth24Stencil8.ordinal()]);

        caps.add(Caps.Depth24);
        caps.add(Caps.FloatDepthBuffer);
        caps.add(Caps.PackedDepthStencilBuffer);
        formats = GLImageFormats.getFormatsForCaps(caps);

        assertNotNull(formats[0][Image.Format.Depth24.ordinal()]);
        assertNull(formats[0][Image.Format.Depth32.ordinal()]);
        assertNotNull(formats[0][Image.Format.Depth32F.ordinal()]);
        assertNotNull(formats[0][Image.Format.Depth24Stencil8.ordinal()]);

        caps.add(Caps.Depth32);
        formats = GLImageFormats.getFormatsForCaps(caps);

        assertNotNull(formats[0][Image.Format.Depth32.ordinal()]);
    }
}
