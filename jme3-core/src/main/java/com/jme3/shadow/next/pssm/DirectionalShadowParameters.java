/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.shadow.next.pssm;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.shadow.PssmShadowUtil;

/**
 * @author Kirill Vainer
 */
public final class DirectionalShadowParameters {

    private float lambda = 0.65f;
    private int numSplits = 4;
    protected float zFarOverride = 0;
    private float[] splitPositions = new float[numSplits + 1];
    private final Vector3f projectionSplitPositions = new Vector3f();

    public float getLambda() {
        return lambda;
    }

    public void setLambda(float lambda) {
        this.lambda = lambda;
    }

    public int getNumSplits() {
        return numSplits;
    }

    public void setNumSplits(int numSplits) {
        if (numSplits < 1 || numSplits > 4) {
            throw new IllegalArgumentException("Number of splits must be between 1 and 4");
        }
        this.numSplits = numSplits;
        this.splitPositions = new float[numSplits + 1];
    }

    public float[] getSplitPositions() {
        return splitPositions;
    }

    public Vector3f getProjectionSplitPositions() {
        return projectionSplitPositions;
    }

    /**
     * How far the shadows are rendered in the view
     *
     * @see #setShadowZExtend(float zFar)
     * @return shadowZExtend
     */
    public float getShadowZExtend() {
        return zFarOverride;
    }
    
    /**
     * Set the distance from the eye where the shadows will be rendered.
     * 
     * The default value is dynamically computed based on the shadow
     * casters/receivers union bound zFar, capped to view frustum far value.
     *
     * @param zFar the zFar values that override the computed one
     */
    public void setShadowZExtend(float zFar) {
        this.zFarOverride = zFar;
        
        // TODO: Fade length not supported yet
//        if (zFarOverride == 0) {
//            fadeInfo = null;
//            frustumCam = null;
//        } else {
//            if (fadeInfo != null) {
//                fadeInfo.set(zFarOverride - fadeLength, 1f / fadeLength);
//            }
//            if (frustumCam == null && viewPort != null) {
//                initFrustumCam();
//            }
//        }
    }
    
    public void updateSplitPositions(Camera viewCamera) {
        float near = viewCamera.getFrustumNear();
        float far = zFarOverride == 0f ? viewCamera.getFrustumFar() : zFarOverride;

        PssmShadowUtil.updateFrustumSplits(splitPositions, near, far, lambda);

        // TODO: Parallel projection can have negative near value, so split
        //       positions must be adjusted.
//        if (viewCamera.isParallelProjection()) {
//            for (int i = 0; i < splitPositions.length; i++) {
//                splitPositions[i] = splitPositions[i] / (far - near);
//            }
//        }

        switch (splitPositions.length) {
            case 5:
//                projectionSplitPositions.w = 1.0f;
            case 4:
                projectionSplitPositions.z = viewCamera.getViewToProjectionZ(splitPositions[3]);
            case 3:
                projectionSplitPositions.y = viewCamera.getViewToProjectionZ(splitPositions[2]);
            case 2:
            case 1:
                projectionSplitPositions.x = viewCamera.getViewToProjectionZ(splitPositions[1]);
                break;
        }
    }
}
