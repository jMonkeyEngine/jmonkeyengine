package com.jme3.util.struct;

public class IntArrayField extends ArrayField<int[], Integer> {

    public IntArrayField(int size) {
        super(size);
    }

    public IntArrayField(String name, int size) {
        super(name, size);
    }

    public IntArrayField(int[] array) {
        super(array);
    }

    public IntArrayField(String name, int[] array) {
        super(name, array);
    }

    @Override
    protected int[] createArray(int length) {
        return new int[length];
    }

    @Override
    protected FieldDescription<Integer> getElementDescription(StructLayout layout) {
        return layout.getFieldDescription(int.class);
    }

    @Override
    protected void setArrayElement(int[] array, int index, Integer value) {
        array[index] = value;
    }

    @Override
    protected Integer getArrayElement(int[] array, int index) {
        return array[index];
    }

    @Override
    protected int getArrayLength(int[] array) {
        return array.length;
    }

}
