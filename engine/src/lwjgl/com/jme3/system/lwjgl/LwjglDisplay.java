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

package com.jme3.system.lwjgl;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext.Type;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.*;

public class LwjglDisplay extends LwjglAbstractDisplay {

    private static final Logger logger = Logger.getLogger(LwjglDisplay.class.getName());

    private final AtomicBoolean needRestart = new AtomicBoolean(false);
    private PixelFormat pixelFormat;

    protected DisplayMode getFullscreenDisplayMode(int width, int height, int bpp, int freq){
        try {
            DisplayMode[] modes = Display.getAvailableDisplayModes();
            for (DisplayMode mode : modes){
                if (mode.getWidth() == width
                 && mode.getHeight() == height
                 && (mode.getBitsPerPixel() == bpp || (bpp==24&&mode.getBitsPerPixel()==32))
                 && mode.getFrequency() == freq){
                    return mode;
                }
            }
        } catch (LWJGLException ex) {
            listener.handleError("Failed to acquire fullscreen display mode!", ex);
        }
        return null;
    }

    protected void createContext(AppSettings settings) throws LWJGLException{
        DisplayMode displayMode = null;
        if (settings.getWidth() <= 0 || settings.getHeight() <= 0){
            displayMode = Display.getDesktopDisplayMode();
            settings.setResolution(displayMode.getWidth(), displayMode.getHeight());
        }else if (settings.isFullscreen()){
            displayMode = getFullscreenDisplayMode(settings.getWidth(), settings.getHeight(),
                                                   settings.getBitsPerPixel(), settings.getFrequency());
            if (displayMode == null)
                throw new RuntimeException("Unable to find fullscreen display mode matching settings");
        }else{
            displayMode = new DisplayMode(settings.getWidth(), settings.getHeight());
        }

	   int samples = 0;
        if (settings.getSamples() > 1){
            samples = settings.getSamples();
        }
        PixelFormat pf = new PixelFormat(settings.getBitsPerPixel(),
                                         0,
                                         settings.getDepthBits(),
                                         settings.getStencilBits(),
                                         samples);

        frameRate = settings.getFrameRate();
        logger.log(Level.INFO, "Selected display mode: {0}", displayMode);

        boolean pixelFormatChanged = false;
        if (created.get() && (pixelFormat.getBitsPerPixel() != pf.getBitsPerPixel()
                            ||pixelFormat.getDepthBits() != pf.getDepthBits()
                            ||pixelFormat.getStencilBits() != pf.getStencilBits()
                            ||pixelFormat.getSamples() != pf.getSamples())){
            renderer.resetGLObjects();
            Display.destroy();
            pixelFormatChanged = true;
        }
        pixelFormat = pf;
        
        Display.setTitle(settings.getTitle());
        if (displayMode != null){
            if (settings.isFullscreen()){
                Display.setDisplayModeAndFullscreen(displayMode);
            }else{
                Display.setFullscreen(false);
                Display.setDisplayMode(displayMode);
            }
        }else{
            Display.setFullscreen(settings.isFullscreen());
        }

        if (settings.getIcons() != null) {
            Display.setIcon(imagesToByteBuffers(settings.getIcons()));
        }
        
        Display.setVSyncEnabled(settings.isVSync());
        
        if (created.get() && !pixelFormatChanged){
            Display.releaseContext();
            Display.makeCurrent();
            Display.update();
        }

        if (!created.get() || pixelFormatChanged){
            ContextAttribs attr = createContextAttribs();
            if (attr != null){
                Display.create(pixelFormat, attr);
            }else{
                Display.create(pixelFormat);
            }
            renderable.set(true);
            
            if (pixelFormatChanged && pixelFormat.getSamples() > 1
             && GLContext.getCapabilities().GL_ARB_multisample){
                GL11.glEnable(ARBMultisample.GL_MULTISAMPLE_ARB);
            }
        }
    }
    
    protected void destroyContext(){
        try {
            renderer.cleanup();
            Display.releaseContext();
            Display.destroy();
        } catch (LWJGLException ex) {
            listener.handleError("Failed to destroy context", ex);
        }
    }

    public void create(boolean waitFor){
        if (created.get()){
            logger.warning("create() called when display is already created!");
            return;
        }

        new Thread(this, "LWJGL Renderer Thread").start();
        if (waitFor)
            waitFor(true);
    }

    @Override
    public void runLoop(){
        // This method is overriden to do restart
        if (needRestart.getAndSet(false)){
            try{
                createContext(settings);
            }catch (LWJGLException ex){
                logger.log(Level.SEVERE, "Failed to set display settings!", ex);
            }
            listener.reshape(settings.getWidth(), settings.getHeight());
            logger.info("Display restarted.");
        }

        super.runLoop();
    }

    @Override
    public void restart() {
        if (created.get()){
            needRestart.set(true);
        }else{
            logger.warning("Display is not created, cannot restart window.");
        }
    }

    public Type getType() {
        return Type.Display;
    }

    public void setTitle(String title){
        if (created.get())
            Display.setTitle(title);
    }
    
    private ByteBuffer[] imagesToByteBuffers(Object[] images) {
        ByteBuffer[] out = new ByteBuffer[images.length];
        for (int i = 0; i < images.length; i++) {
            BufferedImage image = (BufferedImage) images[i];
            out[i] = imageToByteBuffer(image);
        }
        return out;
    }

    private ByteBuffer imageToByteBuffer(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB_PRE) {
            BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = convertedImage.createGraphics();
            double width = image.getWidth() * (double) 1;
            double height = image.getHeight() * (double) 1;
            g.drawImage(image, (int) ((convertedImage.getWidth() - width) / 2),
                    (int) ((convertedImage.getHeight() - height) / 2),
                    (int) (width), (int) (height), null);
            g.dispose();
            image = convertedImage;
        }

        byte[] imageBuffer = new byte[image.getWidth() * image.getHeight() * 4];
        int counter = 0;
        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                int colorSpace = image.getRGB(j, i);
                imageBuffer[counter + 0] = (byte) ((colorSpace << 8) >> 24);
                imageBuffer[counter + 1] = (byte) ((colorSpace << 16) >> 24);
                imageBuffer[counter + 2] = (byte) ((colorSpace << 24) >> 24);
                imageBuffer[counter + 3] = (byte) (colorSpace >> 24);
                counter += 4;
            }
        }
        return ByteBuffer.wrap(imageBuffer);
    }

}
