package com.jme3.vulkan.mesh.attributes;

import com.jme3.export.*;
import com.jme3.util.struct.Struct;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.mesh.VertexAttr;
import com.jme3.vulkan.tmp.Final;
import com.jme3.vulkan.tmp.FinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;

import java.io.IOException;

public final class SingleAttrStruct<T extends Savable> extends Struct<VertexAttr> {

    @Final private VertexAttr<T> field;

    @SerializationOnly
    private SingleAttrStruct() {}

    public SingleAttrStruct(String name, T alias) {
        addFields(field = new VertexAttr<>(name, alias));
    }

    public SingleAttrStruct(String name, T alias, Format... formats) {
        addFields(field = new VertexAttr<>(name, alias, formats));
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(field.getName(), "name", null);
        out.write(field.alias(), "alias", null);
        int[] ordinals = new int[field.getFormats().length];
        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = field.getFormats()[i].ordinal();
        }
        out.write(ordinals, "formats", null);
    }

    @Override
    @FinalWriter
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        int[] ordinals = in.readIntArray("formats", null);
        Format[] formats = new Format[ordinals.length];
        for (int i = 0; i < formats.length; i++) {
            formats[i] = Format.values()[ordinals[i]];
        }
        addFields(field = new VertexAttr<>(in.readString("name", null), (T)in.readSavable("alias", null), formats));
    }

    public VertexAttr<T> get() {
        return field;
    }

}
