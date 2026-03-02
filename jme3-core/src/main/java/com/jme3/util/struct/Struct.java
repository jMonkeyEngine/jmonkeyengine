package com.jme3.util.struct;

import com.jme3.math.FastMath;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class Struct {

    private final List<Field> fields = new LinkedList<>();
    private StructLayout layout;
    private int size, alignment;

    public Struct() {}

    public Struct(StructLayout layout, long address) {
        read(layout, address);
    }

    public void write(StructLayout layout, long address) {
        setLayout(layout);
        for (Field f : fields) {
            f.write(layout, address);
        }
    }

    public void read(StructLayout layout, long address) {
        setLayout(layout);
        for (Field f : fields) {
            f.read(layout, address);
        }
    }

    protected void addFields(Field... fields) {
        this.fields.addAll(Arrays.asList(fields));
    }

    public void setLayout(StructLayout layout) {
        if (this.layout == layout) {
            return;
        }
        this.layout = layout;
        size = 0;
        alignment = Float.BYTES * 4;
        for (Field f : fields) {
            FieldDesc d = layout.getFieldDescription(f.get().getClass());
            int align = d.getAlignment(layout, f.get());
            f.bind(d, size = FastMath.toMultipleOf(size, align));
            size += d.getSize(layout, f.get());
            alignment = Math.max(alignment, align);
        }
        size = FastMath.toMultipleOf(size, alignment);
    }

    public StructLayout getLayout() {
        return layout;
    }

    public int getSize(StructLayout layout) {
        setLayout(layout);
        return size;
    }

    public int getAlignment(StructLayout layout) {
        setLayout(layout);
        return alignment;
    }

    public static class Field <T> {

        private T value;
        private FieldDesc<T> description;
        private int offset;

        public Field(T value) {
            assert value != null : "Struct value cannot be null.";
            this.value = value;
        }

        protected void bind(FieldDesc<T> description, int offset) {
            this.description = description;
            this.offset = offset;
        }

        protected void write(StructLayout layout, long structAddress) {
            description.write(layout, structAddress + offset, value);
        }

        protected void read(StructLayout layout, long structAddress) {
            value = description.read(layout, structAddress + offset, value);
        }

        public void set(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        public FieldDesc<T> getDescription() {
            return description;
        }

        public int getOffset() {
            return offset;
        }

    }

}
