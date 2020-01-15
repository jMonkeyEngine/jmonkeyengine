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
import com.jme3.scene.VertexBuffer.*;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObject;
import java.io.IOException;
import java.nio.*;

/**
 * Buffer objects stores memory to be uploaded/downloaded to/from renderer.
 * 
 */
public class BufferObject extends NativeObject implements Savable, Cloneable {
  
    /**
     * Represents a buffer binding target for this object.
     * Subclasses of BufferObject may set an appropriate target.
     */
    public static enum Target {
        /**
         * Vertex attributes
         */
        Array,
        /**
         * Atomic counter storage
         */
        AtomicCounter,
        /**
         * Buffer copy source
         */
        CopyRead,
        /**
         * Buffer copy destination
         */
        CopyWrite,
        /**
         * Indirect compute dispatch commands
         */
        DispatchIndirect,
        /**
         * Indirect command arguments
         */
        DrawIndirect,
        /**
         * Vertex array indices
         */
        ElementArray,
        /**
         * Pixel read target
         */
        PixelPack,
        /**
         * Texture data source
         */
        PixelUnpack,
        /**
         * Query result buffer
         */
        Query,
        /**
         * Read-write storage for shaders
         */
        ShaderStorage,
        /**
         * Texture data buffer
         */
        Texture,
        /**
         * Transform feedback buffer
         */
        TransformFeedback,
        /**
         * Uniform block storage
         */
        Uniform;
    }

    protected int lastLimit = 0;
    protected int components = 0;

    /**
     * derived from components * format.getComponentSize()
     */
    protected transient int componentsLength = 0;
    protected Buffer data = null;
    
    protected Target target;
    protected Usage usage;
    protected Format format;

    protected transient boolean dataSizeChanged = false;

    /**
     * Serialization only. Do not use.
     */
    public BufferObject() {
        super();
    }

    /**
     * Creates an empty, uninitialized buffer.
     * Must call setupData() to initialize.
     * @param target
     */
    public BufferObject(Target target){
        super();
        this.target = target;
    }

   
    protected BufferObject(int id){
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
        
//        // Does offset exceed buffer limit or negative?
//        if (offset > data.limit() || offset < 0) {
//            throw new AssertionError();
//        }
//        // Are components between 1 and 4?
//        
//        // Are components between 1 and 4 and not InstanceData?
//        if (bufType != Type.InstanceData) {
//            if (components < 1 || components > 4) {
//                throw new AssertionError();
//            }
//        }

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
     * @return The target of this buffer. See {@link Target} for more
     * information.
     */
    public Target getTarget() {
        return target;
    }

    /**
     * @param target The target of this buffer. See {@link Target} for more
     * information.
     */
    public void setTarget(Target target) {
//        if (getId() != -1) 
//            throw new IllegalStateException("Can't change buffer's target after the buffer is initialized.");
        this.target = target;
        this.setUpdateNeeded();
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
        this.setUpdateNeeded();
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
        if( data == null ) {
            return 0;
        }
        int elements = data.limit() / components;
        if (format == Format.Half) {
            elements /= 2;
        }
        return elements;
    }

   /**
     * Called to initialize the data in the <code>BufferObject</code>. 
     * 
     * @param usage The usage for the data, or how often will the data
     * be updated per frame. See the {@link Usage} enum.
     * @param components The number of components per element.
     * @param format The {@link Format format}, or data-type of a single
     * component.
    */
    public void setupData(Usage usage, int components, Format format){
        if (id != -1) {
            throw new UnsupportedOperationException("Data has already been sent. Cannot setupData again.");
        }

        if (usage == null || format == null) {
            throw new IllegalArgumentException("None of the arguments can be null");
        }


//        if (bufType != Type.InstanceData) {
//            if (components < 1 || components > 4) {
//                throw new IllegalArgumentException("components must be between 1 and 4");
//            }
//        }
        
//        this.data = buf;
        this.components = components;
        this.usage = usage;
        this.format = format;
        this.componentsLength = components * format.getComponentSize();
//        this.lastLimit = buf.limit();
        setUpdateNeeded();
    }
    
    /**
     * Called to initialize the data in the <code>BufferObject</code>. Must only
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
        if (id != -1) {
            throw new UnsupportedOperationException("Data has already been sent. Cannot setupData again.");
        }

        if (usage == null || format == null || data == null) {
            throw new IllegalArgumentException("None of the arguments can be null");
        }

        if (data.isReadOnly()) {
            throw new IllegalArgumentException("VertexBuffer data cannot be read-only.");
        }

//        if (bufType != Type.InstanceData) {
//            if (components < 1 || components > 4) {
//                throw new IllegalArgumentException("components must be between 1 and 4");
//            }
//        }
        
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
     * <p>
     * It is allowed to specify a buffer with different capacity than the
     * originally set buffer, HOWEVER, if you do so, you must
     * call Mesh.updateCounts() otherwise bizarre errors can occur.
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
        if (id != -1) {
            throw new UnsupportedOperationException("Data has already been sent.");
        }

        if (format != Format.Float) {
            throw new IllegalStateException("Format must be float!");
        }

        int numElements = data.limit() / components;
        format = Format.Half;
        this.componentsLength = components * format.getComponentSize();
        
        ByteBuffer halfData = BufferUtils.createByteBuffer(componentsLength * numElements);
        halfData.rewind();

        FloatBuffer floatData = (FloatBuffer) data;
        floatData.rewind();

        for (int i = 0; i < floatData.limit(); i++){
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
     * Copies a single element of data from this <code>BufferObject</code>
     * to the given output BufferObject.
     * 
     * @param inIndex The input element index
     * @param outVb The buffer to copy to
     * @param outIndex The output element index
     * 
     * @throws IllegalArgumentException If the formats of the buffers do not
     * match.
     */
    public void copyElement(int inIndex, BufferObject outVb, int outIndex){
        copyElements(inIndex, outVb, outIndex, 1);
    }

    /**
     * Copies a sequence of elements of data from this <code>BufferObject</code>
     * to the given output BufferObject.
     * 
     * @param inIndex The input element index
     * @param outVb The buffer to copy to
     * @param outIndex The output element index
     * @param len The number of elements to copy
     * 
     * @throws IllegalArgumentException If the formats of the buffers do not
     * match.
     */
    public void copyElements(int inIndex, BufferObject outVb, int outIndex, int len){
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
    public BufferObject clone(){
        // NOTE: Superclass GLObject automatically creates shallow clone
        // e.g re-use ID.
        BufferObject bo = (BufferObject) super.clone();
        bo.handleRef = new Object();
        bo.id = -1;
        if (data != null) {
            // Make sure to pass a read-only buffer to clone so that
            // the position information doesn't get clobbered by another
            // reading thread during cloning (and vice versa) since this is
            // a purely read-only operation.
            bo.updateData(BufferUtils.clone(getDataReadOnly()));
        }
        
        return bo;
    }

    

    @Override
    public String toString(){
        String dataTxt = null;
        if (data != null){
            dataTxt = ", elements="+data.limit();
        }
        return getClass().getSimpleName() + "[fmt="+format.name()
//                                            +", type="+bufType.name()
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
        return new BufferObject(id);
    }

    @Override
    public long getUniqueId() {
        return ((long)OBJTYPE_BO << 32) | ((long)id);
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(components, "components", 0);
        oc.write(usage, "usage", Usage.Dynamic);
        oc.write(target, "target", null);
//        oc.write(bufType, "buffer_type", null);
        oc.write(format, "format", Format.Float);
//        oc.write(normalized, "normalized", false);
//        oc.write(offset, "offset", 0);
//        oc.write(stride, "stride", 0);
//        oc.write(instanceSpan, "instanceSpan", 0);

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
        target = ic.readEnum("target", Target.class, null);
//        bufType = ic.readEnum("buffer_type", Type.class, null);
        format = ic.readEnum("format", Format.class, Format.Float);
//        normalized = ic.readBoolean("normalized", false);
//        offset = ic.readInt("offset", 0);
//        stride = ic.readInt("stride", 0);
//        instanceSpan = ic.readInt("instanceSpan", 0);
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
