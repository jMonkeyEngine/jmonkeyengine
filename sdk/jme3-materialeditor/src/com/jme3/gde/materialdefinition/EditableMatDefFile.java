/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition;

import com.jme3.asset.AssetKey;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.materialdefinition.fileStructure.MatDefBlock;
import com.jme3.gde.materialdefinition.fileStructure.ShaderNodeBlock;
import com.jme3.gde.materialdefinition.fileStructure.TechniqueBlock;
import com.jme3.gde.materialdefinition.fileStructure.UberStatement;
import com.jme3.gde.materialdefinition.fileStructure.leaves.InputMappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.LeafStatement;
import com.jme3.gde.materialdefinition.fileStructure.leaves.MatParamBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.OutputMappingBlock;
import com.jme3.gde.materialdefinition.navigator.node.MatDefNode;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.material.plugins.MatParseException;
import com.jme3.shader.Glsl100ShaderGenerator;
import com.jme3.shader.Glsl150ShaderGenerator;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderGenerator;
import com.jme3.util.blockparser.BlockLanguageParser;
import com.jme3.util.blockparser.Statement;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import org.openide.cookies.EditorCookie;
import org.openide.explorer.ExplorerManager;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;

/**
 *
 * @author Nehon
 */
public class EditableMatDefFile {

    private FileObject matDefFile;
    private MatDefDataObject obj;
    private Material material;
    private MatDefBlock matDefStructure;
    private TechniqueBlock currentTechnique;
    private MaterialDef materialDef;
    private ProjectAssetManager assetManager;
//    MatParamTopComponent matParamComponent;
    private ShaderGenerator glsl100;
    private ShaderGenerator glsl150;
    private String selectedTechnique = "Default";
    private final static String GLSL100 = "GLSL100";
    private final static String GLSL150 = "GLSL150";
    private Lookup lookup;
    private boolean loaded = false;
    private boolean dirty = false;

    public EditableMatDefFile(Lookup lookup) {
        obj = lookup.lookup(MatDefDataObject.class);
        load(lookup);

    }

    public final void load(Lookup lookup) {
        this.matDefFile = obj.getPrimaryFile();
        this.assetManager = lookup.lookup(ProjectAssetManager.class);
        this.glsl100 = new Glsl100ShaderGenerator(assetManager);
        this.glsl150 = new Glsl150ShaderGenerator(assetManager);
        this.lookup = lookup;

        if (matDefStructure != null) {
            obj.getLookupContents().remove(matDefStructure);
            matDefStructure = null;
        }
        if (materialDef != null) {
            obj.getLookupContents().remove(materialDef);
            materialDef = null;
        }
        FileLock lock = null;
        try {
            lock = matDefFile.lock();
            List<Statement> sta = BlockLanguageParser.parse(obj.getPrimaryFile().getInputStream());
            matDefStructure = new MatDefBlock(sta.get(0));
            AssetKey<MaterialDef> matDefKey = new AssetKey<MaterialDef>(assetManager.getRelativeAssetPath(assetManager.getRelativeAssetPath(matDefFile.getPath())));
            assetManager.deleteFromCache(matDefKey);
            materialDef = (MaterialDef) assetManager.loadAsset(assetManager.getRelativeAssetPath(matDefFile.getPath()));
            lock.releaseLock();
        } catch (Exception ex) {
            Throwable t = ex.getCause();
            boolean matParseError = false;
            while (t != null) {
                if (t instanceof MatParseException) {
                    Logger.getLogger(EditableMatDefFile.class.getName()).log(Level.SEVERE, t.getMessage());
                    matParseError = true;
                }
                t = t.getCause();
            }
            if (!matParseError) {
                Exceptions.printStackTrace(ex);
            }
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
        if (materialDef != null) {
            currentTechnique = matDefStructure.getTechniques().get(0);
            registerListener(matDefStructure);

            obj.getLookupContents().add(matDefStructure);
            updateLookupWithMaterialData(obj);
            loaded = true;
        }
    }

    private void registerListener(Statement sta) {
        if (sta instanceof UberStatement) {
            ((UberStatement) sta).addPropertyChangeListener(WeakListeners.propertyChange(changeListener, ((UberStatement) sta)));
        } else if (sta instanceof LeafStatement) {
            ((LeafStatement) sta).addPropertyChangeListener(WeakListeners.propertyChange(changeListener, ((LeafStatement) sta)));
        }
        if (sta.getContents() != null) {
            for (Statement statement : sta.getContents()) {
                registerListener(statement);
            }
        }
    }

    public void buildOverview(ExplorerManager mgr) {
        if (materialDef != null) {
            mgr.setRootContext(new MatDefNode(lookup));

        } else {
            mgr.setRootContext(Node.EMPTY);
        }
    }

    public String getShaderCode(String version, Shader.ShaderType type) {
        try {
            material.selectTechnique("Default", SceneApplication.getApplication().getRenderManager());
            Shader s;
            if (version.equals(GLSL100)) {
                glsl100.initialize(material.getActiveTechnique());
                s = glsl100.generateShader();
            } else {
                glsl150.initialize(material.getActiveTechnique());
                s = glsl150.generateShader();
            }
            for (Iterator<Shader.ShaderSource> it = s.getSources().iterator(); it.hasNext();) {
                Shader.ShaderSource source = it.next();
                if (source.getType() == type) {
                    return source.getSource();
                }
            }
            return "";
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
            return "error generating shader " + e.getMessage();
        }
    }

//    public MatParamTopComponent getMatParamComponent() {
//        return matParamComponent;
//    }
//    
//    public void setMatParamComponent(MatParamTopComponent matParamComponent) {
//        this.matParamComponent = matParamComponent;
//    }
    public TechniqueBlock getCurrentTechnique() {
        return currentTechnique;
    }

    public MatDefBlock getMatDefStructure() {
        return matDefStructure;
    }
    private MatStructChangeListener changeListener = new MatStructChangeListener();
    J3MLoader loader = new J3MLoader();

    private void updateLookupWithMaterialData(MatDefDataObject obj) {
        obj.getLookupContents().add(materialDef);
        material = new Material(materialDef);

        try {
            material.selectTechnique("Default", SceneApplication.getApplication().getRenderManager());
            if (matToRemove != null) {
                for (MatParam matParam : matToRemove.getParams()) {
                    material.setParam(matParam.getName(), matParam.getVarType(), matParam.getValue());
                }
                obj.getLookupContents().remove(matToRemove);
                matToRemove = null;
            }
            obj.getLookupContents().add(material);
        } catch (Exception e) {
            Logger.getLogger(EditableMatDefFile.class.getName()).log(Level.WARNING, "Error making material {0}", e.getMessage());
            material = matToRemove;
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    private class MatStructChangeListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() instanceof ShaderNodeBlock && evt.getPropertyName().equals("name")) {
                String oldValue = (String) evt.getOldValue();
                String newValue = (String) evt.getNewValue();
                for (ShaderNodeBlock shaderNodeBlock : currentTechnique.getShaderNodes()) {
                    List<InputMappingBlock> lin = shaderNodeBlock.getInputs();
                    if (lin != null) {
                        for (InputMappingBlock inputMappingBlock : shaderNodeBlock.getInputs()) {
                            if (inputMappingBlock.getLeftNameSpace().equals(oldValue)) {
                                inputMappingBlock.setLeftNameSpace(newValue);
                            }
                            if (inputMappingBlock.getRightNameSpace().equals(oldValue)) {
                                inputMappingBlock.setRightNameSpace(newValue);
                            }
                        }
                    }
                    List<OutputMappingBlock> l = shaderNodeBlock.getOutputs();
                    if (l != null) {
                        for (OutputMappingBlock outputMappingBlock : l) {
                            if (outputMappingBlock.getRightNameSpace().equals(oldValue)) {
                                outputMappingBlock.setRightNameSpace(newValue);
                            }
                        }
                    }
                }
            }
            if (evt.getPropertyName().equals(MatDefBlock.REMOVE_MAT_PARAM)) {
                MatParamBlock oldValue = (MatParamBlock) evt.getOldValue();

                for (ShaderNodeBlock shaderNodeBlock : currentTechnique.getShaderNodes()) {

                    if (shaderNodeBlock.getCondition() != null && shaderNodeBlock.getCondition().contains(oldValue.getName())) {
                        shaderNodeBlock.setCondition(shaderNodeBlock.getCondition().replaceAll(oldValue.getName(), "").trim());                      
                    }
                    List<InputMappingBlock> lin = shaderNodeBlock.getInputs();
                    if (lin != null) {
                        for (InputMappingBlock inputMappingBlock : shaderNodeBlock.getInputs()) {
                            if (inputMappingBlock.getCondition() != null && inputMappingBlock.getCondition().contains(oldValue.getName())) {
                                inputMappingBlock.setCondition(inputMappingBlock.getCondition().replaceAll(oldValue.getName(), "").trim());                               
                            }
                        }
                    }
                    List<OutputMappingBlock> l = shaderNodeBlock.getOutputs();
                    if (l != null) {
                        for (OutputMappingBlock outputMappingBlock : l) {
                            if (outputMappingBlock.getCondition() != null && outputMappingBlock.getCondition().contains(oldValue.getName())) {
                                outputMappingBlock.setCondition(outputMappingBlock.getCondition().replaceAll(oldValue.getName(), "").trim());                             
                            }
                        }
                    }
                }
            }
            if (evt.getPropertyName().equals(MatDefBlock.ADD_MAT_PARAM)
                    || evt.getPropertyName().equals(TechniqueBlock.ADD_SHADER_NODE)
                    || evt.getPropertyName().equals(ShaderNodeBlock.ADD_MAPPING)) {
                registerListener((Statement) evt.getNewValue());
            }
            applyChange();
        }
    }
    Material matToRemove;

    private void applyChange() {

        try {
            EditorCookie ec = lookup.lookup(EditorCookie.class);
            final StyledDocument doc = ec.getDocument();
            final BadLocationException[] exc = new BadLocationException[]{null};
            NbDocument.runAtomicAsUser(ec.getDocument(), new Runnable() {
                public void run() {
                    try {
                        doc.remove(0, doc.getLength());
                        doc.insertString(doc.getLength(),
                                matDefStructure.toString(),
                                SimpleAttributeSet.EMPTY);
                    } catch (BadLocationException e) {
                        exc[0] = e;
                    }
                }
            });
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        AssetKey<MaterialDef> key = new AssetKey<MaterialDef>(assetManager.getRelativeAssetPath(matDefFile.getPath()));
        obj.getLookupContents().remove(materialDef);
        matToRemove = material;

        List<Statement> l = new ArrayList<Statement>();
        l.add(matDefStructure);
        try {
            materialDef = loader.loadMaterialDef(l, assetManager, key);
        } catch (IOException ex) {
            Logger.getLogger(EditableMatDefFile.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
        updateLookupWithMaterialData(obj);
    }
}
