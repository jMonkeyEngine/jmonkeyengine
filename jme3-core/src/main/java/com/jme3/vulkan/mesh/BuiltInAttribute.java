package com.jme3.vulkan.mesh;

/**
 * Names of common attributes such as position, texture coordinates, and normals.
 */
public enum BuiltInAttribute {

    Position("jme_position"),
    TexCoord("jme_texcoord"),
    Normal("jme_normal"),
    Tangent("jme_tangent"),
    Color("jme_color");

    private final String name;

    BuiltInAttribute(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
