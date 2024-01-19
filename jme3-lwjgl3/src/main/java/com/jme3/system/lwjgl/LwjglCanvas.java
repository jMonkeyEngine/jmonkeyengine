/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package com.jme3.system.lwjgl;

import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.awt.AwtKeyInput;
import com.jme3.input.awt.AwtMouseInput;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.system.JmeContext.Type;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Canvas;
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
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LwjglCanvas extends LwjglWindow implements JmeCanvasContext, Runnable {

    private static final Logger logger = Logger.getLogger(LwjglCanvas.class.getName());

    private final Canvas canvas;

    private BufferedImage img;
    private FrameBuffer fb;

    private ByteBuffer byteBuf;
    private IntBuffer intBuf;

    private BufferStrategy strategy;
    private AffineTransformOp transformOp;
    private final AtomicBoolean hasNativePeer = new AtomicBoolean(false);
    private final AtomicBoolean showing = new AtomicBoolean(false);

    private int width = 1;
    private int height = 1;
    private AtomicBoolean needResize = new AtomicBoolean(false);
    private final Object lock = new Object();

    private AwtKeyInput keyInput;
    private AwtMouseInput mouseInput;

    public LwjglCanvas() {
        super(Type.Canvas);

        canvas = new Canvas() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                synchronized (lock) {
                    g2d.drawImage(img, transformOp, 0, 0);
                }
            }

            @Override
            public void addNotify() {
                super.addNotify();

                synchronized (lock) {
                    hasNativePeer.set(true);
                }

                requestFocusInWindow();
            }

            @Override
            public void removeNotify() {
                synchronized (lock) {
                    hasNativePeer.set(false);
                }

                super.removeNotify();
            }
        };
        canvas.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                synchronized (lock) {
                    int newWidth = Math.max(canvas.getWidth(), 1);
                    int newHeight = Math.max(canvas.getHeight(), 1);
                    if (width != newWidth || height != newHeight) {
                        width = newWidth;
                        height = newHeight;
                        needResize.set(true);
                    }
                }
            }
        });
        canvas.setFocusable(true);
        canvas.setIgnoreRepaint(true);
    }

    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    @Override
    protected void showWindow() {
    }

    @Override
    protected void setWindowIcon(final AppSettings settings) {
    }

    @Override
    public void setTitle(String title) {
    }

    @Override
    public KeyInput getKeyInput() {
        if (keyInput == null) {
            keyInput = new AwtKeyInput();
            keyInput.setInputSource(canvas);
        }

        return keyInput;
    }

    @Override
    public MouseInput getMouseInput() {
        if (mouseInput == null) {
            mouseInput = new AwtMouseInput();
            mouseInput.setInputSource(canvas);
        }

        return mouseInput;
    }

    public boolean checkVisibilityState() {
        if (!hasNativePeer.get()) {
            synchronized (lock) {
                if (strategy != null) {
                    strategy.dispose();
                    strategy = null;
                }
            }
            return false;
        }

        boolean currentShowing = canvas.isShowing();
        showing.set(currentShowing);
        return currentShowing;
    }

    @Override
    protected void destroyContext() {
        synchronized (lock) {
            destroyFrameBuffer();
            img = null;
            byteBuf = null;
            intBuf = null;
        }

        super.destroyContext();
    }

    @Override
    protected void createContext(AppSettings settings) {
        super.createContext(settings);

        if (renderer != null) {
            createFrameBuffer(width, height);
        }
    }

    @Override
    protected void runLoop() {
        if (needResize.get()) {
            needResize.set(false);
            listener.reshape(width, height);
            createFrameBuffer(width, height);
        }

        if (!checkVisibilityState()) {
            return;
        }

        super.runLoop();

        drawFrameInThread();
    }

    public void drawFrameInThread() {

        // Convert screenshot
        byteBuf.clear();
        renderer.readFrameBuffer(fb, byteBuf);
        Screenshots.convertScreenShot2(intBuf, img);

        synchronized (lock) {
            // All operations on strategy should be synchronized (?)
            if (strategy == null) {
                try {
                    canvas.createBufferStrategy(1,
                            new BufferCapabilities(
                                    new ImageCapabilities(true),
                                    new ImageCapabilities(true),
                                    BufferCapabilities.FlipContents.UNDEFINED)
                    );
                } catch (AWTException ex) {
                    logger.log(Level.SEVERE, "Failed to create buffer strategy!", ex);
                }
                strategy = canvas.getBufferStrategy();
            }

            // Draw screenshot
            do {
                do {
                    Graphics2D g2d = (Graphics2D) strategy.getDrawGraphics();
                    if (g2d == null) {
                        logger.log(Level.WARNING, "OGL: DrawGraphics was null.");
                        return;
                    }

                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);

                    g2d.drawImage(img, transformOp, 0, 0);
                    g2d.dispose();
                    strategy.show();
                } while (strategy.contentsRestored());
            } while (strategy.contentsLost());
        }
    }

    private void createFrameBuffer(int width, int height) {
        byteBuf = BufferUtils.ensureLargeEnough(byteBuf, width * height * 4);
        intBuf = byteBuf.asIntBuffer();

        destroyFrameBuffer();

        fb = new FrameBuffer(width, height, settings.getSamples());
        fb.setDepthTarget(FrameBufferTarget.newTarget(Image.Format.Depth));
        fb.addColorTarget(FrameBufferTarget.newTarget(Image.Format.RGB8));
        fb.setSrgb(settings.isGammaCorrection());

        renderer.setMainFrameBufferOverride(fb);

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);

        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -img.getHeight());
        transformOp = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    }

    public void destroyFrameBuffer() {
        if (fb != null) {
            fb.dispose();
            fb = null;
        }
    }
}
