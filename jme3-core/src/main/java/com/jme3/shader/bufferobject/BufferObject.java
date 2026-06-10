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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.Renderer;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObject;

/**
 * A generic memory buffer that can be divided in logical regions
 * 
 * @author Riccardo Balbo
 */
public class BufferObject extends NativeObject implements Savable {
    /**
     * Hint to suggest the renderer how to access this buffer
     */
    public static enum AccessHint {
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
    public static enum NatureHint {
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

    private transient int binding = -1;
    protected transient DirtyRegionsIterator dirtyRegionsIterator;

    protected Buffer data = null;
    protected transient boolean ownsData = true;
    protected ArrayList<BufferRegion> regions = new ArrayList<BufferRegion>();
    protected String name;

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
        if (data != null && ownsData) {
            BufferUtils.destroyDirectBuffer(data);
        }
        this.data = BufferUtils.createByteBuffer(length);
        ownsData = true;
        setUpdateNeeded();
    }


    /**
     * Transfer remaining bytes of passed buffer to the internal buffer of this buffer object
     *
     * @param data ByteBuffer containing the data to pass
     */
    public void setByteData(ByteBuffer data) {
        if (data == null) {
            if (this.data != null) {
                if (ownsData) {
                    BufferUtils.destroyDirectBuffer(this.data);
                }
                this.data = null;
            }
            ownsData = true;
            setUpdateNeeded();
            return;
        }
        ByteBuffer source = data.duplicate().order(data.order());
        ByteBuffer oldData = (ByteBuffer) this.data;
        boolean oldOwnsData = ownsData;

        this.data = BufferUtils.createByteBuffer(source.remaining());
        ((ByteBuffer) this.data).order(source.order());
        ((ByteBuffer) this.data).put(source);
        ((ByteBuffer) this.data).clear();
        ownsData = true;

        if (oldData != null && oldOwnsData) {
            BufferUtils.destroyDirectBuffer(oldData);
        }
        setUpdateNeeded();
    }

    /**
     * Sets byte-addressable data for this buffer object.
     *
     * @param data ByteBuffer containing the data to pass
     */
    public void setData(ByteBuffer data) {
        setByteData(data);
    }

    /**
     * Sets byte-addressable data from pointer storage without copying it.
     * <p>
     * The passed buffer is installed as the current internal backing buffer.
     * It remains owned by the caller and will not be destroyed by this buffer
     * object. Use {@link #setByteData(ByteBuffer)} when this object should own
     * a mutable copy.
     * <p>
     * This is not a permanent synchronization contract. Later operations may
     * replace the internal backing buffer with object-owned storage, for
     * example when layout regions require a larger mutable buffer. After that
     * replacement, mutations to the original pointer buffer are no longer
     * reflected by this buffer object.
     * <p>
     * Read-only buffers are allowed, but their limit must already cover all
     * layout regions because this object cannot resize read-only referenced
     * storage in place.
     *
     * @param data ByteBuffer to use directly
     */
    public void setByteDataPointer(ByteBuffer data) {
        if (data != null && !data.isDirect()) {
            throw new IllegalArgumentException("BufferObject data must be direct.");
        }
        if (this.data != null && this.data != data && ownsData) {
            BufferUtils.destroyDirectBuffer(this.data);
        }
        this.data = data;
        ownsData = false;
        setUpdateNeeded();
    }


  
    /**
     * Return buffer data.
     *
     * @return buffer data
     */
    @SuppressWarnings("unchecked")
    public <T extends Buffer> T getData() {
        return (T) getByteData();
    }

    /**
     * Return byte-addressable buffer data.
     * <p>
     * The returned buffer is the internal backing buffer. Do not mutate its
     * position or limit directly. Use {@link ByteBuffer#duplicate()} before
     * changing cursor state for reads, writes, or uploads.
     * <p>
     * When layout regions exist, this method ensures the backing buffer can
     * cover the last region. If the current backing buffer is read-only and too
     * small, this method throws because it cannot resize the referenced storage
     * without replacing it. Provide a correctly sized buffer to
     * {@link #setByteDataPointer(ByteBuffer)} or use {@link #setByteData(ByteBuffer)}
     * to let this object own a mutable copy.
     *
     * @return byte buffer data
     */
    public ByteBuffer getByteData() {
        if (regions.size() == 0) {
            if (data == null) data = BufferUtils.createByteBuffer(0);
        } else {
            int regionsEnd = regions.get(regions.size() - 1).getEnd();
            if (data == null) {
                data = BufferUtils.createByteBuffer(regionsEnd + 1);
                ownsData = true;
            } else if (data.limit() <= regionsEnd) {
                if (data.isReadOnly()) {
                    throw new IllegalStateException("Read-only BufferObject data is too small for its regions. "
                            + "Provide a direct ByteBuffer whose limit covers the last region, or use setByteData() "
                            + "so the BufferObject can own a mutable copy.");
                }
                // new buffer
                ByteBuffer newData = BufferUtils.createByteBuffer(regionsEnd + 1);
                newData.order(((ByteBuffer) data).order());

                // copy old buffer in new buffer
                ByteBuffer oldData = ((ByteBuffer) data).duplicate();
                oldData.clear();
                if (newData.limit() < oldData.limit()) oldData.limit(newData.limit());
                newData.put(oldData);

                // destroy old buffer
                if (ownsData) {
                    BufferUtils.destroyDirectBuffer(data);
                }

                data = newData;
                ownsData = true;
            }
        }
        return (ByteBuffer) data;
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
     * Returns true when this buffer has explicit dirty/layout regions.
     *
     * @return true if regions are defined
     */
    public boolean hasRegions() {
        return !regions.isEmpty();
    }

    /**
     * Adds a byte range that needs to be uploaded.
     *
     * @param start byte offset of the first dirty byte
     * @param length number of dirty bytes
     */
    public void addDirtyRegion(int start, int length) {
        if (start < 0) {
            throw new IllegalArgumentException("Region start cannot be negative");
        }
        if (length <= 0) {
            return;
        }
        BufferRegion region = new BufferRegion(start, start + length - 1);
        region.bo = this;
        region.markDirty();
        int insertIndex = 0;
        while (insertIndex < regions.size() && regions.get(insertIndex).getStart() <= start) {
            insertIndex++;
        }
        regions.add(insertIndex, region);
        updateNeeded = true;
    }

    /**
     * Add a region at the end of the layout
     * 
     * @param lr
     */
    public void setRegions(List<BufferRegion> lr) {
        regions.clear();
        regions.addAll(lr);
        regions.sort((a, b) -> Integer.compare(a.getStart(), b.getStart()));
        regions.trimToSize();
        setUpdateNeeded();
    }

  
    /**
     * Return all the regions of this layout
     * 
     * @return ordered list of regions
     */
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
        setUpdateNeeded();
    }

    @Override
    protected void deleteNativeBuffers() {
        super.deleteNativeBuffers();
        if (data != null && ownsData) BufferUtils.destroyDirectBuffer(data);
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

    public AccessHint getAccessHint() {
        return accessHint;
    }

    /**
     * Set AccessHint to hint the renderer on how to access this data. 
     * 
     * @param accessHint the access hint
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
        ByteBuffer writeData = null;
        if (data != null) {
            writeData = ((ByteBuffer) data).duplicate();
            writeData.clear();
        }
        oc.write(writeData, "data", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        accessHint = AccessHint.values()[ic.readInt("accessHint", 0)];
        natureHint = NatureHint.values()[ic.readInt("natureHint", 0)];
        @SuppressWarnings("unchecked")
        ArrayList<BufferRegion> readRegions = (ArrayList<BufferRegion>) (ArrayList<?>) ic.readSavableArrayList("regions", null);
        if (readRegions != null) {
            regions.addAll(readRegions);
        }
        data = ic.readByteBuffer("data", null);
        setUpdateNeeded(true);
    }

    @Override
    public BufferObject clone() {
        BufferObject clone = (BufferObject) super.clone();
        clone.binding = -1;
        if (data instanceof ByteBuffer) {
            ByteOrder order = ((ByteBuffer) data).order();
            ByteBuffer cloneSource = ((ByteBuffer) data).duplicate();
            cloneSource.order(order);
            cloneSource.clear();
            clone.data = BufferUtils.clone(cloneSource);
            ((ByteBuffer) clone.data).order(order);
            clone.data.clear();
            clone.ownsData = true;
        } else if (data != null) {
            clone.data = BufferUtils.clone(data);
            clone.data.clear();
            clone.ownsData = true;
        } else {
            clone.data = null;
            clone.ownsData = true;
        }
        clone.regions = new ArrayList<BufferRegion>();
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
