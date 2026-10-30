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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTextureTarget;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Valid pre-PR low-level attachment updates must not start a new render pass. */
public class GlRendererAttachmentCompatibilityTest extends FrameBufferTestSupport {
    private void update(boolean textureOnly, FrameBuffer fb, FrameBuffer.RenderBuffer target) {
        if (textureOnly) {
            renderer.updateRenderTexture(fb, target);
        } else {
            renderer.updateFrameBufferAttachment(fb, target);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesCustomViewportIncludingOffset(boolean textureOnly) {
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        renderer.setViewPort(2, 3, 4, 5);
        final int calls = driver.viewportCalls;
        update(textureOnly, fb, fb.getColorTarget(0));
        assertEquals(2, driver.viewportX);
        assertEquals(3, driver.viewportY);
        assertEquals(4, driver.viewportWidth);
        assertEquals(5, driver.viewportHeight);
        assertEquals(calls, driver.viewportCalls, "Even a temporary viewport change is unnecessary");
        renderer.cleanup();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void doesNotRegenerateManuallyRenderedMipLevelsMidPass(boolean textureOnly) {
        Texture2D texture = texture();
        texture.setMinFilter(Texture.MinFilter.Trilinear);
        FrameBuffer fb = framebuffer(texture);
        renderer.setFrameBuffer(fb);
        FrameBufferTextureTarget target = (FrameBufferTextureTarget) fb.getColorTarget(0);
        final int generations = driver.mipmapGenerations.size();
        target.level(1);
        update(textureOnly, fb, target);
        // The application renders a custom level here, then selects the next one.
        target.level(2);
        update(textureOnly, fb, target);
        assertEquals(generations, driver.mipmapGenerations.size(),
                "Selecting another attachment level must not overwrite previous levels");
        renderer.cleanup();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void doesNotChangeSrgbWritePolicy(boolean textureOnly) {
        FrameBuffer fb = framebuffer(texture());
        renderer.setFrameBuffer(fb);
        final int changes = driver.srgbChanges;
        fb.setSrgb(true);
        update(textureOnly, fb, fb.getColorTarget(0));
        assertEquals(changes, driver.srgbChanges);
        renderer.cleanup();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void doesNotResetReadDrawSelection(boolean textureOnly) {
        FrameBuffer fb = framebuffer(texture(), texture());
        renderer.setFrameBuffer(fb);
        final int drawChanges = driver.drawBufferChanges;
        final int readChanges = driver.readBufferChanges;
        update(textureOnly, fb, fb.getColorTarget(0));
        assertEquals(drawChanges, driver.drawBufferChanges);
        assertEquals(readChanges, driver.readBufferChanges);
        renderer.cleanup();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void onlyReattachesRequestedTarget(boolean textureOnly) {
        FrameBuffer fb = framebuffer(texture(), texture());
        renderer.setFrameBuffer(fb);
        final int attachments = driver.textureAttachmentCalls;
        update(textureOnly, fb, fb.getColorTarget(0));
        assertEquals(attachments + 1, driver.textureAttachmentCalls);
        renderer.cleanup();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void doesNotUploadUnrelatedDirtyImage(boolean textureOnly) {
        Texture2D unrelated = texture();
        FrameBuffer fb = framebuffer(texture(), unrelated);
        renderer.setFrameBuffer(fb);
        unrelated.getImage().setUpdateNeeded();
        update(textureOnly, fb, fb.getColorTarget(0));
        assertTrue(unrelated.getImage().isUpdateNeeded(),
                "Updating slot zero must not redefine another attachment's storage");
        renderer.cleanup();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void allowsTemporarilyIncompleteAttachmentsDuringStagedUpdate(boolean textureOnly) {
        FrameBuffer fb = framebuffer(texture(), texture());
        renderer.setFrameBuffer(fb);
        final int checks = driver.framebufferChecks;
        driver.framebufferStatus = GLFbo.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT;
        assertDoesNotThrow(() -> update(textureOnly, fb, fb.getColorTarget(0)));
        assertEquals(checks, driver.framebufferChecks,
                "An individual attachment update is not a framebuffer-completeness boundary");
        driver.framebufferStatus = GLFbo.GL_FRAMEBUFFER_COMPLETE_EXT;
        update(textureOnly, fb, fb.getColorTarget(1));
        renderer.cleanup();
    }

    @Test
    void defaultTargetCanBeSelectedBeforeAddingColorAttachments() {
        FrameBuffer fb = new FrameBuffer(8, 8, 1);
        assertDoesNotThrow(() -> fb.setTargetIndex(0));
        fb.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(texture()));
        renderer.setFrameBuffer(fb);
        assertEquals(0, fb.getTargetIndex());
        assertTrue(driver.attachment(fb, 0) > 0);
        renderer.cleanup();
    }

    @Test
    void defaultTargetCanBeSelectedOnDepthOnlyFramebuffer() {
        FrameBuffer fb = new FrameBuffer(8, 8, 1);
        fb.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.Depth));
        assertDoesNotThrow(() -> fb.setTargetIndex(0));
        renderer.setFrameBuffer(fb);
        assertEquals(java.util.Collections.singletonList(GL.GL_NONE), driver.drawBuffers.get(fb.getId()));
        renderer.cleanup();
    }
}
