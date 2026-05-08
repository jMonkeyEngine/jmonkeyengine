package com.jme3.vulkan.renderexp;

import com.jme3.renderer.ViewPort;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.material.experimental.ShadingTechnique;
import com.jme3.vulkan.render.bucket.GeometryBucket;

import java.util.*;

// running out of descriptive class names :(
public class MainRendererThingy {

    public void render(RenderSession session, Collection<ViewPort> viewPorts, Queue<ShadingTechnique> techniques) {
        for (ViewPort vp : viewPorts) {
            GeometryRenderBuffer buffer = session.createRenderBuffer(vp);
            for (GeometryBucket b : vp.gatherGeometry(s -> s.runControlRender(session.getEngine(), vp))) {
                for (ShadingTechnique t : techniques) {
                    b.selectGeometries(buffer, t);
                    buffer.sort(b.getComparator());
                    buffer.render(e -> t.renderElement(session, vp, e));
                    buffer.clear();
                    if (b.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

}
