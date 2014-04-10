/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.app.state;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;

/**
 *
 * @author kwando
 */
public class ScreenshotProcessor implements SceneProcessor {

    public interface ScreenshotHandler {

        public void screenshotCaptured(Screenshot screenshot);
    }

    public static class Screenshot {

        private static int nextSequenceNumber = 1;
        private ByteBuffer buffer;
        private final int width;
        private final int height;
        private final long sequenceNumber;

        private Screenshot(ByteBuffer buffer, int width, int height) {
            this.buffer = buffer;
            this.width = width;
            this.height = height;
            sequenceNumber = nextSequenceNumber++;
        }

        protected ByteBuffer getBuffer() {
            return buffer;
        }

        public long getSequenceNumber() {
            return this.sequenceNumber;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    private boolean doCapture;
    private Renderer renderer;
    private RenderManager rm;
    private ByteBuffer outBuf;
    private int width, height;

    private ScreenshotHandler handler;

    public ScreenshotProcessor(ScreenshotHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("ScreenshotHandler cannot be null");
        }
        this.handler = handler;
    }

    public void initialize(RenderManager renderManager, ViewPort vp) {
        renderer = rm.getRenderer();
        rm = renderManager;
        reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
    }

    public void reshape(ViewPort vp, int w, int h) {
        outBuf = BufferUtils.createByteBuffer(w * h * 4);
        width = w;
        height = h;
    }

    public boolean isInitialized() {
        return renderer != null;
    }

    public void preFrame(float tpf) {
        // Noop
    }

    public void postQueue(RenderQueue rq) {
        // Noop
    }

    public void postFrame(FrameBuffer out) {
        if (doCapture) {
            Screenshot screenshot = captureScreenshot(out);
            handler.screenshotCaptured(screenshot);
            doCapture = false;
        }
    }

    public void takeScreenshot() {
        this.doCapture = true;
    }

    public void cleanup() {
        // Noop
    }

    private Screenshot captureScreenshot(FrameBuffer out) {
        Camera curCamera = rm.getCurrentCamera();
        int viewX = (int) (curCamera.getViewPortLeft() * curCamera.getWidth());
        int viewY = (int) (curCamera.getViewPortBottom() * curCamera.getHeight());
        int viewWidth = (int) ((curCamera.getViewPortRight() - curCamera.getViewPortLeft()) * curCamera.getWidth());
        int viewHeight = (int) ((curCamera.getViewPortTop() - curCamera.getViewPortBottom()) * curCamera.getHeight());

        renderer.setViewPort(0, 0, width, height);
        renderer.readFrameBuffer(out, outBuf);
        renderer.setViewPort(viewX, viewY, viewWidth, viewHeight);
        return new Screenshot(outBuf, width, height);
    }
}
