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

package com.jme3.asset;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * <code>ThreadingManager</code> manages the threads used to load content
 * within the Content Manager system. A pool of threads and a task queue
 * is used to load resource data and perform I/O while the application's
 * render thread is active. 
 */
public class ThreadingManager {

    protected final ExecutorService executor =
            Executors.newFixedThreadPool(2,
                                         new LoadingThreadFactory());

    protected final AssetManager owner;

    protected int nextThreadId = 0;

    public ThreadingManager(AssetManager owner){
        this.owner = owner;
    }

    protected class LoadingThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "pool" + (nextThreadId++));
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    }

    protected class LoadingTask implements Callable<Object> {
        private final String resourceName;
        public LoadingTask(String resourceName){
            this.resourceName = resourceName;
        }
        public Object call() throws Exception {
            return owner.loadAsset(new AssetKey(resourceName));
        }
    }

//    protected class MultiLoadingTask implements Callable<Void> {
//        private final String[] resourceNames;
//        public MultiLoadingTask(String[] resourceNames){
//            this.resourceNames = resourceNames;
//        }
//        public Void call(){
//            owner.loadContents(resourceNames);
//            return null;
//        }
//    }

//    public Future<Void> loadContents(String ... names){
//        return executor.submit(new MultiLoadingTask(names));
//    }

//    public Future<Object> loadContent(String name) {
//        return executor.submit(new LoadingTask(name));
//    }

    public static boolean isLoadingThread() {
        return Thread.currentThread().getName().startsWith("pool");
    }


}
