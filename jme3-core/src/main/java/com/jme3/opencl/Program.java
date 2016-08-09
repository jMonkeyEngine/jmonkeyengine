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
package com.jme3.opencl;

import java.nio.ByteBuffer;

/**
 * A wrapper for an OpenCL program. A program is created from kernel source code,
 * manages the build process and creates the kernels.
 * <p>
 * Warning: Creating the same kernel more than one leads to undefined behaviour,
 * this is especially important for {@link #createAllKernels() }
 * 
 * @see Context#createProgramFromSourceCode(java.lang.String) 
 * @see #createKernel(java.lang.String) 
 * @author shaman
 */
public abstract class Program extends AbstractOpenCLObject {

    protected Program(ObjectReleaser releaser) {
        super(releaser);
    }
	
	@Override
	public Program register() {
		super.register();
		return this;
	}
    
    /**
     * Builds this program with the specified argument string on the specified
     * devices.
     * Please see the official OpenCL specification for a definition of
     * all supported arguments.
     * The list of devices specify on which device the compiled program
     * can then be executed. It must be a subset of {@link Context#getDevices() }.
     * If {@code null} is passed, the program is built on all available devices.
     * 
     * @param args the compilation arguments
     * @param devices a list of devices on which the program is build.
     * @throws KernelCompilationException if the compilation fails
     * @see #build() 
     */
	public abstract void build(String args, Device... devices) throws KernelCompilationException;
    /**
     * Builds this program without additional arguments
     * @throws KernelCompilationException if the compilation fails
     * @see #build(java.lang.String) 
     */
	public void build() throws KernelCompilationException {
        build("", (Device[]) null);
    }

    /**
     * Creates the kernel with the specified name.
     * @param name the name of the kernel as defined in the source code
     * @return the kernel object
     * @throws OpenCLException if the kernel was not found or some other
     * error occured
     */
	public abstract Kernel createKernel(String name);
    
    /**
     * Creates all available kernels in this program.
     * The names of the kernels can then by queried by {@link Kernel#getName() }.
     * @return an array of all kernels
     */
	public abstract Kernel[] createAllKernels();
    
    /**
     * Queries a compiled binary representation of this program for a particular
     * device. This binary can then be used e.g. in the next application launch
     * to create the program from the binaries and not from the sources.
     * This saves time.
     * @param device the device from which the binaries are taken
     * @return the binaries
     * @see Context#createProgramFromBinary(java.nio.ByteBuffer, com.jme3.opencl.Device) 
     */
    public abstract ByteBuffer getBinary(Device device);
    
}
