package com.jme3.renderer.pipeline;

import com.jme3.material.TechniqueDef;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;

public class TiledBasedDeferred extends RenderPipeline{
    public TiledBasedDeferred(TechniqueDef.Pipeline pipeline) {
        super(pipeline);
    }

    @Override
    public void begin(RenderManager rm, ViewPort vp) {

    }

    @Override
    public void draw(RenderManager rm, RenderQueue rq, ViewPort vp, boolean flush) {

    }

    @Override
    public void drawGeometry(RenderManager rm, Geometry geom) {

    }

    @Override
    public void end(RenderManager rm, ViewPort vp) {

    }
}
