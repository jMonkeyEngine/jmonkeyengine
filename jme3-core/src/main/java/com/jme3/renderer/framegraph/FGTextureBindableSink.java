package com.jme3.renderer.framegraph;

import com.jme3.material.Material;
import com.jme3.shader.VarType;

import java.util.ArrayList;

public class FGTextureBindableSink<T extends FGRenderTargetSource.RenderTargetSourceProxy> extends FGContainerBindableSink<T>{
    private Material material;
    private TextureBindableProxy textureBindableProxy;
    private static class TextureBindableProxy extends FGBindable{
        Material material;
        VarType bindTextureType;
        Object bindValue;
        String bindName;

        public TextureBindableProxy(Material material, String bindName, VarType bindTextureType) {
            // check bindTextureType
            this.bindTextureType = bindTextureType;
            this.material = material;
            this.bindName = bindName;
        }
        public final void setValue(Object value){
            bindValue = value;
        }

        @Override
        public void bind(FGRenderContext renderContext) {
            super.bind(renderContext);
            if(material != null && bindName != null){
                this.material.setParam(bindName, bindTextureType, bindValue);
            }
        }
    }
    public FGTextureBindableSink(String registeredName, ArrayList<FGBindable> container, int index, Material material, VarType bindTextureType) {
        super(registeredName, container, index);
        this.material = material;
        textureBindableProxy = new TextureBindableProxy(material, registeredName, bindTextureType);
    }

    @Override
    public void bind(FGSource fgSource) {
        T p = (T)fgSource.yieldBindable();
        if(p == null){
            System.err.println("Binding input [" + getRegisteredName() + "] to output [" + getLinkPassName() + "." + getLinkPassResName() + "] " + " { " + fgSource.getName() + " } ");
            return;
        }
        if(fgSource instanceof FGRenderTargetSource){
            linked = true;
            FGRenderTargetSource renderTargetSource = (FGRenderTargetSource)fgSource;
            textureBindableProxy.setValue(((FGRenderTargetSource.RenderTargetSourceProxy)renderTargetSource.yieldBindable()).getShaderResource());
            bindableProxy.targetBindable = textureBindableProxy;
        }
        else{
            System.err.println(getRegisteredName() + " needs a FGRenderTargetSource");
        }
    }
}
