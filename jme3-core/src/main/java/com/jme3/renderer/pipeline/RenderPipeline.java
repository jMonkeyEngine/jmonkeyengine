package com.jme3.renderer.pipeline;

import com.jme3.material.TechniqueDef;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;

import java.util.HashMap;
import java.util.Map;

/**
 * RenderPipeline provides specific rendering control for rendering scene entities.<br/>
 * @author JhonKkk
 * @date 2021年11月13日19点10分
 */
public abstract class RenderPipeline {
    private static final Map<TechniqueDef.Pipeline, RenderPipeline> renderPipelines = new HashMap<>();
    private TechniqueDef.Pipeline pipeline;

    public RenderPipeline(TechniqueDef.Pipeline pipeline){
        this.pipeline = pipeline;
    }

    public TechniqueDef.Pipeline getPipeline() {
        return pipeline;
    }

    /**
     * The pipeline is ready to start.<br/>
     * @param rm
     */
    public abstract void begin(RenderManager rm, ViewPort vp);

    /**
     * Call Pipeline in RenderManager to perform rendering.<br/>
     * @param rm
     * @param rq
     */
    public abstract void draw(RenderManager rm, RenderQueue rq, ViewPort vp, boolean flush);

    /**
     * Submit the given Geometry to the Pipeline for rendering.
     * @param rm
     * @param geom
     */
    public abstract void drawGeometry(RenderManager rm, Geometry geom);

    /**
     * End of pipeline execution.<br/>
     * @param rm
     */
    public abstract void end(RenderManager rm, ViewPort vp);

    /**
     * Internal use only.
     * Updates the resolution of all on-screen cameras to match
     * the given width and height.
     *
     * @param w the new width (in pixels)
     * @param h the new height (in pixels)
     */
    public void reshape(int w, int h){
        // do nothing
    }

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
                    addPipeline(pipeline, new Forward(pipeline));
                    break;
                case ForwardPlus:
                    break;
                case Deferred:
                    addPipeline(pipeline, new Deferred(pipeline));
                    break;
                case TiledBasedDeferred:
                    addPipeline(pipeline, new TiledBasedDeferred(pipeline));
                    break;
                case ClusteredBasedDeferred:
                    break;
            }
        }
        return renderPipelines.get(pipeline);
    }
}
