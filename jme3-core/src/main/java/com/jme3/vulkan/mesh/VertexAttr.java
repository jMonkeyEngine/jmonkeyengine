package com.jme3.vulkan.mesh;

import com.jme3.export.*;
import com.jme3.math.*;
import com.jme3.util.struct.FieldDescription;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.tmp.Final;
import com.jme3.vulkan.tmp.FinalWriter;
import com.jme3.vulkan.tmp.SerializationOnly;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VertexAttr <T extends Savable> implements StructField<T>, Savable, Cloneable {

    public static final Map<Class, Format[]> implicitFormats = new HashMap<>();

    public static void addImplicitFormat(Class type, Format... formats) {
        if (formats.length == 0) {
            throw new IllegalArgumentException("At least one format must be specified.");
        }
        implicitFormats.put(type, formats);
    }

    static {
        addImplicitFormat(Boolean.class, Format.R8_SRGB);
        addImplicitFormat(Float.class, Format.R32_SFloat);
        addImplicitFormat(Vector2f.class, Format.RG32_SFloat);
        addImplicitFormat(Vector3f.class, Format.RGB32_SFloat);
        addImplicitFormat(Vector4f.class, Format.RGBA32_SFloat);
        addImplicitFormat(ColorRGBA.class, Format.RGBA32_SFloat);
        addImplicitFormat(Matrix3f.class, Format.RGB32_SFloat, Format.RGB32_SFloat, Format.RGB32_SFloat);
        addImplicitFormat(Matrix4f.class, Format.RGBA32_SFloat, Format.RGBA32_SFloat, Format.RGBA32_SFloat, Format.RGBA32_SFloat);
    }

    @Final private String name;
    @Final private Format[] formats;

    private T alias;
    private Struct struct;
    private FieldDescription<T> description;
    private int offset;

    @SerializationOnly
    public VertexAttr() {}

    public VertexAttr(String name, T alias) {
        assert name != null : "Attribute name cannot be null.";
        assert alias != null : "Struct field alias cannot be null.";
        this.name = name;
        this.alias = alias;
        this.formats = implicitFormats.get(alias.getClass());
        assert formats != null : alias.getClass() + " has no implicit formats assigned to it. " +
                "Specify formats manually or register implicit formats";
    }

    public VertexAttr(String name, T alias, Format... formats) {
        assert name != null : "Name cannot be null.";
        assert alias != null : "Struct field alias cannot be null.";
        assert formats != null && formats.length != 0 : "At least one format must be specified.";
        this.name = name;
        this.formats = formats;
        this.alias = alias;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(name, "name", null);
        out.write(formats, "formats", null);
        out.write(alias, "alias", null);
    }

    @Override
    @FinalWriter
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", null);
        formats = in.readEnumArray("formats", Format.values(), null);
        alias = (T)in.readSavable("alias", null);
    }

    @Override
    public int bind(Struct struct,  int offset) {
        this.struct = struct;
        this.description = struct.getLayout().getFieldDescription(alias.getClass());
        return this.offset = FastMath.toMultipleOf(offset, getAlignment());
    }

    @Override
    public void set(T value) {
        description.write(struct.getLayout(), , value);
    }

    @Override
    public T get() {
        return alias = description.read(struct.getLayout(), , alias);
    }

    @Override
    public T alias() {
        return alias;
    }

    @Override
    public int getSize() {
        return description.getSize(struct.getLayout(), alias);
    }

    @Override
    public int getAlignment() {
        return description.getAlignment(struct.getLayout(), alias);
    }

    @Override
    public String getName() {
        return name;
    }

    public Format[] getFormats() {
        return formats;
    }

    public Struct getBoundStruct() {
        return struct;
    }

    public int getOffset() {
        return offset;
    }

}
