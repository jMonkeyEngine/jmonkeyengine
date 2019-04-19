/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
public class VertexBuffer extends BufferObject implements Savable, Cloneable {
  
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
         * when interleaving is used. By default the format is
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
         * 4 simultaneous POSTION, NORMAL and TANGENT targets.
         * <p>
         * Note that the MorphControl will find how many buffers can be supported for each mesh/material combination.
         * Note that all buffers have 3 components (Vector3f) even the Tangent buffer that
         * does not contain the w (handedness) component that will not be interpolated for morph animation.
         * <p>
         * Note that those buffers contain the difference between the base buffer (POSITION, NORMAL or TANGENT) and the target value
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
        MorphTarget13,

    }

    /**
     * The usage of the VertexBuffer, specifies how often the buffer
     * is used. This can determine if a vertex buffer is placed in VRAM
     * or held in video memory, but no guarantees are made- it's only a hint.
     */
    public static enum Usage {
        
        /**
         * Mesh data is sent once and very rarely updated.
         * The user is uploading data, but only the renderer is reading it. 
         */
        Static,

        /**
         * Mesh data is updated occasionally (once per frame or less).
         * The user is uploading data, but only the renderer is reading it. 
         */
        Dynamic,

        /**
         * Mesh data is updated every frame.
         * The user is uploading data, but only the renderer is reading it. 
         */
        Stream,

        /**
         * Mesh data is <em>not</em> sent to GPU at all. It is only
         * used by the CPU.
         */
        CpuOnly,
        
        /**
         * Mesh data is sent once and very rarely updated.
         * The user will not be uploading data, but the user will be downloading it.
         */
        StaticRead,

        /**
         * Mesh data is updated occasionally (once per frame or less).
         * The user will not be uploading data, but the user will be downloading it.
         */
        DynamicRead,

        /**
         * Mesh data is updated every frame.
         * The user will not be uploading data, but the user will be downloading it.
         */
        StreamRead,
        
        /**
         * Mesh data is sent once and very rarely updated.
         * The user will be neither uploading nor downloading the data.
         */
        StaticCopy,

        /**
         * Mesh data is updated occasionally (once per frame or less).
         * The user will be neither uploading nor downloading the data.
         */
        DynamicCopy,

        /**
         * Mesh data is updated every frame.
         * The user will be neither uploading nor downloading the data.
         */
        StreamCopy
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
    protected int stride = 0;

    protected Type bufType;
    protected boolean normalized = false;
    protected int instanceSpan = 0;

    /**
     * Creates an empty, uninitialized buffer.
     * Must call setupData() to initialize.
     * @param type
     */
    public VertexBuffer(Type type){
        super(type==Type.Index?Target.ElementArray:Target.Array);
        this.bufType = type;
    }

    /**
     * Serialization only. Do not use.
     */
    public VertexBuffer(){
        super();
    }

    protected VertexBuffer(int id){
        super(id);
    }

    public boolean invariant() {
        // Does the VB hold any data?
        if (data == null) {
            throw new AssertionError();
        }
        // Position must be 0.
        if (data.position() != 0) {
            throw new AssertionError();
        }
        // Is the size of the VB == 0?
        if (data.limit() == 0) {
            throw new AssertionError();
        }
        // Does offset exceed buffer limit or negative?
        if (offset > data.limit() || offset < 0) {
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
        /*} else*/ if (usage != Usage.CpuOnly && !data.isDirect()) {
            throw new AssertionError();
        }

        // Double/Char/Long buffers are not supported for VertexBuffers.
        // For the rest, ensure they comply with the "Format" value.
        if (data instanceof DoubleBuffer) {
            throw new AssertionError();
        } else if (data instanceof CharBuffer) {
            throw new AssertionError();
        } else if (data instanceof LongBuffer) {
            throw new AssertionError();
        } else if (data instanceof FloatBuffer && format != Format.Float) {
            throw new AssertionError();
        } else if (data instanceof IntBuffer && format != Format.Int && format != Format.UnsignedInt) {
            throw new AssertionError();
        } else if (data instanceof ShortBuffer && format != Format.Short && format != Format.UnsignedShort) {
            throw new AssertionError();
        } else if (data instanceof ByteBuffer && format != Format.Byte && format != Format.UnsignedByte) {
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
     * Sets the instanceSpan to 1 or 0 depending on
     * the value of instanced and the existing value of
     * instanceSpan.
     */
    public void setInstanced(boolean instanced) {
        if( instanced && instanceSpan == 0 ) {
            instanceSpan = 1;
        } else if( !instanced ) {
            instanceSpan = 0;
        }
    }

    /**
     * Returns true if instanceSpan is more than 0 indicating
     * that this vertex buffer contains per-instance data.
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
    public Type getBufferType(){
        return bufType;
    }

    /**
     *  Returns the number of 'instances' in this VertexBuffer.  This
     *  is dependent on the current instanceSpan.  When instanceSpan
     *  is 0 then 'instances' is 1.  Otherwise, instances is elements *
     *  instanceSpan.  It is possible to render a mesh with more instances
     *  but the instance data begins to repeat.
     */
    public int getBaseInstanceCount() {
        if( instanceSpan == 0 ) {
            return 1;
        }
        return getNumElements() * instanceSpan;
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
        if (outVb.format != format || outVb.components != components) {
            throw new IllegalArgumentException("Buffer format mismatch. Cannot copy");
        }

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
        if (components < 1 || components > 4) {
            throw new IllegalArgumentException("Num components must be between 1 and 4");
        }

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
        vb.instanceSpan = instanceSpan;
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
            dataTxt = ", elements="+data.limit();
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
    protected void deleteNativeBuffers() {
        if (data != null) {
            BufferUtils.destroyDirectBuffer(data);
        }
    }
            
    @Override
    public NativeObject createDestructableClone(){
        return new VertexBuffer(id);
    }

    @Override
    public long getUniqueId() {
        return ((long)OBJTYPE_VERTEXBUFFER << 32) | ((long)id);
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
        target = bufType==Type.Index?Target.ElementArray:Target.Array;
        
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
