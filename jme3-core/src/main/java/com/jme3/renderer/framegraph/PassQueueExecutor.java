/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.profile.FgStep;
import com.jme3.renderer.framegraph.passes.Attribute;
import com.jme3.renderer.framegraph.passes.RenderPass;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author codex
 */
public class PassQueueExecutor implements Runnable, Iterable<RenderPass>, Savable {
    
    private static long threadTimeoutMillis = 5000;
    private static final ArrayList<RenderPass> DEF_QUEUE = new ArrayList<>(0);
    
    private final FrameGraph frameGraph;
    private final LinkedList<RenderPass> queue = new LinkedList<>();
    private int index;
    private Thread thread;
    private FGRenderContext context;
    private boolean complete = false;
    private boolean interrupted = false;

    public PassQueueExecutor(FrameGraph frameGraph, int index) {
        this.frameGraph = frameGraph;
        this.index = index;
    }
    
    @Override
    public void run() {
        try {
            execute();
        } catch (Exception ex) {
            frameGraph.interruptRendering(ex);
        }
    }
    @Override
    public Iterator<RenderPass> iterator() {
        return queue.iterator();
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        ArrayList<RenderPass> list = new ArrayList<>(queue.size());
        list.addAll(queue);
        out.writeSavableArrayList(list, "queue", DEF_QUEUE);
        out.write(index, "index", 0);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        queue.addAll(in.readSavableArrayList("queue", DEF_QUEUE));
        index = in.readInt("index", 0);
    }
    
    public void execute(FGRenderContext context) {
        complete = false;
        this.context = context;
        if (index == FrameGraph.RENDER_THREAD) {
            execute();
        } else {
            thread = new Thread(this);
            thread.start();
        }
    }
    @SuppressWarnings("UseSpecificCatch")
    private void execute() {
        try {
            for (RenderPass p : queue) {
                if (interrupted) {
                    return;
                }
                if (!p.isUsed()) {
                    continue;
                }
                long start = System.currentTimeMillis();
                if (index == FrameGraph.RENDER_THREAD) {
                    if (context.isProfilerAvailable()) {
                        context.getProfiler().fgStep(FgStep.Execute, p.getProfilerName());
                    }
                    if (context.isGraphCaptureActive()) {
                        context.getGraphCapture().executeRenderPass(p.getIndex(), p.getProfilerName());
                    }
                }
                if (frameGraph.isAsync()) {
                    // wait until all input resources are available for use before executing
                    p.waitForInputs();
                }
                p.executeRender(context);
                long end = System.currentTimeMillis();
                if (index == FrameGraph.RENDER_THREAD) {
                    context.popRenderSettings();
                }
            }
            complete = true;
        } catch (Exception ex) {
            frameGraph.interruptRendering(ex);
        }
    }
    
    public void interrupt() {
        interrupted = true;
    }
    
    /**
     * Adds the pass to end of the pass queue.
     * 
     * @param <T>
     * @param pass
     * @return given pass
     */
    public <T extends RenderPass> T add(T pass) {
        queue.addLast(pass);
        pass.initializePass(frameGraph, new PassIndex(index, queue.size()-1));
        return pass;
    }
    /**
     * Adds the pass at the index in the pass queue.
     * <p>
     * If the index is &gt;= the current queue size, the pass will
     * be added to the end of the queue. Passes above the added pass
     * will have their indexes shifted.
     * 
     * @param <T>
     * @param pass
     * @param index
     * @return 
     */
    public <T extends RenderPass> T add(T pass, int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index cannot be negative.");
        }
        if (index >= queue.size()) {
            return add(pass);
        }
        queue.add(index, pass);
        pass.initializePass(frameGraph, new PassIndex(this.index, index));
        for (RenderPass p : queue) {
            p.shiftExecutionIndex(index, true);
        }
        return pass;
    }
    /**
     * Creates and adds an Attribute pass and links it to the given ticket.
     * <p>
     * This is handy for quickly debugging various resources in the graph.
     * 
     * @param <T>
     * @param ticket ticket to reference from
     * @return created Attribute
     */
    public <T> Attribute<T> addAttribute(ResourceTicket<T> ticket) {
        Attribute<T> attr = add(new Attribute<>());
        attr.getInput(Attribute.INPUT).setSource(ticket);
        return attr;
    }
    
    /**
     * Gets the first pass that is of or a subclass of the given class.
     * 
     * @param <T>
     * @param type
     * @return first qualifying pass, or null
     */
    public <T extends RenderPass> T get(Class<T> type) {
        for (RenderPass p : queue) {
            if (type.isAssignableFrom(p.getClass())) {
                return (T)p;
            }
        }
        return null;
    }
    /**
     * Gets the first pass of the given class that is named as given.
     * 
     * @param <T>
     * @param type
     * @param name
     * @return first qualifying pass, or null
     */
    public <T extends RenderPass> T get(Class<T> type, String name) {
        for (RenderPass p : queue) {
            if (name.equals(p.getName()) && type.isAssignableFrom(p.getClass())) {
                return (T)p;
            }
        }
        return null;
    }
    /**
     * Gets the pass that holds the given id number.
     * 
     * @param <T>
     * @param type
     * @param id
     * @return pass of the id, or null
     */
    public <T extends RenderPass> T get(Class<T> type, int id) {
        for (RenderPass p : queue) {
            if (id == p.getId() && type.isAssignableFrom(p.getClass())) {
                return (T)p;
            }
        }
        return null;
    }
    
    /**
     * Removes the pass at the index in the queue.
     * <p>
     * Passes above the removed pass will have their indexes shifted.
     * 
     * @param i
     * @return removed pass
     * @throws IndexOutOfBoundsException if the index is less than zero or &gt;= the queue size
     */
    public RenderPass remove(int i) {
        if (i < 0 || i >= queue.size()) {
            throw new IndexOutOfBoundsException("Index "+i+" is out of bounds for size "+queue.size());
        }
        int j = 0;
        RenderPass removed = null;
        for (Iterator<RenderPass> it = queue.iterator(); it.hasNext();) {
            RenderPass p = it.next();
            if (removed != null) {
                p.disconnectInputsFrom(removed);
                p.shiftExecutionIndex(i, false);
            } else if (j++ == i) {
                removed = p;
                it.remove();
            }
        }
        if (removed != null) {
            removed.cleanupPass(frameGraph);
        }
        return removed;
    }
    /**
     * Removes the given pass from the queue.
     * <p>
     * Passes above the removed pass will have their indexes shifted.
     * 
     * @param pass
     * @return true if the pass was removed from the queue
     */
    public boolean remove(RenderPass pass) {
        int i = 0;
        boolean found = false;
        for (Iterator<RenderPass> it = queue.iterator(); it.hasNext();) {
            RenderPass p = it.next();
            if (found) {
                // shift execution indices down
                p.disconnectInputsFrom(pass);
                p.shiftExecutionIndex(i, false);
                continue;
            }
            if (p == pass) {
                it.remove();
                found = true;
            }
            i++;
        }
        if (found) {
            pass.cleanupPass(frameGraph);
            return true;
        }
        return false;
    }
    /**
     * Clears all passes from the pass queue.
     */
    public void clear() {
        for (RenderPass p : queue) {
            p.cleanupPass(frameGraph);
        }
        queue.clear();
    }
    
    public int shiftIndex(int i, boolean pos) {
        if (index > i) {
            index += pos ? 1 : -1;
        }
        return index;
    }
    
    public int size() {
        return queue.size();
    }
    public int getIndex() {
        return index;
    }
    public boolean isAsync() {
        return index != FrameGraph.RENDER_THREAD;
    }
    public boolean isComplete() {
        return complete;
    }
    
    /**
     * Sets the duration, in milliseconds, that executors will wait
     * for pass inputs to be available before aborting execution.
     * <p>
     * Timeouts can only occur when multiple threads are running.
     * <p>
     * default=5000 (5 seconds)
     * 
     * @param threadTimeoutMillis 
     */
    public static void setThreadTimeoutMillis(long threadTimeoutMillis) {
        PassQueueExecutor.threadTimeoutMillis = threadTimeoutMillis;
    }
    public static long getThreadTimeoutMillis() {
        return threadTimeoutMillis;
    }
    
}
