/*
 * Copyright (c) 2014 jMonkeyEngine
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
 
package com.jme3.profile;

import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;


/**
 *  Can be hooked into the application (and render manager)
 *  to receive callbacks about specific frame steps.  It is up
 *  to the specific implememtation to decide what to do with
 *  the information.
 *
 *  @author    Paul Speed
 */
public interface AppProfiler {

    /**
     *  Called at the beginning of the specified AppStep.
     */
    public void appStep(AppStep step);
    
    /**
     *  Called at the beginning of the specified VpStep during
     *  the rendering of the specified ViewPort.  For bucket-specific
     *  steps the Bucket parameter will be non-null.
     */
    public void vpStep(VpStep step, ViewPort vp, Bucket bucket);

    /**
     * Called at the beginning of the specified SpStep (SceneProcessor step).
     * For more detailed steps it is possible to provide additional information as strings, like the name of the processor.
     */
    public void spStep(SpStep step, String... additionalInfo);
}


