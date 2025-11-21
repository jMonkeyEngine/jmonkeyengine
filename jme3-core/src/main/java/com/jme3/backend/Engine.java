package com.jme3.backend;

import com.jme3.vulkan.mesh.InputRate;
import com.jme3.vulkan.mesh.VertexBinding;
import com.jme3.vulkan.util.IntEnum;

public interface Engine {

    VertexBinding.Builder createMeshVertexBinding(IntEnum<InputRate> rate);

}
