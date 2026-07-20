package com.jme3.util.struct;

import com.jme3.math.FastMath;
import com.jme3.vulkan.buffer.DataBuffer;

public abstract class ArrayField <T, E> implements StructField<T> {

    private final String name;
    private Struct struct;
    private int offset;
    private int alignment, stride;
    private T alias;
    private FieldDescription<E> desc;

    public ArrayField(int size) {
        this(null, size);
    }

    public ArrayField(String name, int size) {
        this.name = name;
        this.alias = createArray(size);
    }

    public ArrayField(T array) {
        this(null, array);
    }

    public ArrayField(String name, T array) {
        this.name = name;
        this.alias = array;
    }

    @Override
    public int bind(Struct struct, int offset) {
        this.struct = struct;
        this.offset = offset;
        desc = getElementDescription(struct.getLayout());
        alignment = Math.max(struct.getLayout().getMinArrayAlignment(), desc.getAlignment());
        stride = FastMath.toMultipleOf(desc.getSize(), alignment);
        return offset + getArrayLength(alias) * stride;
    }

    @Override
    public void set(T value) {
        DataBuffer cache = cache();
        for (int i = 0, l = Math.min(getArrayLength(value), getArrayLength(alias)); i < l; i++) {
            E e = getArrayElement(value, i);
            if (e != null) {
                desc.write(cache, e);
            }
            cache.offset(stride);
        }
    }

    @Override
    public void alias(T value) {
        alias = value;
    }

    @Override
    public T get() {
        DataBuffer cache = cache();
        for (int i = 0, l = getArrayLength(alias); i < l; i++) {
            E e = getArrayElement(alias, i);
            if (e != null) {
                setArrayElement(alias, i, desc.read(cache, e));
            }
            cache.offset(stride);
        }
        return alias;
    }

    @Override
    public T alias() {
        return alias;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getAlignment() {
        return alignment;
    }

    @Override
    public DataBuffer cache() {
        return struct.cache().offset(offset);
    }

    @Override
    public Struct getBoundStruct() {
        return struct;
    }

    @Override
    public int capacity() {
        return getArrayLength(alias) * stride;
    }

    @Override
    public int getInternalOffset() {
        return offset;
    }

    public void set(int i, E e) {
        desc.write(cache().offset(i * stride), e);
    }

    public void alias(int i, E e) {
        setArrayElement(alias, i, e);
    }

    public E get(int i) {
        E e = getArrayElement(alias, i);
        if (e != null) {
            setArrayElement(alias, i, e = desc.read(cache().offset(i * stride), e));
        }
        return e;
    }

    public E alias(int i) {
        return getArrayElement(alias, i);
    }

    public void set(int i) {
        E e = getArrayElement(alias, i);
        if (e != null) {
            set(i, e);
        }
    }

    public void aliasAndSet(int i, E e) {
        set(i, e);
        alias(i, e);
    }

    public int length() {
        return getArrayLength(alias);
    }

    protected abstract T createArray(int length);

    protected abstract FieldDescription<E> getElementDescription(StructLayout layout);

    protected abstract void setArrayElement(T array, int index, E value);

    protected abstract E getArrayElement(T array, int index);

    protected abstract int getArrayLength(T array);

}
