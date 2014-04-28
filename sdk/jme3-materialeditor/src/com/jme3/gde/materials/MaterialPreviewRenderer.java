/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materials;

import com.jme3.asset.MaterialKey;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.PreviewRequest;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneListener;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TangentBinormalGenerator;
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
    private JLabel label;
    
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
        try {
            MaterialKey key = new MaterialKey(assetManager.getRelativeAssetPath(materialFileName));
            assetManager.deleteFromCache(key);
            Material mat = (Material) assetManager.loadAsset(key);
            if (mat != null) {
                currentMaterial = mat;
                showMaterial(mat);
            }
        } catch (Exception e) {
        }
    }
    
    public void showMaterial(Material m) {
        if (!init) {
            init();
        }
        currentGeom.setMaterial(m);
        try {
            if (currentGeom.getMaterial() != null) {
                PreviewRequest request = new PreviewRequest(this, currentGeom, label.getWidth(), label.getHeight());
                request.getCameraRequest().setLocation(new Vector3f(0, 0, 7));
                request.getCameraRequest().setLookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);
                SceneApplication.getApplication().createPreview(request);
            }
        } catch (Exception e) {
            Logger.getLogger(MaterialPreviewRenderer.class.getName()).log(Level.SEVERE, "Error rendering material" + e.getMessage());
        }
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
