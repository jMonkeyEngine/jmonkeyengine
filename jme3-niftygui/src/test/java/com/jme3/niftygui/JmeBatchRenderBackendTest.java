package com.jme3.niftygui;

import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class JmeBatchRenderBackendTest {

    @Test
    public void atlasesHaveIndependentCpuStorageAndClearIndependently() {
        ByteBuffer firstData = JmeBatchRenderBackend.createAtlasImageData(2, 2);
        ByteBuffer secondData = JmeBatchRenderBackend.createAtlasImageData(2, 2);
        assertNotSame(firstData, secondData);

        firstData.put(0, (byte) 0x42);
        Image firstAtlas = new Image(Format.RGBA8, 2, 2, firstData, ColorSpace.sRGB);
        Image secondAtlas = new Image(Format.RGBA8, 2, 2, secondData, ColorSpace.sRGB);
        firstAtlas.clearUpdateNeeded();

        JmeBatchRenderBackend.clearAtlasImageData(firstAtlas);

        assertEquals(0, firstAtlas.getData(0).get(0));
        assertEquals(0xff, firstAtlas.getData(0).get(3) & 0xff);
        assertEquals(0, secondAtlas.getData(0).get(0));
        assertEquals(0xff, secondAtlas.getData(0).get(3) & 0xff);
        assertTrue(firstAtlas.isUpdateNeeded());
    }

    @Test
    public void rgbaUpdatesCopyRowsIntoCpuAtlasWithoutChangingBufferPositions() {
        ByteBuffer atlasData = JmeBatchRenderBackend.createAtlasImageData(4, 3);
        Image atlas = new Image(Format.RGBA8, 4, 3, atlasData, ColorSpace.sRGB);
        ByteBuffer sourceData = BufferUtils.createByteBuffer(16);
        for (int i = 1; i <= 16; i++) {
            sourceData.put((byte) i);
        }
        sourceData.flip();
        sourceData.position(4);
        Image source = new Image(Format.RGBA8, 2, 2, sourceData, ColorSpace.sRGB);

        JmeBatchRenderBackend.copyImageDataToAtlas(source, atlas, 1, 1);

        assertEquals(4, sourceData.position());
        assertEquals(0, atlasData.position());
        assertEquals(1, atlasData.get(20));
        assertEquals(8, atlasData.get(27));
        assertEquals(9, atlasData.get(36));
        assertEquals(16, atlasData.get(43));
        assertEquals(0, atlasData.get(0));
        assertEquals(0xff, atlasData.get(3) & 0xff);
    }

    @Test
    public void nonRgbaUpdatesAreConvertedWithoutDirtyingTheWholeAtlas() {
        ByteBuffer atlasData = JmeBatchRenderBackend.createAtlasImageData(1, 1);
        Image atlas = new Image(Format.RGBA8, 1, 1, atlasData, ColorSpace.sRGB);
        ByteBuffer sourceData = BufferUtils.createByteBuffer(3);
        sourceData.put((byte) 0x80).put((byte) 0x40).put((byte) 0xff).flip();
        Image source = new Image(Format.RGB8, 1, 1, sourceData, ColorSpace.sRGB);
        atlas.clearUpdateNeeded();

        JmeBatchRenderBackend.copyImageDataToAtlas(source, atlas, 0, 0);

        assertEquals(0x80, atlasData.get(0) & 0xff);
        assertEquals(0x40, atlasData.get(1) & 0xff);
        assertEquals(0xff, atlasData.get(2) & 0xff);
        assertEquals(0xff, atlasData.get(3) & 0xff);
        assertFalse(atlas.isUpdateNeeded());
    }

    @Test
    public void resettingAtlasGpuObjectRetainsTheCpuRestorePayload() {
        ByteBuffer atlasData = JmeBatchRenderBackend.createAtlasImageData(2, 2);
        Image atlas = new Image(Format.RGBA8, 2, 2, atlasData, ColorSpace.sRGB);
        ByteBuffer sourceData = BufferUtils.createByteBuffer(4);
        sourceData.put(new byte[] { 11, 22, 33, 44 }).flip();
        Image source = new Image(Format.RGBA8, 1, 1, sourceData, ColorSpace.sRGB);
        JmeBatchRenderBackend.copyImageDataToAtlas(source, atlas, 1, 1);

        atlas.resetObject();

        assertEquals(11, atlas.getData(0).get(12));
        assertEquals(22, atlas.getData(0).get(13));
        assertEquals(33, atlas.getData(0).get(14));
        assertEquals(44, atlas.getData(0).get(15));
    }

    @Test
    public void rejectsAnUpdateOutsideAtlasBounds() {
        Image atlas = new Image(Format.RGBA8, 1, 1,
                JmeBatchRenderBackend.createAtlasImageData(1, 1), ColorSpace.sRGB);
        ByteBuffer sourceData = BufferUtils.createByteBuffer(4);
        sourceData.put(new byte[] { 1, 2, 3, 4 }).flip();
        Image source = new Image(Format.RGBA8, 1, 1, sourceData, ColorSpace.sRGB);

        try {
            JmeBatchRenderBackend.copyImageDataToAtlas(source, atlas, 1, 0);
            fail("Expected out-of-bounds update to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals(0, atlas.getData(0).get(0));
        }
    }

    @Test
    public void rejectsUnsupportedUncompressedFormatsWithoutChangingAtlas() {
        Image atlas = new Image(Format.RGBA8, 1, 1,
                JmeBatchRenderBackend.createAtlasImageData(1, 1), ColorSpace.sRGB);
        Image source = new Image(Format.RGB111110F, 1, 1, BufferUtils.createByteBuffer(4), ColorSpace.Linear);

        try {
            JmeBatchRenderBackend.copyImageDataToAtlas(source, atlas, 0, 0);
            fail("Expected unsupported image format to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals(UnsupportedOperationException.class, expected.getCause().getClass());
            assertEquals(0, atlas.getData(0).get(0));
        }
    }
}
