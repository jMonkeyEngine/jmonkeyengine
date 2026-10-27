package com.jme3.vulkan.pipeline.graphics;

import com.jme3.vulkan.StencilOp;
import com.jme3.vulkan.pipeline.CompareOp;

import java.util.Objects;

public class StencilTest {

    // todo: these defaults are complete balderdash
    private boolean stencilTestEnabled = false;
    private CompareOp frontStencilCompareOp = CompareOp.Always;
    private StencilOp frontStencilFailOp = StencilOp.DecrementClamped;
    private StencilOp frontStencilDepthFailOp = StencilOp.DecrementClamped;
    private StencilOp frontStencilPassOp = StencilOp.DecrementClamped;
    private CompareOp backStencilCompareOp = CompareOp.Always;
    private StencilOp backStencilFailOp = StencilOp.DecrementClamped;
    private StencilOp backStencilDepthFailOp = StencilOp.DecrementClamped;
    private StencilOp backStencilPassOp = StencilOp.DecrementClamped;

    public boolean isStencilTestEnabled() {
        return stencilTestEnabled;
    }

    public void setStencilTestEnabled(boolean stencilTestEnabled) {
        this.stencilTestEnabled = stencilTestEnabled;
    }

    public CompareOp getFrontStencilCompareOp() {
        return frontStencilCompareOp;
    }

    public void setFrontStencilCompareOp(CompareOp frontStencilCompareOp) {
        this.frontStencilCompareOp = frontStencilCompareOp;
    }

    public StencilOp getFrontStencilFailOp() {
        return frontStencilFailOp;
    }

    public void setFrontStencilFailOp(StencilOp frontStencilFailOp) {
        this.frontStencilFailOp = frontStencilFailOp;
    }

    public StencilOp getFrontStencilDepthFailOp() {
        return frontStencilDepthFailOp;
    }

    public void setFrontStencilDepthFailOp(StencilOp frontStencilDepthFailOp) {
        this.frontStencilDepthFailOp = frontStencilDepthFailOp;
    }

    public StencilOp getFrontStencilPassOp() {
        return frontStencilPassOp;
    }

    public void setFrontStencilPassOp(StencilOp frontStencilPassOp) {
        this.frontStencilPassOp = frontStencilPassOp;
    }

    public CompareOp getBackStencilCompareOp() {
        return backStencilCompareOp;
    }

    public void setBackStencilCompareOp(CompareOp backStencilCompareOp) {
        this.backStencilCompareOp = backStencilCompareOp;
    }

    public StencilOp getBackStencilFailOp() {
        return backStencilFailOp;
    }

    public void setBackStencilFailOp(StencilOp backStencilFailOp) {
        this.backStencilFailOp = backStencilFailOp;
    }

    public StencilOp getBackStencilDepthFailOp() {
        return backStencilDepthFailOp;
    }

    public void setBackStencilDepthFailOp(StencilOp backStencilDepthFailOp) {
        this.backStencilDepthFailOp = backStencilDepthFailOp;
    }

    public StencilOp getBackStencilPassOp() {
        return backStencilPassOp;
    }

    public void setBackStencilPassOp(StencilOp backStencilPassOp) {
        this.backStencilPassOp = backStencilPassOp;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        StencilTest that = (StencilTest) o;
        return stencilTestEnabled == that.stencilTestEnabled
                && frontStencilCompareOp == that.frontStencilCompareOp
                && frontStencilFailOp == that.frontStencilFailOp
                && frontStencilDepthFailOp == that.frontStencilDepthFailOp
                && frontStencilPassOp == that.frontStencilPassOp
                && backStencilCompareOp == that.backStencilCompareOp
                && backStencilFailOp == that.backStencilFailOp
                && backStencilDepthFailOp == that.backStencilDepthFailOp
                && backStencilPassOp == that.backStencilPassOp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stencilTestEnabled, frontStencilCompareOp, frontStencilFailOp, frontStencilDepthFailOp,
                frontStencilPassOp, backStencilCompareOp, backStencilFailOp, backStencilDepthFailOp, backStencilPassOp);
    }

}
