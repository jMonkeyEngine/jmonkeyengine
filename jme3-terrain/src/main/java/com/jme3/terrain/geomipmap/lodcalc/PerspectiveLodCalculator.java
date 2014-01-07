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
package com.jme3.terrain.geomipmap.lodcalc;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.terrain.geomipmap.TerrainPatch;
import com.jme3.terrain.geomipmap.UpdatedTerrainPatch;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class PerspectiveLodCalculator implements LodCalculator {

    private Camera cam;
    private float pixelError;
    private boolean turnOffLod = false;

    public PerspectiveLodCalculator() {}
    
    public PerspectiveLodCalculator(Camera cam, float pixelError) {
        this.cam = cam;
        this.pixelError = pixelError;
    }

    /**
     * This computes the "C" value in the geomipmapping paper.
     * See section "2.3.1.2 Pre-calculating d"
     * 
     * @param cam
     * @param pixelLimit
     * @return
     */
    private float getCameraConstant(Camera cam, float pixelLimit){
        float n = cam.getFrustumNear();
        float t = FastMath.abs(cam.getFrustumTop());
        float A = n / t;
        float v_res = cam.getHeight();
        float T = (2f * pixelLimit) / v_res;
        return A / T;
    }
    
    public boolean calculateLod(TerrainPatch patch, List<Vector3f> locations, HashMap<String, UpdatedTerrainPatch> updates) {
        if (turnOffLod) {
            // set to full detail
            int prevLOD = patch.getLod();
            UpdatedTerrainPatch utp = updates.get(patch.getName());
            if (utp == null) {
                utp = new UpdatedTerrainPatch(patch);
                updates.put(utp.getName(), utp);
            }
            utp.setNewLod(0);
            utp.setPreviousLod(prevLOD);
            //utp.setReIndexNeeded(true);
            return true;
        }
        
        float[] lodEntropies = patch.getLodEntropies();
        float cameraConstant = getCameraConstant(cam, pixelError);
        
        Vector3f patchPos = getCenterLocation(patch);

        // vector from camera to patch
        //Vector3f toPatchDir = locations.get(0).subtract(patchPos).normalizeLocal();
        //float facing = cam.getDirection().dot(toPatchDir);
        float distance = patchPos.distance(locations.get(0));

        // go through each lod level to find the one we are in
        for (int i = 0; i <= patch.getMaxLod(); i++) {
            if (distance < lodEntropies[i] * cameraConstant || i == patch.getMaxLod()){
                boolean reIndexNeeded = false;
                if (i != patch.getLod()) {
                    reIndexNeeded = true;
//                    System.out.println("lod change: "+lod+" > "+i+"    dist: "+distance);
                }
                int prevLOD = patch.getLod();

                
                UpdatedTerrainPatch utp = updates.get(patch.getName());
                if (utp == null) {
                    utp = new UpdatedTerrainPatch(patch);//save in here, do not update actual variables
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

    public Vector3f getCenterLocation(TerrainPatch patch) {
        Vector3f loc = patch.getWorldTranslation().clone();
        loc.x += patch.getSize() / 2;
        loc.z += patch.getSize() / 2;
        return loc;
    }

    @Override
    public LodCalculator clone() {
        try {
            return (LodCalculator) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    public void write(JmeExporter ex) throws IOException {
        
    }

    public void read(JmeImporter im) throws IOException {
    }

    public boolean usesVariableLod() {
        return true;
    }

    public float getPixelError() {
        return pixelError;
    }

    public void setPixelError(float pixelError) {
        this.pixelError = pixelError;
    }

    public void setCam(Camera cam) {
        this.cam = cam;
    }

    public void turnOffLod() {
        turnOffLod = true;
    }
    
    public boolean isLodOff() {
        return turnOffLod;
    }
    
    public void turnOnLod() {
        turnOffLod = false;
    }
    
}
