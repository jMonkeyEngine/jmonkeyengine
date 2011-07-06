/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.scene.control;

import com.jme3.app.AppTask;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * Allows for enqueueing tasks onto the update loop / rendering thread.
 * 
 * Usage:
 * mySpatial.addControl(new UpdateControl()); // add it once
 * mySpatial.getControl(UpdateControl.class).enqueue(new Callable() {
 *        public Object call() throws Exception {
 *            // do stuff here
 *            return null;
 *        }
 *    });
 * 
 * @author Brent Owens
 */
public class UpdateControl extends AbstractControl {

    private final ConcurrentLinkedQueue<AppTask<?>> taskQueue = new ConcurrentLinkedQueue<AppTask<?>>();

    /**
     * Enqueues a task/callable object to execute in the jME3
     * rendering thread.
     */
    public <V> Future<V> enqueue(Callable<V> callable) {
        AppTask<V> task = new AppTask<V>(callable);
        taskQueue.add(task);
        return task;
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        AppTask<?> task = taskQueue.poll();
        toploop: do {
            if (task == null) break;
            while (task.isCancelled()) {
                task = taskQueue.poll();
                if (task == null) break toploop;
            }
            task.invoke();
        } while (((task = taskQueue.poll()) != null));
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        
    }

    public Control cloneForSpatial(Spatial newSpatial) {
        UpdateControl control = new UpdateControl(); 
        control.setSpatial(newSpatial);
        control.setEnabled(isEnabled());
        control.taskQueue.addAll(taskQueue);
        return control;
    }
    
}
