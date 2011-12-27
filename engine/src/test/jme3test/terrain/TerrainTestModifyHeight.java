/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package jme3test.terrain;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Sphere;
import com.jme3.terrain.geomipmap.TerrainGrid;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.grid.FractalTileLoader;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Brent Owens
 */
public class TerrainTestModifyHeight extends SimpleApplication {

    private TerrainQuad terrain;
    Material matTerrain;
    Material matWire;
    boolean wireframe = true;
    boolean triPlanar = false;
    boolean wardiso = false;
    boolean minnaert = false;
    protected BitmapText hintText;
    private float grassScale = 64;
    private float dirtScale = 16;
    private float rockScale = 128;
    
    private boolean raiseTerrain = false;
    private boolean lowerTerrain = false;
    
    private Geometry marker;
    private Geometry markerNormal;

    public static void main(String[] args) {
        TerrainTestModifyHeight app = new TerrainTestModifyHeight();
        app.start();
    }

    @Override
    public void simpleUpdate(float tpf){
        Vector3f intersection = getWorldIntersection();
        updateHintText(intersection);
        
        if (raiseTerrain){
            
            if (intersection != null) {
                adjustHeight(intersection, 64, tpf * 60);
            }
        }else if (lowerTerrain){
            if (intersection != null) {
                adjustHeight(intersection, 64, -tpf * 60);
            }
        }
        
        if (terrain != null && intersection != null) {
            float h = terrain.getHeight(new Vector2f(intersection.x, intersection.z));
            Vector3f tl = terrain.getWorldTranslation();
            marker.setLocalTranslation(tl.add(new Vector3f(intersection.x, h, intersection.z)) );
            markerNormal.setLocalTranslation(tl.add(new Vector3f(intersection.x, h, intersection.z)) );
            
            Vector3f normal = terrain.getNormal(new Vector2f(intersection.x, intersection.z));
            ((Arrow)markerNormal.getMesh()).setArrowExtent(normal);
        }
    }
    
    @Override
    public void simpleInitApp() {
        loadHintText();
        initCrossHairs();
        setupKeys();
        
        createMarker();

        // WIREFRAME material
        matWire = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWire.getAdditionalRenderState().setWireframe(true);
        matWire.setColor("Color", ColorRGBA.Green);
        
        createTerrain();
        //createTerrainGrid();
        
        DirectionalLight light = new DirectionalLight();
        light.setDirection((new Vector3f(-0.5f, -1f, -0.5f)).normalize());
        rootNode.addLight(light);

        AmbientLight ambLight = new AmbientLight();
        ambLight.setColor(new ColorRGBA(1f, 1f, 0.8f, 0.2f));
        rootNode.addLight(ambLight);

        cam.setLocation(new Vector3f(0, 256, 0));
        cam.lookAtDirection(new Vector3f(0, -1f, 0).normalizeLocal(), Vector3f.UNIT_X);
    }
    
    public void loadHintText() {
        hintText = new BitmapText(guiFont, false);
        hintText.setLocalTranslation(0, getCamera().getHeight(), 0);
        hintText.setText("Hit 1 to raise terrain, hit 2 to lower terrain");
        guiNode.attachChild(hintText);
    }

    public void updateHintText(Vector3f target) {
        int x = (int) getCamera().getLocation().x;
        int y = (int) getCamera().getLocation().y;
        int z = (int) getCamera().getLocation().z;
        String targetText = "";
        if (target!= null)
            targetText = "  intersect: "+target.toString();
        hintText.setText("Press left mouse button to raise terrain, press right mouse button to lower terrain.  " + x + "," + y + "," + z+targetText);
    }

    protected void initCrossHairs() {
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    private void setupKeys() {
        flyCam.setMoveSpeed(100);
        inputManager.addMapping("wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(actionListener, "wireframe");
        inputManager.addMapping("Raise", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "Raise");
        inputManager.addMapping("Lower", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(actionListener, "Lower");
    }
    private ActionListener actionListener = new ActionListener() {

        public void onAction(String name, boolean pressed, float tpf) {
            if (name.equals("wireframe") && !pressed) {
                wireframe = !wireframe;
                if (!wireframe) {
                    terrain.setMaterial(matWire);
                } else {
                    terrain.setMaterial(matTerrain);
                }
            } else if (name.equals("Raise")) {
                raiseTerrain = pressed;
            } else if (name.equals("Lower")) {
                lowerTerrain = pressed;
            }
        }
    };

    private void adjustHeight(Vector3f loc, float radius, float height) {

        // offset it by radius because in the loop we iterate through 2 radii
        int radiusStepsX = (int) (radius / terrain.getLocalScale().x);
        int radiusStepsZ = (int) (radius / terrain.getLocalScale().z);

        float xStepAmount = terrain.getLocalScale().x;
        float zStepAmount = terrain.getLocalScale().z;
        long start = System.currentTimeMillis();
        List<Vector2f> locs = new ArrayList<Vector2f>();
        List<Float> heights = new ArrayList<Float>();
        
        for (int z = -radiusStepsZ; z < radiusStepsZ; z++) {
            for (int x = -radiusStepsX; x < radiusStepsX; x++) {

                float locX = loc.x + (x * xStepAmount);
                float locZ = loc.z + (z * zStepAmount);

                if (isInRadius(locX - loc.x, locZ - loc.z, radius)) {
                    // see if it is in the radius of the tool
                    float h = calculateHeight(radius, height, locX - loc.x, locZ - loc.z);
                    locs.add(new Vector2f(locX, locZ));
                    heights.add(h);
                }
            }
        }

        terrain.adjustHeight(locs, heights);
        //System.out.println("Modified "+locs.size()+" points, took: " + (System.currentTimeMillis() - start)+" ms");
        terrain.updateModelBound();
    }

    private boolean isInRadius(float x, float y, float radius) {
        Vector2f point = new Vector2f(x, y);
        // return true if the distance is less than equal to the radius
        return point.length() <= radius;
    }

    private float calculateHeight(float radius, float heightFactor, float x, float z) {
        // find percentage for each 'unit' in radius
        Vector2f point = new Vector2f(x, z);
        float val = point.length() / radius;
        val = 1 - val;
        if (val <= 0) {
            val = 0;
        }
        return heightFactor * val;
    }

    private Vector3f getWorldIntersection() {
        Vector3f origin = cam.getWorldCoordinates(new Vector2f(settings.getWidth() / 2, settings.getHeight() / 2), 0.0f);
        Vector3f direction = cam.getWorldCoordinates(new Vector2f(settings.getWidth() / 2, settings.getHeight() / 2), 0.3f);
        direction.subtractLocal(origin).normalizeLocal();

        Ray ray = new Ray(origin, direction);
        CollisionResults results = new CollisionResults();
        int numCollisions = terrain.collideWith(ray, results);
        if (numCollisions > 0) {
            CollisionResult hit = results.getClosestCollision();
            return hit.getContactPoint();
        }
        return null;
    }
    
    private void createTerrain() {
        // First, we load up our textures and the heightmap texture for the terrain

        // TERRAIN TEXTURE material
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
        matTerrain.setBoolean("useTriPlanarMapping", false);
        matTerrain.setBoolean("WardIso", true);
        matTerrain.setFloat("Shininess", 0);

        // ALPHA map (for splat textures)
        matTerrain.setTexture("AlphaMap", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));

        // GRASS texture
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap", grass);
        matTerrain.setFloat("DiffuseMap_0_scale", grassScale);

        // DIRT texture
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_1", dirt);
        matTerrain.setFloat("DiffuseMap_1_scale", dirtScale);

        // ROCK texture
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("DiffuseMap_2", rock);
        matTerrain.setFloat("DiffuseMap_2_scale", rockScale);

        // HEIGHTMAP image (for the terrain heightmap)
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/mountains512.png");
        AbstractHeightMap heightmap = null;
        try {
            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.5f);
            heightmap.load();
            heightmap.smooth(0.9f, 1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // CREATE THE TERRAIN
        terrain = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        control.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
        terrain.addControl(control);
        terrain.setMaterial(matTerrain);
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(2.5f, 0.5f, 2.5f);
        rootNode.attachChild(terrain);
    }

    private void createTerrainGrid() {
        
        // TERRAIN TEXTURE material
        matTerrain = new Material(this.assetManager, "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");

        // Parameters to material:
        // regionXColorMap: X = 1..4 the texture that should be appliad to state X
        // regionX: a Vector3f containing the following information:
        //      regionX.x: the start height of the region
        //      regionX.y: the end height of the region
        //      regionX.z: the texture scale for the region
        //  it might not be the most elegant way for storing these 3 values, but it packs the data nicely :)
        // slopeColorMap: the texture to be used for cliffs, and steep mountain sites
        // slopeTileFactor: the texture scale for slopes
        // terrainSize: the total size of the terrain (used for scaling the texture)
        // GRASS texture
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("region1ColorMap", grass);
        matTerrain.setVector3("region1", new Vector3f(88, 200, this.grassScale));

        // DIRT texture
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("region2ColorMap", dirt);
        matTerrain.setVector3("region2", new Vector3f(0, 90, this.dirtScale));

        // ROCK texture
        Texture rock = assetManager.loadTexture("Textures/Terrain/Rock2/rock.jpg");
        rock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("region3ColorMap", rock);
        matTerrain.setVector3("region3", new Vector3f(198, 260, this.rockScale));

        matTerrain.setTexture("region4ColorMap", rock);
        matTerrain.setVector3("region4", new Vector3f(198, 260, this.rockScale));

        matTerrain.setTexture("slopeColorMap", rock);
        matTerrain.setFloat("slopeTileFactor", 32);

        matTerrain.setFloat("terrainSize", 513);

        FractalSum base = new FractalSum();
        base.setRoughness(0.7f);
        base.setFrequency(1.0f);
        base.setAmplitude(1.0f);
        base.setLacunarity(2.12f);
        base.setOctaves(8);
        base.setScale(0.02125f);
        base.addModulator(new NoiseModulator() {
            @Override
            public float value(float... in) {
                return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
            }
        });

        FilteredBasis ground = new FilteredBasis(base);

        PerturbFilter perturb = new PerturbFilter();
        perturb.setMagnitude(0.119f);

        OptimizedErode therm = new OptimizedErode();
        therm.setRadius(5);
        therm.setTalus(0.011f);

        SmoothFilter smooth = new SmoothFilter();
        smooth.setRadius(1);
        smooth.setEffect(0.7f);

        IterativeFilter iterate = new IterativeFilter();
        iterate.addPreFilter(perturb);
        iterate.addPostFilter(smooth);
        iterate.setFilter(therm);
        iterate.setIterations(1);

        ground.addPreFilter(iterate);

        this.terrain = new TerrainGrid("terrain", 65, 257, new FractalTileLoader(ground, 256f));


        terrain.setMaterial(matTerrain);
        terrain.setLocalTranslation(0, 0, 0);
        terrain.setLocalScale(2f, 1f, 2f);
        
        rootNode.attachChild(this.terrain);

        TerrainLodControl control = new TerrainLodControl(this.terrain, getCamera());
        this.terrain.addControl(control);
    }

    private void createMarker() {
        // collision marker
        Sphere sphere = new Sphere(8, 8, 0.5f);
        marker = new Geometry("Marker");
        marker.setMesh(sphere);
        
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(251f/255f, 130f/255f, 0f, 0.6f));
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        
        marker.setMaterial(mat);
        rootNode.attachChild(marker);
        
        
        // surface normal marker
        Arrow arrow = new Arrow(new Vector3f(0,1,0));
        markerNormal = new Geometry("MarkerNormal");
        markerNormal.setMesh(arrow);
        markerNormal.setMaterial(mat);
        rootNode.attachChild(markerNormal);
    }
}
