package com.jme3.vulkan.shadergen;

import com.jme3.util.IdentityKey;
import com.jme3.vulkan.material.shader.ShaderStage;

import java.util.*;

public class Artifact {

    private final ShaderStage stage;
    private final String source;
    private final Optional<Global> pushConstants;
    private final Map<IdentityKey<Global>, Location> globalLocations = new HashMap<>();

}
