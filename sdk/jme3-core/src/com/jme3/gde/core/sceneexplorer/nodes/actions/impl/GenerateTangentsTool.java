/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.sceneexplorer.nodes.actions.impl;

import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeGeometry;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractToolAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractToolWizardAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ToolAction;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.TangentBinormalGenerator;
import java.awt.Component;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.nodes.Node;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = ToolAction.class)
public class GenerateTangentsTool extends AbstractToolWizardAction {

    public GenerateTangentsTool() {
        name = "Generate Tangents";
    }

    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {
        Geometry geom = rootNode.getLookup().lookup(Geometry.class);
        if( undoObject instanceof Mesh){
            Mesh keptMesh = (Mesh)undoObject;
            geom.setMesh(keptMesh);
            geom.updateModelBound();
        }else{
            Mesh mesh = geom.getMesh();
            if (mesh != null) {
                mesh.clearBuffer(Type.Tangent);
            }
        }
    }

    public Class<?> getNodeClass() {
        return JmeGeometry.class;
    }

    @Override
    protected Object showWizard(Node node) {
         List<WizardDescriptor.Panel<WizardDescriptor>> panels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
        panels.add(new GenerateTangentsWizardPanel1());
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
        wiz.setTitle("Generate tangents for this model");
        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {
            // do something
            return wiz;
        } 
        return null;
    }

    @Override
    protected Object doApplyTool(AbstractSceneExplorerNode rootNode, Object settings) {
        WizardDescriptor wiz = (WizardDescriptor)settings;
        Geometry geom = rootNode.getLookup().lookup(Geometry.class);
        boolean splitMirrored = (Boolean)wiz.getProperties().get("splitMirrored");
        
        Mesh mesh = geom.getMesh();
        Mesh keptMesh = null;
        if (mesh != null) {
            if(splitMirrored){
                keptMesh  = mesh.deepClone();
            }
            TangentBinormalGenerator.generate(geom, splitMirrored);
            
        }
        if(keptMesh == null){
            return splitMirrored;
        }else{
            return keptMesh;
        }
    }

}
