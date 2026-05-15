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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GLRendererMipmapPolicyTest {

    @Test
    public void testDoesNotClampWhenHardwareMipmapGenerationIsPending() {
        int maxLevel = GLRenderer.textureMaxLevelForUpload(
                true,
                true,
                true,
                false,
                null,
                7);

        assertEquals(7, maxLevel);
    }

    @Test
    public void testClampsToBaseLevelWhenNoMipmapsAreUploaded() {
        int maxLevel = GLRenderer.textureMaxLevelForUpload(
                true,
                false,
                false,
                false,
                null,
                4);

        assertEquals(0, maxLevel);
    }

    @Test
    public void testUsesUploadedMipCountForExistingOrCpuMipmaps() {
        int maxLevel = GLRenderer.textureMaxLevelForUpload(
                true,
                true,
                false,
                true,
                new int[] {64, 16, 4, 1},
                7);

        assertEquals(3, maxLevel);
    }

    @Test
    public void testSkipsMaxLevelWhenCapabilityIsUnavailable() {
        int maxLevel = GLRenderer.textureMaxLevelForUpload(
                false,
                false,
                false,
                true,
                new int[] {64, 16},
                7);

        assertEquals(-1, maxLevel);
    }

    @Test
    public void testClampsToBaseLevelWhenRequestedMipmapsCannotBeGeneratedOrUploaded() {
        int maxLevel = GLRenderer.textureMaxLevelForUpload(
                true,
                true,
                false,
                false,
                null,
                7);

        assertEquals(0, maxLevel);
    }

    @Test
    public void testGeneratedMipMaxLevelUsesLargestUploadDimension() {
        assertEquals(0, GLRenderer.generatedMipMaxLevel(1, 1, 1));
        assertEquals(2, GLRenderer.generatedMipMaxLevel(3, 5, 1));
        assertEquals(3, GLRenderer.generatedMipMaxLevel(4, 8, 1));
        assertEquals(4, GLRenderer.generatedMipMaxLevel(4, 8, 16));
    }
}
