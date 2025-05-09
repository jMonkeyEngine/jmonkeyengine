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
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.BloomFilter.GlowMode;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.post.filters.SoftBloomFilter;
import com.jme3.renderer.Limits;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphIterator;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.gltf.GltfModelKey;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Configurable testing environment
 *
 * @author codex
 */
public class TestSceneBuilder {

    /**
     * Cube faces for create skyboxes.
     */
    private static final String[] SKY_CUBE_FACES = {"west", "east", "north", "south", "up", "down"};

    private static final String BASE_SCENE = "Scenes/TestScene/base-scene.gltf";
    private static final Vector3f TILE_SIZE = new Vector3f(20, 0, 20);
    private static final ColorRGBA BACKGROUND_COLOR = new ColorRGBA(.6f, .7f, 1f, 1f);
    private static final float SHADOW_INTENSITY = 0.35f;
    private static final float SUN_INTENSITY = 3f;
    private static final int ANISOTROPIC_LEVEL = 20;

    private final Node node = new Node("Testing_Scene");
    private final Application app;
    private final AssetManager assetManager;

    /**
     *
     * @param app jme application
     */
    public TestSceneBuilder(Application app) {
        this.app = app;
        this.assetManager = this.app.getAssetManager();
        this.node.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    }

    /**
     * Launches a dedicated test application for this class.
     */
    public static void main(String[] args) {
        TestApp.start(args);
    }

    /**
     * Configures various settings for a better testing environment.
     * <p>
     * If {@link #app} is a {@link SimpleApplication}, the test scene is
     * attached to the root node.
     */
    public void configure() {
        app.getViewPort().setBackgroundColor(BACKGROUND_COLOR);
        app.getCamera().setLocation(new Vector3f(-10, 10, -10));
        app.getCamera().lookAtDirection(new Vector3f(1, -1, 1), Vector3f.UNIT_Y);
        Renderer r = app.getRenderManager().getRenderer();
        if (app instanceof SimpleApplication) {
            SimpleApplication simple = (SimpleApplication)app;
            simple.getRootNode().attachChild(node);
            simple.getFlyByCamera().setMoveSpeed(15);
        }
    }

    /**
     * Creates a 3x3 base scene with walls.
     *
     * @see #baseScene(int, int, boolean)
     */
    public void baseScene() {
        baseScene(3, 3, true);
    }

    /**
     * Creates the base scene geometry.
     *
     * @param width number of tiles along the X direction
     * @param height number of tiles along the Z direction
     * @param walls true to generate walls at scene edges
     */
    public void baseScene(int width, int height, boolean walls) {
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
                base.attachChild(tile);
            }
        }
        EnvironmentProbeControl.tagGlobal(base);
        base.setName("BaseScene");
        node.attachChild(base);
    }

    /**
     * Creates a {@link DirectionalLight} for sunlight.
     *
     * @see #sun(Vector3f, ColorRGBA)
     */
    public void sun() {
        sun(new Vector3f(.5f, -1, .2f), new ColorRGBA(1f, .9f, .85f, 1f).multLocal(SUN_INTENSITY));
    }

    /**
     * Creates a {@link DirectionalLight} for sunlight.
     *
     * @param direction direction of light
     * @param color color of light
     */
    public void sun(Vector3f direction, ColorRGBA color) {
        DirectionalLight sun = new DirectionalLight();
        sun.setName("SunLight");
        sun.setDirection(direction);
        sun.setColor(color);
        node.addLight(sun);
    }

    public void brightMountainsSun() {
        sun(new Vector3f(-0.8f, -1, -0.8f), new ColorRGBA(1f, .9f, .85f, 1f).multLocal(SUN_INTENSITY));
    }

    /**
     * Creates an ambient light at an intensity of {@code 0.3f}.
     *
     * @see #ambient(float)
     */
    public void ambient() {
        ambient(0.3f);
    }

    /**
     * Creates a white ambient light multiplied by {@code intensity}.
     *
     * @param intensity intensity of the ambient light's color
     * @see #ambient(ColorRGBA)
     */
    public void ambient(float intensity) {
        ambient(new ColorRGBA(1f, 1f, 1f, 1f).multLocal(intensity));
    }

    /**
     * Creates an {@link AmbientLight}.
     * <p>
     * Use {@link #probe(String, Consumer)} or {@link #hardwareProbe(int, Consumer)} if
     * using {@code PBRLighting} models.
     *
     * @param color color of light
     */
    public void ambient(ColorRGBA color) {
        node.addLight(new AmbientLight(color));
    }

    /**
     * Creates a light probe from the test data default probe.
     *
     * @see #probe(String, Consumer)
     */
    public void probe() {
        probe((Consumer<LightProbe>)null);
    }

    /**
     * Creates a light probe from the test data default probe.
     *
     * @param config configures the light probe (can be null)
     * @see #probe(String, Consumer)
     */
    public void probe(Consumer<LightProbe> config) {
        probe("Scenes/defaultProbe.j3o", config);
    }

    /**
     * Creates a new light probe from the specified J3o asset.
     *
     * @param probeJ3o j3o model containing the light probe
     * @see #probe(String, Consumer)
     */
    public void probe(String probeJ3o) {
        probe(probeJ3o, null);
    }

    /**
     * Loads the specified J3o file and extracts a light probe from it.
     * <p>
     * The light probe is expected to be the first light in the local light
     * list of the root model node. Use {@link #ambient(ColorRGBA)} if using
     * {@code Lighting.j3md} models.
     *
     * @param probeJ3o j3o model containing the light probe
     * @param config configures the light probe (can be null)
     */
    public void probe(String probeJ3o, Consumer<LightProbe> config) {
        Spatial scene = assetManager.loadModel(probeJ3o);
        Object l = scene.getLocalLightList().get(0);
        if (!(l instanceof LightProbe)) {
            throw new IllegalArgumentException("Expected light probe, found " + l);
        }
        LightProbe probe = (LightProbe)l;
        node.addLight(probe);
        if (config != null) {
            config.accept(probe);
        }
    }

    /**
     * Generates a light probe on the GPU with environment maps of size 128.
     *
     * @see #hardwareProbe(int, Consumer)
     */
    public void hardwareProbe() {
        hardwareProbe(128);
    }

    /**
     * Generates a light probe on the GPU.
     *
     * @param envMapSize size of the generated environment maps
     * @see #hardwareProbe(int, Consumer)
     */
    public void hardwareProbe(int envMapSize) {
        hardwareProbe(envMapSize, null);
    }

    /**
     * Generates a light probe on the GPU.
     * <p>
     * The generated probe will account for the base scene and skybox (if they exist).
     * Use {@link #ambient(ColorRGBA)} if using {@code Lighting.j3md} models.
     *
     * @param envMapSize size of the generated environment map
     * @param config configures the probe control (can be null)
     */
    public void hardwareProbe(int envMapSize, Consumer<EnvironmentProbeControl> config) {
        EnvironmentProbeControl env = new EnvironmentProbeControl(assetManager, envMapSize);
        if (config != null) {
            config.accept(env);
        }
        node.addControl(env);
    }

    /**
     * Creates a skybox.
     *
     * @param texture skybox texture asset path
     * @param type specifies the format of the skybox texture
     */
    public void sky(String texture, SkyFactory.EnvMapType type) {
        sky(assetManager.loadTexture(texture), type);
    }

    /**
     * Creates a skybox.
     *
     * @param texture skybox texture
     * @param type specifies the format of the skybox texture
     */
    public void sky(Texture texture, SkyFactory.EnvMapType type) {
        Spatial sky = SkyFactory.createSky(assetManager, texture, type);
        sky.setUserData("IgnorePhysics", true);
        EnvironmentProbeControl.tagGlobal(sky);
        node.attachChild(sky);
    }

    /**
     * Creates a skybox from six seperate textures.
     * <p>
     * Uses {@link #SKY_CUBE_FACES} for {@code faces}.
     *
     * @param textureFormat format of the texture asset paths
     * @see #skyCube(String, String...)
     */
    public void skyCube(String textureFormat) {
        skyCube(textureFormat, SKY_CUBE_FACES);
    }

    /**
     * Creates a skybox from six seperate textures.
     * <p>
     * Each asset path is generated by formatting {@code textureFormat}
     * with the corresponding {@code faces} element as the first argument.
     *
     * @param textureFormat texture asset path format
     * @param faces name of each texture occupying west, east, north, south,
     *              up, and down on the skybox, in that order.
     */
    public void skyCube(String textureFormat, String... faces) {
        assert faces.length == 6 : "Number of faces must be six.";
        Texture west = assetManager.loadTexture(String.format(textureFormat, faces[0]));
        Texture east = assetManager.loadTexture(String.format(textureFormat, faces[1]));
        Texture north = assetManager.loadTexture(String.format(textureFormat, faces[2]));
        Texture south = assetManager.loadTexture(String.format(textureFormat, faces[3]));
        Texture up = assetManager.loadTexture(String.format(textureFormat, faces[4]));
        Texture down = assetManager.loadTexture(String.format(textureFormat, faces[5]));
        Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        sky.setUserData("IgnorePhysics", true);
        EnvironmentProbeControl.tagGlobal(sky);
        node.attachChild(sky);
    }

    /**
     * Creates a skybox of "Textures/Sky/Bright/BrightSky.dds".
     */
    public void brightMountainsSky() {
        sky("Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap);
    }

    /**
     * Creates a skybox of "Textures/Sky/Lagoon/lagoon_%1.jpg", with
     * {@link #SKY_CUBE_FACES} as the faces.
     */
    public void lagoonSky() {
        skyCube("Textures/Sky/Lagoon/lagoon_%1s.jpg");
    }

    /**
     * Creates a skybox of "Textures/Sky/Earth/Earth.jpg".
     */
    public void earthSky() {
        sky("Textures/Sky/Earth/Earth.jpg", SkyFactory.EnvMapType.EquirectMap);
    }

    /**
     * Creates a skybox of "Textures/Sky/St Peters/StPeters.jpg".
     */
    public void stPetersSky() {
        TextureKey key = new TextureKey("Textures/Sky/St Peters/StPeters.jpg");
        key.setFlipY(false);
        sky(assetManager.loadTexture(key), SkyFactory.EnvMapType.SphereMap);
    }

    /**
     * Creates a {@link SoftBloomFilter}.
     *
     * @see #softBloom(Consumer)
     */
    public void softBloom() {
        softBloom(null);
    }

    /**
     * Creates a {@link SoftBloomFilter}.
     * <p>
     * The filter is added to an existing {@link FilterPostProcessor} if possible.
     * Otherwise a new FilterPostProcessor is created.
     *
     * @param config configures the filter (can be null)
     */
    public void softBloom(Consumer<SoftBloomFilter> config) {
        SoftBloomFilter bloom = new SoftBloomFilter();
        if (config != null) {
            config.accept(bloom);
        }
        getOrCreateFpp().addFilter(bloom);
    }

    /**
     * Creates a {@link BloomFilter} with {@link BloomFilter.GlowMode#Scene} as the glow mode.
     *
     * @see #bloom(GlowMode, Consumer)
     */
    public void bloom() {
        bloom(GlowMode.Scene);
    }

    /**
     * Creates a {@link BloomFilter}.
     *
     * @param mode glow mode of the filter
     * @see #bloom(GlowMode, Consumer)
     */
    public void bloom(BloomFilter.GlowMode mode) {
        bloom(mode, null);
    }

    /**
     * Creates a {@link BloomFilter}.
     * <p>
     * The filter is added to an existing {@link FilterPostProcessor} if possible.
     * Otherwise a new FilterPostProcessor is created.
     *
     * @param mode glow mode of the filter
     * @param config configures the filter (can be null)
     */
    public void bloom(BloomFilter.GlowMode mode, Consumer<BloomFilter> config) {
        BloomFilter bloom = new BloomFilter(mode);
        if (config != null) {
            config.accept(bloom);
        }
        getOrCreateFpp().addFilter(bloom);
    }

    /**
     * Creates an {@link FXAAFilter} for antialiasing.
     *
     * @see #antialiasing(Consumer)
     */
    public void antialiasing() {
        antialiasing(null);
    }

    /**
     * Creates an {@link FXAAFilter} for antialiasing.
     * <p>
     * The filter is added to an existing {@link FilterPostProcessor} if possible.
     * Otherwise a new FilterPostProcessor is created.
     *
     * @param config configures the filter (can be null)
     */
    public void antialiasing(Consumer<FXAAFilter> config) {
        FXAAFilter fxaa = new FXAAFilter();
        if (config != null) {
            config.accept(fxaa);
        }
        getOrCreateFpp().addFilter(fxaa);
    }

    /**
     * Creates a {@link LightScatteringFilter}.
     */
    public void scattering() {
        scattering(null);
    }

    /**
     * Creates an {@link LightScatteringFilter}.
     * <p>
     * The filter is added to an existing {@link FilterPostProcessor} if possible.
     * Otherwise a new FilterPostProcessor is created.
     *
     * @param config configures the filter (can be null)
     */
    public void scattering(Consumer<LightScatteringFilter> config) {
        DirectionalLight sun = getSunLight();
        LightScatteringFilter lsf = new LightScatteringFilter(new Vector3f(-1, 1, -1).multLocal(10f));
        lsf.setLightDensity(.5f);
        lsf.setBlurWidth(1.5f);
        if (config != null) {
            config.accept(lsf);
        }
        getOrCreateFpp().addFilter(lsf);
    }

    /**
     * Creates a shadow renderer for the sun directional light.
     *
     * @see #shadowRenderer(int, int, Consumer)
     */
    public void shadowRenderer() {
        shadowRenderer(null);
    }

    /**
     * Creates a shadow renderer for the sun directional light.
     *
     * @param shadowMapSize size of the shadow maps to use
     * @param splits number of cascades (splits) to use
     * @see #shadowRenderer(int, int, Consumer)
     */
    public void shadowRenderer(int shadowMapSize, int splits) {
        shadowRenderer(shadowMapSize, splits, null);
    }

    /**
     * Creates a shadow renderer for the sun directional light.
     *
     * @param config configures the renderer (can be null)
     * @see #shadowRenderer(int, int, Consumer)
     */
    public void shadowRenderer(Consumer<DirectionalLightShadowRenderer> config) {
        shadowRenderer(2048, 3, config);
    }

    /**
     * Creates a shadow renderer for the sun directional light.
     * <p>
     * If {@link #sun(Vector3f, ColorRGBA)} has not been called yet, nothing happens.
     *
     * @param shadowMapSize size of the shadow maps to use
     * @param splits number of cascades (splits) to use
     * @param config configures the shadow renderer (can be null)
     */
    public void shadowRenderer(int shadowMapSize, int splits, Consumer<DirectionalLightShadowRenderer> config) {
        DirectionalLight sun = getSunLight();
        if (sun != null) {
            DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, shadowMapSize, splits);
            dlsr.setLight(sun);
            dlsr.setShadowIntensity(SHADOW_INTENSITY);
            if (config != null) {
                config.accept(dlsr);
            }
            app.getViewPort().addProcessor(dlsr);
        }
    }

    /**
     * Creates a shadow filter for the sun directional light.
     *
     * @see #shadowFilter(int, int, Consumer)
     */
    public void shadowFilter() {
        shadowFilter(null);
    }

    /**
     * Creates a shadow filter for the sun directional light.
     *
     * @param shadowMapSize size of the shadow maps to use
     * @param splits number of cascades (splits) to use
     * @see #shadowFilter(int, int, Consumer)
     */
    public void shadowFilter(int shadowMapSize, int splits) {
        shadowFilter(shadowMapSize, splits, null);
    }

    /**
     * Creates a shadow filter of the sun directional light.
     *
     * @param config configures the filter (can be null)
     * @see #shadowFilter(int, int, Consumer)
     */
    public void shadowFilter(Consumer<DirectionalLightShadowFilter> config) {
        shadowFilter(2048, 3, config);
    }

    /**
     * Creates a shadow renderer for the sun directional light.
     * <p>
     * The filter is added to an existing {@link FilterPostProcessor} if possible.
     * Otherwise a new FilterPostProcessor is created. If {@link #sun(Vector3f, ColorRGBA)}
     * has not been called yet, nothing happens.
     *
     * @param shadowMapSize size of the shadow maps to use
     * @param splits number of cascades (splits) to use
     * @param config configures the shadow renderer (can be null)
     */
    public void shadowFilter(int shadowMapSize, int splits, Consumer<DirectionalLightShadowFilter> config) {
        DirectionalLight sun = getSunLight();
        if (sun != null) {
            DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, shadowMapSize, splits);
            dlsf.setLight(sun);
            dlsf.setShadowIntensity(SHADOW_INTENSITY);
            if (config != null) {
                config.accept(dlsf);
            }
            getOrCreateFpp().addFilter(dlsf);
        }
    }

    /**
     * Initializes static physics for all geometries attached to the test scene node.
     *
     * @see #physics(Predicate)
     */
    public void physics() {
        physics(s -> true);
    }

    /**
     * Initializes static physics for all geometries attached to the test scene node.
     * <p>
     * Spatials and children of spatials that are either rejected by {@code filter} or
     * contain "IgnorePhysics" userdata do not become physical. If a {@link BulletAppState}
     * is not attached to the {@link com.jme3.app.state.AppStateManager}, then a new
     * BulletAppState is created.
     *
     * @param filter filters out spatials that should not become physical.
     */
    public void physics(Predicate<Spatial> filter) {
        PhysicsSpace space = getOrCreatePhysics();
        for (SceneGraphIterator it = new SceneGraphIterator(node); it.hasNext();) {
            Spatial s = it.next();
            if (s.getUserData("IgnorePhysics") != null || !filter.test(s)) {
                it.ignoreChildren();
                continue;
            }
            if (s instanceof Geometry) {
                RigidBodyControl rbc = new RigidBodyControl(0);
                s.addControl(rbc);
                space.add(rbc);
            }
        }
    }

    /**
     * Enables or disables debug mode for physics.
     *
     * @param debug true to enable, false to disable
     */
    public void physicsDebug(boolean debug) {
        getOrCreateBullet().setDebugEnabled(debug);
    }

    /**
     * Creates a basic first person physical character.
     *
     * @see #character(float, float, float, Consumer)
     */
    public void character() {
        character(null);
    }

    /**
     * Creates a basic first person physical character.
     *
     * @param config configures the character
     * @see #character(float, float, float, Consumer)
     */
    public void character(Consumer<FirstPersonCharacter> config) {
        character(1f, 7f, 100f, config);
    }

    /**
     * Creates a basic first person physical character.
     *
     * @param radius radius of the character's collision shape
     * @param height full height of the character's collision shape
     * @param mass mass of the character's rigid body
     * @see #character(float, float, float, Consumer)
     */
    public void character(float radius, float height, float mass) {
        character(radius, height, mass, null);
    }

    /**
     * Creates a basic first person physical character.
     * <p>
     * If a {@link BulletAppState} is not attached to the {@link com.jme3.app.state.AppStateManager},
     * then a new BulletAppState is created.
     *
     * @param radius radius of the character's collision shape
     * @param height full height of the character's collision shape
     * @param mass mass of the character's rigid body
     * @param config configures the character (can be null)
     */
    public void character(float radius, float height, float mass, Consumer<FirstPersonCharacter> config) {
        FirstPersonCharacter fpc = new FirstPersonCharacter(radius, height, mass);
        if (config != null) {
            config.accept(fpc);
        }
        Node n = new Node("TestScene_FirstPersonCharacter");
        node.attachChild(n);
        n.addControl(fpc);
        getOrCreatePhysics().add(fpc);
    }

    /**
     * Get the underlying test scene node.
     *
     * @return underlying test scene node
     */
    public Node getNode() {
        return node;
    }

    private Spatial createTerrainTile(Node terrainSrc, String name) {
        Integer v = terrainSrc.getUserData(name);
        Spatial tile;
        if (v != null) {
            tile = terrainSrc.getChild(name + FastMath.rand.nextInt(v)).clone();
        } else {
            tile = terrainSrc.getChild(name).clone();
        }
        Texture color = assetManager.loadTexture(new TextureKey("Scenes/TestScene/grid-grey.png", false));
        color.setWrap(Texture.WrapMode.Repeat);
        color.setMinFilter(Texture.MinFilter.BilinearNearestMipMap);
        color.setMagFilter(Texture.MagFilter.Bilinear);
        color.setAnisotropicFilter(Math.min(ANISOTROPIC_LEVEL, app.getRenderer().getLimits().get(Limits.TextureAnisotropy)));
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setTexture("BaseColorMap", color);
        mat.setFloat("Metallic", .1f);
        mat.setFloat("Roughness", .9f);
        tile.setMaterial(mat);
        return tile;
    }
    private DirectionalLight getSunLight() {
        for (Light l : node.getLocalLightList()) {
            if (l.getName().equals("SunLight") && l instanceof DirectionalLight) {
                return (DirectionalLight)l;
            }
        }
        return null;
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
    private BulletAppState getOrCreateBullet() {
        BulletAppState bullet = app.getStateManager().getState(BulletAppState.class, false);
        if (bullet == null) {
            bullet = new BulletAppState();
            app.getStateManager().attach(bullet);
        }
        return bullet;
    }
    private PhysicsSpace getOrCreatePhysics() {
        return getOrCreateBullet().getPhysicsSpace();
    }

    /**
     * Dedicated test application for {@link TestSceneBuilder}.
     */
    private static class TestApp extends SimpleApplication {

        private static void start(String[] args) {
            new TestApp().start();
        }

        @Override
        public void simpleInitApp() {

            TestSceneBuilder scene = new TestSceneBuilder(this);
            scene.configure();
            scene.baseScene();
            scene.brightMountainsSun();
            scene.hardwareProbe();
            scene.brightMountainsSky();
            scene.softBloom(b -> b.setGlowFactor(0.1f));
            scene.scattering();
            scene.antialiasing();
            scene.shadowFilter();
            scene.physics();
            scene.character();

        }

    }

    /**
     * Simple first person physical character control.
     */
    public class FirstPersonCharacter extends BetterCharacterControl implements AnalogListener {

        private static final String FORWARD = "TestScene_Character_Forward";
        private static final String BACKWARD = "TestScene_Character_Backward";
        private static final String LEFT = "TestScene_Character_Left";
        private static final String RIGHT = "TestScene_Character_Right";
        private static final String JUMP = "TestScene_Character_Jump";

        private final Vector2f input = new Vector2f();
        private final Vector3f walk = new Vector3f();
        private float walkSpeed = 10f;
        private float strafeSpeed = 7f;
        private float jumpDelay = 0f;
        private Trigger forwardTrigger = new KeyTrigger(KeyInput.KEY_W);
        private Trigger backwardTrigger = new KeyTrigger(KeyInput.KEY_S);
        private Trigger leftTrigger = new KeyTrigger(KeyInput.KEY_A);
        private Trigger rightTrigger = new KeyTrigger(KeyInput.KEY_D);
        private Trigger jumpTrigger = new KeyTrigger(KeyInput.KEY_SPACE);

        /**
         *
         * @param radius collision shape radius
         * @param height collision shape height
         * @param mass rigid body mass
         */
        public FirstPersonCharacter(float radius, float height, float mass) {
            super(radius, height, mass);
        }

        @Override
        public void update(float tpf) {
            jumpDelay -= tpf;
            app.getCamera().getDirection(walk);
            walk.multLocal(input.y * walkSpeed).addLocal(app.getCamera().getLeft().mult(input.x * strafeSpeed)).setY(0f);
            setWalkDirection(walk);
            input.set(0f, 0f);
            super.update(tpf);
            app.getCamera().setLocation(spatial.getWorldTranslation().add(0f, getFinalHeight() - 2f, 0f));
        }

        @Override
        public void setSpatial(Spatial spat) {
            if (spatial == null && spat != null) {
                InputManager in = app.getInputManager();
                in.addMapping(FORWARD, forwardTrigger);
                in.addMapping(BACKWARD, backwardTrigger);
                in.addMapping(LEFT, leftTrigger);
                in.addMapping(RIGHT, rightTrigger);
                in.addMapping(JUMP, jumpTrigger);
                in.addListener(this, FORWARD, BACKWARD, LEFT, RIGHT, JUMP);
            } else if (spatial != null && spat == null) {
                app.getInputManager().removeListener(this);
            }
            super.setSpatial(spat);
        }

        @Override
        public void onAnalog(String name, float value, float tpf) {
            switch (name) {
                case FORWARD: input.y = 1f; break;
                case BACKWARD: input.y = -1f; break;
                case LEFT: input.x = 1f; break;
                case RIGHT: input.x = -1f; break;
                case JUMP: jump(); break;
            }
        }

        @Override
        public void jump() {
            if (jumpDelay <= 0f && isOnGround()) {
                super.jump();
                jumpDelay = 0.05f;
            }
        }

        /**
         * Gets the forward/backward speed.
         *
         * @return forward/backward speed
         */
        public float getWalkSpeed() {
            return walkSpeed;
        }

        /**
         * Sets the forward/backward speed.
         *
         * @param walkSpeed forward/backward speed
         */
        public void setWalkSpeed(float walkSpeed) {
            this.walkSpeed = walkSpeed;
        }

        /**
         * Gets the left/right speed.
         *
         * @return left/right speed
         */
        public float getStrafeSpeed() {
            return strafeSpeed;
        }

        /**
         * Sets the left/right speed.
         *
         * @param strafeSpeed left/right speed
         */
        public void setStrafeSpeed(float strafeSpeed) {
            this.strafeSpeed = strafeSpeed;
        }

        /**
         * Sets the forward input trigger.
         * <p>
         * Has no effect of this control has already been attached to a spatial.
         *
         * @param forwardTrigger input trigger
         */
        public void setForwardTrigger(Trigger forwardTrigger) {
            this.forwardTrigger = forwardTrigger;
        }

        /**
         * Sets the backward input trigger.
         * <p>
         * Has no effect of this control has already been attached to a spatial.
         *
         * @param backwardTrigger input trigger
         */
        public void setBackwardTrigger(Trigger backwardTrigger) {
            this.backwardTrigger = backwardTrigger;
        }

        /**
         * Sets the left input trigger.
         * <p>
         * Has no effect of this control has already been attached to a spatial.
         *
         * @param leftTrigger input trigger
         */
        public void setLeftTrigger(Trigger leftTrigger) {
            this.leftTrigger = leftTrigger;
        }

        /**
         * Sets the right input trigger.
         * <p>
         * Has no effect of this control has already been attached to a spatial.
         *
         * @param rightTrigger input trigger
         */
        public void setRightTrigger(Trigger rightTrigger) {
            this.rightTrigger = rightTrigger;
        }

        /**
         * Sets the jump input trigger.
         * <p>
         * Has no effect of this control has already been attached to a spatial.
         *
         * @param jumpTrigger input trigger
         */
        public void setJumpTrigger(Trigger jumpTrigger) {
            this.jumpTrigger = jumpTrigger;
        }

    }

}

