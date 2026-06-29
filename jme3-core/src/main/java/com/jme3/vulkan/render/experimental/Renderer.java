package com.jme3.vulkan.render.experimental;

import com.jme3.renderer.ViewPort;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.material.experimental.OldPBRTechnique;
import com.jme3.vulkan.render.bucket.GeometryBucket;

import java.util.ArrayList;
import java.util.List;

public class Renderer {

    private MappableBuffer globals;
    private OldPBRTechnique unshaded;
    private final List<ViewPort> viewPorts = new ArrayList<>();

    public void render(RenderSession session) {
        for (ViewPort vp : viewPorts) {
            for (GeometryBucket b : vp.gatherGeometry(s -> s.runControlRender(session.getEngine(), vp))) {
                session.renderSorted(b.getGeometries().stream(), b.getComparator(), vp.getCamera(), unshaded.getProgram(), e -> {
                    unshaded.renderElement(, vp, e);
                });
            }
        }
    }

}
