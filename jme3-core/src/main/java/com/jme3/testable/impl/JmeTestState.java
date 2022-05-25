package com.jme3.testable.impl;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.testable.Testable;

public abstract class JmeTestState extends BaseAppState implements Testable {

    protected volatile boolean active = false;

    @Override
    public void launch(Object userData) {
        active = true;
    }

    @Override
    protected void cleanup(Application app) {
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }}
