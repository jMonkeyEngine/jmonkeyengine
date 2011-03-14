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

import de.jarnbjo.ogg.LogicalOggStream;
import de.jarnbjo.ogg.LogicalOggStreamImpl;
import de.jarnbjo.ogg.OggPage;
import de.jarnbjo.ogg.PhysicalOggStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.logging.Logger;

/**
 *  Implementation of the <code>PhysicalOggStream</code> interface for reading
 *  and caching an Ogg stream from a URL. This class reads the data as fast as
 *  possible from the URL, caches it locally either in memory or on disk, and
 *  supports seeking within the available data.
 */
public class CachedOggStream implements PhysicalOggStream {

    private static final Logger logger = Logger.getLogger(CachedOggStream.class.getName());

    private boolean closed = false;
    private InputStream sourceStream;
    private byte[] memoryCache;
    private ArrayList<Long> pageOffsets = new ArrayList<Long>();
    private ArrayList<Long> pageLengths = new ArrayList<Long>();
    private long cacheLength;

    private boolean bos = false;
    private boolean eos = false;
    private int pageNumber;

   private HashMap<Integer, LogicalOggStream> logicalStreams
           = new HashMap<Integer, LogicalOggStream>();

    /**
     *  Creates an instance of this class, using the specified file as cache. The
     *  file is not automatically deleted when this class is disposed.
     */
    public CachedOggStream(InputStream stream, int length, int numPages) throws IOException {
        logger.info("Creating memory cache of size "+length);

        memoryCache = new byte[length];
        sourceStream = stream;

        while (!eos){
            readOggNextPage();
        }
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

   public long getCacheLength() {
      return cacheLength;
   }

    public OggPage getOggPage(int index) throws IOException {
        Long offset = (Long) pageOffsets.get(index);
        Long length = (Long) pageLengths.get(index);

        byte[] tmpArray = new byte[length.intValue()];
        System.arraycopy(memoryCache, offset.intValue(), tmpArray, 0, length.intValue());
        return OggPage.create(tmpArray);
    }

   /**
    * Set the current time as granule position
    * @param granulePosition
    * @throws IOException
    */
   public void setTime(long granulePosition) throws IOException {
       for (LogicalOggStream los : getLogicalStreams()){
           los.setTime(granulePosition);
       }
   }

   /**
    * Read an OggPage from the input stream and put it in the file's cache.
    * @return the page number
    * @throws IOException
    */
   private int readOggNextPage() throws IOException {
       if (eos) // end of stream
           return -1;

       // create ogg page for the stream
       OggPage op = OggPage.create(sourceStream);

       // find location where to write ogg page
       // based on the last ogg page's offset and length.
       int listSize = pageOffsets.size();
       long pos = listSize > 0 ? pageOffsets.get(listSize - 1) + pageLengths.get(listSize - 1) : 0;

       // various data in the ogg page that is needed
       byte[] arr1 = op.getHeader();
       byte[] arr2 = op.getSegmentTable();
       byte[] arr3 = op.getData();

       // put in the memory cache
       System.arraycopy(arr1, 0, memoryCache, (int) pos, arr1.length);
       System.arraycopy(arr2, 0, memoryCache, (int) pos + arr1.length, arr2.length);
       System.arraycopy(arr3, 0, memoryCache, (int) pos + arr1.length + arr2.length, arr3.length);

       // append the information of the ogg page into the offset and length lists
       pageOffsets.add(pos);
       pageLengths.add((long) (arr1.length + arr2.length + arr3.length));

       // check for beginning of stream
       if (op.isBos()){
           bos = true;
       }

       // check for end of stream
       if (op.isEos()){
           eos = true;
       }

       // find the logical ogg stream, if it was created already, based on
       // the stream serial
       LogicalOggStreamImpl los = (LogicalOggStreamImpl) logicalStreams.get(op.getStreamSerialNumber());
       if(los == null) {
           // not created, make a new one
          los = new LogicalOggStreamImpl(this, op.getStreamSerialNumber());
          logicalStreams.put(op.getStreamSerialNumber(), los);
          los.checkFormat(op);
       }

       los.addPageNumberMapping(pageNumber);
       los.addGranulePosition(op.getAbsoluteGranulePosition());

       pageNumber++;
       cacheLength = op.getAbsoluteGranulePosition();

       return pageNumber-1;
   }

   public boolean isSeekable() {
      return true;
   }
}