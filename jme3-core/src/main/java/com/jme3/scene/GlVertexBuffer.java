/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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

import com.jme3.export.*;
import com.jme3.math.FastMath;
import com.jme3.renderer.Renderer;
import com.jme3.util.*;
import com.jme3.util.natives.GlNative;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.frames.SingleResource;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.VertexBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.*;
import java.util.Iterator;

/**
 * A <code>VertexBuffer</code> contains a particular type of geometry
 * data used by {@link Mesh}es. Every VertexBuffer set on a <code>Mesh</code>
 * is sent as an attribute to the vertex shader to be processed.
 * <p>
 * Several terms are used throughout the javadoc for this class, explanation:
 * <ul>
 * <li>Element - A single element is the largest individual object
 * inside a VertexBuffer. E.g. if the VertexBuffer is used to store 3D position
 * data, then an element will be a single 3D vector.</li>
 * <li>Component - A component represents the parts inside an element.
 * For a 3D vector, a single component is one of the dimensions, X, Y or Z.</li>
 * </ul>
 */
public class GlVertexBuffer extends GlNative<Integer> implements VertexBuffer<NioBuffer>, Savable, Cloneable {

    @Override
    public void set(NioBuffer resource) {

    }

    @Override
    public void set(int frame, NioBuffer resource) {

    }

    @Override
    public NioBuffer get() {
        return null;
    }

    @Override
    public NioBuffer get(int frame) {
        return null;
    }

    @Override
    public int getNumResources() {
        return 0;
    }

    @Override
    public Iterator<NioBuffer> iterator() {
        return new ArrayIterator<>(data);
    }

    /**
     * Type of buffer. Specifies the actual attribute it defines.
     */
    public enum Type {
        /**
         * Position of the vertex (3 floats)
         */
        Position,
        /**
         * The size of the point when using point buffers (float).
         */
        Size,
        /**
         * Normal vector, normalized (3 floats).
         */
        Normal,
        /**
         * Texture coordinate (2 float)
         */
        TexCoord,
        /**
         * Color and Alpha (4 floats)
         */
        Color,
        /**
         * Tangent vector, normalized (4 floats) (x,y,z,w). The w component is
         * called the binormal parity, is not normalized, and is either 1f or
         * -1f. It's used to compute the direction on the binormal vector on the
         * GPU at render time.
         */
        Tangent,
        /**
         * Binormal vector, normalized (3 floats, optional)
         */
        Binormal,
        /**
         * Specifies the source data for various vertex buffers
         * when interleaving is used. By default, the format is
         * byte.
         */
        InterleavedData,
        /**
         * Do not use.
         */
        @Deprecated
        Reserved0,
        /**
         * Specifies the index buffer, must contain integer data
         * (ubyte, ushort, or uint).
         */
        Index,
        /**
         * Initial vertex position, used with animation.
         * Should have the same format and size as {@link Type#Position}.
         * If used with software skinning, the usage should be
         * {@link Usage#CpuOnly}, and the buffer should be allocated
         * on the heap.
         */
        BindPosePosition,
        /**
         * Initial vertex normals, used with animation.
         * Should have the same format and size as {@link Type#Normal}.
         * If used with software skinning, the usage should be
         * {@link Usage#CpuOnly}, and the buffer should be allocated
         * on the heap.
         */
        BindPoseNormal,
        /**
         * Bone weights, used with animation (4 floats).
         * Only used for software skinning, the usage should be
         * {@link Usage#CpuOnly}, and the buffer should be allocated
         * on the heap.
         */
        BoneWeight,
        /**
         * Bone indices, used with animation (4 ubytes).
         * Only used for software skinning, the usage should be
         * {@link Usage#CpuOnly}, and the buffer should be allocated
         * on the heap as a ubytes buffer.
         */
        BoneIndex,
        /**
         * Texture coordinate #2
         */
        TexCoord2,
        /**
         * Texture coordinate #3
         */
        TexCoord3,
        /**
         * Texture coordinate #4
         */
        TexCoord4,
        /**
         * Texture coordinate #5
         */
        TexCoord5,
        /**
         * Texture coordinate #6
         */
        TexCoord6,
        /**
         * Texture coordinate #7
         */
        TexCoord7,
        /**
         * Texture coordinate #8
         */
        TexCoord8,
        /**
         * Initial vertex tangents, used with animation.
         * Should have the same format and size as {@link Type#Tangent}.
         * If used with software skinning, the usage should be
         * {@link Usage#CpuOnly}, and the buffer should be allocated
         * on the heap.
         */
        BindPoseTangent,
        /**
         * Bone weights, used with animation (4 floats).
         * for Hardware Skinning only
         */
        HWBoneWeight,
        /**
         * Bone indices, used with animation (4 ubytes).
         * for Hardware Skinning only
         * either an int or float buffer due to shader attribute types restrictions.
         */
        HWBoneIndex,
        /**
         * Information about this instance.
         *
         * Format should be {@link Format#Float} and number of components
         * should be 16.
         */
        InstanceData,
        /**
         * Morph animations targets.
         * Supports up tp 14 morph target buffers at the same time
         * Limited due to the limited number of attributes you can bind to a vertex shader usually 16
         * <p>
         * MorphTarget buffers are either POSITION, NORMAL or TANGENT buffers.
         * So we can support up to
         * 14 simultaneous POSITION targets
         * 7 simultaneous POSITION and NORMAL targets
         * 4 simultaneous POSITION, NORMAL and TANGENT targets.
         * <p>
         * Note that the MorphControl will find how many buffers
         * can be supported for each mesh/material combination.
         * Note that all buffers have 3 components (Vector3f)
         * even the Tangent buffer that
         * does not contain the w (handedness) component
         * that will not be interpolated for morph animation.
         * <p>
         * Note that those buffers contain the difference between
         * the base buffer (POSITION, NORMAL or TANGENT) and the target value
         * So that you can interpolate with a MADD operation in the vertex shader
         * position = weight * diffPosition + basePosition;
         */
        MorphTarget0,
        MorphTarget1,
        MorphTarget2,
        MorphTarget3,
        MorphTarget4,
        MorphTarget5,
        MorphTarget6,
        MorphTarget7,
        MorphTarget8,
        MorphTarget9,
        MorphTarget10,
        MorphTarget11,
        MorphTarget12,
        MorphTarget13;

        private final String name;

        Type() {
            this.name = name();
        }

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    /**
     * The usage of the VertexBuffer, specifies how often the buffer
     * is used. This can determine if a vertex buffer is placed in VRAM
     * or held in video memory, but no guarantees are made- it's only a hint.
     */
    public enum Usage {
        /**
         * Mesh data is sent once and very rarely updated.
         */
        Static,
        /**
         * Mesh data is updated occasionally (once per frame or less).
         */
        Dynamic,
        /**
         * Mesh data is updated every frame.
         */
        Stream,
        /**
         * Mesh data is <em>not</em> sent to GPU at all. It is only
         * used by the CPU.
         */
        CpuOnly
    }

    /**
     * Specifies format of the data stored in the buffer.
     * This should directly correspond to the buffer's class, for example,
     * an {@link Format#UnsignedShort} formatted buffer should use the
     * class {@link ShortBuffer} (e.g. the closest resembling type).
     * For the {@link Format#Half} type, {@link ByteBuffer}s should
     * be used.
     */
    public enum Format {
        /**
         * Half precision floating point. 2 bytes, signed.
         */
        Half(2),
        /**
         * Single precision floating point. 4 bytes, signed
         */
        Float(4),
        /**
         * Double precision floating point. 8 bytes, signed. May not be
         * supported by all GPUs.
         */
        Double(8),
        /**
         * 1 byte integer, signed.
         */
        Byte(1),
        /**
         * 1 byte integer, unsigned.
         */
        UnsignedByte(1),
        /**
         * 2 byte integer, signed.
         */
        Short(2),
        /**
         * 2 byte integer, unsigned.
         */
        UnsignedShort(2),
        /**
         * 4 byte integer, signed.
         */
        Int(4),
        /**
         * 4 byte integer, unsigned.
         */
        UnsignedInt(4);

        private int componentSize = 0;

        Format(int componentSize) {
            this.componentSize = componentSize;
        }

        /**
         * Returns the size in bytes of this data type.
         *
         * @return Size in bytes of this data type.
         */
        public int getComponentSize() {
            return componentSize;
        }
    }

    protected int offset = 0;
    protected int lastLimit = 0;
    protected int stride = 0;
    protected int components = 0;

    /**
     * derived from components * format.getComponentSize()
     */
    protected transient int componentsLength = 0;
    protected final NioBuffer data = new NioBuffer();
    protected Usage usage;
    protected Type bufType;
    protected Format format;
    protected boolean normalized = false;
    protected int instanceSpan = 0;
    protected transient boolean dataSizeChanged = false;
    protected String name;

    /**
     * Creates an empty, uninitialized buffer.
     * Must call setupData() to initialize.
     * 
     * @param type the type of VertexBuffer, such as Position or Binormal
     */
    public GlVertexBuffer(Type type) {
        super();
        this.bufType = type;
    }

    /**
     * Serialization only. Do not use.
     */
    protected GlVertexBuffer() {
        super();
    }

    protected GlVertexBuffer(int id) {
        super(id);
    }

    @Override
    public PointerBuffer map() {
        return data.map();
    }

    @Override
    public void unmap() {
        data.unmap();
    }

    @Override
    public MemorySize size() {
        return data.size();
    }

    @Override
    public ByteBuffer mapBytes() {
        return data.mapBytes();
    }

    @Override
    public ShortBuffer mapShorts() {
        return data.mapShorts();
    }

    @Override
    public IntBuffer mapInts() {
        return data.mapInts();
    }

    @Override
    public FloatBuffer mapFloats() {
        return data.mapFloats();
    }

    @Override
    public DoubleBuffer mapDoubles() {
        return data.mapDoubles();
    }

    @Override
    public LongBuffer mapLongs() {
        return data.mapLongs();
    }

    public boolean invariant() {
        // Does the VB hold any data?
        if (!data.hasBuffer()) {
            throw new AssertionError();
        }
        Buffer buf = data.getBuffer();
        // Position must be 0.
        if (buf.position() != 0) {
            throw new AssertionError();
        }
        // Is the size of the VB == 0?
        if (buf.limit() == 0) {
            throw new AssertionError();
        }
        // Does offset exceed buffer limit or negative?
        if (offset > buf.limit() || offset < 0) {
            throw new AssertionError();
        }
        // Are components between 1 and 4?

        // Are components between 1 and 4 and not InstanceData?
        if (bufType != Type.InstanceData) {
            if (components < 1 || components > 4) {
                throw new AssertionError();
            }
        }

        // Does usage comply with buffer directness?
        //if (usage == Usage.CpuOnly && data.isDirect()) {
        //    throw new AssertionError();
        /*} else*/ if (usage != Usage.CpuOnly && !buf.isDirect()) {
            throw new AssertionError();
        }

        // Double/Char/Long buffers are not supported for VertexBuffers.
        // For the rest, ensure they comply with the "Format" value.
        if (buf instanceof DoubleBuffer) {
            throw new AssertionError();
        } else if (buf instanceof CharBuffer) {
            throw new AssertionError();
        } else if (buf instanceof LongBuffer) {
            throw new AssertionError();
        } else if (buf instanceof FloatBuffer && format != Format.Float) {
            throw new AssertionError();
        } else if (buf instanceof IntBuffer && format != Format.Int && format != Format.UnsignedInt) {
            throw new AssertionError();
        } else if (buf instanceof ShortBuffer && format != Format.Short && format != Format.UnsignedShort) {
            throw new AssertionError();
        } else if (buf instanceof ByteBuffer && format != Format.Byte && format != Format.UnsignedByte) {
            throw new AssertionError();
        }
        return true;
    }

    /**
     * @return The offset after which the data is sent to the GPU.
     *
     * @see #setOffset(int)
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @param offset Specify the offset (in bytes) from the start of the buffer
     * after which the data is sent to the GPU.
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * @return The stride (in bytes) for the data.
     *
     * @see #setStride(int)
     */
    public int getStride() {
        return stride;
    }

    /**
     * Set the stride (in bytes) for the data.
     * <p>
     * If the data is packed in the buffer, then stride is 0, if there's other
     * data that is between the current component and the next component in the
     * buffer, then this specifies the size in bytes of that additional data.
     *
     * @param stride the stride (in bytes) for the data
     */
    public void setStride(int stride) {
        this.stride = stride;
    }

    /**
     * Returns the raw internal data buffer used by this VertexBuffer.
     * This buffer is not safe to call from multiple threads since buffers
     * have their own internal position state that cannot be shared.
     * Call getData().duplicate(), getData().asReadOnlyBuffer(), or
     * the more convenient getDataReadOnly() if the buffer may be accessed
     * from multiple threads.
     *
     * @return A native buffer, in the specified {@link Format format}.
     */
    public Buffer getData() {
        return data.getBuffer();
    }

    /**
     * Returns a safe read-only version of this VertexBuffer's data.  The
     * contents of the buffer will reflect whatever changes are made on
     * other threads (eventually) but these should not be used in that way.
     * This method provides a read-only buffer that is safe to _read_ from
     * a separate thread since it has its own book-keeping state (position, limit, etc.)
     *
     * @return A rewound native buffer in the specified {@link Format format}
     *         that is safe to read from a separate thread from other readers.
     */
    public Buffer getDataReadOnly() {
        if (!data.hasBuffer()) {
            return null;
        }

        // Create a read-only duplicate().  Note: this does not copy
        // the underlying memory, it just creates a new read-only wrapper
        // with its own buffer position state.
        // Unfortunately, this is not 100% straight forward since Buffer
        // does not have an asReadOnlyBuffer() method.
        Buffer result;
        Buffer buf = data.getBuffer();
        if (buf instanceof ByteBuffer) {
            result = ((ByteBuffer) buf).asReadOnlyBuffer();
        } else if (buf instanceof FloatBuffer) {
            result = ((FloatBuffer) buf).asReadOnlyBuffer();
        } else if (buf instanceof ShortBuffer) {
            result = ((ShortBuffer) buf).asReadOnlyBuffer();
        } else if (buf instanceof IntBuffer) {
            result = ((IntBuffer) buf).asReadOnlyBuffer();
        } else {
            throw new UnsupportedOperationException("Cannot get read-only view of buffer type:" + data);
        }

        // Make sure the caller gets a consistent view since we may
        // have grabbed this buffer while another thread was reading
        // the raw data.
        result.rewind();

        return result;
    }

    /**
     * @return The usage of this buffer. See {@link Usage} for more
     * information.
     */
    public Usage getUsage() {
        return usage;
    }

    /**
     * @param usage The usage of this buffer. See {@link Usage} for more
     * information.
     */
    public void setUsage(Usage usage) {
//        if (id != -1)
//            throw new UnsupportedOperationException("Data has already been sent. Cannot set usage.");

        this.usage = usage;
        this.setUpdateNeeded();
    }

    /**
     * @param normalized Set to true if integer components should be converted
     * from their maximal range into the range 0.0 - 1.0 when converted to
     * a floating-point value for the shader.
     * E.g. if the {@link Format} is {@link Format#UnsignedInt}, then
     * the components will be converted to the range 0.0 - 1.0 by dividing
     * every integer by 2^32.
     */
    public void setNormalized(boolean normalized) {
        this.normalized = normalized;
    }

    /**
     * @return True if integer components should be converted to the range 0-1.
     * @see GlVertexBuffer#setNormalized(boolean)
     */
    public boolean isNormalized() {
        return normalized;
    }

    /**
     * Sets the instanceSpan to 1 or 0 depending on
     * the value of instanced and the existing value of
     * instanceSpan.
     * 
     * @param instanced true for instanced, false for not instanced
     */
    public void setInstanced(boolean instanced) {
        if (instanced && instanceSpan == 0) {
            instanceSpan = 1;
        } else if (!instanced) {
            instanceSpan = 0;
        }
    }

    /**
     * @return true if buffer contains per-instance data, otherwise false
     */
    public boolean isInstanced() {
        return instanceSpan > 0;
    }

    /**
     * Sets how this vertex buffer matches with rendered instances
     * where 0 means no instancing at all, ie: all elements are
     * per vertex.  If set to 1 then each element goes with one
     * instance.  If set to 2 then each element goes with two
     * instances and so on.
     * 
     * @param i the desired number of instances per element
     */
    public void setInstanceSpan(int i) {
        this.instanceSpan = i;
    }

    public int getInstanceSpan() {
        return instanceSpan;
    }

    /**
     * @return The type of information that this buffer has.
     */
    public Type getBufferType() {
        return bufType;
    }

    /**
     * @return The {@link Format format}, or data type of the data.
     */
    public Format getFormat() {
        return format;
    }

    /**
     * @return The number of components of the given {@link Format format} per
     * element.
     */
    public int getNumComponents() {
        return components;
    }

    /**
     * @return The total number of data elements in the data buffer.
     */
    public int getNumElements() {
        if (!data.hasBuffer()) {
            return 0;
        }
        int elements = data.getBuffer().limit() / components;
        if (format == Format.Half) {
            elements /= 2;
        }
        return elements;
    }

    /**
     *  Returns the number of 'instances' in this VertexBuffer.  This
     *  is dependent on the current instanceSpan.  When instanceSpan
     *  is 0 then 'instances' is 1.  Otherwise, instances is elements *
     *  instanceSpan.  It is possible to render a mesh with more instances
     *  but the instance data begins to repeat.
     * 
     * @return the number of instances
     */
    public int getBaseInstanceCount() {
        if (instanceSpan == 0) {
            return 1;
        }
        return getNumElements() * instanceSpan;
    }

    /**
     * Called to initialize the data in the <code>VertexBuffer</code>. Must only
     * be called once.
     *
     * @param usage The usage for the data, or how often will the data
     * be updated per frame. See the {@link Usage} enum.
     * @param components The number of components per element.
     * @param format The {@link Format format}, or data-type of a single
     * component.
     * @param data A native buffer, the format of which matches the {@link Format}
     * argument.
     */
    public void setupData(Usage usage, int components, Format format, Buffer data) {
        if (object != -1) {
            throw new UnsupportedOperationException("Data has already been sent. Cannot setupData again.");
        }

        if (usage == null || format == null || data == null) {
            throw new IllegalArgumentException("None of the arguments can be null");
        }

        if (data.isReadOnly()) {
            throw new IllegalArgumentException("VertexBuffer data cannot be read-only.");
        }

        if (bufType != Type.InstanceData) {
            if (components < 1 || components > 4) {
                throw new IllegalArgumentException("components must be between 1 and 4");
            }
        }

        this.data.setBuffer(data);
        this.components = components;
        this.usage = usage;
        this.format = format;
        this.componentsLength = components * format.getComponentSize();
        this.lastLimit = data.limit();
        setUpdateNeeded();
    }

    /**
     * Called to update the data in the buffer with new data. Can only
     * be called after {@link GlVertexBuffer#setupData(
     * GlVertexBuffer.Usage, int, GlVertexBuffer.Format, java.nio.Buffer) }
     * has been called. Note that it is fine to call this method on the
     * data already set, e.g. vb.updateData(vb.getData()), this will just
     * set the proper update flag indicating the data should be sent to the GPU
     * again.
     * <p>
     * It is allowed to specify a buffer with different capacity than the
     * originally set buffer, HOWEVER, if you do so, you must
     * call Mesh.updateCounts() otherwise bizarre errors can occur.
     *
     * @param data The data buffer to set
     */
    public void updateData(Buffer data) {

        // Check if the data buffer is read-only which is a sign
        // of a bug on the part of the caller
        if (data != null && data.isReadOnly()) {
            throw new IllegalArgumentException("VertexBuffer data cannot be read-only.");
        }

        // will force renderer to call glBufferData again
        if (data != null && (this.data.getBuffer().getClass() != data.getClass() || data.limit() != lastLimit)) {
            dataSizeChanged = true;
            lastLimit = data.limit();
        }

        this.data.setBuffer(data);
        setUpdateNeeded();
    }

    /**
     * Returns true if the data size of the VertexBuffer has changed.
     * Internal use only.
     *
     * @return true if the data size has changed
     */
    public boolean hasDataSizeChanged() {
        return dataSizeChanged;
    }

    @Override
    public void clearUpdateNeeded() {
        super.clearUpdateNeeded();
        dataSizeChanged = false;
    }

    /**
     * Converts single floating-point data to {@link Format#Half half} floating-point data.
     */
    public void convertToHalf() {
        if (object != -1) {
            throw new UnsupportedOperationException("Data has already been sent.");
        }

        if (format != Format.Float) {
            throw new IllegalStateException("Format must be float!");
        }

        int numElements = data.getBuffer().limit() / components;
        format = Format.Half;
        this.componentsLength = components * format.getComponentSize();

        ByteBuffer halfData = BufferUtils.createByteBuffer(componentsLength * numElements);
        halfData.rewind();

        FloatBuffer floatData = (FloatBuffer) data.getBuffer();
        floatData.rewind();

        for (int i = 0; i < floatData.limit(); i++) {
            float f = floatData.get(i);
            short half = FastMath.convertFloatToHalf(f);
            halfData.putShort(half);
        }
        this.data.setBuffer(halfData);
        setUpdateNeeded();
        dataSizeChanged = true;
    }

    /**
     * Reduces the capacity of the buffer to the given amount
     * of elements, any elements at the end of the buffer are truncated
     * as necessary.
     *
     * @param numElements The number of elements to reduce to.
     */
    public void compact(int numElements) {
        int total = components * numElements;
        data.getBuffer().clear();
        switch (format) {
            case Byte:
            case UnsignedByte:
            case Half: {
                ByteBuffer buf = data.asBytes();
                buf.limit(total);
                ByteBuffer newBuf = BufferUtils.createByteBuffer(total);
                newBuf.put(buf);
                data.setBuffer(newBuf, Byte.BYTES);
            } break;
            case Short:
            case UnsignedShort: {
                ShortBuffer buf = data.asShorts();
                buf.limit(total);
                ShortBuffer newBuf = BufferUtils.createShortBuffer(total);
                newBuf.put(buf);
                data.setBuffer(newBuf, Short.BYTES);
            } break;
            case Int:
            case UnsignedInt: {
                IntBuffer buf = data.asInts();
                buf.limit(total);
                IntBuffer newBuf = BufferUtils.createIntBuffer(total);
                newBuf.put(buf);
                data.setBuffer(newBuf, Integer.BYTES);
            } break;
            case Float: {
                FloatBuffer buf = data.asFloats();
                buf.limit(total);
                FloatBuffer newBuf = BufferUtils.createFloatBuffer(total);
                newBuf.put(buf);
                data.setBuffer(newBuf, Float.BYTES);
            } break;
            default: throw new UnsupportedOperationException("Unrecognized buffer format: " + format);
        }
        data.getBuffer().clear();
        setUpdateNeeded();
        dataSizeChanged = true;
    }

    /**
     * Modify a component inside an element.
     * The <code>val</code> parameter must be in the buffer's format:
     * {@link Format}.
     *
     * @param elementIndex The element index to modify
     * @param componentIndex The component index to modify
     * @param val The value to set, either byte, short, int or float depending
     * on the {@link Format}.
     */
    public void setElementComponent(int elementIndex, int componentIndex, Object val) {
        int inPos = elementIndex * components;
        int elementPos = componentIndex;

        if (format == Format.Half) {
            inPos *= 2;
            elementPos *= 2;
        }

        data.getBuffer().clear();

        switch (format) {
            case Byte:
            case UnsignedByte:
            case Half: {
                ByteBuffer bin = data.asBytes();
                bin.put(inPos + elementPos, (Byte) val);
            } break;
            case Short:
            case UnsignedShort: {
                ShortBuffer sin = data.asShorts();
                sin.put(inPos + elementPos, (Short) val);
            } break;
            case Int:
            case UnsignedInt: {
                IntBuffer iin = data.asInts();
                iin.put(inPos + elementPos, (Integer) val);
            } break;
            case Float: {
                FloatBuffer fin = data.asFloats();
                fin.put(inPos + elementPos, (Float) val);
            } break;
            default:
                throw new UnsupportedOperationException("Unrecognized buffer format: " + format);
        }
    }

    /**
     * Get the component inside an element.
     *
     * @param elementIndex The element index
     * @param componentIndex The component index
     * @return The component, as one of the primitive types, byte, short,
     * int or float.
     */
    public Object getElementComponent(int elementIndex, int componentIndex) {
        int inPos = elementIndex * components;
        int elementPos = componentIndex;

        if (format == Format.Half) {
            inPos *= 2;
            elementPos *= 2;
        }

        Buffer srcData = getDataReadOnly();

        switch (format) {
            case Byte:
            case UnsignedByte:
            case Half:
                ByteBuffer bin = (ByteBuffer) srcData;
                return bin.get(inPos + elementPos);
            case Short:
            case UnsignedShort:
                ShortBuffer sin = (ShortBuffer) srcData;
                return sin.get(inPos + elementPos);
            case Int:
            case UnsignedInt:
                IntBuffer iin = (IntBuffer) srcData;
                return iin.get(inPos + elementPos);
            case Float:
                FloatBuffer fin = (FloatBuffer) srcData;
                return fin.get(inPos + elementPos);
            default:
                throw new UnsupportedOperationException("Unrecognized buffer format: " + format);
        }
    }

    /**
     * Copies a single element of data from this <code>VertexBuffer</code>
     * to the given output VertexBuffer.
     *
     * @param inIndex The input element index
     * @param outVb The buffer to copy to
     * @param outIndex The output element index
     *
     * @throws IllegalArgumentException If the formats of the buffers do not
     * match.
     */
    public void copyElement(int inIndex, GlVertexBuffer outVb, int outIndex) {
        copyElements(inIndex, outVb, outIndex, 1);
    }

    /**
     * Copies a sequence of elements of data from this <code>VertexBuffer</code>
     * to the given output VertexBuffer.
     *
     * @param inIndex The input element index
     * @param outVb The buffer to copy to
     * @param outIndex The output element index
     * @param len The number of elements to copy
     *
     * @throws IllegalArgumentException If the formats of the buffers do not
     * match.
     */
    public void copyElements(int inIndex, GlVertexBuffer outVb, int outIndex, int len) {
        if (outVb.format != format || outVb.components != components) {
            throw new IllegalArgumentException("Buffer format mismatch. Cannot copy");
        }

        int inPos = inIndex * components;
        int outPos = outIndex * components;
        int elementSz = components;
        if (format == Format.Half) {
            // because half is stored as ByteBuffer, but it's 2 bytes long
            inPos *= 2;
            outPos *= 2;
            elementSz *= 2;
        }

        // Make sure to grab a read-only copy in case some other
        // thread is also accessing the buffer and messing with its
        // position()
        Buffer srcData = getDataReadOnly();
        outVb.data.getBuffer().clear();

        switch (format) {
            case Byte:
            case UnsignedByte:
            case Half: {
                ByteBuffer bin = (ByteBuffer) srcData;
                ByteBuffer bout = outVb.data.asBytes();
                bin.position(inPos).limit(inPos + elementSz * len);
                bout.position(outPos).limit(outPos + elementSz * len);
                bout.put(bin);
            } break;
            case Short:
            case UnsignedShort: {
                ShortBuffer sin = (ShortBuffer) srcData;
                ShortBuffer sout = outVb.data.asShorts();
                sin.position(inPos).limit(inPos + elementSz * len);
                sout.position(outPos).limit(outPos + elementSz * len);
                sout.put(sin);
            } break;
            case Int:
            case UnsignedInt: {
                IntBuffer iin = (IntBuffer) srcData;
                IntBuffer iout = outVb.data.asInts();
                iin.position(inPos).limit(inPos + elementSz * len);
                iout.position(outPos).limit(outPos + elementSz * len);
                iout.put(iin);
            } break;
            case Float: {
                FloatBuffer fin = (FloatBuffer) srcData;
                FloatBuffer fout = outVb.data.asFloats();
                fin.position(inPos).limit(inPos + elementSz * len);
                fout.position(outPos).limit(outPos + elementSz * len);
                fout.put(fin);
            } break;
            default: throw new UnsupportedOperationException("Unrecognized buffer format: " + format);
        }

        // Clear the output buffer to rewind it and reset its
        // limit from where we shortened it above.
        outVb.data.getBuffer().clear();
    }

    /**
     * Creates a {@link Buffer} that satisfies the given type and size requirements
     * of the parameters. The buffer will be of the type specified by
     * {@link Format format} and would be able to contain the given number
     * of elements with the given number of components in each element.
     * 
     * @param format the desired format of components, such as Float or Half
     * @param components the number of components per element (&ge;1, &le;4)
     * @param numElements the desired capacity (number of elements)
     * @return a new Buffer
     */
    public static Buffer createBuffer(Format format, int components, int numElements) {
        if (components < 1 || components > 4) {
            throw new IllegalArgumentException("Num components must be between 1 and 4");
        }

        int total = numElements * components;

        switch (format) {
            case Byte:
            case UnsignedByte:
                return BufferUtils.createByteBuffer(total);
            case Half:
                return BufferUtils.createByteBuffer(total * 2);
            case Short:
            case UnsignedShort:
                return BufferUtils.createShortBuffer(total);
            case Int:
            case UnsignedInt:
                return BufferUtils.createIntBuffer(total);
            case Float:
                return BufferUtils.createFloatBuffer(total);
            case Double:
                return BufferUtils.createDoubleBuffer(total);
            default:
                throw new UnsupportedOperationException("Unrecognized buffer format: " + format);
        }
    }

    /**
     * Creates a deep clone of the {@link GlVertexBuffer}.
     *
     * @return Deep clone of this buffer
     */
    @Override
    public GlVertexBuffer clone() {
        // NOTE: Superclass GLObject automatically creates shallow clone
        // e.g. re-use ID.
        GlVertexBuffer vb = (GlVertexBuffer) super.clone();
        vb.object = -1;
        if (data.hasBuffer()) {
            // Make sure to pass a read-only buffer to clone so that
            // the position information doesn't get clobbered by another
            // reading thread during cloning (and vice versa) since this is
            // a purely read-only operation.
            vb.updateData(BufferUtils.clone(getDataReadOnly()));
        }

        return vb;
    }

    /**
     * Creates a deep clone of this VertexBuffer but overrides the
     * {@link Type}.
     *
     * @param overrideType The type of the cloned VertexBuffer
     * @return A deep clone of the buffer
     */
    public GlVertexBuffer clone(Type overrideType) {
        GlVertexBuffer vb = new GlVertexBuffer(overrideType);
        vb.components = components;
        vb.componentsLength = componentsLength;

        // Make sure to pass a read-only buffer to clone so that
        // the position information doesn't get clobbered by another
        // reading thread during cloning (and vice versa) since this is
        // a purely read-only operation.
        vb.data.setBuffer(BufferUtils.clone(getDataReadOnly()));
        vb.format = format;
        vb.object = -1;
        vb.normalized = normalized;
        vb.instanceSpan = instanceSpan;
        vb.offset = offset;
        vb.stride = stride;
        vb.updateNeeded = true;
        vb.usage = usage;
        return vb;
    }

    @Override
    public String toString() {
        String dataTxt = null;
        if (data.hasBuffer()) {
            dataTxt = ", elements=" + data.getBuffer().limit();
        }
        return getClass().getSimpleName() + "[fmt=" + format.name()
                + ", type=" + bufType.name()
                + ", usage=" + usage.name()
                + dataTxt + "]";
    }

    @Override
    public void resetObject() {
//        assert this.id != -1;
        this.object = -1;
        setUpdateNeeded();
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            renderer.deleteBuffer(new GlVertexBuffer(object));

        };
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(components, "components", 0);
        oc.write(usage, "usage", Usage.Dynamic);
        oc.write(bufType, "buffer_type", null);
        oc.write(format, "format", Format.Float);
        oc.write(normalized, "normalized", false);
        oc.write(offset, "offset", 0);
        oc.write(stride, "stride", 0);
        oc.write(instanceSpan, "instanceSpan", 0);

        String dataName = "data" + format.name();
        Buffer roData = getDataReadOnly();
        switch (format) {
            case Float:
                oc.write((FloatBuffer) roData, dataName, null);
                break;
            case Short:
            case UnsignedShort:
                oc.write((ShortBuffer) roData, dataName, null);
                break;
            case UnsignedByte:
            case Byte:
            case Half:
                oc.write((ByteBuffer) roData, dataName, null);
                break;
            case Int:
            case UnsignedInt:
                oc.write((IntBuffer) roData, dataName, null);
                break;
            default:
                throw new IOException("Unsupported export buffer format: " + format);
        }
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        components = ic.readInt("components", 0);
        usage = ic.readEnum("usage", Usage.class, Usage.Dynamic);
        bufType = ic.readEnum("buffer_type", Type.class, null);
        format = ic.readEnum("format", Format.class, Format.Float);
        normalized = ic.readBoolean("normalized", false);
        offset = ic.readInt("offset", 0);
        stride = ic.readInt("stride", 0);
        instanceSpan = ic.readInt("instanceSpan", 0);
        componentsLength = components * format.getComponentSize();

        String dataName = "data" + format.name();
        switch (format) {
            case Float: {
                data.setBuffer(ic.readFloatBuffer(dataName, null), Float.BYTES);
            } break;
            case Short:
            case UnsignedShort: {
                data.setBuffer(ic.readShortBuffer(dataName, null), Short.BYTES);
            } break;
            case UnsignedByte:
            case Byte:
            case Half: {
                data.setBuffer(ic.readByteBuffer(dataName, null), Byte.BYTES);
            } break;
            case Int:
            case UnsignedInt: {
                data.setBuffer(ic.readIntBuffer(dataName, null), Integer.BYTES);
            } break;
            default: throw new IOException("Unsupported import buffer format: " + format);
        }
    }

    public String getName() {
        if (name == null) {
            name = getClass().getSimpleName() + "(" + getBufferType().name() + ")";
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
