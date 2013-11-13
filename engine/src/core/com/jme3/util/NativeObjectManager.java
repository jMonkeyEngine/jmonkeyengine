/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import com.jme3.renderer.Renderer;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
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
     * Set to <code>true</code> to enable deletion of native buffers together with GL objects
     * when requested. Note that usage of object after deletion could cause undefined results
     * or native crashes, therefore by default this is set to <code>false</code>.
     */
    public static boolean UNSAFE = false;
    
    /**
     * The maximum number of objects that should be removed per frame.
     * If the limit is reached, no more objects will be removed for that frame.
     */
    private static final int MAX_REMOVES_PER_FRAME = 100;
    
    /**
     * Reference queue for {@link NativeObjectRef native object references}.
     */
    private ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>();

    /**
     * List of currently active GLObjects.
     */
    private HashMap<Long, NativeObjectRef> refMap = new HashMap<Long, NativeObjectRef>();
    
    /**
     * List of real objects requested by user for deletion.
     */
    private ArrayDeque<NativeObject> userDeletionQueue = new ArrayDeque<NativeObject>();

    private static class NativeObjectRef extends PhantomReference<Object> {
        
        private NativeObject objClone;
        private WeakReference<NativeObject> realObj;

        public NativeObjectRef(ReferenceQueue<Object> refQueue, NativeObject obj){
            super(obj.handleRef, refQueue);
            assert obj.handleRef != null;

            this.realObj = new WeakReference<NativeObject>(obj);
            this.objClone = obj.createDestructableClone();
            assert objClone.getId() == obj.getId();
        }
    }

    /**
     * (Internal use only) Register a <code>NativeObject</code> with the manager.
     */
    public void registerObject(NativeObject obj) {
        if (obj.getId() <= 0) {
            throw new IllegalArgumentException("object id must be greater than zero");
        }

        NativeObjectRef ref = new NativeObjectRef(refQueue, obj);
        refMap.put(obj.getUniqueId(), ref);
        
        obj.setNativeObjectManager(this);

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Registered: {0}", new String[]{obj.toString()});
        }
    }
    
    private void deleteNativeObject(Object rendererObject, NativeObject obj, NativeObjectRef ref, 
                                    boolean deleteGL, boolean deleteBufs) {
        assert rendererObject != null;
        
        // "obj" is considered the real object (with buffers and everything else)
        // if "ref" is null.
        NativeObject realObj = ref != null ? 
                                    ref.realObj.get() : 
                                    obj;
        
        assert realObj == null || obj.getId() == realObj.getId();
        
        if (deleteGL) {
            if (obj.getId() <= 0) {
                logger.log(Level.WARNING, "Object already deleted: {0}", obj.getClass().getSimpleName() + "/" + obj.getId());
            } else {
                // Unregister it from cleanup list.
                NativeObjectRef ref2 = refMap.remove(obj.getUniqueId());
                if (ref2 == null) {
                    throw new IllegalArgumentException("This NativeObject is not " + 
                                                       "registered in this NativeObjectManager");
                }

                assert ref == null || ref == ref2;

                int id = obj.getId();

                // Delete object from the GL driver
                obj.deleteObject(rendererObject);
                assert obj.getId() == NativeObject.INVALID_ID;

                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "Deleted: {0}", obj.getClass().getSimpleName() + "/" + id);
                }

                if (realObj != null){
                    // Note: make sure to reset them as well
                    // They may get used in a new renderer in the future
                    realObj.resetObject();
                }
            }
        }
        if (deleteBufs && UNSAFE && realObj != null) {
            // Only the real object has native buffers. 
            // The destructable clone has nothing and cannot be used in this case.
            realObj.deleteNativeBuffersInternal();
        }
    }
    
    /**
     * (Internal use only) Deletes unused NativeObjects.
     * Will delete at most {@link #MAX_REMOVES_PER_FRAME} objects.
     * 
     * @param rendererObject The renderer object. 
     * For graphics objects, {@link Renderer} is used, for audio, {#link AudioRenderer} is used.
     */
    public void deleteUnused(Object rendererObject){
        int removed = 0;
        while (removed < MAX_REMOVES_PER_FRAME && !userDeletionQueue.isEmpty()) {
            // Remove user requested objects.
            NativeObject obj = userDeletionQueue.pop();
            deleteNativeObject(rendererObject, obj, null, true, true);
            removed++;
        }
        while (removed < MAX_REMOVES_PER_FRAME) {
            // Remove objects reclaimed by GC.
            NativeObjectRef ref = (NativeObjectRef) refQueue.poll();
            if (ref == null) {
                break;
            }

            deleteNativeObject(rendererObject, ref.objClone, ref, true, false);
            removed++;
        }
        if (removed >= 1) {
            logger.log(Level.FINE, "NativeObjectManager: {0} native objects were removed from native", removed);
        }
    }

    /**
     * (Internal use only) Deletes all objects. 
     * Must only be called when display is destroyed.
     */
    public void deleteAllObjects(Object rendererObject){
        deleteUnused(rendererObject);
        ArrayList<NativeObjectRef> refMapCopy = new ArrayList<NativeObjectRef>(refMap.values());
        for (NativeObjectRef ref : refMapCopy) {
            deleteNativeObject(rendererObject, ref.objClone, ref, true, false);
        }
        assert refMap.size() == 0;
    }

    /**
     * Marks the given <code>NativeObject</code> as unused, 
     * to be deleted on the next frame. 
     * Usage of this object after deletion will cause an exception. 
     * Note that native buffers are only reclaimed if 
     * {@link #UNSAFE} is set to <code>true</code>.
     * 
     * @param obj The object to mark as unused.
     */
    void enqueueUnusedObject(NativeObject obj) {
        userDeletionQueue.push(obj);
    }
    
    /**
     * (Internal use only) Resets all {@link NativeObject}s.
     * This is typically called when the context is restarted.
     */
    public void resetObjects(){
        for (NativeObjectRef ref : refMap.values()) {
            // Must use the real object here, for this to be effective.
            NativeObject realObj = ref.realObj.get();
            if (realObj == null) {
                continue;
            }
            
            realObj.resetObject();
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Reset: {0}", realObj);
            }
        }
        refMap.clear();
        refQueue = new ReferenceQueue<Object>();
    }

//    public void printObjects(){
//        System.out.println(" ------------------- ");
//        System.out.println(" GL Object count: "+ objectList.size());
//        for (GLObject obj : objectList){
//            System.out.println(obj);
//        }
//    }
}
