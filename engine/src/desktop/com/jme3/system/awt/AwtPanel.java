package com.jme3.system.awt;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class AwtPanel extends Canvas implements SceneProcessor {

    private BufferedImage img;
    private FrameBuffer fb;
    private ByteBuffer byteBuf;
    private IntBuffer intBuf;
    private boolean activeUpdates = true;
    private RenderManager rm;
    private ArrayList<ViewPort> viewPorts = new ArrayList<ViewPort>(); 
    
    // Visibility/drawing vars
    private BufferStrategy strategy;
    private AffineTransformOp transformOp;
    private AtomicBoolean visible = new AtomicBoolean(false);
    
    // Reshape vars
    private int newWidth  = 1;
    private int newHeight = 1;
    private AtomicBoolean reshapeNeeded  = new AtomicBoolean(false);
    private final Object lock = new Object();
    
    public AwtPanel(boolean activeUpdates){
        this.activeUpdates = activeUpdates;
        
        setIgnoreRepaint(true);
        addComponentListener(new ComponentAdapter(){
            @Override
            public void componentResized(ComponentEvent e) {
                synchronized (lock){
                    int newWidth2 = Math.max(getWidth(), 1);
                    int newHeight2 = Math.max(getHeight(), 1);
                    if (newWidth != newWidth2 || newHeight != newHeight2){
                        newWidth = newWidth2;
                        newHeight = newHeight2;
                        reshapeNeeded.set(true);
                        System.out.println("EDT: componentResized " + newWidth + ", " + newHeight);
                    }
                }
            }
        });
    }
    
    @Override
    public void addNotify(){
        super.addNotify();

        synchronized (lock){
            visible.set(true);
            System.out.println("EDT: addNotify");
        }
        
        requestFocusInWindow();
    }

    @Override
    public void removeNotify(){
        synchronized (lock){
            visible.set(false);
//            strategy.dispose();
//            strategy = null;
            System.out.println("EDT: removeNotify");
        }
        
        super.removeNotify();
    }
    
    public void drawFrameInThread(){
        if (!visible.get()){
            if (strategy != null){
                strategy.dispose();
                strategy = null;
            }
            return;
        }

        if (strategy == null || strategy.contentsLost()){
            if (strategy != null){
                strategy.dispose();
            }
            try {
                createBufferStrategy(1, new BufferCapabilities(new ImageCapabilities(true), new ImageCapabilities(true), BufferCapabilities.FlipContents.UNDEFINED));
            } catch (AWTException ex) {
                ex.printStackTrace();
            }
            strategy = getBufferStrategy();
            System.out.println("OGL: BufferStrategy lost!");
        }

        Graphics2D g2d;
        
        synchronized (lock){
            g2d = (Graphics2D) strategy.getDrawGraphics();
        }
        
        g2d.drawImage(img, transformOp, 0, 0);
        g2d.dispose();
        strategy.show();        
    }
    
    public boolean isActiveUpdates() {
        return activeUpdates;
    }

    public void setActiveUpdates(boolean activeUpdates) {
        synchronized (lock){
            this.activeUpdates = activeUpdates;
        }
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
        intBuf = byteBuf.asIntBuffer();
        fb = new FrameBuffer(width, height, 1);
        fb.setDepthBuffer(Format.Depth);
        fb.setColorBuffer(Format.RGB8);
        
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
//        synchronized (lock){
//            img = (BufferedImage) getGraphicsConfiguration().createCompatibleImage(width, height);
//        }
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
        if (out != fb){
            throw new IllegalStateException("Why did you change the output framebuffer?");
        }
        
        if (reshapeNeeded.getAndSet(false)){
            reshapeInThread(newWidth, newHeight);
        }else if (activeUpdates){
            byteBuf.clear();
            rm.getRenderer().readFrameBuffer(fb, byteBuf);
            Screenshots.convertScreenShot2(intBuf, img);
            drawFrameInThread();
        }
        
    }
    
    public void reshape(ViewPort vp, int w, int h) {
    }

    public void cleanup() {
    }
}
