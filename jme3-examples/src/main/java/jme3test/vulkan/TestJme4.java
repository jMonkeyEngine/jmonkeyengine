package jme3test.vulkan;

import com.jme3.app.SimpleApplication;
import com.jme3.backend.Engine;
import com.jme3.backend.SimpleVulkanEngine;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.ViewPortArea;
import com.jme3.renderer.camera.PerspectiveCamera;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.TransparentComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.commands.StandardRenderSettings;
import com.jme3.vulkan.material.structs.UnshadedParams;
import com.jme3.vulkan.mesh.attributes.AttributeMapping;
import com.jme3.vulkan.render.bucket.GeometryBucket;
import com.sun.tools.javac.util.List;
import org.graalvm.compiler.lir.amd64.AMD64BinaryConsumer;

public class TestJme4 extends SimpleApplication {

    private Engine engine;

    public static void main(String[] args) {
        TestJme4 app = new TestJme4();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        engine = new SimpleVulkanEngine(this, 3);

        Geometry g = new Geometry("geom_jme4", new Box(1f, 1f, 1f));
        Material mat = engine.createMaterial("Common/MatDefs/Misc/Unshaded.j3md");
        try (MyParams p = mat.mapParameters()) {
            p.getColor().set(ColorRGBA.Blue);
        }
        g.setMaterial(mat);
        rootNode.attachChild(g);

        Camera cam = new PerspectiveCamera();
        ViewPort vp = new ViewPort(cam, new ViewPortArea(1024, 1024));
        vp.addGeometryBucket("Opaque", new GeometryBucket(new OpaqueComparator()));
        vp.addGeometryBucket("Sky", new GeometryBucket(new OpaqueComparator()) {
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
        vp.addGeometryBucket("Transparent", new GeometryBucket(new TransparentComparator()));
        vp.addGeometryBucket("Translucent", new GeometryBucket(new TransparentComparator()));

    }

    @Override
    public void simpleRender(RenderManager rm) {
        engine.render(List.of(viewPort, guiViewPort));
    }

    @Override
    public Engine getEngine() {
        return engine;
    }

}
