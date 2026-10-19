package com.jme3.util.destruction;

public interface Destructable {

    ResourceLifecycle getLifecycle();

    default void destroy() {
        getLifecycle().destroy();
    }

}
