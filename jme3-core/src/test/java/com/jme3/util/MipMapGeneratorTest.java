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
package com.jme3.util;

import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MipMapGeneratorTest {

    @Test
    public void testGenerateMipMapsAfterResizeToPowerOf2() {
        ByteBuffer data = BufferUtils.createByteBuffer(3 * 5 * 4);
        for (int i = 0; i < data.capacity(); i++) {
            data.put((byte) i);
        }
        data.flip();

        Image image = new Image(Image.Format.RGBA8, 3, 5, data, ColorSpace.Linear);
        Image resized = MipMapGenerator.resizeToPowerOf2(image);

        MipMapGenerator.generateMipMaps(resized, true, false);

        assertEquals(4, resized.getWidth());
        assertEquals(8, resized.getHeight());
        assertTrue(resized.hasMipmaps());
        assertNotNull(resized.getData(0));
        assertNotNull(resized.getMipMapSizes());
        assertTrue(resized.getMipMapSizes().length > 1);
    }

    @Test
    public void testGenerateMipMapsRejectsNullDataBuffer() {
        ArrayList<ByteBuffer> data = new ArrayList<>();
        data.add(null);
        Image image = new Image(Image.Format.RGBA8, 2, 2, 1, data, null, ColorSpace.Linear);

        assertThrows(IllegalArgumentException.class,
                () -> MipMapGenerator.generateMipMaps(image, true, false));
    }
}
