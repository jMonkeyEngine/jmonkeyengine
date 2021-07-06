/*
 * Copyright (c) 2020-2021 jMonkeyEngine
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
package jme3test.app.state;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.ViewPort;
import java.util.List;

/**
 * Test case for JME issue #1421: ScreenshotAppState never cleans up.
 * <p>
 * If successful, the application will complete without throwing an Exception.
 *
 * @author Stephen Gold
 */
public class TestIssue1421 extends SimpleApplication {

    private int updateCount = 0;
    private ScreenshotAppState screenshotAppState;

    public static void main(String[] args) {
        TestIssue1421 app = new TestIssue1421();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // enable screenshots
        screenshotAppState = new ScreenshotAppState("./", "screen_shot");
        boolean success = stateManager.attach(screenshotAppState);
        assert success;
    }

    @Override
    public void simpleUpdate(float tpf) {
        ++updateCount;
        if (updateCount == 10) { // after attached
            // Confirm that the SceneProcessor is attached.
            List<ViewPort> vps = renderManager.getPostViews();
            assert vps.size() == 1 : vps.size();
            ViewPort lastViewPort = vps.get(0);
            List<SceneProcessor> processorList = lastViewPort.getProcessors();
            int numProcessors = processorList.size();
            assert numProcessors == 1 : numProcessors;

            // Confirm that KEY_SYSRQ is mapped.
            assert inputManager.hasMapping("ScreenShot");

            // disable screenshots
            boolean success = stateManager.detach(screenshotAppState);
            assert success;

        } else if (updateCount == 20) { // after detached
            // Check whether the SceneProcessor is still attached.
            List<ViewPort> vps = renderManager.getPostViews();
            ViewPort lastViewPort = vps.get(0);
            List<SceneProcessor> processorList = lastViewPort.getProcessors();
            int numProcessors = processorList.size();
            if (numProcessors != 0) {
                throw new IllegalStateException(
                        "SceneProcessor is still attached.");
            }

            // Check whether KEY_SYSRQ is still mapped.
            if (inputManager.hasMapping("ScreenShot")) {
                throw new IllegalStateException("KEY_SYSRQ is still mapped.");
            }
            stop();
        }
    }
}
