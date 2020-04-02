package com.jme3.util.struct;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * StructField
 * 
 * @author Riccardo Balbo
 */
public class StructField<T> {

    private final int position;
    private T value;
    private boolean isUpdateNeeded = true;

    Field[] fieldPtr;

    private void checkField(T value){
        if(value.getClass().isArray() && value.getClass().getComponentType().isPrimitive()){
            throw new RuntimeException(this.getClass()+" doesn't support primitive arrays. Please use the object wrappers. eg. change int[] to Integer[]");
        }
    }

    public StructField(int position, T value) {
        this.position = position;        
        checkField(value);
        this.value = value;
    }

    public StructField(T value) {
        this(0, value);
    }

    public int getPosition() {
        return position;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        isUpdateNeeded = true;
        checkField(value);
        this.value = value;
    }

    public T getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }

    public boolean isUpdateNeeded() {
        return isUpdateNeeded;
    }

    public void clearUpdateNeeded() {
        isUpdateNeeded = false;
    }

    @Override
    public String toString() {
        return value.toString();
    }

}