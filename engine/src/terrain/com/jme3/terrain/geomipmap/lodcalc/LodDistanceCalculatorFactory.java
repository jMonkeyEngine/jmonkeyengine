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
import com.jme3.terrain.geomipmap.TerrainPatch;
import java.io.IOException;

/**
 *
 * @author bowens
 * @deprecated phasing out
 */
public class LodDistanceCalculatorFactory implements LodCalculatorFactory {

    private float lodThresholdSize = 2.7f;
    private LodThreshold lodThreshold = null;


    public LodDistanceCalculatorFactory() {
    }
    
    public LodDistanceCalculatorFactory(LodThreshold lodThreshold) {
        this.lodThreshold = lodThreshold;
    }

    public LodCalculator createCalculator() {
        return new DistanceLodCalculator();
    }

    public LodCalculator createCalculator(TerrainPatch terrainPatch) {
        return new DistanceLodCalculator();
    }

    public void write(JmeExporter ex) throws IOException {
		OutputCapsule c = ex.getCapsule(this);
		c.write(lodThreshold, "lodThreshold", null);
        c.write(lodThresholdSize, "lodThresholdSize", 2);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule c = im.getCapsule(this);
		lodThresholdSize = c.readFloat("lodThresholdSize", 2);
        lodThreshold = (LodThreshold) c.readSavable("lodThreshold", null);
    }

    @Override
    public LodDistanceCalculatorFactory clone() {
        LodDistanceCalculatorFactory clone = new LodDistanceCalculatorFactory();
        clone.lodThreshold = lodThreshold.clone();
        clone.lodThresholdSize = lodThresholdSize;
        return clone;
    }

}
