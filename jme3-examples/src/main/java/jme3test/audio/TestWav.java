/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioNode;

/**
 * @author capdevon
 */
public class TestWav extends SimpleApplication {

    private float time = 0;
    private AudioNode audioSource;

    public static void main(String[] args) {
        TestWav test = new TestWav();
        test.start();
    }

    @Override
    public void simpleInitApp() {
        testMaxNumChannels();
        testFakeAudio();
        testPlaySourceInstance();

        audioSource = createAudioNode("Sound/Effects/Gun.wav", AudioData.DataType.Buffer);
        audioSource.setName("Gun");
        audioSource.setPositional(true);
    }

    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;
        if (time > 1f) {
            audioSource.playInstance();
            time = 0;
        }
    }

    /**
     * Creates an {@link AudioNode} for the specified audio file.
     * This method demonstrates an alternative way to defer the creation
     * of an AudioNode by explicitly creating and potentially pre-loading
     * the {@link AudioData} and {@link AudioKey} before instantiating
     * the AudioNode. This can be useful in scenarios where you want more
     * control over the asset loading process or when the AudioData and
     * AudioKey are already available.
     *
     * @param filepath The path to the audio file.
     * @param type     The desired {@link AudioData.DataType} for the audio.
     * @return A new {@code AudioNode} configured with the loaded audio data.
     */
    private AudioNode createAudioNode(String filepath, AudioData.DataType type) {
        boolean stream = (type == AudioData.DataType.Stream);
        boolean streamCache = true;
        AudioKey audioKey = new AudioKey(filepath, stream, streamCache);
        AudioData data = assetManager.loadAsset(audioKey);

        AudioNode audio = new AudioNode();
        audio.setAudioData(data, audioKey);
        return audio;
    }

    /**
     * WARNING: No channel available to play instance of AudioNode[status=Stopped, vol=0.1]
     */
    private void testMaxNumChannels() {
        final int MAX_NUM_CHANNELS = 64;
        for (int i = 0; i < MAX_NUM_CHANNELS + 1; i++) {
            AudioNode audio = createAudioNode("Sound/Effects/Gun.wav", AudioData.DataType.Buffer);
            audio.setVolume(0.1f);
            audio.playInstance();
        }
    }

    /**
     * java.lang.UnsupportedOperationException: Cannot play instances of audio streams. Use play() instead.
     * 	at com.jme3.audio.openal.ALAudioRenderer.playSourceInstance()
     */
    private void testPlaySourceInstance() {
        try {
            AudioNode nature = new AudioNode(assetManager,
                    "Sound/Environment/Nature.ogg", AudioData.DataType.Stream);
            audioRenderer.playSourceInstance(nature);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testFakeAudio() {
        /**
         * Tests AudioRenderer.playSource() with an
         * AudioNode lacking AudioData to observe its handling (typically discard).
         */
        AudioNode fakeAudio = new AudioNode() {
            @Override
            public String toString() {
                // includes node name for easier identification in log messages.
                return getName() + " (" + AudioNode.class.getSimpleName() + ")";
            }
        };
        fakeAudio.setName("FakeAudio");
        audioRenderer.playSource(fakeAudio);
        audioRenderer.playSourceInstance(fakeAudio);
    }

}
