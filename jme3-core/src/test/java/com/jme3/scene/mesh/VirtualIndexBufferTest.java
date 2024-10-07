package com.jme3.scene.mesh;

import com.jme3.scene.Mesh.Mode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.IntBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

public class VirtualIndexBufferTest {

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testHybrid() {
        thrown.expect(UnsupportedOperationException.class);
        new VirtualIndexBuffer(8, Mode.Hybrid);
    }

    @Test
    public void testRemaining() {
        final VirtualIndexBuffer bufferPoints = new VirtualIndexBuffer(8, Mode.Points);
        assertEquals(8, bufferPoints.remaining());
        bufferPoints.get();
        assertEquals(7, bufferPoints.remaining());

        final VirtualIndexBuffer bufferLineLoop = new VirtualIndexBuffer(8, Mode.LineLoop);
        assertEquals(16, bufferLineLoop.remaining()); // see JME issue #1603

        final VirtualIndexBuffer bufferLineStrip = new VirtualIndexBuffer(8, Mode.LineStrip);
        assertEquals(14, bufferLineStrip.remaining());

        final VirtualIndexBuffer bufferLines = new VirtualIndexBuffer(8, Mode.Lines);
        assertEquals(8, bufferLines.remaining());

        final VirtualIndexBuffer bufferTriangleFan = new VirtualIndexBuffer(8, Mode.TriangleFan);
        assertEquals(18, bufferTriangleFan.remaining());

        final VirtualIndexBuffer bufferTriangleStrip = new VirtualIndexBuffer(8, Mode.TriangleStrip);
        assertEquals(18, bufferTriangleStrip.remaining());

        final VirtualIndexBuffer bufferTriangles = new VirtualIndexBuffer(8, Mode.Triangles);
        assertEquals(8, bufferTriangles.remaining());

        final VirtualIndexBuffer bufferPatch = new VirtualIndexBuffer(8, Mode.Patch);
        assertEquals(0, bufferPatch.remaining());
    }

    @Test
    public void testRewind() {
        final VirtualIndexBuffer buffer = new VirtualIndexBuffer(5, Mode.Points);

        assertEquals(5, buffer.remaining());
        buffer.get();
        assertEquals(4, buffer.remaining());
        buffer.rewind();
        assertEquals(5, buffer.remaining());
    }

    @Test
    public void testGet() {
        IntBuffer ib = IntBuffer.allocate(27);

        final VirtualIndexBuffer bufferPoints = new VirtualIndexBuffer(27, Mode.Points);
        for (int i=0; i<27; i++) {ib.put(bufferPoints.get());}
        assertArrayEquals(new int[]{
                0, 1, 2,
                3, 4, 5,
                6, 7, 8,
                9, 10, 11,
                12, 13, 14,
                15, 16, 17,
                18, 19, 20,
                21, 22, 23,
                24, 25, 26}, ib.array());
        ib.clear();
        assertEquals(0, bufferPoints.remaining());

        /*
         * Test with Mode.Lines and no leftover vertices.
         */
        IntBuffer ib8 = IntBuffer.allocate(8);
        final VirtualIndexBuffer vibLinesEven
                = new VirtualIndexBuffer(8, Mode.Lines);
        for (int i = 0; i < 8; i++) {
            ib8.put(vibLinesEven.get());
        }
        assertArrayEquals(new int[]{
            0, 1,
            2, 3,
            4, 5,
            6, 7}, ib8.array());
        assertEquals(0, vibLinesEven.remaining());
        ib8.clear();

        final VirtualIndexBuffer bufferLines = new VirtualIndexBuffer(27, Mode.Lines);
        for (int i=0; i<27; i++) {ib.put(bufferLines.get());}
        assertArrayEquals(new int[]{
                0, 1, 2,
                3, 4, 5,
                6, 7, 8,
                9, 10, 11,
                12, 13, 14,
                15, 16, 17,
                18, 19, 20,
                21, 22, 23,
                24, 25, 26}, ib.array());
        assertEquals(0, bufferLines.remaining());
        ib.clear();

        final VirtualIndexBuffer bufferTriangles = new VirtualIndexBuffer(27, Mode.Triangles);
        for (int i=0; i<27; i++) {ib.put(bufferTriangles.get());}
        assertArrayEquals(new int[]{
                0, 1, 2,
                3, 4, 5,
                6, 7, 8,
                9, 10, 11,
                12, 13, 14,
                15, 16, 17,
                18, 19, 20,
                21, 22, 23,
                24, 25, 26}, ib.array());
        assertEquals(0, bufferTriangles.remaining());
        ib.clear();

        /*
         * Test with Mode.LineStrip and no leftover vertices.
         */
        final VirtualIndexBuffer vibLineStripEven
                = new VirtualIndexBuffer(5, Mode.LineStrip);
        for (int i = 0; i < 8; i++) {
            ib8.put(vibLineStripEven.get());
        }
        assertArrayEquals(new int[]{
            0, 1,
            1, 2,
            2, 3,
            3, 4}, ib8.array());
        assertEquals(0, vibLineStripEven.remaining());
        ib8.clear();

        final VirtualIndexBuffer bufferLineStrip = new VirtualIndexBuffer(27, Mode.LineStrip);
        for (int i=0; i<27; i++) {ib.put(bufferLineStrip.get());}
        assertArrayEquals(new int[]{
                0, 1, 1,
                2, 2, 3,
                3, 4, 4,
                5, 5, 6,
                6, 7, 7,
                8, 8, 9,
                9, 10, 10,
                11, 11, 12,
                12, 13, 13}, ib.array());
        assertEquals(25, bufferLineStrip.remaining());
        ib.clear();

        /*
         * Test with Mode.LineLoop and no leftover vertices,
         * to ensure that the loop wraps around to 0 properly.
         * See JME issue #1603.
         */
        final VirtualIndexBuffer vibLineLoopEven
                = new VirtualIndexBuffer(4, Mode.LineLoop);
        for (int i = 0; i < 8; i++) {
            ib8.put(vibLineLoopEven.get());
        }
        assertArrayEquals(new int[]{
            0, 1,
            1, 2,
            2, 3,
            3, 0}, ib8.array());
        assertEquals(0, vibLineLoopEven.remaining());
        ib8.clear();

        final VirtualIndexBuffer bufferLineLoop = new VirtualIndexBuffer(27, Mode.LineLoop);
        for (int i=0; i<27; i++) {ib.put(bufferLineLoop.get());}
        assertArrayEquals(new int[]{
                0, 1, 1,
                2, 2, 3,
                3, 4, 4,
                5, 5, 6,
                6, 7, 7,
                8, 8, 9,
                9, 10, 10,
                11, 11, 12,
                12, 13, 13}, ib.array());
        assertEquals(27, bufferLineLoop.remaining()); // see JME issue #1603
        ib.clear();

        final VirtualIndexBuffer bufferTriangleStrip = new VirtualIndexBuffer(27, Mode.TriangleStrip);
        for (int i=0; i<27; i++) {ib.put(bufferTriangleStrip.get());}
        assertArrayEquals(new int[]{
                0, 1, 2,
                2, 1, 3,
                2, 3, 4,
                4, 3, 5,
                4, 5, 6,
                6, 5, 7,
                6, 7, 8,
                8, 7, 9,
                8, 9, 10}, ib.array());
        assertEquals(48, bufferTriangleStrip.remaining());
        ib.clear();

        final VirtualIndexBuffer bufferTriangleFan = new VirtualIndexBuffer(27, Mode.TriangleFan);
        for (int i=0; i<27; i++) {ib.put(bufferTriangleFan.get());}
        assertArrayEquals(new int[]{
                0, 1, 2,
                0, 2, 3,
                0, 3, 4,
                0, 4, 5,
                0, 5, 6,
                0, 6, 7,
                0, 7, 8,
                0, 8, 9,
                0, 9, 10}, ib.array());
        assertEquals(48, bufferTriangleFan.remaining());
        ib.clear();
    }

    @Test
    public void testGet_Patch() {
        final VirtualIndexBuffer bufferPatch = new VirtualIndexBuffer(27, Mode.Patch);
        thrown.expect(UnsupportedOperationException.class);
        bufferPatch.get();
    }

    @Test
    public void testSize() {
        final VirtualIndexBuffer bufferTriangleFan = new VirtualIndexBuffer(5, Mode.TriangleFan);
        assertEquals(9, bufferTriangleFan.size());

        final VirtualIndexBuffer bufferLineLoop = new VirtualIndexBuffer(8, Mode.LineLoop);
        assertEquals(16, bufferLineLoop.size()); // see JME issue #1603

        final VirtualIndexBuffer bufferPoints = new VirtualIndexBuffer(8, Mode.Points);
        assertEquals(8, bufferPoints.size());

        final VirtualIndexBuffer bufferLines = new VirtualIndexBuffer(8, Mode.Lines);
        assertEquals(8, bufferLines.size());

        final VirtualIndexBuffer bufferLineStrip = new VirtualIndexBuffer(8, Mode.LineStrip);
        assertEquals(14, bufferLineStrip.size());

        final VirtualIndexBuffer bufferTriangles = new VirtualIndexBuffer(8, Mode.Triangles);
        assertEquals(8, bufferTriangles.size());

        final VirtualIndexBuffer bufferTriangleStrip = new VirtualIndexBuffer(8, Mode.TriangleStrip);
        assertEquals(18, bufferTriangleStrip.size());
    }
}
