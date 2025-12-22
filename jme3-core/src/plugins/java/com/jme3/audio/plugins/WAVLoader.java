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
package com.jme3.audio.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioStream;
import com.jme3.audio.SeekableStream;
import com.jme3.export.binary.ByteUtils;
import com.jme3.util.BufferUtils;
import com.jme3.util.LittleEndien;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@code AssetLoader} for loading WAV audio files.
 * This loader supports PCM (Pulse Code Modulation) WAV files,
 * both as in-memory {@link AudioBuffer}s and streaming {@link AudioStream}s.
 * It handles 8-bit and 16-bit audio formats.
 *
 * <p>The WAV file format consists of chunks. This loader specifically parses
 * the 'RIFF', 'WAVE', 'fmt ', and 'data' chunks.
 */
public class WAVLoader implements AssetLoader {

    private static final Logger logger = Logger.getLogger(WAVLoader.class.getName());

    // RIFF chunk identifiers (Big-Endian representation of ASCII characters)
    private static final int i_RIFF = 0x46464952;
    private static final int i_WAVE = 0x45564157;
    private static final int i_fmt  = 0x20746D66;
    private static final int i_data = 0x61746164;

    /**
     * The number of bytes per second for the audio data, calculated from the WAV header.
     * Used to determine the duration of the audio.
     */
    private int bytesPerSec;
    /**
     * The duration of the audio in seconds.
     */
    private float duration;
    /**
     * The input stream for reading the WAV file data.
     */
    private ResettableInputStream in;

    /**
     * A custom {@link InputStream} extension that handles little-endian byte
     * reading and provides seek capabilities for streaming audio by reopening
     * and skipping the input stream.
     */
    private static class ResettableInputStream extends LittleEndien implements SeekableStream {
        
        private final AssetInfo info;
        private int resetOffset = 0;
        
        public ResettableInputStream(AssetInfo info, InputStream in) {
            super(in);
            this.info = info;
        }

        /**
         * Sets the offset from the beginning of the file to reset the stream to.
         * This is typically the start of the audio data chunk.
         *
         * @param offset The byte offset to reset to.
         */
        public void setResetOffset(int offset) {
            this.resetOffset = offset;
        }

        @Override
        public void setTime(float time) {
            if (time != 0f) {
                throw new UnsupportedOperationException("Seeking WAV files not supported");
            }
            InputStream newStream = info.openStream();
            try {
                ByteUtils.skipFully(newStream, resetOffset);
                this.in = new BufferedInputStream(newStream);
            } catch (IOException ex) {
                // Resource could have gotten lost, etc.
                try {
                    newStream.close();
                } catch (IOException ignored) {
                }
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Reads and parses the 'fmt ' (format) chunk of the WAV file.
     * This chunk contains information about the audio format such as
     * compression, channels, sample rate, bits per sample, etc.
     *
     * @param chunkSize The size of the 'fmt ' chunk in bytes.
     * @param audioData The {@link AudioData} object to set the format information on.
     * @throws IOException if the file is not a supported PCM WAV, or if format
     * parameters are invalid.
     */
    private void readFormatChunk(int chunkSize, AudioData audioData) throws IOException {
        // if other compressions are supported, size doesn't have to be 16
//        if (size != 16)
//            logger.warning("Expected size of format chunk to be 16");

        int compression = in.readShort();
        if (compression != 1) { // 1 = PCM (Pulse Code Modulation)
            throw new IOException("WAV Loader only supports PCM wave files");
        }

        int numChannels = in.readShort();
        int sampleRate = in.readInt();
        bytesPerSec = in.readInt(); // Average bytes per second

        int bytesPerSample = in.readShort(); // Bytes per sample block (channels * bytesPerSample)
        int bitsPerSample = in.readShort();

        int expectedBytesPerSec = (bitsPerSample * numChannels * sampleRate) / 8;
        if (expectedBytesPerSec != bytesPerSec) {
            logger.log(Level.WARNING, "Expected {0} bytes per second, got {1}",
                    new Object[]{expectedBytesPerSec, bytesPerSec});
        }

        if (bitsPerSample != 8 && bitsPerSample != 16)
            throw new IOException("Only 8 and 16 bits per sample are supported!");

        if ((bitsPerSample / 8) * numChannels != bytesPerSample)
            throw new IOException("Invalid bytes per sample value");

        if (bytesPerSample * sampleRate != bytesPerSec)
            throw new IOException("Invalid bytes per second value");

        audioData.setupFormat(numChannels, bitsPerSample, sampleRate);

        // Skip any extra parameters in the format chunk (e.g., for non-PCM formats)
        int remainingChunkBytes = chunkSize - 16;
        if (remainingChunkBytes > 0) {
            ByteUtils.skipFully((InputStream)in, remainingChunkBytes);
        }
    }

    /**
     * Reads the 'data' chunk for an {@link AudioBuffer}. This involves loading
     * the entire audio data into a {@link ByteBuffer} in memory.
     *
     * @param dataChunkSize The size of the 'data' chunk in bytes.
     * @param audioBuffer   The {@link AudioBuffer} to update with the loaded data.
     * @throws IOException if an error occurs while reading the data.
     */
    private void readDataChunkForBuffer(int dataChunkSize, AudioBuffer audioBuffer) throws IOException {
        ByteBuffer data = BufferUtils.createByteBuffer(dataChunkSize);
        byte[] buf = new byte[1024]; // Use a larger buffer for efficiency
        int read = 0;
        while ((read = in.read(buf)) > 0) {
            data.put(buf, 0, Math.min(read, data.remaining()));
        }
        data.flip();
        audioBuffer.updateData(data);
        in.close();
    }

    /**
     * Configures the {@link AudioStream} to stream data from the 'data' chunk.
     * This involves setting the reset offset for seeking and passing the
     * input stream and duration to the {@link AudioStream}.
     *
     * @param dataChunkOffset The byte offset from the start of the file where the 'data' chunk begins.
     * @param dataChunkSize   The size of the 'data' chunk in bytes.
     * @param audioStream     The {@link AudioStream} to configure.
     */
    private void readDataChunkForStream(int dataChunkOffset, int dataChunkSize, AudioStream audioStream) {
        in.setResetOffset(dataChunkOffset);
        audioStream.updateData(in, duration);
    }

    /**
     * Main loading logic for WAV files. This method parses the RIFF, WAVE, fmt,
     * and data chunks to extract audio information and data.
     *
     * @param info        The {@link AssetInfo} for the WAV file.
     * @param inputStream The initial {@link InputStream} opened for the asset.
     * @param stream      A boolean indicating whether the audio should be loaded
     *                    as a stream (true) or an in-memory buffer (false).
     * @return The loaded {@link AudioData} (either {@link AudioBuffer} or {@link AudioStream}).
     * @throws IOException if the file is not a valid WAV, or if any I/O error occurs.
     */
    private AudioData load(AssetInfo info, InputStream inputStream, boolean stream) throws IOException {
        this.in = new ResettableInputStream(info, inputStream);
        int inOffset = 0;

        // Read RIFF chunk
        int riffId = in.readInt();
        if (riffId != i_RIFF) {
            throw new IOException("File is not a WAVE file");
        }

        // Skip RIFF chunk size
        in.readInt();

        int waveId = in.readInt();
        if (waveId != i_WAVE)
            throw new IOException("WAVE File does not contain audio");

        inOffset += 4 * 3; // RIFF_ID + ChunkSize + WAVE_ID

        AudioData audioData;
        AudioBuffer audioBuffer = null;
        AudioStream audioStream = null;

        if (stream) {
            audioStream = new AudioStream();
            audioData = audioStream;
        } else {
            audioBuffer = new AudioBuffer();
            audioData = audioBuffer;
        }

        while (true) {
            int chunkType = in.readInt();
            int chunkSize = in.readInt();

            inOffset += 4 * 2; // ChunkType + ChunkSize

            switch (chunkType) {
                case i_fmt:
                    readFormatChunk(chunkSize, audioData);
                    inOffset += chunkSize;
                    break;
                case i_data:
                    // Compute duration based on data chunk size
                    duration = (float) (chunkSize / bytesPerSec);

                    if (stream) {
                        readDataChunkForStream(inOffset, chunkSize, audioStream);
                    } else {
                        readDataChunkForBuffer(chunkSize, audioBuffer);
                    }
                    return audioData;
                default:
                    // Skip unknown chunks
                    int skippedBytes = in.skipBytes(chunkSize);
                    if (skippedBytes <= 0) {
                        logger.log(Level.WARNING, "Reached end of stream prematurely while skipping unknown chunk of size {0}. Asset: {1}",
                                new Object[]{chunkSize, info.getKey().getName()});
                        return null;
                    }
                    inOffset += skippedBytes;
                    break;
            }
        }
    }
    
    @Override
    public Object load(AssetInfo info) throws IOException {
        InputStream is = null;
        try {
            is = info.openStream();
            boolean streamAudio = ((AudioKey) info.getKey()).isStream();
            AudioData loadedData = load(info, is, streamAudio);

            // If it's an AudioStream, the internal inputStream is managed by the stream itself
            // and should not be closed here.
            if (loadedData instanceof AudioStream) {
                // Prevent closing in finally block
                is = null;
            }
            return loadedData;
        } finally {
            // Nullify/reset instance variables to ensure the loader instance is clean
            // for the next load operation.
            in = null;
            bytesPerSec = 0;
            duration = 0.0f;

            if (is != null) {
                is.close();
            }
        }
    }
}
