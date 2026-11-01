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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.NativeObjectManager;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Exercises mixed resource cleanup, shared images, and stale reference edge cases. */
public class GlRendererFrameBufferLifecycleEdgeTest extends FrameBufferTestSupport {
    @Test
    void distinctTextureWrappersCanShareTheSameImageAfterDisposal() {
        Image image = texture().getImage();
        Texture2D a = new Texture2D(image);
        Texture2D b = new Texture2D(image);
        a.setMinFilter(Texture.MinFilter.Trilinear);
        b.setMinFilter(Texture.MinFilter.Trilinear);
        FrameBuffer first = framebuffer(a);
        FrameBuffer second = framebuffer(b);
        renderer.setFrameBuffer(first);
        renderer.setFrameBuffer(second);
        final int imageId = image.getId();
        first.dispose();
        renderer.postFrame();
        renderer.setFrameBuffer(null);
        assertEquals(imageId, image.getId());
        assertTrue(driver.liveTextures.contains(imageId));
        assertEquals(imageId, driver.attachment(second, 0));
        renderer.cleanup();
        assertTrue(driver.liveTextures.isEmpty());
        assertTrue(driver.liveFramebuffers.isEmpty());
    }

    @Test
    void staleImagePhantomReferenceCannotDeleteReusedNativeName() throws Exception {
        Texture2D old = texture();
        renderer.setTexture(0, old);
        final int id = old.getImage().getId();
        Field managerField = GLRenderer.class.getDeclaredField("objManager");
        managerField.setAccessible(true);
        NativeObjectManager manager = (NativeObjectManager) managerField.get(renderer);
        Field mapField = NativeObjectManager.class.getDeclaredField("refMap");
        mapField.setAccessible(true);
        Reference<?> ref = (Reference<?>) ((Map<?, ?>) mapField.get(manager))
                .get(old.getImage().getUniqueId());
        assertTrue(ref.enqueue());
        renderer.deleteImage(old.getImage());
        driver.reuseTextureId = id;
        Texture2D replacement = texture();
        renderer.setTexture(0, replacement);
        renderer.postFrame();
        assertEquals(id, replacement.getImage().getId());
        assertTrue(driver.liveTextures.contains(id));
        renderer.cleanup();
    }

    @Test
    void cleanupOfManyMixedResourcesLeavesNoNativeObjects() {
        FrameBuffer[] buffers = new FrameBuffer[151];
        for (int i = 0; i < buffers.length; i++) {
            Texture2D texture = texture();
            FrameBuffer fb = framebuffer(texture);
            fb.setDepthTarget(FrameBufferTarget.newTarget(Image.Format.Depth24Stencil8));
            renderer.setFrameBuffer(fb);
            buffers[i] = fb;
            if ((i & 1) == 0) {
                fb.dispose();
                texture.getImage().dispose();
            }
        }
        renderer.cleanup();
        assertTrue(driver.liveFramebuffers.isEmpty());
        assertTrue(driver.liveRenderbuffers.isEmpty());
        assertTrue(driver.liveTextures.isEmpty());
        for (FrameBuffer fb : buffers) {
            assertEquals(-1, fb.getId());
            assertEquals(-1, fb.getDepthTarget().getId());
        }
    }

    @Test
    void contextResetReinitializesSharedImagesAndAttachmentsOnce() {
        Texture2D shared = texture();
        FrameBuffer a = framebuffer(shared);
        FrameBuffer b = framebuffer(shared);
        renderer.setFrameBuffer(a);
        renderer.setFrameBuffer(b);
        a.dispose();
        shared.getImage().dispose();
        renderer.resetGLObjects();
        driver.liveTextures.clear();
        driver.liveRenderbuffers.clear();
        driver.liveFramebuffers.clear();
        driver.attachments.clear();
        driver.boundTextures.clear();
        driver.drawFramebuffer = 0;
        driver.readFramebuffer = 0;
        renderer.setFrameBuffer(a);
        renderer.setFrameBuffer(b);
        renderer.postFrame();
        assertEquals(1, driver.liveTextures.size());
        assertEquals(2, driver.liveFramebuffers.size());
        assertEquals(shared.getImage().getId(), driver.attachment(a, 0));
        assertEquals(shared.getImage().getId(), driver.attachment(b, 0));
        renderer.cleanup();
    }

    @Test
    void textureToBufferReplacementDropsOnlyItsMipmapSnapshot() {
        Texture2D texture = texture();
        texture.setMinFilter(Texture.MinFilter.Trilinear);
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        fb.clearColorTargets();
        fb.addColorTarget(FrameBufferTarget.newTarget(Image.Format.RGBA8));
        renderer.setFrameBuffer(fb);
        driver.mipmapGenerations.clear();
        renderer.setFrameBuffer(null);
        assertTrue(driver.mipmapGenerations.isEmpty());
        assertTrue(driver.liveTextures.contains(texture.getImage().getId()));
        fb.dispose();
        renderer.postFrame();
        assertTrue(driver.liveRenderbuffers.isEmpty());
        assertTrue(driver.liveTextures.contains(texture.getImage().getId()));
        renderer.cleanup();
    }

    @Test
    void explicitUnsafeDeletionWithImageMutationDoesNotDoubleFree() {
        boolean before = NativeObjectManager.UNSAFE;
        int[] frees = {0};
        Image image = new Image(Image.Format.RGBA8, 8, 8, (java.nio.ByteBuffer) null) {
            @Override
            protected void deleteNativeBuffers() {
                frees[0]++;
            }
        };
        Texture2D texture = new Texture2D(image);
        texture.setMinFilter(Texture.MinFilter.Trilinear);
        try {
            NativeObjectManager.UNSAFE = true;
            FrameBuffer fb = framebuffer(texture);
            renderer.setFrameBuffer(fb);
            texture.setImage(texture().getImage());
            image.dispose();
            renderer.deleteImage(image);
            renderer.setFrameBuffer(null);
            renderer.postFrame();
            renderer.cleanup();
            assertEquals(1, frees[0]);
        } finally {
            NativeObjectManager.UNSAFE = before;
        }
    }
}
