package com.jme3.vulkan.mesh;

import com.jme3.math.*;
import com.jme3.util.struct.FieldDescription;
import com.jme3.util.struct.StructField;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.buffer.BufferBindingInfo;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;
import com.jme3.vulkan.formats.Format;

import java.util.HashMap;
import java.util.Map;

/**
 * A named attribute within a vertex {@link com.jme3.util.struct.Struct}, such as
 * {@link com.jme3.vulkan.mesh.attributes.CommonAttributes#Position}, describing both how
 * it is read and written on the host (via {@link #get()}/{@link #set}, like
 * {@link com.jme3.util.struct.Struct.Field}) and the device vertex format(s) it occupies
 * (via {@link #getFormats()}).
 *
 * @param <T> value type of this attribute
 */
public class VertexAttr <T> implements StructField, Cloneable {

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

    private final String name;
    private final Format[] formats;
    private final BufferBindingInfo binding = new BufferBindingInfo();

    private T alias;
    private FieldDescription<T> desc;

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
    public void setupLayout(StructLayout layout) {
        desc = layout.getFieldDescription(alias.getClass());
    }

    @Override
    public void bind(GpuBuffer buffer, int offset) {
        binding.setBuffer(buffer);
        binding.setOffset(offset);
        binding.setLimit(offset + desc.getSize());
    }

    @Override
    public void unbind() {
        binding.unbind();
    }

    @Override
    public DataBuffer cache() {
        return binding.cache();
    }

    @Override
    public int alignment() {
        return desc.getAlignment();
    }

    @Override
    public int size() {
        return desc.getSize();
    }

    /**
     * Writes {@code value} into this attribute's bound location.
     *
     * @param value value to write
     */
    public void set(T value) {
        desc.write(binding.cache(), value);
    }

    /**
     * Reads this attribute's bound location into {@code store}.
     *
     * @param store object to read into
     * @return {@code store}
     */
    public T get(T store) {
        return desc.read(binding.cache(), store);
    }

    /**
     * Reads this attribute's bound location into this attribute's own alias, the same
     * object given at construction, and returns it.
     *
     * @return this attribute's alias, updated in place
     */
    public T get() {
        return alias = desc.read(binding.cache(), alias);
    }

    /**
     * Gets this attribute's name, e.g. one of
     * {@link com.jme3.vulkan.mesh.attributes.CommonAttributes}.
     *
     * @return attribute name
     */
    public String getName() {
        return name;
    }

    public Format[] getFormats() {
        return formats;
    }

}
