/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainPatch;
import com.jme3.terrain.geomipmap.UpdatedTerrainPatch;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Calculates the LOD of the terrain based on its distance from the
 * cameras. Taking the minimum distance from all cameras.
 *
 * @author bowens
 */
public class DistanceLodCalculator implements LodCalculator {

    private int size; // size of a terrain patch
    private float lodMultiplier = 2;
    private boolean turnOffLod = false;
    
    public DistanceLodCalculator() {
    }
    
    public DistanceLodCalculator(int patchSize, float multiplier) {
        this.size = patchSize;
        this.lodMultiplier = multiplier;
    }
    
    @Override
    public boolean calculateLod(TerrainPatch terrainPatch, List<Vector3f> locations, HashMap<String, UpdatedTerrainPatch> updates) {
        if (locations == null || locations.isEmpty())
            return false;// no camera yet
        float distance = getCenterLocation(terrainPatch).distance(locations.get(0));

        if (turnOffLod) {
            // set to full detail
            int prevLOD = terrainPatch.getLod();
            UpdatedTerrainPatch utp = updates.get(terrainPatch.getName());
            if (utp == null) {
                utp = new UpdatedTerrainPatch(terrainPatch);
                updates.put(utp.getName(), utp);
            }
            utp.setNewLod(0);
            utp.setPreviousLod(prevLOD);
            //utp.setReIndexNeeded(true);
            return true;
        }
        
        // go through each lod level to find the one we are in
        for (int i = 0; i <= terrainPatch.getMaxLod(); i++) {
            if (distance < getLodDistanceThreshold() * (i + 1)*terrainPatch.getWorldScaleCached().x || i == terrainPatch.getMaxLod()) {
                boolean reIndexNeeded = false;
                if (i != terrainPatch.getLod()) {
                    reIndexNeeded = true;
                    //System.out.println("lod change: "+lod+" > "+i+"    dist: "+distance);
                }
                int prevLOD = terrainPatch.getLod();
                
                UpdatedTerrainPatch utp = updates.get(terrainPatch.getName());
                if (utp == null) {
                    utp = new UpdatedTerrainPatch(terrainPatch);//save in here, do not update actual variables
                    updates.put(utp.getName(), utp);
                }
                utp.setNewLod(i);
                utp.setPreviousLod(prevLOD);
                //utp.setReIndexNeeded(reIndexNeeded);

                return reIndexNeeded;
            }
        }

        return false;
    }

    protected Vector3f getCenterLocation(TerrainPatch terrainPatch) {
        Vector3f loc = terrainPatch.getWorldTranslationCached();
        loc.x += terrainPatch.getSize()*terrainPatch.getWorldScaleCached().x / 2;
        loc.z += terrainPatch.getSize()*terrainPatch.getWorldScaleCached().z / 2;
        return loc;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(size, "patchSize", 32);
        oc.write(lodMultiplier, "lodMultiplier", 32);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        size = ic.readInt("patchSize", 32);
        lodMultiplier = ic.readFloat("lodMultiplier", 2.7f);
    }

    @Override
    public LodCalculator clone() {
        DistanceLodCalculator clone = new DistanceLodCalculator(size, lodMultiplier);
        return clone;
    }

    @Override
    public String toString() {
        return "DistanceLodCalculator "+size+"*"+lodMultiplier;
    }

    /**
     * Gets the camera distance where the LOD level will change
     */
    protected float getLodDistanceThreshold() {
        return size*lodMultiplier;
    }
    
    /**
     * Does this calculator require the terrain to have the difference of 
     * LOD levels of neighbours to be more than 1.
     */
    @Override
    public boolean usesVariableLod() {
        return false;
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

    @Override
    public void turnOffLod() {
        turnOffLod = true;
    }
    
    @Override
    public boolean isLodOff() {
        return turnOffLod;
    }
    
    @Override
    public void turnOnLod() {
        turnOffLod = false;
    }
    
}
