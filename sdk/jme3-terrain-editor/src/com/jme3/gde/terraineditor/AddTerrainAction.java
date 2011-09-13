/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.terraineditor;

import com.jme3.asset.TextureKey;
import com.jme3.bounding.BoundingBox;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractNewSpatialWizardAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.NewSpatialAction;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import jme3tools.converters.ImageToAwt;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = NewSpatialAction.class)
public class AddTerrainAction extends AbstractNewSpatialWizardAction {

    private WizardDescriptor.Panel[] panels;

    public AddTerrainAction() {
        name = "Terrain..";
    }

    @Override
    protected Object showWizard(org.openide.nodes.Node node) {
        WizardDescriptor wizardDescriptor = new WizardDescriptor(getPanels());
        wizardDescriptor.setTitleFormat(new MessageFormat("{0}"));
        wizardDescriptor.setTitle("Terrain Wizard");
        wizardDescriptor.putProperty("main_node", node);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
        dialog.setVisible(true);
        dialog.toFront();
        boolean cancelled = wizardDescriptor.getValue() != WizardDescriptor.FINISH_OPTION;
        if (!cancelled) {
            return wizardDescriptor;
        }
        return null;
    }

    @Override
    protected Spatial doCreateSpatial(Node parent, Object properties) {
        if (properties != null) {
            try {
                return generateTerrain(parent, (WizardDescriptor) properties);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }
    
    protected Spatial generateTerrain(Node parent, final WizardDescriptor wizardDescriptor) throws IOException {
        org.openide.nodes.Node selectedNode = (org.openide.nodes.Node) wizardDescriptor.getProperty("main_node");
        final Spatial spatial = selectedNode.getLookup().lookup(Spatial.class);
        

        String sceneName = selectedNode.getLookup().lookup(DataObject.class).getName();

        int totalSize = (Integer) wizardDescriptor.getProperty("totalSize");
        int patchSize = (Integer) wizardDescriptor.getProperty("patchSize");
        int alphaTextureSize = (Integer) wizardDescriptor.getProperty("alphaTextureSize");

        float[] heightmapData = null;
        AbstractHeightMap heightmap = (AbstractHeightMap) wizardDescriptor.getProperty("abstractHeightMap");
        if (heightmap != null) {
            heightmap.load(); // can take a while
            heightmapData = heightmap.getHeightMap();
        }

        return doCreateTerrain(parent, totalSize, patchSize, alphaTextureSize, heightmapData, sceneName, selectedNode);
    }
    
    
    protected Spatial doCreateTerrain(Node parent,
                                    int totalSize,
                                    int patchSize,
                                    int alphaTextureSize,
                                    float[] heightmapData,
                                    String sceneName,
                                    org.openide.nodes.Node selectedNode) throws IOException
    {
        final ProjectAssetManager manager = selectedNode.getLookup().lookup(ProjectAssetManager.class);
        
        Terrain terrain = new TerrainQuad("terrain-"+sceneName, patchSize, totalSize, heightmapData); //TODO make this pluggable for different Terrain implementations
        com.jme3.material.Material mat = new com.jme3.material.Material(manager, "Common/MatDefs/Terrain/TerrainLighting.j3md");

        String assetFolder = "";
        if (manager != null && manager instanceof ProjectAssetManager)
            assetFolder = ((ProjectAssetManager)manager).getAssetFolderName();

        // write out 3 alpha blend images
        for (int i=0; i<TerrainEditorController.NUM_ALPHA_TEXTURES; i++) {
            BufferedImage alphaBlend = new BufferedImage(alphaTextureSize, alphaTextureSize, BufferedImage.TYPE_INT_ARGB);
            if (i == 0) {
                // the first alpha level should be opaque so we see the first texture over the whole terrain
                for (int h=0; h<alphaTextureSize; h++)
                    for (int w=0; w<alphaTextureSize; w++)
                        alphaBlend.setRGB(w, h, 0x00FF0000);//argb
            }
            File alphaFolder = new File(assetFolder+"/Textures/terrain-alpha/");
            if (!alphaFolder.exists())
                alphaFolder.mkdir();
            String alphaBlendFileName = "/Textures/terrain-alpha/"+sceneName+"-"+((Node)terrain).getName()+"-alphablend"+i+".png";
            File alphaImageFile = new File(assetFolder+alphaBlendFileName);
            ImageIO.write(alphaBlend, "png", alphaImageFile);
            Texture tex = manager.loadAsset(new TextureKey(alphaBlendFileName, false));
            if (i == 0)
                mat.setTexture("AlphaMap", tex);
            else if (i == 1)
                mat.setTexture("AlphaMap_1", tex);
            else if (i == 2)
                mat.setTexture("AlphaMap_2", tex);
        }
        
        Texture defaultTexture = manager.loadTexture(TerrainEditorController.DEFAULT_TERRAIN_TEXTURE);
        
        // copy the default texture to the assets folder if it doesn't exist there yet
        String dirtTextureName = "/Textures/dirt.jpg";
        File dirtTextureFile = new File(assetFolder+dirtTextureName);
        if (!dirtTextureFile.exists()) {
            BufferedImage bi = ImageToAwt.convert(defaultTexture.getImage(), false, true, 0);
            ImageIO.write(bi, "jpg", dirtTextureFile);
        }
        // give the first layer default texture
        Texture dirtTexture = manager.loadTexture(dirtTextureName);
        dirtTexture.setWrap(WrapMode.Repeat);
        mat.setTexture("DiffuseMap", dirtTexture);
        mat.setFloat("DiffuseMap_0_scale", TerrainEditorController.DEFAULT_TEXTURE_SCALE);
        mat.setBoolean("WardIso", true);

        ((Node)terrain).setMaterial(mat);
        ((Node)terrain).setModelBound(new BoundingBox());
        ((Node)terrain).updateModelBound();
        ((Node)terrain).setLocalTranslation(0, 0, 0);
        ((Node)terrain).setLocalScale(1f, 1f, 1f);

        // add the lod control
        TerrainLodControl control = new TerrainLodControl(terrain, SceneApplication.getApplication().getCamera());
        control.setLodCalculator(new DistanceLodCalculator(patchSize, 2.7f));
	((Node)terrain).addControl(control);

        parent.attachChild((Node)terrain);

        //setNeedsSave(true);
        //addSpatialUndo(parent, (Node)terrain, jmeNodeParent);
        
        return (Spatial)terrain;
    }

    /**
     * Initialize panels representing individual wizard's steps and sets
     * various properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel[] getPanels() {
        if (panels == null) {
            panels = new WizardDescriptor.Panel[]{
                new CreateTerrainWizardPanel1(),
                new CreateTerrainWizardPanel2(),
                new CreateTerrainWizardPanel3()
            };
            String[] steps = new String[panels.length];
            for (int i = 0; i < panels.length; i++) {
                Component c = panels[i].getComponent();
                // Default step name to component name of panel. Mainly useful
                // for getting the name of the target chooser to appear in the
                // list of steps.
                steps[i] = c.getName();
                if (c instanceof JComponent) { // assume Swing components
                    JComponent jc = (JComponent) c;
                    // Sets step number of a component
                    // TODO if using org.openide.dialogs >= 7.8, can use WizardDescriptor.PROP_*:
                    jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer(i));
                    // Sets steps names for a panel
                    jc.putClientProperty("WizardPanel_contentData", steps);
                    // Turn on subtitle creation on each step
                    jc.putClientProperty("WizardPanel_autoWizardStyle", Boolean.TRUE);
                    // Show steps on the left side with the image on the background
                    jc.putClientProperty("WizardPanel_contentDisplayed", Boolean.TRUE);
                    // Turn on numbering of all steps
                    jc.putClientProperty("WizardPanel_contentNumbered", Boolean.TRUE);
                }
            }
        }
        return panels;
    }
}
