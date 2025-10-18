package com.jme3.vulkan.pipelines.states;

import com.jme3.vulkan.pipelines.CompareOp;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;

import java.util.Objects;

public class DepthStencilState implements PipelineState<VkPipelineDepthStencilStateCreateInfo> {

    private boolean depthTest = true;
    private boolean depthWrite = true;
    private boolean depthBoundsTest = false;
    private boolean stencilTest = false;
    private IntEnum<CompareOp> depthCompare = CompareOp.LessOrEqual;
    protected long version = 0L;

    @Override
    public VkPipelineDepthStencilStateCreateInfo create(MemoryStack stack) {
        return VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
    }

    @Override
    public VkPipelineDepthStencilStateCreateInfo fill(MemoryStack stack, VkPipelineDepthStencilStateCreateInfo struct) {
        return struct.depthTestEnable(depthTest)
                .depthWriteEnable(depthWrite)
                .depthBoundsTestEnable(depthBoundsTest)
                .stencilTestEnable(stencilTest)
                .depthCompareOp(depthCompare.getEnum());
    }

    @Override
    public long getCurrentVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DepthStencilState that = (DepthStencilState) o;
        return depthTest == that.depthTest
                && depthWrite == that.depthWrite
                && depthBoundsTest == that.depthBoundsTest
                && stencilTest == that.stencilTest
                && IntEnum.is(depthCompare, that.depthCompare);
    }

    @Override
    public int hashCode() {
        return Objects.hash(depthTest, depthWrite, depthBoundsTest, stencilTest, depthCompare);
    }

    public void setDepthTest(boolean depthTest) {
        if (this.depthTest != depthTest) {
            this.depthTest = depthTest;
            version++;
        }
    }

    public void setDepthWrite(boolean depthWrite) {
        if (this.depthWrite != depthWrite) {
            this.depthWrite = depthWrite;
            version++;
        }
    }

    public void setDepthBoundsTest(boolean depthBoundsTest) {
        if (this.depthBoundsTest != depthBoundsTest) {
            this.depthBoundsTest = depthBoundsTest;
            version++;
        }
    }

    public void setStencilTest(boolean stencilTest) {
        if (this.stencilTest != stencilTest) {
            this.stencilTest = stencilTest;
            version++;
        }
    }

    public void setDepthCompare(IntEnum<CompareOp> depthCompare) {
        if (!IntEnum.is(this.depthCompare, depthCompare)) {
            this.depthCompare = depthCompare;
            version++;
        }
    }

}
