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
package com.jme3.gde.core.sceneexplorer;

import com.jme3.gde.core.scene.PreviewRequest;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneListener;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Lookup.Result;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.util.ImageUtilities;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.actions.CopyAction;
import org.openide.actions.CutAction;
import org.openide.actions.DeleteAction;
import org.openide.actions.PasteAction;
import org.openide.awt.UndoRedo;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//com.jme3.gde.core.sceneexplorer//SceneExplorer//EN",
autostore = false)
public final class SceneExplorerTopComponent extends TopComponent implements ExplorerManager.Provider, SceneListener, LookupListener {

    private static SceneExplorerTopComponent instance;
    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "com/jme3/gde/core/sceneexplorer/jme-logo.png";
    private static final String PREFERRED_ID = "SceneExplorerTopComponent";
    private SceneRequest request;
    private final Result<AbstractSceneExplorerNode> nodeSelectionResult;
    private AbstractSceneExplorerNode selectedSpatial;
    private AbstractSceneExplorerNode lastSelected;
    private Map<String, MaterialChangeProvider> materialChangeProviders = new HashMap<String, MaterialChangeProvider>();
    private Map<String, List<MaterialChangeListener>> materialChangeListeners = new HashMap<String, List<MaterialChangeListener>>();

    public SceneExplorerTopComponent() {
        initComponents();
        initActions();
        setName(NbBundle.getMessage(SceneExplorerTopComponent.class, "CTL_SceneExplorerTopComponent"));
        setToolTipText(NbBundle.getMessage(SceneExplorerTopComponent.class, "HINT_SceneExplorerTopComponent"));
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));
        associateLookup(ExplorerUtils.createLookup(explorerManager, getActionMap()));
        nodeSelectionResult = Utilities.actionsGlobalContext().lookupResult(AbstractSceneExplorerNode.class);
        nodeSelectionResult.addLookupListener(this);
    }

    private void initActions() {
        CutAction cut = SystemAction.get(CutAction.class);
        getActionMap().put(cut.getActionMapKey(), ExplorerUtils.actionCut(explorerManager));
        CopyAction copy = SystemAction.get(CopyAction.class);
        getActionMap().put(copy.getActionMapKey(), ExplorerUtils.actionCopy(explorerManager));
        PasteAction paste = SystemAction.get(PasteAction.class);
        getActionMap().put(paste.getActionMapKey(), ExplorerUtils.actionPaste(explorerManager));
        DeleteAction delete = SystemAction.get(DeleteAction.class);
        getActionMap().put(delete.getActionMapKey(), ExplorerUtils.actionDelete(explorerManager, true));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        explorerScrollPane = new BeanTreeView();
        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();

        jToolBar1.setRollover(true);

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(SceneExplorerTopComponent.class, "SceneExplorerTopComponent.jButton1.text")); // NOI18N
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
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .addComponent(explorerScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(explorerScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if (selectedSpatial == null) {
            return;
        }
        selectedSpatial.refresh(false);
    }//GEN-LAST:event_jButton1ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane explorerScrollPane;
    private javax.swing.JButton jButton1;
    private javax.swing.JToolBar jToolBar1;
    // End of variables declaration//GEN-END:variables

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized SceneExplorerTopComponent getDefault() {
        if (instance == null) {
            instance = new SceneExplorerTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the SceneExplorerTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized SceneExplorerTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(SceneExplorerTopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof SceneExplorerTopComponent) {
            return (SceneExplorerTopComponent) win;
        }
        Logger.getLogger(SceneExplorerTopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID
                + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    public HelpCtx getHelpCtx() {
        HelpCtx ctx = new HelpCtx("sdk.scene_explorer");
        //this call is for single components:
        //HelpCtx.setHelpIDString(this, "com.jme3.gde.core.sceneviewer");
        return ctx;
    }

    @Override
    public void componentOpened() {
        SceneApplication.getApplication().addSceneListener(this);
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        SceneApplication.getApplication().removeSceneListener(this);
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    Object readProperties(java.util.Properties p) {
        if (instance == null) {
            instance = this;
        }
        instance.readPropertiesImpl(p);
        return instance;
    }

    private void readPropertiesImpl(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    @Override
    public UndoRedo getUndoRedo() {
        return Lookup.getDefault().lookup(UndoRedo.class);
    }
    private transient ExplorerManager explorerManager = new ExplorerManager();

    public ExplorerManager getExplorerManager() {
        return explorerManager;
    }

    public void resultChanged(LookupEvent ev) {
        Collection collection = nodeSelectionResult.allInstances();
        for (Iterator it = collection.iterator(); it.hasNext();) {
            Object object = it.next();
            if (object instanceof AbstractSceneExplorerNode) {
                selectedSpatial = (AbstractSceneExplorerNode) object;
                lastSelected = (AbstractSceneExplorerNode) object;
                return;
            }
        }
        selectedSpatial = null;
    }

    public void sceneRequested(SceneRequest request) {
        this.request = request;
        JmeNode node = request.getJmeNode();
        for (Iterator it = materialChangeProviders.values().iterator(); it.hasNext();) {
            MaterialChangeProvider provider = (MaterialChangeProvider) it.next();
            provider.clearMaterialChangeListeners();
        }
        if (node != null) {
            explorerManager.setRootContext(node);
            explorerManager.getRootContext().setDisplayName(node.getName());
            requestVisible();
        }
    }

    public boolean sceneClose(SceneRequest request) {
        this.request = null;
        explorerManager.setRootContext(Node.EMPTY);
        return true;
    }

    public void previewRequested(PreviewRequest request) {
    }

    /**
     * @return the selectedSpatial
     */
    public AbstractSceneExplorerNode getLastSelected() {
        return lastSelected;
    }

    public void addMaterialChangeProvider(MaterialChangeProvider provider) {
        Logger.getLogger(SceneExplorerTopComponent.class.getName()).log(Level.INFO, "New materail provider registered for: {0}", provider.getKey());
        materialChangeProviders.put(provider.getKey(), provider);
        List<MaterialChangeListener> listeners = materialChangeListeners.get(provider.getKey());
        if (listeners == null) {
            return;
        }
        provider.addAllMaterialChangeListener(listeners);
    }

    public void removeMaterialChangeProvider(MaterialChangeProvider provider) {
        Logger.getLogger(SceneExplorerTopComponent.class.getName()).log(Level.INFO, "Removing material provider for :  {0}", provider.getKey());
        System.out.println("Removing provider : " + provider.getKey());
        materialChangeProviders.remove(provider.getKey());
    }

    public void addMaterialChangeListener(MaterialChangeListener listener) {

        if (listener.getKey() != null) {
             Logger.getLogger(SceneExplorerTopComponent.class.getName()).log(Level.INFO, "New material listener for : {0}", listener.getKey());
            List<MaterialChangeListener> listeners = materialChangeListeners.get(listener.getKey());
            if (listeners == null) {
                listeners = new ArrayList<MaterialChangeListener>();
                materialChangeListeners.put(listener.getKey(), listeners);
            }
            listeners.add(listener);

            MaterialChangeProvider provider = materialChangeProviders.get(listener.getKey());
            if (provider != null) {
                provider.addMaterialChangeListener(listener);
            }
        }
    }

    public void removeMaterialChangeListener(MaterialChangeListener listener) {
        Logger.getLogger(SceneExplorerTopComponent.class.getName()).log(Level.INFO, "Removing material listener for: {0}", listener.getKey());
        List<MaterialChangeListener> listeners = materialChangeListeners.get(listener.getKey());
        if (listeners == null) {
            return;
        }
        listeners.remove(listener);

        MaterialChangeProvider provider = materialChangeProviders.get(listener.getKey());
        if (provider != null) {
            provider.removeMaterialChangeListener(listener);
        }
    }

    public void swapMaterialChangeListener(MaterialChangeListener listener, String oldKey, String newKey) {
        Logger.getLogger(SceneExplorerTopComponent.class.getName()).log(Level.INFO, "Swaping material listeners : {0} -> {1}", new Object[]{oldKey, newKey});
        if (oldKey != null) {
            List<MaterialChangeListener> listeners = materialChangeListeners.get(oldKey);
            if (listeners != null) {
                listeners.remove(listener);
            }

            MaterialChangeProvider provider = materialChangeProviders.get(oldKey);
            if (provider != null) {
                provider.removeMaterialChangeListener(listener);
            }
        }

        if (newKey != null) {
            //  assert newKey.equals(listener.getKey());
             List<MaterialChangeListener> listeners = materialChangeListeners.get(newKey);
            if (listeners == null) {
                listeners = new ArrayList<MaterialChangeListener>();
                materialChangeListeners.put(newKey, listeners);
            }
            listeners.add(listener);

            MaterialChangeProvider provider = materialChangeProviders.get(newKey);
            if (provider != null) {
                provider.addMaterialChangeListener(listener);
            }
        }
    }
}
