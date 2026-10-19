package com.jme3.vulkan.scene.components;

import com.jme3.util.struct.*;
import com.jme3.vulkan.scene.Scene;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public abstract class StructPipe <T extends Struct<StructField>> implements Component {

    private final Scene scene;
    private final StructList<T> array;
    private final Map<Integer, Integer> slotMapping = new HashMap<>();
    private final BitSet filledSlots = new BitSet();

    public StructPipe(Scene scene, StructList<T> array) {
        this.scene = scene;
        this.array = array;
    }

    public T get(int node) {
        Integer slot = slotMapping.get(node);
        if (slot == null) {
            slot = filledSlots.nextClearBit(0);
            filledSlots.set(slot);
            slotMapping.put(node, slot);
            array.ensureLength(slot + 1);
        }
        return array.index(slot);
    }

    @Override
    public void onNodeDestroyed(int node) {
        Integer slot = slotMapping.remove(node);
        if (slot != null) {
            filledSlots.clear(slot);
        }
    }

}
