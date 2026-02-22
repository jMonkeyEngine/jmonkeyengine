package com.jme3.vulkan.mesh;

import com.jme3.export.*;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.tmp.EffectivelyFinal;
import com.jme3.vulkan.tmp.EffectivelyFinalWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class NamedAttribute implements Savable {

    @EffectivelyFinal
    private String name;
    @EffectivelyFinal
    private Format[] formats;
    @EffectivelyFinal
    private Function<AttributeMappingInfo, Attribute> mapper;
    @EffectivelyFinal
    private int offset, size;

    public NamedAttribute(String name, Format[] formats, Function<AttributeMappingInfo, Attribute> mapper, int offset) {
        this.name = name;
        this.formats = formats;
        this.mapper = mapper;
        this.offset = offset;
        for (Format f : formats) {
            size += f.getBytes();
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(name, "name", null);
        out.write(offset, "offset", 0);
        int[] fmtEnums = new int[formats.length];
        for (int i = 0; i < fmtEnums.length; i++) {
            fmtEnums[i] = formats[i].ordinal();
        }
        out.write(fmtEnums, "formats", null);
    }

    @Override
    @EffectivelyFinalWriter
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", null);
        offset = in.readInt("offset", 0);
        int[] fmtEnums = in.readIntArray("formats", null);
        formats = new Format[fmtEnums.length];
        for (int i = 0; i < formats.length; i++) {
            size += (formats[i] = Format.values()[fmtEnums[i]]).getBytes();
        }
    }

    public <T extends Attribute> T map(Mesh mesh, VertexBinding binding) {
        return (T)mapper.apply(new AttributeMappingInfo(binding, mesh.getVertexBuffer(binding).getData(),
                mesh.getElements(binding.getInputRate()), binding.getOffset()));
    }

    public String getName() {
        return name;
    }

    public Function<AttributeMappingInfo, Attribute> getMapper() {
        return mapper;
    }

    public Format[] getFormats() {
        return formats;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NamedAttribute that = (NamedAttribute) o;
        return offset == that.offset && Arrays.equals(formats, that.formats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(formats), offset);
    }

}
