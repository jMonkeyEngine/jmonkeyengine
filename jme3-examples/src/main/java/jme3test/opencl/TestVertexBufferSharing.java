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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.opencl.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.util.BufferUtils;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This test class tests the capability to read and modify an OpenGL vertex buffer.
 * It is also shown how to use the program binaries to implement a simple program
 * cache.
 * @author shaman
 */
public class TestVertexBufferSharing extends SimpleApplication {
    private static final Logger LOG = Logger.getLogger(TestVertexBufferSharing.class.getName());
    
    private int initCounter;
    private Context clContext;
    private CommandQueue clQueue;
    private Geometry geom;
    private Buffer buffer;
    private Kernel kernel;
    private com.jme3.opencl.Kernel.WorkSize ws;
    private float time;

    public static void main(String[] args){
        TestVertexBufferSharing app = new TestVertexBufferSharing();
        AppSettings settings = new AppSettings(true);
        settings.setOpenCLSupport(true);
        settings.setVSync(false);
//        settings.setRenderer(AppSettings.JOGL_OPENGL_FORWARD_COMPATIBLE);
        app.setSettings(settings);
        app.start(); // start the game
    }

    @Override
    public void simpleInitApp() {
        initOpenCL1();
        
        Box b = new Box(1, 1, 1); // create cube shape
        geom = new Geometry("Box", b);  // create cube geometry from the shape
        Material mat = new Material(assetManager,
          "Common/MatDefs/Misc/Unshaded.j3md");  // create a simple material
        mat.setColor("Color", ColorRGBA.Blue);   // set color of material to blue
        geom.setMaterial(mat);                   // set the cube's material
        rootNode.attachChild(geom);              // make the cube appear in the scene
        
        initCounter = 0;
        time = 0;
        
        flyCam.setDragToRotate(true);
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        
        if (initCounter < 2) {
        initCounter++;
        } else if (initCounter == 2) {
            //when initCounter reaches 2, the scene was drawn once and the texture was uploaded to the GPU
            //then we can bind the texture to OpenCL
            initOpenCL2();
            updateOpenCL(tpf);
            initCounter = 3;
        } else {
            updateOpenCL(tpf);
        }
    }
    
    private void initOpenCL1() {
        clContext = context.getOpenCLContext();
        Device device = clContext.getDevices().get(0);
        clQueue = clContext.createQueue(device).register();
        //create kernel
        Program program = null;
        File tmpFolder = JmeSystem.getStorageFolder();
        File binaryFile = new File(tmpFolder, getClass().getSimpleName()+".clc");
        try {
            //attempt to load cached binary
            byte[] bytes = Files.readAllBytes(binaryFile.toPath());
            ByteBuffer bb = BufferUtils.createByteBuffer(bytes);
            program = clContext.createProgramFromBinary(bb, device);
            program.build();
            LOG.info("reuse program from cached binaries");
        } catch (java.nio.file.NoSuchFileException ex) {
            //do nothing, cache was not created yet
        } catch (Exception ex) {
            LOG.log(Level.INFO, "Unable to use cached program binaries", ex);
        }
        if (program == null) {
            //build from sources
            String source = ""
                    + "__kernel void ScaleKernel(__global float* vb, float scale)\n"
                    + "{\n"
                    + "  int idx = get_global_id(0);\n"
                    + "  float3 pos = vload3(idx, vb);\n"
                    + "  pos *= scale;\n"
                    + "  vstore3(pos, idx, vb);\n"
                    + "}\n";
            program = clContext.createProgramFromSourceCode(source);
            program.build();
            //Save binary
            try {
                ByteBuffer bb = program.getBinary(device);
                byte[] bytes = new byte[bb.remaining()];
                bb.get(bytes);
                Files.write(binaryFile.toPath(), bytes);
            } catch (UnsupportedOperationException | OpenCLException | IOException ex) {
               LOG.log(Level.SEVERE, "Unable to save program binaries", ex);
            }
            LOG.info("create new program from sources");
        }
		program.register();
        kernel = program.createKernel("ScaleKernel").register();
    }
    private void initOpenCL2() {
        //bind vertex buffer to OpenCL
        VertexBuffer vb = geom.getMesh().getBuffer(VertexBuffer.Type.Position);
        buffer = clContext.bindVertexBuffer(vb, MemoryAccess.READ_WRITE).register();
        ws = new com.jme3.opencl.Kernel.WorkSize(geom.getMesh().getVertexCount());
    }
    private void updateOpenCL(float tpf) {
        //advect time
        time += tpf;
        
        //aquire resource
        buffer.acquireBufferForSharingNoEvent(clQueue);
        //no need to wait for the returned event, since the kernel implicitely waits for it (same command queue)
        
        //execute kernel
        float scale = (float) Math.pow(1.1, (1.0 - time%2) / 16.0);
        kernel.Run1NoEvent(clQueue, ws, buffer, scale);
        
        //release resource
        buffer.releaseBufferForSharingNoEvent(clQueue);
    }

}