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

package com.jme3.audio;

import com.jme3.util.NativeObject;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>AudioStream</code> is an implementation of AudioData that
 * acquires the audio from an InputStream. Audio can be streamed
 * from network, hard drive etc. It is assumed the data coming
 * from the input stream is uncompressed.
 *
 * @author Kirill Vainer
 */
public class AudioStream extends AudioData implements Closeable{

    private final static Logger logger = Logger.getLogger(AudioStream.class.getName());
    protected InputStream in;
    protected float duration = -1f;
    protected boolean open = false;
    protected int[] ids;
    
    public AudioStream(){
        super();        
    }
    
    protected AudioStream(int[] ids){
        // Pass some dummy ID so handle
        // doesn't get created.
        super(-1);      
        // This is what gets destroyed in reality
        this.ids = ids;
    }

    public void updateData(InputStream in, float duration){
        if (id != -1 || this.in != null)
            throw new IllegalStateException("Data already set!");

        this.in = in;
        this.duration = duration;
        open = true;
    }

    /**
     * Reads samples from the stream. The format of the data
     * depends on the getSampleRate(), getChannels(), getBitsPerSample()
     * values.
     *
     * @param buf Buffer where to read the samples
     * @param offset The offset in the buffer where to read samples
     * @param length The length inside the buffer where to read samples
     * @return number of bytes read.
     */
    public int readSamples(byte[] buf, int offset, int length){
        if (!open)
            return -1;

        try{
            return in.read(buf, offset, length);
        }catch (IOException ex){
            return -1;
        }
    }

    /**
     * Reads samples from the stream.
     *
     * @see AudioStream#readSamples(byte[], int, int)
     * @param buf Buffer where to read the samples
     * @return number of bytes read.
     */
    public int readSamples(byte[] buf){
        return readSamples(buf, 0, buf.length);
    }

    public float getDuration(){
        return duration;
    }

    @Override
    public int getId(){
        throw new RuntimeException("Don't use getId() on streams");
    }

    @Override
    public void setId(int id){
        throw new RuntimeException("Don't use setId() on streams");
    }

    public void initIds(int count){
        ids = new int[count];
    }

    public int getId(int index){
        return ids[index];
    }

    public void setId(int index, int id){
        ids[index] = id;
    }

    public int[] getIds(){
        return ids;
    }

    public void setIds(int[] ids){
        this.ids = ids;
    }

    @Override
    public DataType getDataType() {
        return DataType.Stream;
    }

    @Override
    public void resetObject() {
        id = -1;
        ids = null;
        setUpdateNeeded();
    }

    @Override
    public void deleteObject(Object rendererObject) {
        // It seems that the audio renderer is already doing a good
        // job at deleting audio streams when they finish playing.
//        ((AudioRenderer)rendererObject).deleteAudioData(this);
    }

    @Override
    public NativeObject createDestructableClone() {
        return new AudioStream(ids);
    }
    
    /**
     * @return Whether the stream is open or not. Reading from a closed
     * stream will always return eof.
     */
    public boolean isOpen(){
        return open;
    }

    /**
     * Closes the stream, releasing all data relating to it. Reading
     * from the stream will return eof.
     * @throws IOException
     */
    public void close() {
        if (in != null && open){
            try{
                in.close();
            }catch (IOException ex){
            }
            open = false;
        }else{
            throw new RuntimeException("AudioStream is already closed!");
        }
    }

  
    public void setTime(float time){
        if(in instanceof SeekableStream){
            ((SeekableStream)in).setTime(time);
        }else{
            logger.log(Level.WARNING,"Cannot use setTime on a stream that is not seekable. You must load the file with the streamCache option set to true");
        }
    }

    
}
