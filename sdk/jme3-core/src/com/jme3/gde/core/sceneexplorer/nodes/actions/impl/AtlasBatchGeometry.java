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

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractToolWizardAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ToolAction;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Component;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import jme3tools.optimize.TextureAtlas;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = ToolAction.class)
public class AtlasBatchGeometry extends AbstractToolWizardAction {

    public static enum AtlasResolution {

        RES_256x256("256x256", 256, 256),
        RES_512x512("512x512", 512, 512),
        RES_1024x1024("1024x1024", 1024, 1024),
        RES_2048x2048("2048x2048", 2048, 2048),
        RES_4096x4096("4096x4096", 4096, 4096),
        RES_8192x8192("8192x8192", 8192, 8192),
        RES_16384x16384("16384x16384", 16384, 16384);
        private final String name;
        private final int resX;
        private final int resY;

        public String getName() {
            return name;
        }

        public int getResX() {
            return resX;
        }

        public int getResY() {
            return resY;
        }

        public static AtlasResolution getNumber(int id) {
            switch (id) {
                case 1:
                    return RES_256x256;
                case 2:
                    return RES_512x512;
                case 3:
                    return RES_1024x1024;
                case 4:
                    return RES_2048x2048;
                case 5:
                    return RES_4096x4096;
                case 6:
                    return RES_8192x8192;
                case 7:
                    return RES_16384x16384;
                default:
                    return RES_2048x2048;
            }
        }

        private AtlasResolution(String name, int resX, int resY) {
            this.name = name;
            this.resX = resX;
            this.resY = resY;
        }
    }

    private class OldNew {

        public OldNew(Spatial newSpat, List<Spatial> oldChildren) {
            this.newSpat = newSpat;
            this.oldChildren = oldChildren;
        }
        Spatial newSpat;
        List<Spatial> oldChildren;
    }

    @Override
    protected Object showWizard(org.openide.nodes.Node node) {
        List<WizardDescriptor.Panel<WizardDescriptor>> panels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
        panels.add(new AtlasBatchGeometryWizardPanel1());
        String[] steps = new String[panels.size()];
        for (int i = 0; i < panels.size(); i++) {
            Component c = panels.get(i).getComponent();
            // Default step name to component name of panel.
            steps[i] = c.getName();
            if (c instanceof JComponent) { // assume Swing components
                JComponent jc = (JComponent) c;
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
                jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
            }
        }
        WizardDescriptor wiz = new WizardDescriptor(new WizardDescriptor.ArrayIterator<WizardDescriptor>(panels));
        // {0} will be replaced by WizardDesriptor.Panel.getComponent().getName()
        wiz.setTitleFormat(new MessageFormat("{0}"));
        wiz.setTitle("Create Batch with Texture Atlas");
        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {
            // do something
            return wiz;
        }
        return null;
    }

    public AtlasBatchGeometry() {
        name = "Batch Geometry with TextureAtlas..";
    }

    @Override
    protected Object doApplyTool(AbstractSceneExplorerNode rootNode, Object settings) {
        if (settings == null) {
            return null;
        }
        WizardDescriptor wiz = (WizardDescriptor)settings;
        AtlasResolution res = (AtlasResolution)wiz.getProperty("size");
        Node parent = rootNode.getLookup().lookup(Node.class);
        AssetManager mgr = rootNode.getLookup().lookup(ProjectAssetManager.class);
        if (parent == null || mgr == null) {
            return null;
        }
        Geometry batch = TextureAtlas.makeAtlasBatch(parent, mgr, res.resX);
        batch.setName(parent.getName() + " - batched");
        List<Spatial> currentChildren = new ArrayList<Spatial>();
        if (parent != null && batch != null) {
            currentChildren.addAll(parent.getChildren());
            parent.detachAllChildren();
            parent.attachChild(batch);
        }
        return new OldNew(batch, currentChildren);
    }

    @Override
    protected void doUndoTool(AbstractSceneExplorerNode rootNode, Object undoObject) {
        if (undoObject == null) {
            return;
        }
        Node parent = rootNode.getLookup().lookup(Node.class);
        OldNew undo = (OldNew) undoObject;
        if (parent == null || undo == null) {
            return;
        }
        parent.detachChild(undo.newSpat);
        for (Iterator<Spatial> it = undo.oldChildren.iterator(); it.hasNext();) {
            Spatial spatial = it.next();
            parent.attachChild(spatial);
        }
    }

    public Class<?> getNodeClass() {
        return JmeNode.class;
    }
}
