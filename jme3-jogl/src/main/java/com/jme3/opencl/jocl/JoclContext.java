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
package com.jme3.opencl.jocl;

import com.jme3.opencl.*;
import com.jme3.opencl.Context;
import com.jme3.opencl.Image.ImageDescriptor;
import com.jme3.opencl.Image.ImageFormat;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLMemory.Mem;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.llb.gl.CLGL;
import com.jogamp.opencl.llb.impl.CLImageFormatImpl;
import com.jogamp.opengl.GL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shaman
 */
public class JoclContext extends Context {
    private static final Logger LOG = Logger.getLogger(JoclContext.class.getName());
    
    final CLContext context;
    final long id;
    final CL cl; 
    private final List<JoclDevice> devices;

    public JoclContext(CLContext context, List<JoclDevice> devices) {
        super(new ReleaserImpl(context.ID, devices));
        this.context = context;
        this.id = context.ID;
        this.cl = context.getCL();
        this.devices = devices;
    }

    public CLContext getContext() {
        return context;
    }

    @Override
    public List<JoclDevice> getDevices() {
        return devices;
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public CommandQueue createQueue(Device device) {
        assert (devices.contains(device)); //this also ensures that device is a JoclDevice
        long d = ((JoclDevice) device).id;
        long properties = 0;
        long q = cl.clCreateCommandQueue(id, d, properties, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateCommandQueue");
        return new JoclCommandQueue(q, device);
    }
    
    @Override
    public Buffer createBuffer(long size, MemoryAccess access) {
        long flags = Utils.getMemoryAccessFlags(access);
        long mem = cl.clCreateBuffer(id, flags, size, null, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateBuffer");
        return new JoclBuffer(mem);
    }

    @Override
    public Buffer createBufferFromHost(ByteBuffer data, MemoryAccess access) {
        long flags = Utils.getMemoryAccessFlags(access);
        flags |= CL.CL_MEM_USE_HOST_PTR;
        long mem = cl.clCreateBuffer(id, flags, data.capacity(), data, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateBuffer");
        return new JoclBuffer(mem);
    }

    @Override
    public Image createImage(MemoryAccess access, ImageFormat format, ImageDescriptor descr) {
        if (descr.type != Image.ImageType.IMAGE_2D && descr.type != Image.ImageType.IMAGE_3D) {
            throw new UnsupportedOperationException("Jocl only supports 2D and 3D images");
        }
        long memFlags = Utils.getMemoryAccessFlags(access);
        Utils.errorBuffer.rewind();
        //fill image format
        CLImageFormatImpl f = CLImageFormatImpl.create();
        f.setImageChannelOrder(JoclImage.decodeImageChannelOrder(format.channelOrder));
        f.setImageChannelDataType(JoclImage.decodeImageChannelType(format.channelType));
        //create image
        long mem;
        if (descr.type == Image.ImageType.IMAGE_2D) {
            mem = cl.clCreateImage2D(id, memFlags, f, descr.width, descr.height, 
                    descr.hostPtr==null ? 0 : descr.rowPitch, descr.hostPtr, Utils.errorBuffer);
            Utils.checkError(Utils.errorBuffer, "clCreateImage2D");
        } else {
            mem = cl.clCreateImage3D(id, memFlags, f, descr.width, descr.height, descr.depth, 
                    descr.hostPtr==null ? 0 : descr.rowPitch, descr.hostPtr==null ? 0 : descr.slicePitch, 
                    descr.hostPtr, Utils.errorBuffer);
            Utils.checkError(Utils.errorBuffer, "clCreateImage3D");
        }
        return new JoclImage(mem);
    }

    @Override
    public ImageFormat[] querySupportedFormats(MemoryAccess access, Image.ImageType type) {
        if (type != Image.ImageType.IMAGE_2D && type != Image.ImageType.IMAGE_3D) {
            throw new UnsupportedOperationException("Jocl only supports 2D and 3D images");
        }
        long memFlags = Utils.getMemoryAccessFlags(access);
        CLImageFormat[] fx;
        if (type == Image.ImageType.IMAGE_2D) {
            fx = context.getSupportedImage2dFormats(Mem.valueOf((int) memFlags));
        }  else {
            fx = context.getSupportedImage3dFormats(Mem.valueOf((int) memFlags));
        }
        //convert formats
        ImageFormat[] formats = new ImageFormat[fx.length];
        for (int i=0; i<fx.length; ++i) {
            Image.ImageChannelOrder channelOrder = JoclImage.encodeImageChannelOrder(fx[i].getFormatImpl().getImageChannelOrder());
            Image.ImageChannelType channelType = JoclImage.encodeImageChannelType(fx[i].getFormatImpl().getImageChannelDataType());
            formats[i] = new ImageFormat(channelOrder, channelType);
        }
        return formats;
    }

    @Override
    public Buffer bindVertexBuffer(VertexBuffer vb, MemoryAccess access) {
        int vbId = vb.getId();
        if (vbId == -1) {
            throw new IllegalArgumentException("vertex buffer was not yet uploaded to the GPU or is CPU only");
        }
        long flags = Utils.getMemoryAccessFlags(access);
        Utils.errorBuffer.rewind();
        long mem = ((CLGL) cl).clCreateFromGLBuffer(id, flags, vbId, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateFromGLBuffer");
        return new JoclBuffer(mem);
    }

    @Override
    public Image bindImage(com.jme3.texture.Image image, Texture.Type textureType, int miplevel, MemoryAccess access) {
        int imageID = image.getId();
        if (imageID == -1) {
            throw new IllegalArgumentException("image was not yet uploaded to the GPU");
        }
        long memFlags = Utils.getMemoryAccessFlags(access);
        int textureTarget = convertTextureType(textureType);
        Utils.errorBuffer.rewind();
        long mem;
        if (textureType == Texture.Type.TwoDimensional) {
            mem = ((CLGL) cl).clCreateFromGLTexture2D(id, memFlags, textureTarget, miplevel, imageID, Utils.errorBuffer);
        } else if (textureType == Texture.Type.ThreeDimensional) {
            mem = ((CLGL) cl).clCreateFromGLTexture3D(id, memFlags, textureTarget, miplevel, imageID, Utils.errorBuffer);
        } else {
            throw new UnsupportedOperationException("Jocl only supports 2D and 3D images");
        }
        Utils.checkError(Utils.errorBuffer, "clCreateFromGLTexture");
        return new JoclImage(mem);
    }

    @Override
    protected Image bindPureRenderBuffer(FrameBuffer.RenderBuffer buffer, MemoryAccess access) {
        int renderbuffer = buffer.getId();
        if (renderbuffer == -1) {
            throw new IllegalArgumentException("renderbuffer was not yet uploaded to the GPU");
        }
        long memFlags = Utils.getMemoryAccessFlags(access);
        Utils.errorBuffer.rewind();
        long mem = ((CLGL) cl).clCreateFromGLRenderbuffer(id, memFlags, renderbuffer, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateFromGLRenderbuffer");
        return new JoclImage(mem);
    }
    
    private int convertTextureType(Texture.Type textureType) {
        switch (textureType) {
            case TwoDimensional: return GL.GL_TEXTURE_2D;
            case CubeMap: return GL.GL_TEXTURE_CUBE_MAP;
            default: throw new IllegalArgumentException("unknown or unsupported texture type "+textureType);
        }
    }

    @Override
    public Program createProgramFromSourceCode(String sourceCode) {
        LOG.log(Level.FINE, "Create program from source:\n{0}", sourceCode);
        Utils.errorBuffer.rewind();
        Utils.pointers[0].rewind();
        Utils.pointers[0].put(0, sourceCode.length());
        long p = cl.clCreateProgramWithSource(id, 1, new String[]{sourceCode}, Utils.pointers[0], Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateProgramWithSource");
        return new JoclProgram(p, this);
    }

    @Override
    public Program createProgramFromBinary(ByteBuffer binaries, Device device) {
        binaries.rewind();
        Utils.errorBuffer.rewind();
        Utils.tempBuffers[0].b16i.rewind();
        Utils.pointers[0].rewind();
        Utils.pointers[0].put(0, ((JoclDevice) device).id);
        Utils.pointers[1].rewind();
        Utils.pointers[1].put(0, binaries.remaining());
        Utils.pointers[2].rewind();
        Utils.pointers[2].referenceBuffer(0, binaries);
        long p = cl.clCreateProgramWithBinary(id, 1, Utils.pointers[0], 
                Utils.pointers[1], Utils.pointers[2], Utils.tempBuffers[0].b16i, 
                Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateProgramWithBinary");
        Utils.checkError(Utils.tempBuffers[0].b16i, "clCreateProgramWithBinary");
        return new JoclProgram(p, this);
    }

    private static class ReleaserImpl implements ObjectReleaser {
        private long id;
        private final List<JoclDevice> devices;
        private ReleaserImpl(long id, List<JoclDevice> devices) {
            this.id = id;
            this.devices = devices;
        }
        @Override
        public void release() {
            if (id != 0) {
                int ret = CLPlatform.getLowLevelCLInterface().clReleaseContext(id);
                id = 0;
                devices.clear();
                Utils.reportError(ret, "clReleaseContext");
            }
        }
        
    }
}
