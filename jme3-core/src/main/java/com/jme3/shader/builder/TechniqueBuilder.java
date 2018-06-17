package com.jme3.shader.builder;

import com.jme3.asset.AssetManager;
import com.jme3.asset.ShaderNodeDefinitionKey;
import com.jme3.material.ShaderGenerationInfo;
import com.jme3.material.TechniqueDef;
import com.jme3.material.logic.*;
import com.jme3.shader.*;

import java.text.ParseException;
import java.util.*;

public class TechniqueBuilder {

    private TechniqueDef techniqueDef;
    private AssetManager assetManager;
    private Map<String, ShaderNodeBuilder> nodeBuilders = new HashMap<>();

    protected TechniqueBuilder(AssetManager assetManager, String name, int sortId) {
        techniqueDef = new TechniqueDef(name, sortId);
        this.assetManager = assetManager;
    }

    protected TechniqueBuilder(AssetManager assetManager, TechniqueDef techniqueDef) {
        this.techniqueDef = techniqueDef;
        this.assetManager = assetManager;
    }

    protected TechniqueDef getTechniqueDef() {
        return techniqueDef;
    }

    public ShaderNodeBuilder node(String name) {
        ShaderNodeBuilder b = nodeBuilders.get(name);
        if (b == null){
            ShaderNode n = findShaderNode(name);
            if(n == null){
                throw new IllegalArgumentException("Can't find node with name " + name + " in technique definition " + techniqueDef.getName());
            }
            b = new ShaderNodeBuilder(n);
            nodeBuilders.put(name, b);
        }
        return b;
    }

    public ShaderNodeBuilder addNode(String name, String defName, String shaderNodeDefPath) {
        List<ShaderNodeDefinition> defs;
        if(shaderNodeDefPath.endsWith(".j3sn")){
            defs = assetManager.loadAsset(new ShaderNodeDefinitionKey(shaderNodeDefPath));
        } else {
            try {
                defs = ShaderUtils.loadSahderNodeDefinition(assetManager, shaderNodeDefPath);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Couldn't parse definition " + shaderNodeDefPath, e);
            }
        }
        ShaderNodeDefinition definition = findDefinition(defName, defs);
        if(definition == null){
            throw new IllegalArgumentException("Couldn't find definition " + defName + " in " + shaderNodeDefPath);
        }

        ShaderNodeBuilder b = new ShaderNodeBuilder(name,definition);
        if(techniqueDef.getShaderNodes() ==  null){
            techniqueDef.setShaderNodes(new ArrayList<ShaderNode>());
        }
        techniqueDef.setShaderFile(techniqueDef.hashCode() + "", techniqueDef.hashCode() + "", "GLSL100", "GLSL100");
        techniqueDef.getShaderNodes().add(b.getNode());
        techniqueDef.setShaderGenerationInfo(new ShaderGenerationInfo());
        techniqueDef.setLogic(new DefaultTechniqueDefLogic(techniqueDef));
        techniqueDef.setShaderPrologue("");
        nodeBuilders.put(name, b);
        return b;
    }


    public InlineShaderNodeBuilder inlineVertexNode(String type, String name, String code){
        return inlineNode(type, name, code, Shader.ShaderType.Vertex);
    }

    public InlineShaderNodeBuilder inlineFragmentNode(String type, String name, String code){
        return inlineNode(type, name, code, Shader.ShaderType.Fragment);
    }

    public InlineShaderNodeBuilder inlineNode(String returnType, String name, String code, Shader.ShaderType type){
        ShaderNodeDefinition def = new ShaderNodeDefinition();
        def.setName(name);
        def.setType(type);
        def.setReturnType(returnType);
        InlineShaderNodeBuilder sb = new InlineShaderNodeBuilder(name, def, code, techniqueDef);
        nodeBuilders.put(name, sb);
        techniqueDef.getShaderNodes().add(sb.getNode());
        return sb;
    }

    public boolean addWorldParam(String name){
        return techniqueDef.addWorldParam(name);
    }

    private void setLightMode(TechniqueDef.LightMode mode){
        switch (techniqueDef.getLightMode()) {
            case Disable:
                techniqueDef.setLogic(new DefaultTechniqueDefLogic(techniqueDef));
                break;
            case MultiPass:
                techniqueDef.setLogic(new MultiPassLightingLogic(techniqueDef));
                break;
            case SinglePass:
                techniqueDef.setLogic(new SinglePassLightingLogic(techniqueDef));
                break;
            case StaticPass:
                techniqueDef.setLogic(new StaticPassLightingLogic(techniqueDef));
                break;
            case SinglePassAndImageBased:
                techniqueDef.setLogic(new SinglePassAndImageBasedLightingLogic(techniqueDef));
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private ShaderNodeDefinition findDefinition(String defName, List<ShaderNodeDefinition> defs) {
        for (ShaderNodeDefinition def : defs) {
            if(def.getName().equals(defName)){
                return def;
            }
        }
        return null;
    }

    private ShaderNode findShaderNode(String name){
        for (ShaderNode shaderNode : techniqueDef.getShaderNodes()) {
            if(shaderNode.getName().equals(name)){
                return shaderNode;
            }
        }
        return null;
    }

    protected void build(){
        for (Map.Entry<String, ShaderNodeBuilder> entry : nodeBuilders.entrySet()) {
            ShaderNodeBuilder nb = entry.getValue();
            nb.build();
        }
    }
}
