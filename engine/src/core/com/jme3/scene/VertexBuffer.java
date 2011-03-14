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

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.renderer.GLObject;
import com.jme3.renderer.Renderer;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * A <code>VertexBuffer</code> contains a particular type of geometry
 * data used by {@link Mesh}es. Every VertexBuffer set on a <code>Mesh</code>
 * is sent as an attribute to the vertex shader to be processed.
 */
public class VertexBuffer extends GLObject implements Savable, Cloneable {

    /**
     * Type of buffer. Specifies the actual attribute it defines.
     */
    public static enum Type {
        /**
         * Position of the vertex (3 floats)
         */
        Position,

        /**
         * The size of the point when using point buffers.
         */
        Size,

        /**
         * Normal vector, normalized.
         */
        Normal,

        /**
         * Texture coordinate
         */
        TexCoord,

        /**
         * Color and Alpha (4 floats)
         */
        Color,

        /**
         * Tangent vector, normalized.
         */
        Tangent,

        /**
         * Binormal vector, normalized.
         */
        Binormal,

        /**
         * Specifies the source data for various vertex buffers
         * when interleaving is used.
         */
        InterleavedData,

        /**
         * Do not use.
         */
        @Deprecated
        MiscAttrib,

        /**
         * Specifies the index buffer, must contain integer data.
         */
        Index,

        /** 
         * Inital vertex position, used with animation 
         */
        BindPosePosition,

        /** 
         * Inital vertex normals, used with animation
         */
        BindPoseNormal,

        /** 
         * Bone weights, used with animation
         */
        BoneWeight,

        /** 
         * Bone indices, used with animation
         */
        BoneIndex,

        /**
         * Texture coordinate #2
         */
        TexCoord2;
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
         * Mesh data is not sent to GPU at all. It is only
         * used by the CPU.
         */
        CpuOnly;
    }

    public static enum Format {
        // Floating point formats
        Half(2),
        Float(4),
        Double(8),

        // Integer formats
        Byte(1),
        UnsignedByte(1),
        Short(2),
        UnsignedShort(2),
        Int(4),
        UnsignedInt(4);

        private int componentSize = 0;

        Format(int componentSize){
            this.componentSize = componentSize;
        }

        /**
         * @return Size in bytes of this data type.
         */
        public int getComponentSize(){
            return componentSize;
        }
    }

    protected int offset = 0;
    protected int stride = 0;
    protected int components = 0;

    /**
     * derived from components * format.getComponentSize()
     */
    protected transient int componentsLength = 0;
    protected Buffer data = null;
    protected transient ByteBuffer mappedData;
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
        super(GLObject.Type.VertexBuffer);
        this.bufType = type;
    }

    /**
     * Do not use this constructor. Serialization purposes only.
     */
    public VertexBuffer(){
        super(GLObject.Type.VertexBuffer);
    }

    protected VertexBuffer(int id){
        super(GLObject.Type.VertexBuffer, id);
    }

    /**
     * @return The offset (in bytes) from the start of the buffer
     * after which the data is sent to the GPU.
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
     * @return The stride (in bytes) for the data. If the data is packed
     * in the buffer, then stride is 0, if there's other data that is between
     * the current component and the next component in the buffer, then this
     * specifies the size in bytes of that additional data.
     */
    public int getStride() {
        return stride;
    }

    /**
     * @param stride The stride (in bytes) for the data. If the data is packed
     * in the buffer, then stride is 0, if there's other data that is between
     * the current component and the next component in the buffer, then this
     * specifies the size in bytes of that additional data.
     */
    public void setStride(int stride) {
        this.stride = stride;
    }

    /**
     * @return A native buffer, in the specified {@link Format format}.
     */
    public Buffer getData(){
        return data;
    }

    public ByteBuffer getMappedData() {
        return mappedData;
    }

    public void setMappedData(ByteBuffer mappedData) {
        this.mappedData = mappedData;
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

        this.data = data;
        this.components = components;
        this.usage = usage;
        this.format = format;
        this.componentsLength = components * format.getComponentSize();
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

        // will force renderer to call glBufferData again
        if (this.data.capacity() != data.capacity()){
            dataSizeChanged = true;
        }
        this.data = data;
        setUpdateNeeded();
    }

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
     * @param numElements
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

    public Object getElementComponent(int elementIndex, int componentIndex){
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
                return bin.get(inPos + elementPos);
            case Short:
            case UnsignedShort:
                ShortBuffer sin = (ShortBuffer) data;
                return sin.get(inPos + elementPos);
            case Int:
            case UnsignedInt:
                IntBuffer iin = (IntBuffer) data;
                return iin.get(inPos + elementPos);
            case Float:
                FloatBuffer fin = (FloatBuffer) data;
                return fin.get(inPos + elementPos);
            default:
                throw new UnsupportedOperationException("Unrecognized buffer format: "+format);
        }
    }

    /**
     * Copies a single element of data from this <code>VertexBuffer</code>
     * to the given output VertexBuffer.
     * 
     * @param inIndex
     * @param outVb
     * @param outIndex
     */
    public void copyElement(int inIndex, VertexBuffer outVb, int outIndex){
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

        data.clear();
        outVb.data.clear();

        switch (format){
            case Byte:
            case UnsignedByte:
            case Half:
                ByteBuffer bin = (ByteBuffer) data;
                ByteBuffer bout = (ByteBuffer) outVb.data;
                bin.position(inPos).limit(inPos + elementSz);
                bout.position(outPos).limit(outPos + elementSz);
                bout.put(bin);
                break;
            case Short:
            case UnsignedShort:
                ShortBuffer sin = (ShortBuffer) data;
                ShortBuffer sout = (ShortBuffer) outVb.data;
                sin.position(inPos).limit(inPos + elementSz);
                sout.position(outPos).limit(outPos + elementSz);
                sout.put(sin);
                break;
            case Int:
            case UnsignedInt:
                IntBuffer iin = (IntBuffer) data;
                IntBuffer iout = (IntBuffer) outVb.data;
                iin.position(inPos).limit(inPos + elementSz);
                iout.position(outPos).limit(outPos + elementSz);
                iout.put(iin);
                break;
            case Float:
                FloatBuffer fin = (FloatBuffer) data;
                FloatBuffer fout = (FloatBuffer) outVb.data;
                fin.position(inPos).limit(inPos + elementSz);
                fout.position(outPos).limit(outPos + elementSz);
                fout.put(fin);
                break;
            default:
                throw new UnsupportedOperationException("Unrecognized buffer format: "+format);
        }

        data.clear();
        outVb.data.clear();
    }

    /**
     * Creates a {@link Buffer} that satisfies the given type and size requirements
     * of the parameters. The buffer will be of the type specified by
     * {@link Format format} and would be able to contain the given number
     * of elements with the given number of components in each element.
     *
     * @param format
     * @param components
     * @param numElements
     * @return
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

    public VertexBuffer clone(){
        // NOTE: Superclass GLObject automatically creates shallow clone
        // e.g re-use ID.
        VertexBuffer vb = (VertexBuffer) super.clone();
        if (data != null)
            vb.updateData(BufferUtils.clone(data));
        
        return vb;
    }

    public VertexBuffer clone(Type overrideType){
        VertexBuffer vb = new VertexBuffer(overrideType);
        vb.components = components;
        vb.componentsLength = componentsLength;
        vb.data = BufferUtils.clone(data);
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
    public void deleteObject(Renderer r) {
        r.deleteBuffer(this);
    }

    @Override
    public GLObject createDestructableClone(){
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
        switch (format){
            case Float:
                oc.write((FloatBuffer) data, dataName, null);
                break;
            case Short:
            case UnsignedShort:
                oc.write((ShortBuffer) data, dataName, null);
                break;
            case UnsignedByte:
            case Byte:
            case Half:
                oc.write((ByteBuffer) data, dataName, null);
                break;
            case Int:
            case UnsignedInt:
                oc.write((IntBuffer) data, dataName, null);
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
