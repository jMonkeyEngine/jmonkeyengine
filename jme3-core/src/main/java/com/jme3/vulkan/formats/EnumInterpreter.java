package com.jme3.vulkan.formats;

import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.mesh.IndexType;
import com.jme3.vulkan.pipeline.Topology;
import com.jme3.vulkan.shaderc.ShaderType;

public interface EnumInterpreter {

    default int getShaderTypeEnum(ShaderType type) {
        throw new UnsupportedOperationException(type.name());
    }

    default int getFormatEnum(Format fmt) {
        throw new UnsupportedOperationException(fmt.name());
    }

    default int getTopologyEnum(Topology topology) {
        throw new UnsupportedOperationException(topology.name());
    }

    default int getIndexTypeEnum(IndexType type) {
        throw new UnsupportedOperationException(type.name());
    }

    default int getImageLoadEnum(VulkanImage.Load load) {
        throw new UnsupportedOperationException(load.name());
    }

    default int getImageStoreEnum(VulkanImage.Store store) {
        throw new UnsupportedOperationException(store.name());
    }

}
