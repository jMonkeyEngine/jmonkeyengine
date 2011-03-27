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

import com.fluendo.jheora.Comment;
import com.fluendo.jheora.Info;
import com.fluendo.jheora.State;
import com.fluendo.jheora.YUVBuffer;
import com.jcraft.jogg.Packet;
import com.jme3.video.Clock;
import com.jme3.video.SystemClock;
import com.jme3.video.VFrame;
import com.jme3.video.VQueue;
import java.nio.ByteBuffer;

@Deprecated
public class VDecoder implements Clock {

    private int packetIndex = 0;

    private Info info;
    private Comment comment;

    private State state;
    private YUVBuffer yuv;

    private int xOff, yOff, width, height;
    private YUVConv conv = new YUVConv();

    private VQueue videoQueue;

    private long lastTs = -1;
    private long lastUpdateTime = 0;
    private Clock masterClock;

    public VDecoder(VQueue queue) {
        info = new Info();
        comment = new Comment();
        state = new State();
        yuv = new YUVBuffer();
        videoQueue = queue;
    }

    public void setMasterClock(Clock masterClock) {
        this.masterClock = masterClock;
    }

    public long getTime(){
        if (lastTs == -1)
            return 0;

        long timeDiff = System.nanoTime() - lastUpdateTime;
        return lastTs + timeDiff;
    }

    public double getTimeSeconds(){
        return (double) getTime() / Clock.SECONDS_TO_NANOS;
    }

    private void initializeFrames(){
        for (int i = 0; i < videoQueue.remainingCapacity(); i++){
            videoQueue.returnFrame(new VFrame(width, height));
        }
    }

    private void decodeRgbFromBuffer(long time){
        VFrame frame = videoQueue.nextReturnedFrame(true);
        conv.convert(yuv, xOff, yOff, width, height);
        int[] rgb = conv.getRGBData();

        frame.setTime(time);
        ByteBuffer data = frame.getImage().getData(0);
        data.clear();
        data.asIntBuffer().put(rgb);

        try {
            // if it throws an exception someone
            // else modified it. "unknown error"..
            videoQueue.put(frame);
        } catch (InterruptedException ex) {
        }
    }

    public void close(){
        // enqueue a frame with time == -2 to indicate end of stream
        VFrame frame = videoQueue.nextReturnedFrame(true);
        frame.setTime(-2);
        try {
            videoQueue.put(frame);
        } catch (InterruptedException ex) {
        }
    }

    public void decode(Packet packet) {
        //System.out.println ("creating packet");
        if (packetIndex < 3) {
            //System.out.println ("decoding header");
            if (info.decodeHeader(comment, packet) < 0) {
                // error case; not a theora header
                System.err.println("does not contain Theora video data.");
                return;
            }
            if (packetIndex == 2) {
                state.decodeInit(info);

                System.out.println("theora frame: " + info.frame_width + "x" + info.frame_height);
                System.out.println("theora resolution: " + info.width + "x" + info.height);
                System.out.println("theora aspect: " + info.aspect_numerator + "x" + info.aspect_denominator);
                System.out.println("theora framerate: " + info.fps_numerator + "x" + info.fps_denominator);

                xOff = info.offset_x;
                yOff = info.offset_y;
                width = info.frame_width;
                height = info.frame_height;
                initializeFrames();

                if (masterClock instanceof SystemClock){
                    SystemClock clock = (SystemClock) masterClock;
                    if (clock.needReset()){
                        clock.reset();
                        System.out.println("Note: master clock was reset by video");
                    }
                }
            }
        } else {
            // convert to nanos
            long granulePos = packet.granulepos;
            long time = (long) (state.granuleTime(granulePos) * Clock.SECONDS_TO_NANOS);
            long oneFrameTime = (long) ((Clock.SECONDS_TO_NANOS * info.fps_denominator) / info.fps_numerator);
            if (time >= 0){
                lastTs = time;
            }else{
                lastTs += oneFrameTime;
                time = lastTs;
            }
            lastUpdateTime = System.nanoTime();

            if (state.decodePacketin(packet) != 0) {
                System.err.println("Error Decoding Theora.");
                return;
            }

//            if (time >= 0){
                if (state.decodeYUVout(yuv) != 0) {
                    System.err.println("Error getting the picture.");
                    return;
                }
                decodeRgbFromBuffer( time );
//            }
        }
        packetIndex++;
    }

}
