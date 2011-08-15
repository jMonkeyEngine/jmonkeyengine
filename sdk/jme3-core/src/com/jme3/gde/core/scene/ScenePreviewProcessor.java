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
package com.jme3.gde.core.scene;

import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author normenhansen
 */
public class ScenePreviewProcessor implements SceneProcessor {

    private static int width = 120, height = 120;
    private ByteBuffer cpuBuf = BufferUtils.createByteBuffer(width * height * 4);
    private byte[] cpuArray = new byte[width * height * 4];
    protected Node previewNode = new Node("Preview Node");
    protected JmeSpatial previewSpat = null;
    private FrameBuffer offBuffer;
    private ViewPort offView;
    private Camera offCamera;
    private ConcurrentLinkedQueue<PreviewRequest> previewQueue = new ConcurrentLinkedQueue<PreviewRequest>();
    private PreviewRequest currentPreviewRequest;
    private RenderManager rm;
    private PointLight light;

    public void addRequest(PreviewRequest request) {
        previewQueue.add(request);
        boolean reInit = false;
        if (request.getCameraRequest().getWidth() != width) {
            reInit = true;
            width = request.getCameraRequest().getWidth();
        }
        if (request.getCameraRequest().getHeight() != height) {
            reInit = true;
            height = request.getCameraRequest().getHeight();
        }
        if (reInit) {
            setupPreviewView();
        }
    }

    private void update(float tpf) {
        previewNode.updateLogicalState(tpf);
        previewNode.updateGeometricState();
    }

    public void setupPreviewView() {
        if (offCamera == null) {
            offCamera = new Camera(width, height);
        } else {
            offCamera.resize(width, height, true);
        }

        // create a pre-view. a view that is rendered before the main view
        if (offView == null) {
            offView = SceneApplication.getApplication().getRenderManager().createPreView("Offscreen View", offCamera);
            offView.setBackgroundColor(ColorRGBA.DarkGray);
            offView.setClearFlags(true, true, true);
            offView.addProcessor(this);
            // setup framebuffer's scene
            light = new PointLight();
            light.setPosition(offCamera.getLocation());
            light.setColor(ColorRGBA.White);
            previewNode.addLight(light);

            // attach the scene to the viewport to be rendered
            offView.attachScene(previewNode);
        }

        cpuBuf = BufferUtils.createByteBuffer(width * height * 4);
        cpuArray = new byte[width * height * 4];

        // create offscreen framebuffer        
        offBuffer = new FrameBuffer(width, height, 0);

        //setup framebuffer's cam
        offCamera.setFrustumPerspective(45f, 1f, 1f, 1000f);
        offCamera.setLocation(new Vector3f(5f, 5f, 5f));
        offCamera.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);

        //setup framebuffer to use texture
        offBuffer.setDepthBuffer(Format.Depth);
        offBuffer.setColorBuffer(Format.RGBA8);

        //set viewport to render to offscreen framebuffer
        offView.setOutputFrameBuffer(offBuffer);


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
        currentPreviewRequest = previewQueue.poll();
        if (currentPreviewRequest != null) {
            previewNode.attachChild(currentPreviewRequest.getSpatial());
            if (currentPreviewRequest.getCameraRequest().location != null) {
                offCamera.setLocation(currentPreviewRequest.getCameraRequest().location);
                light.setPosition(currentPreviewRequest.getCameraRequest().location);
            }
            if (currentPreviewRequest.getCameraRequest().rotation != null) {
                offCamera.setRotation(currentPreviewRequest.getCameraRequest().rotation);
            }
            if (currentPreviewRequest.getCameraRequest().lookAt != null) {
                offCamera.lookAt(currentPreviewRequest.getCameraRequest().lookAt, currentPreviewRequest.getCameraRequest().up);
            }
        }
        update(f);
    }

    public void postQueue(RenderQueue rq) {
    }

    public void postFrame(FrameBuffer fb) {
        if (currentPreviewRequest != null) {
            cpuBuf.clear();
            SceneApplication.getApplication().getRenderer().readFrameBuffer(offBuffer, cpuBuf);

            // copy native memory to java memory
            cpuBuf.clear();
            cpuBuf.get(cpuArray);
            cpuBuf.clear();

            // flip the components the way AWT likes them
            for (int i = 0; i < width * height * 4; i += 4) {
                byte b = cpuArray[i + 0];
                byte g = cpuArray[i + 1];
                byte r = cpuArray[i + 2];
                byte a = cpuArray[i + 3];

                cpuArray[i + 0] = a;
                cpuArray[i + 1] = b;
                cpuArray[i + 2] = g;
                cpuArray[i + 3] = r;
            }

            BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_4BYTE_ABGR);
            WritableRaster wr = image.getRaster();
            DataBufferByte db = (DataBufferByte) wr.getDataBuffer();
            System.arraycopy(cpuArray, 0, db.getData(), 0, cpuArray.length);

            currentPreviewRequest.setImage(image);
            previewNode.detachAllChildren();
            SceneApplication.getApplication().notifySceneListeners(currentPreviewRequest);
            currentPreviewRequest = null;
        }
    }

    public void cleanup() {
    }
}
