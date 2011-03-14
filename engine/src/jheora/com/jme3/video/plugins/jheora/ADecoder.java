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

package com.jme3.video.plugins.jheora;

import com.jcraft.jogg.Packet;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import com.jme3.audio.AudioStream;
import com.jme3.video.Clock;
import com.jme3.video.RingBuffer;
import com.jme3.video.SystemClock;
import java.io.InputStream;

public class ADecoder extends InputStream implements Clock {

    private int packetIndex = 0;

    private final DspState dsp;
    private Block block;

    private Info info;
    private Comment comment;

    private float[][][] pcmAll = new float[1][][];
    private int[] index;

    private AudioStream stream;
    private RingBuffer ringBuffer = new RingBuffer(48000 * 2 * 2 * 2);

    private int UNCOMP_BUFSIZE = 4096;
    private byte[] uncompBuf = new byte[UNCOMP_BUFSIZE];

    private long lastPts = 0;
    private long lastWritten = 0;
    private long lastRead = 0;
    private long lastPtsRead = 0;
    private long lastPtsWrite = 0;

    private long timeDiffSum = 0;
    private int timesDesynced = 0;
    private Clock masterClock;

    public ADecoder(){
        info = new Info();
        info.init();
        
        comment = new Comment();
        comment.init();

        dsp = new DspState();
        block = new Block(dsp);
    }

    public void setMasterClock(Clock masterClock) {
        this.masterClock = masterClock;
    }

    public AudioStream getAudioStream(){
        return stream;
    }

    public long getTime(){
        long bytesRead = ringBuffer.getTotalRead();
        if (bytesRead == 0)
            return 0;
        
        //long diff = bytesRead - lastWritten;
        long diff = bytesRead;
        long diffNs = (diff * Clock.SECONDS_TO_NANOS) / (2 * info.channels * info.rate);
        long timeSinceLastRead = System.nanoTime() - lastPtsRead;
        return /*lastPts +*/ diffNs + timeSinceLastRead;
    }

    public double getTimeSeconds(){
        return (double) getTime() / Clock.SECONDS_TO_NANOS;
    }

    public int read(){
        byte[] buf = new byte[1];
        int read = read(buf, 0, 1);
        if (read < 0)
            return -1;
        else
            return buf[0] & 0xff;
    }

    private static final long NOSYNC_THRESH     = 3 * Clock.SECONDS_TO_NANOS,
                              AUDIO_DIFF_THRESH = Clock.SECONDS_TO_NANOS / 2;

    private static final int  NUM_DIFFS_FOR_SYNC = 5;
    private static final double PRODUCT_FOR_PREV = 6.0 / 10.0,
                                PRODUCT_FOR_PREV_INV = 4.0 / 10.0;

    private int needToSkip = 0;

    /**
     * Only useful if audio is synced to something else.
     */
    private void sync(){
        if (needToSkip > 0){
            int skipped = ringBuffer.skip(needToSkip);
            System.out.println("Skipped: "+skipped);
            needToSkip -= skipped;
        }

        long masterTime = masterClock.getTime();
        long audioTime = getTime();
        long diff = audioTime - masterTime;
        if (diff < NOSYNC_THRESH){
            timeDiffSum = diff + (long) (timeDiffSum * PRODUCT_FOR_PREV);
            if (timesDesynced < NUM_DIFFS_FOR_SYNC){
                timesDesynced ++;
            }else{
                long avgDiff = (long) (timeDiffSum * PRODUCT_FOR_PREV_INV);
                if (Math.abs(avgDiff) >= AUDIO_DIFF_THRESH){
                    if (diff < 0){
                        int toSkip = (int) ((-diff * 2 * info.channels * info.rate) / Clock.SECONDS_TO_NANOS);
                        int skipped = ringBuffer.skip(toSkip);
                        System.out.println("Skipped: "+skipped);
                        if (skipped < toSkip)
                            needToSkip = toSkip - skipped;
                       
                        timeDiffSum = 0;
                        timesDesynced = 0;
                    }
                }
            }
        }else{
            timesDesynced = 0;
            timeDiffSum   = 0;
        }
    }

    @Override
    public int read(byte[] buf, int offset, int length){
//        int diff = (int) (ringBuffer.getTotalWritten() - ringBuffer.getTotalRead());
//        if ( diff > info.rate * info.channels * 2){
//            System.out.println("Warning: more than 1 second lag for audio. Adjusting..");
//            ringBuffer.skip( diff );
//        }


//        if (masterClock != null)
//            sync();

        int r = ringBuffer.read(buf, offset, length);
        if (r <= 0){
//            // put silence
            for (int i = 0; i < length; i++){
                buf[offset + i] = 0x0;
            }
            return length;
        }else{
            lastPtsRead = System.nanoTime();
        }
        return r;
    }

    public void decodeDsp(Packet packet){
        if (block.synthesis(packet) == 0) {
            dsp.synthesis_blockin(block);
        }

        int samplesAvail;
        int channels = info.channels;
        while ((samplesAvail = dsp.synthesis_pcmout(pcmAll, index)) > 0) {
            float[][] pcm = pcmAll[0];
            int samplesCanRead = UNCOMP_BUFSIZE / (channels*2);
            int samplesToRead = (samplesAvail < samplesCanRead ? samplesAvail : samplesCanRead);

            // convert floats to 16 bit signed ints and interleave
            for (int i = 0; i < channels; i++) {
                // each sample is two bytes, the sample for the 2nd
                // channel is at index 2, etc.
                int writeOff = i * 2;
                int readOff = index[i];
                for (int j = 0; j < samplesToRead; j++) {
                    int val = (int) (pcm[i][readOff + j] * 32767.0);
                    // guard against clipping
                    if (val > 32767) {
                        val = 32767;
                    }if (val < -32768) {
                        val = -32768;
                    }
                    uncompBuf[writeOff]     = (byte) (val);
                    uncompBuf[writeOff + 1] = (byte) (val >> 8);

                    writeOff += 2 * channels; // each sample is 2 bytes
                }
            }

            ringBuffer.write(uncompBuf, 0, samplesToRead * channels * 2);

            // tell vorbis how many samples were actualy consumed
            dsp.synthesis_read(samplesToRead);
        }
    }

    @Override
    public void close(){
    }

    public void decode(Packet packet){
        if (packetIndex < 3) {
            if (info.synthesis_headerin(comment, packet) < 0) {
                // error case; not a Vorbis header
                System.err.println("does not contain Vorbis audio data.");
                return;
            }
            if (packetIndex == 2) {
                dsp.synthesis_init(info);
                block.init(dsp);
                System.out.println("vorbis: "+info);
                System.out.println(comment.toString());
                index = new int[info.channels];

                if (stream == null){
                    stream = new AudioStream();
                    stream.setupFormat(info.channels, 16, info.rate);
                    stream.updateData(this, -1);
                }

                if (masterClock instanceof SystemClock){
                    SystemClock clock = (SystemClock) masterClock;
                    if (clock.needReset()){
                        clock.reset();
                        System.out.println("Note: master clock was reset by audio");
                    }
                }
            }
        } else {
            long gp = packet.granulepos;
            if (gp != -1){
                lastPts = (gp * Clock.SECONDS_TO_NANOS) / info.rate;
                lastWritten = ringBuffer.getTotalWritten();
                lastRead    = ringBuffer.getTotalRead();
                lastPtsWrite = System.nanoTime();
            }
            
            decodeDsp(packet);
        }
        packetIndex++;
    }

}
