/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package com.jme3.shader.bufferobject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.Renderer;
import com.jme3.util.NativeObject;
import com.jme3.util.struct.StructuredBuffer;
import com.jme3.vulkan.buffers.NioBuffer;
import com.jme3.vulkan.buffers.stream.Updateable;
import com.jme3.vulkan.memory.MemorySize;

/**
 * A generic memory buffer that can be divided in logical regions
 * 
 * @author Riccardo Balbo
 */
public class BufferObject extends NativeObject implements StructuredBuffer, Savable {

    /**
     * Hint to suggest the renderer how to access this buffer
     */
    public enum AccessHint {
        /**
         * The data store contents will be modified once and used many times.
         */
        Static,
        /**
         * The data store contents will be modified once and used at most a few
         * times.
         */
        Stream,
        /**
         * The data store contents will be modified repeatedly and used many
         * times.
         */
        Dynamic,
        /**
         * Used only by the cpu.
         */
        CpuOnly
    }

    /**
     * Hint to suggest the renderer how the data should be used
     */
    public enum NatureHint {
        /**
         * The data store contents are modified by the application, and used as
         * the source for GL drawing and image specification commands.
         */
        Draw,
        /**
         * The data store contents are modified by reading data from the GL, and
         * used to return that data when queried by the application.
         */
        Read,
        /**
         * The data store contents are modified by reading data from the GL, and
         * used as the source for GL drawing and image specification commands.
         */
        Copy
    }

    private AccessHint accessHint = AccessHint.Dynamic;
    private NatureHint natureHint = NatureHint.Draw;

    private transient WeakReference<BufferObject> weakRef;
    private transient int binding = -1;
    protected transient DirtyRegionsIterator dirtyRegionsIterator;

    protected NioBuffer data;
    protected ArrayList<BufferRegion> regions = new ArrayList<>();
    private String name;

    public BufferObject() {
        super();
    }

  
    protected BufferObject(int id) {
        super(id);
    }

  
    /**
     * Internal use only. Indicates that the object has changed and its state
     * needs to be updated. Mark all the regions as dirty.
     */
    @Override
    public final void setUpdateNeeded() {
        setUpdateNeeded(true);
    }

    /**
     * Indicates that the object has changed and its state needs to be updated.
     * 
     * @param dirtyAll
     *            mark all regions for update
     */
    public void setUpdateNeeded(boolean dirtyAll) {
        if (dirtyAll) markAllRegionsDirty();
        updateNeeded = true;
    }


    /**
     * Get binding point
     * 
     * @return the binding point
     */
    public int getBinding() {
        return binding;
    }


    /**
     * Initialize an empty buffer object of the given length
     * 
     * @param length expected length of the buffer object
     */
    public void initializeEmpty(int length) {
        data = new NioBuffer(MemorySize.bytes(length));
        data.push();
    }


    /**
     * Transfer remaining bytes of passed buffer to the internal buffer of this buffer object
     * 
     * @param data ByteBuffer containing the data to pass
     */
    public void setData(ByteBuffer data) {
        this.data = new NioBuffer(MemorySize.bytes(data.remaining()), 0, false);
        this.data.copy(data);
        this.data.push();
    }


  
    /**
     * Rewind and return buffer data
     * 
     * @return
     */
    public Updateable getData() {
//        if (regions.isEmpty()) {
//            if (data == null) data = BufferUtils.createByteBuffer(0);
//        } else {
//            int regionsEnd = regions.get(regions.size() - 1).getEnd();
//            if (data == null) {
//                data = BufferUtils.createByteBuffer(regionsEnd + 1);
//            } else if (data.limit() < regionsEnd) {
//                // new buffer
//                ByteBuffer newData = BufferUtils.createByteBuffer(regionsEnd + 1);
//
//                // copy old buffer in new buffer
//                if (newData.limit() < data.limit()) data.limit(newData.limit());
//                newData.put(data);
//
//                // destroy old buffer
//                BufferUtils.destroyDirectBuffer(data);
//
//                data = newData;
//            }
//        }
//        data.rewind();
        return data;
    }

    @Override
    public boolean isUpdateNeeded() {
        return super.isUpdateNeeded() || (data != null && data.getUpdateRegions().getCoverage() > 0);
    }

    /**
     * Get dirty regions
     * 
     * @return Helper object to iterate through dirty regions
     */
    public DirtyRegionsIterator getDirtyRegions() {
        if (dirtyRegionsIterator == null) dirtyRegionsIterator = new DirtyRegionsIterator(this);
        dirtyRegionsIterator.rewind();
        return dirtyRegionsIterator;
    }

    /**
     * Reset layour definition
     */
    public void unsetRegions() {
        regions.clear();
        regions.trimToSize();
    }

    /**
     * Add a list of regions at the end of the layout
     * 
     * @param lr list of regions
     */
    @Override
    public void setRegions(List<BufferRegion> lr) {
        regions.clear();
        regions.addAll(lr);
        regions.trimToSize();
        setUpdateNeeded();
    }

  
    /**
     * Return all the regions of this layout
     * 
     * @return ordered list of regions
     */
    @Override
    public BufferRegion getRegion(int i) {
        BufferRegion region = regions.get(i);
        region.bo = this;
        return region;
    }

    /**
     * Mark all regions as dirty
     */
    public void markAllRegionsDirty() {
        for (BufferRegion r : regions) r.markDirty();
    }


    @Override
    public void resetObject() {
        this.id = -1;
    }

    @Override
    protected void deleteNativeBuffers() {
        super.deleteNativeBuffers();
        //if (data != null) BufferUtils.destroyDirectBuffer(data);
    }

    @Override
    public void deleteObject(final Object rendererObject) {
        if (!(rendererObject instanceof Renderer)) {
            throw new IllegalArgumentException("This bo can't be deleted from " + rendererObject);
        }
        ((Renderer) rendererObject).deleteBuffer(this);
    }

    @Override
    public NativeObject createDestructableClone() {
        return new BufferObject(getId());
    }

    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_BO << 32) | (0xffffffffL & (long) id);
    }

    /**
     * Set binding point
     * @param binding binding point
     */
    public void setBinding(final int binding) {
        this.binding = binding;
    }

    @Override
    public WeakReference<BufferObject> getWeakRef() {
        if (weakRef == null) weakRef = new WeakReference<BufferObject>(this);
        return weakRef;
    }

    public AccessHint getAccessHint() {
        return accessHint;
    }

    /**
     * Set AccessHint to hint the renderer on how to access this data. 
     * 
     * @param accessHint hint
     */
    public void setAccessHint(AccessHint accessHint) {
        this.accessHint = accessHint;
        setUpdateNeeded();
    }

    public NatureHint getNatureHint() {
        return natureHint;
    }

    /**
     * Set NatureHint to hint the renderer on how to use this data. 
     * 
     * @param natureHint
     */
    public void setNatureHint(NatureHint natureHint) {
        this.natureHint = natureHint;
        setUpdateNeeded();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(accessHint.ordinal(), "accessHint", 0);
        oc.write(natureHint.ordinal(), "natureHint", 0);
        oc.writeSavableArrayList(regions, "regions", null);
        oc.write(data.mapBytes(), "data", null);
        data.unmap();
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        accessHint = AccessHint.values()[ic.readInt("accessHint", 0)];
        natureHint = NatureHint.values()[ic.readInt("natureHint", 0)];
        regions.addAll(ic.readSavableArrayList("regions", null));
        ByteBuffer readData = ic.readByteBuffer("data", null);
        data = new NioBuffer(MemorySize.bytes(readData.remaining()));
        data.copy(readData);
        data.push();
        setUpdateNeeded(true);
    }

    @Override
    public BufferObject clone() {
        BufferObject clone = (BufferObject) super.clone();
        clone.binding = -1;
        clone.weakRef = null;
        clone.data = new NioBuffer(data.size());
        clone.data.copy(data);
        clone.data.push();
        clone.regions = new ArrayList<>();
        assert clone.regions != regions;
        for (BufferRegion r : regions) {
            clone.regions.add(r.clone());
        }
        clone.dirtyRegionsIterator = null;

        clone.setUpdateNeeded();
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("{\n");
        for (BufferRegion r : regions) {
            sb.append("    ").append(r).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Get name of the buffer object
     * 
     * @return the name of this buffer object, can be null
     */
    public String getName() {
        return name;
    }

    /**
     * Set name for debugging purposes
     * 
     * @param name
     *            the name of this buffer object
     */
    public void setName(String name) {
        this.name = name;
    }
}
