/*
 * Copyright (c) 2003-2012 jMonkeyEngine
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
package com.jme3.gde.core.appstates;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.PreviewRequest;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneListener;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import javax.swing.ActionMap;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

/**
 * TODO: preliminary implementation 'til new scene system
 */
@ConvertAsProperties(
    dtd = "-//com.jme3.gde.core.appstates//AppStateExplorer//EN",
autostore = false)
@TopComponent.Description(
    preferredID = "AppStateExplorerTopComponent",
//iconBase="SET/PATH/TO/ICON/HERE", 
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "navigator", openAtStartup = true)
@ActionID(category = "Window", id = "com.jme3.gde.core.appstates.AppStateExplorerTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
    displayName = "#CTL_AppStateExplorerAction",
preferredID = "AppStateExplorerTopComponent")
@Messages({
    "CTL_AppStateExplorerAction=AppStateExplorer",
    "CTL_AppStateExplorerTopComponent=AppStateExplorer Window",
    "HINT_AppStateExplorerTopComponent=This is a AppStateExplorer window"
})
public final class AppStateExplorerTopComponent extends TopComponent implements ExplorerManager.Provider {

    private transient ExplorerManager explorerManager = new ExplorerManager();
    private FakeApplication fakeApp;
    private ProjectAssetManager mgr;
    private SceneListener listener = new SceneListener() {
        public void sceneOpened(SceneRequest request) {
            Spatial rootNode = request.getRootNode();
            if (!(rootNode instanceof com.jme3.scene.Node)) {
                return;
            }
            mgr = request.getManager();
            AssetManager assetManager = request.getManager();
            Camera cam = SceneApplication.getApplication().getCamera();
            com.jme3.scene.Node guiNode = SceneApplication.getApplication().getGuiNode();
            fakeApp = new FakeApplication((com.jme3.scene.Node) rootNode, guiNode, assetManager, cam);
            //TODO: ermagherd, hackish
            SceneApplication.getApplication().setFakeApp(fakeApp);
            final AppStateManagerNode nod = new AppStateManagerNode(fakeApp.getStateManager());
            jButton1.setEnabled(true);
            explorerManager.setRootContext(nod);
            setActivatedNodes(new Node[]{nod});
        }

        public void sceneClosed(SceneRequest request) {
            SceneApplication.getApplication().setFakeApp(null);
            mgr = null;
            fakeApp = null;
            jButton1.setEnabled(false);
            explorerManager.setRootContext(Node.EMPTY);
            setActivatedNodes(new Node[]{Node.EMPTY});
        }

        public void previewCreated(PreviewRequest request) {
        }
    };

    public AppStateExplorerTopComponent() {
        initComponents();
        setName(Bundle.CTL_AppStateExplorerTopComponent());
        setToolTipText(Bundle.HINT_AppStateExplorerTopComponent());
        ActionMap map = getActionMap();
        map.put("delete", ExplorerUtils.actionDelete(explorerManager, true));
//        map.put("moveup", new MoveUpAction());
//        map.put("movedown", new MoveDownAction());
        associateLookup(ExplorerUtils.createLookup(explorerManager, map));
        SceneApplication.getApplication().addSceneListener(listener);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new BeanTreeView();
        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(AppStateExplorerTopComponent.class, "AppStateExplorerTopComponent.jButton1.text")); // NOI18N
        jButton1.setEnabled(false);
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)
            .addComponent(jScrollPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        ProjectAssetManager projectAssetManager = mgr;
        FakeApplication fakeApp = this.fakeApp;
        if (fakeApp != null && mgr != null) {
            new NewAppStateWizardAction(projectAssetManager, fakeApp).showWizard();
        }
    }//GEN-LAST:event_jButton1ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar jToolBar1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
    }

    @Override
    public void componentClosed() {
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
    }

    public ExplorerManager getExplorerManager() {
        return explorerManager;
    }
}
