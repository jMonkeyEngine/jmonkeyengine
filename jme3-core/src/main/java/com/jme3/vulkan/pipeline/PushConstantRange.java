package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.vulkan.VkPushConstantRange;

import java.util.Objects;

/**
 * Range of a pipeline layout's push constants, and the shader stages that can read it.
 * Immutable, and compared by value, so it can be part of pipeline and layout identity.
 */
public final class PushConstantRange {

    private final Flag<ShaderStage> stages;
    private final int offset, size;

    /**
     * @param stages stages that read the range, at least one
     * @param offset byte offset of the range, a multiple of 4
     * @param size byte size of the range, a positive multiple of 4
     * @throws IllegalArgumentException if any argument breaks the requirements above
     */
    public PushConstantRange(Flag<ShaderStage> stages, int offset, int size) {
        if (stages.bits() == 0) {
            throw new IllegalArgumentException("Push constant range must be visible to at least one stage.");
        }
        if (offset < 0 || (offset & 3) != 0) {
            throw new IllegalArgumentException("Push constant offset must be a non-negative multiple of 4: " + offset);
        }
        if (size <= 0 || (size & 3) != 0) {
            throw new IllegalArgumentException("Push constant size must be a positive multiple of 4: " + size);
        }
        this.stages = stages;
        this.offset = offset;
        this.size = size;
    }

    public void fill(VkPushConstantRange range) {
        range.set(stages.bits(), offset, size);
    }

    public Flag<ShaderStage> getStages() {
        return stages;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    /**
     * @return byte offset after the end of the range
     */
    public int getEnd() {
        return offset + size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PushConstantRange that = (PushConstantRange)o;
        return offset == that.offset && size == that.size && stages.bits() == that.stages.bits();
    }

    @Override
    public int hashCode() {
        return Objects.hash(stages.bits(), offset, size);
    }

    @Override
    public String toString() {
        return "PushConstantRange{stages=" + Integer.toBinaryString(stages.bits()) + ", offset=" + offset + ", size=" + size + '}';
    }

}
