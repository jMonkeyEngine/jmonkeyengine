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

import com.jme3.scene.Geometry;
import java.util.ArrayList;
import java.util.List;

/**
 * This strategy returns the closest probe from the rendered object.
 * 
 * This is the most basic strategy : The fastest and the easiest.
 * Though it has severe graphical draw backs as there might be very visible seams
 * on static object and some "poping" on dynamic objects.
 *
 * @author Nehon
 */
public class BasicProbeBlendingStrategy implements LightProbeBlendingStrategy {

    List<LightProbe> lightProbes = new ArrayList<LightProbe>();

    @Override
    public void registerProbe(LightProbe probe) {
        lightProbes.add(probe);
    }

    @Override
    public void populateProbes(Geometry g, LightList lightList) {
        if (!lightProbes.isEmpty()) {
            //The first probe is actually the closest to the geometry since the 
            //light list is sorted according to the distance to the geom.
            LightProbe p = lightProbes.get(0);
            if (p.isReady()) {
                lightList.add(p);
            }            
            //clearing the list for next pass.
            lightProbes.clear();
        }        
    }

}
