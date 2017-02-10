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

package com.jme3.app;

import android.os.Build;
import com.jme3.profile.*;

import static com.jme3.profile.AppStep.BeginFrame;
import static com.jme3.profile.AppStep.EndFrame;
import static com.jme3.profile.AppStep.ProcessAudio;
import static com.jme3.profile.AppStep.ProcessInput;
import static com.jme3.profile.AppStep.QueuedTasks;
import static com.jme3.profile.AppStep.RenderFrame;
import static com.jme3.profile.AppStep.RenderMainViewPorts;
import static com.jme3.profile.AppStep.RenderPostViewPorts;
import static com.jme3.profile.AppStep.RenderPreviewViewPorts;
import static com.jme3.profile.AppStep.SpatialUpdate;
import static com.jme3.profile.AppStep.StateManagerRender;
import static com.jme3.profile.AppStep.StateManagerUpdate;
import static com.jme3.profile.VpStep.BeginRender;
import static com.jme3.profile.VpStep.EndRender;
import static com.jme3.profile.VpStep.FlushQueue;
import static com.jme3.profile.VpStep.PostFrame;
import static com.jme3.profile.VpStep.PostQueue;
import static com.jme3.profile.VpStep.RenderScene;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;

/**
 *  An AppProfiler implementation that integrates the
 *  per-frame application-wide timings for update versus
 *  render into the Android systrace utility.
 *
 *  <p>This profiler uses the Android Trace class which is only supported
 *  on Android SDK rev 18 and higher (ver 4.3 and higher).  If the
 *  device is running a version < rev 18, the logging will
 *  be skipped.</p>
 *
 *  <p>In the MainActivity class, add the following:</p>
 *  <pre><code>
 *  {@literal @}Override
 *  public void onCreate(Bundle savedInstanceState) {
 *      super.onCreate(savedInstanceState);
 *      app.setAppProfiler(new DefaultAndroidProfiler());
 *  }
 *  </code></pre>
 *  Start the Android systrace utility and run the application to
 *  see the detailed timings of the application.
 *
 * @author iwgeric
 */
public class DefaultAndroidProfiler implements AppProfiler {
    private int androidApiLevel = Build.VERSION.SDK_INT;

    public void appStep(AppStep appStep) {
        if (androidApiLevel >= 18) {
            switch(appStep) {
                case BeginFrame:
                    android.os.Trace.beginSection("Frame");
                    break;
                case QueuedTasks:
                    android.os.Trace.beginSection("QueuedTask");
                    break;
                case ProcessInput:
                    android.os.Trace.endSection();
                    android.os.Trace.beginSection("ProcessInput");
                    break;
                case ProcessAudio:
                    android.os.Trace.endSection();
                    android.os.Trace.beginSection("ProcessAudio");
                    break;
                case StateManagerUpdate:
                    android.os.Trace.endSection();
                    android.os.Trace.beginSection("StateManagerUpdate");
                    break;
                case SpatialUpdate:
                    android.os.Trace.endSection();
                    android.os.Trace.beginSection("SpatialUpdate");
                    break;
                case StateManagerRender:
                    android.os.Trace.endSection();
                    android.os.Trace.beginSection("StateManagerRender");
                    break;
                case RenderFrame:
                    android.os.Trace.endSection();
                    android.os.Trace.beginSection("RenderFrame");
                    break;
                case RenderPreviewViewPorts:
                    android.os.Trace.beginSection("RenderPreviewViewPorts");
                    break;
                case RenderMainViewPorts:
                    android.os.Trace.endSection();
                    android.os.Trace.beginSection("RenderMainViewPorts");
                    break;
                case RenderPostViewPorts:
                    android.os.Trace.endSection();
                    android.os.Trace.beginSection("RenderPostViewPorts");
                    break;
                case EndFrame:
                    android.os.Trace.endSection();
                    android.os.Trace.endSection();
                    android.os.Trace.endSection();
                    break;
            }
        }
    }

    public void vpStep(VpStep vpStep, ViewPort vp, RenderQueue.Bucket bucket) {
        if (androidApiLevel >= 18) {
            switch (vpStep) {
                case BeginRender:
                    android.os.Trace.beginSection("Render: " + vp.getName());
                    break;
                case RenderScene:
                    android.os.Trace.beginSection("RenderScene: " + vp.getName());
                    break;
                case PostQueue:
                    android.os.Trace.endSection();
                    android.os.Trace.beginSection("PostQueue: " + vp.getName());
                    break;
                case FlushQueue:
                    android.os.Trace.endSection();
                    android.os.Trace.beginSection("FlushQueue: " + vp.getName());
                    break;
                case PostFrame:
                    android.os.Trace.endSection();
                    android.os.Trace.beginSection("PostFrame: " + vp.getName());
                    break;
                case EndRender:
                    android.os.Trace.endSection();
                    android.os.Trace.endSection();
                    break;
            }
        }
    }

    @Override
    public void spStep(SpStep step, String... additionalInfo) {

    }

}
