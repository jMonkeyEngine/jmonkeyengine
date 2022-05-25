package com.jme3.testable.impl;

import com.jme3.app.SimpleApplication;
import com.jme3.testable.Testable;

public abstract class JmeTestApp extends SimpleApplication implements Testable {

    protected volatile boolean active = false;

    @Override
    public void launch(Object userData) {
        active = true;
    }

    @Override
    public void destroy() {
        super.destroy();
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
