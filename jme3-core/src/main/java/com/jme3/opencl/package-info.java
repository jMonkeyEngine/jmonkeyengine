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

/**
 * This package contains an API for using OpenCL together with jME3.
 * <p>
 * <b>Activation:</b><br>
 * OpenCL is deactivated by default. To activate it, set {@link com.jme3.system.AppSettings#setOpenCLSupport(boolean) }
 * to {@code true}.
 * If the current platform supports OpenCL, then the central {@link com.jme3.opencl.Context} 
 * can be fetched by {@link com.jme3.system.JmeContext#getOpenCLContext() } which is
 * available in each application. If OpenCL is deactivated or not available, 
 * this method returns {@code null}.
 * 
 * <p>
 * <b>First steps:</b><br>
 * Once you have obtained your {@link com.jme3.opencl.Context} you start by
 * creating a {@link com.jme3.opencl.CommandQueue} by calling 
 * {@link com.jme3.opencl.Context#createQueue() } or alternative versions.
 * The command queue must be passed to every following method that execute
 * some action involving the GPU. All actions are executed in the order in which they
 * are added to the queue.
 * <br>
 * <b>Programs and Kernels:</b>
 * The main purpose of OpenCL is to execute code in parallel
 * on the GPU. From the source code, a {@link com.jme3.opencl.Program} object
 * is created by {@link com.jme3.opencl.Context#createProgramFromSourceCode(java.lang.String) },
 * {@link com.jme3.opencl.Context#createProgramFromSourceFilesWithInclude(com.jme3.asset.AssetManager, java.lang.String, java.util.List) }
 * or alternative versions.
 * Before using it, the source code must be build using {@link com.jme3.opencl.Program#build() }.
 * Any compilation error is thrown here. Each program consists of multiple kernels.
 * Each kernel represents one executable unit and is declared in the source code
 * with the following syntax: {@code __kernel void KernelName(KernelArgs) {Code} }.
 * On the programming side, a {@link com.jme3.opencl.Kernel} instance is obtained
 * by calling {@link com.jme3.opencl.Program#createKernel(java.lang.String) }.
 * To execute the kernel, the method {@link com.jme3.opencl.Kernel#Run1(com.jme3.opencl.CommandQueue, com.jme3.opencl.WorkSize, java.lang.Object...) }
 * is provided. You first pass the command queue and the work size (i.e. the number of parallel executed threads)
 * followed by the kernel arguments.
 * <br>
 * <b>Buffers and Images:</b>
 * OpenCL Kernels show their true power first when they operate on buffers and images.
 * Buffers are simple one dimensional consecutive chunks of memory of arbitrary size.
 * These {@link com.jme3.opencl.Buffer} instances are created by calling
 * {@link com.jme3.opencl.Context#createBuffer(long)} with the size in bytes as
 * the argument. A buffer on its own is typeless. In the kernel, you then specify
 * the type of the buffer by argument declarations like {@code __global float* buffer}.
 * Note that OpenCL does not check buffer boundaries. If you read or write outside
 * of the buffer, the behavior is completely undefined and may often result in
 * a program cache later on.
 * {@link com.jme3.opencl.Image} objects are structured one, two or three dimensional
 * memory chunks of a fixed type. They are created by 
 * {@link com.jme3.opencl.Context#createImage(com.jme3.opencl.MemoryAccess, com.jme3.opencl.Image.ImageFormat, com.jme3.opencl.Image.ImageDescriptor, java.nio.ByteBuffer) }.
 * They need special functions in the kernel code to write to or read from images.
 * Both buffer and image objects provide methods for copying between buffers and images,
 * reading and writing to host code and directly mapping memory parts to the host code.
 * <br>
 * <b>Events:</b>
 * Most methods are provided in two variations: blocking calls or asynchronous
 * calls (the later one have the suffix -Async, or all kernel calls).
 * These async calls all return {@link com.jme3.opencl.Event} objects.
 * These events can be used to check (non-blocking) if the action has completed, e.g. a memory copy
 * is finished, or to block the execution until the action has finished.
 * <br>
 * Some methods have the suffix {@code -NoEvent}. This means that these methods
 * don't return an event object even if the OpenCL function would return an event.
 * There exists always an alternative version that does return an event.
 * These methods exist to increase the performance: since all actions (like multiple kernel calls)
 * that are sent to the same command queue are executed in order, there is no
 * need for intermediate events. (These intermediate events would be released
 * immediately). Therefore, the no-event alternatives increase the performance
 * because no additional event object has to be allocated and less system calls
 * are neccessary.
 * 
 * <p>
 * <b>Interoperability between OpenCL and jME3:</b><br>
 * This Wrapper allows to share jME3 Images and VertexBuffers with OpenCL.<br>
 * {@link com.jme3.scene.VertexBuffer} objects can be shared with OpenCL
 * by calling {@link com.jme3.opencl.Context#bindVertexBuffer(com.jme3.scene.VertexBuffer, com.jme3.opencl.MemoryAccess) }
 * resulting in a {@link com.jme3.opencl.Buffer} object. This buffer object
 * can then be used as usual, allowing e.g. the dynamic modification of position buffers for particle systems.<br>
 * {@link com.jme3.texture.Image} and {@link com.jme3.texture.Texture} objects can be used in OpenCL with the method
 * {@link com.jme3.opencl.Context#bindImage(com.jme3.texture.Texture, com.jme3.opencl.MemoryAccess) }
 * or variations of this method. The same holds for {@link com.jme3.texture.FrameBuffer.RenderBuffer} objects
 * using {@link com.jme3.opencl.Context#bindRenderBuffer(com.jme3.texture.FrameBuffer.RenderBuffer, com.jme3.opencl.MemoryAccess) }.
 * These methods result in an OpenCL-Image. Usages are e.g. animated textures,
 * terrain based on height maps, post processing effects and so forth.
 * <br>
 * <i>Important:</i> Before shared objects can be used by any OpenCL function
 * like kernel calls or read/write/copy methods, they must be aquired explicitly
 * by {@link com.jme3.opencl.Buffer#acquireBufferForSharingAsync(com.jme3.opencl.CommandQueue) }
 * or {@link com.jme3.opencl.Image#acquireImageForSharingAsync(com.jme3.opencl.CommandQueue) }.
 * After the work is done, release the resource with
 * {@link com.jme3.opencl.Buffer#releaseBufferForSharingAsync(com.jme3.opencl.CommandQueue) }
 * or {@link com.jme3.opencl.Image#releaseImageForSharingAsync(com.jme3.opencl.CommandQueue) }.
 * This ensures the synchronization of OpenCL and OpenGL.
 * 
 * <p>
 * <b>Experts: choosing the right platform and devices</b><br>
 * OpenCL can run on different platforms and different devices. On some systems,
 * like multi-GPU setups, this choice really matters. To specify which platform
 * and which devices are used, a custom implementation of 
 * {@link com.jme3.opencl.PlatformChooser} can be used by calling
 * {@link com.jme3.system.AppSettings#setOpenCLPlatformChooser(java.lang.Class) }.
 * For more details, see the documentation of {@code PlatformChooser}.
 * 
 * <p>
 * <b>Exception handling:</b><br>
 * All OpenCL-wrapper classes in this package
 * (this includes {@link com.jme3.opencl.Platform}, {@link com.jme3.opencl.Device},
 * {@link com.jme3.opencl.Context}, {@link com.jme3.opencl.CommandQueue},
 * {@link com.jme3.opencl.Buffer}, {@link com.jme3.opencl.Image},
 * {@link com.jme3.opencl.Program}, {@link com.jme3.opencl.Kernel} and
 * {@link com.jme3.opencl.Event})
 * may throw the following exceptions in each method without being mentioned 
 * explicetly in the documentation:
 * <ul>
 * <li>{@code NullPointerException}: one of the arguments is {@code null} and 
 * {@code null} is not allowed</li>
 * <li>{@code IllegalArgumentException}: the arguments don't follow the rules
 * as specified in the documentation of the method, e.g. values are out of range
 * or an array has the wrong size</li>
 * <li>{@link com.jme3.opencl.OpenCLException}: some low-level exception was
 * thrown. The exception always records the error code and error name and the 
 * OpenCL function call where the error was detected. Please check the official
 * OpenCL specification for the meanings of these errors for that particular function.</li>
 * <li>{@code UnsupportedOperationException}: the OpenCL implementation does not
 * support some operations. This is currently only an issue for Jogamp's Jogl
 * renderer, since Jocl only supports OpenCL 1.1. LWJGL has full support for
 * OpenCL 1.2 and 2.0.
 * </ul>
 */
package com.jme3.opencl;

//TODO: add profiling to Kernel, CommandQueue
