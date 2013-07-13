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
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractToolWizardAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ToolAction;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import java.awt.Component;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import jme3tools.optimize.LodGenerator;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.nodes.Node;

/**
 *
 * @author nehon
 */
@org.openide.util.lookup.ServiceProvider(service = ToolAction.class)
public class GenerateLODTool extends AbstractToolWizardAction {

    public GenerateLODTool() {
        name = "Generate Levels of Detail";
    }

    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {

        Geometry geom = rootNode.getLookup().lookup(Geometry.class);
        Mesh mesh = geom.getMesh();
        if (mesh != null) {
            mesh.setLodLevels((VertexBuffer[]) undoObject);
        }

    }

    public Class<?> getNodeClass() {
        return JmeGeometry.class;
    }

    @Override
    protected Object showWizard(Node node) {
        AbstractSceneExplorerNode rootNode = (AbstractSceneExplorerNode) node;
        Geometry geom = rootNode.getLookup().lookup(Geometry.class);
        int triSize = geom.getMesh().getTriangleCount();

        List<WizardDescriptor.Panel<WizardDescriptor>> panels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
        panels.add(new GenerateLODWizardPanel1());
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
        wiz.setTitle("Generate Levels of Detail for this model");
        wiz.putProperty("triSize", triSize);

        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {
            return wiz;
        }
        return null;
    }

    @Override
    protected Object doApplyTool(AbstractSceneExplorerNode rootNode, Object settings) {
        WizardDescriptor wiz = (WizardDescriptor) settings;

        Geometry geom = rootNode.getLookup().lookup(Geometry.class);
        Mesh mesh = geom.getMesh();
        if (mesh != null) {
            //save old lods
            VertexBuffer[] lods = null;
            if (geom.getMesh().getNumLodLevels() > 0) {
                lods = new VertexBuffer[geom.getMesh().getNumLodLevels()];
                for (int i = 0; i < lods.length; i++) {
                    lods[i] = geom.getMesh().getLodLevel(i);
                }
            }

            
            if (wiz != null) {
                float[] values = (float[]) wiz.getProperties().get("reductionValues");
                if (values != null) {
                    //generate lods
                    LodGenerator generator = new LodGenerator(geom);
                    LodGenerator.TriangleReductionMethod method = (LodGenerator.TriangleReductionMethod) wiz.getProperties().get("reductionMethod");
                    generator.bakeLods(method, values);
                }else{
                    mesh.setLodLevels(null);                    
                }
            }

            return lods;
        }
        return null;

    }
}
