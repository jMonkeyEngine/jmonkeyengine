package com.jme3.vulkan.scene.components;

import com.jme3.util.struct.*;
import com.jme3.vulkan.scene.Scene;

import java.util.*;
import java.util.function.Function;

public abstract class StructPipe <T extends Struct<StructField>> implements Component {

    private final Scene scene;
    private final StructList<T> array;
    private final Map<Integer, Integer> slotMapping = new HashMap<>();
    private final BitSet filledSlots = new BitSet();
    private final Collection<PropertyReceiver> receivers = new ArrayList<>();

    public StructPipe(Scene scene, StructList<T> array) {
        this.scene = scene;
        this.array = array;
    }

    protected final <E, P extends Property<E>> void map(Class<P> property, Function<T, ValueField<E>> field) {
        receivers.add(new PropertyReceiver<>(property, field).register());
    }

    private int acquireSlot(int node) {
        int slot = filledSlots.nextClearBit(0);
        filledSlots.set(slot);
        slotMapping.put(node, slot);
        array.ensureLength(slot + 1);
        for (PropertyReceiver r : receivers) {
            r.update(node);
        }
        return slot;
    }

    public T get(int node) {
        Integer slot = slotMapping.get(node);
        if (slot == null) {
            slot = acquireSlot(node);
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

    private class PropertyReceiver <E, P extends Property<E>> implements PropertyListener<E> {

        private final Class<P> type;
        private final Function<T, ValueField<E>> field;

        public PropertyReceiver(Class<P> type, Function<T, ValueField<E>> field) {
            this.type = type;
            this.field = field;
        }

        public PropertyReceiver<E, P> register() {
            scene.getComponent(type).addListener(this);
            return this;
        }

        public void update(int node) {
            onPropertyChanged(scene.getComponent(type), node);
        }

        @Override
        public void onPropertyChanged(Property<E> property, int node) {
            Integer slot = slotMapping.get(node);
            if (slot != null) {
                field.apply(array.index(slot)).set(property.get(node));
            }
        }

    }

}
