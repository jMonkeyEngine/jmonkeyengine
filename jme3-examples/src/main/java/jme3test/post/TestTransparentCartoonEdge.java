package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.CartoonEdgeFilter;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

public class TestTransparentCartoonEdge extends SimpleApplication {

    public static void main(String[] args){
        TestTransparentCartoonEdge app = new TestTransparentCartoonEdge();
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
        makeToonish(teaGeom);

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

        FilterPostProcessor fpp=new FilterPostProcessor(assetManager);
        CartoonEdgeFilter toon=new CartoonEdgeFilter();
        toon.setEdgeWidth(0.5f);
        toon.setEdgeIntensity(1.0f);
        toon.setNormalThreshold(0.8f);
        fpp.addFilter(toon);
        viewPort.addProcessor(fpp);
    }

        public void makeToonish(Spatial spatial){
        if (spatial instanceof Node){
            Node n = (Node) spatial;
            for (Spatial child : n.getChildren())
                makeToonish(child);
        }else if (spatial instanceof Geometry){
            Geometry g = (Geometry) spatial;
            Material m = g.getMaterial();
            if (m.getMaterialDef().getName().equals("Phong Lighting")){
                Texture t = assetManager.loadTexture("Textures/ColorRamp/toon.png");
//                t.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
//                t.setMagFilter(Texture.MagFilter.Nearest);
                m.setTexture("ColorRamp", t);
                m.setBoolean("UseMaterialColors", true);
                m.setColor("Specular", ColorRGBA.Black);
                m.setColor("Diffuse", ColorRGBA.White);
                m.setBoolean("VertexLighting", true);
            }
        }
    }
}
