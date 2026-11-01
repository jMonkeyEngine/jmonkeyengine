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
package com.jme3.texture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jme3.texture.FrameBuffer.FrameBufferBufferTarget;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.FrameBuffer.FrameBufferTextureTarget;
import org.junit.jupiter.api.Test;

/** Target instances are exclusive; underlying textures may be shared. */
public class FrameBufferTargetOwnershipTest {
    private static FrameBuffer framebuffer() {
        return new FrameBuffer(8, 8, 1);
    }

    private static FrameBufferTextureTarget colorTarget() {
        return FrameBufferTarget.newTarget(new Texture2D(8, 8, Image.Format.RGBA8));
    }

    @Test
    void colorRenderbufferCannotBeSharedAndFailureDoesNotChangeItsSlot() {
        FrameBuffer first = framebuffer();
        FrameBuffer second = framebuffer();
        FrameBufferBufferTarget target = FrameBufferTarget.newTarget(Image.Format.RGBA8);
        first.addColorTarget(target);
        second.addColorTarget(colorTarget());
        second.clearUpdateNeeded();
        assertThrows(IllegalArgumentException.class, () -> second.addColorTarget(target));
        assertEquals(0, target.getSlot());
        assertEquals(1, second.getNumColorTargets());
        assertSame(target, first.getColorTarget(0));
        assertFalse(second.isUpdateNeeded());
    }

    @Test
    void colorTextureTargetCannotBeShared() {
        FrameBuffer first = framebuffer();
        FrameBuffer second = framebuffer();
        FrameBufferTextureTarget target = colorTarget();
        first.addColorTarget(target);
        assertThrows(IllegalArgumentException.class, () -> second.addColorTarget(target));
        assertEquals(0, second.getNumColorTargets());
    }

    @Test
    void depthRenderbufferCannotBeShared() {
        FrameBuffer first = framebuffer();
        FrameBuffer second = framebuffer();
        FrameBufferBufferTarget target = FrameBufferTarget.newTarget(Image.Format.Depth);
        first.setDepthTarget(target);
        assertThrows(IllegalArgumentException.class, () -> second.setDepthTarget(target));
        assertNull(second.getDepthTarget());
        assertSame(target, first.getDepthTarget());
    }

    @Test
    void depthTextureTargetCannotBeShared() {
        FrameBuffer first = framebuffer();
        FrameBuffer second = framebuffer();
        FrameBufferTextureTarget target = FrameBufferTarget.newTarget(
                new Texture2D(8, 8, Image.Format.Depth));
        first.setDepthTarget(target);
        assertThrows(IllegalArgumentException.class, () -> second.setDepthTarget(target));
        assertNull(second.getDepthTarget());
    }

    @Test
    void rejectedReplacementPreservesBothFramebuffers() {
        FrameBuffer first = framebuffer();
        FrameBuffer second = framebuffer();
        FrameBufferTextureTarget shared = colorTarget();
        FrameBufferTextureTarget original = colorTarget();
        first.addColorTarget(shared);
        second.addColorTarget(colorTarget());
        second.addColorTarget(original);
        second.clearUpdateNeeded();
        assertThrows(IllegalArgumentException.class, () -> second.replaceColorTarget(1, shared));
        assertSame(original, second.getColorTarget(1));
        assertEquals(0, shared.getSlot());
        assertFalse(second.isUpdateNeeded());
    }

    @Test
    void cubemapOverloadRejectsSharingBeforeChangingFaceOrSlot() {
        FrameBuffer first = framebuffer();
        FrameBuffer second = framebuffer();
        FrameBufferTextureTarget target = FrameBufferTarget.newTarget(
                new TextureCubeMap(8, 8, Image.Format.RGBA8));
        first.addColorTarget(target, TextureCubeMap.Face.PositiveX);
        second.addColorTarget(colorTarget());
        assertThrows(IllegalArgumentException.class,
                () -> second.addColorTarget(target, TextureCubeMap.Face.NegativeX));
        assertEquals(TextureCubeMap.Face.PositiveX.ordinal(), target.getFace());
        assertEquals(0, target.getSlot());
        assertEquals(1, second.getNumColorTargets());
    }

    @Test
    void duplicateRenderbufferSlotsAreRejected() {
        FrameBuffer fb = framebuffer();
        FrameBufferBufferTarget target = FrameBufferTarget.newTarget(Image.Format.RGBA8);
        fb.addColorTarget(target);
        assertThrows(IllegalArgumentException.class, () -> fb.addColorTarget(target));
        assertEquals(1, fb.getNumColorTargets());
        assertEquals(0, target.getSlot());
    }

    @Test
    void duplicateTextureSlotsAreRejectedIncludingReplacement() {
        FrameBuffer fb = framebuffer();
        FrameBufferTextureTarget first = colorTarget();
        FrameBufferTextureTarget second = colorTarget();
        fb.addColorTarget(first);
        fb.addColorTarget(second);
        assertThrows(IllegalArgumentException.class, () -> fb.addColorTarget(first));
        assertThrows(IllegalArgumentException.class, () -> fb.replaceColorTarget(1, first));
        assertSame(second, fb.getColorTarget(1));
        assertEquals(0, first.getSlot());
    }

    @Test
    void sameTargetCannotOccupyColorAndDepthSlots() {
        FrameBuffer fb = framebuffer();
        FrameBufferBufferTarget target = FrameBufferTarget.newTarget(Image.Format.Depth);
        fb.setDepthTarget(target);
        assertThrows(IllegalArgumentException.class, () -> fb.addColorTarget(target));
        assertEquals(FrameBuffer.SLOT_DEPTH, target.getSlot());
    }

    @Test
    void settingExistingTargetAgainIsAllowed() {
        FrameBuffer fb = framebuffer();
        FrameBufferBufferTarget depth = FrameBufferTarget.newTarget(Image.Format.Depth);
        FrameBufferTextureTarget color = colorTarget();
        fb.setDepthTarget(depth);
        fb.addColorTarget(color);
        assertDoesNotThrow(() -> fb.setDepthTarget(depth));
        assertDoesNotThrow(() -> fb.replaceColorTarget(0, color));
        assertSame(depth, fb.getDepthTarget());
        assertSame(color, fb.getColorTarget(0));
    }

    @Test
    void removedTargetCanReturnToOriginalFramebufferButCannotBeTransferred() {
        FrameBuffer first = framebuffer();
        FrameBuffer second = framebuffer();
        FrameBufferBufferTarget target = FrameBufferTarget.newTarget(Image.Format.RGBA8);
        first.addColorTarget(target);
        first.removeColorTarget(0);
        assertThrows(IllegalArgumentException.class, () -> second.addColorTarget(target));
        assertDoesNotThrow(() -> first.addColorTarget(target));
        first.clearColorTargets();
        assertThrows(IllegalArgumentException.class, () -> second.addColorTarget(target));
        assertDoesNotThrow(() -> first.addColorTarget(target));
    }

    @Test
    void replacedTextureTargetKeepsOriginalOwner() {
        FrameBuffer first = framebuffer();
        FrameBuffer second = framebuffer();
        FrameBufferTextureTarget target = colorTarget();
        first.addColorTarget(target);
        first.replaceColorTarget(0, colorTarget());
        assertThrows(IllegalArgumentException.class, () -> second.addColorTarget(target));
        assertDoesNotThrow(() -> first.replaceColorTarget(0, target));
    }

    @Test
    void replacedDepthTargetKeepsOriginalOwner() {
        FrameBuffer first = framebuffer();
        FrameBuffer second = framebuffer();
        FrameBufferBufferTarget target = FrameBufferTarget.newTarget(Image.Format.Depth);
        first.setDepthTarget(target);
        first.setDepthTarget(FrameBufferTarget.newTarget(Image.Format.Depth));
        assertThrows(IllegalArgumentException.class, () -> second.setDepthTarget(target));
        assertDoesNotThrow(() -> first.setDepthTarget(target));
    }

    @Test
    void failedValidationDoesNotClaimTarget() {
        FrameBuffer first = framebuffer();
        FrameBuffer second = framebuffer();
        FrameBufferBufferTarget color = FrameBufferTarget.newTarget(Image.Format.RGBA8);
        assertThrows(IllegalArgumentException.class, () -> first.setDepthTarget(color));
        assertDoesNotThrow(() -> second.addColorTarget(color));
        FrameBufferTextureTarget texture = colorTarget();
        assertThrows(IndexOutOfBoundsException.class, () -> first.replaceColorTarget(0, texture));
        assertDoesNotThrow(() -> second.addColorTarget(texture));
    }

    @Test
    void separateTargetsCanShareColorAndDepthTextures() {
        FrameBuffer first = framebuffer();
        FrameBuffer second = framebuffer();
        Texture2D color = new Texture2D(8, 8, Image.Format.RGBA8);
        Texture2D depth = new Texture2D(8, 8, Image.Format.Depth);
        first.addColorTarget(FrameBufferTarget.newTarget(color));
        second.addColorTarget(FrameBufferTarget.newTarget(color));
        first.setDepthTarget(FrameBufferTarget.newTarget(depth));
        second.setDepthTarget(FrameBufferTarget.newTarget(depth));
        assertSame(first.getColorTarget(0).getTexture(), second.getColorTarget(0).getTexture());
        assertSame(first.getDepthTarget().getTexture(), second.getDepthTarget().getTexture());
    }
}
