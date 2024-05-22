/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package jme3test.renderpath;

import com.jme3.app.SimpleApplication;
import com.jme3.renderer.framegraph.FrameGraph;

/**
 * Tests framegraph loading speeds.
 * <p>
 * The test loads a total of 9 framegraphs (3 forward, 3 deferred, and 3 tiled deferred)
 * after a delay of 100 frames, with 10 frames seperating each load. After all loading
 * is complete, the application quits. Results are printed to the console as milliseconds
 * each load took to complete.
 * <p>
 * In general, earlier loads take around 15ms, and later loads take around 2ms.
 * <p>
 * Note that framegraph data are not cached.
 * 
 * @author codex
 */
public class TestFrameGraphLoadingSpeeds extends SimpleApplication {
    
    private int frameDelay = 100;
    private int loadIndex = 0;
    
    public static void main(String[] args) {
        new TestFrameGraphLoadingSpeeds().start();
    }
    
    @Override
    public void simpleInitApp() {}
    @Override
    public void simpleUpdate(float tpf) {
        if (--frameDelay <= 0) {
            switch (loadIndex++) {
                case 6:
                case 3:
                case 0: load(loadIndex, "forward", "Common/FrameGraphs/Forward.j3g"); break;
                case 7:
                case 4:
                case 1: load(loadIndex, "deferred", "Common/FrameGraphs/Deferred.j3g"); break;
                case 8:
                case 5:
                case 2: load(loadIndex, "tiled deferred", "Common/FrameGraphs/TiledDeferred.j3g"); break;
                default: stop();
            }
            frameDelay = 10;
        }
    }
    
    private void load(int index, String name, String path) {
        long timeBefore = System.currentTimeMillis();
        FrameGraph fg = new FrameGraph(assetManager, path);
        long timeAfter = System.currentTimeMillis();
        System.out.println(index+": "+name+": "+(timeAfter-timeBefore)+"ms");
    }
    
}
