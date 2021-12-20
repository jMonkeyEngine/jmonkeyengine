/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioStream;
import com.jme3.audio.SeekableStream;
import com.jme3.util.BufferUtils;
import de.jarnbjo.ogg.EndOfOggStreamException;
import de.jarnbjo.ogg.LogicalOggStream;
import de.jarnbjo.ogg.PhysicalOggStream;
import de.jarnbjo.vorbis.IdentificationHeader;
import de.jarnbjo.vorbis.VorbisStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;

public class OGGLoader implements AssetLoader {

//    private static int BLOCK_SIZE = 4096*64;

    private PhysicalOggStream oggStream;
    private LogicalOggStream loStream;
    private VorbisStream vorbisStream;

//    private CommentHeader commentHdr;
    private IdentificationHeader streamHdr;
  
    private static class JOggInputStream extends InputStream {

        protected boolean endOfStream = false;
        protected PhysicalOggStream ps;
        protected LogicalOggStream ls;
        protected VorbisStream vs;
        protected int current = 0;
        protected final int maximum;

        public JOggInputStream(PhysicalOggStream ps, LogicalOggStream ls, VorbisStream vs, int maximum){   
            this.ps = ps;
            this.ls = ls;
            this.vs = vs;
            this.maximum = maximum;
        }

        @Override
        public int read() throws IOException {
            return 0;
        }

        @Override
        public int read(byte[] buf) throws IOException{
            return read(buf,0,buf.length);
        }

        @Override
        public int read(byte[] buf, int offset, int length) throws IOException {
            if (endOfStream) {
                return -1;
            }

            int bytesRead = 0, cnt = 0;
            assert length % 2 == 0; // read buffer should be even

            while (bytesRead < length) {
                try {
                    int toRead;
                    if (maximum != -1) {
                        if (current >= maximum) {
                            endOfStream = true;
                            break;
                        }
                        int remainingInStream = maximum - current;
                        int remainingToBuffer = length - bytesRead;
                        toRead = Math.min(remainingInStream, remainingToBuffer);
                    } else {
                        toRead = length - bytesRead;
                    }
                    if ((cnt = vs.readPcm(buf, offset + bytesRead, toRead)) <= 0) {
                        endOfStream = true;
                        break;
                    }
                } catch (EndOfOggStreamException ex) {
                    endOfStream = true;
                    break;
                }
                bytesRead += cnt;
                current += cnt;
            }

            if (endOfStream && bytesRead <= 0) {
                return -1;
            } else {
                swapBytes(buf, offset, bytesRead);
                return bytesRead;
            }
        }

        @Override
        public void close() throws IOException{
            vs.close();
        }

    }
    
    private static class SeekableJOggInputStream extends JOggInputStream implements SeekableStream {
     
        public SeekableJOggInputStream(PhysicalOggStream ps, LogicalOggStream ls, VorbisStream vs, int maximum){           
            super(ps, ls, vs, maximum);
        }

        @Override
        public void setTime(float time) {
            if (time != 0.0) {
                throw new UnsupportedOperationException("OGG/Vorbis seeking only supported for time = 0");
            }
            
            try {
                // HACK: Reload the logical and vorbis streams from scratch
                // based on the existing ogg page data. 
                // This fixes an audio discontinuity issue when looping
                // a streaming OGG file via setTime(0).
                ls = ((CachedOggStream) ps).reloadLogicalOggStream();
                vs = new VorbisStream(ls);
                endOfStream = false;
                current = 0;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
    }
    
    /**
     * Returns the total of expected OGG bytes. 
     * 
     * @param dataBytesTotal The number of bytes in the input
     * @return If the computed number of bytes is less than the number
     * of bytes in the input, it is returned, otherwise the number 
     * of bytes in the input is returned.
     */
    private int getOggTotalBytes(int dataBytesTotal){
        // Vorbis stream could have more samples than the duration of the sound.
        // Must truncate.
        int numSamples;
        if (oggStream instanceof CachedOggStream){
            CachedOggStream cachedOggStream = (CachedOggStream) oggStream;
            numSamples = (int) cachedOggStream.getLastOggPage().getAbsoluteGranulePosition();
        }else{
            UncachedOggStream uncachedOggStream = (UncachedOggStream) oggStream;
            numSamples = (int) uncachedOggStream.getLastOggPage().getAbsoluteGranulePosition();
        }

        // Number of Samples * Number of Channels * Bytes Per Sample
        int totalBytes = numSamples * streamHdr.getChannels() * 2;

//        System.out.println("Sample Rate: " + streamHdr.getSampleRate());
//        System.out.println("Channels: " + streamHdr.getChannels());
//        System.out.println("Stream Length: " + numSamples);
//        System.out.println("Bytes Calculated: " + totalBytes);
//        System.out.println("Bytes Available:  " + dataBytes.length);

        // Take the minimum of the number of bytes available
        // and the expected duration of the audio.
        return Math.min(totalBytes, dataBytesTotal);
    }
    
    private float computeStreamDuration(){
        // for uncached stream sources, the granule position is not known.
        if (oggStream instanceof UncachedOggStream)
            return -1;
        
        // 2 bytes(16bit) * channels * sampleRate
        int bytesPerSec = 2 * streamHdr.getChannels() * streamHdr.getSampleRate();
        
        // Don't know how many bytes are in input, pass MAX_VALUE
        int totalBytes = getOggTotalBytes(Integer.MAX_VALUE);
        
        return (float)totalBytes / bytesPerSec;
    }

    private ByteBuffer readToBuffer() throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[512];
        int read = 0;

        try {
            while ( (read = vorbisStream.readPcm(buf, 0, buf.length)) > 0){
                baos.write(buf, 0, read);
            }
        } catch (EndOfOggStreamException ex){
        }

       
        byte[] dataBytes = baos.toByteArray();
        swapBytes(dataBytes, 0, dataBytes.length);
        
        int bytesToCopy = getOggTotalBytes( dataBytes.length );

        ByteBuffer data = BufferUtils.createByteBuffer(bytesToCopy);
        data.put(dataBytes, 0, bytesToCopy).flip();

        vorbisStream.close();
        loStream.close();
        oggStream.close();

        return data;
    }

    private static void swapBytes(byte[] b, int off, int len) {
        byte tempByte;
        for (int i = off; i < (off+len); i+=2) {
            tempByte = b[i];
            b[i] = b[i+1];
            b[i+1] = tempByte;
        }
    }

    private InputStream readToStream(boolean seekable) {
        if (seekable) {
            int maximum = getOggTotalBytes(Integer.MAX_VALUE);
            return new SeekableJOggInputStream(oggStream, loStream, vorbisStream, maximum);
        } else {
            return new JOggInputStream(oggStream, loStream, vorbisStream, -1);
        }
    }
    
    @SuppressWarnings("unchecked")
    private AudioData load(InputStream in, boolean readStream, boolean streamCache) throws IOException{
        if (readStream && streamCache){
            oggStream = new CachedOggStream(in);
        }else{
            oggStream = new UncachedOggStream(in);
        }

        Collection<LogicalOggStream> streams = oggStream.getLogicalStreams();
        loStream = streams.iterator().next();

//        if (loStream == null){
//            throw new IOException("OGG File does not contain vorbis audio stream");
//        }

        vorbisStream = new VorbisStream(loStream);
        streamHdr = vorbisStream.getIdentificationHeader();
//        commentHdr = vorbisStream.getCommentHeader();
    
        if (!readStream){
            AudioBuffer audioBuffer = new AudioBuffer();
            audioBuffer.setupFormat(streamHdr.getChannels(), 16, streamHdr.getSampleRate());
            audioBuffer.updateData(readToBuffer());
            return audioBuffer;
        }else{
            AudioStream audioStream = new AudioStream();
            audioStream.setupFormat(streamHdr.getChannels(), 16, streamHdr.getSampleRate());
            
            // might return -1 if unknown
            float streamDuration = computeStreamDuration();
            
            audioStream.updateData(readToStream(oggStream.isSeekable()), streamDuration);
            return audioStream;
        }
    }

    @Override
    public Object load(AssetInfo info) throws IOException {
        if (!(info.getKey() instanceof AudioKey)){
            throw new IllegalArgumentException("Audio assets must be loaded using an AudioKey");
        }
        
        AudioKey key = (AudioKey) info.getKey();
        boolean readStream = key.isStream();
        boolean streamCache = key.useStreamCache();
        
        InputStream in = null;
        try {
            in = info.openStream();
            AudioData data = load(in, readStream, streamCache);
            if (readStream && !streamCache) {
                // We still need the stream in this case.
                in = null;
            }
            return data;
        } finally {
            if (in != null){
                in.close();
            }
        }
        
    }

}
