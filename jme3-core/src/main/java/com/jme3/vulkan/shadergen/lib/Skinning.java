package com.jme3.vulkan.shadergen.lib;

import com.jme3.vulkan.shadergen.In;
import com.jme3.vulkan.shadergen.Out;
import com.jme3.vulkan.shadergen.Shader;
import com.jme3.vulkan.shadergen.vars.Vec4;

public class Skinning extends Shader {

    public void skinningCompute(@In @Out Vec4 modelSpacePos) {
        call("_skinningCompute", modelSpacePos);
    }
    private void _skinningCompute(@In @Out Vec4 modelSpacePos) {
        throw new UnsupportedOperationException();
    }

    public void morphCompute(@In @Out Vec4 modelSpacePos) {
        call("_morphCompute", modelSpacePos);
    }
    private void _morphCompute(@In @Out Vec4 modelSpacePos) {
        throw new UnsupportedOperationException();
    }

}
