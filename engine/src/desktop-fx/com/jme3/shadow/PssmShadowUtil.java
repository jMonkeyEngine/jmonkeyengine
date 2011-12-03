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
package com.jme3.shadow;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryList;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Includes various useful shadow mapping functions.
 *
 * @see
 * <ul>
 * <li><a href="http://appsrv.cse.cuhk.edu.hk/~fzhang/pssm_vrcia/">http://appsrv.cse.cuhk.edu.hk/~fzhang/pssm_vrcia/</a></li>
 * <li><a href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a></li>
 * </ul>
 * for more info.
 */
public final class PssmShadowUtil {

    /**
     * Updates the frustum splits stores in <code>splits</code> using PSSM.
     */
    public static void updateFrustumSplits(float[] splits, float near, float far, float lambda) {
        for (int i = 0; i < splits.length; i++) {
            float IDM = i / (float) splits.length;
            float log = near * FastMath.pow((far / near), IDM);
            float uniform = near + (far - near) * IDM;
            splits[i] = log * lambda + uniform * (1.0f - lambda);
        }

        // This is used to improve the correctness of the calculations. Our main near- and farplane
        // of the camera always stay the same, no matter what happens.
        splits[0] = near;
        splits[splits.length - 1] = far;
    }

    /**
     * Compute the Zfar in the model vieuw to adjust the Zfar distance for the splits calculation
     */
    public static float computeZFar(GeometryList occ, GeometryList recv, Camera cam) {
        Matrix4f mat = cam.getViewMatrix();
        BoundingBox bbOcc = ShadowUtil.computeUnionBound(occ, mat);
        BoundingBox bbRecv = ShadowUtil.computeUnionBound(recv, mat);

        return min(max(bbOcc.getZExtent() - bbOcc.getCenter().z, bbRecv.getZExtent() - bbRecv.getCenter().z), cam.getFrustumFar());
    }
}
