package com.jme3.shader.builder;

import com.jme3.asset.AssetManager;
import com.jme3.material.*;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.*;
import com.jme3.texture.image.ColorSpace;

import java.util.*;

public class MaterialBuilder {

    private MaterialDef matDef;
    private Map<String, TechniqueBuilder> techBuilders = new HashMap<>();
    private AssetManager assetManager;
    private TechniqueBuilder currentTechnique;

    public MaterialBuilder(AssetManager assetManager) {
        matDef = new MaterialDef(assetManager, "MatDef");
        this.assetManager = assetManager;
    }

    public MaterialBuilder(AssetManager assetManager, MaterialDef matDef) {
        this.matDef = matDef;
        this.assetManager = assetManager;
    }

    public MaterialBuilder(AssetManager assetManager, String matDefName) {
        this.matDef = (MaterialDef) assetManager.loadAsset(matDefName);
        this.assetManager = assetManager;
    }

    public TechniqueBuilder technique(String name) {
        TechniqueBuilder tb = techBuilders.get(name);
        if (tb == null) {
            List<TechniqueDef> defs = matDef.getTechniqueDefs(name);
            if (defs == null || defs.isEmpty()) {
                String techniqueUniqueName = matDef.getAssetName() + "@" + name;
                tb = new TechniqueBuilder(assetManager, name, techniqueUniqueName.hashCode());
                matDef.addTechniqueDef(tb.getTechniqueDef());

            } else {
                tb = new TechniqueBuilder(assetManager, defs.get(0));
            }
            techBuilders.put(name, tb);
        }
        currentTechnique = tb;
        return tb;
    }

    public TechniqueBuilder technique() {
        return this.technique("Default");
    }

    public Material build() {

        for (Map.Entry<String, TechniqueBuilder> entry : techBuilders.entrySet()) {
            TechniqueBuilder tb = entry.getValue();
            tb.build();
            ShaderUtils.computeShaderNodeGenerationInfo(tb.getTechniqueDef(), matDef);
        }
        return new Material(matDef);
    }

    public ShaderNodeVariable var(String expression) {
        String[] names = expression.split("\\.");
        if (names.length != 2) {
            // we might have an inlined expression
            ShaderNodeVariable tmp = new ShaderNodeVariable(null, null);
            tmp.setDefaultValue(expression);
            return tmp;
        }
        if (names[0].equals("MatParam")) {
            MatParam param = matDef.getMaterialParam(names[1]);
            if (param == null) {
                throw new IllegalArgumentException("Couldn't find material parameter named " + names[1]);
            }
            return new ShaderNodeVariable(param.getVarType().getGlslType(), names[0], names[1], null, "m_");
        }
        if (names[0].equals("WorldParam")) {
            UniformBinding worldParam = UniformBinding.valueOf(names[1]);
            currentTechnique.addWorldParam(worldParam.name());
            return new ShaderNodeVariable(worldParam.getGlslType(), "WorldParam", worldParam.name(), null, "g_");
        }
        if (names[0].equals("Attr")) {
            String n = names[1].substring(2);
            VertexBuffer.Type attribute = VertexBuffer.Type.valueOf(n);
            return new ShaderNodeVariable(ShaderUtils.getDefaultAttributeType(attribute), names[0], names[1]);
        }

        if (names[0].equals("Global")) {
            if(!names[1].equals("position") && !names[1].startsWith("color")){
                throw new IllegalArgumentException("Global output must be outPosition or outColor, got " + names[1]);
            }

            return new ShaderNodeVariable("vec4", names[0], names[1]);
        }

        ShaderNodeBuilder nb = currentTechnique.node(names[0]);
        if (nb == null) {
            throw new IllegalArgumentException("Couldn't find node named " + names[0]);
        }

        ShaderNodeVariable v = nb.variable(names[1]);
        if (v == null) {
            throw new IllegalArgumentException("Couldn't find variable named " + names[1] + " in node " + names[0]);
        }

        return v;
    }

    public VariableMappingBuilder map(String param, String expression) {
        return new VariableMappingBuilder(param, var(expression));
    }

    public VariableMappingBuilder map(String param, ShaderNodeVariable variable) {
        return new VariableMappingBuilder(param, variable);
    }

    public VariableMappingBuilder map(String param, VertexBuffer.Type attribute) {
        ShaderNodeVariable variable = new ShaderNodeVariable(ShaderUtils.getDefaultAttributeType(attribute), "Attr", "in" + attribute.name());
        return new VariableMappingBuilder(param, variable);
    }

    public VariableMappingBuilder map(String param, UniformBinding worldParam) {
        ShaderNodeVariable variable = new ShaderNodeVariable(worldParam.getGlslType(), "WorldParam", worldParam.name(), null, "g_");
        currentTechnique.addWorldParam(worldParam.name());
        return new VariableMappingBuilder(param, variable);
    }

    public void addMatParam(VarType type, String name){
        if(type.isTextureType()){
            matDef.addMaterialParamTexture(type, name, ColorSpace.sRGB);
        } else {
            matDef.addMaterialParam(type, name, null);
        }
    }

    public void addMatParamTexture(VarType type, String name, ColorSpace colorSpace){
        if(!type.isTextureType()){
            throw new IllegalArgumentException(type + "is not a texture type ");
        }
        matDef.addMaterialParamTexture(type, name, colorSpace);
    }

}
