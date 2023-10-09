/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.ArrayList;

/**
 *
 * @author JohnKkk
 */
public class FGContainerBindableSink<T extends FGBindable> extends FGSink{
    protected boolean linked = false;
    protected ArrayList<FGBindable> container;
    protected int index;
    protected FGBindableProxy bindableProxy;

    public final static class FGBindableProxy extends FGBindable{
        public FGBindable targetBindable;

        public FGBindableProxy(FGBindable targetBindable) {
            this.targetBindable = targetBindable;
        }

        @Override
        public void bind(FGRenderContext renderContext) {
            if(targetBindable != null){
                targetBindable.bind(renderContext);
            }
        }
    }

    public FGContainerBindableSink(String registeredName, ArrayList<FGBindable> container, int index) {
        super(registeredName);
        this.container = container;
        this.index = index;
        bindableProxy = new FGBindableProxy(null);
        if(index < this.container.size()){
            this.container.set(index, bindableProxy);
        }
        else{
            this.container.add(bindableProxy);
            this.index = this.container.size() - 1;
        }
    }

    @Override
    public void bind(FGSource fgSource) {
        T p = (T)fgSource.yieldBindable();
        if(p == null){
            System.err.println("Binding input [" + getRegisteredName() + "] to output [" + getLinkPassName() + "." + getLinkPassResName() + "] " + " { " + fgSource.getName() + " } ");
            return;
        }
        bindableProxy.targetBindable = p;
//        container.set(index, p);
        linked = true;
    }

    @Override
    public void postLinkValidate() {
        if(!linked){
            if(bIsRequired)
                System.err.println("Unlinked input: " + getRegisteredName());
        }
        else{
            bLinkValidate = true;
        }
    }

    @Override
    public FGBindable getBind() {
        return bindableProxy.targetBindable;
    }
}
