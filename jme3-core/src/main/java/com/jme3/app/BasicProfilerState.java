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

import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;


/**
 *  Provides a basic profiling visualization that shows
 *  per-frame application-wide timings for update and
 *  rendering.
 *
 *  @author    Paul Speed
 */
public class BasicProfilerState extends BaseAppState {

    public static final String INPUT_MAPPING_PROFILER_TOGGLE = "BasicProfilerState_Toggle";

    private BasicProfiler profiler;
    private Geometry graph;
    private Geometry background;
    private float scale = 2;

    private final ProfilerKeyListener keyListener = new ProfilerKeyListener();

    public BasicProfilerState() {
        this(false);
    }

    public BasicProfilerState( boolean enabled ) {
        setEnabled(enabled);
        this.profiler = new BasicProfiler();
    }

    public void toggleProfiler() {
        setEnabled(!isEnabled());
    }

    public BasicProfiler getProfiler() {
        return profiler;
    }

    /**
     *  Sets the vertical scale of the visualization where
     *  each unit is a millisecond.  Defaults to 2, ie: a
     *  single millisecond stretches two pixels high.
     * @param scale the scale
     */
    public void setGraphScale( float scale ) {
        if( this.scale == scale ) {
            return;
        }
        this.scale = scale;
        if( graph != null ) {
            graph.setLocalScale(1, scale, 1);            
        }
    }

    public float getGraphScale() {
        return scale;
    }
 
    /**
     *  Sets the number frames displayed and tracked.
     * @param count the number of frames
     */
    public void setFrameCount( int count ) {
        if( profiler.getFrameCount() == count ) {
            return;
        }
        profiler.setFrameCount(count);
        refreshBackground();
    }
    
    public int getFrameCount() {
        return profiler.getFrameCount();
    }
    
    protected void refreshBackground() {
        Mesh mesh = background.getMesh();
        
        int size = profiler.getFrameCount();
        float frameTime = 1000f / 60;
        mesh.setBuffer(Type.Position, 3, new float[] {
                    
                    // first quad
                    0, 0, 0,
                    size, 0, 0,
                    size, frameTime, 0,
                    0, frameTime, 0,
                    
                    // second quad
                    0, frameTime, 0,
                    size, frameTime, 0,
                    size, frameTime * 2, 0,
                    0, frameTime * 2, 0,
                    
                    // A lower dark border just to frame the
                    // 'update' stats against bright backgrounds
                    0, -2, 0,
                    size, -2, 0,
                    size, 0, 0,
                    0, 0, 0 
                });
                
        mesh.setBuffer(Type.Color, 4, new float[] {
                    // first quad, within normal frame limits
                    0, 1, 0, 0.25f,
                    0, 1, 0, 0.25f,
                    0, 0.25f, 0, 0.25f,
                    0, 0.25f, 0, 0.25f,
                    
                    // Second quad, dropped frames                    
                    0.25f, 0, 0, 0.25f,
                    0.25f, 0, 0, 0.25f,
                    1, 0, 0, 0.25f,
                    1, 0, 0, 0.25f,
                    
                    0, 0, 0, 0.5f,
                    0, 0, 0, 0.5f,
                    0, 0, 0, 0.5f,
                    0, 0, 0, 0.5f                                         
                });
                
        mesh.setBuffer(Type.Index, 3, new short[] {
                    0, 1, 2,
                    0, 2, 3,
                    4, 5, 6,
                    4, 6, 7,
                    8, 9, 10,
                    8, 10, 11
                });
    }

    @Override
    protected void initialize( Application app ) {
        
        graph = new Geometry("profiler", profiler.getMesh());
        
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setBoolean("VertexColor", true);
        graph.setMaterial(mat);
        graph.setLocalTranslation(0, 300, 0);
        graph.setLocalScale(1, scale, 1);
               
        Mesh mesh = new Mesh();
        background = new Geometry("profiler.background", mesh);
        mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setBoolean("VertexColor", true);
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        background.setMaterial(mat);
        background.setLocalTranslation(0, 300, -1);
        background.setLocalScale(1, scale, 1);        
        
        refreshBackground();
        
        InputManager inputManager = app.getInputManager();        
        if( inputManager != null ) { 
            inputManager.addMapping(INPUT_MAPPING_PROFILER_TOGGLE, new KeyTrigger(KeyInput.KEY_F6));
            inputManager.addListener(keyListener, INPUT_MAPPING_PROFILER_TOGGLE); 
        }               
    }

    @Override
    protected void cleanup( Application app ) {    
        InputManager inputManager = app.getInputManager();        
        if( inputManager.hasMapping(INPUT_MAPPING_PROFILER_TOGGLE) ) {
            inputManager.deleteMapping(INPUT_MAPPING_PROFILER_TOGGLE);        
        }
        inputManager.removeListener(keyListener);
    }

    @Override
    protected void onEnable() {
    
        // Set the number of visible frames to the current width of the screen
        setFrameCount(getApplication().getCamera().getWidth());
    
        getApplication().setAppProfiler(profiler);
        Node gui = ((SimpleApplication)getApplication()).getGuiNode();
        gui.attachChild(graph);
        gui.attachChild(background);
    }

    @Override
    protected void onDisable() {
        getApplication().setAppProfiler(null);
        graph.removeFromParent();
        background.removeFromParent();
    }
    
    private class ProfilerKeyListener implements ActionListener {

        @Override
        public void onAction(String name, boolean value, float tpf) {
            if (!value) {
                return;
            }
            toggleProfiler();
        }
    }
}

