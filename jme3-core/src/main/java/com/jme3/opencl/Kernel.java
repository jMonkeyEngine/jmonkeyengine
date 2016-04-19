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

import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;

/**
 *
 * @author Sebastian Weiss
 */
public final class Kernel {
/*
    private final WorkSize globalWorkSize;
    private final WorkSize workGroupSize;
    private final long kernel;

    public Kernel(long kernel) {
        this.kernel = kernel;
        this.globalWorkSize = new WorkSize(0);
        this.workGroupSize = new WorkSize(0);
    }

    public String getName() {
        throw new UnsupportedOperationException("not supported yet");
    }

    public int getArgCount() {
        throw new UnsupportedOperationException("not supported yet");
    }

    public WorkSize getGlobalWorkSize() {
        return globalWorkSize;
    }

    public void setGlobalWorkSize(WorkSize ws) {
        globalWorkSize.set(ws);
    }

    public void setGlobalWorkSize(int size) {
        globalWorkSize.set(1, size);
    }

    public void setGlobalWorkSize(int width, int height) {
        globalWorkSize.set(2, width, height);
    }

    public void setGlobalWorkSize(int width, int height, int depth) {
        globalWorkSize.set(3, width, height, depth);
    }

    public WorkSize getWorkGroupSize() {
        return workGroupSize;
    }

    public void setWorkGroupSize(WorkSize ws) {
        workGroupSize.set(ws);
    }

    public void setWorkGroupSize(int size) {
        workGroupSize.set(1, size);
    }

    public void setWorkGroupSize(int width, int height) {
        workGroupSize.set(2, width, height);
    }

    public void setWorkGroupSize(int width, int height, int depth) {
        workGroupSize.set(3, width, height, depth);
    }

    public void setWorkGroupSizeToNull() {
        workGroupSize.set(1, 0);
    }

    public void setArg(int index, LocalMemPerElement t) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void setArg(int index, LocalMem t) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void setArg(int index, Buffer t) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void setArg(int index, byte b) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void setArg(int index, short s) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void setArg(int index, int i) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void setArg(int index, long l) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void setArg(int index, float f) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void setArg(int index, double d) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void setArg(int index, Vector2f v) {
        throw new UnsupportedOperationException("not supported yet");
    }

    //Vector3f not supported because cl_float3 is the same as a float4

    public void setArg(int index, Vector4f v) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void setArg(int index, Quaternion q) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public void setArg(int index, Object arg) {
        if (arg instanceof Byte) {
            setArg(index, (byte) arg);
        } else if (arg instanceof Short) {
            setArg(index, (short) arg);
        } else if (arg instanceof Integer) {
            setArg(index, (int) arg);
        } else if (arg instanceof Long) {
            setArg(index, (long) arg);
        } else if (arg instanceof Float) {
            setArg(index, (float) arg);
        } else if (arg instanceof Double) {
            setArg(index, (double) arg);
        } else if (arg instanceof Vector2f) {
            setArg(index, (Vector2f) arg);
        } else if (arg instanceof Vector4f) {
            setArg(index, (Vector4f) arg);
        } else if (arg instanceof Quaternion) {
            setArg(index, (Quaternion) arg);
        } else if (arg instanceof LocalMemPerElement) {
            setArg(index, (LocalMemPerElement) arg);
        } else if (arg instanceof LocalMem) {
            setArg(index, (LocalMem) arg);
        } else if (arg instanceof Buffer) {
            setArg(index, (Buffer) arg);
        } else {
            throw new IllegalArgumentException("unknown kernel argument type: " + arg);
        }
    }

    private void setArgs(Object... args) {
        for (int i = 0; i < args.length; ++i) {
            setArg(i, args[i]);
        }
    }

    public Event Run(CommandQueue queue) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public Event Run1(CommandQueue queue, WorkSize globalWorkSize, Object... args) {
        setGlobalWorkSize(globalWorkSize);
        setWorkGroupSizeToNull();
        setArgs(args);
        return Run(queue);
    }

    public Event Run2(CommandQueue queue, WorkSize globalWorkSize,
            WorkSize workGroupSize, Object... args) {
        setGlobalWorkSize(globalWorkSize);
        setWorkGroupSize(workGroupSize);
        setArgs(args);
        return Run(queue);
    }

    @Override
    public void deleteObject() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
*/
}
