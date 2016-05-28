/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

package jme3test.opencl;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.opencl.*;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple test checking if the basic functions of the OpenCL wrapper work
 * @author shaman
 */
public class HelloOpenCL extends SimpleApplication {
    private static final Logger LOG = Logger.getLogger(HelloOpenCL.class.getName());

    public static void main(String[] args){
        HelloOpenCL app = new HelloOpenCL();
        AppSettings settings = new AppSettings(true);
        settings.setOpenCLSupport(true);
        settings.setVSync(true);
//        settings.setRenderer(AppSettings.JOGL_OPENGL_FORWARD_COMPATIBLE);
        app.setSettings(settings);
        app.start(); // start the game
    }

    @Override
    public void simpleInitApp() {
        BitmapFont fnt = assetManager.loadFont("Interface/Fonts/Default.fnt");
        Context clContext = context.getOpenCLContext();
        if (clContext == null) {
            BitmapText txt = new BitmapText(fnt);
            txt.setText("No OpenCL Context created!\nSee output log for details.");
            txt.setLocalTranslation(5, settings.getHeight() - 5, 0);
            guiNode.attachChild(txt);
            return;
        }
        CommandQueue clQueue = clContext.createQueue();
        
        StringBuilder str = new StringBuilder();
        str.append("OpenCL Context created:\n  Platform: ")
                .append(clContext.getDevices().get(0).getPlatform().getName())
                .append("\n  Devices: ").append(clContext.getDevices());
        str.append("\nTests:");
        str.append("\n  Buffers: ").append(testBuffer(clContext, clQueue));
        str.append("\n  Kernel: ").append(testKernel(clContext, clQueue));
        str.append("\n  Images: ").append(testImages(clContext, clQueue));
        
        clQueue.release();
        
        BitmapText txt1 = new BitmapText(fnt);
        txt1.setText(str.toString());
        txt1.setLocalTranslation(5, settings.getHeight() - 5, 0);
        guiNode.attachChild(txt1);
        
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);
    }
    
    private static void assertEquals(byte expected, byte actual, String message) {
        if (expected != actual) {
            System.err.println(message+": expected="+expected+", actual="+actual);
            throw new AssertionError();
        }
    }
    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            System.err.println(message+": expected="+expected+", actual="+actual);
            throw new AssertionError();
        }
    }
    private static void assertEquals(double expected, double actual, String message) {
        if (Math.abs(expected - actual) >= 0.00001) {
            System.err.println(message+": expected="+expected+", actual="+actual);
            throw new AssertionError();
        }
    }
    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            System.err.println(message+": expected="+expected+", actual="+actual);
            throw new AssertionError();
        }
    }
    
    private boolean testBuffer(Context clContext, CommandQueue clQueue) {
        try {
            //create two buffers
            ByteBuffer h1 = BufferUtils.createByteBuffer(256);
            Buffer b1 = clContext.createBuffer(256);
            ByteBuffer h2 = BufferUtils.createByteBuffer(256);
            Buffer b2 = clContext.createBuffer(256);

            //fill buffer
            h2.rewind();
            for (int i=0; i<256; ++i) {
                h2.put((byte)i);
            }
            h2.rewind();
            b2.write(clQueue, h2);
            
            //copy b2 to b1
            b2.copyTo(clQueue, b1);
            
            //read buffer
            h1.rewind();
            b1.read(clQueue, h1);
            h1.rewind();
            for (int i=0; i<256; ++i) {
                byte b = h1.get();
                assertEquals((byte) i, b, "Wrong byte read");
            }
            
            //read buffer with offset
            int low = 26;
            int high = 184;
            h1.position(5);
            Event event = b1.readAsync(clQueue, h1, high-low, low);
            event.waitForFinished();
            h1.position(5);
            for (int i=0; i<high-low; ++i) {
                byte b = h1.get();
                assertEquals((byte) (i+low), b, "Wrong byte read");
            }
        
            //release
            b1.release();
            b2.release();
            
        } catch (AssertionError ex) {
            LOG.log(Level.SEVERE, "Buffer test failed with an assertion error");
            return false;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Buffer test failed with:", ex);
            return false;
        }
        return true;
    }
    
    private boolean testKernel(Context clContext, CommandQueue clQueue) {
        try {
            //create fill code
            String include = "#define TYPE float\n";
            Program program = clContext.createProgramFromSourceFilesWithInclude(assetManager, include, "jme3test/opencl/Blas.cl");
            program.build();
            Kernel kernel = program.createKernel("Fill");
            System.out.println("number of args: "+kernel.getArgCount());

            //fill buffer
            int size = 256+128;
            Buffer buffer = clContext.createBuffer(size*4);
            float value = 5;
            Event event = kernel.Run1(clQueue, new com.jme3.opencl.Kernel.WorkSize(buffer.getSize() / 4), buffer, value);
            event.waitForFinished();
            
            //check if filled
            ByteBuffer buf = buffer.map(clQueue, MappingAccess.MAP_READ_ONLY);
            FloatBuffer buff = buf.asFloatBuffer();
            for (int i=0; i<size; ++i) {
                float v = buff.get(i);
                assertEquals(value, v, "Buffer filled with the wrong value at index "+i);
            }
            buffer.unmap(clQueue, buf);
            
            //release
            buffer.release();
            kernel.release();
            program.release();

        } catch (AssertionError ex) {
            LOG.log(Level.SEVERE, "kernel test failed with an assertion error");
            return false;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "kernel test failed with:", ex);
            return false;
        }
        return true;
    }
    
    private boolean testImages(Context clContext, CommandQueue clQueue) {
        try {
            //query supported formats
            for (MemoryAccess ma : MemoryAccess.values()) {
                for (Image.ImageType type : Image.ImageType.values()) {
                    try {
                        System.out.println("Formats for " + ma + " and " + type + ": " + Arrays.toString(clContext.querySupportedFormats(ma, type)));
                    } catch (UnsupportedOperationException e) {
                        LOG.warning(e.getLocalizedMessage());
                    }
                }
            }
            
            //create an image
            Image.ImageFormat format = new Image.ImageFormat(Image.ImageChannelOrder.RGBA, Image.ImageChannelType.FLOAT);
            Image.ImageDescriptor descr = new Image.ImageDescriptor(Image.ImageType.IMAGE_2D, 1920, 1080, 0, 0);
            Image image = clContext.createImage(MemoryAccess.READ_WRITE, format, descr);
            System.out.println("image created");
            
            //check queries
            assertEquals(descr.type, image.getImageType(), "Wrong image type");
            assertEquals(format, image.getImageFormat(), "Wrong image format");
            assertEquals(descr.width, image.getWidth(), "Wrong width");
            assertEquals(descr.height, image.getHeight(), "Wrong height");
            
            //fill with red and blue
            ColorRGBA color1 = ColorRGBA.Red;
            ColorRGBA color2 = ColorRGBA.Blue;
            Event e1 = image.fillAsync(clQueue, new long[]{0,0,0}, new long[]{descr.width/2, descr.height, 1}, color1);
            Event e2 = image.fillAsync(clQueue, new long[]{descr.width/2,0,0}, new long[]{descr.width/2, descr.height, 1}, color2);
            e1.waitForFinished();
            e2.waitForFinished();
            
            //copy to a buffer
            Buffer buffer = clContext.createBuffer(4*4*500*1024);
            Event e3 = image.copyToBufferAsync(clQueue, buffer, new long[]{10,10,0}, new long[]{500,1024,1}, 0);
            e3.release();
            //this buffer must be completely red
            ByteBuffer map1 = buffer.map(clQueue, MappingAccess.MAP_READ_ONLY);
            FloatBuffer map1F = map1.asFloatBuffer(); map1F.rewind();
            for (int x=0; x<500; ++x) {
                for (int y=0; y<1024; ++y) {
                    float r = map1F.get(); 
                    float g = map1F.get(); 
                    float b = map1F.get(); 
                    float a = map1F.get();
                    assertEquals(1, r, "Wrong red component");
                    assertEquals(0, g, "Wrong green component");
                    assertEquals(0, b, "Wrong blue component");
                    assertEquals(1, a, "Wrong alpha component");
                }
            }
            buffer.unmap(clQueue, map1);
            
            //create a second image
            format = new Image.ImageFormat(Image.ImageChannelOrder.RGBA, Image.ImageChannelType.FLOAT);
            descr = new Image.ImageDescriptor(Image.ImageType.IMAGE_2D, 512, 512, 0, 0);
            Image image2 = clContext.createImage(MemoryAccess.READ_WRITE, format, descr);
            //copy an area of image1 to image2
            image.copyTo(clQueue, image2, new long[]{1000, 20,0}, new long[]{0,0,0}, new long[]{512, 512,1});
            //this area should be completely blue
            Image.ImageMapping map2 = image2.map(clQueue, new long[]{0,0,0}, new long[]{512,512,1}, MappingAccess.MAP_READ_WRITE);
            FloatBuffer map2F = map2.buffer.asFloatBuffer();
            for (int y=0; y<512; ++y) {
                for (int x=0; x<512; ++x) {
                    long index = 4 * x + y * (map2.rowPitch / 4);
                    map2F.position((int) index);
                    float r = map2F.get();
                    float g = map2F.get();
                    float b = map2F.get();
                    float a = map2F.get();
                    assertEquals(0, r, "Wrong red component");
                    assertEquals(0, g, "Wrong green component");
                    assertEquals(1, b, "Wrong blue component");
                    assertEquals(1, a, "Wrong alpha component");
                }
            }
            image2.unmap(clQueue, map2);
            
            //release
            image.release();
            image2.release();
            buffer.release();
            
        } catch (AssertionError ex) {
            LOG.log(Level.SEVERE, "image test failed with an assertion error");
            return false;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "image test failed with:", ex);
            return false;
        }
        return true;
    }
}