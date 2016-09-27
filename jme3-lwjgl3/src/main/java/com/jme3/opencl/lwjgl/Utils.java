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

import com.jme3.opencl.MappingAccess;
import com.jme3.opencl.MemoryAccess;
import com.jme3.opencl.OpenCLException;
import com.jme3.opencl.lwjgl.info.CLUtil;
import java.nio.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;


/**
 *
 * @author shaman
 */
public class Utils {
    private static final Logger LOG = Logger.getLogger(Utils.class.getName());
    private Utils() {}
   
    public static final boolean CL_GL_SHARING_POSSIBLE = com.jme3.system.lwjgl.LwjglContext.CL_GL_SHARING_POSSIBLE;
    public static void assertSharingPossible() {
        if (!CL_GL_SHARING_POSSIBLE) {
            throw new OpenCLException("OpenGL/CL sharing not possible");
        }
    }
    
    public static int getMajorVersion(String version, String prefix) {
        String s = version.substring(prefix.length());
        return Integer.parseInt(s);
    }
    
    public static int getMinorVersion(String version, String prefix) {
        String s = version.substring(prefix.length());
        int major = Integer.parseInt(s);
        s = s.substring((int) (Math.log10(major) + 2));
        return Integer.parseInt(s);
    }
    
    public static final class TempBuffer {
        public final ByteBuffer b16;
        public final ShortBuffer b16s;
        public final IntBuffer b16i;
        public final LongBuffer b16l;
        public final FloatBuffer b16f;
        public final DoubleBuffer b16d;
        public TempBuffer() {
            b16 = BufferUtils.createByteBuffer(16);
            b16s = b16.asShortBuffer();
            b16i = b16.asIntBuffer();
            b16l = b16.asLongBuffer();
            b16f = b16.asFloatBuffer();
            b16d = b16.asDoubleBuffer();
        }
    }
    public static final ByteBuffer b80; //needed for ImageDescriptor
    public static final LongBuffer b80l;
    public static final FloatBuffer b80f;
    public static final TempBuffer[] tempBuffers = new TempBuffer[8];
    public static final PointerBuffer[] pointerBuffers = new PointerBuffer[8];
    static {
        for (int i=0; i<8; ++i) {
            tempBuffers[i] = new TempBuffer();
            pointerBuffers[i] = PointerBuffer.allocateDirect(4);
        }
        errorBuffer = BufferUtils.createIntBuffer(1);
        b80 = BufferUtils.createByteBuffer(80);
        b80l = b80.asLongBuffer();
        b80f = b80.asFloatBuffer();
    }
    
    public static IntBuffer errorBuffer;
    public static void checkError(IntBuffer errorBuffer, String callName) {
        checkError(errorBuffer.get(0), callName);
    }
    public static void checkError(int error, String callName) {
        if (error != CL10.CL_SUCCESS) {
            String errname = getErrorName(error);
            if (errname == null) {
                errname = "UNKNOWN";
            }
            throw new OpenCLException("OpenCL error in " + callName + ": " + errname + " (0x" + Integer.toHexString(error) + ")", error);
        }
    }
    
    public static void reportError(int error, String callName) {
        if (error != CL10.CL_SUCCESS) {
            String errname = getErrorName(error);
            if (errname == null) {
                errname = "UNKNOWN";
            }
            LOG.log(Level.WARNING, "OpenCL error in {0}: {1} (0x{2})", new Object[]{callName, errname, Integer.toHexString(error)});
        }
    }
    
    public static String getErrorName(int code) {
        return CLUtil.getErrcodeName(code);
    }
    
    public static long getMemoryAccessFlags(MemoryAccess ma) {
        switch (ma) {
            case READ_ONLY: return CL10.CL_MEM_READ_ONLY;
            case WRITE_ONLY: return CL10.CL_MEM_WRITE_ONLY;
            case READ_WRITE: return CL10.CL_MEM_READ_WRITE;
            default: throw new IllegalArgumentException("Unknown memory access: "+ma);
        }
    }
    public static MemoryAccess getMemoryAccessFromFlag(long flag) {
        if ((flag & CL10.CL_MEM_READ_WRITE) > 0) {
            return MemoryAccess.READ_WRITE;
        }
        if ((flag & CL10.CL_MEM_READ_ONLY) > 0) {
            return MemoryAccess.READ_ONLY;
        }
        if ((flag & CL10.CL_MEM_WRITE_ONLY) > 0) {
            return MemoryAccess.WRITE_ONLY;
        }
        throw new OpenCLException("Unknown memory access flag: "+flag);
    }
    
    public static long getMappingAccessFlags(MappingAccess ma) {
        switch (ma) {
            case MAP_READ_ONLY: return CL10.CL_MAP_READ;
            case MAP_READ_WRITE: return CL10.CL_MAP_READ | CL10.CL_MAP_WRITE;
            case MAP_WRITE_ONLY: return CL10.CL_MAP_WRITE;
            case MAP_WRITE_INVALIDATE: return CL12.CL_MAP_WRITE_INVALIDATE_REGION;
            default: throw new IllegalArgumentException("Unknown mapping access: "+ma);
        }
    }

}
