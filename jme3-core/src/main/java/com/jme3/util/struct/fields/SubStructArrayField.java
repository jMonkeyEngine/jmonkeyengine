package com.jme3.util.struct.fields;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;

public class SubStructArrayField<T extends Struct> extends StructField<T[]> {

    public SubStructArrayField(int position, String name, T[] value) {
        super(position, name, value);
        initializeToZero((Class<? extends T>) value[0].getClass());
    }

    public SubStructArrayField(int position, String name, int length, Class<? extends T> structClass) {
        super(position, name, (T[]) Array.newInstance(structClass, length));
        initializeToZero(structClass);
    }

    private void initializeToZero(Class<? extends T> structClass) {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) try {
                Constructor<? extends T> constructor = structClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                value[i] = constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Can't create new instance of " + structClass + " default constructor is missing? ",e);
            }
        }
    }

}
