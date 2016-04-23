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

import com.jme3.opencl.Kernel;
import com.jme3.opencl.KernelCompilationException;
import com.jme3.opencl.OpenCLObjectManager;
import com.jme3.opencl.Program;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;

/**
 *
 * @author Sebastian Weiss
 */
public class LwjglProgram implements Program {
    private static final Logger LOG = Logger.getLogger(LwjglProgram.class.getName());
    
    private final CLProgram program;
    private final LwjglContext context;

    public LwjglProgram(CLProgram program, LwjglContext context) {
        this.program = program;
        this.context = context;
        OpenCLObjectManager.getInstance().registerObject(this);
    }

    public CLProgram getProgram() {
        return program;
    }

    @Override
    public void build(String args) throws KernelCompilationException {
        int ret = CL10.clBuildProgram(program, (PointerBuffer) null, args, null);
        if (ret != CL10.CL_SUCCESS) {
            String log = Log();
            LOG.log(Level.WARNING, "Unable to compile program:\n{0}", log);
            if (ret == CL10.CL_BUILD_PROGRAM_FAILURE) {
                throw new KernelCompilationException("Failed to build program", ret, log);
            } else {
                Utils.checkError(ret, "clBuildProgram");
            }
        } else {
            LOG.log(Level.INFO, "Program compiled:\n{0}", Log());
        }
    }

    @Override
    public void build() throws KernelCompilationException {
        build("");
    }
    
    private String Log() {
        StringBuilder str = new StringBuilder();
        for (LwjglDevice device : context.getDevices()) {
            CLDevice d = device.getDevice();
            str.append(device.getName()).append(":\n");
            str.append(program.getBuildInfoString(d, CL10.CL_PROGRAM_BUILD_LOG));
            str.append('\n');
        }
        return str.toString();
    }

    @Override
    public Kernel createKernel(String name) {
        CLKernel kernel = CL10.clCreateKernel(program, name, Utils.errorBuffer);
        Utils.checkError(Utils.errorBuffer, "clCreateKernel");
        return new LwjglKernel(kernel);
    }

    @Override
    public Kernel[] createAllKernels() {
        CLKernel[] kernels = program.createKernelsInProgram();
        Kernel[] kx = new Kernel[kernels.length];
        for (int i=0; i<kernels.length; ++i) {
            kx[i] = new LwjglKernel(kernels[i]);
        }
        return kx;
    }

    @Override
    public ObjectReleaser getReleaser() {
        return new ReleaserImpl(program);
    }
    private static class ReleaserImpl implements ObjectReleaser {
        private CLProgram program;
        private ReleaserImpl(CLProgram program) {
            this.program = program;
        }
        @Override
        public void release() {
            //LWJGL Bug: releasing a program also released every! kernel associated with that program
            /*
            if (program != null) {
                int ret = CL10.clReleaseProgram(program);
                program = null;
                Utils.reportError(ret, "clReleaseProgram");
            }
            */
        }
    }
}
