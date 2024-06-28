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
    private static int threadTimeoutAttempts = 100;
    private static final ArrayList<RenderPass> DEF_QUEUE = new ArrayList<>(0);
    
    private final FrameGraph frameGraph;
    private final LinkedList<RenderPass> queue = new LinkedList<>();
    private int index;
    private Thread thread;
    private FGRenderContext context;
    private boolean async = false;
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
    
    /**
     * Executes this queue with the context.
     * 
     * @param context 
     */
    public void execute(FGRenderContext context) {
        async = frameGraph.isAsync();
        this.context = context;
        if (isMainThread()) {
            execute();
        } else {
            thread = new Thread(this);
            thread.start();
        }
    }
    private void execute() {
        try {
            boolean main = isMainThread();
            for (RenderPass p : queue) {
                if (interrupted) {
                    return;
                }
                if (!p.isUsed()) {
                    continue;
                }
                if (main) {
                    if (context.isProfilerAvailable()) {
                        context.getProfiler().fgStep(FgStep.Execute, p.getProfilerName());
                    }
                    if (context.isGraphCaptureActive()) {
                        context.getGraphCapture().executeRenderPass(p.getIndex(), p.getProfilerName());
                    }
                }
                if (async) {
                    // wait until all input resources are available for use before executing
                    p.waitForInputs(threadTimeoutMillis, threadTimeoutAttempts);
                }
                p.executeRender(context);
                if (main) {
                    context.popRenderSettings();
                }
            }
            frameGraph.notifyComplete(this);
        } catch (Exception ex) {
            if (!interrupted) {
                frameGraph.interruptRendering(ex);
            }
        }
    }
    
    /**
     * Notifies the queue that all other queues have completed execution.
     */
    public void notifyLast() {
        async = false;
    }
    /**
     * Interrupts this queue from executing the next pass.
     */
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
    
    /**
     * Shifts the thread index if the thread index is above {@code i}.
     * 
     * @param i
     * @param pos
     * @return 
     */
    public int shiftThreadIndex(int i, boolean pos) {
        if (index > i) {
            index += pos ? 1 : -1;
        }
        for (RenderPass p : queue) {
            p.getIndex().shiftThread(i, pos);
        }
        return index;
    }
    
    /**
     * Gets the number of passes in this queue.
     * 
     * @return 
     */
    public int size() {
        return queue.size();
    }
    /**
     * Gets the thread index of this queue.
     * 
     * @return 
     */
    public int getIndex() {
        return index;
    }
    /**
     * Returns true if this queue is running on the main render thread.
     * 
     * @return 
     */
    public boolean isMainThread() {
        return index == FrameGraph.RENDER_THREAD;
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
    /**
     * Sets the maximum number of attempts executors will make
     * for pass inputs to be available before aboring execution.
     * <p>
     * default=100
     * 
     * @param threadTimeoutAttempts 
     */
    public static void setThreadTimeoutAttempts(int threadTimeoutAttempts) {
        PassQueueExecutor.threadTimeoutAttempts = threadTimeoutAttempts;
    }
    
    /**
     * 
     * @return 
     */
    public static long getThreadTimeoutMillis() {
        return threadTimeoutMillis;
    }
    /**
     * 
     * @return 
     */
    public static int getThreadTimeoutAttempts() {
        return threadTimeoutAttempts;
    }
    
}
