package com.jme3.vulkan.scene.components;

import java.util.ArrayList;
import java.util.Collection;

@Deprecated
public abstract class AbstractComponentType <T> implements Component<T> {

    private final Collection<Component> observers = new ArrayList<>(4);

    @Override
    public void addPipe(ComponentPipe pipe) {
        observers.add(pipe);
    }

    protected void signalChange(int node) {
        for (Component o : observers) {
            o.componentChangeObserved(this, node);
        }
    }

}
