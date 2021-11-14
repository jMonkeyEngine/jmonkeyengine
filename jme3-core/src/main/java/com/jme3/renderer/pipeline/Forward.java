package com.jme3.renderer.pipeline;

import com.jme3.material.TechniqueDef;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.VpStep;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;

public class Forward extends RenderPipeline{
    public Forward(TechniqueDef.Pipeline pipeline) {
        super(pipeline);
    }

    @Override
    public void begin(RenderManager rm, ViewPort vp) {

    }

    @Override
    public void draw(RenderManager rm, RenderQueue rq, ViewPort vp, boolean flush) {
        Camera cam = vp.getCamera();
        Renderer renderer = rm.getRenderer();
        AppProfiler prof = rm.getAppProfiler();
        boolean depthRangeChanged = false;

        // render opaque objects with default depth range
        // opaque objects are sorted front-to-back, reducing overdraw
        if (prof!=null) prof.vpStep(VpStep.RenderBucket, vp, RenderQueue.Bucket.Opaque);
        rq.renderQueue(RenderQueue.Bucket.Opaque, rm, cam, flush);

        // render the sky, with depth range set to the farthest
        if (!rq.isQueueEmpty(RenderQueue.Bucket.Sky)) {
            if (prof!=null) prof.vpStep(VpStep.RenderBucket, vp, RenderQueue.Bucket.Sky);
            renderer.setDepthRange(1, 1);
            rq.renderQueue(RenderQueue.Bucket.Sky, rm, cam, flush);
            depthRangeChanged = true;
        }


        // transparent objects are last because they require blending with the
        // rest of the scene's objects. Consequently, they are sorted
        // back-to-front.
        if (!rq.isQueueEmpty(RenderQueue.Bucket.Transparent)) {
            if (prof!=null) prof.vpStep(VpStep.RenderBucket, vp, RenderQueue.Bucket.Transparent);
            if (depthRangeChanged) {
                renderer.setDepthRange(0, 1);
                depthRangeChanged = false;
            }

            rq.renderQueue(RenderQueue.Bucket.Transparent, rm, cam, flush);
        }

        if (!rq.isQueueEmpty(RenderQueue.Bucket.Gui)) {
            if (prof!=null) prof.vpStep(VpStep.RenderBucket, vp, RenderQueue.Bucket.Gui);
            renderer.setDepthRange(0, 0);
            rm.setCamera(cam, true);
            rq.renderQueue(RenderQueue.Bucket.Gui, rm, cam, flush);
            rm.setCamera(cam, false);
            depthRangeChanged = true;
        }

        // restore range to default
        if (depthRangeChanged) {
            renderer.setDepthRange(0, 1);
        }
    }

    @Override
    public void drawGeometry(RenderManager rm, Geometry geom) {
        rm.renderGeometry(geom);
    }

    @Override
    public void end(RenderManager rm, ViewPort vp) {

    }
}
