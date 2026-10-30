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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.Caps;
import com.jme3.renderer.RendererException;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Texture2D;
import org.junit.jupiter.api.Test;

/** Compatibility of single-target updates with deferred full-FBO lifecycle work. */
public class GlRendererAttachmentUpdateStateTest extends FrameBufferTestSupport {
    @Test
    void preservesPendingReplacementsAndViewportUntilFullUpdate() {
        FrameBuffer fb = framebuffer(texture(), texture());
        renderer.setFrameBuffer(fb);
        final int oldSecond = driver.attachment(fb, 1);
        Texture2D first = texture();
        Texture2D second = texture();
        fb.replaceColorTarget(0, FrameBufferTarget.newTarget(first));
        fb.replaceColorTarget(1, FrameBufferTarget.newTarget(second));
        renderer.setViewPort(2, 3, 4, 5);
        final int checks = driver.framebufferChecks;
        final int drawChanges = driver.drawBufferChanges;
        renderer.updateRenderTexture(fb, fb.getColorTarget(0));
        assertEquals(first.getImage().getId(), driver.attachment(fb, 0));
        assertEquals(oldSecond, driver.attachment(fb, 1));
        assertEquals(-1, second.getImage().getId());
        assertEquals(4, driver.viewportWidth);
        assertEquals(5, driver.viewportHeight);
        assertEquals(checks, driver.framebufferChecks);
        assertEquals(drawChanges, driver.drawBufferChanges);
        assertTrue(fb.isUpdateNeeded());
        renderer.setFrameBuffer(fb);
        assertEquals(second.getImage().getId(), driver.attachment(fb, 1));
        assertEquals(checks + 1, driver.framebufferChecks);
        assertFalse(fb.isUpdateNeeded());
        renderer.cleanup();
    }

    @Test
    void successfulSingleUpdateDoesNotDirtySubsequentSameFramebufferBind() {
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        renderer.setViewPort(2, 3, 4, 5);
        final int checks = driver.framebufferChecks;
        renderer.updateFrameBufferAttachment(fb, fb.getColorTarget(0));
        renderer.setFrameBuffer(fb);
        assertEquals(checks, driver.framebufferChecks);
        assertEquals(4, driver.viewportWidth);
        assertEquals(5, driver.viewportHeight);
        assertFalse(fb.isUpdateNeeded());
        renderer.cleanup();
    }

    @Test
    void failedSingleUpdateKeepsNativeRecoveryPending() {
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        renderer.setViewPort(2, 3, 4, 5);
        final int checks = driver.framebufferChecks;
        driver.failTextureAttachmentSlot = 0;
        assertThrows(RendererException.class,
                () -> renderer.updateRenderTexture(fb, fb.getColorTarget(0)));
        assertEquals(4, driver.viewportWidth);
        assertEquals(5, driver.viewportHeight);
        assertFalse(fb.isUpdateNeeded());
        renderer.setFrameBuffer(fb);
        assertEquals(checks + 1, driver.framebufferChecks);
        renderer.cleanup();
    }

    @Test
    void failedDirectFullUpdateOnCleanFramebufferRetriesNormally() {
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        driver.framebufferStatus = GLFbo.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT;
        assertThrows(IllegalStateException.class, () -> renderer.updateFrameBuffer(fb));
        final int checks = driver.framebufferChecks;
        driver.framebufferStatus = GLFbo.GL_FRAMEBUFFER_COMPLETE_EXT;
        renderer.setFrameBuffer(fb);
        assertEquals(checks + 1, driver.framebufferChecks);
        renderer.cleanup();
    }

    @Test
    void singleRepairDoesNotLoseOtherDeletedImageAttachments() {
        Texture2D shared = texture();
        FrameBuffer fb = framebuffer(shared, shared);
        renderer.setFrameBuffer(fb);
        renderer.deleteImage(shared.getImage());
        renderer.updateRenderTexture(fb, fb.getColorTarget(0));
        assertEquals(shared.getImage().getId(), driver.attachment(fb, 0));
        assertEquals(0, driver.attachment(fb, 1));
        renderer.setFrameBuffer(fb);
        assertEquals(shared.getImage().getId(), driver.attachment(fb, 1));
        renderer.cleanup();
    }

    @Test
    void directUpdateDetachesRemovedStorageWithoutReallocatingSurvivors() {
        FrameBuffer fb = bufferedFramebuffer(2);
        renderer.setFrameBuffer(fb);
        final int removed = fb.getColorTarget(1).getId();
        final int allocations = driver.renderbufferStorageCalls.size();
        fb.removeColorTarget(1);
        renderer.setViewPort(2, 3, 4, 5);
        renderer.updateFrameBufferAttachment(fb, fb.getColorTarget(0));
        assertEquals(0, driver.attachment(fb, 1));
        assertFalse(driver.liveRenderbuffers.contains(removed));
        assertEquals(allocations, driver.renderbufferStorageCalls.size());
        assertEquals(4, driver.viewportWidth);
        renderer.setFrameBuffer(fb);
        renderer.cleanup();
        assertTrue(driver.liveRenderbuffers.isEmpty());
    }

    @Test
    void uninitializedFramebufferRemainsPendingAfterSingleTargetUpdate() {
        Texture2D first = texture();
        Texture2D second = texture();
        FrameBuffer fb = framebuffer(first, second);
        renderer.setViewPort(2, 3, 4, 5);
        renderer.updateFrameBufferAttachment(fb, fb.getColorTarget(0));
        assertEquals(0, driver.drawFramebuffer);
        assertEquals(first.getImage().getId(), driver.attachment(fb, 0));
        assertEquals(-1, second.getImage().getId());
        assertTrue(fb.isUpdateNeeded());
        assertEquals(0, driver.framebufferChecks);
        renderer.setFrameBuffer(fb);
        assertTrue(second.getImage().getId() > 0);
        assertEquals(second.getImage().getId(), driver.attachment(fb, 1));
        assertFalse(fb.isUpdateNeeded());
        renderer.cleanup();
    }

    @Test
    void lowLevelUpdateStillRejectsUnsupportedFramebuffersBeforeAllocation() {
        renderer.getCaps().remove(Caps.FrameBuffer);
        FrameBuffer fb = framebuffer(texture());
        assertThrows(RendererException.class,
                () -> renderer.updateRenderTexture(fb, fb.getColorTarget(0)));
        assertThrows(RendererException.class,
                () -> renderer.updateFrameBufferAttachment(fb, fb.getColorTarget(0)));
        assertEquals(-1, fb.getId());
        assertTrue(driver.liveFramebuffers.isEmpty());
    }

    @Test
    void updatingAnotherCleanFramebufferDoesNotSwallowTheNextRenderSwitch() {
        Texture2D texture = texture();
        texture.setMinFilter(com.jme3.texture.Texture.MinFilter.Trilinear);
        FrameBuffer previous = framebuffer(texture);
        final FrameBuffer other = framebuffer(texture());
        renderer.setFrameBuffer(other);
        renderer.setFrameBuffer(previous);
        renderer.setViewPort(2, 3, 4, 5);
        final int generations = driver.mipmapGenerations.size();
        renderer.updateRenderTexture(other, other.getColorTarget(0));
        assertEquals(previous.getId(), driver.drawFramebuffer);
        assertEquals(previous.getId(), driver.readFramebuffer);
        assertEquals(previous, renderer.getCurrentFrameBuffer());
        assertEquals(4, driver.viewportWidth);
        assertEquals(generations, driver.mipmapGenerations.size());
        renderer.setFrameBuffer(other);
        assertEquals(other.getId(), driver.drawFramebuffer);
        assertEquals(8, driver.viewportWidth);
        assertEquals(generations + 1, driver.mipmapGenerations.size());
        renderer.cleanup();
    }

    @Test
    void failedUpdateOfAnotherFramebufferRestoresBinding() {
        FrameBuffer previous = framebuffer(texture());
        final FrameBuffer other = framebuffer(texture());
        renderer.setFrameBuffer(previous);
        renderer.setViewPort(2, 3, 4, 5);
        driver.failTextureAttachmentSlot = 0;
        assertThrows(RendererException.class,
                () -> renderer.updateRenderTexture(other, other.getColorTarget(0)));
        assertEquals(previous.getId(), driver.drawFramebuffer);
        assertEquals(previous, renderer.getCurrentFrameBuffer());
        assertEquals(4, driver.viewportWidth);
        renderer.setFrameBuffer(other);
        assertEquals(other.getId(), driver.drawFramebuffer);
        renderer.cleanup();
    }

    @Test
    void lowLevelUpdateRestoresNonzeroPresentationFramebuffer() {
        initializeRenderer(77);
        final FrameBuffer other = framebuffer(texture());
        renderer.updateFrameBufferAttachment(other, other.getColorTarget(0));
        assertEquals(77, driver.drawFramebuffer);
        assertEquals(77, driver.readFramebuffer);
        renderer.cleanup();
    }

    @Test
    void lowLevelUpdateAfterInvalidationRestoresNativeBindingAndUnknownCache() {
        FrameBuffer previous = framebuffer(texture());
        final FrameBuffer other = framebuffer(texture());
        renderer.setFrameBuffer(previous);
        renderer.invalidateState();
        renderer.updateRenderTexture(other, other.getColorTarget(0));
        assertEquals(previous.getId(), driver.drawFramebuffer);
        assertEquals(previous.getId(), driver.readFramebuffer);
        renderer.setFrameBuffer(null);
        assertEquals(0, driver.drawFramebuffer, "The invalid cache must still force a real screen bind");
        renderer.cleanup();
    }
}
