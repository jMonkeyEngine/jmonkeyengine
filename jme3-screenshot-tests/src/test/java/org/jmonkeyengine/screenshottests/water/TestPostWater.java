package org.jmonkeyengine.screenshottests.water;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;
import com.jme3.water.WaterFilter;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Test;

public class TestPostWater extends ScreenshotTestBase{

    /**
     * This test creates a scene with a terrain and post process water filter.
     */
    @Test
    public void testPostWater(){
       screenshotTest(new BaseAppState(){
           @Override
            protected void initialize(Application app){
                Vector3f lightDir = new Vector3f(-4.9236743f, -1.27054665f, 5.896916f);
                SimpleApplication simpleApplication = ((SimpleApplication)app);
                Node rootNode = simpleApplication.getRootNode();
                AssetManager assetManager = simpleApplication.getAssetManager();

                Node mainScene = new Node("Main Scene");
                rootNode.attachChild(mainScene);

                createTerrain(mainScene, assetManager);
                DirectionalLight sun = new DirectionalLight();
                sun.setDirection(lightDir);
                sun.setColor(ColorRGBA.White.clone().multLocal(1f));
                mainScene.addLight(sun);

                AmbientLight al = new AmbientLight();
                al.setColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
                mainScene.addLight(al);


                simpleApplication.getCamera().setLocation(new Vector3f(-370.31592f, 182.04016f, 196.81192f));
                simpleApplication.getCamera().setRotation(new Quaternion(0.10058216f, 0.51807004f, -0.061508257f, 0.8471738f));

                Spatial sky = SkyFactory.createSky(assetManager,
                        "Scenes/Beach/FullskiesSunset0068.dds", EnvMapType.CubeMap);
                sky.setLocalScale(350);

                mainScene.attachChild(sky);
                simpleApplication.getCamera().setFrustumFar(4000);

                //Water Filter
                WaterFilter water = new WaterFilter(rootNode, lightDir);
                water.setWaterColor(new ColorRGBA().setAsSrgb(0.0078f, 0.3176f, 0.5f, 1.0f));
                water.setDeepWaterColor(new ColorRGBA().setAsSrgb(0.0039f, 0.00196f, 0.145f, 1.0f));
                water.setUnderWaterFogDistance(80);
                water.setWaterTransparency(0.12f);
                water.setFoamIntensity(0.4f);
                water.setFoamHardness(0.3f);
                water.setFoamExistence(new Vector3f(0.8f, 8f, 1f));
                water.setReflectionDisplace(50);
                water.setRefractionConstant(0.25f);
                water.setColorExtinction(new Vector3f(30, 50, 70));
                water.setCausticsIntensity(0.4f);
                water.setWaveScale(0.003f);
                water.setMaxAmplitude(2f);
                water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg"));
                water.setRefractionStrength(0.2f);
                //0.8f;
                float initialWaterHeight = 90f;
                water.setWaterHeight(initialWaterHeight);

                //Bloom Filter
                BloomFilter bloom = new BloomFilter();
                bloom.setExposurePower(55);
                bloom.setBloomIntensity(1.0f);

                //Light Scattering Filter
                LightScatteringFilter lsf = new LightScatteringFilter(lightDir.mult(-300));
                lsf.setLightDensity(0.5f);

                //Depth of field Filter
                DepthOfFieldFilter dof = new DepthOfFieldFilter();
                dof.setFocusDistance(0);
                dof.setFocusRange(100);

                FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

                fpp.addFilter(water);
                fpp.addFilter(bloom);
                fpp.addFilter(dof);
                fpp.addFilter(lsf);
                fpp.addFilter(new FXAAFilter());

                int numSamples = simpleApplication.getContext().getSettings().getSamples();
                if (numSamples > 0) {
                    fpp.setNumSamples(numSamples);
                }
                simpleApplication.getViewPort().addProcessor(fpp);
            }

            @Override protected void cleanup(Application app){}

            @Override protected void onEnable(){}

            @Override protected void onDisable(){}

            @Override
            public void update(float tpf){
                super.update(tpf);
            }

            private void createTerrain(Node rootNode, AssetManager assetManager) {
                Material matRock = new Material(assetManager,
                        "Common/MatDefs/Terrain/TerrainLighting.j3md");
                matRock.setBoolean("useTriPlanarMapping", false);
                matRock.setBoolean("WardIso", true);
                matRock.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));
                Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/mountains512.png");
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
                matRock.setTexture("NormalMap_1", normalMap1);
                matRock.setTexture("NormalMap_2", normalMap2);

                AbstractHeightMap heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.25f);
                heightmap.load();

                TerrainQuad terrain
                        = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());
                terrain.setMaterial(matRock);
                terrain.setLocalScale(new Vector3f(5, 5, 5));
                terrain.setLocalTranslation(new Vector3f(0, -30, 0));
                terrain.setLocked(false); // unlock it so we can edit the height

                terrain.setShadowMode(ShadowMode.Receive);
                rootNode.attachChild(terrain);
            }
        }).run();
    }

}