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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.RendererException;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.NativeObjectManager;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** Tests native attachment snapshots across deletion and low-level updates. */
public class GlRendererFrameBufferSnapshotTest extends FrameBufferTestSupport {
    private static Texture2D mipmappedTexture() {
        Texture2D texture = texture();
        texture.setMinFilter(Texture.MinFilter.Trilinear);
        return texture;
    }

    @Test
    void replacementThenDirectImageDeletionSkipsOldMipmaps() {
        Texture2D old = mipmappedTexture();
        Texture2D replacement = texture();
        FrameBuffer fb = framebuffer(old);
        renderer.setFrameBuffer(fb);
        fb.replaceColorTarget(0, FrameBufferTarget.newTarget(replacement));
        renderer.deleteImage(old.getImage());
        assertDoesNotThrow(() -> renderer.setFrameBuffer(fb));
        assertEquals(replacement.getImage().getId(), driver.attachment(fb, 0));
        assertTrue(driver.mipmapGenerations.isEmpty());
        assertDoesNotThrow(() -> renderer.cleanup());
    }

    @Test
    void replacementThenQueuedImageDisposalSkipsOldMipmaps() {
        Texture2D old = mipmappedTexture();
        Texture2D replacement = texture();
        FrameBuffer fb = framebuffer(old);
        renderer.setFrameBuffer(fb);
        fb.replaceColorTarget(0, FrameBufferTarget.newTarget(replacement));
        old.getImage().dispose();
        renderer.postFrame();
        assertDoesNotThrow(() -> renderer.setFrameBuffer(fb));
        assertEquals(replacement.getImage().getId(), driver.attachment(fb, 0));
        assertTrue(driver.mipmapGenerations.isEmpty());
        renderer.cleanup();
    }

    @Test
    void deletedImageRecreatedWithSameNameCannotReviveOldSnapshot() throws Exception {
        Texture2D texture = mipmappedTexture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        final int originalId = texture.getImage().getId();
        texture.getImage().dispose();
        renderer.postFrame();
        driver.reuseTextureId = originalId;
        renderer.setTexture(0, texture);
        assertEquals(originalId, texture.getImage().getId());
        renderer.setFrameBuffer(null);
        assertTrue(driver.mipmapGenerations.isEmpty(), "The recreated image was not rendered into");
        fb.setUpdateNeeded();
        renderer.setFrameBuffer(fb);
        renderer.setFrameBuffer(null);
        assertEquals(Collections.singletonList(originalId), driver.mipmapGenerations);
        renderer.cleanup();
    }

    @Test
    void textureImageMutationUsesImageActuallyAttached() {
        Texture2D texture = mipmappedTexture();
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        final int originalId = texture.getImage().getId();
        Image replacement = texture().getImage();
        texture.setImage(replacement);
        renderer.setFrameBuffer(null);
        assertEquals(Collections.singletonList(originalId), driver.mipmapGenerations);
        assertEquals(-1, replacement.getId());
        renderer.cleanup();
    }

    @Test
    void directAttachmentUpdateRefreshesMipmapSnapshot() {
        Texture2D replacement = mipmappedTexture();
        FrameBuffer fb = framebuffer(mipmappedTexture());
        renderer.setFrameBuffer(fb);
        fb.replaceColorTarget(0, FrameBufferTarget.newTarget(replacement));
        renderer.updateFrameBufferAttachment(fb, fb.getColorTarget(0));
        assertEquals(replacement.getImage().getId(), driver.attachment(fb, 0));
        driver.mipmapGenerations.clear();
        renderer.setFrameBuffer(null);
        assertEquals(Collections.singletonList(replacement.getImage().getId()), driver.mipmapGenerations);
        renderer.cleanup();
    }

    @Test
    void directRenderTextureUpdateRefreshesMipmapSnapshot() {
        Texture2D replacement = mipmappedTexture();
        FrameBuffer fb = framebuffer(mipmappedTexture());
        renderer.setFrameBuffer(fb);
        fb.replaceColorTarget(0, FrameBufferTarget.newTarget(replacement));
        renderer.updateRenderTexture(fb, fb.getColorTarget(0));
        assertEquals(replacement.getImage().getId(), driver.attachment(fb, 0));
        driver.mipmapGenerations.clear();
        renderer.setFrameBuffer(null);
        assertEquals(Collections.singletonList(replacement.getImage().getId()), driver.mipmapGenerations);
        renderer.cleanup();
    }

    @Test
    void directAttachmentUpdateInitializesAndBindsNewFramebuffer() {
        FrameBuffer previous = framebuffer(texture());
        renderer.setFrameBuffer(previous);
        final int previousTexture = driver.attachment(previous, 0);
        Texture2D texture = mipmappedTexture();
        FrameBuffer fb = framebuffer(texture);
        renderer.updateFrameBufferAttachment(fb, fb.getColorTarget(0));
        assertTrue(fb.getId() > 0);
        assertEquals(fb.getId(), driver.drawFramebuffer);
        assertEquals(previousTexture, driver.attachment(previous, 0));
        assertEquals(texture.getImage().getId(), driver.attachment(fb, 0));
        renderer.setFrameBuffer(null);
        assertEquals(Collections.singletonList(texture.getImage().getId()), driver.mipmapGenerations);
        renderer.cleanup();
    }

    @Test
    void directTextureUpdateInitializesNewFramebuffer() {
        Texture2D texture = mipmappedTexture();
        FrameBuffer fb = framebuffer(texture);
        renderer.updateRenderTexture(fb, fb.getColorTarget(0));
        assertEquals(fb.getId(), driver.drawFramebuffer);
        assertEquals(texture.getImage().getId(), driver.attachment(fb, 0));
        renderer.setFrameBuffer(null);
        assertEquals(Collections.singletonList(texture.getImage().getId()), driver.mipmapGenerations);
        renderer.cleanup();
    }

    @Test
    void directUpdateRejectsForeignTargetBeforeTouchingNativeState() {
        FrameBuffer first = framebuffer(texture());
        FrameBuffer second = framebuffer(texture());
        renderer.setFrameBuffer(first);
        int attachment = driver.attachment(first, 0);
        assertThrows(IllegalArgumentException.class,
                () -> renderer.updateFrameBufferAttachment(first, second.getColorTarget(0)));
        assertEquals(attachment, driver.attachment(first, 0));
        assertEquals(-1, second.getId());
        renderer.cleanup();
    }

    @Test
    void renderTextureUpdateRejectsRenderbufferBeforeTouchingNativeState() {
        FrameBuffer fb = bufferedFramebuffer(1);
        assertThrows(IllegalArgumentException.class,
                () -> renderer.updateRenderTexture(fb, fb.getColorTarget(0)));
        assertEquals(-1, fb.getId());
        assertTrue(driver.liveFramebuffers.isEmpty());
    }

    @Test
    void directUpdateReconcilesRemovedSlotsToo() {
        FrameBuffer fb = framebuffer(mipmappedTexture(), mipmappedTexture());
        renderer.setFrameBuffer(fb);
        fb.removeColorTarget(1);
        renderer.updateFrameBufferAttachment(fb, fb.getColorTarget(0));
        driver.mipmapGenerations.clear();
        renderer.setFrameBuffer(null);
        assertEquals(0, driver.attachment(fb, 1));
        assertEquals(Collections.singletonList(fb.getColorTarget(0).getTexture().getImage().getId()),
                driver.mipmapGenerations);
        renderer.cleanup();
    }

    @Test
    void deletingSharedImageInvalidatesEveryFramebufferSnapshot() {
        Texture2D shared = mipmappedTexture();
        FrameBuffer first = framebuffer(shared);
        FrameBuffer second = framebuffer(shared);
        renderer.setFrameBuffer(first);
        renderer.setFrameBuffer(second);
        renderer.deleteImage(shared.getImage());
        driver.mipmapGenerations.clear();
        renderer.setFrameBuffer(null);
        renderer.setFrameBuffer(first);
        renderer.setFrameBuffer(null);
        assertTrue(driver.mipmapGenerations.isEmpty());
        renderer.cleanup();
    }

    @Test
    void failedUpdatePreservesSnapshotsForAttachmentsNotYetChanged() {
        Texture2D oldFirst = mipmappedTexture();
        Texture2D oldSecond = mipmappedTexture();
        Texture2D newFirst = mipmappedTexture();
        Texture2D newSecond = mipmappedTexture();
        FrameBuffer fb = framebuffer(oldFirst, oldSecond);
        renderer.setFrameBuffer(fb);
        fb.replaceColorTarget(0, FrameBufferTarget.newTarget(newFirst));
        fb.replaceColorTarget(1, FrameBufferTarget.newTarget(newSecond));
        driver.failTextureAttachmentSlot = 1;
        assertThrows(RendererException.class, () -> renderer.setFrameBuffer(fb));
        assertEquals(newFirst.getImage().getId(), driver.attachment(fb, 0));
        assertEquals(oldSecond.getImage().getId(), driver.attachment(fb, 1));
        driver.mipmapGenerations.clear();
        renderer.setFrameBuffer(null);
        assertEquals(Arrays.asList(newFirst.getImage().getId(), oldSecond.getImage().getId()),
                driver.mipmapGenerations);
        renderer.setFrameBuffer(fb);
        assertEquals(newSecond.getImage().getId(), driver.attachment(fb, 1));
        renderer.cleanup();
    }

    @Test
    void directImageDeletionPreservesQueuedUnsafeCpuCleanup() {
        boolean originalUnsafe = NativeObjectManager.UNSAFE;
        int[] bufferDeletes = {0};
        Image image = new Image(Image.Format.RGBA8, 8, 8, (ByteBuffer) null, ColorSpace.Linear) {
            @Override
            protected void deleteNativeBuffers() {
                bufferDeletes[0]++;
            }
        };
        try {
            NativeObjectManager.UNSAFE = true;
            renderer.setFrameBuffer(framebuffer(new Texture2D(image)));
            image.dispose();
            renderer.deleteImage(image);
            renderer.postFrame();
            renderer.cleanup();
            assertEquals(1, bufferDeletes[0]);
        } finally {
            NativeObjectManager.UNSAFE = originalUnsafe;
        }
    }
}
