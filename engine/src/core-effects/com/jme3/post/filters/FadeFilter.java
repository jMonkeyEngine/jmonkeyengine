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
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.io.IOException;

/**
 *
 * Fade Filter allows you to make an animated fade effect on a scene.
 * @author Rémy Bouquet aka Nehon
 * implemented from boxjar implementation
 * @see <a href="http://jmonkeyengine.org/groups/graphics/forum/topic/newbie-question-general-fade-inout-effect/#post-105559">http://jmonkeyengine.org/groups/graphics/forum/topic/newbie-question-general-fade-inout-effect/#post-105559</a>
 */
public class FadeFilter extends Filter {

    private float value = 1;
    private boolean playing = false;
    private float direction = 1;
    private float duration = 1;

    /**
     * Creates a FadeFilter
     */
    public FadeFilter() {
        super("Fade In/Out");
    }

    /**
     * Creates a FadeFilter with the given duration
     * @param duration 
     */
    public FadeFilter(float duration) {
        this();
        this.duration = duration;
    }

    @Override
    protected Material getMaterial() {
        material.setFloat("Value", value);
        return material;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        material = new Material(manager, "Common/MatDefs/Post/Fade.j3md");
    }

    @Override
    protected void preFrame(float tpf) {
        if (playing) {
            value += tpf * direction / duration;

            if (direction > 0 && value > 1) {
                value = 1;
                playing = false;
                setEnabled(false);
            }
            if (direction < 0 && value < 0) {
                value = 0;
                playing = false;
                setEnabled(false);
            }
        }
    }

    /**
     * returns the duration of the effect 
     * @return 
     */
    public float getDuration() {
        return duration;
    }

    /**
     * Sets the duration of the filter default is 1 second
     * @param duration 
     */
    public void setDuration(float duration) {
        this.duration = duration;
    }

    /**
     * fades the scene in (black to scene)
     */
    public void fadeIn() {
        setEnabled(true);
        direction = 1;
        playing = true;
    }

    /**
     * fades the scene out (scene to black)
     */
    public void fadeOut() {
        setEnabled(true);
        direction = -1;
        playing = true;

    }

    public void pause() {
        playing = false;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(duration, "duration", 1);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        duration = ic.readFloat("duration", 1);
    }

    /**
     * return the current value of the fading
     * can be used to chack if fade is complete (eg value=1)
     * @return 
     */
    public float getValue() {
        return value;
    }

    /**
     * sets the fade value
     * can be used to force complete black or compete scene
     * @param value 
     */
    public void setValue(float value) {
        this.value = value;       
        if (material != null) {
            material.setFloat("Value", value);
        }
    }
}
