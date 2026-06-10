/*
 * Copyright (c) 2026 jMonkeyEngine
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
package com.jme3.scene;

import com.jme3.shader.bufferobject.BufferRegion;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VertexBufferTest {

    @Test
    public void testCustomAttributeBuffer() {
        Mesh mesh = new Mesh();
        FloatBuffer data = BufferUtils.createFloatBuffer(0f, 0f, 0f);

        mesh.setBuffer("inCustomData", 3, VertexBuffer.Format.Float, data);

        VertexBuffer vb = mesh.getBuffer("inCustomData");
        assertSame(vb, mesh.getBufferList().get(0));
        assertEquals(VertexBuffer.Type.Custom, vb.getBufferType());
        assertEquals("inCustomData", vb.getAttributeName());
        assertEquals("inCustomData", vb.getShaderAttributeName());
        assertThrows(IllegalArgumentException.class, () -> mesh.getBuffer(VertexBuffer.Type.Custom));
    }

    @Test
    public void testNamedVertexBufferUsage() {
        Mesh mesh = new Mesh();

        mesh.setBuffer(VertexBuffer.Type.Position, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(0f, 0f, 0f, 1f, 0f, 0f));
        mesh.setBuffer("inWeight", 1, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(0.25f, 0.75f));

        VertexBuffer weights = mesh.getBuffer("inWeight");
        assertEquals(VertexBuffer.Type.Custom, weights.getBufferType());
        assertEquals("inWeight", weights.getShaderAttributeName());
        assertEquals(2, weights.getNumElements());

        mesh.setBuffer("inWeight", 1, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(1f, 0f));

        assertSame(weights, mesh.getBuffer("inWeight"));
        assertEquals(1f, ((FloatBuffer) weights.getData()).get(0), 0f);
        assertTrue(weights.isUpdateNeeded());

        Mesh clone = mesh.deepClone();
        VertexBuffer clonedWeights = clone.getBuffer("inWeight");
        assertEquals(VertexBuffer.Type.Custom, clonedWeights.getBufferType());
        assertEquals("inWeight", clonedWeights.getShaderAttributeName());
        assertEquals(1f, ((FloatBuffer) clonedWeights.getData()).get(0), 0f);

        mesh.clearBuffer("inWeight");
        assertNull(mesh.getBuffer("inWeight"));
    }

    @Test
    public void testCloneWithOverrideTypeClearsCustomAttributeName() {
        VertexBuffer custom = new VertexBuffer(VertexBuffer.Type.Custom);
        custom.setAttributeName("inCustomData");
        custom.setupData(VertexBuffer.Usage.Dynamic, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(0f, 0f, 0f));

        VertexBuffer position = custom.clone(VertexBuffer.Type.Position);

        assertEquals(VertexBuffer.Type.Position, position.getBufferType());
        assertNull(position.getAttributeName());
        assertEquals("inPosition", position.getShaderAttributeName());
    }

    @Test
    public void testMarkElementsDirtyUsesByteRanges() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.Position);
        vb.setupData(VertexBuffer.Usage.Dynamic, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(0f, 0f, 0f, 1f, 1f, 1f, 2f, 2f, 2f));
        vb.clearUpdateNeeded();

        vb.markElementsDirty(1, 1);

        BufferRegion region = vb.getDirtyRegions().next();
        assertTrue(vb.isUpdateNeeded());
        assertEquals(12, region.getStart());
        assertEquals(23, region.getEnd());
        assertEquals(12, region.length());
    }

    @Test
    public void testDirtyRangesAreValidatedBeforeRendererUpload() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.Position);
        vb.setupData(VertexBuffer.Usage.Dynamic, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(0f, 0f, 0f, 1f, 1f, 1f));

        assertThrows(IllegalArgumentException.class, () -> vb.markBytesDirty(2, 4));
        assertThrows(IllegalArgumentException.class, () -> vb.markBytesDirty(24, 4));
        assertThrows(IllegalArgumentException.class, () -> vb.markElementsDirty(2, 1));
    }

    @Test
    public void testTypedVertexBuffersCreateCachedByteData() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.Position);
        vb.setupData(VertexBuffer.Usage.Dynamic, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(1f, 2f, 3f));

        ByteBuffer first = vb.getByteData();
        first.position(first.limit());
        ByteBuffer second = vb.getByteData();

        assertSame(first, second);
        assertEquals(second.limit(), second.position());
        assertEquals(3 * Float.BYTES, second.limit());
        ByteBuffer read = second.duplicate();
        read.order(second.order());
        read.clear();
        assertEquals(1f, read.getFloat(), 0f);
        assertEquals(2f, read.getFloat(), 0f);
        assertEquals(3f, read.getFloat(), 0f);
    }

    @Test
    public void testTypedByteDataSupportsShortAndIntBuffers() {
        VertexBuffer shorts = new VertexBuffer(VertexBuffer.Type.BoneIndex);
        ShortBuffer shortData = BufferUtils.createShortBuffer(new short[] {1, 2, 3});
        shorts.setupData(VertexBuffer.Usage.Dynamic, 3, VertexBuffer.Format.Short, shortData);
        ByteBuffer shortBytes = shorts.getByteData();

        assertEquals(3 * Short.BYTES, shortBytes.limit());
        assertEquals((short) 1, shortBytes.getShort());
        assertEquals((short) 2, shortBytes.getShort());
        assertEquals((short) 3, shortBytes.getShort());

        VertexBuffer ints = new VertexBuffer(VertexBuffer.Type.Index);
        IntBuffer intData = BufferUtils.createIntBuffer(new int[] {4, 5, 6});
        ints.setupData(VertexBuffer.Usage.Dynamic, 3, VertexBuffer.Format.Int, intData);
        ByteBuffer intBytes = ints.getByteData();

        assertEquals(3 * Integer.BYTES, intBytes.limit());
        assertEquals(4, intBytes.getInt());
        assertEquals(5, intBytes.getInt());
        assertEquals(6, intBytes.getInt());
    }

    @Test
    public void testTypedByteDataCacheIsInvalidatedWhenDataChanges() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.Position);
        vb.setupData(VertexBuffer.Usage.Dynamic, 1, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(1f, 2f));

        ByteBuffer first = vb.getByteData();

        vb.updateData(BufferUtils.createFloatBuffer(3f, 4f));
        ByteBuffer second = vb.getByteData();

        assertTrue(first != second);
        assertEquals(3f, second.getFloat(), 0f);
        assertEquals(4f, second.getFloat(), 0f);
    }

    @Test
    public void testCopyElementsMarksOutputDirtyAndInvalidatesByteCache() {
        VertexBuffer source = new VertexBuffer(VertexBuffer.Type.Position);
        source.setupData(VertexBuffer.Usage.Dynamic, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(1f, 2f, 3f, 4f, 5f, 6f));
        VertexBuffer target = new VertexBuffer(VertexBuffer.Type.Position);
        target.setupData(VertexBuffer.Usage.Dynamic, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(0f, 0f, 0f, 0f, 0f, 0f));

        ByteBuffer cachedBytes = target.getByteData();
        target.clearUpdateNeeded();

        source.copyElements(1, target, 0, 1);

        assertTrue(target.isUpdateNeeded());
        BufferRegion region = target.getDirtyRegions().next();
        assertEquals(0, region.getStart());
        assertEquals(11, region.getEnd());
        ByteBuffer updatedBytes = target.getByteData();
        assertTrue(cachedBytes != updatedBytes);
        assertEquals(4f, updatedBytes.getFloat(0), 0f);
        assertEquals(5f, updatedBytes.getFloat(Float.BYTES), 0f);
        assertEquals(6f, updatedBytes.getFloat(2 * Float.BYTES), 0f);
    }

    @Test
    public void testSetByteDataConvertsByteDataToTypedVertexData() {
        VertexBuffer floats = new VertexBuffer(VertexBuffer.Type.Position);
        floats.setupData(VertexBuffer.Usage.Dynamic, 2, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(0f, 0f));
        ByteBuffer floatBytes = ByteBuffer.allocateDirect(2 * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        floatBytes.putFloat(1.5f);
        floatBytes.putFloat(2.5f);
        floatBytes.flip();

        floats.setByteData(floatBytes);

        assertEquals(0, floatBytes.position());
        FloatBuffer floatData = (FloatBuffer) floats.getData();
        assertEquals(2, floatData.limit());
        assertEquals(1.5f, floatData.get(0), 0f);
        assertEquals(2.5f, floatData.get(1), 0f);

        VertexBuffer shorts = new VertexBuffer(VertexBuffer.Type.BoneIndex);
        shorts.setupData(VertexBuffer.Usage.Dynamic, 2, VertexBuffer.Format.Short,
                BufferUtils.createShortBuffer(new short[] {0, 0}));
        ByteBuffer shortBytes = ByteBuffer.allocateDirect(2 * Short.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        shortBytes.putShort((short) 3);
        shortBytes.putShort((short) 4);
        shortBytes.flip();

        shorts.setByteData(shortBytes);

        ShortBuffer shortData = (ShortBuffer) shorts.getData();
        assertEquals((short) 3, shortData.get(0));
        assertEquals((short) 4, shortData.get(1));

        VertexBuffer ints = new VertexBuffer(VertexBuffer.Type.Index);
        ints.setupData(VertexBuffer.Usage.Dynamic, 2, VertexBuffer.Format.Int,
                BufferUtils.createIntBuffer(new int[] {0, 0}));
        ByteBuffer intBytes = ByteBuffer.allocateDirect(2 * Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        intBytes.putInt(5);
        intBytes.putInt(6);
        intBytes.flip();

        ints.setByteData(intBytes);

        IntBuffer intData = (IntBuffer) ints.getData();
        assertEquals(5, intData.get(0));
        assertEquals(6, intData.get(1));
    }

    @Test
    public void testSetByteDataRejectsMisalignedByteData() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.Position);
        vb.setupData(VertexBuffer.Usage.Dynamic, 1, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(0f));

        assertThrows(IllegalArgumentException.class, () -> vb.setByteData(BufferUtils.createByteBuffer(3)));
    }

    @Test
    public void testSetDataDelegatesToByteDataConversion() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.Position);
        vb.setupData(VertexBuffer.Usage.Dynamic, 1, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(0f));
        ByteBuffer bytes = ByteBuffer.allocateDirect(Float.BYTES);
        bytes.putFloat(7f);
        bytes.flip();

        vb.setData(bytes);

        assertEquals(7f, ((FloatBuffer) vb.getData()).get(0), 0f);
    }

    @Test
    public void testUninitializedVertexBufferRejectsByteDataAccess() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.Position);

        assertThrows(IllegalStateException.class, () -> vb.getByteData());
    }

    @Test
    public void testByteBackedVertexBufferSetDataPreservesLayoutMetadata() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.BoneIndex);
        vb.setupData(VertexBuffer.Usage.Dynamic, 4, VertexBuffer.Format.UnsignedByte,
                BufferUtils.createByteBuffer(new byte[] {0, 0, 0, 0}));
        vb.clearUpdateNeeded();

        ByteBuffer source = BufferUtils.createByteBuffer(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
        vb.setByteData(source);

        assertEquals(VertexBuffer.Format.UnsignedByte, vb.getFormat());
        assertEquals(4, vb.getNumComponents());
        assertEquals(2, vb.getNumElements());
        assertTrue(vb.hasDataSizeChanged());
        assertEquals(8, vb.getByteData().limit());
    }

    @Test
    public void testByteBackedVertexBufferSetDataPreservesByteOrder() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.BoneIndex);
        vb.setupData(VertexBuffer.Usage.Dynamic, 4, VertexBuffer.Format.UnsignedByte,
                BufferUtils.createByteBuffer(new byte[] {0, 0, 0, 0}));

        ByteBuffer source = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN);
        source.putInt(0x11223344);
        source.flip();

        vb.setByteData(source);

        assertEquals(ByteOrder.LITTLE_ENDIAN, vb.getByteData().order());
        assertEquals(0x11223344, vb.getByteData().getInt());
    }

    @Test
    public void testByteBackedVertexBufferCanUseByteDataPointer() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.BoneIndex);
        vb.setupData(VertexBuffer.Usage.Dynamic, 4, VertexBuffer.Format.UnsignedByte,
                BufferUtils.createByteBuffer(new byte[] {0, 0, 0, 0}));
        ByteBuffer source = BufferUtils.createByteBuffer(new byte[] {1, 2, 3, 4});

        vb.setByteDataPointer(source);

        assertSame(source, vb.getData());
        source.put(0, (byte) 9);
        assertEquals((byte) 9, vb.getByteData().get());
    }

    @Test
    public void testTypedVertexBufferRejectsByteDataPointer() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.Position);
        vb.setupData(VertexBuffer.Usage.Dynamic, 1, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(0f));

        assertThrows(UnsupportedOperationException.class,
                () -> vb.setByteDataPointer(BufferUtils.createByteBuffer(Float.BYTES)));
    }

    @Test
    public void testByteDataPointerRejectsHeapVertexBuffers() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.BoneIndex);
        vb.setupData(VertexBuffer.Usage.Dynamic, 4, VertexBuffer.Format.UnsignedByte,
                BufferUtils.createByteBuffer(new byte[] {0, 0, 0, 0}));

        assertThrows(IllegalArgumentException.class, () -> vb.setByteDataPointer(ByteBuffer.allocate(4)));
    }

    @Test
    public void testByteDataPointerAllowsReadOnlyVertexBuffers() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.BoneIndex);
        vb.setupData(VertexBuffer.Usage.Dynamic, 4, VertexBuffer.Format.UnsignedByte,
                BufferUtils.createByteBuffer(new byte[] {0, 0, 0, 0}));
        ByteBuffer source = ByteBuffer.allocateDirect(4);
        source.put(new byte[] {1, 2, 3, 4});
        source.clear();

        vb.setByteDataPointer(source.asReadOnlyBuffer());

        assertTrue(vb.getByteData().isReadOnly());
        assertEquals((byte) 1, vb.getByteData().get(0));
    }

    @Test
    public void testHalfBuffersUseTwoBytesPerComponent() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.TexCoord);
        ByteBuffer data = BufferUtils.createByteBuffer(6);
        data.putShort((short) 0);
        data.putShort((short) 0);
        data.putShort((short) 0);
        data.clear();

        vb.setupData(VertexBuffer.Usage.Dynamic, 1, VertexBuffer.Format.Half, data);
        assertTrue(vb.invariant());

        vb.clearUpdateNeeded();
        vb.setElementComponent(1, 0, (short) 0x3c00);

        assertEquals((short) 0x3c00, vb.getElementComponent(1, 0));
        BufferRegion region = vb.getDirtyRegions().next();
        assertEquals(2, region.getStart());
        assertEquals(3, region.getEnd());

        vb.compact(2);
        assertEquals(4, vb.getData().limit());
    }

    @Test
    public void testByteBackedCompactPreservesByteOrder() {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.TexCoord);
        ByteBuffer data = ByteBuffer.allocateDirect(6).order(ByteOrder.LITTLE_ENDIAN);
        data.putShort((short) 0x0102);
        data.putShort((short) 0x0304);
        data.putShort((short) 0x0506);
        data.clear();

        vb.setupData(VertexBuffer.Usage.Dynamic, 1, VertexBuffer.Format.Half, data);
        vb.compact(2);

        ByteBuffer compacted = (ByteBuffer) vb.getData();
        assertEquals(ByteOrder.LITTLE_ENDIAN, compacted.order());
        assertEquals((short) 0x0102, compacted.getShort(0));
        assertEquals((short) 0x0304, compacted.getShort(2));
    }
}
