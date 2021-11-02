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

import com.jme3.opencl.*;
import com.jme3.opencl.Context;
import com.jme3.opencl.Image.ImageDescriptor;
import com.jme3.opencl.Image.ImageFormat;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opencl.*;
import org.lwjgl.opengl.*;

/**
 *
 * @author shaman
 */
public class LwjglContext extends Context {
    private static final Logger LOG = Logger.getLogger(LwjglContext.class.getName());
    private final CLContext context;
    private final List<LwjglDevice> devices;

    public LwjglContext(CLContext context, List<LwjglDevice> devices) {
        super(new ReleaserImpl(context, devices));
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
        return new LwjglCommandQueue(q, device);
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
    public Image createImage(MemoryAccess access, ImageFormat format, ImageDescriptor descr) {
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
        CLMem mem = CL12.clCreateImage(context, memFlags, Utils.tempBuffers[0].b16, Utils.b80, descr.hostPtr, Utils.errorBuffer);
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
    public Buffer bindVertexBuffer(VertexBuffer vb, MemoryAccess access) {
        int id = vb.getId();
        if (id == -1) {
            throw new IllegalArgumentException("vertex buffer was not yet uploaded to the GPU or is CPU only");
        }
        long flags = Utils.getMemoryAccessFlags(access);
        Utils.errorBuffer.rewind();
        CLMem mem = CL10GL.clCreateFromGLBuffer(context, flags, id, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateFromGLBuffer");
        return new LwjglBuffer(mem);
    }

    @Override
    public Image bindImage(com.jme3.texture.Image image, Texture.Type textureType, int mipLevel, MemoryAccess access) {
        int imageID = image.getId();
        if (imageID == -1) {
            throw new IllegalArgumentException("image was not yet uploaded to the GPU");
        }
        long memFlags = Utils.getMemoryAccessFlags(access);
        int textureTarget = convertTextureType(textureType);
        Utils.errorBuffer.rewind();
        CLMem mem = CL12GL.clCreateFromGLTexture(context, memFlags, textureTarget, mipLevel, imageID, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateFromGLTexture");
        return new LwjglImage(mem);
    }

    @Override
    protected Image bindPureRenderBuffer(FrameBuffer.RenderBuffer buffer, MemoryAccess access) {
        int renderbuffer = buffer.getId();
        if (renderbuffer == -1) {
            throw new IllegalArgumentException("renderbuffer was not yet uploaded to the GPU");
        }
        long memFlags = Utils.getMemoryAccessFlags(access);
        Utils.errorBuffer.rewind();
        CLMem mem = CL10GL.clCreateFromGLRenderbuffer(context, memFlags, renderbuffer, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateFromGLRenderbuffer");
        return new LwjglImage(mem);
    }
    
    private int convertTextureType(Texture.Type textureType) {
        switch (textureType) {
            case TwoDimensional: return GL11.GL_TEXTURE_2D;
            case TwoDimensionalArray: return GL30.GL_TEXTURE_2D_ARRAY;
            case ThreeDimensional: return GL12.GL_TEXTURE_3D;
            case CubeMap: return GL13.GL_TEXTURE_CUBE_MAP;
            default: throw new IllegalArgumentException("unknown texture type "+textureType);
        }
    }

    @Override
    public Program createProgramFromSourceCode(String sourceCode) {
        LOG.log(Level.FINE, "Create program from source:\n{0}", sourceCode);
        Utils.errorBuffer.rewind();
        CLProgram p = CL10.clCreateProgramWithSource(context, sourceCode, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateProgramWithSource");
        return new LwjglProgram(p, this);
    }

    @Override
    public Program createProgramFromBinary(ByteBuffer binaries, Device device) {
        Utils.errorBuffer.rewind();
        Utils.tempBuffers[0].b16i.rewind();
        CLProgram p = CL10.clCreateProgramWithBinary(context, ((LwjglDevice) device).device, 
                binaries, Utils.tempBuffers[0].b16i, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateProgramWithBinary");
        Utils.checkError(Utils.tempBuffers[0].b16i, "clCreateProgramWithBinary");
        return new LwjglProgram(p, this);
    }

    private static class ReleaserImpl implements ObjectReleaser {
        private CLContext context;
        private final List<LwjglDevice> devices;
        private ReleaserImpl(CLContext mem, List<LwjglDevice> devices) {
            this.context = mem;
            this.devices = devices;
        }
        @Override
        public void release() {
            if (context != null) {
                int ret = CL10.clReleaseContext(context);
                context = null;
                devices.clear();
                Utils.reportError(ret, "clReleaseMemObject");
            }
        }
        
    }
}
