package com.jme3.vulkan.scene;

import com.jme3.util.SafeArrayList;

public class NotNode extends NotSpatial {

    private SafeArrayList<NotSpatial> children = new SafeArrayList<>(NotSpatial.class);

    public void attachChild(NotSpatial child) {
        children.add(child);
        child.setParent(this);
    }

    public boolean detachChild(NotSpatial child) {
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
