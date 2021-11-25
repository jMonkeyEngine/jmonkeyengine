package com.jme3.scene.plugins.fbx.objects;

import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxMaterial extends FbxObject {

    public String shadingModel = "phong";
    public Vector3f ambientColor = new Vector3f(0.2f, 0.2f, 0.2f);
    public float ambientFactor = 1.0f;
    public Vector3f diffuseColor = new Vector3f(0.8f, 0.8f, 0.8f);
    public float diffuseFactor = 1.0f;
    public Vector3f specularColor = new Vector3f(0.2f, 0.2f, 0.2f);
    public float specularFactor = 1.0f;
    public float shininessExponent = 1.0f;

    public Material material;

    public FbxMaterial(SceneLoader scene, FbxElement element) {
        super(scene, element);
        if(type.equals("")) {
            for(FbxElement e : element.children) {
                if(e.id.equals("ShadingModel")) {
                    shadingModel = (String) e.properties.get(0);
                } else if(e.id.equals("Properties70")) {
                    for(FbxElement e2 : e.children) {
                        if(e2.id.equals("P")) {
                            double x, y, z;
                            String propName = (String) e2.properties.get(0);
                            switch(propName) {
                            case "AmbientColor":
                                x = (Double) e2.properties.get(4);
                                y = (Double) e2.properties.get(5);
                                z = (Double) e2.properties.get(6);
                                ambientColor.set((float) x, (float) y, (float) z);
                                break;
                            case "AmbientFactor":
                                x = (Double) e2.properties.get(4);
                                ambientFactor = (float) x;
                                break;
                            case "DiffuseColor":
                                x = (Double) e2.properties.get(4);
                                y = (Double) e2.properties.get(5);
                                z = (Double) e2.properties.get(6);
                                diffuseColor.set((float) x, (float) y, (float) z);
                                break;
                            case "DiffuseFactor":
                                x = (Double) e2.properties.get(4);
                                diffuseFactor = (float) x;
                                break;
                            case "SpecularColor":
                                x = (Double) e2.properties.get(4);
                                y = (Double) e2.properties.get(5);
                                z = (Double) e2.properties.get(6);
                                specularColor.set((float) x, (float) y, (float) z);
                                break;
                            case "Shininess":
                            case "ShininessExponent":
                                x = (Double) e2.properties.get(4);
                                shininessExponent = (float) x;
                                break;
                            }
                        }
                    }
                }
            }
            material = createMaterial();
        }
    }

    @Override
    public void link(FbxObject otherObject, String propertyName) {
        if(otherObject instanceof FbxTexture) {
            FbxTexture tex = (FbxTexture) otherObject;
            if(tex.texture == null || material == null)
                return;
            switch(propertyName) {
            case "DiffuseColor":
                material.setTexture("DiffuseMap", tex.texture);
                material.setColor("Diffuse", ColorRGBA.White);
                break;
            case "SpecularColor":
                material.setTexture("SpecularMap", tex.texture);
                material.setColor("Specular", ColorRGBA.White);
                break;
            case "NormalMap":
                material.setTexture("NormalMap", tex.texture);
                break;
            }
        }
    }

    private Material createMaterial() {
        Material m = new Material(scene.assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setName(name);
        ambientColor.multLocal(ambientFactor);
        diffuseColor.multLocal(diffuseFactor);
        specularColor.multLocal(specularFactor);
        m.setColor("Ambient", new ColorRGBA(ambientColor.x, ambientColor.y, ambientColor.z, 1));
        m.setColor("Diffuse", new ColorRGBA(diffuseColor.x, diffuseColor.y, diffuseColor.z, 1));
        m.setColor("Specular", new ColorRGBA(specularColor.x, specularColor.y, specularColor.z, 1));
        m.setFloat("Shininess", shininessExponent);
        m.setBoolean("UseMaterialColors", true);
        m.setFloat("AlphaDiscardThreshold", 0.5f); // TODO replace with right way in JME to set "Alpha Test"
        m.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        return m;
    }

}
