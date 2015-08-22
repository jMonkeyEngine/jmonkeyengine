/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.renderer.opengl;

import com.jme3.renderer.RenderContext;
import com.jme3.renderer.RendererException;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Kirill Vainer
 */
final class AsyncFrameReader {
    
    private final ArrayList<PixelBuffer> pboPool = new ArrayList<PixelBuffer>();
    private final List<FrameBufferReadRequest> pending = Collections.synchronizedList(new ArrayList<FrameBufferReadRequest>());
    private final GLRenderer renderer;
    private final GL gl;
    private final GLExt glext;
    private final IntBuffer intBuf = BufferUtils.createIntBuffer(1);
    private final RenderContext context;
    private final Thread glThread;
    
    AsyncFrameReader(GLRenderer renderer, GL gl, GLExt glext, RenderContext context) {
        this.renderer = renderer;
        this.gl = gl;
        this.glext = glext;
        this.context = context;
        this.glThread = Thread.currentThread();
    }
    
    private PixelBuffer acquirePixelBuffer(int dataSize) {
        PixelBuffer pb;
        
        if (pboPool.isEmpty()) {
            // create PBO
            pb = new PixelBuffer();
            intBuf.clear();
            gl.glGenBuffers(intBuf);
            pb.id = intBuf.get(0);
        } else {
            // reuse PBO.
            pb = pboPool.remove(pboPool.size() - 1);
        }
        
        // resize or allocate PBO if required.
        if (pb.size != dataSize) {
            if (context.boundPixelPackPBO != pb.id) {
                gl.glBindBuffer(GLExt.GL_PIXEL_PACK_BUFFER_ARB, pb.id);
                context.boundPixelPackPBO = pb.id;
            }
            gl.glBufferData(GLExt.GL_PIXEL_PACK_BUFFER_ARB, dataSize, GL.GL_STREAM_READ);
        }
        
        pb.size = dataSize;
        
        return pb;
    }
    
    private void readFrameBufferFromPBO(FrameBufferReadRequest fbrr) {
        // assumes waitForCompletion was already called!
        if (context.boundPixelPackPBO != fbrr.pb.id) {
            gl.glBindBuffer(GLExt.GL_PIXEL_PACK_BUFFER_ARB, fbrr.pb.id);
            context.boundPixelPackPBO = fbrr.pb.id;
        }
        gl.glGetBufferSubData(GLExt.GL_PIXEL_PACK_BUFFER_ARB, 0, fbrr.targetBuf);
    }
    
    private boolean waitForCompletion(FrameBufferReadRequest fbrr, long time, TimeUnit unit, boolean flush) {
        int flags = flush ? GLExt.GL_SYNC_FLUSH_COMMANDS_BIT : 0;
        long nanos = unit.toNanos(time);
        switch (glext.glClientWaitSync(fbrr.fence, flags, nanos)) {
            case GLExt.GL_ALREADY_SIGNALED:
            case GLExt.GL_CONDITION_SATISFIED:
                return true;
            case GLExt.GL_TIMEOUT_EXPIRED:
                return false;
            case GLExt.GL_WAIT_FAILED:
                throw new RendererException("Waiting for fence failed");
            default:
                throw new RendererException("Unexpected result from glClientWaitSync");
        }
    }
    
    private void signalFinished(FrameBufferReadRequest fbrr) {
        fbrr.lock.lock();
        try {
            fbrr.done = true;
            fbrr.cond.signalAll();
        } finally {
            fbrr.lock.unlock();
        }
    }
    
    void signalCancelled(FrameBufferReadRequest fbrr) {
        fbrr.lock.lock();
        try {
            fbrr.cancelled = true;
            fbrr.cond.signalAll();
        } finally {
            fbrr.lock.unlock();
        }
    }
    
    public void updateReadRequests() {
        // Update requests in the order they were made (e.g. earliest first)
        for (Iterator<FrameBufferReadRequest> it = pending.iterator(); it.hasNext();) {
            FrameBufferReadRequest fbrr = it.next();
            
            // Check status for the user... (non-blocking)
            if (!fbrr.cancelled && !fbrr.done) {
                // Request a flush if we know clients are waiting 
                // (to speed up the process, or make it take finite time ..)
                boolean flush = false; // fbrr.clientsWaiting.get() > 0;
                if (waitForCompletion(fbrr, 0, TimeUnit.NANOSECONDS, flush)) {
                    if (!fbrr.cancelled) {
                        // Operation completed.
                        // Read data into user's ByteBuffer
                        readFrameBufferFromPBO(fbrr);

                        // Signal any waiting threads that we are done.
                        // Also, set the done flag.
                        signalFinished(fbrr);
                    }
                }
            }
            
            if (fbrr.cancelled || fbrr.done) {
                // Cleanup
                // Return the pixel buffer back into the pool.
                if (!pboPool.contains(fbrr.pb)) {
                    pboPool.add(fbrr.pb);
                }

                // Remove this request from the pending requests list.
                it.remove();
                
                // Get rid of the fence
                glext.glDeleteSync(fbrr.fence);

                fbrr.pb = null;
                fbrr.fence = null;
            }
        }
    }
    
    ByteBuffer getFrameBufferData(FrameBufferReadRequest fbrr, long time, TimeUnit unit) 
            throws InterruptedException, ExecutionException, TimeoutException {
        
        if (fbrr.cancelled) {
            throw new CancellationException();
        }
        
        if (fbrr.done) {
            return fbrr.targetBuf;
        }
        
        if (glThread == Thread.currentThread()) {
            // Running on GL thread, hence can use GL commands ..
            try {
                // Wait until we reach the fence..
                
                // PROBLEM: if the user is holding any locks,
                // they will not be released here,
                // causing a potential deadlock!
                if (!waitForCompletion(fbrr, time, unit, true)) {
                    throw new TimeoutException();
                }
                
                // Command stream reached this point.
                if (fbrr.cancelled) {
                    // User not interested in this anymore.
                    throw new CancellationException();
                } else {
                    // Read data into user's ByteBuffer
                    readFrameBufferFromPBO(fbrr);
                }
                
                // Mark it as done, so future get() calls always return.
                signalFinished(fbrr);
                
                return fbrr.targetBuf;
            } catch (RendererException ex) {
                throw new ExecutionException(ex);
            }
        } else {
            long nanos = unit.toNanos(time);
            
            fbrr.lock.lock();
            try {
                // Not running on GL thread, indicate that we are running
                // so GL thread can request GPU to finish quicker ...
                fbrr.clientsWaiting.getAndIncrement();
                
                // Wait until we finish
                while (!fbrr.done && !fbrr.cancelled) {
                    if (nanos <= 0L) {
                        throw new TimeoutException();
                    }

                    nanos = fbrr.cond.awaitNanos(nanos);
                }
                
                if (fbrr.cancelled) {
                    throw new CancellationException();
                }
                
                return fbrr.targetBuf;
            } finally {
                fbrr.lock.unlock();
                fbrr.clientsWaiting.getAndDecrement();
            }
        }
    }
    
    public Future<ByteBuffer> readFrameBufferLater(FrameBuffer fb, ByteBuffer byteBuf) {
        // Create & allocate a PBO (or reuse an existing one if available)
        FrameBufferReadRequest fbrr = new FrameBufferReadRequest(this);
        fbrr.targetBuf = byteBuf;
        
        int desiredSize = fb.getWidth() * fb.getHeight() * 4;
        
        if (byteBuf.remaining() != desiredSize) {
            throw new IllegalArgumentException("Ensure buffer size matches framebuffer size");
        }
        
        fbrr.pb = acquirePixelBuffer(desiredSize);
        
        // Read into PBO (asynchronous)
//        renderer.readFrameBufferWithGLFormat(fb, null, GL2.GL_BGRA, GL2.GL_UNSIGNED_BYTE, fbrr.pb.id);
        
        // Insert fence into command stream.
        fbrr.fence = glext.glFenceSync(GLExt.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        
        // Insert into FIFO
        pending.add(fbrr);
        
        return fbrr;
    }
}
