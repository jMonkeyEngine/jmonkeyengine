/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materials;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.MaterialKey;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.PreviewRequest;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneListener;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RendererException;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.MaterialDebugAppState;
import com.jme3.util.TangentBinormalGenerator;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 *
 * @author Nehon
 */
public class MaterialPreviewRenderer implements SceneListener {
    
    private Geometry sphere;
    private Geometry box;
    private Geometry quad;
    private Geometry currentGeom;
    private Material currentMaterial;
    private boolean init = false;
    private final JLabel label;
    
    public enum DisplayType {
        
        Sphere,
        Box,
        Quad
    }
    
    public MaterialPreviewRenderer(JLabel label) {
        this.label = label;
    }
    
    private void init() {
        SceneApplication.getApplication().addSceneListener(this);
        Sphere sphMesh = new Sphere(32, 32, 2.5f);
        sphMesh.setTextureMode(Sphere.TextureMode.Projected);
        sphMesh.updateGeometry(32, 32, 2.5f, false, false);
        TangentBinormalGenerator.generate(sphMesh);
        sphere = new Geometry("previewSphere", sphMesh);
        sphere.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_X));
        
        Box boxMesh = new Box(1.75f, 1.75f, 1.75f);
        TangentBinormalGenerator.generate(boxMesh);
        box = new Geometry("previewBox", boxMesh);
        box.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.DEG_TO_RAD * 30, Vector3f.UNIT_X).multLocal(new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Y)));
        
        Quad quadMesh = new Quad(4.5f, 4.5f);
        TangentBinormalGenerator.generate(quadMesh);
        quad = new Geometry("previewQuad", quadMesh);
        quad.setLocalTranslation(new Vector3f(-2.25f, -2.25f, 0));
        currentGeom = sphere;
        init = true;
    }
    
    @SuppressWarnings("unchecked")
    public void showMaterial(ProjectAssetManager assetManager, String materialFileName) {
        if (!init) {
            init();
        }        
        MaterialKey key = new MaterialKey(assetManager.getRelativeAssetPath(materialFileName));
        assetManager.deleteFromCache(key);
        Material mat = (Material) assetManager.loadAsset(key);
        if (mat != null) {
            showMaterial(mat);
        }
        
    }
    
    public void showMaterial(final Material m) {
        if (!init) {
            init();
        }
        final DesktopAssetManager assetManager = (DesktopAssetManager) SceneApplication.getApplication().getAssetManager();
        SceneApplication.getApplication().enqueue(new Callable<Material>() {

            public Material call() throws Exception {
                final Material mat = reloadMaterial(m, assetManager);
                if (mat != null) {
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            currentMaterial = mat;
                            currentGeom.setMaterial(mat);
                            try {
                                if (currentGeom.getMaterial() != null) {
                                    PreviewRequest request = new PreviewRequest(MaterialPreviewRenderer.this, currentGeom, label.getWidth(), label.getHeight());
                                    request.getCameraRequest().setLocation(new Vector3f(0, 0, 7));
                                    request.getCameraRequest().setLookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
                                    SceneApplication.getApplication().createPreview(request);
                                }
                            } catch (Exception e) {
                                Logger.getLogger(MaterialPreviewRenderer.class.getName()).log(Level.SEVERE, "Error rendering material" + e.getMessage());
                            }
                        }
                    });

                }
                return mat;
            }
        });

    }
    
      public Material reloadMaterial(Material mat, DesktopAssetManager assetManager) {

        MaterialKey key = new MaterialKey(mat.getMaterialDef().getAssetName());
        assetManager.deleteFromCache(key);
        
        //creating a dummy mat with the mat def of the mat to reload
        Material dummy = new Material(mat.getMaterialDef());

        for (MatParam matParam : mat.getParams()) {
            dummy.setParam(matParam.getName(), matParam.getVarType(), matParam.getValue());
        }
        
        dummy.getAdditionalRenderState().set(mat.getAdditionalRenderState());        

        //creating a dummy geom and assigning the dummy material to it
        Geometry dummyGeom = new Geometry("dummyGeom", new Box(1f, 1f, 1f));
        dummyGeom.setMaterial(dummy);

        try {
            //preloading the dummyGeom, this call will compile the shader again
           SceneApplication.getApplication().getRenderManager().preloadScene(dummyGeom);
        } catch (RendererException e) {
            //compilation error, the shader code will be output to the console
            //the following code will output the error
            //System.err.println(e.getMessage());
            Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.SEVERE, e.getMessage());
            return null;
        }

        //Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.INFO, "Material succesfully reloaded");
        //System.out.println("Material succesfully reloaded");
        return dummy;
    }
    
    
    public void switchDisplay(DisplayType type) {
        switch (type) {
            case Box:
                currentGeom = box;
                break;
            case Sphere:
                currentGeom = sphere;
                break;
            case Quad:
                currentGeom = quad;
                break;
        }
        showMaterial(currentMaterial);
    }
    
    public void sceneOpened(SceneRequest request) {
    }
    
    public void sceneClosed(SceneRequest request) {
    }
    
    public void previewCreated(PreviewRequest request) {
        if (request.getRequester() == this) {
            final ImageIcon icon = new ImageIcon(request.getImage());
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    label.setIcon(icon);
                }
            });
        }
    }
    
    public void cleanUp() {
        SceneApplication.getApplication().removeSceneListener(this);
    }
}
