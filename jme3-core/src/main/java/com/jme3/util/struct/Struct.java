package com.jme3.util.struct;

import com.jme3.math.FastMath;
import com.jme3.vulkan.buffer.BufferBindingInfo;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Defines the layout of properties in native memory. In contrast to traditional struct patterns,
 * {@code Struct} does not directly contain any data. Rather, it dictates how the data is read from
 * and written to memory. This design allows a single Struct instance to be used to read and write
 * data from multiple memory locations instead of needing a seperate instance for each unique memory
 * location.
 *
 * <p>Struct fields are expected to be added sometime during initialization through {@link #add(StructField[])}
 * and at no other point. Adding or altering fields post-initialization can result in undefined behavior.</p>
 *
 * <p>The precise layout represented by a Struct is determined by assigning {@link FieldDescription} to each
 * member field depending on their type from a specific {@link StructLayout}. Structs (unless otherwise
 * stated by an implementation) are initialized without a StructLayout assigned. Attempting to access
 * statistics or fields of a struct without a layout results in undefined behavior.</p>
 *
 * @param <T> field type accepted by the struct
 */
public class Struct <T extends StructField> implements StructField {

    private final List<T> fields = new LinkedList<>();
    private final BufferBindingInfo binding = new BufferBindingInfo();
    private StructLayout layout;
    private int alignment, size;

    public Struct() {}

    public Struct(StructLayout layout) {
        this.layout = layout;
    }

    @Override
    public void setupLayout(StructLayout layout) {
        this.layout = layout;
        alignment = size = 0;
        for (T f : fields) {
            f.setupLayout(layout);
            alignment = Math.max(alignment, f.alignment());
            size = FastMath.toMultipleOfPow2(size, f.alignment()) + f.size();
        }
        alignment = layout.getStructAlignment(alignment);
        size = layout.getStructSize(alignment, size);
    }

    @Override
    public void bind(GpuBuffer buffer, int offset) {
        for (T f : fields) {
            f.bind(buffer, offset = FastMath.toMultipleOfPow2(offset, f.alignment()));
            offset += f.size();
        }
    }

    @Override
    public void unbind() {
        binding.unbind();
        for (T f : fields) {
            f.unbind();
        }
    }

    @Override
    public DataBuffer cache() {
        return binding.cache();
    }

    @Override
    public int alignment() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @SafeVarargs
    protected final void add(T... fields) {
        if (binding.isBound()) {
            throw new IllegalStateException("Unable to add fields while bound.");
        }
        this.fields.addAll(Arrays.asList(fields));
    }

    protected final <F extends T> F add(F field) {
        if (binding.isBound()) {
            throw new IllegalStateException("Unable to add fields while bound.");
        }
        fields.add(field);
        return field;
    }

    public List<T> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * Gets the byte offset of {@code field} within one instance of this struct, i.e. the
     * same offset {@link #bind} would pass to {@code field}'s own {@code bind} call
     * relative to this struct's own offset. Recomputed from {@link #getFields()} on every
     * call rather than cached, since a struct's field count is small and this is only
     * expected to be queried at layout-setup time, not per element or per frame.
     *
     * @param field field to find the offset of; compared by identity, not equality
     * @return byte offset of {@code field} within this struct
     * @throws IllegalArgumentException if {@code field} is not one of this struct's fields
     */
    public int getFieldOffset(T field) {
        int offset = 0;
        for (T f : fields) {
            offset = FastMath.toMultipleOfPow2(offset, f.alignment());
            if (f == field) {
                return offset;
            }
            offset += f.size();
        }
        throw new IllegalArgumentException(field + " is not a field of this struct.");
    }

    public static class Field <T> implements StructField {

        private final BufferBindingInfo binding = new BufferBindingInfo();
        private FieldDescription<T> desc;
        private T alias;

        public Field(T alias) {
            assert alias != null : "Alias cannot be null.";
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

        public void set(T value) {
            desc.write(binding.cache(), value);
        }

        public T get(T store) {
            return desc.read(binding.cache(), store);
        }

        public T get() {
            return alias = desc.read(binding.cache(), alias);
        }

    }

}
