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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.RendererException;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import org.junit.jupiter.api.Test;

/** Tests ownership, deletion, native-name reuse, and context reset. */
public class GlRendererFrameBufferLifecycleTest extends FrameBufferTestSupport {
    @Test
    void disposalDeletesEveryColorAndDepthRenderbuffer() {
        FrameBuffer fb = bufferedFramebuffer(2);
        renderer.setFrameBuffer(fb);
        assertEquals(3, driver.liveRenderbuffers.size());
        fb.dispose();
        renderer.postFrame();
        assertTrue(driver.liveRenderbuffers.isEmpty());
        assertTrue(driver.liveFramebuffers.isEmpty());
    }

    @Test
    void managedCleanupDeletesOwnedRenderbuffersFromDestructibleClone() {
        FrameBuffer fb = bufferedFramebuffer(1);
        renderer.setFrameBuffer(fb);
        assertEquals(2, driver.liveRenderbuffers.size());
        renderer.cleanup();
        assertTrue(driver.liveFramebuffers.isEmpty());
        assertTrue(driver.liveRenderbuffers.isEmpty());
    }

    @Test
    void disposalBeforeRebindStillDeletesRemovedBuffers() {
        FrameBuffer fb = bufferedFramebuffer(2);
        renderer.setFrameBuffer(fb);
        fb.removeColorTarget(0);
        fb.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.Depth));
        fb.dispose();
        renderer.postFrame();
        assertTrue(driver.liveRenderbuffers.isEmpty());
        assertEquals(3, driver.deletedRenderbuffers.size());
    }

    @Test
    void managedCleanupIncludesRenderbuffersAddedAfterRegistration() {
        FrameBuffer fb = bufferedFramebuffer(1);
        renderer.setFrameBuffer(fb);
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8));
        renderer.setFrameBuffer(fb);
        renderer.cleanup();
        assertTrue(driver.liveRenderbuffers.isEmpty());
        assertEquals(3, driver.deletedRenderbuffers.size());
    }

    @Test
    void removedBufferCanBeAddedAgainWithFreshNativeStorage() {
        FrameBuffer fb = bufferedFramebuffer(1);
        FrameBuffer.FrameBufferBufferTarget color
                = (FrameBuffer.FrameBufferBufferTarget) fb.getColorTarget(0);
        renderer.setFrameBuffer(fb);
        final int original = color.getId();
        fb.clearColorTargets();
        renderer.setFrameBuffer(fb);
        assertEquals(-1, color.getId());
        fb.addColorTarget(color);
        renderer.setFrameBuffer(fb);
        assertNotEquals(original, color.getId());
        assertEquals(color.getId(), driver.attachment(fb, 0));
        assertEquals(3, driver.renderbufferStorageCalls.size());
    }

    @Test
    void resetClearsRemovedWrappersAndDoesNotDeleteOldContextNames() {
        FrameBuffer fb = bufferedFramebuffer(2);
        renderer.setFrameBuffer(fb);
        FrameBuffer.RenderBuffer removed = fb.getColorTarget(0);
        fb.removeColorTarget(0);
        renderer.resetGLObjects();
        assertEquals(-1, removed.getId());
        assertEquals(-1, fb.getId());
        // The old driver context and all of its names no longer exist.
        driver.liveRenderbuffers.clear();
        driver.liveFramebuffers.clear();
        driver.attachments.clear();
        driver.boundRenderbuffer = 0;
        renderer.setFrameBuffer(fb);
        fb.dispose();
        renderer.postFrame();
        assertEquals(2, driver.deletedRenderbuffers.size());
        assertTrue(driver.liveRenderbuffers.isEmpty());
    }

    @Test
    void deletingBoundRenderbufferInvalidatesCacheBeforeNameReuse() {
        FrameBuffer first = bufferedFramebuffer(1);
        renderer.setFrameBuffer(first);
        final int reused = driver.boundRenderbuffer;
        first.dispose();
        renderer.postFrame();
        assertEquals(0, driver.boundRenderbuffer);
        FrameBuffer second = new FrameBuffer(8, 8, 1);
        second.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8));
        driver.reuseRenderbufferId = reused;
        renderer.setFrameBuffer(second);
        assertEquals(reused, driver.boundRenderbuffer);
        assertEquals(reused, second.getColorTarget(0).getId());
    }

    @Test
    void directDeletionUnregistersOldCloneBeforeFramebufferNameReuse() throws Exception {
        FrameBuffer first = bufferedFramebuffer(1);
        renderer.setFrameBuffer(first);
        final int oldName = first.getId();
        Reference<?> staleReference = registeredReference(first);
        // Queue the old reference before direct deletion to exercise the queue guard.
        assertTrue(staleReference.enqueue());
        first.dispose();
        renderer.deleteFrameBuffer(first);
        driver.reuseFramebufferId = oldName;
        FrameBuffer second = bufferedFramebuffer(2);
        renderer.setFrameBuffer(second);
        renderer.postFrame();
        assertEquals(oldName, second.getId());
        assertEquals(3, driver.liveRenderbuffers.size());
        renderer.cleanup();
        assertTrue(driver.liveRenderbuffers.isEmpty());
        assertTrue(driver.liveFramebuffers.isEmpty());
    }

    @Test
    void simulatedGcQueueDeletesAllOwnedBuffers() throws Exception {
        FrameBuffer fb = bufferedFramebuffer(2);
        renderer.setFrameBuffer(fb);
        renderer.setFrameBuffer(null);
        assertTrue(registeredReference(fb).enqueue());
        renderer.postFrame();
        assertTrue(driver.liveRenderbuffers.isEmpty());
        assertTrue(driver.liveFramebuffers.isEmpty());
    }

    @Test
    void storageAllocationFailureKeepsNameTrackedForCleanup() {
        FrameBuffer fb = bufferedFramebuffer(1);
        driver.failNextStorage = true;
        assertThrows(RendererException.class, () -> renderer.setFrameBuffer(fb));
        assertEquals(1, driver.liveRenderbuffers.size());
        renderer.cleanup();
        assertTrue(driver.liveRenderbuffers.isEmpty());
        assertTrue(driver.liveFramebuffers.isEmpty());
    }

    @Test
    void storageAllocationFailureCanRetryExistingName() {
        FrameBuffer fb = bufferedFramebuffer(1);
        driver.failNextStorage = true;
        assertThrows(RendererException.class, () -> renderer.setFrameBuffer(fb));
        renderer.setFrameBuffer(fb);
        assertEquals(2, driver.renderbufferStorageCalls.size());
        assertEquals(2, driver.liveRenderbuffers.size());
    }

    @Test
    void incompleteFramebufferFailureStillCleansEveryAllocation() {
        FrameBuffer fb = bufferedFramebuffer(2);
        driver.framebufferStatus = GLFbo.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT;
        assertThrows(IllegalStateException.class, () -> renderer.setFrameBuffer(fb));
        renderer.cleanup();
        assertTrue(driver.liveRenderbuffers.isEmpty());
        assertTrue(driver.liveFramebuffers.isEmpty());
    }

    @Test
    void textureAttachmentsAreNotDeletedAsRenderbufferNames() {
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        renderer.deleteFrameBuffer(fb);
        assertTrue(driver.deletedRenderbuffers.isEmpty());
        assertEquals(1, driver.liveTextures.size());
    }

    @Test
    void disposingOneFramebufferPreservesSharedColorAndDepthTextures() {
        Texture2D color = texture();
        Texture2D depth = new Texture2D(8, 8, Image.Format.Depth);
        FrameBuffer first = framebuffer(color);
        FrameBuffer second = framebuffer(color);
        first.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(depth));
        second.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(depth));
        renderer.setFrameBuffer(first);
        renderer.setFrameBuffer(second);
        final int colorId = color.getImage().getId();
        final int depthId = depth.getImage().getId();
        first.dispose();
        renderer.postFrame();
        assertEquals(colorId, driver.attachment(second, 0));
        assertEquals(depthId, driver.attachmentAt(second, GLFbo.GL_DEPTH_ATTACHMENT_EXT));
        assertEquals(2, driver.liveTextures.size());
        assertTrue(driver.deletedRenderbuffers.isEmpty());
        second.setUpdateNeeded();
        renderer.setFrameBuffer(second);
        second.dispose();
        renderer.postFrame();
        assertTrue(driver.liveFramebuffers.isEmpty());
        assertEquals(2, driver.liveTextures.size());
        assertEquals(colorId, color.getImage().getId());
        assertEquals(depthId, depth.getImage().getId());
    }

    @Test
    void disposalDoesNotAllowTargetTransferButOriginalFramebufferCanBeReused() {
        FrameBuffer first = bufferedFramebuffer(1);
        final FrameBuffer.FrameBufferBufferTarget target
                = (FrameBuffer.FrameBufferBufferTarget) first.getColorTarget(0);
        renderer.setFrameBuffer(first);
        first.dispose();
        renderer.postFrame();
        FrameBuffer second = framebuffer(texture());
        assertThrows(IllegalArgumentException.class, () -> second.addColorTarget(target));
        renderer.setFrameBuffer(first);
        assertEquals(target.getId(), driver.attachment(first, 0));
        renderer.cleanup();
        assertTrue(driver.liveRenderbuffers.isEmpty());
    }

    @Test
    void duplicateDisposalRequestsCannotDeleteAReinitializedFramebuffer() {
        FrameBuffer fb = bufferedFramebuffer(1);
        renderer.setFrameBuffer(fb);
        for (int i = 0; i < 150; i++) {
            fb.dispose();
        }
        renderer.postFrame();
        assertTrue(driver.liveFramebuffers.isEmpty());
        renderer.setFrameBuffer(fb);
        int newId = fb.getId();
        renderer.postFrame();
        assertEquals(newId, fb.getId());
        assertTrue(driver.liveFramebuffers.contains(newId));
        assertEquals(2, driver.liveRenderbuffers.size());
        renderer.cleanup();
    }

    @Test
    void batchDisposalStillHonorsThePerFrameLimit() {
        for (int i = 0; i < 250; i++) {
            FrameBuffer fb = bufferedFramebuffer(1);
            renderer.setFrameBuffer(fb);
            fb.dispose();
            fb.dispose();
        }
        renderer.postFrame();
        assertEquals(150, driver.liveFramebuffers.size());
        assertEquals(300, driver.liveRenderbuffers.size());
        renderer.postFrame();
        assertEquals(50, driver.liveFramebuffers.size());
        renderer.postFrame();
        assertTrue(driver.liveFramebuffers.isEmpty());
        assertTrue(driver.liveRenderbuffers.isEmpty());
    }

    @Test
    void retainedTargetDoesNotPreventOwnerGcOrNativeCleanup() throws InterruptedException {
        final FrameBuffer.FrameBufferBufferTarget target
                = FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8);
        WeakReference<FrameBuffer> owner = initializeCollectibleFramebuffer(target);
        try {
            // Keep the target strongly reachable, but not its owning framebuffer.
            for (int i = 0; i < 100 && !driver.liveFramebuffers.isEmpty(); i++) {
                System.gc();
                Thread.sleep(25);
                renderer.postFrame();
            }
            assertNull(owner.get(), "The target must not retain its owner");
            assertTrue(driver.liveFramebuffers.isEmpty(), "The phantom handle must also be collectible");
            assertTrue(driver.liveRenderbuffers.isEmpty());
            assertEquals(-1, target.getId());
        } finally {
            Reference.reachabilityFence(target);
        }
    }

    private WeakReference<FrameBuffer> initializeCollectibleFramebuffer(
            FrameBuffer.FrameBufferBufferTarget target) {
        FrameBuffer fb = new FrameBuffer(8, 8, 1);
        fb.addColorTarget(target);
        renderer.setFrameBuffer(fb);
        renderer.setFrameBuffer(null);
        return new WeakReference<>(fb);
    }

    @Test
    void contextResetDiscardsPendingDisposalOfOldNativeNames() {
        FrameBuffer fb = bufferedFramebuffer(1);
        renderer.setFrameBuffer(fb);
        fb.dispose();
        renderer.resetGLObjects();
        driver.liveRenderbuffers.clear();
        driver.liveFramebuffers.clear();
        driver.attachments.clear();
        driver.boundRenderbuffer = 0;
        renderer.setFrameBuffer(fb);
        int newId = fb.getId();
        renderer.postFrame();
        assertEquals(newId, fb.getId());
        assertTrue(driver.liveFramebuffers.contains(newId));
        assertEquals(2, driver.liveRenderbuffers.size());
        fb.dispose();
        renderer.postFrame();
        assertTrue(driver.liveRenderbuffers.isEmpty());
    }

    @Test
    void fullCleanupDiscardsRequestsBeyondThePerFrameLimit() {
        FrameBuffer[] buffers = new FrameBuffer[150];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = bufferedFramebuffer(1);
            renderer.setFrameBuffer(buffers[i]);
            buffers[i].dispose();
        }
        renderer.cleanup();
        assertTrue(driver.liveFramebuffers.isEmpty());
        assertTrue(driver.liveRenderbuffers.isEmpty());
        FrameBuffer reused = buffers[0];
        renderer.setFrameBuffer(reused);
        int newId = reused.getId();
        renderer.postFrame();
        assertEquals(newId, reused.getId());
        assertTrue(driver.liveFramebuffers.contains(newId));
        reused.dispose();
        renderer.postFrame();
        assertTrue(driver.liveRenderbuffers.isEmpty());
    }
}
