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

/**
 * FGContainerBindableSink is used to proxy a FGSink, and also has the role of FGBindable.
 * 
 * Typically, a Sink needed by a Pass may be a Bindable object.
 * 
 * @author JohnKkk
 */
public class BindableSink extends AbstractFGSink implements FGBindable {
    
    protected FGBindable target;
    protected boolean linked = false;

    public BindableSink(String registeredName) {
        super(registeredName);
    }

    @Override
    public void bind(RenderContext renderContext) {
        if (target != null) {
            target.bind(renderContext);
        }
    }
    
    @Override
    public void bind(FGSource src) {
        FGBindable p = src.yieldBindable();
        if(p == null){
            System.err.println("Binding input [" + getRegisteredName() + "] to output [" + getLinkPassName() + "." + getLinkPassResName() + "] " + " { " + src.getName() + " } ");
            return;
        }
        target = p;
        //container.set(index, p);
        linked = true;
    }

    @Override
    public void postLinkValidate() {
        if(!linked){
            if(bIsRequired)
                System.err.println("Unlinked input: " + getRegisteredName());
        }
        else{
            bLinkValidate = true;
        }
    }

    @Override
    public FGBindable getBind() {
        return target;
    }
}
