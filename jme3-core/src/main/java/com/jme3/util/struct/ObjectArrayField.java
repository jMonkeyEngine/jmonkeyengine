package com.jme3.util.struct;

import java.util.function.IntFunction;

public class ObjectArrayField <T> extends ArrayField<T[], T> {

    private final Class<T> type;
    private final IntFunction<T[]> factory;

    public ObjectArrayField(int size, Class<T> type, IntFunction<T[]> factory) {
        super(size);
        this.type = type;
        this.factory = factory;
    }

    public ObjectArrayField(String name, int size, Class<T> type, IntFunction<T[]> factory) {
        super(name, size);
        this.type = type;
        this.factory = factory;
    }

    public ObjectArrayField(T[] array, Class<T> type, IntFunction<T[]> factory) {
        super(array);
        this.type = type;
        this.factory = factory;
    }

    public ObjectArrayField(String name, T[] array, Class<T> type, IntFunction<T[]> factory) {
        super(name, array);
        this.type = type;
        this.factory = factory;
    }

    @Override
    protected T[] createArray(int length) {
        return factory.apply(length);
    }

    @Override
    protected FieldDescription<T> getElementDescription(StructLayout layout) {
        return layout.getFieldDescription(type);
    }

    @Override
    protected void setArrayElement(T[] array, int index, T value) {
        array[index] = value;
    }

    @Override
    protected T getArrayElement(T[] array, int index) {
        return array[index];
    }

    @Override
    protected int getArrayLength(T[] array) {
        return array.length;
    }

}
