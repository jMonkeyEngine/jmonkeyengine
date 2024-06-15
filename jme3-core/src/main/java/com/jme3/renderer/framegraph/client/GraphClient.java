/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.client;

import com.jme3.renderer.ViewPort;

/**
 *
 * @author codex
 * @param <T>
 */
public class GraphClient <T> implements GraphSource<T>, GraphTarget<T> {
    
    private T value;
    private ViewPortFilter filter;
    
    public GraphClient() {}
    public GraphClient(T value) {
        this.value = value;
    }
    
    @Override
    public T getGraphValue(ViewPort viewPort) {
        if (filter == null || filter.confirm(viewPort)) {
            return value;
        }
        return null;
    }
    @Override
    public boolean setGraphValue(ViewPort viewPort, T value) {
        if (filter == null || filter.confirm(viewPort)) {
            this.value = value;
            return true;
        }
        return false;
    }
    
    public void setValue(T value) {
        this.value = value;
    }
    public void setFilter(ViewPortFilter filter) {
        this.filter = filter;
    }
    
    public T getValue() {
        return value;
    }
    public ViewPortFilter getFilter() {
        return filter;
    }
    
}
