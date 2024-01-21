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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An helper class that iterates and merges dirty regions
 * 
 * @author Riccardo Balbo
 */
public class DirtyRegionsIterator implements Iterator<BufferRegion> {

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

    public boolean hasNext() {
        return pos < bufferObject.regions.size();
    }

    public BufferRegion next() {

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
