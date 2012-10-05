/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
import com.jme3.font.BitmapText;
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
import com.jme3.scene.shape.Sphere;
import com.jme3.terrain.ProgressMonitor;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.MultiTerrainLodControl;
import com.jme3.terrain.geomipmap.NeighbourFinder;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import java.util.List;

/**
 * Demonstrates the NeighbourFinder interface for TerrainQuads,
 * allowing you to tile terrains together without having to use
 * TerrainGrid. It also introduces the MultiTerrainLodControl that
 * will seam the edges of all the terrains supplied.
 * 
 * @author sploreg
 */
public class TerrainTestTile extends SimpleApplication {

    private TiledTerrain terrain;
    Material matTerrain;
    Material matWire;
    boolean wireframe = true;
    boolean triPlanar = false;
    boolean wardiso = false;
    boolean minnaert = false;
    protected BitmapText hintText;
    private float grassScale = 256;
    

    public static void main(String[] args) {
        TerrainTestTile app = new TerrainTestTile();
        app.start();
    }

    
    
    @Override
    public void simpleInitApp() {
        loadHintText();
        setupKeys();

        // WIREFRAME material
        matWire = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWire.getAdditionalRenderState().setWireframe(true);
        matWire.setColor("Color", ColorRGBA.Green);
        
        terrain = new TiledTerrain();
        rootNode.attachChild(terrain);
        
        DirectionalLight light = new DirectionalLight();
        light.setDirection((new Vector3f(-0.5f, -1f, -0.5f)).normalize());
        rootNode.addLight(light);

        AmbientLight ambLight = new AmbientLight();
        ambLight.setColor(new ColorRGBA(1f, 1f, 0.8f, 0.2f));
        rootNode.addLight(ambLight);

        cam.setLocation(new Vector3f(0, 256, 0));
        cam.lookAtDirection(new Vector3f(0, -1, -1).normalizeLocal(), Vector3f.UNIT_Y);
        
        
        Sphere s = new Sphere(12, 12, 3);
        Geometry g = new Geometry("marker");
        g.setMesh(s);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Red);
        g.setMaterial(mat);
        g.setLocalTranslation(0, -100, 0);
        rootNode.attachChild(g);
        
        Geometry g2 = new Geometry("marker");
        g2.setMesh(s);
        mat.setColor("Color", ColorRGBA.Red);
        g2.setMaterial(mat);
        g2.setLocalTranslation(10, -100, 0);
        rootNode.attachChild(g2);
        
        Geometry g3 = new Geometry("marker");
        g3.setMesh(s);
        mat.setColor("Color", ColorRGBA.Red);
        g3.setMaterial(mat);
        g3.setLocalTranslation(0, -100, 10);
        rootNode.attachChild(g3);
    }
    
    public void loadHintText() {
        hintText = new BitmapText(guiFont, false);
        hintText.setLocalTranslation(0, getCamera().getHeight(), 0);
        hintText.setText("Hit 'T' to toggle wireframe");
        guiNode.attachChild(hintText);
    }


    private void setupKeys() {
        flyCam.setMoveSpeed(100);
        inputManager.addMapping("wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(actionListener, "wireframe");
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
            }
        }
    };

    /**
     * A sample class (node in this case) that demonstrates
     * the use of NeighbourFinder.
     * It just links up the left,right,top,bottom TerrainQuads
     * so LOD can work.
     * It does not implement many of the Terrain interface's methods,
     * you will want to do that for your own implementations.
     */
    private class TiledTerrain extends Node implements Terrain, NeighbourFinder {

        private TerrainQuad terrain1;
        private TerrainQuad terrain2;
        private TerrainQuad terrain3;
        private TerrainQuad terrain4;
        
        TiledTerrain() {
            // TERRAIN TEXTURE material
            matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/TerrainLighting.j3md");
            matTerrain.setBoolean("useTriPlanarMapping", false);
            matTerrain.setBoolean("WardIso", true);
            matTerrain.setFloat("Shininess", 0);

            // GRASS texture
            Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
            grass.setWrap(WrapMode.Repeat);
            matTerrain.setTexture("DiffuseMap", grass);
            matTerrain.setFloat("DiffuseMap_0_scale", grassScale);

            // CREATE THE TERRAIN
            terrain1 = new TerrainQuad("terrain 1", 65, 513, null);
            terrain1.setMaterial(matTerrain);
            terrain1.setLocalTranslation(-256, -100, -256);
            terrain1.setLocalScale(1f, 1f, 1f);
            this.attachChild(terrain1);

            terrain2 = new TerrainQuad("terrain 2", 65, 513, null);
            terrain2.setMaterial(matTerrain);
            terrain2.setLocalTranslation(-256, -100, 256);
            terrain2.setLocalScale(1f, 1f, 1f);
            this.attachChild(terrain2);

            terrain3 = new TerrainQuad("terrain 3", 65, 513, null);
            terrain3.setMaterial(matTerrain);
            terrain3.setLocalTranslation(256, -100, -256);
            terrain3.setLocalScale(1f, 1f, 1f);
            this.attachChild(terrain3);

            terrain4 = new TerrainQuad("terrain 4", 65, 513, null);
            terrain4.setMaterial(matTerrain);
            terrain4.setLocalTranslation(256, -100, 256);
            terrain4.setLocalScale(1f, 1f, 1f);
            this.attachChild(terrain4);
            
            terrain1.setNeighbourFinder(this);
            terrain2.setNeighbourFinder(this);
            terrain3.setNeighbourFinder(this);
            terrain4.setNeighbourFinder(this);
            
            MultiTerrainLodControl lodControl = new MultiTerrainLodControl(getCamera());
            lodControl.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
            lodControl.addTerrain(terrain1);
            lodControl.addTerrain(terrain2);
            lodControl.addTerrain(terrain3);// order of these seems to matter
            lodControl.addTerrain(terrain4);
            this.addControl(lodControl);
            
        }
        
        /**
         * 1  3
         * 2  4
         */
        public TerrainQuad getRightQuad(TerrainQuad center) {
            //System.out.println("lookup neighbour");
            if (center == terrain1)
                return terrain3;
            if (center == terrain2)
                return terrain4;
            
            return null;
        }

        /**
         * 1  3
         * 2  4
         */
        public TerrainQuad getLeftQuad(TerrainQuad center) {
            //System.out.println("lookup neighbour");
            if (center == terrain3)
                return terrain1;
            if (center == terrain4)
                return terrain2;
            
            return null;
        }

        /**
         * 1  3
         * 2  4
         */
        public TerrainQuad getTopQuad(TerrainQuad center) {
            //System.out.println("lookup neighbour");
            if (center == terrain2)
                return terrain1;
            if (center == terrain4)
                return terrain3;
            
            return null;
        }

        /**
         * 1  3
         * 2  4
         */
        public TerrainQuad getDownQuad(TerrainQuad center) {
            //System.out.println("lookup neighbour");
            if (center == terrain1)
                return terrain2;
            if (center == terrain3)
                return terrain4;
            
            return null;
        }
        
        public float getHeight(Vector2f xz) {
            // you will have to offset the coordinate for each terrain, to center on it
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Vector3f getNormal(Vector2f xz) {
            // you will have to offset the coordinate for each terrain, to center on it
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public float getHeightmapHeight(Vector2f xz) {
            // you will have to offset the coordinate for each terrain, to center on it
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setHeight(Vector2f xzCoordinate, float height) {
            // you will have to offset the coordinate for each terrain, to center on it
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setHeight(List<Vector2f> xz, List<Float> height) {
            // you will have to offset the coordinate for each terrain, to center on it
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void adjustHeight(Vector2f xzCoordinate, float delta) {
            // you will have to offset the coordinate for each terrain, to center on it
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void adjustHeight(List<Vector2f> xz, List<Float> height) {
            // you will have to offset the coordinate for each terrain, to center on it
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public float[] getHeightMap() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int getMaxLod() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setLocked(boolean locked) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void generateEntropy(ProgressMonitor monitor) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Material getMaterial() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Material getMaterial(Vector3f worldLocation) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int getTerrainSize() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public int getNumMajorSubdivisions() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        
        
    }
}
