/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.modules;

import com.jme3.renderer.framegraph.modules.RenderModule;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.PassIndex;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * @author codex
 * @param <R>
 */
public class RenderContainer <R extends RenderModule> extends RenderModule implements Iterable<R> {

    protected final ArrayList<R> queue = new ArrayList<>();
    
    @Override
    public void initModule(FrameGraph frameGraph) {
        for (RenderModule m : queue) {
            m.initializeModule(frameGraph);
        }
    }
    @Override
    public void cleanupModule(FrameGraph frameGraph) {
        for (RenderModule m : queue) {
            m.cleanupModule();
        }
    }
    @Override
    public void prepareModuleRender(FGRenderContext context, PassIndex index) {
        super.prepareModuleRender(context, index);
        for (RenderModule m : queue) {
            index.queueIndex++;
            m.prepareModuleRender(context, index);
        }
    }
    @Override
    public void prepareRender(FGRenderContext context) {}
    @Override
    public void executeRender(FGRenderContext context) {
        for (RenderModule m : queue) {
            if (isInterrupted()) {
                break;
            }
            m.executeModuleRender(context);
        }
    }
    @Override
    public void resetRender(FGRenderContext context) {
        for (RenderModule m : queue) {
            m.resetRender(context);
        }
    }
    @Override
    public void renderingComplete() {
        for (RenderModule m : queue) {
            m.renderingComplete();
        }
    }
    @Override
    public void countReferences() {
        for (RenderModule m : queue) {
            m.countReferences();
        }
    }
    @Override
    public boolean isUsed() {
        // if executing a container becomes heavy on its own, change this to
        // check isUsed() for each contained module.
        return !queue.isEmpty();
    }
    @Override
    public void interrupt() {
        super.interrupt();
        for (RenderModule m : queue) {
            m.interrupt();
        }
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        ArrayList<RenderModule> array = new ArrayList<>();
        array.addAll(queue);
        out.writeSavableArrayList(array, "queue", new ArrayList<>(0));
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        ArrayList<R> array = in.readSavableArrayList("queue", new ArrayList<>(0));
        queue.addAll(array);
    }
    @Override
    public Iterator<R> iterator() {
        return queue.iterator();
    }
    @Override
    public void traverse(Consumer<RenderModule> traverser) {
        traverser.accept(this);
        for (RenderModule m : queue) {
            m.traverse(traverser);
        }
    }
    
    public <T extends R> T add(T module, int index) {
        assert module != null : "Cannot add null module.";
        assert this != module : "Cannot add container to itself.";
        if (module.getParent() != null) {
            module.getParent().remove(module);
        }
        if (index < 0) {
            index = queue.size();
        }
        queue.add(index, module);
        if (module.setParent(this)) {
            if (isAssigned()) {
                module.initializeModule(frameGraph);
            }
            return module;
        }
        throw new IllegalArgumentException(module+" cannot be added to this container.");
    }
    public <T extends R> T add(T module) {
        return add(module, queue.size());
    }
    public <T extends R> T[] addLoop(T[] array, int start, Function<Integer, T> factory, String source, String target) {
        for (int i = 0; i < array.length; i++) {
            T module = array[i];
            if (module == null) {
                if (factory == null) {
                    throw new NullPointerException("Module to add cannot be null.");
                }
                module = array[i] = factory.apply(i);
            }
            if (start >= 0) {
                add(module, start++);
            } else {
                add(module);
            }
            if (i > 0) {
                array[i].makeInput(array[i-1], source, target);
            }
        }
        return array;
    }
    public R get(int index) {
        return queue.get(index);
    }
    public <T extends RenderModule> T get(ModuleLocator<T> by) {
        for (RenderModule m : queue) {
            T module = by.accept(m);
            if (module != null) {
                return module;
            } else if (m instanceof RenderContainer) {
                module = (T)((RenderContainer)m).get(by);
                if (module != null) {
                    return module;
                }
            }
        }
        return null;
    }
    public boolean remove(R module) {
        if (queue.remove(module)) {
            module.cleanupModule();
            return true;
        }
        return false;
    }
    public R remove(int index) {
        if (index < 0 || index >= queue.size()) {
            return null;
        }
        R m = queue.remove(index);
        m.cleanupModule();
        return m;
    }
    public void clear() {
        for (RenderModule m : queue) {
            m.cleanupModule();
        }
        queue.clear();
    }
    
    public int size() {
        return queue.size();
    }
    
}
