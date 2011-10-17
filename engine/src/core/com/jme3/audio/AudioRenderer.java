/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.audio;

/**
 * Interface to be implemented by audio renderers.
 *
 * @author Kirill Vainer
 */
public interface AudioRenderer {

    /**
     * @param listener The listener camera, all 3D sounds will be
     * oriented around the listener.
     */
    public void setListener(Listener listener);

    /**
     * Sets the environment, used for reverb effects.
     *
     * @see AudioNode#setReverbEnabled(boolean)
     * @param env The environment to set.
     */
    public void setEnvironment(Environment env);

    public void playSourceInstance(AudioNode src);
    public void playSource(AudioNode src);
    public void pauseSource(AudioNode src);
    public void stopSource(AudioNode src);

    public void updateSourceParam(AudioNode src, AudioParam param);
    public void updateListenerParam(Listener listener, ListenerParam param);

    public void deleteFilter(Filter filter);
    public void deleteAudioData(AudioData ad);

    /**
     * Initializes the renderer. Should be the first method called
     * before using the system.
     */
    public void initialize();

    /**
     * Update the audio system. Must be called periodically.
     * @param tpf Time per frame.
     */
    public void update(float tpf);

    /**
     * Cleanup/destroy the audio system. Call this when app closes.
     */
    public void cleanup();
}
