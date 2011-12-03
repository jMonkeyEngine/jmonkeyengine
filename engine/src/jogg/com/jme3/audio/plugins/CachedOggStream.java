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

import com.jme3.util.IntMap;
import de.jarnbjo.ogg.LogicalOggStream;
import de.jarnbjo.ogg.LogicalOggStreamImpl;
import de.jarnbjo.ogg.OggPage;
import de.jarnbjo.ogg.PhysicalOggStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Implementation of the <code>PhysicalOggStream</code> interface for reading
 *  and caching an Ogg stream from a URL. This class reads the data as fast as
 *  possible from the URL, caches it locally either in memory or on disk, and
 *  supports seeking within the available data.
 */
public class CachedOggStream implements PhysicalOggStream {

    private boolean closed = false;
    private boolean eos = false;
    private boolean bos = false;
    private InputStream sourceStream;
    private HashMap<Integer, LogicalOggStream> logicalStreams 
            = new HashMap<Integer, LogicalOggStream>();
    
    private IntMap<OggPage> oggPages = new IntMap<OggPage>();
    private OggPage lastPage;
   
    private int pageNumber;
    
    public CachedOggStream(InputStream in) throws IOException {
        sourceStream = in;

        // Read all OGG pages in file
        long time = System.nanoTime();
        while (!eos){
            readOggNextPage();
        }
        long dt = System.nanoTime() - time;
        Logger.getLogger(CachedOggStream.class.getName()).log(Level.INFO, "Took {0} ms to load OGG", dt/1000000);
    }

    public OggPage getLastOggPage() {
        return lastPage;
    }
    
    private LogicalOggStream getLogicalStream(int serialNumber) {
        return logicalStreams.get(Integer.valueOf(serialNumber));
    }

    public Collection<LogicalOggStream> getLogicalStreams() {
        return logicalStreams.values();
    }

    public boolean isOpen() {
        return !closed;
    }

    public void close() throws IOException {
        closed = true;
        sourceStream.close();
    }

    public OggPage getOggPage(int index) throws IOException {
        return oggPages.get(index);
    }

   public void setTime(long granulePosition) throws IOException {
       for (LogicalOggStream los : getLogicalStreams()){
           los.setTime(granulePosition);
       }
   }

   private int readOggNextPage() throws IOException {
       if (eos)
           return -1;

       OggPage op = OggPage.create(sourceStream);
       if (!op.isBos()){
           bos = true;
       }
       if (op.isEos()){
           eos = true;
           lastPage = op;
       }

       LogicalOggStreamImpl los = (LogicalOggStreamImpl) logicalStreams.get(op.getStreamSerialNumber());
       if(los == null) {
          los = new LogicalOggStreamImpl(this, op.getStreamSerialNumber());
          logicalStreams.put(op.getStreamSerialNumber(), los);
          los.checkFormat(op);
       }

       los.addPageNumberMapping(pageNumber);
       los.addGranulePosition(op.getAbsoluteGranulePosition());

       oggPages.put(pageNumber, op);
       pageNumber++;

       return pageNumber-1;
   }

   public boolean isSeekable() {
      return true;
   }
}