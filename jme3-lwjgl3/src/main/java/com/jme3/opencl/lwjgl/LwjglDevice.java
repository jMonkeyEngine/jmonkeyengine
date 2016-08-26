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

import com.jme3.opencl.Device;
import com.jme3.opencl.Platform;
import com.jme3.opencl.lwjgl.info.Info;
import java.util.Arrays;
import java.util.Collection;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL11;

/**
 *
 * @author shaman
 */
public final class LwjglDevice implements Device {

    final long device;
    final LwjglPlatform platform;

    public LwjglDevice(long device, LwjglPlatform platform) {
        this.device = device;
        this.platform = platform;
    }
    
    public long getDevice() {
        return device;
    }
    
    @Override
    public LwjglPlatform getPlatform() {
        return platform;
    }

    @Override
    public DeviceType getDeviceType() {
        int type = Info.clGetDeviceInfoInt(device, CL10.CL_DEVICE_TYPE);
        switch (type) {
            case CL10.CL_DEVICE_TYPE_ACCELERATOR: return DeviceType.ACCELEARTOR;
            case CL10.CL_DEVICE_TYPE_CPU: return DeviceType.CPU;
            case CL10.CL_DEVICE_TYPE_GPU: return DeviceType.GPU;
            default: return DeviceType.DEFAULT;
        }
    }

    @Override
    public int getVendorId() {
        return Info.clGetDeviceInfoInt(device, CL10.CL_DEVICE_VENDOR_ID);
    }

    @Override
    public boolean isAvailable() {
        return Info.clGetDeviceInfoBoolean(device, CL10.CL_DEVICE_AVAILABLE);
    }

    @Override
    public boolean hasCompiler() {
        return Info.clGetDeviceInfoBoolean(device, CL10.CL_DEVICE_COMPILER_AVAILABLE);
    }

    @Override
    public boolean hasDouble() {
        return hasExtension("cl_khr_fp64");
    }

    @Override
    public boolean hasHalfFloat() {
        return hasExtension("cl_khr_fp16");
    }

    @Override
    public boolean hasErrorCorrectingMemory() {
        return Info.clGetDeviceInfoBoolean(device, CL10.CL_DEVICE_ERROR_CORRECTION_SUPPORT);
    }

    @Override
    public boolean hasUnifiedMemory() {
        return Info.clGetDeviceInfoBoolean(device, CL11.CL_DEVICE_HOST_UNIFIED_MEMORY);
    }

    @Override
    public boolean hasImageSupport() {
        return Info.clGetDeviceInfoBoolean(device, CL10.CL_DEVICE_IMAGE_SUPPORT);
    }
    
    @Override
    public boolean hasWritableImage3D() {
        return hasExtension("cl_khr_3d_image_writes");
    }

    @Override
    public boolean hasOpenGLInterop() {
        return hasExtension("cl_khr_gl_sharing");
    }
    
    @Override
    public boolean hasExtension(String extension) {
        return getExtensions().contains(extension);
    }

    @Override
    public Collection<? extends String> getExtensions() {
        return Arrays.asList(Info.clGetDeviceInfoStringASCII(device, CL10.CL_DEVICE_EXTENSIONS).split(" "));
    }

    @Override
    public int getComputeUnits() {
        return Info.clGetDeviceInfoInt(device, CL10.CL_DEVICE_MAX_COMPUTE_UNITS);
    }

    @Override
    public int getClockFrequency() {
        return Info.clGetDeviceInfoInt(device, CL10.CL_DEVICE_MAX_CLOCK_FREQUENCY);
    }

    @Override
    public int getAddressBits() {
        return Info.clGetDeviceInfoInt(device, CL10.CL_DEVICE_ADDRESS_BITS);
    }

    @Override
    public boolean isLittleEndian() {
        return Info.clGetDeviceInfoBoolean(device, CL10.CL_DEVICE_ENDIAN_LITTLE);
    }

    @Override
    public long getMaximumWorkItemDimensions() {
        return Info.clGetDeviceInfoInt(device, CL10.CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
    }

    @Override
    public long[] getMaximumWorkItemSizes() {
        int dim = (int) getMaximumWorkItemDimensions();
        PointerBuffer sizes = PointerBuffer.allocateDirect(dim);
        Info.clGetDeviceInfoPointers(device, CL10.CL_DEVICE_MAX_WORK_ITEM_SIZES, sizes);
        long[] sx = new long[dim];
        sizes.get(sx);
        return sx;
    }

    @Override
    public long getMaxiumWorkItemsPerGroup() {
        return Info.clGetDeviceInfoPointer(device, CL10.CL_DEVICE_MAX_WORK_GROUP_SIZE);
    }

    @Override
    public int getMaximumSamplers() {
        return Info.clGetDeviceInfoInt(device, CL10.CL_DEVICE_MAX_SAMPLERS);
    }

    @Override
    public int getMaximumReadImages() {
        return Info.clGetDeviceInfoInt(device, CL10.CL_DEVICE_MAX_READ_IMAGE_ARGS);
    }

    @Override
    public int getMaximumWriteImages() {
        return Info.clGetDeviceInfoInt(device, CL10.CL_DEVICE_MAX_WRITE_IMAGE_ARGS);
    }

    @Override
    public long[] getMaximumImage2DSize() {
        return new long[] {
            Info.clGetDeviceInfoPointer(device, CL10.CL_DEVICE_IMAGE2D_MAX_WIDTH),
            Info.clGetDeviceInfoPointer(device, CL10.CL_DEVICE_IMAGE2D_MAX_HEIGHT)
        };
    }

    @Override
    public long[] getMaximumImage3DSize() {
        return new long[] {
            Info.clGetDeviceInfoPointer(device, CL10.CL_DEVICE_IMAGE3D_MAX_WIDTH),
            Info.clGetDeviceInfoPointer(device, CL10.CL_DEVICE_IMAGE3D_MAX_HEIGHT),
            Info.clGetDeviceInfoPointer(device, CL10.CL_DEVICE_IMAGE3D_MAX_DEPTH)
        };
    }
    
    @Override
    public long getMaximumAllocationSize() {
        return Info.clGetDeviceInfoLong(device, CL10.CL_DEVICE_MAX_MEM_ALLOC_SIZE);
    }
    
    @Override
    public long getGlobalMemorySize() {
        return Info.clGetDeviceInfoLong(device, CL10.CL_DEVICE_GLOBAL_MEM_SIZE);
    }
    
    @Override
    public long getLocalMemorySize() {
        return Info.clGetDeviceInfoLong(device, CL10.CL_DEVICE_LOCAL_MEM_SIZE);
    }
    
    @Override
    public long getMaximumConstantBufferSize() {
        return Info.clGetDeviceInfoLong(device, CL10.CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE);
    }
    
    @Override
    public int getMaximumConstantArguments() {
        return Info.clGetDeviceInfoInt(device, CL10.CL_DEVICE_MAX_CONSTANT_ARGS);
    }

    @Override
    public String getProfile() {
        return Info.clGetDeviceInfoStringASCII(device, CL10.CL_DEVICE_PROFILE);
    }

    @Override
    public String getVersion() {
        return  Info.clGetDeviceInfoStringASCII(device, CL10.CL_DEVICE_VERSION);
    }

    @Override
    public int getVersionMajor() {
        return Utils.getMajorVersion(getVersion(), "OpenCL ");
    }

    @Override
    public int getVersionMinor() {
        return Utils.getMinorVersion(getVersion(), "OpenCL ");
    }

    @Override
    public String getCompilerVersion() {
        return  Info.clGetDeviceInfoStringASCII(device, CL11.CL_DEVICE_OPENCL_C_VERSION);
    }

    @Override
    public int getCompilerVersionMajor() {
        return Utils.getMajorVersion(getCompilerVersion(), "OpenCL C ");
    }

    @Override
    public int getCompilerVersionMinor() {
        return Utils.getMinorVersion(getCompilerVersion(), "OpenCL C ");
    }

    @Override
    public String getDriverVersion() {
        return  Info.clGetDeviceInfoStringASCII(device, CL10.CL_DRIVER_VERSION);
    }

    @Override
    public int getDriverVersionMajor() {
        return Utils.getMajorVersion(getDriverVersion(), "");
    }

    @Override
    public int getDriverVersionMinor() {
        return Utils.getMinorVersion(getDriverVersion(), "");
    }

    @Override
    public String getName() {
        return  Info.clGetDeviceInfoStringASCII(device, CL10.CL_DEVICE_NAME);
    }

    @Override
    public String getVendor() {
        return  Info.clGetDeviceInfoStringASCII(device, CL10.CL_DEVICE_VENDOR);
    }

    @Override
    public String toString() {
        return getName();
    }
    
}
