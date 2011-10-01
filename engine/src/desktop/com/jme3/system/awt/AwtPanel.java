package com.jme3.system.awt;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

public class AwtPanel extends Canvas implements SceneProcessor {

    private BufferedImage img;
    private FrameBuffer fb;
    private ByteBuffer byteBuf;
    private boolean activeUpdates = true;
    private RenderManager rm;
    private ArrayList<ViewPort> viewPorts = new ArrayList<ViewPort>(); 
    
    // Visibility/drawing vars
    private BufferStrategy strategy;
    private AffineTransformOp transformOp;
    private CyclicBarrier visibleBarrier = new CyclicBarrier(2);
    private AtomicBoolean visible = new AtomicBoolean(false);
    private boolean glVisible = false;
    
    // Reshape vars
    private int newWidth  = 0;
    private int newHeight = 0;
    private CyclicBarrier reshapeBarrier = new CyclicBarrier(2);
    private AtomicBoolean reshapeNeeded  = new AtomicBoolean(false);
    
    public AwtPanel(boolean activeUpdates){
        this.activeUpdates = activeUpdates;
        
        setIgnoreRepaint(true);
        addComponentListener(new ComponentAdapter(){
            @Override
            public void componentResized(ComponentEvent e) {
                newWidth = Math.max(getWidth(), 1);
                newHeight = Math.max(getHeight(), 1);
                reshapeNeeded.set(true);

                try {
                    reshapeBarrier.await();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (BrokenBarrierException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
    
    @Override
    public void addNotify(){
        super.addNotify();

        try {
            createBufferStrategy(2);
            strategy = getBufferStrategy();
            visible.set(true);
            visibleBarrier.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (BrokenBarrierException ex) {
            ex.printStackTrace();
        }
        
        requestFocusInWindow();
    }

    @Override
    public void removeNotify(){
        try {
            visible.set(false);
            visibleBarrier.await();
            strategy = null;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (BrokenBarrierException ex) {
            ex.printStackTrace();
        }
        
        super.removeNotify();
    }
    
    private void checkVisibility(){
        if (visible.get() != glVisible){
            try {
                glVisible = visible.get();
                visibleBarrier.await();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } catch (BrokenBarrierException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void drawFrame(){
        checkVisibility();
        if (!glVisible)
            return;
        
        if (strategy.contentsLost()){
            strategy.dispose();
            createBufferStrategy(2);
            strategy = getBufferStrategy();
            System.out.println("BufferStrategy lost!");
        }
        
        Graphics2D g2d = (Graphics2D) strategy.getDrawGraphics();
        g2d.drawImage(img, transformOp, 0, 0);
        g2d.dispose();
        strategy.show();
    }
    
    public boolean isActiveUpdates() {
        return activeUpdates;
    }

    public void setActiveUpdates(boolean activeUpdates) {
        this.activeUpdates = activeUpdates;
    }
    
    public void attachTo(ViewPort ... vps){
        if (viewPorts.size() > 0){
            for (ViewPort vp : viewPorts){
                vp.setOutputFrameBuffer(null);
            }
            viewPorts.get(viewPorts.size()-1).removeProcessor(this);
        }
        
        viewPorts.addAll(Arrays.asList(vps));
        viewPorts.get(viewPorts.size()-1).addProcessor(this);
    }
    
    public void initialize(RenderManager rm, ViewPort vp) {
        if (this.rm == null){
            // First time called in OGL thread
            this.rm = rm;
            reshapeInThread(1, 1);
        }
    }

    private void reshapeInThread(int width, int height) {
        byteBuf = BufferUtils.ensureLargeEnough(byteBuf, width * height * 4);
        fb = new FrameBuffer(width, height, 1);
        fb.setDepthBuffer(Format.Depth);
        fb.setColorBuffer(Format.RGB8);
        
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -img.getHeight());
        transformOp = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        
        for (ViewPort vp : viewPorts){
            vp.setOutputFrameBuffer(fb);
            vp.getCamera().resize(width, height, true);
        }
    }

    public boolean isInitialized() {
        return fb != null;
    }

    public void preFrame(float tpf) {
    }

    public void postQueue(RenderQueue rq) {
    }

    public void postFrame(FrameBuffer out) {
        if (out != fb)
            throw new IllegalStateException("Why did you change the output framebuffer?");
        
        if (reshapeNeeded.getAndSet(false)){
            reshapeInThread(newWidth, newHeight);
            try {
                reshapeBarrier.await();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } catch (BrokenBarrierException ex) {
                ex.printStackTrace();
            }
        }else if (activeUpdates){
            byteBuf.clear();
            rm.getRenderer().readFrameBuffer(fb, byteBuf);
            Screenshots.convertScreenShot2(byteBuf, img);
            drawFrame();
        }else{
            checkVisibility();
        }
    }
    
    public void reshape(ViewPort vp, int w, int h) {
    }

    public void cleanup() {
    }
}
