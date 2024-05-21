package jme3test.renderpath;

import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.renderer.framegraph.FrameGraphFactory;
import com.jme3.scene.Geometry;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;

/**
 * <a href="https://leifnode.com/2015/05/tiled-deferred-shading/">https://leifnode.com/2015/05/tiled-deferred-shading/</a>
 * @author JohnKkk
 */
public class TestTileBasedDeferredShading extends SimpleApplication {
    private Material material;
    @Override
    public void simpleInitApp() {
        
        viewPort.setFrameGraph(FrameGraphFactory.deferred(assetManager, renderManager, true));
        
        // Based on your current resolution and total light source count, try adjusting the tileSize - it must be a power of 2 such as 32, 64, 128 etc.
        //renderManager.setForceTileSize(128);// 1600 * 900 resolution config
//        renderManager.setPreferredLightMode(TechniqueDef.LightMode.SinglePass);
//        renderManager.setSinglePassLightBatchSize(30);
//        renderManager.setRenderPath(RenderManager.RenderPath.Forward);
        //renderManager.setMaxDeferredShadingLights(1000);
        //renderManager.setRenderPath(RenderManager.RenderPath.TiledDeferred);
        Quad quad = new Quad(15, 15);
        Geometry geo = new Geometry("Floor", quad);
        material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setFloat("Shininess", 25);
        material.setColor("Ambient",  ColorRGBA.White);
        material.setColor("Diffuse",  ColorRGBA.White);
        material.setColor("Specular", ColorRGBA.White);
        material.setBoolean("UseMaterialColors", true);
        geo.setMaterial(material);
        geo.rotate((float) Math.toRadians(-90), 0, 0);
        geo.setLocalTranslation(-7, 0, -7);
        rootNode.attachChild(geo);

        Sphere sphere = new Sphere(15, 15, 0.1f);
        Geometry sp = new Geometry("sp", sphere);
        sp.setMaterial(material.clone());
        sp.getMaterial().setBoolean("UseInstancing", true);
        ColorRGBA colors[] = new ColorRGBA[]{
                ColorRGBA.White,
                ColorRGBA.Red,
                ColorRGBA.Blue,
                ColorRGBA.Green,
                ColorRGBA.Yellow,
                ColorRGBA.Orange,
                ColorRGBA.Brown,
        };

        InstancedNode instancedNode = new InstancedNode("sp");
        for(int i = 0;i < 1000;i++){
            PointLight pl = new PointLight();
            pl.setColor(colors[i % colors.length]);
            pl.setPosition(new Vector3f(FastMath.nextRandomFloat(-5.0f, 5.0f), 0.1f, FastMath.nextRandomFloat(-20.0f, -10.0f)));
//            pl.setPosition(new Vector3f(0, 1, 0));
//            if(i % 2 == 0){
//                pl.setColor(ColorRGBA.Red);
//                pl.setPosition(new Vector3f(-5, 0.1f, -15.0f));
//            }
//            else{
//                pl.setColor(ColorRGBA.White);
//                pl.setPosition(new Vector3f(-4, 0.1f, -14.0f));
//            }
            pl.setRadius(1.0f);
            rootNode.addLight(pl);
            Geometry g = sp.clone(false);
//            g.getMaterial().setColor("Ambient",  ColorRGBA.Gray);
//            g.getMaterial().setColor("Diffuse", colors[i % colors.length]);
            g.setLocalTranslation(pl.getPosition());
            instancedNode.attachChild(g);
        }
        instancedNode.instance();
        rootNode.attachChild(instancedNode);


//        AmbientLight ambientLight = new AmbientLight(new ColorRGBA(0.15f, 0.15f, 0.15f, 1.0f));
//        rootNode.addLight(ambientLight);
//        DirectionalLight sun = new DirectionalLight();
//        sun.setDirection((new Vector3f(-0.5f, -0.5f, -0.5f)).normalizeLocal());
//        sun.setColor(ColorRGBA.Gray);
//        rootNode.addLight(sun);


        cam.setLocation(new Vector3f(0, 2, 0));
        cam.lookAtDirection(Vector3f.UNIT_Z.negate(), Vector3f.UNIT_Y);
//        cam.lookAtDirection(new Vector3f(-0.30149722f, 0.04880875f, -0.952217f), Vector3f.UNIT_Y);
        cam.setFrustumPerspective(45.0f, cam.getWidth() * 1.0f / cam.getHeight(), 0.1f, 100.0f);
        flyCam.setMoveSpeed(10.0f);
//        flyCam.setEnabled(false);


        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        int numSamples = context.getSettings().getSamples();
        if (numSamples > 0) {
            fpp.setNumSamples(numSamples);
        }

        BloomFilter bloom=new BloomFilter();
        bloom.setDownSamplingFactor(1);
        bloom.setBlurScale(1.1f);
        bloom.setExposurePower(1.30f);
        bloom.setExposureCutOff(0.3f);
        bloom.setBloomIntensity(1.15f);
        fpp.addFilter(bloom);

        fpp.addFilter(new ToneMapFilter(Vector3f.UNIT_XYZ.mult(2.5f)));
        viewPort.addProcessor(fpp);
    }

    public static void main(String[] args) {
        TestTileBasedDeferredShading testTileBasedDeferredShading = new TestTileBasedDeferredShading();
        AppSettings appSettings = new AppSettings(true);
        appSettings.setWidth(1600);
        appSettings.setHeight(900);
//        appSettings.setRenderer(AppSettings.LWJGL_OPENGL33);
        testTileBasedDeferredShading.setSettings(appSettings);
        testTileBasedDeferredShading.start();
    }
}
