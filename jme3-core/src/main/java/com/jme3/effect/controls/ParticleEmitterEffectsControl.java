  
/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.effect.controls;
import com.jme3.effect.ParticleEmitter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import java.io.IOException;


public class ParticleEmitterEffectsControl extends AbstractControl implements Control{
    
    private ParticleEmitter particleEmitter;
    
    private ColorRGBA startColorToFadeTo = new ColorRGBA();
    private ColorRGBA initialStartColorToFadeFrom  = new ColorRGBA();
    private float totalStartColorFadeDuration;
    private float startColorFadeDuration;        
    private ColorRGBA endColorToFadeTo  = new ColorRGBA();
    private ColorRGBA initialEndColorToFadeFrom  = new ColorRGBA();
    private float totalEndColorFadeDuration;
    private float endColorFadeDuration; 
    
    private ColorRGBA currentStartColor = new ColorRGBA(); //used internally to store the interpolated values in the fade process in the update loop
    private ColorRGBA currentEndColor = new ColorRGBA();
    

    
    public ParticleEmitter getParticlEmitter() {        return particleEmitter;    }
    public ColorRGBA getStartColorToFadeTo() {        return startColorToFadeTo;    }
    public ColorRGBA getInitialStartColorToFadeFrom() {        return initialStartColorToFadeFrom;    }
    public float getStartColorFadeDuration() {        return startColorFadeDuration;    }    
    public float getTotalStartColorFadeDuration() {        return totalStartColorFadeDuration;    }
    public ColorRGBA getEndColorToFadeTo() {        return endColorToFadeTo;    }    
    public ColorRGBA getInitialEndColorToFadeFrom() {        return initialEndColorToFadeFrom;    }
    public float getTotalEndColorFadeDuration() {        return totalEndColorFadeDuration;    }
    public float getEndColorFadeDuration() {        return endColorFadeDuration;    }
    
    public ParticleEmitterEffectsControl(ParticleEmitter particleEmitter) {
        super();
        
        this.particleEmitter =  particleEmitter;
        
    }


    
    public void setFadeToStartColor(float fadeDuration, ColorRGBA newStartColor){
        totalStartColorFadeDuration = fadeDuration;  
        startColorFadeDuration = fadeDuration;
        startColorToFadeTo.set(newStartColor);
        initialStartColorToFadeFrom.set(particleEmitter.getStartColor());
    }
    
    public void setFadeToEndColor(float fadeDuration, ColorRGBA newEndColor){
        totalEndColorFadeDuration = fadeDuration;
        endColorFadeDuration = fadeDuration;
        endColorToFadeTo.set(newEndColor);
        initialEndColorToFadeFrom.set(particleEmitter.getEndColor());
    }
     
     //convenience method for fading both start and end color over the same duration
    public void setFadeToColors(float fadeDuration, ColorRGBA newStartColor, ColorRGBA newEndColor){
        setFadeToEndColor(fadeDuration, newEndColor);
        setFadeToStartColor(fadeDuration, newStartColor);
    }  
    
    


    private void updateColorFading(float tpf){
        
        //color fading for start color
        if(startColorFadeDuration > 0){
            startColorFadeDuration -= tpf;
            
            if(startColorFadeDuration < 0){
                particleEmitter.setStartColor(startColorToFadeTo);
            }else{
                float timeSlerpPct = 1 - (startColorFadeDuration / totalStartColorFadeDuration);
                
                currentStartColor.set(initialStartColorToFadeFrom);
                currentStartColor.interpolateLocal(startColorToFadeTo, timeSlerpPct);
                particleEmitter.setStartColor(currentStartColor);
            }            
        }        
        
        //color fading for end color
        if(endColorFadeDuration > 0){
            endColorFadeDuration -= tpf;
            if(endColorFadeDuration < 0){
                currentEndColor.set(endColorToFadeTo);
            }else{
                float timeSlerpPct =  1 - (endColorFadeDuration / totalEndColorFadeDuration);
                
                currentEndColor.set(initialEndColorToFadeFrom);
                currentEndColor.interpolateLocal(endColorToFadeTo, timeSlerpPct);
                particleEmitter.setEndColor(currentEndColor);
            }
        }

    }

    @Override
    protected void controlUpdate(float tpf) {
        if(enabled){
            updateColorFading(tpf);
        }   
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        return super.cloneForSpatial(spatial);    
    }
    
   

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
   
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
        super.render(rm, vp);
    }
    
     @Override
     public void cloneFields(Cloner cloner, Object original) {       
        super.cloneFields(cloner, original);
        
        this.startColorToFadeTo = cloner.clone(startColorToFadeTo);
        this.initialStartColorToFadeFrom = cloner.clone(initialStartColorToFadeFrom);
        this.totalStartColorFadeDuration = cloner.clone(totalStartColorFadeDuration);
        this.startColorFadeDuration = cloner.clone(startColorFadeDuration);
        
        this.endColorToFadeTo = cloner.clone(endColorToFadeTo);
        this.initialEndColorToFadeFrom = cloner.clone(initialEndColorToFadeFrom);
        this.totalEndColorFadeDuration = cloner.clone(totalEndColorFadeDuration);
        this.endColorFadeDuration = cloner.clone(endColorFadeDuration);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        
        OutputCapsule oc = ex.getCapsule(this);
        
        oc.write(startColorToFadeTo, "startColorToFadeTo", new ColorRGBA());
        oc.write(initialStartColorToFadeFrom, "initialStartColorToFadeFrom", new ColorRGBA());
        oc.write(totalStartColorFadeDuration, "totalStartColorFadeDuration", 0);
        oc.write(startColorFadeDuration, "startColorFadeDuration", 0);

        oc.write(endColorToFadeTo, "endColorToFadeTo", new ColorRGBA());
        oc.write(initialEndColorToFadeFrom, "initialEndColorToFadeFrom", new ColorRGBA());
        oc.write(totalEndColorFadeDuration, "totalEndColorFadeDuration", 0);
        oc.write(endColorFadeDuration, "endColorFadeDuration", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        
        InputCapsule ic = im.getCapsule(this);
        
        startColorToFadeTo = (ColorRGBA) ic.readSavable("startColorToFadeTo", new ColorRGBA());
        initialStartColorToFadeFrom = (ColorRGBA) ic.readSavable("initialStartColorToFadeFrom", new ColorRGBA());
        totalStartColorFadeDuration = ic.readFloat("totalStartColorFadeDuration", 0);
        startColorFadeDuration = ic.readFloat("startColorFadeDuration", 0);        
        endColorToFadeTo = (ColorRGBA) ic.readSavable("endColorToFadeTo", new ColorRGBA());
        initialEndColorToFadeFrom = (ColorRGBA) ic.readSavable("initialEndColorToFadeFrom", new ColorRGBA());
        totalEndColorFadeDuration = ic.readFloat("totalEndColorFadeDuration", 0);
        endColorFadeDuration = ic.readFloat("endColorFadeDuration", 0);
    }
}
