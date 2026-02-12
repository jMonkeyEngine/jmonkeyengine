package com.jme3.vulkan.mesh.attribute;

import com.jme3.vulkan.mesh.AttributeMappingInfo;

import java.nio.ByteBuffer;

public class BoneIndices extends AbstractAttribute<Integer[], Integer> {

    public BoneIndices(ValueMapper<Integer> mapper, AttributeMappingInfo info) {
        super(mapper, info);
    }

    @Override
    public void set(int element, Integer[] value) {
        ByteBuffer buf = getBuffer(element);
        for (int i = 0, l = Math.min(4, value.length); i < l; i++) {
            mapper.put(buf, value[i]);
        }
    }

    @Override
    public Integer[] get(int element) {
        return get(element, (Integer[])null);
    }

    @Override
    public Integer[] get(int element, Integer[] store) {
        if (store == null) {
            store = new Integer[4];
        }
        ByteBuffer buf = getBuffer(element);
        for (int i = 0, l = Math.min(store.length, 4); i < l; i++) {
            store[i] = mapper.get(buf);
        }
        return store;
    }

    public void set(int element, int w1, int w2, int w3, int w4) {
        ByteBuffer buf = getBuffer(element);
        mapper.put(buf, w1);
        mapper.put(buf, w2);
        mapper.put(buf, w3);
        mapper.put(buf, w4);
    }

}
