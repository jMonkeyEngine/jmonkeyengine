/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.modules;

import com.jme3.renderer.RendererException;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.PassIndex;

/**
 * Container that launches renderering threads.
 * <p>
 * This module cannot be added to another container. It is designed to be
 * handled by the FrameGraph directly.
 * 
 * @author codex
 */
public class ThreadLauncher extends RenderContainer<RenderThread> {
    
    private static final long threadTimeoutMillis = 2000;
    
    private int activeThreads = 0;

    public ThreadLauncher() {}
    
    @Override
    public void prepareModuleRender(FGRenderContext context, PassIndex index) {
        super.prepareModuleRender(context, index);
        for (RenderThread t : queue) {
            t.prepareModuleRender(context, index);
            index.threadIndex++;
            index.queueIndex = 0;
        }
    }
    @Override
    public void executeRender(FGRenderContext context) {
        activeThreads = queue.size();
        for (int i = queue.size()-1; i >= 0; i--) {
            if (isInterrupted()) {
                break;
            }
            queue.get(i).startThreadExecution(context);
        }
        long start = System.currentTimeMillis();
        while (activeThreads > 0) {
            // wait for all threads to complete
            if (System.currentTimeMillis()-start > threadTimeoutMillis) {
                throw new RendererException("Timeout occured waiting for threads to complete.");
            }
        }
    }
    @Override
    protected boolean setParent(RenderContainer parent) {
        return false;
    }
    
    public void notifyThreadComplete(RenderThread thread) {
        activeThreads--;
    }
    
    public RenderThread getOrCreate(int i) {
        if (i >= queue.size()) {
            return add();
        } else if (i >= 0) {
            return queue.get(i);
        } else {
            return queue.get(PassIndex.MAIN_THREAD);
        }
    }
    public RenderThread add() {
        return add(new RenderThread());
    }
    
}
