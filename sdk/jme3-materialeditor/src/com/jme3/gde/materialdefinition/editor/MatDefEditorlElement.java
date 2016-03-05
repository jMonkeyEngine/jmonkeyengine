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
package com.jme3.gde.materialdefinition.editor;

import com.jme3.asset.ShaderNodeDefinitionKey;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.materialdefinition.EditableMatDefFile;
import com.jme3.gde.materialdefinition.MatDefDataObject;
import com.jme3.gde.materialdefinition.MatDefMetaData;
import com.jme3.gde.materialdefinition.fileStructure.MatDefBlock;
import com.jme3.gde.materialdefinition.fileStructure.ShaderNodeBlock;
import com.jme3.gde.materialdefinition.fileStructure.TechniqueBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.InputMappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.MappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.MatParamBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.OutputMappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.WorldParamBlock;
import com.jme3.gde.materialdefinition.navigator.MatDefNavigatorPanel;
import com.jme3.gde.materialdefinition.utils.MaterialUtils;
import com.jme3.material.Material;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderNodeDefinition;
import com.jme3.shader.ShaderNodeVariable;
import com.jme3.shader.ShaderUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.awt.UndoRedo;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.TopComponent;

@MultiViewElement.Registration(
        displayName = "#LBL_MatDef_EDITOR",
        iconBase = "com/jme3/gde/materialdefinition/icons/matdef.png",
        mimeType = "text/jme-materialdefinition",
        persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
        preferredID = "MatDefVisual",
        position = 2000)
@Messages("LBL_MatDef_EDITOR=Editor")
public final class MatDefEditorlElement extends JPanel implements MultiViewElement {

    protected MatDefDataObject obj;
    private final MatDefEditorToolBar toolbar = new MatDefEditorToolBar();
    private transient MultiViewElementCallback callback;
    InstanceContent content;
    Selectable prevNode;
    MatDefMetaData metaData;

    @SuppressWarnings("LeakingThisInConstructor")
    public MatDefEditorlElement(final Lookup lkp) {
        initComponents();
        obj = lkp.lookup(MatDefDataObject.class);
        metaData = lkp.lookup(MatDefMetaData.class);
        assert obj != null;
        final EditableMatDefFile file = obj.getEditableFile();
        shaderEditPanel1.setVisible(false);
        shaderEditPanel1.setParent(this);
        if(!file.isLoaded()){
            file.load(lkp);
        }
        reload(file, lkp);        
        toolbar.setParent(this);
        toolbar.addTechnique(lkp.lookup(MatDefBlock.class).getTechniques());
    }

    private void initDiagram(Lookup lkp) throws NumberFormatException {

        diagram1.clear();
        diagram1.setParent(this);

        Material mat = lkp.lookup(Material.class);

        ProjectAssetManager manager = obj.getLookup().lookup(ProjectAssetManager.class);
        final MatDefBlock matDef = obj.getLookup().lookup(MatDefBlock.class);
        TechniqueBlock technique = getTechnique();

        diagram1.setCurrentTechniqueName(technique.getName());

        List<ShaderNodeVariable> vertexGlobals = new ArrayList<ShaderNodeVariable>();
        List<ShaderNodeVariable> fragmentGlobals = new ArrayList<ShaderNodeVariable>();
        List<ShaderNodeVariable> attributes = new ArrayList<ShaderNodeVariable>();
        List<ShaderNodeVariable> uniforms = new ArrayList<ShaderNodeVariable>();
        initData(technique, manager, vertexGlobals, fragmentGlobals, attributes, matDef, uniforms);

        int i = 0;
        for (ShaderNodeBlock sn : technique.getShaderNodes()) {
            ShaderNodeDefinition def = MaterialUtils.loadShaderNodeDefinition(sn, manager);
            NodePanel np = new NodePanel(sn, def);
            diagram1.addNode(np);
            Point position = getPositionFromMetaData(np.getKey(), 150 * i + 20, 190);
            np.setLocation(position);
            sn.setSpatialOrder(np.getLocation().x);
            i++;
        }
        //  TechniqueDef tech = def.getDefaultTechniques().get(0);
        for (ShaderNodeVariable shaderNodeVariable : vertexGlobals) {
            OutBusPanel out = new OutBusPanel(shaderNodeVariable.getName(), Shader.ShaderType.Vertex);
            diagram1.addOutBus(out);
            Point position = getPositionFromMetaData(out.getKey(), 0, 125);
            out.setLocation(position);
        }

        i = 2;
        for (ShaderNodeVariable var : fragmentGlobals) {
            OutBusPanel out2 = new OutBusPanel(var.getName(), Shader.ShaderType.Fragment);
            diagram1.addOutBus(out2);
            Point position = getPositionFromMetaData(out2.getKey(), 0, 150 * i + 190);
            out2.setLocation(position);
            i++;
        }
        i = 0;
        for (ShaderNodeVariable shaderNodeVariable : attributes) {
            NodePanel np = diagram1.getNodePanel(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
            if (np == null) {
                np = new NodePanel(shaderNodeVariable, NodePanel.NodeType.Attribute);
                diagram1.addNode(np);
                Point position = getPositionFromMetaData(np.getKey(), 150 * i + 20, 5);
                np.setLocation(position);
                i++;
            }
        }
        i = 0;
        for (ShaderNodeVariable shaderNodeVariable : uniforms) {
            NodePanel np = diagram1.getNodePanel(shaderNodeVariable.getNameSpace() + "." + shaderNodeVariable.getName());
            if (np == null) {
                np = new NodePanel(shaderNodeVariable, shaderNodeVariable.getNameSpace().equals("MatParam") ? NodePanel.NodeType.MatParam : NodePanel.NodeType.WorldParam);
                diagram1.addNode(np);
                Point position = getPositionFromMetaData(np.getKey(), 150 * i + 20, 65);
                np.setLocation(position);

                i++;
            }
        }

        for (ShaderNodeBlock sn : technique.getShaderNodes()) {
            //NodePanel np = diagram1.getNodePanel(sn.getName());
            List<InputMappingBlock> ins = sn.getInputs();
            if (ins != null) {
                for (InputMappingBlock mapping : ins) {
                    makeConnection(mapping);
                    if (!mapping.getRightNameSpace().equals("Global")
                            && !mapping.getRightNameSpace().equals("MatParam")
                            && !mapping.getRightNameSpace().equals("Attribute")
                            && !mapping.getRightNameSpace().equals("WorldParam")) {
                        sn.addInputNode(mapping.getRightNameSpace());
                    } else if (mapping.getRightNameSpace().equals("Global")) {
                        sn.setGlobalInput(true);
                    }
                }
            }
            List<OutputMappingBlock> outs = sn.getOutputs();
            if (outs != null) {
                for (OutputMappingBlock mapping : outs) {
                    makeConnection(mapping);
                    if (mapping.getLeftNameSpace().equals("Global")) {
                        sn.setGlobalOutput(true);
                    }
                }
            }

        }

        diagram1.setPreferredSize(new Dimension(jScrollPane1.getWidth() - 2, jScrollPane1.getHeight() - 2));
        diagram1.revalidate();
        jScrollPane1.addComponentListener(diagram1);

        
        diagram1.refreshPreviews(mat,obj.getEditableFile().getCurrentTechnique().getName());
        final Lookup.Result<Material> resMat = obj.getLookup().lookupResult(Material.class);
        resMat.addLookupListener(new LookupListener() {
            public void resultChanged(LookupEvent ev) {
                Collection<? extends Material> col = (Collection<? extends Material>) resMat.allInstances();
                if (!col.isEmpty()) {
                    Material material = col.iterator().next();                    
                    diagram1.refreshPreviews(material,obj.getEditableFile().getCurrentTechnique().getName());
                }
            }
        });

        final MatDefNavigatorPanel nav = obj.getLookup().lookup(MatDefNavigatorPanel.class);
        if (nav != null) {

            Lookup.Result<Selectable> res = nav.getLookup().lookupResult(Selectable.class);
            res.addLookupListener(new LookupListener() {
                public void resultChanged(LookupEvent ev) {
                    Selectable selected = nav.getLookup().lookup(Selectable.class);
                    if (selected != null && (prevNode == null || !(selected.getKey().equals(prevNode.getKey())))) {
                        prevNode = diagram1.select(selected.getKey());
                    }

                }
            });
        }
    }

    public void switchTechnique(TechniqueBlock tech) {
        obj.getEditableFile().setCurrentTechnique(tech);        
        reload(obj.getEditableFile(), obj.getLookup());
    }

    public Diagram getDiagram() {
        return diagram1;
    }
    
    

    @Override
    public String getName() {
        return "MatDefVisualElement";
    }

    protected void selectionChanged(Selectable selectable) {
        MatDefNavigatorPanel nav = obj.getLookup().lookup(MatDefNavigatorPanel.class);
        //It's possible that the navigator is null if it's collapsed in the ui.
        //In that case we early return to avoid further issues
        if(nav == null){
            return;
        }
        try {
            Node n = findNode(nav.getExplorerManager().getRootContext(), selectable.getKey());
            if (n == null) {
                n = nav.getExplorerManager().getRootContext();
            }
            prevNode = selectable;
            nav.getExplorerManager().setSelectedNodes(new Node[]{n});
            //FIXME this is hackish, each time it's used it spits a warning in the log.
            //without this line selecting a node in the editor select it in 
            //the navigator explorer but does not displays its property sheet.
            //the warning says to manipulate the MultiViewElement lookup, but it just voids the tree view             
            callback.getTopComponent().setActivatedNodes(new Node[]{n});

        } catch (PropertyVetoException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void refresh() {

        Lookup.Result<Material> resMat = obj.getLookup().lookupResult(Material.class);
        Collection<? extends Material> col = (Collection<? extends Material>) resMat.allInstances();
        if (!col.isEmpty()) {
            Material material = col.iterator().next();            
            diagram1.refreshPreviews(material,obj.getEditableFile().getCurrentTechnique().getName());
        }

    }

    public void setModified() {
        obj.setModified(true);
    }

    public ProjectAssetManager getAssetManager() {
        return obj.getLookup().lookup(ProjectAssetManager.class);
    }

    public void showShaderEditor(String nodeName, NodePanel.NodeType type, List<String> pathList) {

        List<FileObject> fos = new ArrayList<FileObject>();
        Map<String, String> readOnlyFiles = new HashMap<String, String>();
        for (String path : pathList) {
            try {
                FileObject text = FileUtil.toFileObject(new File(getAssetManager().getAbsoluteAssetPath(path)));
                fos.add(text);
            } catch (NullPointerException e) {
                try {
                    //cannot load the files because they are probably in a jar
                    InputStream is = getAssetManager().getResourceAsStream(path);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder out = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.append(line).append("\n");
                    }
                    readOnlyFiles.put(path.substring(path.lastIndexOf("/") + 1), out.toString());   //Prints the string content read from input stream
                    reader.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }

            }

        }
        shaderEditPanel1.setFiles(nodeName, type, fos, readOnlyFiles);
        shaderEditPanel1.revalidate();
        if (!shaderEditPanel1.isVisible() || jSplitPane.getDividerLocation() == jSplitPane.getMinimumDividerLocation()) {
            shaderEditPanel1.setVisible(true);
            jSplitPane.setDividerLocation(650);
        }

    }

    public ShaderEditPanel getShaderEditor() {
        return shaderEditPanel1;
    }

    private Node findNode(Node root, String key) {
        if (root instanceof Selectable) {
            Selectable s = (Selectable) root;
            if (s.getKey().equals(key)) {
                return root;
            } else if (root.getChildren() != Children.LEAF) {
                Node n;
                for (Node node : root.getChildren().getNodes()) {
                    n = findNode(node, key);
                    if (n != null) {
                        return n;
                    }
                }
            }
        }

        return null;

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane = new javax.swing.JSplitPane();
        shaderEditPanel1 = new com.jme3.gde.materialdefinition.editor.ShaderEditPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        diagram1 = new com.jme3.gde.materialdefinition.editor.Diagram();

        jSplitPane.setDividerLocation(1);
        jSplitPane.setDividerSize(6);

        shaderEditPanel1.setMinimumSize(new java.awt.Dimension(0, 0));
        jSplitPane.setLeftComponent(shaderEditPanel1);

        jScrollPane1.setMinimumSize(new java.awt.Dimension(0, 0));
        jScrollPane1.setName(""); // NOI18N

        diagram1.setBackground(new java.awt.Color(102, 102, 102));

        javax.swing.GroupLayout diagram1Layout = new javax.swing.GroupLayout(diagram1);
        diagram1.setLayout(diagram1Layout);
        diagram1Layout.setHorizontalGroup(
            diagram1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1591, Short.MAX_VALUE)
        );
        diagram1Layout.setVerticalGroup(
            diagram1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 782, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(diagram1);

        jSplitPane.setRightComponent(jScrollPane1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 333, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.jme3.gde.materialdefinition.editor.Diagram diagram1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane;
    private com.jme3.gde.materialdefinition.editor.ShaderEditPanel shaderEditPanel1;
    // End of variables declaration//GEN-END:variables

    @Override
    public JComponent getVisualRepresentation() {
        return this;
    }

    @Override
    public JComponent getToolbarRepresentation() {
        return toolbar;
    }

    @Override
    public Action[] getActions() {
        return new Action[0];
    }

    @Override
    public Lookup getLookup() {
        return obj.getLookup();
    }

    @Override
    public void componentOpened() {
        if (!obj.getEditableFile().isLoaded()) {
            obj.getEditableFile().load(obj.getLookup());
            reload(obj.getEditableFile(), obj.getLookup());
        }
    }

    @Override
    public void componentClosed() {
    }

    @Override
    public void componentShowing() {
        if (!obj.getEditableFile().isLoaded()) {
            obj.getEditableFile().load(obj.getLookup());
            reload(obj.getEditableFile(), obj.getLookup());
        }
    }

    @Override
    public void componentHidden() {
    }

    @Override
    public void componentActivated() {
        if (!obj.getEditableFile().isLoaded()) {
            obj.getEditableFile().load(obj.getLookup());
            reload(obj.getEditableFile(), obj.getLookup());
        }
    }

    @Override
    public void componentDeactivated() {
    }

    @Override
    public UndoRedo getUndoRedo() {
        return obj.getLookup().lookup(MultiViewEditorElement.class).getUndoRedo();
    }

    @Override
    public void setMultiViewCallback(MultiViewElementCallback callback) {
        this.callback = callback;
    }

    @Override
    public CloseOperationState canCloseElement() {
        return CloseOperationState.STATE_OK;
    }

    protected void makeMapping(Connection conn) {
        InOut startNode = (InOut) conn.start.getNode();
        InOut endNode = (InOut) conn.end.getNode();
        String leftVarName = conn.end.getText();
        String rightVarName = conn.start.getText();
        String leftVarSwizzle = null;
        String rightVarSwizzle = null;

        int endCard = ShaderUtils.getCardinality(conn.end.getType(), "");
        int startCard = ShaderUtils.getCardinality(conn.start.getType(), "");
        String swizzle = "xyzw";
        if (startCard > endCard) {
            rightVarSwizzle = swizzle.substring(0, endCard);
        } else if (endCard > startCard) {
            leftVarSwizzle = swizzle.substring(0, startCard);
        }

        if (endNode instanceof OutBusPanel) {
            OutputMappingBlock mapping = new OutputMappingBlock(leftVarName, rightVarName, leftVarSwizzle, rightVarSwizzle, endNode.getName(), startNode.getName(), null);
            startNode.addOutputMapping(mapping);
            conn.makeKey(mapping, diagram1.getCurrentTechniqueName());
        } else {
            InputMappingBlock mapping = new InputMappingBlock(leftVarName, rightVarName, leftVarSwizzle, rightVarSwizzle, endNode.getName(), startNode.getName(), null);
            endNode.addInputMapping(mapping);
            conn.makeKey(mapping, diagram1.getCurrentTechniqueName());
        }
    }

    protected void notifyRemoveConnection(Connection conn) {
        InOut startNode = (InOut) conn.start.getNode();
        InOut endNode = (InOut) conn.end.getNode();
        if (endNode instanceof OutBusPanel) {
            startNode.removeOutputMapping((OutputMappingBlock) conn.mapping);
        } else {
            endNode.removeInputMapping((InputMappingBlock) conn.mapping);
        }
    }

    public void notifyAddNode(ShaderNodeBlock node, ShaderNodeDefinition def) {
        TechniqueBlock technique = getTechnique();
        if (def.getType() == Shader.ShaderType.Vertex) {
            technique.addVertexShaderNode(node);
        } else if (def.getType() == Shader.ShaderType.Fragment) {
            technique.addFragmentShaderNode(node);
        }
    }

    public void notifyAddTechnique(TechniqueBlock tech) {
       
        String path = "Common/MatDefs/ShaderNodes/Common/Unshaded.j3sn";
        ShaderNodeDefinitionKey key =  new ShaderNodeDefinitionKey(path);
        List<ShaderNodeDefinition> defs = getAssetManager().loadAsset(key);
        ShaderNodeBlock node = new ShaderNodeBlock(defs.get(0), path);
        tech.addFragmentShaderNode(node);
        node.addOutputMapping(new OutputMappingBlock("color", "color", "", "", "Global", "Unshaded", null));
        
        path = "Common/MatDefs/ShaderNodes/Common/CommonVert.j3sn";
        key =  new ShaderNodeDefinitionKey(path);
        defs = getAssetManager().loadAsset(key);
        node = new ShaderNodeBlock(defs.get(0), path);
        tech.addVertexShaderNode(node);
        
        node.addInputMapping(new InputMappingBlock("worldViewProjectionMatrix", "WorldViewProjectionMatrix", "", "", "CommonVert", "WorldParam", null));
        node.addInputMapping(new InputMappingBlock("modelPosition", "position", "", "xyz", "CommonVert", "Global", null));        
        
        node.addOutputMapping(new OutputMappingBlock("position", "projPosition", "", "", "Global", "CommonVert", null));
        
        
        WorldParamBlock param = new WorldParamBlock("WorldViewProjectionMatrix");
        tech.addWorldParam(param);
        
        obj.getEditableFile().getMatDefStructure().addTechnique(tech);     
        
    }

    public void autoLayout(){
        diagram1.autoLayout();
    }
    
    public void notifyAddMapParam(String type, String name) {
        MatDefBlock matDef = obj.getLookup().lookup(MatDefBlock.class);
        MatParamBlock param = new MatParamBlock(type, name, null, null);
        matDef.addMatParam(param);
    }

    public void notifyAddWorldParam(String name) {
        WorldParamBlock param = new WorldParamBlock(name);
        getTechnique().addWorldParam(param);
    }

    public void notifyRemoveNode(NodePanel node) {
        MatDefBlock matDef = obj.getLookup().lookup(MatDefBlock.class);
        if (node.getType() == NodePanel.NodeType.Fragment || node.getType() == NodePanel.NodeType.Vertex) {
            TechniqueBlock technique = getTechnique();
            for (ShaderNodeBlock shaderNodeBlock : technique.getShaderNodes()) {
                if (shaderNodeBlock.getName().equals(node.getName())) {
                    technique.removeShaderNode(shaderNodeBlock);
                }
            }
        } else if (node.getType() == NodePanel.NodeType.MatParam) {
            matDef.removeMatParam(new MatParamBlock("", node.getKey().replaceAll("MatParam.", ""), "", ""));
        } else if (node.getType() == NodePanel.NodeType.WorldParam) {
            getTechnique().removeWorldParam(new WorldParamBlock(node.getKey().replaceAll("WorldParam.", "")));
        } else if (node.getType() == NodePanel.NodeType.Attribute) {
            getTechnique().cleanupMappings("Attr", node.getKey().replaceAll("Attr.", ""));
        }
    }

    private Dot findConnectPoint(String nameSpace, String name, boolean isInput) {

        if (nameSpace.equals("MatParam")
                || nameSpace.equals("WorldParam")
                || nameSpace.equals("Attr")) {
            NodePanel np = diagram1.getNodePanel(nameSpace + "." + name);
            return isInput ? np.getInputConnectPoint(name) : np.getOutputConnectPoint(name);
        } else if (nameSpace.equals("Global")) {
            OutBusPanel outBus = diagram1.getOutBusPanel(name);
            return outBus.getConnectPoint();
        } else {
            NodePanel np = diagram1.getNodePanel(diagram1.getCurrentTechniqueName() + "/" + nameSpace);
            return isInput ? np.getInputConnectPoint(name) : np.getOutputConnectPoint(name);
        }
    }

    private void makeConnection(MappingBlock mapping) {

        Dot leftDot = findConnectPoint(mapping.getLeftNameSpace(), mapping.getLeftVar(), true);
        Dot rightDot = findConnectPoint(mapping.getRightNameSpace(), mapping.getRightVar(), false);
        Connection conn = diagram1.connect(leftDot, rightDot);
        mapping.addPropertyChangeListener(WeakListeners.propertyChange(conn, mapping));
        conn.makeKey(mapping, diagram1.getCurrentTechniqueName());
    }

    private void initData(TechniqueBlock technique, ProjectAssetManager manager, List<ShaderNodeVariable> vertexGlobals, List<ShaderNodeVariable> fragmentGlobals, List<ShaderNodeVariable> attributes, MatDefBlock matDef, List<ShaderNodeVariable> uniforms) {
        for (ShaderNodeBlock sn : technique.getShaderNodes()) {
            ShaderNodeDefinition def = MaterialUtils.loadShaderNodeDefinition(sn, manager);
            List<InputMappingBlock> in = sn.getInputs();
            if (in != null) {
                for (InputMappingBlock map : in) {
                    ShaderNodeVariable var = new ShaderNodeVariable("", map.getRightNameSpace(), map.getRightVar());
                    if (var.getNameSpace().equals("Global")) {
                        var.setType("vec4");
                        if (def.getType() == Shader.ShaderType.Vertex) {
                            if (!MaterialUtils.contains(vertexGlobals, var)) {
                                vertexGlobals.add(var);
                            }
                        } else {
                            if (!MaterialUtils.contains(fragmentGlobals, var)) {
                                fragmentGlobals.add(var);
                            }
                        }
                    } else if (var.getNameSpace().equals("Attr")) {
                        ShaderNodeVariable left = MaterialUtils.getVar(def.getInputs(), map.getLeftVar());
                        var.setType(MaterialUtils.guessType(map, left));
                        attributes.add(var);
                    }
                }
            }
            List<OutputMappingBlock> out = sn.getOutputs();
            if (out != null) {
                for (OutputMappingBlock map : out) {
                    ShaderNodeVariable var = new ShaderNodeVariable("", map.getLeftNameSpace(), map.getLeftVar());
                    if (var.getNameSpace().equals("Global")) {
                        var.setType("vec4");
                        if (def.getType() == Shader.ShaderType.Vertex) {
                            if (!MaterialUtils.contains(vertexGlobals, var)) {
                                vertexGlobals.add(var);
                            }
                        } else {
                            if (!MaterialUtils.contains(fragmentGlobals, var)) {
                                fragmentGlobals.add(var);
                            }
                        }
                    }
                }
            }

        }

        for (WorldParamBlock worldParamBlock : technique.getWorldParams()) {
            ShaderNodeVariable var = new ShaderNodeVariable("", "WorldParam", worldParamBlock.getName());
            var.setType(MaterialUtils.getWorldParamType(var.getName()));
            uniforms.add(var);
        }

        for (MatParamBlock matParamBlock : matDef.getMatParams()) {
            ShaderNodeVariable var = new ShaderNodeVariable("", "MatParam", matParamBlock.getName());
            var.setType(MaterialUtils.getMatParamType(matParamBlock));
            uniforms.add(var);
        }

    }

    private TechniqueBlock getTechnique() {
        TechniqueBlock technique = obj.getEditableFile().getCurrentTechnique();
        return technique;
    }

    protected Point getPositionFromMetaData(String key, int defaultx, int defaulty) throws NumberFormatException {
        Point position = new Point();
        String pos = metaData.getProperty(diagram1.getCurrentTechniqueName() + "/" + key, defaultx + "," + defaulty);

        if (pos != null) {
            String[] s = pos.split(",");
            position.x = Integer.parseInt(s[0]);
            position.y = Integer.parseInt(s[1]);
        }
        return position;
    }

    protected void savePositionToMetaData(String key, int x, int y) throws NumberFormatException {

        metaData.setProperty(diagram1.getCurrentTechniqueName() + "/" + key, x + "," + y);

    }

    public void reload() {
        try {
            obj.getLookup().lookup(EditorCookie.class).saveDocument();
            obj.getEditableFile().load(obj.getLookup());
            reload(obj.getEditableFile(), obj.getLookup());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void reload(final EditableMatDefFile file, final Lookup lkp) throws NumberFormatException {
        if (file.isLoaded()) {
            initDiagram(lkp);
            MatDefNavigatorPanel nav = obj.getLookup().lookup(MatDefNavigatorPanel.class);
            if (nav != null) {
                nav.updateData(obj);
            }
        } else {
            diagram1.clear();
            JLabel error = new JLabel("<html><center>Cannot load material definition.<br>Please see the error log and fix it in the text editor</center></html>");
            error.setForeground(Color.ORANGE);
            error.setFont(new Font("Arial", Font.BOLD, 24));
            error.setBounds(0, 0, 400, 100);
            jScrollPane1.getHorizontalScrollBar().setValue(0);
            error.setLocation(jScrollPane1.getViewport().getWidth() / 2 - 200, jScrollPane1.getViewport().getHeight() / 2 - 50);
            diagram1.add(error);
            diagram1.repaint();

        }
    }
}
