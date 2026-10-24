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
