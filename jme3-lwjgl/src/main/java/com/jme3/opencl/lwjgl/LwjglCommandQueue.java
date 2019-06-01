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

import com.jme3.opencl.CommandQueue;
import com.jme3.opencl.Device;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;

/**
 *
 * @author shaman
 */
public class LwjglCommandQueue extends CommandQueue {

    private final CLCommandQueue queue;

    public LwjglCommandQueue(CLCommandQueue queue, Device device) {
        super(new ReleaserImpl(queue), device);
        this.queue = queue;
    }
    
    public CLCommandQueue getQueue() {
        return queue;
    }
    
    @Override
    public void flush() {
        int ret = CL10.clFlush(queue);
        Utils.checkError(ret, "clFlush");
    }

    @Override
    public void finish() {
        int ret = CL10.clFinish(queue);
        Utils.checkError(ret, "clFinish");
    }
    
    private static class ReleaserImpl implements ObjectReleaser {
        private CLCommandQueue queue;
        private ReleaserImpl(CLCommandQueue queue) {
            this.queue = queue;
        }
        @Override
        public void release() {
            if (queue != null) {
                int ret = CL10.clReleaseCommandQueue(queue);
                queue = null;
                Utils.reportError(ret, "clReleaseCommandQueue");
            }
        }
    }
}
