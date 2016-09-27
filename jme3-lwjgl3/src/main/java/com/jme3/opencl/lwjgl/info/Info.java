/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 * MACHINE GENERATED FILE, DO NOT EDIT
 */
package com.jme3.opencl.lwjgl.info;

import org.lwjgl.PointerBuffer;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL12.*;
import static org.lwjgl.opencl.CL20.*;
import static org.lwjgl.opencl.CL10GL.*;

/**
 * This class provides methods that can be used to query information about
 * OpenCL objects. These methods are similar to the corresponding
 * {@code clGet&lt;Type&gt;Info} function for each object type, except that only
 * a single value is returned. Which one of these methods should be used depends
 * on the type of the information being queried.
 */
public final class Info {

    private Info() {
    }

    // ------------------------------------
    // Platform (CL10.clGetPlatformInfo)
    // ------------------------------------
    private static final InfoQuery PLATFORM = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetPlatformInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * String version of: {@link CL10#clGetPlatformInfo GetPlatformInfo}
     */
    public static String clGetPlatformInfoStringASCII(long platform, int param_name) {
        return PLATFORM.getStringASCII(platform, param_name);
    }

    /**
     * String with explicit length version of: {@link CL10#clGetPlatformInfo GetPlatformInfo}
     */
    public static String clGetPlatformInfoStringASCII(long platform, int param_name, int param_value_size) {
        return PLATFORM.getStringASCII(platform, param_name, param_value_size);
    }

    /**
     * UTF-8 string version of: {@link CL10#clGetPlatformInfo GetPlatformInfo}
     */
    public static String clGetPlatformInfoStringUTF8(long platform, int param_name) {
        return PLATFORM.getStringUTF8(platform, param_name);
    }

    /**
     * UTF-8 string with explicit length version of:
     * {@link CL10#clGetPlatformInfo GetPlatformInfo}
     */
    public static String clGetPlatformInfoStringUTF8(long platform, int param_name, int param_value_size) {
        return PLATFORM.getStringUTF8(platform, param_name, param_value_size);
    }

    // ------------------------------------
    // Device (CL10.clGetDeviceInfo)
    // ------------------------------------
    private static final InfoQuery DEVICE = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetDeviceInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single boolean value version of:
     * {@link CL10#clGetDeviceInfo GetDeviceInfo}
     */
    public static boolean clGetDeviceInfoBoolean(long device, int param_name) {
        return DEVICE.getBoolean(device, param_name);
    }

    /**
     * Single int value version of: {@link CL10#clGetDeviceInfo GetDeviceInfo}
     */
    public static int clGetDeviceInfoInt(long device, int param_name) {
        return DEVICE.getInt(device, param_name);
    }

    /**
     * Single long value version of: {@link CL10#clGetDeviceInfo GetDeviceInfo}
     */
    public static long clGetDeviceInfoLong(long device, int param_name) {
        return DEVICE.getLong(device, param_name);
    }

    /**
     * Single pointer value version of:
     * {@link CL10#clGetDeviceInfo GetDeviceInfo}
     */
    public static long clGetDeviceInfoPointer(long device, int param_name) {
        return DEVICE.getPointer(device, param_name);
    }

    /**
     * PointBuffer version of: {@link CL10#clGetDeviceInfo GetDeviceInfo}
     */
    public static int clGetDeviceInfoPointers(long device, int param_name, PointerBuffer target) {
        return DEVICE.getPointers(device, param_name, target);
    }

    /**
     * String version of: {@link CL10#clGetDeviceInfo GetDeviceInfo}
     */
    public static String clGetDeviceInfoStringASCII(long device, int param_name) {
        return DEVICE.getStringASCII(device, param_name);
    }

    /**
     * String with explicit length version of: {@link CL10#clGetDeviceInfo GetDeviceInfo}
     */
    public static String clGetDeviceInfoStringASCII(long device, int param_name, int param_value_size) {
        return DEVICE.getStringASCII(device, param_name, param_value_size);
    }

    /**
     * UTF-8 string version of: {@link CL10#clGetDeviceInfo GetDeviceInfo}
     */
    public static String clGetDeviceInfoStringUTF8(long device, int param_name) {
        return DEVICE.getStringUTF8(device, param_name);
    }

    /**
     * UTF-8 string with explicit length version of:
     * {@link CL10#clGetDeviceInfo GetDeviceInfo}
     */
    public static String clGetDeviceInfoStringUTF8(long device, int param_name, int param_value_size) {
        return DEVICE.getStringUTF8(device, param_name, param_value_size);
    }

    // ------------------------------------
    // Context (CL10.clGetContextInfo)
    // ------------------------------------
    private static final InfoQuery CONTEXT = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetContextInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single int value version of: {@link CL10#clGetContextInfo GetContextInfo}
     */
    public static int clGetContextInfoInt(long context, int param_name) {
        return CONTEXT.getInt(context, param_name);
    }

    /**
     * Single pointer value version of:
     * {@link CL10#clGetContextInfo GetContextInfo}
     */
    public static long clGetContextInfoPointer(long context, int param_name) {
        return CONTEXT.getPointer(context, param_name);
    }

    /**
     * PointBuffer version of: {@link CL10#clGetContextInfo GetContextInfo}
     */
    public static int clGetContextInfoPointers(long context, int param_name, PointerBuffer target) {
        return CONTEXT.getPointers(context, param_name, target);
    }

    // ------------------------------------
    // Command Queue (CL10.clGetCommandQueueInfo)
    // ------------------------------------
    private static final InfoQuery COMMAND_QUEUE = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetCommandQueueInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single int value version of:
     * {@link CL10#clGetCommandQueueInfo GetCommandQueueInfo}
     */
    public static int clGetCommandQueueInfoInt(long command_queue, int param_name) {
        return COMMAND_QUEUE.getInt(command_queue, param_name);
    }

    /**
     * Single pointer value version of:
     * {@link CL10#clGetCommandQueueInfo GetCommandQueueInfo}
     */
    public static long clGetCommandQueueInfoPointer(long command_queue, int param_name) {
        return COMMAND_QUEUE.getPointer(command_queue, param_name);
    }

    /**
     * PointBuffer version of:
     * {@link CL10#clGetCommandQueueInfo GetCommandQueueInfo}
     */
    public static int clGetCommandQueueInfoPointers(long command_queue, int param_name, PointerBuffer target) {
        return COMMAND_QUEUE.getPointers(command_queue, param_name, target);
    }

    // ------------------------------------
    // Mem Object (CL10.clGetMemObjectInfo)
    // ------------------------------------
    private static final InfoQuery MEM_OBJECT = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetMemObjectInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single boolean value version of:
     * {@link CL10#clGetMemObjectInfo GetMemObjectInfo}
     */
    public static boolean clGetMemObjectInfoBoolean(long memobj, int param_name) {
        return MEM_OBJECT.getBoolean(memobj, param_name);
    }

    /**
     * Single int value version of:
     * {@link CL10#clGetMemObjectInfo GetMemObjectInfo}
     */
    public static int clGetMemObjectInfoInt(long memobj, int param_name) {
        return MEM_OBJECT.getInt(memobj, param_name);
    }

    /**
     * Single long value version of:
     * {@link CL10#clGetMemObjectInfo GetMemObjectInfo}
     */
    public static long clGetMemObjectInfoLong(long memobj, int param_name) {
        return MEM_OBJECT.getLong(memobj, param_name);
    }

    /**
     * Single pointer value version of:
     * {@link CL10#clGetMemObjectInfo GetMemObjectInfo}
     */
    public static long clGetMemObjectInfoPointer(long memobj, int param_name) {
        return MEM_OBJECT.getPointer(memobj, param_name);
    }

    /**
     * PointBuffer version of: {@link CL10#clGetMemObjectInfo GetMemObjectInfo}
     */
    public static int clGetMemObjectInfoPointers(long memobj, int param_name, PointerBuffer target) {
        return MEM_OBJECT.getPointers(memobj, param_name, target);
    }

    // ------------------------------------
    // Image (CL10.clGetImageInfo)
    // ------------------------------------
    private static final InfoQuery IMAGE = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetImageInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single int value version of: {@link CL10#clGetImageInfo GetImageInfo}
     */
    public static int clGetImageInfoInt(long image, int param_name) {
        return IMAGE.getInt(image, param_name);
    }

    /**
     * Single pointer value version of: {@link CL10#clGetImageInfo GetImageInfo}
     */
    public static long clGetImageInfoPointer(long image, int param_name) {
        return IMAGE.getPointer(image, param_name);
    }

    /**
     * PointBuffer version of: {@link CL10#clGetImageInfo GetImageInfo}
     */
    public static int clGetImageInfoPointers(long image, int param_name, PointerBuffer target) {
        return IMAGE.getPointers(image, param_name, target);
    }

    // ------------------------------------
    // Pipe (CL20.clGetPipeInfo)
    // ------------------------------------
    private static final InfoQuery PIPE = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetPipeInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single int value version of: {@link CL20#clGetPipeInfo GetPipeInfo}
     */
    public static int clGetPipeInfoInt(long pipe, int param_name) {
        return PIPE.getInt(pipe, param_name);
    }

    // ------------------------------------
    // Program (CL10.clGetProgramInfo)
    // ------------------------------------
    private static final InfoQuery PROGRAM = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetProgramInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single int value version of: {@link CL10#clGetProgramInfo GetProgramInfo}
     */
    public static int clGetProgramInfoInt(long program, int param_name) {
        return PROGRAM.getInt(program, param_name);
    }

    /**
     * Single pointer value version of:
     * {@link CL10#clGetProgramInfo GetProgramInfo}
     */
    public static long clGetProgramInfoPointer(long program, int param_name) {
        return PROGRAM.getPointer(program, param_name);
    }

    /**
     * PointBuffer version of: {@link CL10#clGetProgramInfo GetProgramInfo}
     */
    public static int clGetProgramInfoPointers(long program, int param_name, PointerBuffer target) {
        return PROGRAM.getPointers(program, param_name, target);
    }

    /**
     * String version of: {@link CL10#clGetProgramInfo GetProgramInfo}
     */
    public static String clGetProgramInfoStringASCII(long program, int param_name) {
        return PROGRAM.getStringASCII(program, param_name);
    }

    /**
     * String with explicit length version of: {@link CL10#clGetProgramInfo GetProgramInfo}
     */
    public static String clGetProgramInfoStringASCII(long program, int param_name, int param_value_size) {
        return PROGRAM.getStringASCII(program, param_name, param_value_size);
    }

    /**
     * UTF-8 string version of: {@link CL10#clGetProgramInfo GetProgramInfo}
     */
    public static String clGetProgramInfoStringUTF8(long program, int param_name) {
        return PROGRAM.getStringUTF8(program, param_name);
    }

    /**
     * UTF-8 string with explicit length version of:
     * {@link CL10#clGetProgramInfo GetProgramInfo}
     */
    public static String clGetProgramInfoStringUTF8(long program, int param_name, int param_value_size) {
        return PROGRAM.getStringUTF8(program, param_name, param_value_size);
    }

    // ------------------------------------
    // Program Build (CL10.clGetProgramBuildInfo)
    // ------------------------------------
    private static final InfoQueryObject PROGRAM_BUILD = new InfoQueryObject() {
        @Override
        protected int get(long pointer, long arg, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetProgramBuildInfo(pointer, arg, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single int value version of:
     * {@link CL10#clGetProgramBuildInfo GetProgramBuildInfo}
     */
    public static int clGetProgramBuildInfoInt(long program, long device, int param_name) {
        return PROGRAM_BUILD.getInt(program, device, param_name);
    }

    /**
     * Single pointer value version of:
     * {@link CL10#clGetProgramBuildInfo GetProgramBuildInfo}
     */
    public static long clGetProgramBuildInfoPointer(long program, long device, int param_name) {
        return PROGRAM_BUILD.getPointer(program, device, param_name);
    }

    /**
     * PointBuffer version of:
     * {@link CL10#clGetProgramBuildInfo GetProgramBuildInfo}
     */
    public static int clGetProgramBuildInfoPointers(long program, long device, int param_name, PointerBuffer target) {
        return PROGRAM_BUILD.getPointers(program, device, param_name, target);
    }

    /**
     * String version of: {@link CL10#clGetProgramBuildInfo GetProgramBuildInfo}
     */
    public static String clGetProgramBuildInfoStringASCII(long program, long device, int param_name) {
        return PROGRAM_BUILD.getStringASCII(program, device, param_name);
    }

    /**
     * String with explicit length version of: {@link CL10#clGetProgramBuildInfo GetProgramBuildInfo}
     */
    public static String clGetProgramBuildInfoStringASCII(long program, long device, int param_name, int param_value_size) {
        return PROGRAM_BUILD.getStringASCII(program, device, param_name, param_value_size);
    }

    /**
     * UTF-8 string version of:
     * {@link CL10#clGetProgramBuildInfo GetProgramBuildInfo}
     */
    public static String clGetProgramBuildInfoStringUTF8(long program, long device, int param_name) {
        return PROGRAM_BUILD.getStringUTF8(program, device, param_name);
    }

    /**
     * UTF-8 string with explicit length version of:
     * {@link CL10#clGetProgramBuildInfo GetProgramBuildInfo}
     */
    public static String clGetProgramBuildInfoStringUTF8(long program, long device, int param_name, int param_value_size) {
        return PROGRAM_BUILD.getStringUTF8(program, device, param_name, param_value_size);
    }

    // ------------------------------------
    // Kernel (CL10.clGetKernelInfo)
    // ------------------------------------
    private static final InfoQuery KERNEL = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetKernelInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single int value version of: {@link CL10#clGetKernelInfo GetKernelInfo}
     */
    public static int clGetKernelInfoInt(long kernel, int param_name) {
        return KERNEL.getInt(kernel, param_name);
    }

    /**
     * Single pointer value version of:
     * {@link CL10#clGetKernelInfo GetKernelInfo}
     */
    public static long clGetKernelInfoPointer(long kernel, int param_name) {
        return KERNEL.getPointer(kernel, param_name);
    }

    /**
     * PointBuffer version of: {@link CL10#clGetKernelInfo GetKernelInfo}
     */
    public static int clGetKernelInfoPointers(long kernel, int param_name, PointerBuffer target) {
        return KERNEL.getPointers(kernel, param_name, target);
    }

    /**
     * String version of: {@link CL10#clGetKernelInfo GetKernelInfo}
     */
    public static String clGetKernelInfoStringASCII(long kernel, int param_name) {
        return KERNEL.getStringASCII(kernel, param_name);
    }

    /**
     * String with explicit length version of: {@link CL10#clGetKernelInfo GetKernelInfo}
     */
    public static String clGetKernelInfoStringASCII(long kernel, int param_name, int param_value_size) {
        return KERNEL.getStringASCII(kernel, param_name, param_value_size);
    }

    /**
     * UTF-8 string version of: {@link CL10#clGetKernelInfo GetKernelInfo}
     */
    public static String clGetKernelInfoStringUTF8(long kernel, int param_name) {
        return KERNEL.getStringUTF8(kernel, param_name);
    }

    /**
     * UTF-8 string with explicit length version of:
     * {@link CL10#clGetKernelInfo GetKernelInfo}
     */
    public static String clGetKernelInfoStringUTF8(long kernel, int param_name, int param_value_size) {
        return KERNEL.getStringUTF8(kernel, param_name, param_value_size);
    }

    // ------------------------------------
    // Kernel WorkGroup (CL10.clGetKernelWorkGroupInfo)
    // ------------------------------------
    private static final InfoQueryObject KERNEL_WORKGROUP = new InfoQueryObject() {
        @Override
        protected int get(long pointer, long arg, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetKernelWorkGroupInfo(pointer, arg, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single long value version of:
     * {@link CL10#clGetKernelWorkGroupInfo GetKernelWorkGroupInfo}
     */
    public static long clGetKernelWorkGroupInfoLong(long kernel, long device, int param_name) {
        return KERNEL_WORKGROUP.getLong(kernel, device, param_name);
    }

    /**
     * Single pointer value version of:
     * {@link CL10#clGetKernelWorkGroupInfo GetKernelWorkGroupInfo}
     */
    public static long clGetKernelWorkGroupInfoPointer(long kernel, long device, int param_name) {
        return KERNEL_WORKGROUP.getPointer(kernel, device, param_name);
    }

    /**
     * PointBuffer version of:
     * {@link CL10#clGetKernelWorkGroupInfo GetKernelWorkGroupInfo}
     */
    public static int clGetKernelWorkGroupInfoPointers(long kernel, long device, int param_name, PointerBuffer target) {
        return KERNEL_WORKGROUP.getPointers(kernel, device, param_name, target);
    }

    // ------------------------------------
    // Kernel Arg (CL12.clGetKernelArgInfo)
    // ------------------------------------
    private static final InfoQueryInt KERNEL_ARG = new InfoQueryInt() {
        @Override
        protected int get(long pointer, int arg, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetKernelArgInfo(pointer, arg, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single int value version of:
     * {@link CL12#clGetKernelArgInfo GetKernelArgInfo}
     */
    public static int clGetKernelArgInfoInt(long kernel, int arg_indx, int param_name) {
        return KERNEL_ARG.getInt(kernel, arg_indx, param_name);
    }

    /**
     * Single long value version of:
     * {@link CL12#clGetKernelArgInfo GetKernelArgInfo}
     */
    public static long clGetKernelArgInfoLong(long kernel, int arg_indx, int param_name) {
        return KERNEL_ARG.getLong(kernel, arg_indx, param_name);
    }

    /**
     * String version of: {@link CL12#clGetKernelArgInfo GetKernelArgInfo}
     */
    public static String clGetKernelArgInfoStringASCII(long kernel, int arg_indx, int param_name) {
        return KERNEL_ARG.getStringASCII(kernel, arg_indx, param_name);
    }

    /**
     * String with explicit length version of: {@link CL12#clGetKernelArgInfo GetKernelArgInfo}
     */
    public static String clGetKernelArgInfoStringASCII(long kernel, int arg_indx, int param_name, int param_value_size) {
        return KERNEL_ARG.getStringASCII(kernel, arg_indx, param_name, param_value_size);
    }

    /**
     * UTF-8 string version of: {@link CL12#clGetKernelArgInfo GetKernelArgInfo}
     */
    public static String clGetKernelArgInfoStringUTF8(long kernel, int arg_indx, int param_name) {
        return KERNEL_ARG.getStringUTF8(kernel, arg_indx, param_name);
    }

    /**
     * UTF-8 string with explicit length version of:
     * {@link CL12#clGetKernelArgInfo GetKernelArgInfo}
     */
    public static String clGetKernelArgInfoStringUTF8(long kernel, int arg_indx, int param_name, int param_value_size) {
        return KERNEL_ARG.getStringUTF8(kernel, arg_indx, param_name, param_value_size);
    }

    // ------------------------------------
    // Sampler (CL10.clGetSamplerInfo)
    // ------------------------------------
    private static final InfoQuery SAMPLER = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetSamplerInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single boolean value version of:
     * {@link CL10#clGetSamplerInfo GetSamplerInfo}
     */
    public static boolean clGetSamplerInfoBoolean(long sampler, int param_name) {
        return SAMPLER.getBoolean(sampler, param_name);
    }

    /**
     * Single int value version of: {@link CL10#clGetSamplerInfo GetSamplerInfo}
     */
    public static int clGetSamplerInfoInt(long sampler, int param_name) {
        return SAMPLER.getInt(sampler, param_name);
    }

    /**
     * Single pointer value version of:
     * {@link CL10#clGetSamplerInfo GetSamplerInfo}
     */
    public static long clGetSamplerInfoPointer(long sampler, int param_name) {
        return SAMPLER.getPointer(sampler, param_name);
    }

    /**
     * PointBuffer version of: {@link CL10#clGetSamplerInfo GetSamplerInfo}
     */
    public static int clGetSamplerInfoPointers(long sampler, int param_name, PointerBuffer target) {
        return SAMPLER.getPointers(sampler, param_name, target);
    }

    // ------------------------------------
    // Event (CL10.clGetEventInfo)
    // ------------------------------------
    private static final InfoQuery EVENT = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetEventInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single int value version of: {@link CL10#clGetEventInfo GetEventInfo}
     */
    public static int clGetEventInfoInt(long event, int param_name) {
        return EVENT.getInt(event, param_name);
    }

    /**
     * Single pointer value version of: {@link CL10#clGetEventInfo GetEventInfo}
     */
    public static long clGetEventInfoPointer(long event, int param_name) {
        return EVENT.getPointer(event, param_name);
    }

    /**
     * PointBuffer version of: {@link CL10#clGetEventInfo GetEventInfo}
     */
    public static int clGetEventInfoPointers(long event, int param_name, PointerBuffer target) {
        return EVENT.getPointers(event, param_name, target);
    }

    // ------------------------------------
    // Event Profiling (CL10.clGetEventProfilingInfo)
    // ------------------------------------
    private static final InfoQuery EVENT_PROFILING = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetEventProfilingInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single long value version of:
     * {@link CL10#clGetEventProfilingInfo GetEventProfilingInfo}
     */
    public static long clGetEventProfilingInfoLong(long event, int param_name) {
        return EVENT_PROFILING.getLong(event, param_name);
    }

    // ------------------------------------
    // GL Texture (CL10GL.clGetGLTextureInfo)
    // ------------------------------------
    private static final InfoQuery GL_TEXTURE = new InfoQuery() {
        @Override
        protected int get(long pointer, int param_name, long param_value_size, long param_value, long param_value_size_ret) {
            return nclGetGLTextureInfo(pointer, param_name, param_value_size, param_value, param_value_size_ret);
        }
    };

    /**
     * Single int value version of:
     * {@link CL10GL#clGetGLTextureInfo GetGLTextureInfo}
     */
    public static int clGetGLTextureInfoInt(long memobj, int param_name) {
        return GL_TEXTURE.getInt(memobj, param_name);
    }

}
