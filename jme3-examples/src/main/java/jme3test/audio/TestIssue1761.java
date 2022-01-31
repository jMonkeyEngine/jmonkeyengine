/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package jme3test.audio;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import java.util.Random;

/**
 * Stress test to reproduce JME issue #1761 (AssertionError in ALAudioRenderer).
 *
 * <p>After some network delay, a song will play,
 * albeit slowly and in a broken fashion.
 * If the issue is solved, the song will play all the way through.
 * If the issue is present, an AssertionError will be thrown, usually within a
 * second of the song starting.
 */
public class TestIssue1761 extends SimpleApplication {

    private AudioNode audioNode;
    final private Random random = new Random();

    /**
     * Main entry point for the TestIssue1761 application.
     *
     * @param unused array of command-line arguments
     */
    public static void main(String[] unused) {
        TestIssue1761 test = new TestIssue1761();
        test.start();
    }

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator(
                "https://web.archive.org/web/20170625151521if_/http://www.vorbis.com/music/",
                UrlLocator.class);
        audioNode = new AudioNode(assetManager, "Lumme-Badloop.ogg",
                AudioData.DataType.Stream);
        audioNode.setPositional(false);
        audioNode.play();
    }

    @Override
    public void simpleUpdate(float tpf) {
        /*
         * Randomly pause and restart the audio.
         */
        if (random.nextInt(2) == 0) {
            audioNode.pause();
        } else {
            audioNode.play();
        }
    }
}
