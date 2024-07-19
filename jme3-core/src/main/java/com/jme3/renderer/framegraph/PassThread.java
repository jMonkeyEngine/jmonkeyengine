/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
import java.util.Objects;
import java.util.function.Function;

/**
 * A queue of RenderPasses executing on a particular thread.
 * <p>
 * The primary PassThread runs on the main JME render thread.
 * 
 * @author codex
 */
public class PassThread implements Runnable, Iterable<RenderPass>, Savable {
    
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
    
    /**
     * Serialization only.
     */
    public PassThread() {
        frameGraph = null;
    }
    /**
     * 
     * @param frameGraph
     * @param index 
     */
    public PassThread(FrameGraph frameGraph, int index) {
        this.frameGraph = frameGraph;
        this.index = index;
    }
    
    @Override
    public void run() {
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
            run();
        } else {
            thread = new Thread(this);
            thread.start();
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
        Objects.requireNonNull(pass, "Pass to add cannot be null.");
        queue.addLast(pass);
        pass.initializePass(frameGraph, new PassIndex(index, queue.size()-1));
        return pass;
    }
    /**
     * Adds the pass at the index in the pass queue.
     * <p>
     * If the queue index is &gt;= the current queue size, the pass will
     * be added to the end of the queue. Passes above the added pass
     * will have their queue indices shifted.
     * 
     * @param <T>
     * @param pass
     * @param index
     * @return 
     */
    public <T extends RenderPass> T add(T pass, PassIndex index) {
        Objects.requireNonNull(pass, "Pass to add cannot be null.");
        int i = index.getQueueIndex();
        if (i < 0 || i >= queue.size()) {
            return add(pass);
        }
        queue.add(i, pass);
        pass.initializePass(frameGraph, new PassIndex(this.index, i));
        for (RenderPass p : queue) {
            p.shiftExecutionIndex(i, true);
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
     * Adds a series (loop) of passes at the index.
     * <p>
     * Passes in the loop are connecting the next and previous pass in the loop
     * if they exist using the ticket names given.
     * 
     * @param <T>
     * @param array
     * @param index index to start adding passes at (unaffected)
     * @param function creates render passes, or null to use what is already
     * in the array.
     * @param inTicket the input ticket for each pass to connect with the
     * previous pass in the loop
     * @param outTicket the output ticket for each pass to connect with
     * the next pass in the loop
     * @return pass array
     */
    public <T extends RenderPass> T[] addLoop(T[] array, PassIndex index,
            Function<Integer, T> function, String inTicket, String outTicket) {
        PassIndex ind = new PassIndex(index);
        for (int i = 0; i < array.length; i++) {
            if (function != null && array[i] == null) {
                array[i] = function.apply(i);
            }
            if (index.queueIndex < 0) {
                add(array[i]);
            } else {
                add(array[i], ind);
                ind.queueIndex++;
            }
            if (i > 0) {
                array[i].makeInput(array[i-1], outTicket, inTicket);
            }
        }
        return array;
    }
    
    /**
     * Gets the first pass that qualifies.
     * 
     * @param <T>
     * @param by
     * @return first qualifying pass, or null
     */
    public <T extends RenderPass> T get(PassLocator<T> by) {
        for (RenderPass p : queue) {
            T a = by.accept(p);
            if (a != null) {
                return a;
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
        PassThread.threadTimeoutMillis = threadTimeoutMillis;
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
        PassThread.threadTimeoutAttempts = threadTimeoutAttempts;
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
