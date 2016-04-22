/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.opencl.lwjgl;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.opencl.*;
import com.jme3.opencl.Image.ImageDescriptor;
import com.jme3.opencl.Image.ImageFormat;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opencl.*;
import sun.misc.IOUtils;

/**
 *
 * @author Sebastian Weiss
 */
public class LwjglContext extends Context {
    private static final Logger LOG = Logger.getLogger(LwjglContext.class.getName());
    private final CLContext context;
    private final List<LwjglDevice> devices;

    public LwjglContext(CLContext context, List<LwjglDevice> devices) {
        this.context = context;
        this.devices = devices;
    }

    public CLContext getContext() {
        return context;
    }

    @Override
    public List<LwjglDevice> getDevices() {
        return devices;
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public CommandQueue createQueue(Device device) {
        assert (devices.contains(device)); //this also ensures that device is a LwjglDevice
        CLDevice d = ((LwjglDevice) device).getDevice();
        long properties = 0;
        CLCommandQueue q = CL10.clCreateCommandQueue(context, d, properties, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateCommandQueue");
        return new LwjglCommandQueue(q);
    }
    
    @Override
    public Buffer createBuffer(long size, MemoryAccess access) {
        long flags = Utils.getMemoryAccessFlags(access);
        CLMem mem = CL10.clCreateBuffer(context, flags, size, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateBuffer");
        return new LwjglBuffer(mem);
    }

    @Override
    public Buffer createBufferFromHost(ByteBuffer data, MemoryAccess access) {
        long flags = Utils.getMemoryAccessFlags(access);
        flags |= CL10.CL_MEM_USE_HOST_PTR;
        CLMem mem = CL10.clCreateBuffer(context, flags, data, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateBuffer");
        return new LwjglBuffer(mem);
    }

    @Override
    public Image createImage(MemoryAccess access, ImageFormat format, ImageDescriptor descr, ByteBuffer hostPtr) {
        long memFlags = Utils.getMemoryAccessFlags(access);
        Utils.errorBuffer.rewind();
        //fill image format
        Utils.tempBuffers[0].b16i.rewind();
        Utils.tempBuffers[0].b16i.put(LwjglImage.decodeImageChannelOrder(format.channelOrder))
                .put(LwjglImage.decodeImageChannelType(format.channelType));
        Utils.tempBuffers[0].b16.rewind();
        //fill image desc
        Utils.b80l.rewind();
        Utils.b80l.put(LwjglImage.decodeImageType(descr.type))
                .put(descr.width).put(descr.height).put(descr.depth)
                .put(descr.arraySize).put(descr.rowPitch).put(descr.slicePitch)
                .put(0).put(0).put(0);
        Utils.b80.rewind();
        //create image
        CLMem mem = CL12.clCreateImage(context, memFlags, Utils.tempBuffers[0].b16, Utils.b80, hostPtr, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateImage");
        return new LwjglImage(mem);
    }

    @Override
    public ImageFormat[] querySupportedFormats(MemoryAccess access, Image.ImageType type) {
        long memFlags = Utils.getMemoryAccessFlags(access);
        int typeFlag = LwjglImage.decodeImageType(type);
        Utils.tempBuffers[0].b16i.rewind();
        //query count
        int ret = CL10.clGetSupportedImageFormats(context, memFlags, typeFlag, null, Utils.tempBuffers[0].b16i);
        Utils.checkError(ret, "clGetSupportedImageFormats");
        int count = Utils.tempBuffers[0].b16i.get(0);
        if (count == 0) {
            return new ImageFormat[0];
        }
        //get formats
        ByteBuffer formatsB = BufferUtils.createByteBuffer(count * 8);
        ret = CL10.clGetSupportedImageFormats(context, memFlags, typeFlag, formatsB, null);
        Utils.checkError(ret, "clGetSupportedImageFormats");
        //convert formats
        ImageFormat[] formats = new ImageFormat[count];
        IntBuffer formatsBi = formatsB.asIntBuffer();
        formatsBi.rewind();
        for (int i=0; i<count; ++i) {
            Image.ImageChannelOrder channelOrder = LwjglImage.encodeImageChannelOrder(formatsBi.get());
            Image.ImageChannelType channelType = LwjglImage.encodeImageChannelType(formatsBi.get());
            formats[i] = new ImageFormat(channelOrder, channelType);
        }
        return formats;
    }

    @Override
    public Buffer bindVertexBuffer(VertexBuffer vb) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Buffer bindIndexBuffer(IndexBuffer ib) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Image bindImage(com.jme3.texture.Image image) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Program createProgramFromSourceCode(String sourceCode) {
        LOG.log(Level.INFO, "Create program from source:\n{0}", sourceCode);
        Utils.errorBuffer.rewind();
        CLProgram p = CL10.clCreateProgramWithSource(context, sourceCode, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateProgramWithSource");
        return new LwjglProgram(p, this);
    }
    
}
