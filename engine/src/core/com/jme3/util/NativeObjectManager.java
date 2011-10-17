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

package com.jme3.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GLObjectManager tracks all GLObjects used by the Renderer. Using a
 * <code>ReferenceQueue</code> the <code>GLObjectManager</code> can delete
 * unused objects from GPU when their counterparts on the CPU are no longer used.
 *
 * On restart, the renderer may request the objects to be reset, thus allowing
 * the GLObjects to re-initialize with the new display context.
 */
public class NativeObjectManager {

    private static final Logger logger = Logger.getLogger(NativeObjectManager.class.getName());

    /**
     * The queue will receive notifications of {@link NativeObject}s which are no longer
     * referenced.
     */
    private ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>();

    /**
     * List of currently active GLObjects.
     */
    private ArrayList<NativeObjectRef> refList
            = new ArrayList<NativeObjectRef>();

    private class NativeObjectRef extends PhantomReference<Object>{
        
        private NativeObject objClone;
        private WeakReference<NativeObject> realObj;

        public NativeObjectRef(NativeObject obj){
            super(obj.handleRef, refQueue);
            assert obj.handleRef != null;

            this.realObj = new WeakReference<NativeObject>(obj);
            this.objClone = obj.createDestructableClone();       
        }
    }

    /**
     * Register a GLObject with the manager.
     */
    public void registerForCleanup(NativeObject obj){
        NativeObjectRef ref = new NativeObjectRef(obj);
        refList.add(ref);
        if (logger.isLoggable(Level.FINEST))
            logger.log(Level.FINEST, "Registered: {0}", new String[]{obj.toString()});
    }

    /**
     * Deletes unused GLObjects
     */
    public void deleteUnused(Object rendererObject){
        while (true){
            NativeObjectRef ref = (NativeObjectRef) refQueue.poll();
            if (ref == null)
                return;

            refList.remove(ref);
            ref.objClone.deleteObject(rendererObject);
            if (logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "Deleted: {0}", ref.objClone);
        }
    }

    /**
     * Deletes all objects. Must only be called when display is destroyed.
     */
    public void deleteAllObjects(Object rendererObject){
        deleteUnused(rendererObject);
        for (NativeObjectRef ref : refList){
            ref.objClone.deleteObject(rendererObject);
            NativeObject realObj = ref.realObj.get();
            if (realObj != null){
                // Note: make sure to reset them as well
                // They may get used in a new renderer in the future
                realObj.resetObject();
            }
        }
        refList.clear();
    }

    /**
     * Resets all {@link NativeObject}s.
     */
    public void resetObjects(){
        for (NativeObjectRef ref : refList){
            // here we use the actual obj not the clone,
            // otherwise its useless
            NativeObject realObj = ref.realObj.get();
            if (realObj == null)
                continue;
            
            realObj.resetObject();
            if (logger.isLoggable(Level.FINEST))
                logger.log(Level.FINEST, "Reset: {0}", realObj);
        }
        refList.clear();
    }

//    public void printObjects(){
//        System.out.println(" ------------------- ");
//        System.out.println(" GL Object count: "+ objectList.size());
//        for (GLObject obj : objectList){
//            System.out.println(obj);
//        }
//    }
}
