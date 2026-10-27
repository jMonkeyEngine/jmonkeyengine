package com.jme3.vulkan.shadergen.lib;

import com.jme3.vulkan.shadergen.Return;
import com.jme3.vulkan.shadergen.Shader;
import com.jme3.vulkan.shadergen.vars.*;

/**
 * Shared PBR lighting math: tangent-space normal mapping and a metallic/roughness BRDF
 * evaluation, factored out of any one technique's shader the same way {@link Skinning}
 * factors out vertex skinning/morphing.
 */
public class Lighting extends Shader {

    /**
     * Perturbs {@code normal} using {@code normalMap} sampled at {@code texCoord}, via the
     * tangent-space basis {@code tangent} (with handedness in its {@code w} component)
     * forms with {@code normal}.
     *
     * @param normalMap tangent-space normal map
     * @param texCoord texture coordinate to sample {@code normalMap} at
     * @param normal interpolated vertex normal, in the same space as {@code tangent}
     * @param tangent interpolated vertex tangent (xyz) and handedness (w)
     * @return the perturbed normal
     */
    public Result<Vec3> getNormal(Sampler2D normalMap, Vec2 texCoord, Vec3 normal, Vec4 tangent) {
        return call("_getNormal", normalMap, texCoord, normal, tangent);
    }
    private @Return(Vec3.class) void _getNormal(Sampler2D normalMap, Vec2 texCoord, Vec3 normal, Vec4 tangent) {
        throw new UnsupportedOperationException();
    }

    /**
     * Evaluates a metallic/roughness BRDF (e.g. Cook-Torrance) for every light affecting
     * this fragment, plus ambient, and returns the resulting outgoing radiance.
     *
     * @param albedo base color
     * @param metallic metalness, from 0 (dielectric) to 1 (metal)
     * @param roughness perceptual roughness, from 0 (smooth) to 1 (rough)
     * @param normal shading normal (see {@link #getNormal})
     * @param worldPosition fragment position in world space
     * @param viewDir normalized direction from the fragment to the camera
     * @return outgoing radiance
     */
    public Result<Vec3> computeLighting(Vec3 albedo, Flt metallic, Flt roughness, Vec3 normal, Vec3 worldPosition, Vec3 viewDir) {
        return call("_computeLighting", albedo, metallic, roughness, normal, worldPosition, viewDir);
    }
    private @Return(Vec3.class) void _computeLighting(Vec3 albedo, Flt metallic, Flt roughness, Vec3 normal, Vec3 worldPosition, Vec3 viewDir) {
        throw new UnsupportedOperationException();
    }

}
