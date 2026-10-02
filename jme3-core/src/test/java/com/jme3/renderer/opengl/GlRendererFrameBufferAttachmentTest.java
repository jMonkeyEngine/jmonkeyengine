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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Tests updates to already initialized framebuffer attachments. */
public class GlRendererFrameBufferAttachmentTest extends FrameBufferTestSupport {
    @Test
    void initialAttachmentAndExplicitDirtyReplacement() {
        Texture2D first = texture();
        Texture2D replacement = texture();
        FrameBuffer fb = framebuffer(first);
        renderer.setFrameBuffer(fb);
        assertEquals(first.getImage().getId(), driver.attachment(fb, 0));
        fb.replaceColorTarget(0, FrameBuffer.FrameBufferTarget.newTarget(replacement));
        fb.setUpdateNeeded();
        renderer.setFrameBuffer(fb);
        assertEquals(replacement.getImage().getId(), driver.attachment(fb, 0));
        assertNotEquals(first.getImage().getId(), replacement.getImage().getId());
    }

    @Test
    void replacementWhileBoundUpdatesNativeAttachment() throws Exception {
        Texture2D replacement = texture();
        renderer.setTexture(0, replacement);
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        fb.replaceColorTarget(0, FrameBuffer.FrameBufferTarget.newTarget(replacement));
        renderer.setFrameBuffer(fb);
        assertEquals(replacement.getImage().getId(), driver.attachment(fb, 0));
    }

    @Test
    void replacementAfterSwitchingAwayUpdatesNativeAttachment() throws Exception {
        Texture2D replacement = texture();
        renderer.setTexture(0, replacement);
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        renderer.setFrameBuffer(null);
        fb.replaceColorTarget(0, FrameBuffer.FrameBufferTarget.newTarget(replacement));
        renderer.setFrameBuffer(fb);
        assertEquals(replacement.getImage().getId(), driver.attachment(fb, 0));
    }

    @Test
    void removedTextureIsDetached() {
        FrameBuffer fb = framebuffer(texture(), texture());
        renderer.setFrameBuffer(fb);
        fb.removeColorTarget(1);
        renderer.setFrameBuffer(fb);
        assertEquals(0, driver.attachment(fb, 1));
    }

    @Test
    void removalReattachesShiftedRenderbufferWithoutReallocatingIt() {
        FrameBuffer fb = bufferedFramebuffer(2);
        renderer.setFrameBuffer(fb);
        FrameBuffer.RenderBuffer removed = fb.getColorTarget(0);
        final int survivor = fb.getColorTarget(1).getId();
        final int allocations = driver.renderbufferStorageCalls.size();
        fb.removeColorTarget(0);
        renderer.setFrameBuffer(fb);
        assertEquals(-1, removed.getId());
        assertEquals(survivor, driver.attachment(fb, 0));
        assertEquals(0, driver.attachment(fb, 1));
        assertEquals(allocations, driver.renderbufferStorageCalls.size());
    }

    @Test
    void addingColorTargetAllocatesOnlyNewStorage() {
        FrameBuffer fb = bufferedFramebuffer(1);
        renderer.setFrameBuffer(fb);
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8));
        renderer.setFrameBuffer(fb);
        assertEquals(3, driver.renderbufferStorageCalls.size());
        assertEquals(fb.getColorTarget(1).getId(), driver.attachment(fb, 1));
    }

    @Test
    void clearingColorsLeavesDepthOnlyFramebuffer() {
        FrameBuffer fb = bufferedFramebuffer(2);
        renderer.setFrameBuffer(fb);
        fb.clearColorTargets();
        renderer.setFrameBuffer(fb);
        assertEquals(0, driver.attachment(fb, 0));
        assertEquals(0, driver.attachment(fb, 1));
        assertEquals(1, driver.liveRenderbuffers.size());
        assertEquals(Arrays.asList(GL.GL_NONE), driver.drawBuffers.get(fb.getId()));
    }

    @Test
    void removingSelectedLastTargetKeepsSelectionValid() {
        FrameBuffer fb = framebuffer(texture(), texture());
        fb.setTargetIndex(1);
        renderer.setFrameBuffer(fb);
        fb.removeColorTarget(1);
        renderer.setFrameBuffer(fb);
        assertEquals(0, fb.getTargetIndex());
        assertEquals(Arrays.asList(GLFbo.GL_COLOR_ATTACHMENT0_EXT), driver.drawBuffers.get(fb.getId()));
        assertThrows(IllegalArgumentException.class, () -> fb.setTargetIndex(1));
    }

    @Test
    void removingEarlierTargetKeepsSelectedSurvivor() {
        FrameBuffer fb = framebuffer(texture(), texture(), texture());
        fb.setTargetIndex(2);
        renderer.setFrameBuffer(fb);
        final int selected = driver.attachment(fb, 2);
        fb.removeColorTarget(0);
        renderer.setFrameBuffer(fb);
        assertEquals(1, fb.getTargetIndex());
        assertEquals(selected, driver.attachment(fb, 1));
        assertEquals(0, driver.attachment(fb, 2));
    }

    @Test
    void mrtChangesUpdateDrawBuffersWithoutDiscardingStorage() {
        FrameBuffer fb = bufferedFramebuffer(2);
        renderer.setFrameBuffer(fb);
        final int allocations = driver.renderbufferStorageCalls.size();
        fb.setMultiTarget(true);
        renderer.setFrameBuffer(fb);
        assertEquals(Arrays.asList(GLFbo.GL_COLOR_ATTACHMENT0_EXT, GLFbo.GL_COLOR_ATTACHMENT0_EXT + 1),
                driver.drawBuffers.get(fb.getId()));
        fb.setMultiTarget(false);
        renderer.setFrameBuffer(fb);
        assertEquals(Arrays.asList(GLFbo.GL_COLOR_ATTACHMENT0_EXT), driver.drawBuffers.get(fb.getId()));
        assertEquals(allocations, driver.renderbufferStorageCalls.size());
    }

    @Test
    void replacingPackedDepthStencilDetachesOldStencil() {
        FrameBuffer fb = framebuffer(texture());
        fb.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.Depth24Stencil8));
        renderer.setFrameBuffer(fb);
        int oldDepth = fb.getDepthTarget().getId();
        assertEquals(oldDepth, driver.attachmentAt(fb, GL_STENCIL_ATTACHMENT));
        fb.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.Depth));
        renderer.setFrameBuffer(fb);
        assertFalse(driver.liveRenderbuffers.contains(oldDepth));
        assertEquals(0, driver.attachmentAt(fb, GL_STENCIL_ATTACHMENT));
        assertEquals(fb.getDepthTarget().getId(), driver.attachmentAt(fb, GLFbo.GL_DEPTH_ATTACHMENT_EXT));
    }

    @Test
    void replacingDepthBufferWithTextureUpdatesAttachment() {
        FrameBuffer fb = bufferedFramebuffer(1);
        renderer.setFrameBuffer(fb);
        Texture2D depth = new Texture2D(8, 8, Image.Format.Depth);
        fb.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(depth));
        renderer.setFrameBuffer(fb);
        assertEquals(depth.getImage().getId(), driver.attachmentAt(fb, GLFbo.GL_DEPTH_ATTACHMENT_EXT));
        assertEquals(1, driver.liveRenderbuffers.size());
    }

    @Test
    void mipmappedReplacementGeneratesOldRenderedTextureBeforeAllocatingNewOne() {
        Texture2D old = texture();
        old.setMinFilter(Texture.MinFilter.Trilinear);
        Texture2D replacement = texture();
        replacement.setMinFilter(Texture.MinFilter.Trilinear);
        FrameBuffer fb = framebuffer(old);
        renderer.setFrameBuffer(fb);
        fb.replaceColorTarget(0, FrameBuffer.FrameBufferTarget.newTarget(replacement));
        renderer.setFrameBuffer(fb);
        assertEquals(Arrays.asList(old.getImage().getId()), driver.mipmapGenerations);
        assertEquals(replacement.getImage().getId(), driver.attachment(fb, 0));
        renderer.setFrameBuffer(null);
        assertEquals(Arrays.asList(old.getImage().getId(), replacement.getImage().getId()),
                driver.mipmapGenerations);
    }

    @Test
    void explicitSrgbStorageChangeStillReallocatesInternalFormat() {
        FrameBuffer fb = bufferedFramebuffer(1);
        renderer.setFrameBuffer(fb);
        int id = fb.getColorTarget(0).getId();
        final int oldFormat = driver.renderbufferFormats.get(id);
        fb.setSrgb(true);
        fb.setUpdateNeeded();
        renderer.setFrameBuffer(fb);
        assertNotEquals(oldFormat, driver.renderbufferFormats.get(id));
        assertEquals(GLExt.GL_SRGB8_ALPHA8_EXT, driver.renderbufferFormats.get(id));
    }
}
