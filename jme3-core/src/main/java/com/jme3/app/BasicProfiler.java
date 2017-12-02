/*
 * Copyright (c) 2014 jMonkeyEngine
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
 
package com.jme3.app;

import com.jme3.profile.*;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;


/**
 *  An AppProfiler implementation that collects two
 *  per-frame application-wide timings for update versus 
 *  render and uses it to create a bar chart style Mesh.  
 *  The number of frames displayed and the update interval 
 *  can be specified.  The chart Mesh is in 'milliseconds' 
 *  and can be scaled up or down as required.
 *
 *  <p>Each column of the chart represents a single frames
 *  timing.  Yellow represents the time it takes to
 *  perform all non-rendering activities (running enqueued
 *  tasks, stateManager.update, control.update(), etc) while
 *  the cyan portion represents the rendering time.</p>
 *
 *  <p>When the end of the chart is reached, the current
 *  frame cycles back around to the beginning.</p> 
 *
 *  @author    Paul Speed
 */
public class BasicProfiler implements AppProfiler {

    private int size;
    private int frameIndex = 0;
    private long[] frames;
    private long startTime;
    private long renderTime;
    private long previousFrame;
    private long updateInterval = 1000000L; // once a millisecond
    private long lastUpdate = 0;
    
    private Mesh mesh;
    
    public BasicProfiler() {
        this(1280);
    }
    
    public BasicProfiler( int size ) {
        setFrameCount(size);
    }

    /**
     *  Sets the number of frames to display and track.  By default
     *  this is 1280.
     */
    public final void setFrameCount( int size ) {
        if( this.size == size ) {
            return;
        }
        
        this.size = size;
        this.frames = new long[size*2];
 
        createMesh();
        
        if( frameIndex >= size ) {
            frameIndex = 0;
        }       
    }
    
    public int getFrameCount() {
        return size;
    }

    /**
     *  Sets the number of nanoseconds to wait before updating the
     *  mesh.  By default this is once a millisecond, ie: 1000000 nanoseconds.
     */
    public void setUpdateInterval( long nanos ) {
        this.updateInterval = nanos;
    }
    
    public long getUpdateInterval() {
        return updateInterval;
    }

    /**
     *  Returns the mesh that contains the bar chart of tracked frame
     *  timings.
     */
    public Mesh getMesh() {
        return mesh;
    }

    protected final void createMesh() {
        if( mesh == null ) {
            mesh = new Mesh();
            mesh.setMode(Mesh.Mode.Lines);
        }
        
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(size * 4 * 3));
        
        FloatBuffer cb = BufferUtils.createFloatBuffer(size * 4 * 4);
        for( int i = 0; i < size; i++ ) {
            // For each index we add 4 colors, one for each line
            // endpoint for two layers.
            cb.put(0.5f).put(0.5f).put(0).put(1);
            cb.put(1).put(1).put(0).put(1);
            cb.put(0).put(0.5f).put(0.5f).put(1);
            cb.put(0).put(1).put(1).put(1);
        }         
        mesh.setBuffer(Type.Color, 4, cb);
    }
    
    protected void updateMesh() {
        FloatBuffer pb = (FloatBuffer)mesh.getBuffer(Type.Position).getData();
        pb.rewind();
        float scale = 1 / 1000000f; // scaled to ms as pixels
        for( int i = 0; i < size; i++ ) {
            float t1 = frames[i * 2] * scale;
            float t2 = frames[i * 2 + 1] * scale;
            
            pb.put(i).put(0).put(0);
            pb.put(i).put(t1).put(0);
            pb.put(i).put(t1).put(0);
            pb.put(i).put(t2).put(0);
        }
        mesh.setBuffer(Type.Position, 3, pb);
    }

    @Override
    public void appStep( AppStep step ) {
        
        switch(step) {
            case BeginFrame:
                startTime = System.nanoTime();
                break;
            case RenderFrame:
                renderTime = System.nanoTime();
                frames[frameIndex * 2] = renderTime - startTime;
                break;
            case EndFrame:
                long time = System.nanoTime();
                frames[frameIndex * 2 + 1] = time - renderTime;
                previousFrame = startTime; 
                frameIndex++;
                if( frameIndex >= size ) {
                    frameIndex = 0;
                }
                if( startTime - lastUpdate > updateInterval ) {
                    updateMesh();
                    lastUpdate = startTime;
                }                
                break;
        }
    }
    
    @Override
    public void vpStep( VpStep step, ViewPort vp, Bucket bucket ) {
    }

    @Override
    public void spStep(SpStep step, String... additionalInfo) {

    }

}


