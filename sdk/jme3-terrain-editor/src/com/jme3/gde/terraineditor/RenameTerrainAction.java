/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.terraineditor;

import com.jme3.asset.TextureKey;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeTerrainQuad;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractToolWizardAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ToolAction;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Texture;
import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

/**
 * Rename the alpha-maps.
 * 
 * @author bowens
 */
@org.openide.util.lookup.ServiceProvider(service = ToolAction.class)
public class RenameTerrainAction extends AbstractToolWizardAction {

    private String oldName;
    private String newName;
    
    public RenameTerrainAction() {
        name = "Rename Terrain Alphamaps";
    }
    
    public Class<?> getNodeClass() {
        return JmeTerrainQuad.class;
    }

    @Override
    protected Object showWizard(Node node) {
        AbstractSceneExplorerNode rootNode = (AbstractSceneExplorerNode) node;
        TerrainQuad quad = rootNode.getLookup().lookup(TerrainQuad.class);
        MatParam param = quad.getMaterial().getParam("AlphaMap");
        if (param == null)
            oldName = null;
        else {
            String[] splita = param.getValueAsString().split("/");
            String[] split = splita[splita.length-1].split("-alphablend0.png");
            String first = split[0];
            oldName = first;
        }
        List<WizardDescriptor.Panel<WizardDescriptor>> panels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
        panels.add(new RenameTerrainWizardPanel1());
        for (int i = 0; i < panels.size(); i++) {
            Component c = panels.get(i).getComponent();
            if (c instanceof JComponent) { // assume Swing components
                JComponent jc = (JComponent) c;
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
            }
        }
        WizardDescriptor wiz = new WizardDescriptor(new WizardDescriptor.ArrayIterator<WizardDescriptor>(panels));
        // {0} will be replaced by WizardDesriptor.Panel.getComponent().getName()
        wiz.setTitleFormat(new MessageFormat("{0}"));
        wiz.setTitle("Rename the alphamaps of the terrain");
        wiz.putProperty("oldName", oldName);

        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {
            return wiz;
        }
        return null;
    }

    @Override
    protected Object doApplyTool(AbstractSceneExplorerNode rootNode, Object settings) {
        WizardDescriptor wiz = (WizardDescriptor) settings;
        if (wiz == null || wiz.getProperties() == null || wiz.getProperties().get("newName") == null)
            return null;
        newName = cleanFileName( wiz.getProperties().get("newName").toString() );
        if (newName == null)
            return null;
        
        TerrainQuad quad = rootNode.getLookup().lookup(TerrainQuad.class);
        rename(quad, oldName, newName);
        
        return quad;
    }

    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {
        TerrainQuad quad = rootNode.getLookup().lookup(TerrainQuad.class);
        rename(quad, newName, oldName);
    }
    
    private void rename(TerrainQuad quad, String prevName, String newName) {
        
        ProjectAssetManager manager = (ProjectAssetManager) SceneApplication.getApplication().getAssetManager();
        String texFolder =  "Textures/terrain-alpha/";
        
        
        // rename the files
        String texFolderFilePath = manager.getAssetFolderName() +"/"+ texFolder;
        String prevPath0 = texFolderFilePath+prevName+"-alphablend0.png";
        String prevPath1 = texFolderFilePath+prevName+"-alphablend1.png";
        String prevPath2 = texFolderFilePath+prevName+"-alphablend2.png";
        String newPath0 = texFolderFilePath+newName+"-alphablend0.png";
        String newPath1 = texFolderFilePath+newName+"-alphablend1.png";
        String newPath2 = texFolderFilePath+newName+"-alphablend2.png";
        try {
            File f0_a = new File(prevPath0);
            File f0_b = new File(newPath0);
            copyFile(f0_a, f0_b);
            //f0_a.delete();
            
            File f1_a = new File(prevPath1);
            File f1_b = new File(newPath1);
            copyFile(f1_a, f1_b);
            //f1_a.delete();
            
            File f2_a = new File(prevPath2);
            File f2_b = new File(newPath2);
            copyFile(f2_a, f2_b);
            //f2_a.delete();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        
        // set the new mat params
        String alphaPath0 = texFolder+newName+"-alphablend0.png";
        String alphaPath1 = texFolder+newName+"-alphablend1.png";
        String alphaPath2 = texFolder+newName+"-alphablend2.png";
        
        TextureKey tk0 = (TextureKey) manager.loadTexture(texFolder+prevName+"-alphablend0.png").getKey();
        TextureKey tk1 = (TextureKey) manager.loadTexture(texFolder+prevName+"-alphablend1.png").getKey();
        TextureKey tk2 = (TextureKey) manager.loadTexture(texFolder+prevName+"-alphablend2.png").getKey();
        
        Texture tex0 = manager.loadTexture(cloneKeyParams(tk0, alphaPath0) );
        Texture tex1 = manager.loadTexture(cloneKeyParams(tk1, alphaPath1) );
        Texture tex2 = manager.loadTexture(cloneKeyParams(tk2, alphaPath2) );
        
        Material material = quad.getMaterial();
        material.setTexture("AlphaMap", tex0);
        material.setTexture("AlphaMap_1", tex1);
        material.setTexture("AlphaMap_2", tex2);
    }
    
    private void copyFile(File sourceFile, File destFile) throws IOException {
        if(!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
    }

    private String cleanFileName(String name) {
        if (name == null)
            return null;
        
        return name.replaceAll("[\\W]|_", "");   
    }

    private TextureKey cloneKeyParams(TextureKey tkOrig, String path) {
        TextureKey tk = new TextureKey(path, false);
        tk.setAnisotropy(tkOrig.getAnisotropy());
        tk.setGenerateMips(tkOrig.isGenerateMips());
        tk.setTextureTypeHint(tkOrig.getTextureTypeHint());
        return tk;
    }
}
