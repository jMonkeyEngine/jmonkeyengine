package com.jme3.vulkan.scene;

import com.jme3.util.SafeArrayList;

public class Node extends Spatial {

    private SafeArrayList<Spatial> children = new SafeArrayList<>(Spatial.class);

    public void attachChild(Spatial child) {
        children.add(child);
        child.setParent(this);
    }

    public boolean detachChild(Spatial child) {
        if (children.remove(child)) {
            child.setParent(null);
            return true;
        }
        return false;
    }

    @Override
    protected void findNextIteration(GraphIterator iterator) {
        int i = iterator.advanceIndex();
        if (i >= children.size()) {
            iterator.moveUp();
        } else {
            iterator.moveDown(children.get(i));
        }
    }

}
