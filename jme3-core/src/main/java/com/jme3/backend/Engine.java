
package com.jme3.backend;

import com.jme3.material.Material;
import com.jme3.renderer.ViewPort;

import java.util.Collection;

public interface Engine {

    void render(Collection<ViewPort> viewPorts);

    Material createMaterial();

    Material createMaterial(String matdefName);

}
