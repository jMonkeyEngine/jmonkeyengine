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
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jme3.audio.AudioStream;
import com.jme3.util.IntMap;
import com.jme3.video.Clock;
import com.jme3.video.SystemClock;
import com.jme3.video.VQueue;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AVThread implements Runnable {

    private static final Logger logger = Logger.getLogger(AVThread.class.getName());

    private static final int BUFFSIZE = 8192;

    /**
     * OggPage.
     */
    private Page page;

    /**
     * OggPacket, constructed from OggPages.
     */
    private Packet packet;

    /**
     * SyncState.
     */
    private SyncState syncState;

    /**
     * Stream of .ogg file.
     */
    private InputStream oggStream;

    private AtomicBoolean cancel = new AtomicBoolean(false);

    private IntMap<StreamState> streams = new IntMap<StreamState>();
    private int theoraSerial, vorbisSerial;
    private ADecoder audioDecoder;
    private VDecoder videoDecoder;
    private Clock masterClock;
    private Clock systemClock;

    public AVThread(InputStream oggStream, VQueue videoQueue){
        this.oggStream = oggStream;
        videoDecoder = new VDecoder(videoQueue);
        audioDecoder = new ADecoder();
        systemClock = new SystemClock();
//        masterClock = systemClock;//audioDecoder;
        masterClock = audioDecoder;
        audioDecoder.setMasterClock(masterClock);
        videoDecoder.setMasterClock(masterClock);
//        masterClock = videoDecoder;
//        masterClock = systemClock;
//        audioDecoder.setMasterClock(masterClock);
    }

    public AudioStream getAudioStream(){
        return audioDecoder.getAudioStream();
    }

    public void stop(){
        cancel.set(true);
    }

    private void done(){
        videoDecoder.close();
        audioDecoder.close();
        try {
            oggStream.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error while closing Ogg Video", ex);
        }
    }

    public Clock getMasterClock(){
        return masterClock;
    }

    public Clock getSystemClock(){
        return systemClock;
    }

    public Clock getVideoClock(){
        return videoDecoder;
    }

    public Clock getAudioClock(){
        return audioDecoder;
    }

    public void run(){
        page = new Page();
        packet = new Packet();
        syncState = new SyncState();

        while (!cancel.get()) {
            int index = syncState.buffer(BUFFSIZE);
            
            // Read from stream into syncState's buffer.
            int read;
            try {
                read = oggStream.read(syncState.data, index, BUFFSIZE);
            } catch (IOException ex){
                logger.log(Level.SEVERE, "Error while decoding Ogg Video", ex);
                return;
            }

            if (read < 0){
                // EOF
                break;
            }

            syncState.wrote(read);

            while (!cancel.get()) {
                // Acquire page from syncState
                int res = syncState.pageout(page);
                if (res == 0)
                    break; // need more data
                
                if (res == -1) {
                    // missing or corrupt data at this page position
                    // no reason to complain; already complained above
                } else {
                    int serial = page.serialno();
                    StreamState state = streams.get(serial);
                    boolean newStream = false;
                    if (state == null){
                        state = new StreamState();
                        state.init(serial);
                        state.reset();
                        streams.put(serial, state);
                        newStream = true;
                    }

                    // Give StreamState the page
                    res = state.pagein(page);
                    if (res < 0) {
                        // error; stream version mismatch perhaps
                        System.err.println("Error reading first page of Ogg bitstream data.");
                        return;
                    }
                    while (!cancel.get()) {
                        // Get a packet out of the stream state
                        res = state.packetout(packet);
                        if (res == 0)
                            break;
                        
                        if (res == -1) {
                            // missing or corrupt data at this page position
                            // no reason to complain; already complained above
                        } else {
                            // Packet acquired!
                            if (newStream) {
                                // typefind
                                int packetId = packet.packet;
                                byte[] packetBase = packet.packet_base;
                                if (packetBase[packetId + 1] == 0x76) {
                                    vorbisSerial = serial;
                                } else if (packet.packet_base[packet.packet + 1] == 0x73) {
                                    // smoke video! ignored
                                    logger.log(Level.WARNING, "Smoke video detected. Unsupported!");
                                } else if (packet.packet_base[packet.packet + 1] == 0x74) {
                                    theoraSerial = serial;
                                }
                            }
                            if (serial == theoraSerial){
                                videoDecoder.decode(packet);
                            }else if (serial == vorbisSerial){
                                audioDecoder.decode(packet);
                            }
                        }
                    }
                }
            }
        }
        done();
    }

}
