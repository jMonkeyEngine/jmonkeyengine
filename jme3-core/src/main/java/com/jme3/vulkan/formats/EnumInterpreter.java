package com.jme3.vulkan.formats;

import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.mesh.IndexType;
import com.jme3.vulkan.pipeline.Topology;

public interface EnumInterpreter {

    int getFormatEnum(Format fmt);

    int getTopologyEnum(Topology topology);

    int getIndexTypeEnum(IndexType type);

    int getLoadEnum(VulkanImage.Load load);

    int getStoreEnum(VulkanImage.Store store);

}
