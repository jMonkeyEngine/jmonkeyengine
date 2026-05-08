package com.jme3.util.struct;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.vulkan.tmp.SerializationOnly;

import java.io.IOException;

/**
 * Stupid idea in the first place.
 *
 * @param <T>
 */
@Deprecated
public abstract class FixedSizeStruct <T extends StructField> extends Struct<T> {

    private int fixedSize = 1;

    @SerializationOnly
    public FixedSizeStruct() {}

    public FixedSizeStruct(int size) {
        assert size > 0 : "Fixed size must be positive.";
        this.fixedSize = size;
    }

    @Override
    public int getSize() {
        if (super.getSize() > fixedSize) {
            throw new IllegalStateException("Computed struct size exceeded specified fixed size.");
        }
        return fixedSize;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(fixedSize, "fixedSize", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        fixedSize = in.readInt("fixedSize", 0);
    }

    public void setFixedSize(int fixedSize) {
        this.fixedSize = fixedSize;
    }

    public int getFixedSize() {
        return fixedSize;
    }

    public int getRealSize() {
        return super.getSize();
    }

}
