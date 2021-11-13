package com.jme3.renderer.pipeline;

import com.jme3.material.TechniqueDef;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;

import java.util.HashMap;
import java.util.Map;

/**
 * RenderPipeline provides specific rendering control for rendering scene entities.<br/>
 * @author JhonKkk
 * @date 2021年11月13日19点10分
 */
public abstract class RenderPipeline {
    private static final Map<TechniqueDef.Pipeline, RenderPipeline> renderPipelines = new HashMap<>();

    /**
     * The pipeline is ready to start.<br/>
     */
    public abstract void begin();

    /**
     * Call Pipeline in RenderManager to perform rendering.<br/>
     * @param rm
     * @param rq
     */
    public abstract void draw(RenderManager rm, RenderQueue rq, ViewPort vp, boolean flush);

    /**
     * End of pipeline execution.<br/>
     */
    public abstract void end();

    /**
     *
     * @param pipeline
     * @param renderPipeline
     */
    public static final void addPipeline(TechniqueDef.Pipeline pipeline, RenderPipeline renderPipeline){
        if(renderPipelines.containsKey(pipeline)){
            throw new IllegalStateException(pipeline + " already exists!");
        }
        // add
        renderPipelines.put(pipeline, renderPipeline);
    }

    /**
     *
     * @param pipeline
     * @return
     */
    public static final RenderPipeline getPipeline(TechniqueDef.Pipeline pipeline){
        if(!renderPipelines.containsKey(pipeline)){
            switch (pipeline){
                case Forward:
                    addPipeline(pipeline, new Forward());
                    break;
                case ForwardPlus:
                    break;
                case Deferred:
                    addPipeline(pipeline, new Deferred());
                    break;
                case TiledBasedDeferred:
                    addPipeline(pipeline, new TiledBasedDeferred());
                    break;
                case ClusteredBasedDeferred:
                    break;
            }
        }
        return renderPipelines.get(pipeline);
    }
}
