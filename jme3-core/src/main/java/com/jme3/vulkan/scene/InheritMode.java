package com.jme3.vulkan.scene;

public enum InheritMode {

    /**
     * Pulls the component of the same type of the parent node, if not null.
     */
    Pull(1, false),

    /**
     * Pushes this component to descendents.
     */
    Push(2, false),

    /**
     * Pushes this component to descendents. Pushes from parent components are ignored.
     */
    PushIgnoreParent(2, true),

    /**
     * No inheritance, but can be {@link InheritMode#Push pushed} to.
     */
    Off(0, false),

    /**
     * No inheritance. Pushes from parent components are ignored.
     */
    OffIgnoreParent(0, true);

    private static final byte OFF = 0x0;
    private static final byte PULL = 0x1;
    private static final byte PUSH = 0x2;

    private final byte mode;
    private final boolean ignoreParent;

    InheritMode(int mode, boolean ignoreParent) {
        this.mode = (byte)mode;
        this.ignoreParent = ignoreParent;
    }

    public boolean isOff() {
        return mode == OFF;
    }

    public boolean isPull() {
        return mode == PULL;
    }

    public boolean isPush() {
        return mode == PUSH;
    }

    public boolean isIgnoreParent() {
        return ignoreParent;
    }

}
