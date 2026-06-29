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
import com.jme3.vulkan.material.experimental.PBR;
import com.jme3.vulkan.material.experimental.PBRTechnique;
import com.jme3.vulkan.render.bucket.GeometryBucket;
import com.sun.tools.javac.util.List;

public class TestJme4 extends SimpleApplication {

    private Engine engine;
    private ViewPort mainView;
    private final PBRTechnique pbrTech = new PBRTechnique();

    public static void main(String[] args) {
        TestJme4 app = new TestJme4();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        engine = new SimpleVulkanEngine(this, 3);

        Geometry g = new Geometry("geom_jme4", new Box(1f, 1f, 1f));
        Material mat = new Material();
        PBR pbr = mat.setInterface(PBR.class, new PBR(engine));
        pbr.setColor(ColorRGBA.White);
        pbr.setMetallic(1f);
        pbr.setRoughness(0.2f);
        pbr.setNormalMap(assetManager.loadTexture("Textures/mat_normal.jpg"));
        g.setMaterial(mat);
        rootNode.attachChild(g);

        Camera cam = new PerspectiveCamera();
        mainView = new ViewPort(cam, new ViewPortArea(1024, 1024));
        mainView.addGeometryBucket("Opaque", new GeometryBucket(new OpaqueComparator()));
        mainView.addGeometryBucket("Sky", new SkyBucket());
        mainView.addGeometryBucket("Transparent", new GeometryBucket(new TransparentComparator()));
        mainView.addGeometryBucket("Translucent", new GeometryBucket(new TransparentComparator()));

    }

    @Override
    public void simpleRender(RenderManager rm) {
        engine.renderViewPorts(List.of(mainView), List.of(pbrTech));
    }

    @Override
    public Engine getEngine() {
        return engine;
    }

}
