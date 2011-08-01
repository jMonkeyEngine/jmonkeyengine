/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * ScenePanel.java
 *
 * Created on 06.11.2010, 02:25:02
 */
package com.jme3.gde.core.scene;

import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author normenhansen
 */
public class OffScenePanel extends javax.swing.JPanel implements SceneProcessor {

    private int width = 640, height = 480;
    private ByteBuffer cpuBuf;
//    private byte[] cpuArray;
    private Node rootNode = new Node("Root Node");
    private FrameBuffer offBuffer;
    private ViewPort viewPort;
    private Camera camera;
    private RenderManager rm;
    //AWT image
    private final Object imageLock = new Object();
    private BufferedImage image;
    private AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
    private AffineTransformOp op;
    //camera
    protected Quaternion rot = new Quaternion();
    protected Vector3f vector = new Vector3f();
    protected Vector3f focus = new Vector3f();
    protected PointLight light;

    public OffScenePanel() {
        this(640, 480);
    }

    /** Creates new form ScenePanel */
    public OffScenePanel(int width, int height) {
        this.width = width;
        this.height = height;
        initComponents();
    }

    public void resizeGLView(final int x, final int y) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                width = x;
                height = y;
                if (viewPort != null) {
                    synchronized (imageLock) {
                        setupOffBuffer();
                    }
                }
                return null;
            }
        });
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                Dimension dim = new Dimension(x, y);
                setPreferredSize(dim);
                validate();
            }
        });
    }

    public void startPreview() {
//        java.awt.EventQueue.invokeLater(new Runnable() {
//
//            public void run() {
//                if(!SceneViewerTopComponent.findInstance().isOpened()){
//                    SceneViewerTopComponent.findInstance().open();
//                }
//            }
//        });
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                setupOffView();
                setupOffBuffer();
                setupScene();
                return null;
            }
        });
    }

    public void stopPreview() {
        //TODO add your handling code here:
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                SceneApplication.getApplication().getRenderManager().removePreView(viewPort);
                return null;
            }
        });
        Logger.getLogger(OffScenePanel.class.getName()).log(Level.INFO, "Component hidden");
    }

    private void setupScene() {
        //setup framebuffer's cam
        camera.setFrustumPerspective(45f, 1f, 1f, 1000f);
        camera.setLocation(new Vector3f(5f, 5f, 5f));
        camera.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);

        // setup framebuffer's scene
        light = new PointLight();
        light.setPosition(camera.getLocation());
        light.setColor(ColorRGBA.White);
        rootNode.addLight(light);
        // attach the scene to the viewport to be rendered
        viewPort.attachScene(rootNode);
    }

    private void setupOffBuffer() {
        image = new BufferedImage(width, height,
                BufferedImage.TYPE_4BYTE_ABGR);
        cpuBuf = BufferUtils.createByteBuffer(width * height * 4);
//        cpuArray = new byte[width * height * 4];
        offBuffer = new FrameBuffer(width, height, 0);
        //setup framebuffer to use texture
        offBuffer.setDepthBuffer(Format.Depth);
        offBuffer.setColorBuffer(Format.RGBA8);
        //set viewport to render to offscreen framebuffer
        viewPort.setOutputFrameBuffer(offBuffer);
        camera.resize(width, height, false);
    }

    private void setupOffView() {
        camera = new Camera(width, height);
        // create a pre-view. a view that is rendered before the main view
        viewPort = SceneApplication.getApplication().getRenderManager().createPreView("Offscreen View", camera);
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        viewPort.setClearFlags(true, true, true);
        viewPort.addProcessor(this);
    }

    public void initialize(RenderManager rm, ViewPort vp) {
        this.rm = rm;
    }

    public void reshape(ViewPort vp, int i, int i1) {
    }

    public boolean isInitialized() {
        return true;
    }

    public void preFrame(float f) {
        light.setPosition(camera.getLocation());
        rootNode.updateLogicalState(f);
        rootNode.updateGeometricState();
    }

    public void postQueue(RenderQueue rq) {
    }

    public void postFrame(FrameBuffer fb) {
//        cpuBuf.clear();
        SceneApplication.getApplication().getRenderer().readFrameBuffer(offBuffer, cpuBuf);
//
//        // copy native memory to java memory
//        cpuBuf.clear();
//        cpuBuf.get(cpuArray);
//        cpuBuf.clear();
//
//        // flip the components the way AWT likes them
//        for (int i = 0; i < width * height * 4; i += 4) {
//            byte b = cpuArray[i + 0];
//            byte g = cpuArray[i + 1];
//            byte r = cpuArray[i + 2];
//            byte a = cpuArray[i + 3];
//
//            cpuArray[i + 0] = a;
//            cpuArray[i + 1] = b;
//            cpuArray[i + 2] = g;
//            cpuArray[i + 3] = r;
//        }

        synchronized (imageLock) {
            Screenshots.convertScreenShot(cpuBuf, image);
//            WritableRaster wr = image.getRaster();
//            DataBufferByte db = (DataBufferByte) wr.getDataBuffer();
//            System.arraycopy(cpuArray, 0, db.getData(), 0, cpuArray.length);
        }
        repaint();
    }

    public void cleanup() {
    }

    @Override
    public void paintComponent(Graphics gfx) {
        super.paintComponent(gfx);
        Graphics2D g2d = (Graphics2D) gfx;
        synchronized (imageLock) {
            if (image != null) {
                tx.translate(0, -image.getHeight());
                if (op == null) {
                    op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                }
                g2d.drawImage(image, null, 0, 0);
            }
        }
    }

    public Node getRootNode() {
        return rootNode;
    }

    public Camera getCamera() {
        return camera;
    }

    public ViewPort getViewPort() {
        return viewPort;
    }

    /**
     * threadsafe attach to root node
     * @param spat
     */
    public void attach(final Spatial spat) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                rootNode.attachChild(spat);
                return null;
            }
        });
    }

    /**
     * threadsafe detach from root node
     * @param spat
     */
    public void detach(final Spatial spat) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                rootNode.detachChild(spat);
                return null;
            }
        });
    }

    public void setCamFocus(final Vector3f focus) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doSetCamFocus(focus);
                return null;
            }
        });

    }

    public void doSetCamFocus(final Vector3f focus_) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                focus.set(focus_);
                camera.setLocation(focus_.add(vector, camera.getLocation()));
                return null;
            }
        });
    }

    /*
     * methods to move camera (threadsafe)
     */
    public void rotateCamera(final Vector3f axis, final float amount_) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                float amount = amount_;
                if (axis.equals(camera.getLeft())) {
                    float elevation = -FastMath.asin(camera.getDirection().y);
                    amount = Math.min(Math.max(elevation + amount,
                            -FastMath.HALF_PI), FastMath.HALF_PI)
                            - elevation;
                }
                rot.fromAngleAxis(amount, axis);
                camera.getLocation().subtract(focus, vector);
                rot.mult(vector, vector);
                focus.add(vector, camera.getLocation());

                Quaternion curRot = camera.getRotation().clone();
                camera.setRotation(rot.mult(curRot));
                return null;
            }
        });
    }

    public void panCamera(final float left, final float up) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                camera.getLeft().mult(left, vector);
                vector.scaleAdd(up, camera.getUp(), vector);
                vector.multLocal(camera.getLocation().distance(focus));
                camera.setLocation(camera.getLocation().add(vector));
                focus.addLocal(vector);
                return null;
            }
        });
    }

    public void moveCamera(final float forward) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                camera.getDirection().mult(forward, vector);
                camera.setLocation(camera.getLocation().add(vector));
                return null;
            }
        });
    }

    public void zoomCamera(final float amount_) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                float amount = amount_;
                amount = camera.getLocation().distance(focus) * amount;
                float dist = camera.getLocation().distance(focus);
                amount = dist - Math.max(0f, dist - amount);
                Vector3f loc = camera.getLocation().clone();
                loc.scaleAdd(amount, camera.getDirection(), loc);
                camera.setLocation(loc);
                return null;
            }
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                OffScenePanel.this.componentHidden(evt);
            }
        });
        addContainerListener(new java.awt.event.ContainerAdapter() {
            public void componentRemoved(java.awt.event.ContainerEvent evt) {
                OffScenePanel.this.componentRemoved(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void componentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_componentHidden
//        removePreView();
    }//GEN-LAST:event_componentHidden

    private void componentRemoved(java.awt.event.ContainerEvent evt) {//GEN-FIRST:event_componentRemoved
        // TODO add your handling code here:
//        removePreView();
    }//GEN-LAST:event_componentRemoved
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
