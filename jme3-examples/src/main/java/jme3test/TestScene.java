/*
 * Copyright (c) 2025 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3test;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.input.FlyByCamera;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.BloomFilter.GlowMode;
import com.jme3.post.filters.ContrastAdjustmentFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Limits;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphIterator;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.gltf.GltfModelKey;
import com.jme3.scene.shape.RectangleMesh;
import com.jme3.shadow.AbstractShadowFilter;
import com.jme3.shadow.AbstractShadowRenderer;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads a configurable PBR test scene.
 *
 * @author codex
 */
public class TestScene extends Node {

    /**
     * Shadow modes used in the scene.
     */
    public enum Shadows {

        /**
         * No shadows.
         */
        None,

        /**
         * Use an {@link AbstractShadowRenderer} to render shadows.
         */
        Renderer,

        /**
         * Use an {@link AbstractShadowFilter} to render shadows.
         */
        Filter;

    }

    /**
     * Bloom modes used in the scene.
     */
    public enum Bloom {

        /**
         * No bloom.
         */
        None(null),

        /**
         * Use {@link SoftBloomFilter} to add a glow effect to the entire scene.
         * <p>
         * This setting is currently unsupported.
         */
        SoftScene(null),

        /**
         * Use {@link BloomFilter} to add a glow effect to the entire scene.
         * @see GlowMode#Scene
         */
        Scene(GlowMode.Scene),

        /**
         * Use {@link BloomFilter} to add a glow effect to the scene and specific objects.
         * @see GlowMode#SceneAndObjects
         */
        SceneAndObjects(GlowMode.SceneAndObjects),

        /**
         * Use {@link BloomFilter} to add a glow effect to specific objects.
         * @see GlowMode#Objects
         */
        Objects(GlowMode.Objects);

        private final GlowMode equivalent;
        private Bloom(GlowMode equivalent) {
            this.equivalent = equivalent;
        }

        /**
         * Gets the {@link GlowMode} equivalent, or null if none exists.
         *
         * @return GlowMode equivalent
         */
        public GlowMode getEquivalent() {
            return equivalent;
        }

        /**
         * Returns true if glow is made using {@link SoftBloomFilter}.
         *
         * @return
         */
        public boolean usesSoftBloom() {
            return equivalent == null;
        }

    }

    private static final String[] SKY_CUBE_FACES = {"west", "east", "north", "south", "up", "down"};

    /**
     * Represents a probe created at runtime by the GPU.
     */
    public static final String HARDWARE_PROBE = "HARDWARE_LIGHT_PROBE";

    /**
     * Represents a subscene meant for testing character movement.
     */
    public static final String CHARACTER_SUBSCENE = "Scenes/TestScene/character.gltf";

    /**
     * Represents a subscene meant for testing physics.
     */
    public static final String PHYSICS_SUBSCENE = "Scenes/TestScene/physics.gltf";

    /**
     * Represents a single sky texture used to create a skybox.
     */
    public static final String BRIGHT_SKY = "Textures/Sky/Bright/BrightSky.dds";

    /**
     * Represents 6 sky textures used to create a skybox.
     * <p>
     * The "multi:" prefix indicates that 6 textures will be used. On load,
     * the "$" is replaced by "west", "east", "north", "south", "up", and "down"
     * for each of 6 texture loads to create a texture for each orthogonal direction.
     */
    public static final String LAGOON_SKY = "Textures/Sky/Lagoon/lagoon_{0}.jpg";

    /**
     * Represents grey grid texture.
     */
    public static final String GRAY_TEXTURE = "Scenes/TestScene/grid-grey.png";

    /**
     * Represents grey grid texture.
     */
    public static final String BLUE_TEXTURE = "Scenes/TestScene/grid-blue.png";

    /**
     * Represents grey grid texture.
     */
    public static final String RED_TEXTURE = "Scenes/TestScene/grid-red.png";

    /**
     * Represents grey grid texture.
     */
    public static final String GREEN_TEXTURE = "Scenes/TestScene/grid-green.png";

    /**
     * Represents grey grid texture.
     */
    public static final String YELLOW_TEXTURE = "Scenes/TestScene/grid-yellow.png";

    /**
     * Represents grey grid texture.
     */
    public static final String PURPLE_TEXTURE = "Scenes/TestScene/grid-purple.png";

    /**
     * Represents the size of each tile.
     * <p>
     * The Y axis parameter is set to zero, as it is not used.
     */
    public static final Vector3f TILE_SIZE = new Vector3f(20, 0, 20);

    /**
     * Default background color for the scene.
     * <p>
     * Apply this color to the viewport by calling {@link #configureBackgroundColor()}.
     */
    public static final ColorRGBA SCENE_BACKGROUND_COLOR = new ColorRGBA(.6f, .7f, 1f, 1f);

    private static final String BASE_SCENE = "Scenes/TestScene/base-scene.gltf";
    private static final String MULTI_TEXTURE = "multi:";
    private static final float SHADOW_INTENSITY = .5f;
    private static final Logger logger = Logger.getLogger(TestScene.class.getName());

    private final Application app;
    private final AssetManager assetManager;
    private final LinkedList<RigidBodyControl> rigidBodyList = new LinkedList<>();
    private boolean loaded = false;

    private int width = 3;
    private int height = 3;
    private boolean sceneGeometry = true;
    private boolean walls = true;
    private boolean lights = true;
    private String subSceneAsset;
    private boolean filters = true;
    private FilterPostProcessor fpp;
    private PhysicsSpace space;
    private boolean boundaries = true;
    private int shadowRes = 2048;
    private int sunSplits = 4;
    private Shadows shadows = Shadows.Renderer;
    private boolean occlusion = true;
    private Bloom bloom = Bloom.None;
    private String lightProbeAsset = HARDWARE_PROBE;
    private int hardwareProbeRes = 256;
    private String skyAsset = null;

    private Node subScene;
    private Spatial sky;
    private DirectionalLight sun;
    private DirectionalLight atmosphere;
    private AmbientLight ambient;
    private DirectionalLightShadowRenderer shadowRenderer;
    private DirectionalLightShadowFilter shadowFilter;
    private SSAOFilter ssaoFilter;
    private BloomFilter bloomFilter;
    //private SoftBloomFilter softBloomFilter;
    private EnvironmentProbeControl hardwareProbe;
    private LightProbe lightProbe;

    public TestScene(Application app) {
        this.app = app;
        this.assetManager = this.app.getAssetManager();
    }

    /**
     * Launches a dedicated test application for this class.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        TestApp.start(args);
    }

    public TestScene baseScene() {
        return baseScene(3, 3);
    }
    public TestScene baseScene(int width, int height) {
        GltfModelKey key = new GltfModelKey(BASE_SCENE);
        Node base = (Node)assetManager.loadModel(key);
        Node terrainSrc = (Node)base.getChild("Terrain");
        base.detachChild(terrainSrc);
        Vector3f offset = new Vector3f(-0.5f*(width-1)*TILE_SIZE.x, 0, -0.5f*(height-1)*TILE_SIZE.z);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Spatial tile = null;
                // note: coordinates are reversed: N -> S, E -> W
                if (walls && width > 1 && height > 1) {
                    if (i == 0 && j == 0) {
                        // south-east
                        tile = createTerrainTile(terrainSrc, "TerrainNW");
                    } else if (i == width-1 && j == 0) {
                        // south-west
                        tile = createTerrainTile(terrainSrc, "TerrainNE");
                    } else if (i == width-1 && j == height-1) {
                        // north-west
                        tile = createTerrainTile(terrainSrc, "TerrainSE");
                    } else if (i == 0 && j == height-1) {
                        // north-east
                        tile = createTerrainTile(terrainSrc, "TerrainSW");
                    } else if (i == 0) {
                        // east
                        tile = createTerrainTile(terrainSrc, "TerrainW");
                    } else if (i == width-1) {
                        // west
                        tile = createTerrainTile(terrainSrc, "TerrainE");
                    } else if (j == 0) {
                        // south
                        tile = createTerrainTile(terrainSrc, "TerrainN");
                    } else if (j == height-1) {
                        // north
                        tile = createTerrainTile(terrainSrc, "TerrainS");
                    }
                }
                if (tile == null) {
                    // center
                    tile = createTerrainTile(terrainSrc, "TerrainC");
                }
                tile.setLocalTranslation(TILE_SIZE.x*i+offset.x, offset.y, TILE_SIZE.z*j+offset.z);
                if (!boundaries) {
                    tile.setUserData("Mass", 0.0);
                }
                base.attachChild(tile);
            }
        }
        EnvironmentProbeControl.tagGlobal(base);
        base.setName("BaseScene");
        attachChild(base);
        return this;
    }
    public TestScene lighting() {
        return lighting(new Vector3f(.5f, -1, .2f), new ColorRGBA(1f, 1f, .9f, 1f).multLocal(1.5f));
    }
    public TestScene lighting(Vector3f direction, ColorRGBA color) {
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(direction);
        sun.setColor(color);
        addLight(sun);
        return this;
    }
    public TestScene ambient() {
        return ambient(128);
    }
    public TestScene ambient(int envMapSize) {
        addControl(new EnvironmentProbeControl(assetManager, envMapSize));
        return this;
    }
    public TestScene sky(String texture, SkyFactory.EnvMapType type) {
        Spatial sky = SkyFactory.createSky(assetManager, texture, type);
        EnvironmentProbeControl.tagGlobal(sky);
        attachChild(sky);
        return this;
    }
    public TestScene skyCube(String textureFormat) {
        return skyCube(textureFormat, SKY_CUBE_FACES);
    }
    public TestScene skyCube(String textureFormat, String... faces) {
        assert faces.length == 6 : "Number of faces must be six.";
        Texture west = assetManager.loadTexture(String.format(textureFormat, faces[0]));
        Texture east = assetManager.loadTexture(String.format(textureFormat, faces[1]));
        Texture north = assetManager.loadTexture(String.format(textureFormat, faces[2]));
        Texture south = assetManager.loadTexture(String.format(textureFormat, faces[3]));
        Texture up = assetManager.loadTexture(String.format(textureFormat, faces[4]));
        Texture down = assetManager.loadTexture(String.format(textureFormat, faces[5]));
        Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        EnvironmentProbeControl.tagGlobal(sky);
        attachChild(sky);
        return this;
    }
    public TestScene brightMountainsSky() {
        return sky("Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.SphereMap);
    }
    public TestScene lagoonSky() {
        return skyCube("Textures/Sky/Lagoon/lagoon_{0}.jpg");
    }
    public TestScene bloom(Bloom bloom) {
        FilterPostProcessor fpp = getOrCreateFpp();
        if (bloom.usesSoftBloom()) {

        }
    }

    private FilterPostProcessor getOrCreateFpp() {
        ViewPort vp = app.getViewPort();
        for (SceneProcessor p : vp.getProcessors()) {
            if (p instanceof FilterPostProcessor) {
                return (FilterPostProcessor)p;
            }
        }
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        vp.addProcessor(fpp);
        return fpp;
    }

    /**
     * Loads the scene.
     *
     * @return this instance
     */
    public TestScene load() {
        if (loaded) {
            return this;
        }
        try {

            setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

            // scene
            if (sceneGeometry) {
                if (subSceneAsset != null) {
                    logger.info("Loading subscene.");
                    loadSubScene(subSceneAsset);
                }
                logger.info("Loading main scene.");
                EnvironmentProbeControl.tagGlobal(baseScene());
                if (boundaries) {
                    logger.info("Loading boundary meshes.");
                    loadBoundaries();
                }
            }
            if (skyAsset != null) {
                loadSky();
            }

            // lights
            if (lights) {
                logger.info("Loading lights (3).");
                sun = new DirectionalLight();
                sun.setDirection(new Vector3f(.5f, -1, .2f));
                sun.setColor(new ColorRGBA(1f, 1f, .9f, 1f).multLocal(1.5f));
                addLight(sun);
                atmosphere = new DirectionalLight();
                atmosphere.setDirection(new Vector3f(-1, 1, 0));
                atmosphere.setColor(ColorRGBA.White.mult(.3f));
                //addLight(atmosphere);
                ambient = new AmbientLight();
                ambient.setColor(ColorRGBA.White.mult(.8f));
                addLight(ambient);
                PointLight pl = new PointLight(new Vector3f(TILE_SIZE.x*0.5f*width-1, 5, TILE_SIZE.x*0.5f*height-1));
                pl.setColor(ColorRGBA.White.mult(.9f));
                //addLight(pl);
            }

            // filters
            if (filters) {
                boolean makeFpp = (fpp == null);
                if (makeFpp) {
                    logger.info("No FilterPostProcessor specified. Creating new instance.");
                    fpp = new FilterPostProcessor(assetManager);
                }
                ContrastAdjustmentFilter contrast = new ContrastAdjustmentFilter();
                contrast.setExponents(1.6f, 1.6f, 1.6f);
                contrast.setLowerLimit(-0.01f);
                contrast.setUpperLimit(.9f);
                fpp.addFilter(contrast);
                FXAAFilter fxaa = new FXAAFilter();
                fpp.addFilter(fxaa);
                if (lights) {
                    if (shadows == Shadows.Renderer) {
                        logger.info("Loading shadow renderer.");
                        shadowRenderer = new DirectionalLightShadowRenderer(assetManager, shadowRes, sunSplits);
                        shadowRenderer.setLight(sun);
                        shadowRenderer.setRenderBackFacesShadows(false);
                        shadowRenderer.setShadowIntensity(SHADOW_INTENSITY);
                        shadowRenderer.setShadowCompareMode(CompareMode.Hardware);
                        viewPort.addProcessor(shadowRenderer);
                    } else if (shadows == Shadows.Filter) {
                        logger.info("Loading shadow filter.");
                        shadowFilter = new DirectionalLightShadowFilter(assetManager, shadowRes, sunSplits);
                        shadowFilter.setLight(sun);
                        shadowFilter.setShadowIntensity(SHADOW_INTENSITY);
                        shadowFilter.setRenderBackFacesShadows(false);
                        shadowFilter.setShadowCompareMode(CompareMode.Hardware);
                        fpp.addFilter(shadowFilter);
                    }
                }
                if (occlusion) {
                    logger.info("Loading screenspace ambient occlusion filter.");
                    ssaoFilter = new SSAOFilter();
                    //ssaoFilter.setIntensity(2.5f);
                    //ssaoFilter.setSampleRadius(1);
                    //ssaoFilter.setScale(.1f);
                    fpp.addFilter(ssaoFilter);
                }
                if (bloom != Bloom.None) {
                    if (!bloom.usesSoftBloom()) {
                        logger.log(Level.INFO, "Loading bloom filter. GlowMode={0}", bloom.getEquivalent().toString());
                        bloomFilter = new BloomFilter(bloom.getEquivalent());
                        fpp.addFilter(bloomFilter);
                    } else {
                        //softBloomFilter = new SoftBloomFilter();
                        //fpp.addFilter(softBloomFilter);
                        logger.warning("SoftScene bloom is currently unsupported.");
                    }
                }
                if (makeFpp) {
                    if (!fpp.getFilterList().isEmpty()) {
                        viewPort.addProcessor(fpp);
                    } else {
                        logger.info("No filters created. Deleting new FilterPostProcessor.");
                        fpp = null;
                    }
                }
            }

            // light probes
            if (lightProbeAsset != null) {
                if (lightProbeAsset.equals(HARDWARE_PROBE)) {
                    logger.info("Loading hardware light probe.");
                    hardwareProbe = new EnvironmentProbeControl(assetManager, hardwareProbeRes);
                    addControl(hardwareProbe);
                } else {
                    logger.info("Loading light probe from asset.");
                    try {
                        Spatial m = assetManager.loadModel(lightProbeAsset);
                        lightProbe = (LightProbe)m.getLocalLightList().get(0);
                        addLight(lightProbe);
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Failed to load light probe from \""+lightProbeAsset+"\"", ex);
                    }
                }
            }

            // remove spatials
            LinkedList<Spatial> remove = new LinkedList<>();
            for (Spatial s : new SceneGraphIterator(this)) {
                if (s.getUserData("Remove") != null) {
                    remove.add(s);
                }
            }
            for (Spatial s : remove) {
                s.removeFromParent();
            }
            if (!remove.isEmpty()) {
                logger.log(Level.INFO, "{0} spatials removed from scene by userdata.", remove.size());
            }

            // physics
            if (space != null) {
                for (SceneGraphIterator it = new SceneGraphIterator(this); it.hasNext();) {
                    Spatial s = it.next();
                    Double mass = s.getUserData("Mass");
                    if (mass != null && mass >= 0) {
                        addRigidBody(s, mass.floatValue());
                        it.ignoreChildren();
                    }
                }
                logger.log(Level.INFO, "{0} rigid bodies created.", rigidBodyList.size());
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "An exception occured while loading test scene", ex);
        }
        loaded = true;
        return this;
    }

    /**
     * Dumps all resources for this scene.
     * <p>
     * After dumping, {@link #load()} can be called again.
     */
    public void dump() {
        clearRigidBodies();
        detachAllChildren();
        subScene = null;
        if (sun != null) {
            removeLight(sun);
            sun = null;
        }
        if (atmosphere != null) {
            removeLight(atmosphere);
            atmosphere = null;
        }
        if (ambient != null) {
            removeLight(ambient);
            ambient = null;
        }
        if (hardwareProbe != null) {
            removeLight(hardwareProbe);
            removeControl(hardwareProbe);
            hardwareProbe = null;
        }
        if (lightProbe != null) {
            removeLight(lightProbe);
            lightProbe = null;
        }
        if (fpp != null) {
            if (shadowFilter != null) {
                this.fpp.removeFilter(shadowFilter);
                shadowFilter = null;
            }
            if (ssaoFilter != null) {
                this.fpp.removeFilter(ssaoFilter);
                ssaoFilter = null;
            }
            if (bloomFilter != null) {
                this.fpp.removeFilter(bloomFilter);
                bloomFilter = null;
            }
//            if (softBloomFilter != null) {
//                this.fpp.removeFilter(softBloomFilter);
//                softBloomFilter = null;
//            }
        }
        if (shadowRenderer != null) {
            viewPort.removeProcessor(shadowRenderer);
            shadowRenderer = null;
        }
        loaded = false;
    }

    /**
     * Configures the viewport background to match the scene.
     */
    public void configureBackgroundColor() {
        viewPort.setBackgroundColor(SCENE_BACKGROUND_COLOR);
    }

    /**
     * Configures {@link FlyByCamera} to the scene.
     *
     * @param cam
     */
    public void configureFlyCamSpeed(FlyByCamera cam) {
        //cam.get.lookAt(new Vector3f(0, 2, 0), Vector3f.UNIT_Y);
        //cam.setLocation(new Vector3f(-10, 10, -10));
        cam.setMoveSpeed(15);
    }

    /**
     * Configures the {@link Camera} to the scene.
     *
     * @param cam
     */
    public void configureCameraPosition(Camera cam) {
        cam.setLocation(new Vector3f(-10, 10, -10));
        cam.lookAtDirection(new Vector3f(1, -1, 1), Vector3f.UNIT_Y);
    }

    /**
     * Configures the default texture anisotropy level to match the scene.
     * <p>
     * Anisotropy makes textures viewed at an oblique angle sharper.
     *
     * @param renderer
     */
    public void configureDefaultAnisotropy(Renderer renderer) {
        renderer.setDefaultAnisotropicFilter(Math.min(8, renderer.getLimits().get(Limits.TextureAnisotropy)));
    }

    private Spatial createTerrainTile(Node terrainSrc, String name) {
        int v = getInt(terrainSrc, name, -1);
        Spatial tile;
        if (v > 0) {
            v = FastMath.rand.nextInt(v);
            tile = terrainSrc.getChild(name+v).clone();
        } else {
            tile = terrainSrc.getChild(name).clone();
        }
        Texture color = assetManager.loadTexture(new TextureKey("Scenes/TestScene/grid-grey.png", false));
        color.setWrap(Texture.WrapMode.Repeat);
        color.setMinFilter(Texture.MinFilter.Trilinear);
        color.setMagFilter(Texture.MagFilter.Bilinear);
        color.setAnisotropicFilter(6);
        Texture light = assetManager.loadTexture(new TextureKey("Scenes/TestScene/lightmapAll.png", false));
        light.setWrap(Texture.WrapMode.Repeat);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setTexture("BaseColorMap", color);
        mat.setTexture("LightMap", light);
        mat.setFloat("Metallic", .3f);
        mat.setFloat("Roughness", .8f);
        mat.setBoolean("SeparateTexCoord", true);
        tile.setMaterial(mat);
        return tile;
    }

    private Node loadBoundaries() {
        float y = 40;
        float x = TILE_SIZE.x*width*0.5f;
        float z = TILE_SIZE.z*height*0.5f;
        Node n = new Node("Boundaries");
        Geometry[] bounds = {
                new Geometry("NorthBounds", new RectangleMesh(new Vector3f(x, 0, 0), new Vector3f(-x, 0, 0), new Vector3f(x, y*2, 0))),
                new Geometry("EastBounds", new RectangleMesh(new Vector3f(0, 0, z), new Vector3f(0, 0, -z), new Vector3f(0, y*2, z))),
                new Geometry("SouthBounds", new RectangleMesh(new Vector3f(-x, 0, 0), new Vector3f(x, 0, 0), new Vector3f(-x, y*2, 0))),
                new Geometry("WestBounds", new RectangleMesh(new Vector3f(0, 0, -z), new Vector3f(0, 0, z), new Vector3f(0, y*2, -z))),
                new Geometry("UpperBounds", new RectangleMesh(new Vector3f(-x, y, -z), new Vector3f(x, y, -z), new Vector3f(-x, y, z))),
                new Geometry("LowerBounds", new RectangleMesh(new Vector3f(x, 0, -z), new Vector3f(-x, 0, -z), new Vector3f(x, 0, z))),
        };
        bounds[0].setLocalTranslation(0, 0, z);
        bounds[1].setLocalTranslation(-x, 0, 0);
        bounds[2].setLocalTranslation(0, 0, -z);
        bounds[3].setLocalTranslation(x, 0, 0);
        bounds[4].setLocalTranslation(0, y, 0);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.BlackNoAlpha);
        for (Geometry g : bounds) {
            g.setMaterial(mat);
            g.setCullHint(Spatial.CullHint.Always);
            g.setUserData("Mass", 0.0);
            n.attachChild(g);
        }
        attachChild(n);
        return n;
    }

    private void loadSubScene(String assetPath) {
        try {
            subScene = (Node)assetManager.loadModel(assetPath);
            subScene.setName("SubScene");
            width = Math.max(width, getInt(subScene, "Width", 1));
            height = Math.max(height, getInt(subScene, "Height", 1));
            attachChild(subScene);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to load subscene from \"{0}\"", assetPath);
        }
    }

    private void loadSky() {
        try {
            if (!skyAsset.startsWith(MULTI_TEXTURE)) {
                logger.info("Loading sky from texture.");
                sky = SkyFactory.createSky(assetManager, skyAsset, SkyFactory.EnvMapType.CubeMap);
            } else {
                logger.info("Loading sky from multiple textures.");
                String a = skyAsset.substring(MULTI_TEXTURE.length());
                Texture west = assetManager.loadTexture(a.replace("$", "west"));
                Texture east = assetManager.loadTexture(a.replace("$", "east"));
                Texture north = assetManager.loadTexture(a.replace("$", "north"));
                Texture south = assetManager.loadTexture(a.replace("$", "south"));
                Texture up = assetManager.loadTexture(a.replace("$", "up"));
                Texture down = assetManager.loadTexture(a.replace("$", "down"));
                sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
            }
            EnvironmentProbeControl.tagGlobal(sky);
            attachChild(sky);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to load sky from \"{0}\"", skyAsset);
        }
    }

    private void addRigidBody(Spatial model, float mass) {
        RigidBodyControl rigidBody = new RigidBodyControl(mass);
        model.addControl(rigidBody);
        space.add(rigidBody);
        rigidBodyList.add(rigidBody);
    }

    private void clearRigidBodies() {
        for (PhysicsCollisionObject object : rigidBodyList) {
            space.remove(object);
        }
        rigidBodyList.clear();
    }

    private static int getInt(Spatial spatial, String name, int defaultValue) {
        Double value = spatial.getUserData(name);
        if (value != null) {
            return value.intValue();
        } else {
            return defaultValue;
        }
    }

    /**
     * Sets the width (X axis) of the scene in tiles.
     * <p>
     * A single tile is 20 units in width. If the subscene requires a width greater
     * than what is set, the width will be changed.
     * <p>
     * default=3
     *
     * @param width number of tiles in width
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Sets the height (Z axis) of the scene in tiles.
     * <p>
     * A single tile is 20 units in height. If the subscene requires a height greater
     * than what is set, the height will be changed.
     * <p>
     * default=3
     *
     * @param height number of tiles in height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Sets the width and height of the scene in tiles.
     *
     * @param width
     * @param height
     * @see #setWidth(int)
     * @see #setHeight(int)
     */
    public void setMapSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    /**
     * Enables visible scene geometry.
     * <p>
     * If not enabled, no scene geometry will be loaded, except the skybox.
     * <p>
     * default=true
     *
     * @param sceneGeometry
     */
    public void setEnableSceneGeometry(boolean sceneGeometry) {
        this.sceneGeometry = sceneGeometry;
    }

    /**
     * Enables visible walls along the border of the scene.
     * <p>
     * If disabled, the scene will be the equivalent of a plane.
     * <p>
     * default=true
     *
     * @param walls
     */
    public void setEnableWalls(boolean walls) {
        this.walls = walls;
    }

    /**
     * Sets the subscene asset path.
     * <p>
     * The subscene is loaded on top of the main scene. This allows TestScene
     * to be used for a variety of applications and tests.
     * <p>
     * The subscene can specify the minimum width and height (in tiles) of the scene
     * by assigning userdata ("Width" and "Height" keys, respectively) to the subscene
     * root. This can be done in Blender by assigning custom properties to the scene
     * (properties -> scene -> custom properties).
     * <p>
     * Any spatial in the subscene that contains a "Remove" userdata entry will not
     * be included in the overall scene. Any spatial containing a double userdata entry
     * under "Mass" will be given a {@link RigidBodyControl} of the specified mass.
     * <p>
     * Any spatial in the subscene containing a "tag.env" (according to
     * {@link EnvironmentProbeControl}) userdata entry that is equal to {@code true}
     * will be included in environment map creation. These spatials should be static.
     * <p>
     * default=null (no subscene)
     *
     * @param subSceneAsset
     * @see #CHARACTER_SUBSCENE
     * @see #PHYSICS_SUBSCENE
     */
    public void setSubScene(String subSceneAsset) {
        this.subSceneAsset = subSceneAsset;
    }

    /**
     * Enables scene lights.
     * <p>
     * default=true
     *
     * @param lights
     */
    public void setEnableLights(boolean lights) {
        this.lights = lights;
    }

    /**
     * Enables post-processing filters.
     * <p>
     * If disabled, a {@link FilterPostProcessor} will not be created. This setting
     * also enables/disables shadows.
     * <p>
     * default=true
     *
     * @param filters
     */
    public void setEnableFilters(boolean filters) {
        this.filters = filters;
    }

    /**
     * Sets the {@link FilterPostProcessor} to use for filter effects.
     * <p>
     * The internal FilterPostProcessor cannot be set after load.
     * <p>
     * If null, a new FilterPostProcessor will be created on load if any filters are used.
     *
     * @param fpp
     */
    public void setFilterPostProcessor(FilterPostProcessor fpp) {
        if (!loaded) {
            this.fpp = fpp;
        }
    }

    /**
     * Sets the physics space.
     * <p>
     * If the physics space is not null, any spatials containing a "Mass" userdata
     * entry will become physical.
     * <p>
     * If the internal physics space is not null and null is passed, then all
     * {@link RigidBodyControl} (represented by {@link #getRigidBodyList()})
     * objects currently in the scene will be removed from the physics space.
     * <p>
     * default=null
     *
     * @param space
     */
    public void setPhysicsSpace(PhysicsSpace space) {
        if (this.space != null && space == null && loaded) {
            clearRigidBodies();
        }
        this.space = space;
    }

    /**
     * Enables boundaries created from 6 quads around the edge of the scene.
     * <p>
     * The resulting quads are always culled. If the physics space is not null,
     * the resulting quads will become physical and the visible terrain will
     * not become physical.
     * <p>
     * default=true
     *
     * @param boundaries
     */
    public void setEnableBoundaries(boolean boundaries) {
        this.boundaries = boundaries;
    }

    /**
     * Sets the resolution of shadow maps.
     * <p>
     * default=2048
     *
     * @param shadowRes
     */
    public void setShadowResolution(int shadowRes) {
        this.shadowRes = shadowRes;
    }

    /**
     * Sets the number of splits used for sunlight shadows.
     * <p>
     * default=4
     *
     * @param splits
     */
    public void setSunShadowSplits(int splits) {
        this.sunSplits = splits;
    }

    /**
     * Sets the shadow mode.
     *
     * @param shadows
     */
    public void setShadows(Shadows shadows) {
        this.shadows = shadows;
    }

    /**
     * Enables {@link SSAOFilter} for ambient occlusion effects.
     *
     * @param occlusion
     */
    public void setEnableOcclusion(boolean occlusion) {
        this.occlusion = occlusion;
    }

    /**
     * Sets the bloom mode.
     * <p>
     * <em>Note: {@link Bloom#SoftScene} is currently unsupported.</em>
     * <p>
     * default={@link Bloom#None}
     *
     * @param bloom
     */
    public void setBloom(Bloom bloom) {
        this.bloom = bloom;
    }

    /**
     * Sets the light probe asset path.
     * <p>
     * The light probe are expected to be the only light added to the main
     * node of the j3o scene.
     * <p>
     * If the light probe asset path is set to {@link #HARDWARE_PROBE}, then
     * {@link EnvironmentProbeControl} will be used to generate a light probe.
     * <p>
     * default={@link #HARDWARE_PROBE}
     *
     * @param lightProbeAsset
     */
    public void setLightProbe(String lightProbeAsset) {
        this.lightProbeAsset = lightProbeAsset;
    }

    /**
     * Sets the resolution of the probe generated with {@link EnvironmentProbeControl}.
     * <p>
     * This is only applicable if the value set by {@link #setLightProbe(java.lang.String)}
     * is equal to {@link #HARDWARE_PROBE}.
     * <p>
     * default=256
     *
     * @param probeRes
     */
    public void setHardwareProbeResolution(int probeRes) {
        hardwareProbeRes = probeRes;
    }

    /**
     * Sets the skybox asset path.
     * <p>
     * If the asset path begins with "multi:", then the path is assumed to reference 6
     * textures. Any "$" symbols detected in the asset path will be replaced with
     * west, east, north, south, up, and down (for each orthogonal direction).
     * <p>
     * default=null
     *
     * @param skyAsset
     * @see #BRIGHT_SKY
     * @see #LAGOON_SKY for an example of multiple textures
     */
    public void setSky(String skyAsset) {
        this.skyAsset = skyAsset;
    }

    /**
     * Gets the width (X axis) of the scene in tiles.
     *
     * @return
     * @see #setWidth(int)
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the height (Z axis) of the scene in tiles.
     *
     * @return
     * @see #setHeight(int)
     */
    public int getHeight() {
        return height;
    }

    /**
     * Get the width (X axis) of the scene.
     *
     * @return
     * @see #setWidth(int)
     */
    public float getSceneWidth() {
        return TILE_SIZE.x*width;
    }

    /**
     * Get the height (Z axis) of the scene.
     *
     * @return
     * @see #setHeight(int)
     */
    public float getSceneHeight() {
        return TILE_SIZE.z*height;
    }

    /**
     * Get the subscene.
     *
     * @return
     * @see #setSubScene(java.lang.String)
     */
    public Node getSubScene() {
        return subScene;
    }

    /**
     * Returns true if scene geometry is enabled.
     *
     * @return
     * @see #setEnableSceneGeometry(boolean)
     */
    public boolean isSceneGeometryEnabled() {
        return sceneGeometry;
    }

    /**
     * Returns true if lights are enabled.
     *
     * @return
     * @see #setEnableLights(boolean)
     */
    public boolean isLightsEnabled() {
        return lights;
    }

    /**
     * Returns true if filters are enabled.
     *
     * @return
     * @see #setEnableFilters(boolean)
     */
    public boolean isFiltersEnabled() {
        return filters;
    }

    /**
     * Returns true if boundary meshes are enabled.
     *
     * @return
     * @see #setEnableBoundaries(boolean)
     */
    public boolean isBoundariesEnabled() {
        return boundaries;
    }

    /**
     * Returns true if ambient occlusion is enabled.
     *
     * @return
     * @see #setEnableOcclusion(boolean)
     */
    public boolean isOcclusionEnabled() {
        return occlusion;
    }

    /**
     * Returns true if visible walls along the scene edge are enabled.
     *
     * @return
     * @see #setEnableWalls(boolean)
     */
    public boolean isWallsEnabled() {
        return walls;
    }

    /**
     * Gets the {@link FilterPostProcessor} used.
     *
     * @return FilterPostProcessor used, or null if none specifically set and no filters used
     * @see #setFilterPostProcessor(com.jme3.post.FilterPostProcessor)
     */
    public FilterPostProcessor getFilterPostProcessor() {
        return fpp;
    }

    /**
     * Gets the sun light source.
     * <p>
     * This is the only light source in the scene that casts shadows.
     *
     * @return
     */
    public DirectionalLight getSunLight() {
        return sun;
    }

    /**
     * Gets the atmospheric light source.
     * <p>
     * This light is very dim, but provides much-needed constrast between faces.
     * It comes from generally the same direction as the sun light (within 90
     * degrees of).
     * <p>
     * This light does not produce shadows.
     *
     * @return
     */
    public DirectionalLight getAtmosphericLight() {
        return atmosphere;
    }

    /**
     * Gets the ambient light source.
     *
     * @return
     */
    public AmbientLight getAmbientLight() {
        return ambient;
    }

    /**
     * Gets the {@link SSAOFilter} used.
     *
     * @return
     * @see #setEnableOcclusion(boolean)
     */
    public SSAOFilter getSSAOFilter() {
        return ssaoFilter;
    }

    /**
     * Gets the {@link BloomFilter} used.
     *
     * @return
     * @see #setBloom(jme3test.TestScene.Bloom)
     */
    public BloomFilter getBloomFilter() {
        return bloomFilter;
    }

    /**
     * Gets the {@link EnvironmentProbeControl} used.
     *
     * @return
     */
    public EnvironmentProbeControl getHardwareLightProbe() {
        return hardwareProbe;
    }

    /**
     * Gets the list of {@link RigidBodyControl} created.
     * <p>
     * Do not modify the resulting list.
     *
     * @return
     */
    public LinkedList<RigidBodyControl> getRigidBodyList() {
        return rigidBodyList;
    }

    /**
     * Gets the skybox used.
     *
     * @return
     * @see #setSky(java.lang.String)
     */
    public Spatial getSky() {
        return sky;
    }

    /**
     * Gets the shadow renderer for sunlight.
     * <p>
     * Returns null if the shadow mode is not {@link Shadows#Renderer}.
     *
     * @return
     */
    public DirectionalLightShadowRenderer getSunShadowRenderer() {
        return shadowRenderer;
    }

    /**
     * Gets the shadow filter for sunlight.
     * <p>
     * Returns null if the shadow mode is not {@link Shadows#Filter}.
     *
     * @return
     */
    public DirectionalLightShadowFilter getSunShadowFilter() {
        return shadowFilter;
    }

    /**
     * Dedicated test application for {@link TestScene}.
     */
    private static class TestApp extends SimpleApplication {

        private static void start(String[] args) {
            new TestApp().start();
        }

        @Override
        public void simpleInitApp() {

            BulletAppState bullet = new BulletAppState();
            stateManager.attach(bullet);

            TestScene scene = new TestScene(assetManager, viewPort);
            scene.setPhysicsSpace(bullet.getPhysicsSpace());
            scene.setSubScene(TestScene.PHYSICS_SUBSCENE);
            scene.configureFlyCamSpeed(flyCam);
            scene.configureBackgroundColor();
            scene.configureCameraPosition(cam);
            //scene.configureDefaultAnisotropy(renderer);
            scene.setShadows(TestScene.Shadows.None);
            rootNode.attachChild(scene.load());

        }

    }

}

