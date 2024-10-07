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

import com.jme3.opencl.Event;
import java.util.logging.Logger;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLEvent;

/**
 *
 * @author shaman
 */
public class LwjglEvent extends Event {
    private static final Logger LOG = Logger.getLogger(LwjglEvent.class.getName());
    private CLEvent event;

    public LwjglEvent(CLEvent event) {
        super(new ReleaserImpl(event));
        this.event = event;
    }

    public CLEvent getEvent() {
        return event;
    }

    @Override
    public void waitForFinished() {
        if (event==null) {
            return;
        }
        CL10.clWaitForEvents(event);
        release(); // shortcut to save resources
    }

    @Override
    public boolean isCompleted() {
        if (event==null) {
            return true;
        }
        int status = event.getInfoInt(CL10.CL_EVENT_COMMAND_EXECUTION_STATUS);
        if (status == CL10.CL_SUCCESS) {
            release(); // shortcut to save resources
            return true;
        } else if (status < 0) {
            Utils.checkError(status, "EventStatus");
            return false;
        } else {
            return false;
        }
    }

    private static class ReleaserImpl implements ObjectReleaser {
        private CLEvent event;

        private ReleaserImpl(CLEvent event) {
            this.event = event;
        }
        
        @Override
        public void release() {
            if (event != null && event.isValid()) {
                int ret = CL10.clReleaseEvent(event);
                event = null;
                Utils.reportError(ret, "clReleaseEvent");
                LOG.finer("Event deleted");
            }
        }
        
    }
}
