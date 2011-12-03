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

import de.jarnbjo.ogg.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Single-threaded physical ogg stream. Decodes audio in the same thread
 * that reads.
 */
public class UncachedOggStream implements PhysicalOggStream {

    private boolean closed = false;
    private boolean eos = false;
    private boolean bos = false;
    private InputStream sourceStream;
    private LinkedList<OggPage> pageCache = new LinkedList<OggPage>();
    private HashMap<Integer, LogicalOggStream> logicalStreams 
            = new HashMap<Integer, LogicalOggStream>();
    private OggPage lastPage = null;

    public UncachedOggStream(InputStream in) throws OggFormatException, IOException {
        this.sourceStream = in;

        // read until beginning of stream
        while (!bos){
            readNextOggPage();
        }

        // now buffer up an addition 25 pages
//        while (pageCache.size() < 25 && !eos){
//            readNextOggPage();
//        }
    }

    public OggPage getLastOggPage() {
        return lastPage;
    }

    private void readNextOggPage() throws IOException {
        OggPage op = OggPage.create(sourceStream);
        if (!op.isBos()){
            bos = true;
        }
        if (op.isEos()){
            eos = true;
            lastPage = op;
        }

        LogicalOggStreamImpl los = (LogicalOggStreamImpl) getLogicalStream(op.getStreamSerialNumber());
        if (los == null){
            los = new LogicalOggStreamImpl(this, op.getStreamSerialNumber());
            logicalStreams.put(op.getStreamSerialNumber(), los);
            los.checkFormat(op);
        }

        pageCache.add(op);
    }

    public OggPage getOggPage(int index) throws IOException {
        if (eos){
            return null;
        }

//        if (!eos){
//            int num = pageCache.size();
//            long fiveMillis = 5000000;
//            long timeStart  = System.nanoTime();
//            do {
//                readNextOggPage();
//            } while ( !eos && (System.nanoTime() - timeStart) < fiveMillis );
//            System.out.println( pageCache.size() - num );

            if (pageCache.size() == 0 /*&& !eos*/){
                readNextOggPage();
            }
//        }

        return pageCache.removeFirst();
    }

    private LogicalOggStream getLogicalStream(int serialNumber) {
        return logicalStreams.get(Integer.valueOf(serialNumber));
    }

    public Collection<LogicalOggStream> getLogicalStreams() {
        return logicalStreams.values();
    }

    public void setTime(long granulePosition) throws IOException {
    }

    public boolean isSeekable() {
        return false;
    }

    public boolean isOpen() {
        return !closed;
    }

    public void close() throws IOException {
        closed = true;
        sourceStream.close();
    }
}
