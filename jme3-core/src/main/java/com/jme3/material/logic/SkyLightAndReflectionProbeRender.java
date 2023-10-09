package com.jme3.material.logic;

import com.jme3.light.AmbientLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.LightProbe;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.TextureUnitException;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import com.jme3.texture.TextureCubeMap;

import java.util.List;

/**
 * Rendering logic for handling SkyLight and ReflectionProbe.<br/>
 * @author JohnKkk
 */
public class SkyLightAndReflectionProbeRender {

    public static int setSkyLightAndReflectionProbeData(RenderManager rm, int lastTexUnit, Uniform lightProbeData, Uniform shCoeffs, Uniform lightProbePemMap, LightProbe lightProbe) {

        lightProbeData.setValue(VarType.Matrix4, lightProbe.getUniformMatrix());
        //setVector4InArray(lightProbe.getPosition().x, lightProbe.getPosition().y, lightProbe.getPosition().z, 1f / area.getRadius() + lightProbe.getNbMipMaps(), 0);
        shCoeffs.setValue(VarType.Vector3Array, lightProbe.getShCoeffs());
        /*
         * Assign the prefiltered env map to the next available texture unit.
         */
        int pemUnit = lastTexUnit++;
        Renderer renderer = rm.getRenderer();
        TextureCubeMap pemTexture = lightProbe.getPrefilteredEnvMap();
        try {
            renderer.setTexture(pemUnit, pemTexture);
        } catch (TextureUnitException exception) {
            String message = "Can't assign texture unit for SkyLightAndReflectionProbe."
                    + " lastTexUnit=" + lastTexUnit;
            throw new IllegalArgumentException(message);
        }
        lightProbePemMap.setValue(VarType.Int, pemUnit);
        return lastTexUnit;
    }

    public static void extractSkyLightAndReflectionProbes(LightList lightList, ColorRGBA ambientLightColor, List<LightProbe> skyLightAndReflectionProbes, boolean removeLights) {
        ambientLightColor.set(0, 0, 0, 1);
        skyLightAndReflectionProbes.clear();
        for (int j = 0; j < lightList.size(); j++) {
            Light l = lightList.get(j);
            if (l instanceof AmbientLight) {
                ambientLightColor.addLocal(l.getColor());
                if(removeLights){
                    lightList.remove(l);
                    j--;
                }
            }
            if (l instanceof LightProbe) {
                skyLightAndReflectionProbes.add((LightProbe) l);
                if(removeLights){
                    lightList.remove(l);
                    j--;
                }
            }
        }
        // todo:For reflection probes, only top three in view frustum are processed per frame (but scene can contain large amount of reflection probes)
        if(skyLightAndReflectionProbes.size() > 3){

        }
        ambientLightColor.a = 1.0f;
    }

}
