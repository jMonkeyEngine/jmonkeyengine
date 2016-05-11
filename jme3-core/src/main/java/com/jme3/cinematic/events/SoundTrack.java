/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.cinematic.events;

import com.jme3.animation.LoopMode;

/**
 * A sound track to be played in a cinematic.
 * @author Nehon
 * @deprecated use SoundEvent instead
 */
@Deprecated
public class SoundTrack extends SoundEvent {

    /**
     * creates a sound track from the given resource path
     * @param path the path to an audio file (ie : "Sounds/mySound.wav")
     */    
    public SoundTrack(String path) {
        super(path);
    }

    /**
     * creates a sound track from the given resource path
     * @param path the path to an audio file (ie : "Sounds/mySound.wav")
     * @param stream true to make the audio data streamed
     */    
    public SoundTrack(String path, boolean stream) {
        super(path, stream);
    }

    public SoundTrack(String path, boolean stream, float initialDuration) {
        super(path, stream, initialDuration);
    }

    public SoundTrack(String path, boolean stream, LoopMode loopMode) {
        super(path, stream, loopMode);
    }

    public SoundTrack(String path, boolean stream, float initialDuration, LoopMode loopMode) {
        super(path, stream, initialDuration, loopMode);

    }

    public SoundTrack(String path, float initialDuration) {
        super(path, initialDuration);
    }

    public SoundTrack(String path, LoopMode loopMode) {
        super(path, loopMode);
    }

    public SoundTrack(String path, float initialDuration, LoopMode loopMode) {
        super(path, initialDuration, loopMode);
    }

    public SoundTrack() {
        super();
    }
}
