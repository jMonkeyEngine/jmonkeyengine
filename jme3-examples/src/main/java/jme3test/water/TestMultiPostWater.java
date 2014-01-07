package jme3test.water;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * test
 *
 * @author normenhansen
 */
public class TestMultiPostWater extends SimpleApplication {

    private Vector3f lightDir = new Vector3f(-4.9236743f, -1.27054665f, 5.896916f);
    private WaterFilter water;
    private TerrainQuad terrain;
    private Material matRock;    
    private static float WATER_HEIGHT = 90;

    public static void main(String[] args) {
        TestMultiPostWater app = new TestMultiPostWater();
        AppSettings s = new AppSettings(true);
        s.setRenderer(AppSettings.LWJGL_OPENGL2);
        s.setAudioRenderer(AppSettings.LWJGL_OPENAL);
//       
//        s.setRenderer("JOGL");
//        s.setAudioRenderer("JOAL");
        app.setSettings(s);

        app.start();
    }

    @Override
    public void simpleInitApp() {

//      setDisplayFps(false);
//      setDisplayStatView(false);

        Node mainScene = new Node("Main Scene");
        rootNode.attachChild(mainScene);

        createTerrain(mainScene);
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(lightDir);
        sun.setColor(ColorRGBA.White.clone().multLocal(1.7f));
        mainScene.addLight(sun);

        flyCam.setMoveSpeed(100);

        //cam.setLocation(new Vector3f(-700, 100, 300));
        //cam.setRotation(new Quaternion().fromAngleAxis(0.5f, Vector3f.UNIT_Z));
        cam.setLocation(new Vector3f(-327.21957f, 251.6459f, 126.884346f));
        cam.setRotation(new Quaternion().fromAngles(new float[]{FastMath.PI * 0.06f, FastMath.PI * 0.65f, 0}));


        Spatial sky = SkyFactory.createSky(assetManager, "Scenes/Beach/FullskiesSunset0068.dds", false);
        sky.setLocalScale(350);

        mainScene.attachChild(sky);
        cam.setFrustumFar(4000);



        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        water = new WaterFilter(rootNode, lightDir);
        water.setCenter(new Vector3f(9.628218f, -15.830074f, 199.23595f));
        water.setRadius(260);
        water.setWaveScale(0.003f);
        water.setMaxAmplitude(2f);
        water.setFoamExistence(new Vector3f(1f, 4, 0.5f));
        water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg"));
        water.setRefractionStrength(0.2f);
        water.setWaterHeight(WATER_HEIGHT);
        fpp.addFilter(water);

        WaterFilter water2 = new WaterFilter(rootNode, lightDir);
        water2.setCenter(new Vector3f(-280.46027f, -24.971727f, -271.71976f));
        water2.setRadius(260);
        water2.setWaterHeight(WATER_HEIGHT);
        water2.setUseFoam(false);
        water2.setUseRipples(false);
        water2.setDeepWaterColor(ColorRGBA.Brown);
        water2.setWaterColor(ColorRGBA.Brown.mult(2.0f));
        water2.setWaterTransparency(0.2f);
        water2.setMaxAmplitude(0.3f);
        water2.setWaveScale(0.008f);
        water2.setSpeed(0.7f);
        water2.setShoreHardness(1.0f);
        water2.setRefractionConstant(0.2f);
        water2.setShininess(0.3f);
        water2.setSunScale(1.0f);
        water2.setColorExtinction(new Vector3f(10.0f, 20.0f, 30.0f));
        fpp.addFilter(water2);


        WaterFilter water3 = new WaterFilter(rootNode, lightDir);
        water3.setCenter(new Vector3f(319.6663f, -18.367947f, -236.67674f));
        water3.setRadius(260);
        water3.setWaterHeight(WATER_HEIGHT);
        water3.setWaveScale(0.003f);
        water3.setMaxAmplitude(2f);
        water3.setFoamExistence(new Vector3f(1f, 4, 0.5f));
        water3.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg"));
        water3.setRefractionStrength(0.2f);
        water3.setDeepWaterColor(ColorRGBA.Red);
        water3.setWaterColor(ColorRGBA.Red.mult(2.0f));
        water3.setLightColor(ColorRGBA.Red);
        fpp.addFilter(water3);

        viewPort.addProcessor(fpp);

        //fpp.setNumSamples(4);
    }

    private void createTerrain(Node rootNode) {
        matRock = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matRock.setBoolean("useTriPlanarMapping", false);
        matRock.setBoolean("WardIso", true);
        matRock.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/pools.png");
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matRock.setTexture("DiffuseMap", grass);
        matRock.setFloat("DiffuseMap_0_scale", 64);
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        matRock.setTexture("DiffuseMap_1", dirt);
        matRock.setFloat("DiffuseMap_1_scale", 16);
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        matRock.setTexture("DiffuseMap_2", rock);
        matRock.setFloat("DiffuseMap_2_scale", 128);
        Texture normalMap0 = assetManager.loadTexture("Textures/Terrain/splat/grass_normal.jpg");
        normalMap0.setWrap(WrapMode.Repeat);
        Texture normalMap1 = assetManager.loadTexture("Textures/Terrain/splat/dirt_normal.png");
        normalMap1.setWrap(WrapMode.Repeat);
        Texture normalMap2 = assetManager.loadTexture("Textures/Terrain/splat/road_normal.png");
        normalMap2.setWrap(WrapMode.Repeat);
        matRock.setTexture("NormalMap", normalMap0);
        matRock.setTexture("NormalMap_1", normalMap2);
        matRock.setTexture("NormalMap_2", normalMap2);

        AbstractHeightMap heightmap = null;
        try {
            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.25f);
            heightmap.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
        terrain = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());
        List<Camera> cameras = new ArrayList<Camera>();
        cameras.add(getCamera());
        terrain.setMaterial(matRock);
        terrain.setLocalScale(new Vector3f(5, 5, 5));
        terrain.setLocalTranslation(new Vector3f(0, -30, 0));
        terrain.setLocked(false); // unlock it so we can edit the height

        terrain.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(terrain);

    }

    @Override
    public void simpleUpdate(float tpf) {
    }
}
