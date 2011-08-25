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