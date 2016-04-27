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

import java.util.Collection;

/**
 * Represents a hardware device actually running the OpenCL kernels.
 * A {@link Context} can be accociated with multiple {@code Devices}
 * that all belong to the same {@link Platform}.
 * For execution, a single device must be chosen and passed to a command
 * queue ({@link Context#createQueue(com.jme3.opencl.Device) }).
 * <p>
 * This class is used to query the capabilities of the underlying device.
 * 
 * @author shaman
 */
public interface Device {
	
    /**
     * @return the platform accociated with this device
     */
    Platform getPlatform();
    
    /**
     * The device type
     */
	public static enum DeviceType {
		DEFAULT,
		CPU,
		GPU,
		ACCELEARTOR,
		ALL
	}
    /**
     * @return queries the device type
     */
	DeviceType getDeviceType();
    /**
     * @return the vendor id
     */
	int getVendorId();
    /**
     * checks if this device is available at all, must always be tested
     * @return checks if this device is available at all, must always be tested
     */
	boolean isAvailable();
	
    /**
     * @return if this device has a compiler for kernel code
     */
	boolean hasCompiler();
    /**
     * @return supports double precision floats (64 bit)
     */
	boolean hasDouble();
    /**
     * @return supports half precision floats (16 bit)
     */
	boolean hasHalfFloat();
    /**
     * @return supports error correction for every access to global or constant memory
     */
	boolean hasErrorCorrectingMemory();
    /**
     * @return supports unified virtual memory (OpenCL 2.0)
     */
	boolean hasUnifiedMemory();
    /**
     * @return supports images
     */
	boolean hasImageSupport();
    /**
     * @return supports writes to 3d images (this is an extension)
     */
    boolean hasWritableImage3D();
    /**
     * @return supports sharing with OpenGL
     */
    boolean hasOpenGLInterop();
    /**
     * Explicetly tests for the availability of the specified extension
     * @param extension the name of the extension
     * @return {@code true} iff this extension is supported
     */
	boolean hasExtension(String extension);
    /**
     * Lists all available extensions
     * @return all available extensions
     */
	Collection<? extends String> getExtensions();
	
    /**
     * Returns the number of parallel compute units on
     * the OpenCL device. A work-group
     * executes on a single compute unit. The
     * minimum value is 1.
     * @return the number of parallel compute units
     * @see #getMaximumWorkItemDimensions() 
     * @see #getMaximumWorkItemSizes() 
     */
	int getComputeUnits();
    /**
     * @return maximum clock frequency of the device in MHz
     */
	int getClockFrequency();
    /**
     * Returns the default compute device address space
     * size specified as an unsigned integer value
     * in bits. Currently supported values are 32
     * or 64 bits.
     * @return the size of an adress
     */
	int getAddressBits();
    /**
     * @return {@code true} if this device is little endian
     */
	boolean isLittleEndian();
	
    /**
     * The maximum dimension that specify the local and global work item ids.
     * You can always assume to be this at least 3.
     * Therefore, the ids are always three integers x,y,z.
     * @return the maximum dimension of work item ids
     */
	long getMaximumWorkItemDimensions();
    /**
     * Maximum number of work-items that can be specified in each dimension of the
     * work-group to {@link Kernel#Run2(com.jme3.opencl.CommandQueue, com.jme3.opencl.WorkSize, com.jme3.opencl.WorkSize, java.lang.Object...) }.
     * The array has a length of at least 3.
     * @return the maximum size of the work group in each dimension
     */
	long[] getMaximumWorkItemSizes();
    /**
     * Maximum number of work-items in a
     * work-group executing a kernel on a single
     * compute unit, using the data parallel
     * execution model.
     * @return maximum number of work-items in a work-group
     */
	long getMaxiumWorkItemsPerGroup();
	
    /**
     * @return the maximum number of samples that can be used in a kernel
     */
	int getMaximumSamplers();
    /**
     * @return the maximum number of images that can be used for reading in a kernel
     */
	int getMaximumReadImages();
    /**
     * @return the maximum number of images that can be used for writing in a kernel
     */
	int getMaximumWriteImages();
    /**
     * Queries the maximal size of a 2D image
     * @return an array of length 2 with the maximal size of a 2D image
     */
	long[] getMaximumImage2DSize();
    /**
     * Queries the maximal size of a 3D image
     * @return an array of length 3 with the maximal size of a 3D image
     */
	long[] getMaximumImage3DSize();
	
    /**
     * @return the maximal size of a memory object (buffer and image) in bytes
     */
    long getMaximumAllocationSize();
    /**
     * @return the total available global memory in bytes
     */
    long getGlobalMemorySize();
    /**
     * @return the total available local memory in bytes
     */
    long getLocalMemorySize();
    /**
     * Returns the maximal size of a constant buffer.
     * <br>
     * Constant buffers are normal buffer objects, but passed to the kernel
     * with the special declaration {@code __constant BUFFER_TYPE* BUFFER_NAME}.
     * Because they have a special caching, their size is usually very limited.
     * 
     * @return the maximal size of a constant buffer
     */
    long getMaximumConstantBufferSize();
    /**
     * @return the maximal number of constant buffer arguments in a kernel call
     */
    int getMaximumConstantArguments();
    
	//TODO: cache, prefered sizes properties
    /**
     * OpenCL profile string. Returns the profile name supported by the device.
     * The profile name returned can be one of the following strings:<br>
     * FULL_PROFILE – if the device supports the OpenCL specification
     * (functionality defined as part of the core specification and does not
     * require any extensions to be supported).<br>
     * EMBEDDED_PROFILE - if the device supports the OpenCL embedded profile.
     *
     * @return the profile string
     */
	String getProfile();
    /**
     * OpenCL version string. Returns the OpenCL version supported by the
     * device. This version string has the following format: OpenCL space
     * major_version.minor_version space vendor-specific information.
     * <br>
     * E.g. OpenCL 1.1, OpenCL 1.2, OpenCL 2.0
     *
     * @return the version string
     */
	String getVersion();
    /**
     * Extracts the major version from the version string
     * @return the major version
     * @see #getVersion() 
     */
	int getVersionMajor();
    /**
     * Extracts the minor version from the version string
     * @return the minor version
     * @see #getVersion() }
     */
	int getVersionMinor();
    
    /**
     * OpenCL C version string. Returns the highest OpenCL C version supported
     * by the compiler for this device that is not of type
     * CL_DEVICE_TYPE_CUSTOM. This version string has the following format:
     * OpenCL space C space major_version.minor_version space vendor-specific
     * information.<br>
     * The major_version.minor_version value returned must be 1.2 if
     * CL_DEVICE_VERSION is OpenCL 1.2. The major_version.minor_version value
     * returned must be 1.1 if CL_DEVICE_VERSION is OpenCL 1.1. The
     * major_version.minor_version value returned can be 1.0 or 1.1 if
     * CL_DEVICE_VERSION is OpenCL 1.0.
     *
     * @return the compiler version
     */
	String getCompilerVersion();
    /**
     * Extracts the major version from the compiler version
     * @return the major compiler version
     * @see #getCompilerVersion() 
     */
	int getCompilerVersionMajor();
    /**
     * Extracts the minor version from the compiler version
     * @return the minor compiler version
     * @see #getCompilerVersion() 
     */
	int getCompilerVersionMinor();
    /**     
     * @return the OpenCL software driver version string in the form
     * major_number.minor_number
     */
	String getDriverVersion();
    /**
     * Extracts the major version from the driver version
     * @return the major driver version
     * @see #getDriverVersion() 
     */
	int getDriverVersionMajor();
    /**
     * Extracts the minor version from the driver version
     * @return the minor driver version
     * @see #getDriverVersion() 
     */
	int getDriverVersionMinor();
    
    /**
     * @return the device name
     */
	String getName();
    /**
     * @return the vendor
     */
	String getVendor();

}
