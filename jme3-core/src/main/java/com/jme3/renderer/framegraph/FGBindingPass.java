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

import java.util.ArrayList;

/**
 *A FGBindingPass represents a Pass that needs to perform state machine binding, ShaderResource binding, FrameBuffer binding and other operations.
 * @author JohnKkk
 */
public class FGBindingPass extends AbstractFGPass {
    
    protected ArrayList<FGBindable> binds;
    
    protected FGBindingPass(String name){
        this(name, new ArrayList<FGBindable>());
    }
    protected FGBindingPass(String name, ArrayList<FGBindable> binds){
        super(name);
        this.binds = binds;
    }
    
    public void addBind(FGBindable bind){
        binds.add(bind);
    }
    
    public <T extends FGBindable>void addBindSink(String name){
        int index = binds.size() - 1;
        registerSink(new FGContainerBindableSink<T>(name, binds, index));
    }
    
    public void bindAll(FGRenderContext renderContext){
        // Bind all objects
        for(FGBindable bind : binds){
            bind.bind(renderContext);
        }
    }

    @Override
    public void prepare(FGRenderContext renderContext) {}

    @Override
    public void execute(FGRenderContext renderContext) {}
    
}
