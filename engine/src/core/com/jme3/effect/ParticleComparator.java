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

package com.jme3.effect;

import com.jme3.renderer.Camera;
import java.util.Comparator;

@Deprecated
class ParticleComparator implements Comparator<Particle> {

    private Camera cam;

    public void setCamera(Camera cam){
        this.cam = cam;
    }

    public int compare(Particle p1, Particle p2) {
        return 0; // unused
        /*
        if (p1.life <= 0 || p2.life <= 0)
            return 0;

//        if (p1.life <= 0)
//            return 1;
//        else if (p2.life <= 0)
//            return -1;

        float d1 = p1.distToCam, d2 = p2.distToCam;

        if (d1 == -1){
            d1 = cam.distanceToNearPlane(p1.position);
            p1.distToCam = d1;
        }
        if (d2 == -1){
            d2 = cam.distanceToNearPlane(p2.position);
            p2.distToCam = d2;
        }

        if (d1 < d2)
            return 1;
        else if (d1 > d2)
            return -1;
        else
            return 0;
        */
    }
}