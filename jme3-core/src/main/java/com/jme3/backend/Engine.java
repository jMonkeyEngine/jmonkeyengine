
package com.jme3.backend;

import com.jme3.material.Material;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.Texture;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.buffers.saving.BufferAllocator;
import com.jme3.vulkan.material.uniforms.Uniform;

import java.util.Collection;

public interface Engine {

    void render(Collection<ViewPort> viewPorts);

    Material createMaterial();

    Material createMaterial(String matdefName);

}
