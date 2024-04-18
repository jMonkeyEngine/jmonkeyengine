/*
 * Copyright (c) 2009-2023 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
