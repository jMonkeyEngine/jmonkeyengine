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

package com.jme3.audio.plugins;

import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioStream;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.audio.AudioKey;
import com.jme3.util.BufferUtils;
import com.jme3.util.LittleEndien;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WAVLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(WAVLoader.class.getName());

    // all these are in big endian
    private static final int i_RIFF = 0x46464952;
    private static final int i_WAVE = 0x45564157;
    private static final int i_fmt  = 0x20746D66 ;
    private static final int i_data = 0x61746164;

    private static final int[] index_table =
    {
      -1, -1, -1, -1, 2, 4, 6, 8,
      -1, -1, -1, -1, 2, 4, 6, 8
    };
    private static final int[] step_table = 
    {
      7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
      19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
      50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
      130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
      337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
      876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
      2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
      5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
      15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
    };

    private boolean readStream = false;

    private AudioBuffer audioBuffer;
    private AudioStream audioStream;
    private AudioData audioData;
    private int bytesPerSec;
    private int dataLength;
    private float duration;

    private LittleEndien in;

    private boolean adpcm = false;
    private int predictor;
    private int step_index;
    private int step;

    private void readFormatChunk(int size) throws IOException{
        // if other compressions are supported, size doesn't have to be 16
//        if (size != 16)
//            logger.warning("Expected size of format chunk to be 16");

        int compression = in.readShort();
        if (compression == 1){

        }else if (compression == 17){
            adpcm = true;
        }else{
            throw new IOException("WAV Loader only supports PCM or ADPCM wave files");
        }

        int channels = in.readShort();
        int sampleRate = in.readInt();

        bytesPerSec = in.readInt(); // used to calculate duration

        int bytesPerSample = in.readShort();
        int bitsPerSample = in.readShort();

        int expectedBytesPerSec = (bitsPerSample * channels * sampleRate) / 8;
        if (expectedBytesPerSec != bytesPerSec){
            logger.log(Level.WARNING, "Expected {0} bytes per second, got {1}",
                    new Object[]{expectedBytesPerSec, bytesPerSec});
        }
        duration = dataLength / bytesPerSec;
        
        if (!adpcm){
            if (bitsPerSample != 8 && bitsPerSample != 16)
                throw new IOException("Only 8 and 16 bits per sample are supported!");

            if ( (bitsPerSample / 8) * channels != bytesPerSample)
                throw new IOException("Invalid bytes per sample value");

            if (bytesPerSample * sampleRate != bytesPerSec)
                throw new IOException("Invalid bytes per second value");

            audioData.setupFormat(channels, bitsPerSample, sampleRate);

            int remaining = size - 16;
            if (remaining > 0)
                in.skipBytes(remaining);
        }else{
            if (bitsPerSample != 4)
                throw new IOException("IMA ADPCM header currupt");

            predictor = in.readShort();
            step_index = in.readByte(); // ????
            int what = in.readByte(); // skip reserved byte
            step = index_table[what];

            audioData.setupFormat(channels, 16, sampleRate);
        }
    }

    private int decodeNibble(int nibble){
        step = step_table[step_index];
        step_index += index_table[nibble];

        if (step_index < 0)
            step_index = 0;
        else if (step_index > 88)
            step_index = 88;

        boolean sign = (nibble & 8) != 0;
        int delta = nibble & 7;

        int diff = (2 * delta + 1) * step;
        if (sign) predictor -= diff;
        else predictor += diff;

        predictor &= 0xFFFF;

        return predictor;
    }

//    private ByteBuffer decodeAdpcm(int len){
//        dataLength = len * 4; // 4 bits per sample to 16 bits per sample
//    }

    private void readDataChunkForBuffer(int len) throws IOException{
        dataLength = len;
        ByteBuffer data = BufferUtils.createByteBuffer(dataLength);
        byte[] buf = new byte[512];
        int read = 0;
        while ( (read = in.read(buf)) > 0){
            data.put(buf, 0, read);
        }
        data.flip();
        audioBuffer.updateData(data);
        in.close();
    }

    public Object load(AssetInfo info) throws IOException {
        this.in = new LittleEndien(info.openStream());

        int sig = in.readInt();
        if (sig != i_RIFF)
            throw new IOException("File is not a WAVE file");
        
        // skip size
        in.readInt();
        if (in.readInt() != i_WAVE)
            throw new IOException("WAVE File does not contain audio");

        readStream = ((AudioKey)info.getKey()).isStream();

        if (readStream){
            audioStream = new AudioStream();
            audioData = audioStream;
        }else{
            audioBuffer = new AudioBuffer();
            audioData = audioBuffer;
        }

        while (true){
            int type = in.readInt();
            int len = in.readInt();

            switch (type){
                case i_fmt:
                    readFormatChunk(len);
                    break;
                case i_data:
                    if (readStream){
                        audioStream.updateData(in, duration);
                    }else{
                        readDataChunkForBuffer(len);
                    }
                    return audioData;
                default:
                    int skipped = in.skipBytes(len);
                    if (skipped <= 0)
                        return null;
                    
                    break;
            }
        }
    }
}
