/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.system;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.system.AWTFrameProcessor.TransferMode;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;


/**
 * <p>
 * This class enables to update graphics of an AWT component with the result of JMonkey 3D rendering. 
 * </p>
 * <p>
 * This class is based on the <a href="http://www.oracle.com/technetwork/java/javase/overview/javafx-overview-2158620.html">JavaFX</a> original code provided by Alexander Brui (see <a href="https://github.com/JavaSaBr/JME3-JFX">JME3-FX</a>)
 * </p>
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 * @author Alexander Brui (JavaSaBr)
 *
 */
public class AWTComponentRenderer {

  /**
   * The constant RUNNING_STATE.
   */
  protected static final int RUNNING_STATE = 1;
  /**
   * The constant WAITING_STATE.
   */
  protected static final int WAITING_STATE = 2;
  /**
   * The constant DISPOSING_STATE.
   */
  protected static final int DISPOSING_STATE = 3;
  /**
   * The constant DISPOSED_STATE.
   */
  protected static final int DISPOSED_STATE = 4;

  /**
   * The Frame state.
   */
  protected final AtomicInteger frameState;

  /**
   * The Image state.
   */
  protected final AtomicInteger imageState;

  /**
   * The Frame buffer.
   */
  protected final FrameBuffer frameBuffer;

  /**
   * The Pixel writer.
   */
  protected final Graphics pixelWriter;

  /**
   * The Frame byte buffer.
   */
  protected final ByteBuffer frameByteBuffer;


  /**
   * The transfer mode.
   */
  protected final TransferMode transferMode;

  /**
   * The byte buffer.
   */
  protected final byte[] byteBuffer;

  /**
   * The image byte buffer.
   */
  protected final int[] imageByteBuffer;

  /**
   * The prev image byte buffer.
   */
  protected final byte[] prevImageByteBuffer;

  /**
   * How many frames need to write else.
   */
  protected int frameCount;

  /**
   * The width.
   */
  private final int width;

  /**
   * The height.
   */
  private final int height;

  private ColorModel colorModel = null;

  private Component component = null;
  
  private Graphics2D offGraphics = null;
  
  /**
   * Create a new component renderer attached to the given {@link Component destination}. 
   * The graphics of the destination are updated with the JMonkeyEngine rendering result.
   * @param destination the AWT component to use as target of the JMonkeyEngine rendering.
   * @param width the width of the component in pixels.
   * @param height the height of the component in pixels.
   * @param transferMode the rendering mode that can be {@link TransferMode#ALWAYS} if the component has to be rendered at each update 
   * or {@link TransferMode#ON_CHANGES} if the component has to be rendered only when changes are occurring.
   */
  public AWTComponentRenderer(Component destination, final int width, final int height, TransferMode transferMode) {
    this(destination, transferMode, null, width, height);
  }

  /**
   * Create a new component renderer attached to the given {@link Component destination}. 
   * The graphics of the destination are updated with the JMonkeyEngine rendering result.
   * @param destination the AWT component to use as target of the JMonkeyEngine rendering.
   * @param transferMode the rendering mode that can be {@link TransferMode#ALWAYS} if the component has to be rendered at each update or {@link TransferMode#ON_CHANGES} if the component has to be rendered only when changes are occurring.
   * @param frameBuffer the JMonkey frame buffer to use (if <code>null</code> is passed, a new default frame buffer is created)
   * @param width the width of the component in pixels.
   * @param height the height of the component in pixels.
   */
  public AWTComponentRenderer(Component destination, TransferMode transferMode, FrameBuffer frameBuffer, int width, int height) {
    this.transferMode = transferMode;
    this.frameState = new AtomicInteger(WAITING_STATE);
    this.imageState = new AtomicInteger(WAITING_STATE);
    this.width = frameBuffer != null ? frameBuffer.getWidth() : width;
    this.height = frameBuffer != null ? frameBuffer.getHeight() : height;
    this.frameCount = 0;

    if (frameBuffer != null) {
      this.frameBuffer = frameBuffer;
    } else {
      this.frameBuffer = new FrameBuffer(width, height, 1);
      this.frameBuffer.setDepthBuffer(Image.Format.Depth);
      this.frameBuffer.setColorBuffer(Image.Format.RGBA8);
      this.frameBuffer.setSrgb(true);
    }

    colorModel = ColorModel.getRGBdefault();
    
    frameByteBuffer = BufferUtils.createByteBuffer(getWidth() * getHeight() * 4);
    byteBuffer = new byte[getWidth() * getHeight() * 4];
    prevImageByteBuffer = new byte[getWidth() * getHeight() * 4];
    imageByteBuffer = new int[getWidth() * getHeight()];
    pixelWriter = getGraphics(destination);
    
    this.component = destination;
    
  }

  /**
   * Initialize the component renderer.
   * @param renderer the JMonkey {@link Renderer renderer} to use.
   * @param main <code>true</code> if the attached frame buffer is the main one or <code>false</code> otherwise.
   */
  public void init(Renderer renderer, boolean main) {
    if (main) {
      renderer.setMainFrameBufferOverride(frameBuffer);
    }
  }

  /**
   * Get the graphics context of the given component.
   * @param destination the AWT component used for rendering (not null)
   * @return the graphics context of the given component.
   */
  protected Graphics getGraphics(Component destination) {
    if (destination != null) {

      if (destination.getGraphics() != null) {
        return destination.getGraphics();
      } else {
        System.out.println("AWT component "+destination.getClass().getSimpleName()+" does not provide 2D graphics capabilities.");
        return null;
        //throw new IllegalArgumentException("AWT component "+destination.getClass().getSimpleName()+" does not provide 2D graphics capabilities.");
      }
    } else {
      throw new IllegalArgumentException("destination component cannot be null");
    }
  }

  /**
   * Get the width of the area to render.
   * @return the width of the area to render.
   * @see #getHeight()
   */
  public int getWidth() {
    return width;
  }

  /**
   * Get the height of the area to render.
   * @return the height of the area to render.
   * @see #getWidth()
   */
  public int getHeight() {
    return height;
  }

  /**
   * Copy the JMonkey frame buffer that has been rendered by the JMonkey engine and schedule the rendering of the component.
   * @param renderManager the JMonkey render manager.
   */
  public void copyFrameBufferToImage(RenderManager renderManager) {

    while (!frameState.compareAndSet(WAITING_STATE, RUNNING_STATE)) {
      if (frameState.get() == DISPOSED_STATE) {
        return;
      }
    }

    // Convert screenshot.
    try {

      frameByteBuffer.clear();

      final Renderer renderer = renderManager.getRenderer();
      renderer.readFrameBufferWithFormat(frameBuffer, frameByteBuffer, Image.Format.RGBA8);

    } finally {
      if (!frameState.compareAndSet(RUNNING_STATE, WAITING_STATE)) {
        throw new RuntimeException("unknown problem with the frame state");
      }
    }

    synchronized (byteBuffer) {
      frameByteBuffer.get(byteBuffer);

      if (transferMode == TransferMode.ON_CHANGES) {

        final byte[] prevBuffer = getPrevImageByteBuffer();

        if (Arrays.equals(prevBuffer, byteBuffer)) {
          if (frameCount == 0) return;
        } else {
          frameCount = 2;
          System.arraycopy(byteBuffer, 0, prevBuffer, 0, byteBuffer.length);
        }

        frameByteBuffer.position(0);
        frameCount--;
      }
    }

    EventQueue.invokeLater(new Runnable() {

      @Override
      public void run() {
        writeFrame();
      }});
  }

  /**
   * Write the current rendered frame to the component graphics context.
   */
  protected void writeFrame() {


    if (pixelWriter != null) {
      while (!imageState.compareAndSet(WAITING_STATE, RUNNING_STATE)) {
        if (imageState.get() == DISPOSED_STATE) {
          return;
        }
      }

      try {

        final int[] imageDataBuffer = getImageByteBuffer();

        synchronized (byteBuffer) {
          
          for(int i = 0; i < width * height; i++) {
            imageDataBuffer[i] =   ((0xff & byteBuffer[i*4+3]) << 24)  // Alpha 
                                 | ((0xff & byteBuffer[i*4])   << 16)  // Red
                                 | ((0xff & byteBuffer[i*4+1]) <<  8)  // Green 
                                 | ((0xff & byteBuffer[i*4+2]));       // BLue
            
          }
        }

        DataBuffer buffer = new DataBufferInt(imageDataBuffer, imageDataBuffer.length);

        SampleModel sm = new  SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, getWidth(), getHeight(), new int[] {0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000});

        WritableRaster raster = Raster.createWritableRaster(sm, buffer, null);
        
        BufferedImage img = new BufferedImage(colorModel, raster, false, null);
        
        BufferedImage img2 = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        offGraphics = img2.createGraphics();
        offGraphics.setColor(component.getBackground());
        img2.createGraphics().fillRect(0, 0, getWidth(), getHeight());
        img2.createGraphics().drawImage(img, null, null);    

        component.getGraphics().drawImage(img2, 0, 0, null);

      } finally {
        if (!imageState.compareAndSet(RUNNING_STATE, WAITING_STATE)) {
          throw new RuntimeException("unknown problem with the image state");
        }
      }
    } else {
      System.out.println("No graphics context available for rendering.");
    }


  }

  /**
   * Get the image byte buffer.
   * @return the image byte buffer.
   */
  protected int[] getImageByteBuffer() {
    return imageByteBuffer;
  }

  /**
   * Get the previous image byte buffer.
   * @return the previous image byte buffer.
   */
  protected byte[] getPrevImageByteBuffer() {
    return prevImageByteBuffer;
  }

  /**
   * Dispose this renderer. The underlying frame buffer is also disposed.
   */
  public void dispose() {
    while (!frameState.compareAndSet(WAITING_STATE, DISPOSING_STATE)) ;
    while (!imageState.compareAndSet(WAITING_STATE, DISPOSING_STATE)) ;
    frameBuffer.dispose();
    BufferUtils.destroyDirectBuffer(frameByteBuffer);
    frameState.compareAndSet(DISPOSING_STATE, DISPOSED_STATE);
    imageState.compareAndSet(DISPOSING_STATE, DISPOSED_STATE);
  }
}