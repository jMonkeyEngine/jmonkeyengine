package com.jme3.vulkan.scene;

public abstract class AbstractComponent implements Component {

    private final InheritMode inherit;

    public AbstractComponent(InheritMode inherit) {
        this.inherit = inherit;
    }

    @Override
    public InheritMode getInheritMode() {
        return inherit;
    }

}
