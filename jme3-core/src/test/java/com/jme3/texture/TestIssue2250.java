/*
 * Copyright (c) 2024 jMonkeyEngine
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

import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.junit.Test;

/**
 * Verify that setMultiSamples(1) can be applied to any Image. This was issue
 * #2250 at GitHub.
 *
 * @author Stephen Gold
 */
public class TestIssue2250 {

    /**
     * Test setMultiSamples() on an Image with a data buffer.
     */
    @Test
    public void testIssue2250WithData() {
        int width = 8;
        int height = 8;
        int numBytes = 4 * width * height;
        ByteBuffer data = BufferUtils.createByteBuffer(numBytes);
        Image image1 = new Image(
                Image.Format.RGBA8, width, height, data, ColorSpace.Linear);

        image1.setMultiSamples(1);
    }

    /**
     * Test setMultiSamples() on an Image with mip maps.
     */
    @Test
    public void testIssue2250WithMips() {
        int width = 8;
        int height = 8;
        int depth = 1;
        int[] mipMapSizes = {256, 64, 16, 4};

        ArrayList<ByteBuffer> data = new ArrayList<>();
        Image image2 = new Image(Image.Format.RGBA8, width, height, depth, data,
                mipMapSizes, ColorSpace.Linear);

        image2.setMultiSamples(1);
    }
}
