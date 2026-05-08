package jme3test.vulkan;

import com.jme3.app.SimpleApplication;
import com.jme3.backend.Engine;
import com.jme3.backend.SimpleVulkanEngine;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.ViewPortArea;
import com.jme3.renderer.camera.PerspectiveCamera;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.TransparentComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.vulkan.commands.StandardRenderSettings;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.material.experimental.FrameRenderer;
import com.jme3.vulkan.material.experimental.PBR;
import com.jme3.vulkan.render.bucket.GeometryBucket;

public class TestJme4 extends SimpleApplication {

    private Engine engine;
    private ViewPort mainView;

    public static void main(String[] args) {
        TestJme4 app = new TestJme4();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        engine = new SimpleVulkanEngine(this, 3);

        Geometry g = new Geometry("geom_jme4", new Box(1f, 1f, 1f));
        Material mat = engine.createMaterial("Common/MatDefs/Misc/Unshaded.j3md");
        mat.setInterface(PBR.class, PBR::new);
        try (PBR u = mat.getInterface(PBR.class)) {
            u.setColor(ColorRGBA.White);
            u.setColorMap(null);
        }
        g.setMaterial(mat);
        rootNode.attachChild(g);

        Camera cam = new PerspectiveCamera();
        mainView = new ViewPort(cam, new ViewPortArea(1024, 1024));
        mainView.addGeometryBucket("Opaque", new GeometryBucket(new OpaqueComparator()));
        mainView.addGeometryBucket("Sky", new GeometryBucket(new OpaqueComparator()) {
            @Override
            public void setupRender(ViewPort vp, StandardRenderSettings settings) {
                super.setupRender(vp, settings);
                settings.pushViewPort(vp.getArea().clone().toMaxDepth());
            }
            @Override
            public void cleanupRender(ViewPort vp, StandardRenderSettings settings) {
                super.cleanupRender(vp, settings);
                settings.popViewPort();
            }
        });
        mainView.addGeometryBucket("Transparent", new GeometryBucket(new TransparentComparator()));
        mainView.addGeometryBucket("Translucent", new GeometryBucket(new TransparentComparator()));

    }

    @Override
    public void simpleRender(RenderManager rm) {
        try (RenderSession r = engine.createRenderSession(tpf)) {
            r.ren
        }
    }

    @Override
    public Engine getEngine() {
        return engine;
    }

}
