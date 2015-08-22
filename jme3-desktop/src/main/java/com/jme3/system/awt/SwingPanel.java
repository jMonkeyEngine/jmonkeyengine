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
package com.jme3.system.awt;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.ImageCapabilities;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

public class SwingPanel extends JPanel implements JmePanel, SceneProcessor {

    private boolean attachAsMain = false;
    
    private BufferedImage img;
//    private FrameBuffer fb;
    private RenderManager rm;
    private PaintMode paintMode;
    private ArrayList<ViewPort> viewPorts = new ArrayList<ViewPort>(); 
    
    // Visibility/drawing vars
    private AffineTransformOp transformOp;
    private AtomicBoolean hasNativePeer = new AtomicBoolean(false);
    private AtomicBoolean showing = new AtomicBoolean(false);
    private AtomicBoolean repaintRequest = new AtomicBoolean(false);
    
    // Reshape vars
    private int newWidth  = 1;
    private int newHeight = 1;
    private AtomicBoolean reshapeNeeded  = new AtomicBoolean(true);
    private final Object lock = new Object();
    
    // Buffer pool and pending buffers
    private final int NUM_FRAMES = 2;
    private final ArrayBlockingQueue<Future<ByteBuffer>> pendingFrames = new ArrayBlockingQueue<Future<ByteBuffer>>(NUM_FRAMES);
    private final ArrayBlockingQueue<ByteBuffer> bufferPool = new ArrayBlockingQueue<ByteBuffer>(NUM_FRAMES);
    private final ArrayList<FrameBuffer> fbs = new ArrayList<FrameBuffer>(NUM_FRAMES);
    private int frameIndex = 0;
    
    private final ComponentAdapter resizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            onResize(e);
        }
    };
    
    public SwingPanel(PaintMode paintMode, boolean srgb){
        this.paintMode = paintMode;
        invalidatePendingFrames();
        addComponentListener(resizeListener);
    }
    
    public void onResize(ComponentEvent e) {
        synchronized (lock) {
            int newWidth2 = Math.max(getWidth(), 1);
            int newHeight2 = Math.max(getHeight(), 1);
            if (newWidth != newWidth2 || newHeight != newHeight2) {
                newWidth = newWidth2;
                newHeight = newHeight2;
                reshapeNeeded.set(true);
                System.out.println("EDT: componentResized " + newWidth + ", " + newHeight);
            }
        }
    }
    
    @Override
    public Component getComponent() {
        return this;
    }
    
    @Override
    public void addNotify(){
        super.addNotify();

        synchronized (lock){
            hasNativePeer.set(true);
            System.out.println("EDT: addNotify");
        }
        
        requestFocusInWindow();
    }

    @Override
    public void removeNotify(){
        synchronized (lock){
            hasNativePeer.set(false);
            System.out.println("EDT: removeNotify");
        }
        
        super.removeNotify();
    }
    
    public boolean checkVisibilityState() {
        if (!hasNativePeer.get()) {
            return false;
        }

        boolean currentShowing = isShowing();
        if (showing.getAndSet(currentShowing) != currentShowing) {
            if (currentShowing) {
                System.out.println("OGL: Enter showing state.");
            } else {
                System.out.println("OGL: Exit showing state.");
            }
        }
        return currentShowing;
    }
    
    @Override
    public void paintComponent(Graphics g){
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_SPEED);
        
        ByteBuffer byteBuf = null;
        
        synchronized (lock){
            if (pendingFrames.size() > NUM_FRAMES - 1) {
                byteBuf = acquireNextFrame();
            }

            if (byteBuf != null) { 
                // Convert the frame into the image so it can be rendered.
                Screenshots.convertScreenShot2(byteBuf.asIntBuffer(), img);

                try {
                    // return the frame back to its rightful owner.
                    bufferPool.put(byteBuf);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SwingPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        g2d.drawImage(img, transformOp, 0, 0);
    }
    
    public ByteBuffer acquireNextFrame() {
        if (pendingFrames.isEmpty()) {
            System.out.println("!!! No pending frames, returning null.");
            return null;
        }
        
        try {
            return pendingFrames.take().get();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Grabs an available buffer from the available frames pool, 
     * reads the OpenGL backbuffer into it, then adds it to the pending frames pool.
     */
    public void readNextFrame() {
        if (bufferPool.isEmpty()) {
            System.out.println("??? Too many pending frames!");
            return; // need to draw more frames ..
        }
        
        try {
            int size = fbs.get(frameIndex).getWidth() * fbs.get(frameIndex).getHeight() * 4;
            ByteBuffer byteBuf = bufferPool.take();
            byteBuf = BufferUtils.ensureLargeEnough(byteBuf, size);
            byteBuf.clear();
            
            GLRenderer renderer = (GLRenderer) rm.getRenderer();
//            Future<ByteBuffer> future = renderer.readFrameBufferLater(fbs.get(frameIndex), byteBuf);
//            if (!pendingFrames.offer(future)) {
//                throw new AssertionError();
//            }
            
            frameIndex ++;
            if (frameIndex >= NUM_FRAMES) {
                frameIndex = 0;
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public boolean isActiveDrawing() {
        return paintMode != PaintMode.OnRequest && showing.get();
    }

    public void attachTo(boolean overrideMainFramebuffer, ViewPort ... vps){
        if (viewPorts.size() > 0){
            for (ViewPort vp : viewPorts){
                vp.setOutputFrameBuffer(null);
            }
            viewPorts.get(viewPorts.size()-1).removeProcessor(this);
        }
        
        viewPorts.addAll(Arrays.asList(vps));
        viewPorts.get(viewPorts.size()-1).addProcessor(this);
        
        this.attachAsMain = overrideMainFramebuffer;
    }
    
    public void initialize(RenderManager rm, ViewPort vp) {
        if (this.rm == null){
            // First time called in OGL thread
            this.rm = rm;
//            reshapeInThread(1, 1);
        }
    }
    
    private void invalidatePendingFrames() {
        // NOTE: all pending read requests are invalid!
        for (Future<ByteBuffer> pendingRequest : pendingFrames) {
            pendingRequest.cancel(true);
        }
        pendingFrames.clear();
        bufferPool.clear();
        
        // Populate buffer pool.
        int cap = bufferPool.remainingCapacity();
        for (int i = 0; i < cap; i++) {
            bufferPool.add(BufferUtils.createByteBuffer(1 * 1 * 4));
        }
    }

    private void reshapeInThread(int width, int height) {
        invalidatePendingFrames();

        for (FrameBuffer fb : fbs) {
            fb.dispose();
        }
        
        fbs.clear();
        
        for (int i = 0; i < NUM_FRAMES; i++) {
            FrameBuffer fb = new FrameBuffer(width, height, 1);
            fb.setDepthBuffer(Image.Format.Depth);
            fb.setColorBuffer(Image.Format.RGBA8);
            fbs.add(fb);
        }
        
        synchronized (lock){
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            
            AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
            tx.translate(0, -img.getHeight());
            transformOp = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        }
        
        if (attachAsMain) {
            rm.notifyReshape(width, height);
        } else {
            for (ViewPort vp : viewPorts){
                vp.getCamera().resize(width, height, true);

                // NOTE: Hack alert. This is done ONLY for custom framebuffers.
                // Main framebuffer should use RenderManager.notifyReshape().
                for (SceneProcessor sp : vp.getProcessors()){
                    sp.reshape(vp, width, height);
                }
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return rm != null;
    }

    @Override
    public void preFrame(float tpf) {
    }

    @Override
    public void postQueue(RenderQueue rq) {
    }
    
    @Override
    public void invalidate(){
        // For "PaintMode.OnDemand" only.
        repaintRequest.set(true);
    }
    
    @Override
    public void onFrameBegin() {
        if (attachAsMain && rm != null){
            rm.getRenderer().setMainFrameBufferOverride(fbs.get(frameIndex));
        }
    }

    @Override
    public void onFrameEnd() {
        if (reshapeNeeded.getAndSet(false)) {
            reshapeInThread(newWidth, newHeight);
        } else {
            if (!checkVisibilityState()) {
                return;
            }

            switch (paintMode) {
                case Accelerated:
                case Repaint:
                    readNextFrame();
                    repaint();
                    break;
                case OnRequest:
                    if (repaintRequest.getAndSet(false)) {
                        readNextFrame();
                        repaint();
                    }
                    break;
            }
        }
    }
    
    public void postFrame(FrameBuffer out) {
    }
    
    public void reshape(ViewPort vp, int w, int h) {
    }

    public void cleanup() {
    }
}
