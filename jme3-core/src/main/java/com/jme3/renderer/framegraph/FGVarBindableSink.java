package com.jme3.renderer.framegraph;

import java.util.ArrayList;

/**
 * @author JohnKkk
 * @param <T>
 */
public class FGVarBindableSink<T extends FGVarSource.FGVarBindableProxy> extends FGContainerBindableSink<T>{
    public FGVarBindableSink(String registeredName, ArrayList container, int index) {
        super(registeredName, container, index);
    }

    @Override
    public void bind(FGSource fgSource) {
        super.bind(fgSource);
    }
}
