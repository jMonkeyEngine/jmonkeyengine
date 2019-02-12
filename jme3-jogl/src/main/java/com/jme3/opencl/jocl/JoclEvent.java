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

import com.jme3.opencl.Event;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.llb.CL;
import java.util.logging.Logger;

/**
 *
 * @author shaman
 */
public class JoclEvent extends Event {
    private static final Logger LOG = Logger.getLogger(JoclEvent.class.getName());
    
    final long id;
    final CL cl;

    public JoclEvent(long id) {
        super(new ReleaserImpl(id));
        this.id = id;
        this.cl = CLPlatform.getLowLevelCLInterface();
    }

    @Override
    public void waitForFinished() {
        Utils.pointers[0].rewind();
        Utils.pointers[0].put(0, id);
        int ret = cl.clWaitForEvents(1, Utils.pointers[0]);
        Utils.checkError(ret, "clWaitForEvents");
        release();
    }

    @Override
    public boolean isCompleted() {
        Utils.tempBuffers[0].b16.rewind();
        int err = cl.clGetEventInfo(id, CL.CL_EVENT_COMMAND_EXECUTION_STATUS, 4, Utils.tempBuffers[0].b16, null);
        Utils.checkError(err, "clGetEventInfo");
        int status = Utils.tempBuffers[0].b16i.get(0);
        if (status == CL.CL_SUCCESS) {
            release();
            return true;
        } else if (status < 0) {
            Utils.checkError(status, "EventStatus");
            return false;
        } else {
            return false;
        }
    }
    
    private static class ReleaserImpl implements ObjectReleaser {
        private long event;

        private ReleaserImpl(long event) {
            this.event = event;
        }
        
        @Override
        public void release() {
            if (event != 0) {
                int ret = CLPlatform.getLowLevelCLInterface().clReleaseEvent(event);
                event = 0;
                Utils.reportError(ret, "clReleaseEvent");
                LOG.finer("Event deleted");
            }
        }
        
    }
}
