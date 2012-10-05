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
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;

/**
 * <a href="http://www.geeks3d.com/20110405/fxaa-fast-approximate-anti-aliasing-demo-glsl-opengl-test-radeon-geforce/3/" rel="nofollow">http://www.geeks3d.com/20110405/fxaa-fast-approximate-anti-aliasing-demo-glsl-<span class="domtooltips" title="OpenGL (Open Graphics Library) is a standard specification defining a cross-language, cross-platform API for writing applications that produce 2D and 3D computer graphics." id="domtooltipsspan11">opengl</span>-test-radeon-geforce/3/</a>
 * <a href="http://developer.download.nvidia.com/assets/gamedev/files/sdk/11/FXAA_WhitePaper.pdf" rel="nofollow">http://developer.download.nvidia.com/assets/gamedev/files/sdk/11/FXAA_WhitePaper.pdf</a>
 *
 * @author Phate666 (adapted to jme3)
 *
 */
public class FXAAFilter extends Filter {

    private float subPixelShift = 1.0f / 4.0f;
    private float vxOffset = 0.0f;
    private float spanMax = 8.0f;
    private float reduceMul = 1.0f / 8.0f;

    public FXAAFilter() {
        super("FXAAFilter");
    }

    @Override
    protected void initFilter(AssetManager manager,
            RenderManager renderManager, ViewPort vp, int w, int h) {
        material = new Material(manager, "Common/MatDefs/Post/FXAA.j3md");   
        material.setFloat("SubPixelShift", subPixelShift);
        material.setFloat("VxOffset", vxOffset);
        material.setFloat("SpanMax", spanMax);
        material.setFloat("ReduceMul", reduceMul);
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    public void setSpanMax(float spanMax) {
        this.spanMax = spanMax;
        if (material != null) {
            material.setFloat("SpanMax", this.spanMax);
        }
    }

    /**
     * set to 0.0f for higher quality
     *
     * @param subPixelShift
     */
    public void setSubPixelShift(float subPixelShift) {
        this.subPixelShift = subPixelShift;
        if (material != null) {
            material.setFloat("SubPixelShif", this.subPixelShift);
        }
    }

    /**
     * set to 0.0f for higher quality
     *
     * @param reduceMul
     */
    public void setReduceMul(float reduceMul) {
        this.reduceMul = reduceMul;
        if (material != null) {
            material.setFloat("ReduceMul", this.reduceMul);
        }
    }

    public void setVxOffset(float vxOffset) {
        this.vxOffset = vxOffset;
        if (material != null) {
            material.setFloat("VxOffset", this.vxOffset);
        }
    }

    public float getReduceMul() {
        return reduceMul;
    }

    public float getSpanMax() {
        return spanMax;
    }

    public float getSubPixelShift() {
        return subPixelShift;
    }

    public float getVxOffset() {
        return vxOffset;
    }
}