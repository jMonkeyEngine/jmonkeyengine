package com.jme3.vulkan.pipelines.states;

import com.jme3.vulkan.pipelines.Topology;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;

import java.util.Objects;

public class InputAssemblyState implements PipelineState<VkPipelineInputAssemblyStateCreateInfo> {

    private IntEnum<Topology> topology = Topology.TriangleList;
    private boolean primitiveRestart = false;
    protected long version = 0L;

    @Override
    public VkPipelineInputAssemblyStateCreateInfo create(MemoryStack stack) {
        return VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
    }

    @Override
    public VkPipelineInputAssemblyStateCreateInfo fill(MemoryStack stack, VkPipelineInputAssemblyStateCreateInfo struct) {
        return struct.topology(topology.getEnum())
                .primitiveRestartEnable(primitiveRestart);
    }

    @Override
    public long getCurrentVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        InputAssemblyState that = (InputAssemblyState) o;
        return primitiveRestart == that.primitiveRestart
                && IntEnum.is(topology, that.topology);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topology, primitiveRestart);
    }

    public void setTopology(IntEnum<Topology> topology) {
        if (!IntEnum.is(this.topology, topology)) {
            this.topology = topology;
            version++;
        }
    }

    public void setPrimitiveRestart(boolean primitiveRestart) {
        if (this.primitiveRestart != primitiveRestart) {
            this.primitiveRestart = primitiveRestart;
            version++;
        }
    }

}
