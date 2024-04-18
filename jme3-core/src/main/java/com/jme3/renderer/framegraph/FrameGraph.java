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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The FrameGraph system is used to manage render passes and the dependencies between them in a declarative way. Some key aspects:<br/>
 * - It represents the rendering pipeline as a directed acyclic graph of passes and their inputs/outputs.<br/>
 * - Passes can be things like shadow map creation, geometry passes, post-processing etc.<br/>
 * - Outputs from one pass can be reused as inputs to others, avoiding redundant work.<br/>
 * - The FrameGraph handles synchronization, pass scheduling and resource transitions automatically based on dependencies.<br/>
 * - Developers define passes and connect inputs/outputs, while the system handles execution.<br/>
 * - Passes can have arbitrary Java logic, shaders, and framebuffer configurations.<br/>
 * - Framebuffers are created on demand based on pass requirements.<br/>
 * - FrameGraphRecursion queues allow recursive pass execution (e.g. for nested realtime reflections).<br/>
 * - The system optimizes what needs to run each frame based on dirty state.<br/>
 * In summary, FrameGraph enables declaring a rendering pipeline at a high level while handling all the underlying complexity of synchronization, resource management, and efficient execution. This simplifies the developer's job and avoids boilerplate code.<br/>
 * The key advantages are automatic input/output reuse, efficient scheduling and batching, simplified boilerplate, and handling advanced cases like recursions. Overall it provides a clean abstraction for building complex, efficient rendering pipelines.<br/>
 * <a href="https://www.gdcvault.com/play/1024612/FrameGraph-Extensible-Rendering-Architecture-in">https://www.gdcvault.com/play/1024612/FrameGraph-Extensible-Rendering-Architecture-in.</a>
 * @author JohnKkk
 */
public class FrameGraph {
    
    private static final Logger logger = Logger.getLogger(FrameGraph.class.getName());
    private ArrayList<FGPass> passes;
    private ArrayList<FGSource> globalSources;
    private ArrayList<FGSink> globalSinks;
    private boolean finalized = false;
    private FGRenderContext renderContext;
    
    public FrameGraph(FGRenderContext renderContext){
        passes = new ArrayList<>();
        globalSinks = new ArrayList<>();
        globalSources = new ArrayList<>();
        this.renderContext = renderContext;
    }

    public FGRenderContext getRenderContext() {
        return renderContext;
    }

    /**
     * Binding input resources to the global sink in a FrameGraph refers to making certain resources available globally to all passes.
     */
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

    /**
     * Binding input resources to a specific pass in a FrameGraph refers to connecting the necessary resources that pass requires as inputs.
     * @param pass targetPass
     */
    protected void linkSinks(FGPass pass){
        for(FGSink sink : pass.getSinks()){
            String linkPassName = sink.getLinkPassName();
            
            if((linkPassName == null || linkPassName.isEmpty())){
                if(sink.isRequired()){
                    logger.throwing(FrameGraph.class.toString(), "<linkSinks>", new NullPointerException("In pass named [" + pass.getName() + "] sink named [" + sink.getRegisteredName() + "] has no target source set."));
//                    logger.log(Level.WARNING, "In pass named [" + pass.getName() + "] sink named [" + sink.getRegisteredName() + "] has no target source set.");
//                    System.err.println("In pass named [" + pass.getName() + "] sink named [" + sink.getRegisteredName() + "] has no target source set.");
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
                        logger.throwing(FrameGraph.class.toString(), "<linkSinks>", new NullPointerException("Pass named [" + linkPassName + "] not found"));
//                        System.err.println("Pass named [" + linkPassName + "] not found");
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
                        logger.throwing(FrameGraph.class.toString(), "<linkSinks>", new NullPointerException("Pass named [" + linkPassName + "] not found"));
//                        System.err.println("Pass named [" + linkPassName + "] not found");
                        return;
                    }
                }
            }
        }
    }

    /**
     * Execute frameGraph
     * <p>
     * example:
     * <code>
     * FrameGraph fg = new FrameGraph();
     * fg.addPass(pass1);
     * fg.addPass(pass2);
     * fg.finalize();
     * fg.execute();
     * </code>
     */
    public void execute(){
        assert finalized;
        for(FGPass nextPass : passes){
            nextPass.execute(renderContext);
        }
        finalized = false;
    }

    /**
     * The FrameGraph can be reused by just calling reset() to clear the current graph, then re-adding the required passes and binding the necessary resources again, before calling execute() once more.<br/>
     * This allows reusing the same FrameGraph instance to construct different render pipelines, avoiding repeated resource creation. Just update the passes and connections as needed. This improves code reuse and simplifies render pipeline adjustments.<br/>
     */
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

    /**
     * Some resources may not belong to any pass, but need to be shared across multiple framegraphs.<br/>
     * @param source targetFGSource
     */
    public void addGlobalSource(FGSource source){
        globalSources.add(source);
    }

    /**
     * Some resources may not belong to any pass, but need to be shared across multiple framegraphs.<br/>
     * @param source targetFGSource
     */
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

    /**
     * A FrameGraph may contain global sinks, such as a backBuffer.<br/>
     * @param sink targetFGSink
     */
    public void addGlobalSink(FGSink sink){
        globalSinks.add(sink);
    }

    /**
     * Adding an executable Pass to a FrameGraph, note that Passes will execute in the order they are added:<br/>
     * - To add a Pass to a FrameGraph, call frameGraph.addPass() and provide a name and a Pass executor function.<br/>
     * - The executor function contains the actual rendering commands for that Pass.<br/>
     * - Passes added earlier will execute before ones added later.<br/>
     * - Add passes in the order of desired execution.<br/>
     * - After adding passes, call frameGraph.validate() to validate the graph before execution.<br/>
     * - Then call frameGraph.compile() to prepare the graph for execution.<br/>
     * - In the render loop, call frameGraph.execute() to run the Pass network.<br/>
     * - Passes with unsatisfied resource dependencies will be skipped until their inputs are ready.<br/>
     * - FrameGraph handles scheduling passes in the correct order automatically based on dependencies.<br/>
     * - But the order passes are added determines the base execution order.<br/>
     * So in summary, add Passes in the desired execution order to the FrameGraph. The FrameGraph system will then handle scheduling them based on resource availability while respecting the original adding order.<br/>
     * @param pass targetPass
     */
    public final void addPass(FGPass pass){
        assert !finalized;
        // validate name uniqueness
        for(FGPass nextPass : passes){
            if(nextPass.getName().equals(pass.getName())){
                logger.throwing(FrameGraph.class.toString(), "<linkSinks>", new NullPointerException("Pass name already exists: " + pass.getName()));
//                System.err.println("Pass name already exists: " + pass.getName());
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
        logger.throwing(FrameGraph.class.toString(), "<linkSinks>", new NullPointerException("Failed to find pass name"));
//        System.err.println("Failed to find pass name");
        return null;
    }

    /**
     * Prepare all passes to get ready for execution of the frameGraph, by calling this function before executing the frameGraph.<br/>
     */
    public final void finalize(){
        assert !finalized;
        for(FGPass nextPass : passes){
            nextPass.finalizePass();
        }
        linkGlobalSinks();
        finalized = true;
    }
}
