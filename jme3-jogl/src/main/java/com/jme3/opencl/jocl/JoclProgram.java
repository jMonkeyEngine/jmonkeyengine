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
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.llb.CL;
import com.jogamp.opencl.util.CLUtil;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jogamp.common.nio.Buffers.newDirectByteBuffer;
/**
 *
 * @author shaman
 */
public class JoclProgram extends Program {
    private static final Logger LOG = Logger.getLogger(JoclProgram.class.getName());
    
    final long program;
    final CL cl;
    private final JoclContext context;

    public JoclProgram(long program, JoclContext context) {
        super(new ReleaserImpl(program));
        this.program = program;
        this.context = context;
        this.cl = CLPlatform.getLowLevelCLInterface();
    }

    @Override
    public void build(String args, Device... devices) throws KernelCompilationException {
        PointerBuffer deviceList = null;
        int deviceCount = 0;
        if (devices != null) {
            deviceList = PointerBuffer.allocateDirect(devices.length);
            for (Device d : devices) {
                deviceList.put(((JoclDevice) d).id);
            }
            deviceCount = devices.length;
            deviceList.rewind();
        }
        int ret = cl.clBuildProgram(program, deviceCount, deviceList, args, null);
        if (ret != CL.CL_SUCCESS) {
            String log = Log();
            LOG.log(Level.WARNING, "Unable to compile program:\n{0}", log);
            if (ret == CL.CL_BUILD_PROGRAM_FAILURE) {
                throw new KernelCompilationException("Failed to build program", ret, log);
            } else {
                Utils.checkError(ret, "clBuildProgram");
            }
        } else {
            LOG.log(Level.INFO, "Program compiled:\n{0}", Log());
        }
    }
    
    private String Log(long device) {
        Utils.pointers[0].rewind();
        int ret = cl.clGetProgramBuildInfo(program, device, CL.CL_PROGRAM_BUILD_LOG, 0, null, Utils.pointers[0]);
        Utils.checkError(ret, "clGetProgramBuildInfo");
        int count = (int) Utils.pointers[0].get(0);
        final ByteBuffer buffer = newDirectByteBuffer(count);
        ret = cl.clGetProgramBuildInfo(program, device, CL.CL_PROGRAM_BUILD_LOG, buffer.capacity(), buffer, null);
        Utils.checkError(ret, "clGetProgramBuildInfo");
        return CLUtil.clString2JavaString(buffer, count);
    }
    
    private String Log() {
        StringBuilder str = new StringBuilder();
        for (JoclDevice device : context.getDevices()) {
            long d = device.id;
            str.append(device.getName()).append(":\n");
            str.append(Log(d));
            str.append('\n');
        }
        return str.toString();
    }

    @Override
    public Kernel createKernel(String name) {
        Utils.errorBuffer.rewind();
        long kernel = cl.clCreateKernel(program, name, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateKernel");
        return new JoclKernel(kernel);
    }

    @Override
    public Kernel[] createAllKernels() {
        Utils.tempBuffers[0].b16i.rewind();
        int ret = cl.clCreateKernelsInProgram(program, 0, null, Utils.tempBuffers[0].b16i);
        Utils.checkError(ret, "clCreateKernelsInProgram");
        int count = Utils.tempBuffers[0].b16i.get(0);
        PointerBuffer buf = PointerBuffer.allocateDirect(count);
        ret = cl.clCreateKernelsInProgram(program, count, buf, null);
        Utils.checkError(ret, "clCreateKernelsInProgram");
        Kernel[] kx = new Kernel[count];
        for (int i=0; i<count; ++i) {
            kx[i] = new JoclKernel(buf.get());
        }
        return kx;
    }

    @Override
    public ByteBuffer getBinary(Device d) {
        
        JoclDevice device = (JoclDevice) d;
        
        Utils.tempBuffers[0].b16i.rewind();
        int ret = cl.clGetProgramInfo(program, CL.CL_PROGRAM_NUM_DEVICES, 4, Utils.tempBuffers[0].b16i, null);
        Utils.checkError(ret, "clGetProgramInfo: CL_PROGRAM_NUM_DEVICES");
        int numDevices = Utils.tempBuffers[0].b16i.get(0);
        
        PointerBuffer devices = PointerBuffer.allocateDirect(numDevices);
        ret = cl.clGetProgramInfo(program, CL.CL_PROGRAM_DEVICES, numDevices * PointerBuffer.ELEMENT_SIZE, devices.getBuffer(), null);
        Utils.checkError(ret, "clGetProgramInfo: CL_PROGRAM_DEVICES");
        int index = -1;
        for (int i=0; i<numDevices; ++i) {
            if (devices.get(i) == device.id) {
                index = i;
            }
        }
        if (index == -1) {
             throw new com.jme3.opencl.OpenCLException("Program was not built against the specified device "+device);
        }
        
        final PointerBuffer sizes = PointerBuffer.allocateDirect(numDevices);
        ret = cl.clGetProgramInfo(program, CL.CL_PROGRAM_BINARY_SIZES, numDevices * PointerBuffer.ELEMENT_SIZE, sizes.getBuffer(), null);
        Utils.checkError(ret, "clGetProgramInfo: CL_PROGRAM_BINARY_SIZES");

        final ByteBuffer binaries = Buffers.newDirectByteBuffer((int) sizes.get(index));
        final PointerBuffer addresses = PointerBuffer.allocateDirect(numDevices);
        for (int i=0; i<numDevices; ++i) {
            if (index == i) {
                addresses.referenceBuffer(binaries);
            } else {
                addresses.put(0);
            }
        }
        addresses.rewind();

        ret = cl.clGetProgramInfo(program, CL.CL_PROGRAM_BINARIES, numDevices * PointerBuffer.ELEMENT_SIZE, addresses.getBuffer(), null);
        Utils.checkError(ret, "clGetProgramInfo: CL_PROGRAM_BINARIES");

        return binaries;
    }

    private static class ReleaserImpl implements ObjectReleaser {
        private long program;
        private ReleaserImpl(long program) {
            this.program = program;
        }
        @Override
        public void release() {
            if (program != 0) {
                int ret = CLPlatform.getLowLevelCLInterface().clReleaseProgram(program);
                program = 0;
                Utils.reportError(ret, "clReleaseProgram");
            }
        }
    }
}
