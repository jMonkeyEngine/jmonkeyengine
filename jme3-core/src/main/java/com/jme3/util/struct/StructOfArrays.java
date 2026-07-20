package com.jme3.util.struct;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.tmp.Final;
import com.jme3.vulkan.tmp.FinalWriter;

import java.io.IOException;
import java.util.Iterator;

public class StructOfArrays <T extends StructField> extends Struct<T> implements StructuredArray<StructOfArrays<T>> {

    @Final private int length;
    private int index = 0;

    protected StructOfArrays() {}

    public StructOfArrays(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Array length must be positive.");
        }
        this.length = length;
    }

    @Override
    public void computeOffsets() {
        size = 0;
        alignment = Math.max(layout.getMinStructAlignment(), layout.getMinArrayAlignment());
        for (T f : getFields()) {
            int align = Math.max(f.getAlignment(), layout.getMinArrayAlignment());
            int stride = FastMath.toMultipleOf(f.capacity(), align);
            size = f.bind(this, size + index * stride) + (length - index) * stride;
            alignment = Math.max(alignment, align);
        }
        size = FastMath.toMultipleOf(size, alignment);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(length, "length", 0);
    }

    @Override
    @FinalWriter
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        length = in.readInt("length", 0);
    }

    @SuppressWarnings("unchecked")
    public void copyArraysTo(StructOfArrays dstStruct) {
        DataBuffer srcCache = cache();
        DataBuffer dstCache = dstStruct.cache();
        for (Iterator<? extends StructField> srcIt = getFields().iterator(), dstIt = dstStruct.getFields().iterator(); srcIt.hasNext() && dstIt.hasNext();) {
            StructField src = srcIt.next();
            StructField dst = dstIt.next();
            int srcAlign = Math.max(src.getAlignment(), layout.getMinArrayAlignment());
            int dstAlign = Math.max(dst.getAlignment(), layout.getMinArrayAlignment());
            srcCache.position(FastMath.toMultipleOf(srcCache.position(), srcAlign));
            dstCache.position(FastMath.toMultipleOf(dstCache.position(), dstAlign));
            int srcSize = length * FastMath.toMultipleOf(src.capacity(), srcAlign);
            int dstSize = dstStruct.getLength() * FastMath.toMultipleOf(dst.capacity(), dstAlign);
            int copySize = Math.min(srcSize, dstSize);
            srcCache.size(copySize);
            dstCache.size(copySize);
            srcCache.copyTo(dstCache);
            srcCache.offset(srcSize);
            dstCache.offset(dstSize);
        }
    }

    @Override
    public StructOfArrays<T> index(int index) {
        if (this.index != index) {
            this.index = index;
            computeOffsets();
        }
        return this;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getIndex() {
        return index;
    }

}
