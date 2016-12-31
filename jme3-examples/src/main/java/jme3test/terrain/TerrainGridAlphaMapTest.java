package jme3test.terrain;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.terrain.geomipmap.TerrainGrid;
import com.jme3.terrain.geomipmap.TerrainGridListener;
import com.jme3.terrain.geomipmap.TerrainGridLodControl;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.grid.FractalTileLoader;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import java.io.File;

public class TerrainGridAlphaMapTest extends SimpleApplication {

    private TerrainGrid terrain;
    private float grassScale = 64;
    private float dirtScale = 16;
    private float rockScale = 128;
    private boolean usePhysics = false;

    public static void main(final String[] args) {
        TerrainGridAlphaMapTest app = new TerrainGridAlphaMapTest();
        app.start();
    }
    private CharacterControl player3;
    private FractalSum base;
    private PerturbFilter perturb;
    private OptimizedErode therm;
    private SmoothFilter smooth;
    private IterativeFilter iterate;
    private Material material;
    private Material matWire;

    @Override
    public void simpleInitApp() {
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(sun);

        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        File file = new File("TerrainGridTestData.zip");
        if (!file.exists()) {
            assetManager.registerLocator("https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/jmonkeyengine/TerrainGridTestData.zip", HttpZipLocator.class);
        } else {
            assetManager.registerLocator("TerrainGridTestData.zip", ZipLocator.class);
        }

        this.flyCam.setMoveSpeed(100f);
        ScreenshotAppState state = new ScreenshotAppState();
        this.stateManager.attach(state);

        // TERRAIN TEXTURE material
        material = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        material.setBoolean("useTriPlanarMapping", false);
        //material.setBoolean("isTerrainGrid", true);
        material.setFloat("Shininess", 0.0f);

        // GRASS texture
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        material.setTexture("DiffuseMap", grass);
        material.setFloat("DiffuseMap_0_scale", grassScale);

        // DIRT texture
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        material.setTexture("DiffuseMap_1", dirt);
        material.setFloat("DiffuseMap_1_scale", dirtScale);

        // ROCK texture
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        material.setTexture("DiffuseMap_2", rock);
        material.setFloat("DiffuseMap_2_scale", rockScale);

        // WIREFRAME material
        matWire = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWire.getAdditionalRenderState().setWireframe(true);
        matWire.setColor("Color", ColorRGBA.Green);

        this.base = new FractalSum();
        this.base.setRoughness(0.7f);
        this.base.setFrequency(1.0f);
        this.base.setAmplitude(1.0f);
        this.base.setLacunarity(2.12f);
        this.base.setOctaves(8);
        this.base.setScale(0.02125f);
        this.base.addModulator(new NoiseModulator() {

            @Override
            public float value(float... in) {
                return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
            }
        });

        FilteredBasis ground = new FilteredBasis(this.base);

        this.perturb = new PerturbFilter();
        this.perturb.setMagnitude(0.119f);

        this.therm = new OptimizedErode();
        this.therm.setRadius(5);
        this.therm.setTalus(0.011f);

        this.smooth = new SmoothFilter();
        this.smooth.setRadius(1);
        this.smooth.setEffect(0.7f);

        this.iterate = new IterativeFilter();
        this.iterate.addPreFilter(this.perturb);
        this.iterate.addPostFilter(this.smooth);
        this.iterate.setFilter(this.therm);
        this.iterate.setIterations(1);

        ground.addPreFilter(this.iterate);

        this.terrain = new TerrainGrid("terrain", 33, 257, new FractalTileLoader(ground, 256));
        this.terrain.setMaterial(this.material);

        this.terrain.setLocalTranslation(0, 0, 0);
        this.terrain.setLocalScale(2f, 1f, 2f);
        this.rootNode.attachChild(this.terrain);

        TerrainLodControl control = new TerrainGridLodControl(this.terrain, this.getCamera());
        control.setLodCalculator( new DistanceLodCalculator(33, 2.7f) ); // patch size, and a multiplier
        this.terrain.addControl(control);

        final BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);


        this.getCamera().setLocation(new Vector3f(0, 256, 0));

        this.viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));

        if (usePhysics) {
            CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.5f, 1.8f, 1);
            player3 = new CharacterControl(capsuleShape, 0.5f);
            player3.setJumpSpeed(20);
            player3.setFallSpeed(10);
            player3.setGravity(10);

            player3.setPhysicsLocation(new Vector3f(cam.getLocation().x, 256, cam.getLocation().z));

            bulletAppState.getPhysicsSpace().add(player3);

        }
        terrain.addListener(new TerrainGridListener() {

            public void gridMoved(Vector3f newCenter) {
            }

            public void tileAttached(Vector3f cell, TerrainQuad quad) {
                Texture alpha = null;
                try {
                    alpha = assetManager.loadTexture("TerrainAlphaTest/alpha_" + (int)cell.x+ "_" + (int)cell.z + ".png");
                } catch (Exception e) {
                    alpha = assetManager.loadTexture("TerrainAlphaTest/alpha_default.png");
                }
                quad.getMaterial().setTexture("AlphaMap", alpha);
                if (usePhysics) {
                    quad.addControl(new RigidBodyControl(new HeightfieldCollisionShape(quad.getHeightMap(), terrain.getLocalScale()), 0));
                    bulletAppState.getPhysicsSpace().add(quad);
                }
                updateMarkerElevations();
            }

            public void tileDetached(Vector3f cell, TerrainQuad quad) {
                if (usePhysics) {
                    if (quad.getControl(RigidBodyControl.class) != null) {
                        bulletAppState.getPhysicsSpace().remove(quad);
                        quad.removeControl(RigidBodyControl.class);
                    }
                }
                updateMarkerElevations();
            }
        });
        
        this.initKeys();
    
        markers = new Node();
        rootNode.attachChild(markers);
        createMarkerPoints(1);
    }
    
    Node markers;
    
    
    private void createMarkerPoints(float count) {
        Node center = createAxisMarker(10);
        markers.attachChild(center);
        
        float xS = (count-1)*terrain.getTerrainSize() - (terrain.getTerrainSize()/2);
        float zS = (count-1)*terrain.getTerrainSize() - (terrain.getTerrainSize()/2);
        float xSi = xS;
        float zSi = zS;
        for (int x=0; x<count*2; x++) {
            for (int z=0; z<count*2; z++) {
                Node m = createAxisMarker(5);
                m.setLocalTranslation(xSi, 0, zSi);
                markers.attachChild(m);
                zSi += terrain.getTerrainSize();
            }
            zSi = zS;
            xSi += terrain.getTerrainSize();
        }
    }
    
    private void updateMarkerElevations() {
        for (Spatial s : markers.getChildren()) {
            float h = terrain.getHeight(new Vector2f(s.getLocalTranslation().x, s.getLocalTranslation().z));
            s.setLocalTranslation(s.getLocalTranslation().x, h+1, s.getLocalTranslation().z);
        }
    }
    
    private void initKeys() {
        // You can map one or several inputs to one named action
        this.inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_A));
        this.inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_D));
        this.inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_W));
        this.inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_S));
        this.inputManager.addMapping("Jumps", new KeyTrigger(KeyInput.KEY_SPACE));
        this.inputManager.addListener(this.actionListener, "Lefts");
        this.inputManager.addListener(this.actionListener, "Rights");
        this.inputManager.addListener(this.actionListener, "Ups");
        this.inputManager.addListener(this.actionListener, "Downs");
        this.inputManager.addListener(this.actionListener, "Jumps");
    }
    private boolean left;
    private boolean right;
    private boolean up;
    private boolean down;
    private final ActionListener actionListener = new ActionListener() {

        @Override
        public void onAction(final String name, final boolean keyPressed, final float tpf) {
            if (name.equals("Lefts")) {
                if (keyPressed) {
                    TerrainGridAlphaMapTest.this.left = true;
                } else {
                    TerrainGridAlphaMapTest.this.left = false;
                }
            } else if (name.equals("Rights")) {
                if (keyPressed) {
                    TerrainGridAlphaMapTest.this.right = true;
                } else {
                    TerrainGridAlphaMapTest.this.right = false;
                }
            } else if (name.equals("Ups")) {
                if (keyPressed) {
                    TerrainGridAlphaMapTest.this.up = true;
                } else {
                    TerrainGridAlphaMapTest.this.up = false;
                }
            } else if (name.equals("Downs")) {
                if (keyPressed) {
                    TerrainGridAlphaMapTest.this.down = true;
                } else {
                    TerrainGridAlphaMapTest.this.down = false;
                }
            } else if (name.equals("Jumps")) {
                TerrainGridAlphaMapTest.this.player3.jump();
            }
        }
    };
    private final Vector3f walkDirection = new Vector3f();

    @Override
    public void simpleUpdate(final float tpf) {
        Vector3f camDir = this.cam.getDirection().clone().multLocal(0.6f);
        Vector3f camLeft = this.cam.getLeft().clone().multLocal(0.4f);
        this.walkDirection.set(0, 0, 0);
        if (this.left) {
            this.walkDirection.addLocal(camLeft);
        }
        if (this.right) {
            this.walkDirection.addLocal(camLeft.negate());
        }
        if (this.up) {
            this.walkDirection.addLocal(camDir);
        }
        if (this.down) {
            this.walkDirection.addLocal(camDir.negate());
        }

        if (usePhysics) {
            this.player3.setWalkDirection(this.walkDirection);
            this.cam.setLocation(this.player3.getPhysicsLocation());
        }
    }
    
    protected Node createAxisMarker(float arrowSize) {

        Material redMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        redMat.getAdditionalRenderState().setWireframe(true);
        redMat.setColor("Color", ColorRGBA.Red);
        
        Material greenMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        greenMat.getAdditionalRenderState().setWireframe(true);
        greenMat.setColor("Color", ColorRGBA.Green);
        
        Material blueMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        blueMat.getAdditionalRenderState().setWireframe(true);
        blueMat.setColor("Color", ColorRGBA.Blue);

        Node axis = new Node();

        // create arrows
        Geometry arrowX = new Geometry("arrowX", new Arrow(new Vector3f(arrowSize, 0, 0)));
        arrowX.setMaterial(redMat);
        Geometry arrowY = new Geometry("arrowY", new Arrow(new Vector3f(0, arrowSize, 0)));
        arrowY.setMaterial(greenMat);
        Geometry arrowZ = new Geometry("arrowZ", new Arrow(new Vector3f(0, 0, arrowSize)));
        arrowZ.setMaterial(blueMat);
        axis.attachChild(arrowX);
        axis.attachChild(arrowY);
        axis.attachChild(arrowZ);

        //axis.setModelBound(new BoundingBox());
        return axis;
    }
}
