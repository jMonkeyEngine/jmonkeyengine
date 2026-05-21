package com.jme3.vulkan.material;

import java.util.Objects;

public class DepthBias {

    public static final DepthBias ZERO = new DepthBias();

    private float constant = 0f;
    private float slope = 0f;
    private float clamp = 0f;

    public DepthBias() {}

    public DepthBias(float constant, float slope, float clamp) {
        this.constant = constant;
        this.slope = slope;
        this.clamp = clamp;
    }

    public DepthBias(DepthBias bias) {
        constant = bias.constant;
        slope = bias.slope;
        clamp = bias.clamp;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DepthBias depthBias = (DepthBias) o;
        return Float.compare(constant, depthBias.constant) == 0
                && Float.compare(slope, depthBias.slope) == 0
                && Float.compare(clamp, depthBias.clamp) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(constant, slope, clamp);
    }

    @Override
    public DepthBias clone() {
        try {
            return (DepthBias)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void set(DepthBias bias) {
        constant = bias.constant;
        slope = bias.slope;
        clamp = bias.clamp;
    }

    public float getConstant() {
        return constant;
    }

    public void setConstant(float constant) {
        this.constant = constant;
    }

    public float getSlope() {
        return slope;
    }

    public void setSlope(float slope) {
        this.slope = slope;
    }

    public float getClamp() {
        return clamp;
    }

    public void setClamp(float clamp) {
        this.clamp = clamp;
    }

}
