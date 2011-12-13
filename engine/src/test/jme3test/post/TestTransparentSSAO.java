package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

public class TestTransparentSSAO extends SimpleApplication {

    public static void main(String[] args) {
        TestTransparentSSAO app = new TestTransparentSSAO();
        app.start();
    }

    public void simpleInitApp() {
        renderManager.setAlphaToCoverage(true);
        cam.setLocation(new Vector3f(0.14914267f, 0.58147097f, 4.7686534f));
        cam.setRotation(new Quaternion(-0.0044764364f, 0.9767943f, 0.21314798f, 0.020512417f));

//        cam.setLocation(new Vector3f(2.0606942f, 3.20342f, 6.7860126f));
//        cam.setRotation(new Quaternion(-0.017481906f, 0.98241085f, -0.12393151f, -0.13857932f));

        viewPort.setBackgroundColor(ColorRGBA.DarkGray);

        Quad q = new Quad(20, 20);
        q.scaleTextureCoordinates(Vector2f.UNIT_XY.mult(5));
        Geometry geom = new Geometry("floor", q);
        Material mat = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
        geom.setMaterial(mat);

        geom.rotate(-FastMath.HALF_PI, 0, 0);
        geom.center();
        geom.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(geom);

        // create the geometry and attach it
        Spatial teaGeom = assetManager.loadModel("Models/Tree/Tree.mesh.j3o");
        teaGeom.setQueueBucket(Bucket.Transparent);
        teaGeom.setShadowMode(ShadowMode.Cast);

        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(2));
        rootNode.addLight(al);

        DirectionalLight dl1 = new DirectionalLight();
        dl1.setDirection(new Vector3f(1, -1, 1).normalizeLocal());
        dl1.setColor(new ColorRGBA(0.965f, 0.949f, 0.772f, 1f).mult(0.7f));
        rootNode.addLight(dl1);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        dl.setColor(new ColorRGBA(0.965f, 0.949f, 0.772f, 1f).mult(0.7f));
        rootNode.addLight(dl);

        rootNode.attachChild(teaGeom);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        SSAOFilter ssao = new SSAOFilter(0.49997783f, 42.598858f, 35.999966f, 0.39299846f);
        fpp.addFilter(ssao);

        SSAOUI ui = new SSAOUI(inputManager, ssao);

        viewPort.addProcessor(fpp);
    }
}
