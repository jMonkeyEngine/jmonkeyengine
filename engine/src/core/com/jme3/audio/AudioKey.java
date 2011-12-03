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

import com.jme3.asset.AssetKey;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

/**
 * <code>AudioKey</code> is extending AssetKey by holding stream flag.
 *
 * @author Kirill Vainer
 */
public class AudioKey extends AssetKey<AudioData> {

    private boolean stream;
    private boolean streamCache;

    /**
     * Create a new AudioKey.
     * 
     * @param name Name of the asset
     * @param stream If true, the audio will be streamed from harddrive,
     * otherwise it will be buffered entirely and then played.
     * @param streamCache If stream is true, then this specifies if
     * the stream cache is used. When enabled, the audio stream will
     * be read entirely but not decoded, allowing features such as 
     * seeking, determining duration and looping.
     */
    public AudioKey(String name, boolean stream, boolean streamCache){
        this(name, stream);
        this.streamCache = streamCache;
    }
    
    /**
     * Create a new AudioKey
     *
     * @param name Name of the asset
     * @param stream If true, the audio will be streamed from harddrive,
     * otherwise it will be buffered entirely and then played.
     */
    public AudioKey(String name, boolean stream){
        super(name);
        this.stream = stream;
    }

    public AudioKey(String name){
        super(name);
        this.stream = false;
    }

    public AudioKey(){
    }

    @Override
    public String toString(){
        return name + (stream ? 
                          (streamCache ? 
                            " (Stream/Cache)" : 
                            " (Stream)") : 
                         " (Buffer)");
    }

    /**
     * @return True if the loaded audio should be a {@link AudioStream} or
     * false if it should be a {@link AudioBuffer}.
     */
    public boolean isStream() {
        return stream;
    }
    
    /**
     * Specifies if the stream cache is used. 
     * 
     * When enabled, the audio stream will
     * be read entirely but not decoded, allowing features such as 
     * seeking, looping and determining duration.
     */
    public boolean useStreamCache(){
        return streamCache;
    }

    @Override
    public boolean shouldCache(){
        return !stream && !streamCache;
    }

    @Override
    public void write(JmeExporter ex) throws IOException{
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(stream, "do_stream", false);
        oc.write(streamCache, "use_stream_cache", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException{
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        stream = ic.readBoolean("do_stream", false);
        streamCache = ic.readBoolean("use_stream_cache", false);
    }

}
