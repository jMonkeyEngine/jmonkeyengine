/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
