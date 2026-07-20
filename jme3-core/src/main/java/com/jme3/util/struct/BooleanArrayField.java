package com.jme3.util.struct;

public class BooleanArrayField extends ArrayField<boolean[], Boolean> {

    public BooleanArrayField(int size) {
        super(size);
    }

    public BooleanArrayField(String name, int size) {
        super(name, size);
    }

    public BooleanArrayField(boolean[] array) {
        super(array);
    }

    public BooleanArrayField(String name, boolean[] array) {
        super(name, array);
    }

    @Override
    protected boolean[] createArray(int length) {
        return new boolean[length];
    }

    @Override
    protected FieldDescription<Boolean> getElementDescription(StructLayout layout) {
        return layout.getFieldDescription(boolean.class);
    }

    @Override
    protected void setArrayElement(boolean[] array, int index, Boolean value) {
        array[index] = value;
    }

    @Override
    protected Boolean getArrayElement(boolean[] array, int index) {
        return array[index];
    }

    @Override
    protected int getArrayLength(boolean[] array) {
        return array.length;
    }

}
