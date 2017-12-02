package jme3test.water;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.LowPassFilter;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
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
public class TestPostWater extends SimpleApplication {

    private Vector3f lightDir = new Vector3f(-4.9236743f, -1.27054665f, 5.896916f);
    private WaterFilter water;
    TerrainQuad terrain;
    Material matRock;
    AudioNode waves;
    LowPassFilter underWaterAudioFilter = new LowPassFilter(0.5f, 0.1f);
    LowPassFilter underWaterReverbFilter = new LowPassFilter(0.5f, 0.1f);
    LowPassFilter aboveWaterAudioFilter = new LowPassFilter(1, 1);

    public static void main(String[] args) {
        TestPostWater app = new TestPostWater();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        setDisplayFps(false);
        setDisplayStatView(false);
        
        Node mainScene = new Node("Main Scene");
        rootNode.attachChild(mainScene);

        createTerrain(mainScene);
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(lightDir);
        sun.setColor(ColorRGBA.White.clone().multLocal(1f));
        mainScene.addLight(sun);
        
        AmbientLight al = new AmbientLight();
        al.setColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
        mainScene.addLight(al);
        
        flyCam.setMoveSpeed(50);

        //cam.setLocation(new Vector3f(-700, 100, 300));
        //cam.setRotation(new Quaternion().fromAngleAxis(0.5f, Vector3f.UNIT_Z));
//        cam.setLocation(new Vector3f(-327.21957f, 61.6459f, 126.884346f));
//        cam.setRotation(new Quaternion(0.052168474f, 0.9443102f, -0.18395276f, 0.2678024f));


        cam.setLocation(new Vector3f(-370.31592f, 182.04016f, 196.81192f));
        cam.setRotation(new Quaternion(0.015302252f, 0.9304095f, -0.039101653f, 0.3641086f));




        Spatial sky = SkyFactory.createSky(assetManager, "Scenes/Beach/FullskiesSunset0068.dds", false);
        sky.setLocalScale(350);

        mainScene.attachChild(sky);
        cam.setFrustumFar(4000);

        //Water Filter
        water = new WaterFilter(rootNode, lightDir);
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
        
//      fpp.addFilter(new TranslucentBucketFilter());
        int numSamples = getContext().getSettings().getSamples();
        if (numSamples > 0) {
            fpp.setNumSamples(numSamples);
        }

        
        uw = cam.getLocation().y < waterHeight;

        waves = new AudioNode(assetManager, "Sound/Environment/Ocean Waves.ogg", false);
        waves.setLooping(true);
        waves.setReverbEnabled(true);
        if (uw) {
            waves.setDryFilter(new LowPassFilter(0.5f, 0.1f));
        } else {
            waves.setDryFilter(aboveWaterAudioFilter);
        }
        audioRenderer.playSource(waves);
        //  
        viewPort.addProcessor(fpp);

        inputManager.addListener(new ActionListener() {
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    if (name.equals("foam1")) {
                        water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam.jpg"));
                    }
                    if (name.equals("foam2")) {
                        water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg"));
                    }
                    if (name.equals("foam3")) {
                        water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam3.jpg"));
                    }

                    if (name.equals("upRM")) {
                        water.setReflectionMapSize(Math.min(water.getReflectionMapSize() * 2, 4096));
                        System.out.println("Reflection map size : " + water.getReflectionMapSize());
                    }
                    if (name.equals("downRM")) {
                        water.setReflectionMapSize(Math.max(water.getReflectionMapSize() / 2, 32));
                        System.out.println("Reflection map size : " + water.getReflectionMapSize());
                    }
                }
            }
        }, "foam1", "foam2", "foam3", "upRM", "downRM");
        inputManager.addMapping("foam1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("foam2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("foam3", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("upRM", new KeyTrigger(KeyInput.KEY_PGUP));
        inputManager.addMapping("downRM", new KeyTrigger(KeyInput.KEY_PGDN));
//        createBox();
//        createFire();
    }
    Geometry box;

    private void createBox() {
        //creating a transluscent box
        box = new Geometry("box", new Box(50, 50, 50));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(1.0f, 0, 0, 0.3f));
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        //mat.getAdditionalRenderState().setDepthWrite(false);
        //mat.getAdditionalRenderState().setDepthTest(false);
        box.setMaterial(mat);
        box.setQueueBucket(Bucket.Translucent);


        //creating a post view port
//        ViewPort post=renderManager.createPostView("transpPost", cam);
//        post.setClearFlags(false, true, true);


        box.setLocalTranslation(-600, 0, 300);

        //attaching the box to the post viewport
        //Don't forget to updateGeometricState() the box in the simpleUpdate
        //  post.attachScene(box);

        rootNode.attachChild(box);
    }

    private void createFire() {
        /**
         * Uses Texture from jme3-test-data library!
         */
        ParticleEmitter fire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
        Material mat_red = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat_red.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));

        fire.setMaterial(mat_red);
        fire.setImagesX(2);
        fire.setImagesY(2); // 2x2 texture animation
        fire.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f));   // red
        fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
        fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
        fire.setStartSize(10f);
        fire.setEndSize(1f);
        fire.setGravity(0, 0, 0);
        fire.setLowLife(0.5f);
        fire.setHighLife(1.5f);
        fire.getParticleInfluencer().setVelocityVariation(0.3f);
        fire.setLocalTranslation(-600, 50, 300);

        fire.setQueueBucket(Bucket.Translucent);
        rootNode.attachChild(fire);
    }

    private void createTerrain(Node rootNode) {
        matRock = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
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
    //This part is to emulate tides, slightly varrying the height of the water plane
    private float time = 0.0f;
    private float waterHeight = 0.0f;
    private float initialWaterHeight = 90f;//0.8f;
    private boolean uw = false;

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        //     box.updateGeometricState();
        time += tpf;
        waterHeight = (float) Math.cos(((time * 0.6f) % FastMath.TWO_PI)) * 1.5f;
        water.setWaterHeight(initialWaterHeight + waterHeight);
        if (water.isUnderWater() && !uw) {

            waves.setDryFilter(new LowPassFilter(0.5f, 0.1f));
            uw = true;
        }
        if (!water.isUnderWater() && uw) {
            uw = false;
            //waves.setReverbEnabled(false);
            waves.setDryFilter(new LowPassFilter(1, 1f));
            //waves.setDryFilter(new LowPassFilter(1,1f));

        }
    }
}
