/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObject;
import java.io.IOException;
import java.nio.*;

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
public class VertexBuffer extends NativeObject implements Savable, Cloneable {
  
    /**
     * Type of buffer. Specifies the actual attribute it defines.
     */
    public static enum Type {
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
         * Tangent vector, normalized (4 floats) (x,y,z,w)
         * the w component is called the binormal parity, is not normalized and is either 1f or -1f
         * It's used to compuste the direction on the binormal verctor on the GPU at render time.
         */
        Tangent,

        /**
         * Binormal vector, normalized (3 floats, optional)
         */
        Binormal,

        /**
         * Specifies the source data for various vertex buffers
         * when interleaving is used. By default the format is
         * byte.
         */
        InterleavedData,

        /**
         * Do not use.
         */
        @Deprecated
        MiscAttrib,

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
         * If used with software skinning, the usage should be 
         * {@link Usage#CpuOnly}, and the buffer should be allocated
         * on the heap.
         */
        BoneWeight,

        /** 
         * Bone indices, used with animation (4 ubytes).
         * If used with software skinning, the usage should be 
         * {@link Usage#CpuOnly}, and the buffer should be allocated
         * on the heap.
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
    }

    /**
     * The usage of the VertexBuffer, specifies how often the buffer
     * is used. This can determine if a vertex buffer is placed in VRAM
     * or held in video memory, but no guarantees are made- it's only a hint.
     */
    public static enum Usage {
        
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
        CpuOnly;
    }

    /**
     * Specifies format of the data stored in the buffer.
     * This should directly correspond to the buffer's class, for example,
     * an {@link Format#UnsignedShort} formatted buffer should use the
     * class {@link ShortBuffer} (e.g. the closest resembling type).
     * For the {@link Format#Half} type, {@link ByteBuffer}s should
     * be used.
     */
    public static enum Format {
        /**
         * Half precision floating point.
         * 2 bytes, signed.
         */
        Half(2),
        
        /**
         * Single precision floating point.
         * 4 bytes, signed
         */
        Float(4),
        
        /**
         * Double precision floating point.
         * 8 bytes, signed. May not
         * be supported by all GPUs.
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

        Format(int componentSize){
            this.componentSize = componentSize;
        }

        /**
         * Returns the size in bytes of this data type.
         * 
         * @return Size in bytes of this data type.
         */
        public int getComponentSize(){
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
    protected Buffer data = null;
    protected Usage usage;
    protected Type bufType;
    protected Format format;
    protected boolean normalized = false;
    protected transient boolean dataSizeChanged = false;

    /**
     * Creates an empty, uninitialized buffer.
     * Must call setupData() to initialize.
     */
    public VertexBuffer(Type type){
        super(VertexBuffer.class);
        this.bufType = type;
    }

    /**
     * Serialization only. Do not use.
     */
    public VertexBuffer(){
        super(VertexBuffer.class);
    }

    protected VertexBuffer(int id){
        super(VertexBuffer.class, id);
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
    public Buffer getData(){
        return data;
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
    
        if (data == null) {
            return null;
        }
        
        // Create a read-only duplicate().  Note: this does not copy
        // the underlying memory, it just creates a new read-only wrapper
        // with its own buffer position state.
        
        // Unfortunately, this is not 100% straight forward since Buffer
        // does not have an asReadOnlyBuffer() method.
        Buffer result;
        if( data instanceof ByteBuffer ) {
            result = ((ByteBuffer)data).asReadOnlyBuffer();
        } else if( data instanceof FloatBuffer ) {
            result = ((FloatBuffer)data).asReadOnlyBuffer();
        } else if( data instanceof ShortBuffer ) {
            result = ((ShortBuffer)data).asReadOnlyBuffer();
        } else if( data instanceof IntBuffer ) {
            result = ((IntBuffer)data).asReadOnlyBuffer();
        } else {
            throw new UnsupportedOperationException( "Cannot get read-only view of buffer type:" + data );
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
    public Usage getUsage(){
        return usage;
    }

    /**
     * @param usage The usage of this buffer. See {@link Usage} for more
     * information.
     */
    public void setUsage(Usage usage){
//        if (id != -1)
//            throw new UnsupportedOperationException("Data has already been sent. Cannot set usage.");

        this.usage = usage;
    }

    /**
     * @param normalized Set to true if integer components should be converted
     * from their maximal range into the range 0.0 - 1.0 when converted to
     * a floating-point value for the shader.
     * E.g. if the {@link Format} is {@link Format#UnsignedInt}, then
     * the components will be converted to the range 0.0 - 1.0 by dividing
     * every integer by 2^32.
     */
    public void setNormalized(boolean normalized){
        this.normalized = normalized;
    }

    /**
     * @return True if integer components should be converted to the range 0-1.
     * @see VertexBuffer#setNormalized(boolean) 
     */
    public boolean isNormalized(){
        return normalized;
    }

    /**
     * @return The type of information that this buffer has.
     */
    public Type getBufferType(){
        return bufType;
    }

    /**
     * @return The {@link Format format}, or data type of the data.
     */
    public Format getFormat(){
        return format;
    }

    /**
     * @return The number of components of the given {@link Format format} per
     * element.
     */
    public int getNumComponents(){
        return components;
    }

    /**
     * @return The total number of data elements in the data buffer.
     */
    public int getNumElements(){
        int elements = data.capacity() / components;
        if (format == Format.Half)
            elements /= 2;
        return elements;
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
    public void setupData(Usage usage, int components, Format format, Buffer data){
        if (id != -1)
            throw new UnsupportedOperationException("Data has already been sent. Cannot setupData again.");

        if (usage == null || format == null || data == null)
            throw new IllegalArgumentException("None of the arguments can be null");
            
        if (data.isReadOnly()) 
            throw new IllegalArgumentException( "VertexBuffer data cannot be read-only." );

        if (components < 1 || components > 4)
            throw new IllegalArgumentException("components must be between 1 and 4");

        this.data = data;
        this.components = components;
        this.usage = usage;
        this.format = format;
        this.componentsLength = components * format.getComponentSize();
        this.lastLimit = data.limit();
        setUpdateNeeded();
    }

    /**
     * Called to update the data in the buffer with new data. Can only
     * be called after {@link VertexBuffer#setupData(com.jme3.scene.VertexBuffer.Usage, int, com.jme3.scene.VertexBuffer.Format, java.nio.Buffer) }
     * has been called. Note that it is fine to call this method on the
     * data already set, e.g. vb.updateData(vb.getData()), this will just
     * set the proper update flag indicating the data should be sent to the GPU
     * again.
     * It is allowed to specify a buffer with different capacity than the
     * originally set buffer.
     *
     * @param data The data buffer to set
     */
    public void updateData(Buffer data){
        if (id != -1){
            // request to update data is okay
        }

        // Check if the data buffer is read-only which is a sign
        // of a bug on the part of the caller
        if (data != null && data.isReadOnly()) {
            throw new IllegalArgumentException( "VertexBuffer data cannot be read-only." );
        }

        // will force renderer to call glBufferData again
        if (data != null && (this.data.getClass() != data.getClass() || data.limit() != lastLimit)){
            dataSizeChanged = true;
            lastLimit = data.limit();
        }
        
        this.data = data;
        setUpdateNeeded();
    }

    /**
     * Returns true if the data size of the VertexBuffer has changed.
     * Internal use only.
     * @return true if the data size has changed
     */
    public boolean hasDataSizeChanged() {
        return dataSizeChanged;
    }

    @Override
    public void clearUpdateNeeded(){
        super.clearUpdateNeeded();
        dataSizeChanged = false;
    }

    /**
     * Converts single floating-point data to {@link Format#Half half} floating-point data.
     */
    public void convertToHalf(){
        if (id != -1)
            throw new UnsupportedOperationException("Data has already been sent.");

        if (format != Format.Float)
            throw new IllegalStateException("Format must be float!");

        int numElements = data.capacity() / components;
        format = Format.Half;
        this.componentsLength = components * format.getComponentSize();
        
        ByteBuffer halfData = BufferUtils.createByteBuffer(componentsLength * numElements);
        halfData.rewind();

        FloatBuffer floatData = (FloatBuffer) data;
        floatData.rewind();

        for (int i = 0; i < floatData.capacity(); i++){
            float f = floatData.get(i);
            short half = FastMath.convertFloatToHalf(f);
            halfData.putShort(half);
        }
        this.data = halfData;
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
    public void compact(int numElements){
        int total = components * numElements;
        data.clear();
        switch (format){
            case Byte:
            case UnsignedByte:
            case Half:
                ByteBuffer bbuf = (ByteBuffer) data;
                bbuf.limit(total);
                ByteBuffer bnewBuf = BufferUtils.createByteBuffer(total);
                bnewBuf.put(bbuf);
                data = bnewBuf;
                break;
            case Short:
            case UnsignedShort:
                ShortBuffer sbuf = (ShortBuffer) data;
                sbuf.limit(total);
                ShortBuffer snewBuf = BufferUtils.createShortBuffer(total);
                snewBuf.put(sbuf);
                data = snewBuf;
                break;
            case Int:
            case UnsignedInt:
                IntBuffer ibuf = (IntBuffer) data;
                ibuf.limit(total);
                IntBuffer inewBuf = BufferUtils.createIntBuffer(total);
                inewBuf.put(ibuf);
                data = inewBuf;
                break;
            case Float:
                FloatBuffer fbuf = (FloatBuffer) data;
                fbuf.limit(total);
                FloatBuffer fnewBuf = BufferUtils.createFloatBuffer(total);
                fnewBuf.put(fbuf);
                data = fnewBuf;
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized buffer format: "+format);
        }
        data.clear();
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
    public void setElementComponent(int elementIndex, int componentIndex, Object val){
        int inPos = elementIndex * components;
        int elementPos = componentIndex;

        if (format == Format.Half){
            inPos *= 2;
            elementPos *= 2;
        }

        data.clear();

        switch (format){
            case Byte:
            case UnsignedByte:
            case Half:
                ByteBuffer bin = (ByteBuffer) data;
                bin.put(inPos + elementPos, (Byte)val);
                break;
            case Short:
            case UnsignedShort:
                ShortBuffer sin = (ShortBuffer) data;
                sin.put(inPos + elementPos, (Short)val);
                break;
            case Int:
            case UnsignedInt:
                IntBuffer iin = (IntBuffer) data;
                iin.put(inPos + elementPos, (Integer)val);
                break;
            case Float:
                FloatBuffer fin = (FloatBuffer) data;
                fin.put(inPos + elementPos, (Float)val);
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized buffer format: "+format);
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
    public Object getElementComponent(int elementIndex, int componentIndex){
        int inPos = elementIndex * components;
        int elementPos = componentIndex;

        if (format == Format.Half){
            inPos *= 2;
            elementPos *= 2;
        }

        Buffer srcData = getDataReadOnly();

        switch (format){
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
                throw new UnsupportedOperationException("Unrecognized buffer format: "+format);
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
    public void copyElement(int inIndex, VertexBuffer outVb, int outIndex){
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
    public void copyElements(int inIndex, VertexBuffer outVb, int outIndex, int len){
        if (outVb.format != format || outVb.components != components)
            throw new IllegalArgumentException("Buffer format mismatch. Cannot copy");

        int inPos  = inIndex  * components;
        int outPos = outIndex * components;
        int elementSz = components;
        if (format == Format.Half){
            // because half is stored as bytebuf but its 2 bytes long
            inPos *= 2;
            outPos *= 2;
            elementSz *= 2;
        }

        // Make sure to grab a read-only copy in case some other
        // thread is also accessing the buffer and messing with its
        // position()
        Buffer srcData = getDataReadOnly();
        outVb.data.clear();

        switch (format){
            case Byte:
            case UnsignedByte:
            case Half:
                ByteBuffer bin = (ByteBuffer) srcData;
                ByteBuffer bout = (ByteBuffer) outVb.data;
                bin.position(inPos).limit(inPos + elementSz * len);
                bout.position(outPos).limit(outPos + elementSz * len);
                bout.put(bin);                        
                break;
            case Short:
            case UnsignedShort:
                ShortBuffer sin = (ShortBuffer) srcData;
                ShortBuffer sout = (ShortBuffer) outVb.data;
                sin.position(inPos).limit(inPos + elementSz * len);
                sout.position(outPos).limit(outPos + elementSz * len);
                sout.put(sin);
                break;
            case Int:
            case UnsignedInt:
                IntBuffer iin = (IntBuffer) srcData;
                IntBuffer iout = (IntBuffer) outVb.data;
                iin.position(inPos).limit(inPos + elementSz * len);
                iout.position(outPos).limit(outPos + elementSz * len);
                iout.put(iin);
                break;
            case Float:
                FloatBuffer fin = (FloatBuffer) srcData;
                FloatBuffer fout = (FloatBuffer) outVb.data;
                fin.position(inPos).limit(inPos + elementSz * len);
                fout.position(outPos).limit(outPos + elementSz * len);
                fout.put(fin);
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized buffer format: "+format);
        }

        // Clear the output buffer to rewind it and reset its
        // limit from where we shortened it above.
        outVb.data.clear();
    }

    /**
     * Creates a {@link Buffer} that satisfies the given type and size requirements
     * of the parameters. The buffer will be of the type specified by
     * {@link Format format} and would be able to contain the given number
     * of elements with the given number of components in each element.
     */
    public static Buffer createBuffer(Format format, int components, int numElements){
        if (components < 1 || components > 4)
            throw new IllegalArgumentException("Num components must be between 1 and 4");

        int total = numElements * components;

        switch (format){
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
                throw new UnsupportedOperationException("Unrecoginized buffer format: "+format);
        }
    }

    /**
     * Creates a deep clone of the {@link VertexBuffer}.
     * 
     * @return Deep clone of this buffer
     */
    @Override
    public VertexBuffer clone(){
        // NOTE: Superclass GLObject automatically creates shallow clone
        // e.g re-use ID.
        VertexBuffer vb = (VertexBuffer) super.clone();
        vb.handleRef = new Object();
        vb.id = -1;
        if (data != null) {
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
    public VertexBuffer clone(Type overrideType){
        VertexBuffer vb = new VertexBuffer(overrideType);
        vb.components = components;
        vb.componentsLength = componentsLength;
        
        // Make sure to pass a read-only buffer to clone so that
        // the position information doesn't get clobbered by another
        // reading thread during cloning (and vice versa) since this is
        // a purely read-only operation.
        vb.data = BufferUtils.clone(getDataReadOnly());
        vb.format = format;
        vb.handleRef = new Object();
        vb.id = -1;
        vb.normalized = normalized;
        vb.offset = offset;
        vb.stride = stride;
        vb.updateNeeded = true;
        vb.usage = usage;
        return vb;
    }

    @Override
    public String toString(){
        String dataTxt = null;
        if (data != null){
            dataTxt = ", elements="+data.capacity();
        }
        return getClass().getSimpleName() + "[fmt="+format.name()
                                            +", type="+bufType.name()
                                            +", usage="+usage.name()
                                            +dataTxt+"]";
    }

    @Override
    public void resetObject() {
//        assert this.id != -1;
        this.id = -1;
        setUpdateNeeded();
    }

    @Override
    public void deleteObject(Object rendererObject) {
        ((Renderer)rendererObject).deleteBuffer(this);
    }

    @Override
    public NativeObject createDestructableClone(){
        return new VertexBuffer(id);
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(components, "components", 0);
        oc.write(usage, "usage", Usage.Dynamic);
        oc.write(bufType, "buffer_type", null);
        oc.write(format, "format", Format.Float);
        oc.write(normalized, "normalized", false);
        oc.write(offset, "offset", 0);
        oc.write(stride, "stride", 0);

        String dataName = "data" + format.name();
        Buffer roData = getDataReadOnly();
        switch (format){
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
                throw new IOException("Unsupported export buffer format: "+format);
        }
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        components = ic.readInt("components", 0);
        usage = ic.readEnum("usage", Usage.class, Usage.Dynamic);
        bufType = ic.readEnum("buffer_type", Type.class, null);
        format = ic.readEnum("format", Format.class, Format.Float);
        normalized = ic.readBoolean("normalized", false);
        offset = ic.readInt("offset", 0);
        stride = ic.readInt("stride", 0);
        componentsLength = components * format.getComponentSize();

        String dataName = "data" + format.name();
        switch (format){
            case Float:
                data = ic.readFloatBuffer(dataName, null);
                break;
            case Short:
            case UnsignedShort:
                data = ic.readShortBuffer(dataName, null);
                break;
            case UnsignedByte:
            case Byte:
            case Half:
                data = ic.readByteBuffer(dataName, null);
                break;
            case Int:
            case UnsignedInt:
                data = ic.readIntBuffer(dataName, null);
                break;
            default:
                throw new IOException("Unsupported import buffer format: "+format);
        }
    }

}
