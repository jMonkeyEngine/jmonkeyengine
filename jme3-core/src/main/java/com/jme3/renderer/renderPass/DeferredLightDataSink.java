package com.jme3.renderer.renderPass;

import com.jme3.renderer.framegraph.FGBindable;
import com.jme3.renderer.framegraph.FGContainerBindableSink;

import java.util.ArrayList;

public class DeferredLightDataSink<T extends DeferredLightDataSource.DeferredLightDataProxy> extends FGContainerBindableSink<T> {
    public DeferredLightDataSink(String registeredName, ArrayList<FGBindable> container, int index) {
        super(registeredName, container, index);
    }

    @Override
    public void postLinkValidate() {
        bLinkValidate = bindableProxy.targetBindable != null;
    }
}
