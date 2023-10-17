package com.jme3.shader.bufferobject;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;

/**
 * A slice of a buffer
 * 
 * @author Riccardo Balbo
 *
 */
public class BufferRegion implements Savable, Cloneable {
    protected int start = -1;
    protected int end = -1;
    protected boolean dirty = true;
    protected boolean fullBufferRegion = false;
    protected BufferObject bo;
    protected ByteBuffer slice;
    protected ByteBuffer source;

    public BufferRegion(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public BufferRegion() {

    }

    /**
     * Rewind and get a ByteBuffer pointing to this region of the main buffer
     * 
     * @return ByteBuffer
     */
    public ByteBuffer getData() {
        ByteBuffer d = bo.getData();
        if (source == null || d != source || slice == null) {
            source = d;
            int currentPos = source.position();
            int currentLimit = source.limit();
            source.position(start);
            assert end < source.capacity() : "Can't set limit at " + end + " on capacity " + source.capacity();
            source.limit(end + 1);
            slice = source.slice();
            slice.order(source.order());
            assert slice.limit() == (end - start + 1) : "Capacity is " + slice.limit() + " but " + (end - start + 1) + " expected";
            source.position(currentPos);
            source.limit(currentLimit);
        }
        slice.rewind();
        return slice;
    }

    /**
     * Get beginning of the region
     * 
     * @return position in the buffer
     */
    public int getStart() {
        return start;
    }

    /**
     * Get end of the region
     * 
     * @return position in the buffer
     */
    public int getEnd() {
        return end;
    }

    /**
     * Get the length of this region
     * 
     * @return the length of this region
     */
    public int length() {
        return end - start + 1;
    }

    /**
     * Returns true of the region is dirty
     * 
     * @return
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Mark this region for update
     */
    public void markDirty() {
        dirty = true;
    }

    /**
     * Clear this region mark
     */
    public void clearDirty() {
        dirty = false;
    }

    /**
     * Returns true if this region includes the entire buffer. Can be used for
     * optimization.
     * 
     * @return
     */
    public boolean isFullBufferRegion() {
        return fullBufferRegion;
    }

    @Override
    public String toString() {
        return "Region [start=" + start + ", end=" + end + ", size=" + (end - start) + ", dirty=" + dirty + "]";
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(start, "start", 0);
        oc.write(end, "end", 0);
        oc.write(dirty, "dirty", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        start = ic.readInt("start", 0);
        end = ic.readInt("end", 0);
        dirty = ic.readBoolean("dirty", false);
    }

    @Override
    public BufferRegion clone() {
        try {
            return (BufferRegion) super.clone();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
