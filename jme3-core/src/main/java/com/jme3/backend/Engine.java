
package com.jme3.backend;

import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.descriptors.PoolSize;
import com.jme3.vulkan.material.experimental.MaterialData;
import com.jme3.vulkan.util.Flag;

public interface Engine extends MemoryAllocator {

    DescriptorPool createDescriptorPool(int sets, Flag<DescriptorPool.Create> flags, PoolSize... sizes);

    default DescriptorPool createDescriptorPool(int sets, PoolSize... sizes) {
        return createDescriptorPool(sets, Flag.empty(), sizes);
    }

    DescriptorSetLayout createDescriptorSetLayout(DescriptorSetLayout.Info info);

    MaterialData getMaterialData();

}
