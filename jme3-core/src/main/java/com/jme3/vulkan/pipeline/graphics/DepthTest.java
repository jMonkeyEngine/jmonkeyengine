package com.jme3.vulkan.pipeline.graphics;

import com.jme3.vulkan.pipeline.CompareOp;

import java.util.Objects;

/**
 * Groups the render-state parameters that determine how a draw interacts with the depth
 * buffer: whether depth testing/writing is enabled, which comparison rejects a fragment, and
 * whether a depth bias is applied (not the bias value itself - that's dynamic state, see
 * {@link com.jme3.vulkan.material.experimental.PipelineKey}). Mirrors {@link ColorBlendAttachment}
 * and {@link StencilTest} in bundling several related pipeline-identity fields into one value.
 */
public class DepthTest {

    private boolean testEnabled = true;
    private boolean writeEnabled = true;
    private CompareOp compareOp = CompareOp.LessOrEqual;
    private boolean biasEnabled = false;

    public DepthTest() {}

    public DepthTest(boolean testEnabled, boolean writeEnabled, CompareOp compareOp, boolean biasEnabled) {
        this.testEnabled = testEnabled;
        this.writeEnabled = writeEnabled;
        this.compareOp = compareOp;
        this.biasEnabled = biasEnabled;
    }

    public void setTestEnabled(boolean testEnabled) {
        this.testEnabled = testEnabled;
    }

    public void setWriteEnabled(boolean writeEnabled) {
        this.writeEnabled = writeEnabled;
    }

    public void setCompareOp(CompareOp compareOp) {
        this.compareOp = compareOp;
    }

    public void setBiasEnabled(boolean biasEnabled) {
        this.biasEnabled = biasEnabled;
    }

    public boolean isTestEnabled() {
        return testEnabled;
    }

    public boolean isWriteEnabled() {
        return writeEnabled;
    }

    public CompareOp getCompareOp() {
        return compareOp;
    }

    public boolean isBiasEnabled() {
        return biasEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DepthTest that = (DepthTest)o;
        return testEnabled == that.testEnabled
                && writeEnabled == that.writeEnabled
                && biasEnabled == that.biasEnabled
                && compareOp == that.compareOp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(testEnabled, writeEnabled, compareOp, biasEnabled);
    }

}
