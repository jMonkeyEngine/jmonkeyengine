 /*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.light;

import com.jme3.bounding.BoundingSphere;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.TempVars;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * this processor allows to blend several light probes maps together according to a Point of Interest.
 * This is all based on this article by Sebastien lagarde
 * https://seblagarde.wordpress.com/2012/09/29/image-based-lighting-approaches-and-parallax-corrected-cubemap/
 * @author Nehon
 */
public class LightProbeBlendingProcessor implements SceneProcessor {
    
    private ViewPort viewPort;
    private LightFilter prevFilter;
    private RenderManager renderManager;
    private LightProbe probe = new LightProbe();
    private Spatial poi;
    private AppProfiler prof;

    public LightProbeBlendingProcessor(Spatial poi) {        
        this.poi = poi;
    }
    
    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        viewPort = vp;        
        renderManager = rm;
        prevFilter = rm.getLightFilter();
        rm.setLightFilter(new PoiLightProbeLightFilter(this));        
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        
    }

    @Override
    public boolean isInitialized() {
        return viewPort != null;
    }

    @Override
    public void preFrame(float tpf) {
        
    }
    
    /** 1. For POI take a spatial in the constructor and make all calculation against its world pos
    *      - Alternatively compute an arbitrary POI by casting rays from the camera 
    *        (one in the center and one for each corner and take the median point)
    *   2. Take the 4 most weighted probes for default. Maybe allow the user to change this
    *   3. For the inner influence radius take half of the radius for a start we'll see then how to change this.
    *   
    */
    @Override
    public void postQueue(RenderQueue rq) {
        List<BlendFactor> blendFactors = new ArrayList<BlendFactor>();
        float sumBlendFactors = computeBlendFactors(blendFactors);
        
        //Sort blend factors according to their weight
        Collections.sort(blendFactors);        
        
        //normalize blend factors;
        float normalizer = 1f / sumBlendFactors;
        for (BlendFactor blendFactor : blendFactors) {
            blendFactor.ndf *= normalizer;
           // System.err.println(blendFactor);
        }
        
        
        //for now just pick the first probe.
        if(!blendFactors.isEmpty()){
            probe = blendFactors.get(0).lightProbe;            
        }else{
            probe = null;
        }
    }

    private float computeBlendFactors(List<BlendFactor> blendFactors) {
        float sumBlendFactors = 0;
        for (Spatial scene : viewPort.getScenes()) {
            for (Light light : scene.getWorldLightList()) {
                if(light.getType() == Light.Type.Probe){
                    LightProbe p = (LightProbe)light;
                    TempVars vars = TempVars.get();
                    boolean intersect = p.intersectsFrustum(viewPort.getCamera(), vars);
                    vars.release();
                    //check if the probe is inside the camera frustum
                    if(intersect){

                        //is the poi inside the bounds of this probe
                        if(poi.getWorldBound().intersects(p.getBounds())){
                            
                            //computing the distance as we need it to check if th epoi in in the inner radius and later to compute the weight
                            float outerRadius = ((BoundingSphere)p.getBounds()).getRadius();
                            float innerRadius = outerRadius * 0.5f;
                            float distance = p.getBounds().getCenter().distance(poi.getWorldTranslation());
                            
                            // if the poi in inside the inner range of this probe, then this probe is the only one that matters.
                            if( distance < innerRadius ){
                                blendFactors.clear();
                                blendFactors.add(new BlendFactor(p, 1.0f));
                                return 1.0f;
                            }
                            //else we need to compute the weight of this probe and collect it for blending
                            float ndf = (distance - innerRadius) / (outerRadius - innerRadius);
                            sumBlendFactors += ndf;
                            blendFactors.add(new BlendFactor(p, ndf));
                        }
                    }
                }
            }
        }
        return sumBlendFactors;
    }

    @Override
    public void postFrame(FrameBuffer out) {
        
    }

    @Override
    public void cleanup() {
        viewPort = null;
        renderManager.setLightFilter(prevFilter);
    }

    public void populateProbe(LightList lightList){
        if(probe != null && probe.isReady()){
            lightList.add(probe);
        }
    }

    public Spatial getPoi() {
        return poi;
    }

    public void setPoi(Spatial poi) {
        this.poi = poi;
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
        this.prof = profiler;
    }

    private class BlendFactor implements Comparable<BlendFactor>{
        
        LightProbe lightProbe;
        float ndf;       

        public BlendFactor(LightProbe lightProbe, float ndf) {
            this.lightProbe = lightProbe;
            this.ndf = ndf;
        }

        @Override
        public String toString() {
            return "BlendFactor{" + "lightProbe=" + lightProbe + ", ndf=" + ndf + '}';
        }
        
        @Override
        public int compareTo(BlendFactor o) {
            if(o.ndf > ndf){
                return -1;
            }else if(o.ndf < ndf){
                return 1;
            }
            return 0;
        }
        
    }
}
