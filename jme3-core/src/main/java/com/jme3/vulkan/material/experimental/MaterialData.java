package com.jme3.vulkan.material.experimental;

import com.jme3.util.struct.Struct;

import java.util.HashMap;
import java.util.Map;

public class MaterialData {

    private final Map<Class, SlottedBuffer<? extends Struct>> data = new HashMap<>();

    public <T extends Struct> void initDataType(Class<T> type, SlottedBuffer<T> buffer) {
        data.put(type, buffer);
    }

    public <T extends Struct> SlottedBuffer<T> getDataType(Class<T> type) {
        //noinspection unchecked
        return (SlottedBuffer<T>)data.get(type);
    }

}
