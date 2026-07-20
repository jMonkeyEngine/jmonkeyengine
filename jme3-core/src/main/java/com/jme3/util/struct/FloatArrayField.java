package com.jme3.util.struct;

public class FloatArrayField extends ArrayField<float[], Float> {

    public FloatArrayField(int size) {
        super(size);
    }

    public FloatArrayField(String name, int size) {
        super(name, size);
    }

    public FloatArrayField(float[] array) {
        super(array);
    }

    public FloatArrayField(String name, float[] array) {
        super(name, array);
    }

    @Override
    protected float[] createArray(int length) {
        return new float[length];
    }

    @Override
    protected FieldDescription<Float> getElementDescription(StructLayout layout) {
        return layout.getFieldDescription(float.class);
    }

    @Override
    protected void setArrayElement(float[] array, int index, Float value) {
        array[index] = value;
    }

    @Override
    protected Float getArrayElement(float[] array, int index) {
        return array[index];
    }

    @Override
    protected int getArrayLength(float[] array) {
        return array.length;
    }

}
