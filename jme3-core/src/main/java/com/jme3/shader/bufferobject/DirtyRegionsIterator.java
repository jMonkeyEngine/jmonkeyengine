package com.jme3.shader.bufferobject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * An helper class that iterates and merges dirty regions
 * 
 * @author Riccardo Balbo
 */
public class DirtyRegionsIterator {

    private static class DirtyRegion extends BufferRegion {
        List<BufferRegion> regions = new ArrayList<BufferRegion>();

        @Override
        public ByteBuffer getData() {
            ByteBuffer d = bo.getData();
            if (source == null || d != source || slice == null) {
                source = d;
                int currentPos = source.position();
                int currentLimit = source.limit();
                source.limit(source.capacity());
                source.position(0);
                slice = source.slice();
                source.limit(currentLimit);
                source.position(currentPos);
            }
            slice.limit(end);
            slice.position(start);
            return slice;
        }

        public void clearDirty() {
            regions.forEach(BufferRegion::clearDirty);
            super.clearDirty();
        }
    }

    private BufferObject bufferObject;

    public DirtyRegionsIterator(BufferObject bufferObject) {
        this.bufferObject = bufferObject;
    }

    private final DirtyRegion dirtyRegion = new DirtyRegion();
    private int pos = 0;

    public void rewind() {
        pos = 0;
    }

    public BufferRegion getNext() {

        dirtyRegion.bo = bufferObject;
        dirtyRegion.regions.clear();

        if (bufferObject.regions.size() == 0) {
            if (!bufferObject.isUpdateNeeded()) return null;
            dirtyRegion.fullBufferRegion = true;
            dirtyRegion.end = bufferObject.getData().limit();
            dirtyRegion.start = 0;
            return dirtyRegion;
        }


        while (pos < bufferObject.regions.size()) {
            BufferRegion dr = bufferObject.regions.get(pos++);
            if (dr.isDirty()) {
                if (dirtyRegion.regions.size() == 0) dirtyRegion.start = dr.start;
                dirtyRegion.end = dr.end;
                dirtyRegion.regions.add(dr);
            } else if (dirtyRegion.regions.size() != 0) break;
        }

        if (dirtyRegion.regions.size() == 0) return null;
        dirtyRegion.fullBufferRegion = dirtyRegion.regions.size() == bufferObject.regions.size();
        dirtyRegion.markDirty();

        return dirtyRegion;
    }

}
