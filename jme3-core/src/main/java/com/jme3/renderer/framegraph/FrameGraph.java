/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.ArrayList;

/**
 * @see https://www.gdcvault.com/play/1024612/FrameGraph-Extensible-Rendering-Architecture-in.<br/>
 * @author JohnKkk
 */
public class FrameGraph {
    private ArrayList<FGPass> passes;
    private ArrayList<FGSource> globalSources;
    private ArrayList<FGSink> globalSinks;
    private boolean finalized = false;
    private FGRenderContext renderContext;
    
    public FrameGraph(FGRenderContext renderContext){
        passes = new ArrayList<FGPass>();
        globalSinks = new ArrayList<FGSink>();
        globalSources = new ArrayList<FGSource>();
        this.renderContext = renderContext;
    }

    public FGRenderContext getRenderContext() {
        return renderContext;
    }

    protected void linkGlobalSinks(){
        for(FGSink sink : globalSinks){
            String linkPassName = sink.getLinkPassName();
            for(FGPass nextPass : passes){
                if(nextPass.getName().equals(linkPassName)){
                    FGSource source = nextPass.getSource(sink.getLinkPassResName());
                    sink.bind(source);
                    break;
                }
            }
        }
    }
    
    protected void linkSinks(FGPass pass){
        for(FGSink sink : pass.getSinks()){
            String linkPassName = sink.getLinkPassName();
            
            if((linkPassName == null || linkPassName.isEmpty())){
                if(sink.isRequired()){
                    System.err.println("In pass named [" + pass.getName() + "] sink named [" + sink.getRegisteredName() + "] has no target source set.");
                    return;
                }
                else{
                    continue;
                }
            }
            
            // check check whether target source is global
            if(linkPassName.equals(FGGlobal.S_GLOABLE_PASS_SOURCE_NAME)){
                boolean bound = false;
                for(FGSource source : globalSources){
                    if(source.getName().equals(sink.getLinkPassResName())){
                        sink.bind(source);
                        bound = true;
                        break;
                    }
                }
                if(!bound){
                    bound = FGGlobal.linkSink(sink);
                    if(!bound && sink.isRequired()){
                        System.err.println("Pass named [" + linkPassName + "] not found");
                        return;
                    }
                }
            }
            else{
                // find source from within existing passes
                boolean bound = false;
                for(FGPass nextPass : passes){
                    if(nextPass.getName().equals(linkPassName)){
                        FGSource source = nextPass.getSource(sink.getLinkPassResName());
                        sink.bind(source);
                        bound = true;
                        break;
                    }
                }
                if(!bound){
                    bound = FGGlobal.linkSink(sink);
                    if(!bound && sink.isRequired()){
                        System.err.println("Pass named [" + linkPassName + "] not found");
                        return;
                    }
                }
            }
        }
    }
    
    public void execute(){
        assert finalized;
        for(FGPass nextPass : passes){
            nextPass.execute(renderContext);
        }
        finalized = false;
    }
    
    public void reset(){
        assert !finalized;
        for(FGPass nextPass : passes){
            nextPass.reset();
        }
        passes.clear();
        if(renderContext != null && renderContext.renderManager != null){
            renderContext.renderManager.setRenderGeometryHandler(null);
        }
    }
    
    public void addGlobalSource(FGSource source){
        globalSources.add(source);
    }

    public void replaceOrAddGlobalSource(FGSource source){
        int index = -1;
        for(int i = 0;i < globalSources.size();i++){
            if(globalSources.get(i).getName().equals(source.getName())){
                index = i;
                break;
            }
        }
        if(index >= 0){
            globalSources.remove(index);
        }
        globalSources.add(source);
    }
    
    public void addGlobalSink(FGSink sink){
        globalSinks.add(sink);
    }
    
    public final void addPass(FGPass pass){
        assert !finalized;
        // validate name uniqueness
        for(FGPass nextPass : passes){
            if(nextPass.getName().equals(pass.getName())){
                System.err.println("Pass name already exists: " + pass.getName());
                return;
            }
        }
        
        // link outputs from passes (and global outputs) to pass inputs
        if(pass.getSinks() != null && pass.getSinks().size() > 0)
            linkSinks(pass);
        
        // add to container of passes
        pass.prepare(renderContext);
        passes.add(pass);
    }
    
    public final FGPass findPassByName(String name){
        for(FGPass nextPass : passes){
            if(nextPass.getName().equals(name)){
                return nextPass;
            }
        }
        System.err.println("Failed to find pass name");
        return null;
    }
    
    public final void finalize(){
        assert !finalized;
        for(FGPass nextPass : passes){
            nextPass.finalize();
        }
        linkGlobalSinks();
        finalized = true;
    }
}
