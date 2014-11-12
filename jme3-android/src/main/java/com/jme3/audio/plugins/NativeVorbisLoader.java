package com.jme3.audio.plugins;

import android.content.res.AssetFileDescriptor;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.plugins.AndroidLocator;
import com.jme3.asset.plugins.AndroidLocator.AndroidAssetInfo;
import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioKey;
import com.jme3.audio.SeekableStream;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class NativeVorbisLoader implements AssetLoader {
    
    private static class VorbisInputStream extends InputStream implements SeekableStream {

        private final NativeVorbisFile file;
        
        public VorbisInputStream(NativeVorbisFile file) {
            this.file = file;
        }
        
        @Override
        public int read() throws IOException {
            return 0;
        }
        
        @Override
        public int read(byte[] buf) throws IOException {
            return file.read(buf, 0, buf.length);
        }
        
        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            return file.read(buf, off, len);
        }
        
        @Override
        public long skip(long n) throws IOException {
            throw new IOException("Not supported for audio streams");
        }

        public void setTime(float time) {
            try {
                file.seekTime(time);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
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
        
        AndroidAssetInfo aai = (AndroidAssetInfo) assetInfo;
        AssetFileDescriptor afd = aai.openFileDescriptor();
        int fd = afd.getParcelFileDescriptor().getFd();
        
        NativeVorbisFile file = new NativeVorbisFile(fd, afd.getStartOffset(), 
                                                         afd.getLength());
        
        ByteBuffer data = BufferUtils.createByteBuffer(file.totalBytes);
        file.readFully(data);
        AudioBuffer ab = new AudioBuffer();
        ab.setupFormat(file.channels, 16, file.sampleRate);
        ab.updateData(data);
        return ab;
    }
}