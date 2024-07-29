/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.modules;

import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.modules.RenderContainer;
import com.jme3.renderer.framegraph.modules.RenderModule;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author codex
 */
public class RenderThread extends RenderContainer<RenderModule> implements Runnable {
    
    private static final Logger LOG = Logger.getLogger(RenderThread.class.getName());
    
    private FGRenderContext context;
    
    @Override
    public void run() {
        try {
            executeModuleRender(context);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "An exception occured while executing RenderThread at index "+index.threadIndex+'.', ex);
            frameGraph.interruptRendering();
        } finally {
            frameGraph.notifyThreadComplete(this);
        }
    }
    
    /**
     * Starts running the group of modules contained by this RenderThread.
     * <p>
     * If the thread index of this module is asynchronous (index !=
     * {@link PassIndex#MAIN_THREAD}), a new thread is spawned for this
     * to execute on.
     * <p>
     * All exceptions that occur under this module are caught and 
     * the FrameGraph notified to ensure graceful interruption of other threads.
     * 
     * @param context 
     */
    public void startThreadExecution(FGRenderContext context) {
        this.context = context;
        if (isInterrupted()) {
            frameGraph.notifyThreadComplete(this);
            return;
        }
        if (!isAsync()) {
            run();
        } else {
            Thread t = new Thread(this);
            t.start();
        }
    }
    
}
