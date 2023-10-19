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
 * FGPass.
 * @author JohnKkk
 */
public abstract class FGPass {
    private String name;
    private ArrayList<FGSink> sinks;
    private ArrayList<FGSource> sources;
    protected boolean resetClearSinksAndSources = false;
    public FGPass(String name){
        this.name = name;
        this.sinks = new ArrayList<>();
        this.sources = new ArrayList<>();
    }
    public void prepare(FGRenderContext renderContext){}
    public abstract void execute(FGRenderContext renderContext);

    public String getName() {
        return name;
    }

    public ArrayList<FGSink> getSinks() {
        return sinks;
    }

    public ArrayList<FGSource> getSources() {
        return sources;
    }
    
    public FGSource getSource(String name){
        for(FGSource src : sources){
            if(src.getName().equals(name)){
                return src;
            }
        }
        
        System.err.println("Output name [" + name + "] not fount in pass:" + getName());
        return null;
    }
    
    public FGSink getSink(String registeredName){
        for(FGSink sink : sinks){
            if(sink.getRegisteredName().equals(registeredName)){
                return sink;
            }
        }
        return null;
    }
    
    public void setSinkLinkage(String registeredName, String target){
        FGSink sink = getSink(registeredName);
        
        String targetSplit[] = target.split("\\.");
        if(targetSplit.length != 2){
            System.err.println("Input target has incorrect format");
        }
        sink.setTarget(targetSplit[0], targetSplit[1]);
    }
    
    protected void registerSink(FGSink sink){
        // check for overlap of input names
        for(FGSink si : sinks){
            if(si.getRegisteredName().equals(sink.getRegisteredName())){
                System.err.println("Registered input overlaps with existing: " + sink.getRegisteredName());
                return;
            }
        }
        
        sinks.add(sink);
    }
    
    public void registerSource(FGSource source){
        // check for overlap of output names
        for(FGSource src : sources){
            if(src.getName().equals(source.getName())){
                System.err.println("Registered input overlaps with existing: " + source.getName());
                return;
            }
        }
        
        sources.add(source);
    }
    
    public void reset(){
        if(resetClearSinksAndSources){
            this.sources.clear();
            this.sinks.clear();
        }
    }
    
    public void finalize(){
        if(sinks != null && sinks.size() > 0){
            for(FGSink sink : sinks){
                sink.postLinkValidate();
            }
        }
        
        if(sources != null && sources.size() > 0){
            for(FGSource src : sources){
                src.postLinkValidate();
            }
        }
    }
    
}
