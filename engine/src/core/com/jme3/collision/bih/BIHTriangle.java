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

package com.jme3.collision.bih;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public final class BIHTriangle {

    private final Vector3f pointa = new Vector3f();
    private final Vector3f pointb = new Vector3f();
    private final Vector3f pointc = new Vector3f();
    private final Vector3f center = new Vector3f();

    public BIHTriangle(Vector3f p1, Vector3f p2, Vector3f p3) {
        pointa.set(p1);
        pointb.set(p2);
        pointc.set(p3);
        center.set(pointa);
        center.addLocal(pointb).addLocal(pointc).multLocal(FastMath.ONE_THIRD);
    }

    public Vector3f get1(){
        return pointa;
    }

    public Vector3f get2(){
        return pointb;
    }

    public Vector3f get3(){
        return pointc;
    }

    public Vector3f getCenter() {
        return center;
    }

    public Vector3f getNormal(){
        Vector3f normal = new Vector3f(pointb);
        normal.subtractLocal(pointa).crossLocal(pointc.x-pointa.x, pointc.y-pointa.y, pointc.z-pointa.z);
        normal.normalizeLocal();
        return normal;
    }

    public float getExtreme(int axis, boolean left){
        float v1, v2, v3;
        switch (axis){
            case 0: v1 = pointa.x; v2 = pointb.x; v3 = pointc.x; break;
            case 1: v1 = pointa.y; v2 = pointb.y; v3 = pointc.y; break;
            case 2: v1 = pointa.z; v2 = pointb.z; v3 = pointc.z; break;
            default: assert false; return 0;
        }
        if (left){
            if (v1 < v2){
                if (v1 < v3)
                    return v1;
                else
                    return v3;
            }else{
                if (v2 < v3)
                    return v2;
                else
                    return v3;
            }
        }else{
            if (v1 > v2){
                if (v1 > v3)
                    return v1;
                else
                    return v3;
            }else{
                if (v2 > v3)
                    return v2;
                else
                    return v3;
            }
        }
    }

}
