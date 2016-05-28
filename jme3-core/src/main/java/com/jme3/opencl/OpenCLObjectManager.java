/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.opencl;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shaman
 */
public class OpenCLObjectManager {
    private static final Logger LOG = Logger.getLogger(OpenCLObjectManager.class.getName());
    private static final Level LOG_LEVEL1 = Level.FINER;
    private static final Level LOG_LEVEL2 = Level.FINE;
    /**
     * Call Runtime.getRuntime().gc() every these frames
     */
    private static final int GC_FREQUENCY = 10;
    
    private static final OpenCLObjectManager INSTANCE = new OpenCLObjectManager();
    private OpenCLObjectManager() {}
    
    public static OpenCLObjectManager getInstance() {
        return INSTANCE;
    }
    
    private ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>();
    private HashSet<OpenCLObjectRef> activeObjects = new HashSet<OpenCLObjectRef>();
    private int gcCounter = 0;
    
    private static class OpenCLObjectRef extends PhantomReference<Object> {
        
        private OpenCLObject.ObjectReleaser releaser;

        public OpenCLObjectRef(ReferenceQueue<Object> refQueue, OpenCLObject obj){
            super(obj, refQueue);
            releaser = obj.getReleaser();
        }
    }
    
    public void registerObject(OpenCLObject obj) {
        OpenCLObjectRef ref = new OpenCLObjectRef(refQueue, obj);
        activeObjects.add(ref);
        LOG.log(LOG_LEVEL1, "registered OpenCL object: {0}", obj);
    }
    
    private void deleteObject(OpenCLObjectRef ref) {
        LOG.log(LOG_LEVEL1, "deleting OpenCL object by: {0}", ref.releaser);
        ref.releaser.release();
        activeObjects.remove(ref);
    }
        
    public void deleteUnusedObjects() {
        if (activeObjects.isEmpty()) {
            LOG.log(LOG_LEVEL2, "no active natives");
            return; //nothing to do
        }
        
        gcCounter++;
        if (gcCounter >= GC_FREQUENCY) {
            //The program is that the OpenCLObjects are so small that they are 
            //enqueued for finalization very late. Therefore, without this
            //hack, we are running out of host memory on the OpenCL side quickly.
            gcCounter = 0;
            Runtime.getRuntime().gc();
        }
        
        int removed = 0;
        while (true) {
            // Remove objects reclaimed by GC.
            OpenCLObjectRef ref = (OpenCLObjectRef) refQueue.poll();
            if (ref == null) {
                break;
            }
            deleteObject(ref);
            removed++;
        }
        if (removed >= 1) {
            LOG.log(LOG_LEVEL2, "{0} native objects were removed from native", removed);
        }
    }
    
    public void deleteAllObjects() {
        for (OpenCLObjectRef ref : activeObjects) {
            LOG.log(LOG_LEVEL1, "deleting OpenCL object by: {0}", ref.releaser);
            ref.releaser.release();
        }
        activeObjects.clear();
    }
}
