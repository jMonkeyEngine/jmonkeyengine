/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import com.jme3.scene.Node;
import com.jme3.terrain.ProgressMonitor;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.NeighbourFinder;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import java.util.List;

/**
 * Demonstrates the NeighbourFinder interface for TerrainQuads,
 * allowing you to tile terrains together without having to use
 * TerrainGrid.
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
        cam.lookAtDirection(new Vector3f(0, -1f, 0).normalizeLocal(), Vector3f.UNIT_X);
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
            terrain1 = new TerrainQuad("terrain", 65, 513, null);
            TerrainLodControl control1 = new TerrainLodControl(terrain1, getCamera());
            control1.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
            terrain1.addControl(control1);
            terrain1.setMaterial(matTerrain);
            terrain1.setLocalTranslation(-256, -100, -256);
            terrain1.setLocalScale(1f, 1f, 1f);
            this.attachChild(terrain1);

            terrain2 = new TerrainQuad("terrain", 65, 513, null);
            TerrainLodControl control2 = new TerrainLodControl(terrain2, getCamera());
            control2.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
            terrain2.addControl(control2);
            terrain2.setMaterial(matTerrain);
            terrain2.setLocalTranslation(-256, -100, 256);
            terrain2.setLocalScale(1f, 1f, 1f);
            this.attachChild(terrain2);

            terrain3 = new TerrainQuad("terrain", 65, 513, null);
            TerrainLodControl control3 = new TerrainLodControl(terrain3, getCamera());
            control3.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
            terrain3.addControl(control3);
            terrain3.setMaterial(matTerrain);
            terrain3.setLocalTranslation(256, -100, -256);
            terrain3.setLocalScale(1f, 1f, 1f);
            this.attachChild(terrain3);

            terrain4 = new TerrainQuad("terrain", 65, 513, null);
            TerrainLodControl control4 = new TerrainLodControl(terrain4, getCamera());
            control4.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
            terrain4.addControl(control4);
            terrain4.setMaterial(matTerrain);
            terrain4.setLocalTranslation(256, -100, 256);
            terrain4.setLocalScale(1f, 1f, 1f);
            this.attachChild(terrain4);
        }
        
        /**
         * 1  3
         * 2  4
         */
        public TerrainQuad getRightQuad(TerrainQuad center) {
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
