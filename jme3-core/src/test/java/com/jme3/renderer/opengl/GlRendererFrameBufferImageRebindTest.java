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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.TextureUnitException;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import org.junit.jupiter.api.Test;

/** Deleted texture storage must be reattached without manually dirtying the FBO. */
public class GlRendererFrameBufferImageRebindTest extends FrameBufferTestSupport {
    private void assertCurrentTexture(FrameBuffer fb, Texture2D texture) {
        assertTrue(texture.getImage().getId() > 0);
        assertEquals(texture.getImage().getId(), driver.attachment(fb, 0));
        assertEquals(driver.textureVersions.get(texture.getImage().getId()),
                driver.attachedTextureVersions.get(fb.getId()).get(GLFbo.GL_COLOR_ATTACHMENT0_EXT));
    }

    @Test
    void boundColorImageIsRecreatedAndReattached() {
        Texture2D texture = texture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        renderer.deleteImage(texture.getImage());
        assertEquals(0, driver.attachment(fb, 0));
        assertFalse(fb.isUpdateNeeded());
        renderer.setFrameBuffer(fb);
        assertCurrentTexture(fb, texture);
        renderer.cleanup();
    }

    @Test
    void unboundImageUploadedBeforeBindingIsReattached() throws TextureUnitException {
        Texture2D texture = texture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        final int oldId = texture.getImage().getId();
        renderer.setFrameBuffer(null);
        renderer.deleteImage(texture.getImage());
        assertEquals(oldId, driver.attachment(fb, 0), "Unbound FBO retains the deleted object");
        renderer.setTexture(0, texture);
        assertNotEquals(oldId, texture.getImage().getId());
        renderer.setFrameBuffer(fb);
        assertCurrentTexture(fb, texture);
        renderer.cleanup();
    }

    @Test
    void sameNativeNameStillReattachesANewObjectToUnboundFramebuffer() throws TextureUnitException {
        Texture2D texture = texture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        final int oldId = texture.getImage().getId();
        renderer.setFrameBuffer(null);
        renderer.deleteImage(texture.getImage());
        driver.reuseTextureId = oldId;
        renderer.setTexture(0, texture);
        assertEquals(oldId, texture.getImage().getId());
        renderer.setFrameBuffer(fb);
        assertCurrentTexture(fb, texture);
        renderer.cleanup();
    }

    @Test
    void sameNativeNameStillReattachesToBoundFramebuffer() throws TextureUnitException {
        Texture2D texture = texture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        final int oldId = texture.getImage().getId();
        renderer.deleteImage(texture.getImage());
        driver.reuseTextureId = oldId;
        renderer.setTexture(0, texture);
        renderer.setFrameBuffer(fb);
        assertCurrentTexture(fb, texture);
        renderer.cleanup();
    }

    @Test
    void queuedDisposalIsRecoveredOnNextBind() {
        Texture2D texture = texture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        texture.getImage().dispose();
        renderer.postFrame();
        renderer.setFrameBuffer(fb);
        assertCurrentTexture(fb, texture);
        renderer.cleanup();
    }

    @Test
    void sharedImageReattachesEveryFramebufferWithDistinctWrappers() {
        Texture2D a = texture();
        Texture2D b = new Texture2D(a.getImage());
        FrameBuffer first = framebuffer(a);
        FrameBuffer second = framebuffer(b);
        renderer.setFrameBuffer(first);
        renderer.setFrameBuffer(second);
        renderer.deleteImage(a.getImage());
        renderer.setFrameBuffer(first);
        renderer.setFrameBuffer(second);
        assertCurrentTexture(first, a);
        assertCurrentTexture(second, b);
        assertEquals(1, driver.liveTextures.size());
        renderer.cleanup();
    }

    private void checkDepthRecovery(Image.Format format) {
        Texture2D depth = new Texture2D(8, 8, format);
        FrameBuffer fb = new FrameBuffer(8, 8, 1);
        fb.setDepthTarget(FrameBufferTarget.newTarget(depth));
        renderer.setFrameBuffer(fb);
        renderer.deleteImage(depth.getImage());
        renderer.setFrameBuffer(fb);
        assertTrue(depth.getImage().getId() > 0);
        assertEquals(depth.getImage().getId(), driver.attachmentAt(fb, GLFbo.GL_DEPTH_ATTACHMENT_EXT));
        if (format == Image.Format.Depth24Stencil8) {
            assertEquals(depth.getImage().getId(), driver.attachmentAt(fb, GL_STENCIL_ATTACHMENT));
        }
        renderer.cleanup();
    }

    @Test
    void depthOnlyImageIsRecreatedAndReattached() {
        checkDepthRecovery(Image.Format.Depth);
    }

    @Test
    void packedDepthStencilImageIsRecreatedAndReattached() {
        checkDepthRecovery(Image.Format.Depth24Stencil8);
    }

    @Test
    void blitSourceIsReattachedBeforeCopy() {
        Texture2D texture = texture();
        FrameBuffer source = framebuffer(texture);
        FrameBuffer destination = framebuffer(texture());
        renderer.setFrameBuffer(source);
        renderer.setFrameBuffer(destination);
        renderer.deleteImage(texture.getImage());
        renderer.copyFrameBuffer(source, destination, true, false);
        assertCurrentTexture(source, texture);
        assertEquals(destination.getId(), driver.drawFramebuffer);
        renderer.cleanup();
    }

    @Test
    void blitDestinationIsReattachedBeforeCopy() {
        Texture2D texture = texture();
        FrameBuffer source = framebuffer(texture());
        FrameBuffer destination = framebuffer(texture);
        renderer.setFrameBuffer(destination);
        renderer.setFrameBuffer(source);
        renderer.deleteImage(texture.getImage());
        renderer.copyFrameBuffer(source, destination, true, false);
        assertCurrentTexture(destination, texture);
        assertEquals(source.getId(), driver.drawFramebuffer);
        renderer.cleanup();
    }

    @Test
    void mainOverrideIsRepairedForNullBind() {
        Texture2D texture = texture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setMainFrameBufferOverride(fb);
        renderer.deleteImage(texture.getImage());
        renderer.setFrameBuffer(null);
        assertCurrentTexture(fb, texture);
        renderer.cleanup();
    }

    @Test
    void mainOverrideIsRepairedForNullBlitEndpoint() {
        Texture2D texture = texture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setMainFrameBufferOverride(fb);
        FrameBuffer source = framebuffer(texture());
        renderer.setFrameBuffer(source);
        renderer.deleteImage(texture.getImage());
        renderer.copyFrameBuffer(source, null, true, false);
        assertCurrentTexture(fb, texture);
        assertEquals(fb.getId(), driver.blitDrawFramebuffer);
        renderer.cleanup();
    }

    @Test
    void readbackRepairsDeletedAttachment() {
        Texture2D texture = texture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        renderer.deleteImage(texture.getImage());
        renderer.readFrameBuffer(fb, BufferUtils.createByteBuffer(8 * 8 * 4));
        assertCurrentTexture(fb, texture);
        renderer.cleanup();
    }

    @Test
    void failedReattachmentRemainsDirtyForRetry() {
        Texture2D texture = texture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        renderer.deleteImage(texture.getImage());
        driver.failTextureAttachmentSlot = 0;
        assertThrows(RendererException.class, () -> renderer.setFrameBuffer(fb));
        assertFalse(fb.isUpdateNeeded(), "This retry relies on renderer-owned invalidation");
        renderer.setFrameBuffer(fb);
        assertCurrentTexture(fb, texture);
        renderer.cleanup();
    }

    @Test
    void failedCompletenessCheckRemainsDirtyForRetry() {
        Texture2D texture = texture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        renderer.deleteImage(texture.getImage());
        driver.framebufferStatus = GLFbo.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT;
        assertThrows(IllegalStateException.class, () -> renderer.setFrameBuffer(fb));
        final int checks = driver.framebufferChecks;
        driver.framebufferStatus = GLFbo.GL_FRAMEBUFFER_COMPLETE_EXT;
        renderer.setFrameBuffer(fb);
        assertEquals(checks + 1, driver.framebufferChecks);
        assertCurrentTexture(fb, texture);
        renderer.cleanup();
    }

    @Test
    void repairPreservesRenderbufferStorageAndReturnsToFastPath() {
        Texture2D texture = texture();
        FrameBuffer fb = framebuffer(texture);
        fb.setDepthTarget(FrameBufferTarget.newTarget(Image.Format.Depth));
        renderer.setFrameBuffer(fb);
        final int allocations = driver.renderbufferStorageCalls.size();
        renderer.deleteImage(texture.getImage());
        renderer.setFrameBuffer(fb);
        assertCurrentTexture(fb, texture);
        final int checks = driver.framebufferChecks;
        final int attachments = driver.textureAttachmentCalls;
        renderer.setFrameBuffer(fb);
        renderer.setFrameBuffer(null);
        renderer.setFrameBuffer(fb);
        assertEquals(checks, driver.framebufferChecks);
        assertEquals(attachments, driver.textureAttachmentCalls);
        assertEquals(allocations, driver.renderbufferStorageCalls.size());
        renderer.cleanup();
    }

    @Test
    void unrelatedImageDeletionDoesNotDirtyFramebuffer() throws TextureUnitException {
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        Texture2D unrelated = texture();
        renderer.setTexture(0, unrelated);
        final int checks = driver.framebufferChecks;
        renderer.deleteImage(unrelated.getImage());
        renderer.setFrameBuffer(fb);
        assertEquals(checks, driver.framebufferChecks);
        renderer.cleanup();
    }

    @Test
    void deletedOldDepthTextureDoesNotInvalidateItsRenderbufferReplacement() {
        Texture2D depth = new Texture2D(8, 8, Image.Format.Depth24Stencil8);
        FrameBuffer fb = framebuffer(texture());
        fb.setDepthTarget(FrameBufferTarget.newTarget(depth));
        renderer.setFrameBuffer(fb);
        fb.setDepthTarget(FrameBufferTarget.newTarget(Image.Format.Depth));
        renderer.setFrameBuffer(fb);
        final int checks = driver.framebufferChecks;
        renderer.deleteImage(depth.getImage());
        renderer.setFrameBuffer(fb);
        assertEquals(checks, driver.framebufferChecks);
        assertEquals(fb.getDepthTarget().getId(), driver.attachmentAt(fb, GLFbo.GL_DEPTH_ATTACHMENT_EXT));
        assertEquals(0, driver.attachmentAt(fb, GL_STENCIL_ATTACHMENT));
        renderer.cleanup();
    }

    @Test
    void publicTextureUpdateSelectsFramebufferAndResetsViewport() {
        FrameBuffer previous = framebuffer(texture());
        renderer.setFrameBuffer(previous);
        final int previousAttachment = driver.attachment(previous, 0);
        renderer.setViewPort(2, 3, 4, 5);
        FrameBuffer next = framebuffer(texture());
        renderer.updateRenderTexture(next, next.getColorTarget(0));
        assertEquals(next.getId(), driver.drawFramebuffer);
        assertEquals(previousAttachment, driver.attachment(previous, 0));
        assertEquals(8, driver.viewportWidth);
        assertEquals(8, driver.viewportHeight);
        renderer.cleanup();
    }

    @Test
    void publicAttachmentUpdateResetsViewportEvenOnSameFramebuffer() {
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        renderer.setViewPort(2, 3, 4, 5);
        renderer.updateFrameBufferAttachment(fb, fb.getColorTarget(0));
        assertEquals(fb.getId(), driver.drawFramebuffer);
        assertEquals(8, driver.viewportWidth);
        assertEquals(8, driver.viewportHeight);
        renderer.cleanup();
    }
}
