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

package com.jme3.terrain.geomipmap.lodcalc;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.io.IOException;


/**
 * Just multiplies the terrain patch size by 2. So every two
 * patches away the camera is, the LOD changes.
 * 
 * Set it higher to have the LOD change less frequently.
 * 
 * @author bowens
 */
public class SimpleLodThreshold implements LodThreshold {
	
    private int size; // size of a terrain patch
    private float lodMultiplier = 2;

    
    public SimpleLodThreshold() {
    }
    
    public SimpleLodThreshold(Terrain terrain) {
        if (terrain instanceof TerrainQuad)
            this.size = ((TerrainQuad)terrain).getPatchSize();
    }

    public SimpleLodThreshold(int patchSize, float lodMultiplier) {
        this.size = patchSize;
    }

    public float getLodMultiplier() {
        return lodMultiplier;
    }

    public void setLodMultiplier(float lodMultiplier) {
        this.lodMultiplier = lodMultiplier;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
	

    public float getLodDistanceThreshold() {
        return size*lodMultiplier;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(size, "size", 16);
        oc.write(lodMultiplier, "lodMultiplier", 2);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        size = ic.readInt("size", 16);
        lodMultiplier = ic.readInt("lodMultiplier", 2);
    }

    @Override
    public LodThreshold clone() {
        SimpleLodThreshold clone = new SimpleLodThreshold();
        clone.size = size;
        clone.lodMultiplier = lodMultiplier;
        
        return clone;
    }

    @Override
    public String toString() {
        return "SimpleLodThreshold "+size+", "+lodMultiplier;
    }
}
