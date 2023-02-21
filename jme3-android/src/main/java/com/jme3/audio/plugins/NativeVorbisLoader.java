/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.audio.plugins;

import android.content.res.AssetFileDescriptor;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.plugins.AndroidLocator;
import com.jme3.asset.plugins.AndroidLocator.AndroidAssetInfo;
import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioStream;
import com.jme3.audio.SeekableStream;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class NativeVorbisLoader implements AssetLoader {
    
    private static class VorbisInputStream extends InputStream implements SeekableStream {

        private final AssetFileDescriptor afd;
        private final NativeVorbisFile file;
        
        public VorbisInputStream(AssetFileDescriptor afd, NativeVorbisFile file) {
            this.afd = afd;
            this.file = file;
        }
        
        @Override
        public int read() throws IOException {
            return 0;
        }
        
        @Override
        public int read(byte[] buf) throws IOException {
            return file.readIntoArray(buf, 0, buf.length);
        }
        
        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            return file.readIntoArray(buf, off, len);
        }
        
        @Override
        public long skip(long n) throws IOException {
            throw new IOException("Not supported for audio streams");
        }

        @Override
        public void setTime(float time) {
            try {
                file.seekTime(time);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        @Override
        public void close() throws IOException {
            file.clearResources();
            afd.close();
        }
    }
    
    private static AudioBuffer loadBuffer(AssetInfo assetInfo) throws IOException {
        AndroidAssetInfo aai = (AndroidAssetInfo) assetInfo;
        AssetFileDescriptor afd = null;
        NativeVorbisFile file = null;
        try {
            afd = aai.openFileDescriptor();
            int fd = afd.getParcelFileDescriptor().getFd();
            file = new NativeVorbisFile(fd, afd.getStartOffset(), afd.getLength());
            ByteBuffer data = BufferUtils.createByteBuffer(file.totalBytes);
            file.readIntoBuffer(data);
            AudioBuffer ab = new AudioBuffer();
            ab.setupFormat(file.channels, 16, file.sampleRate);
            ab.updateData(data);
            return ab;
        } finally {
            if (file != null) {
                file.clearResources();
            }
            if (afd != null) {
                afd.close();
            }
        }
    }
    
    private static AudioStream loadStream(AssetInfo assetInfo) throws IOException {
        AndroidAssetInfo aai = (AndroidAssetInfo) assetInfo;
        AssetFileDescriptor afd = null;
        NativeVorbisFile file = null;
        boolean success = false;
        
        try {
            afd = aai.openFileDescriptor();
            int fd = afd.getParcelFileDescriptor().getFd();
            file = new NativeVorbisFile(fd, afd.getStartOffset(), afd.getLength());
            
            AudioStream stream = new AudioStream();
            stream.setupFormat(file.channels, 16, file.sampleRate);
            stream.updateData(new VorbisInputStream(afd, file), file.duration);
            
            success = true;
            
            return stream;
        } finally {
            if (!success) {
                if (file != null) {
                    file.clearResources();
                }
                if (afd != null) {
                    afd.close();
                }
            }
        }
    }
    
    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AudioKey key = (AudioKey) assetInfo.getKey();
        if (!(assetInfo instanceof AndroidLocator.AndroidAssetInfo)) {
            throw new UnsupportedOperationException("Cannot load audio files from classpath." + 
                                                    "Place your audio files in " +
                                                    "Android's assets directory");
        }
        
        if (key.isStream()) {
            return loadStream(assetInfo);
        } else {
            return loadBuffer(assetInfo);
        }
    }
}