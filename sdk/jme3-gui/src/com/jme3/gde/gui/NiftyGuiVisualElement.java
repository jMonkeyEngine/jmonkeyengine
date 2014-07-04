/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.gui.nodes.GUINode;
import com.jme3.gde.gui.palette.NiftyGUIPaletteFactory;
import de.lessvoid.nifty.Nifty;
import jada.ngeditor.controller.GUIEditor;
import jada.ngeditor.guiviews.DND.PaletteDropTarget;
import jada.ngeditor.guiviews.DND.TrasferHandling;
import jada.ngeditor.guiviews.J2DNiftyView;
import jada.ngeditor.model.elements.GElement;
import jada.ngeditor.model.elements.GLayer;
import jada.ngeditor.model.exception.NoProductException;
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.text.AbstractDocument;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.UndoRedo;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;
import org.xml.sax.SAXException;

@MultiViewElement.Registration(
        displayName = "#LBL_NiftyGui_VISUAL",
        iconBase = "com/jme3/gde/gui/Computer_File_043.gif",
        mimeType = "text/x-niftygui+xml",
        persistenceType = TopComponent.PERSISTENCE_NEVER,
        preferredID = "NiftyGuiVisual",
        position = 2000)
@Messages("LBL_NiftyGui_VISUAL=Visual")
public final class NiftyGuiVisualElement extends JPanel implements MultiViewElement , ExplorerManager.Provider,Observer {

    private NiftyGuiDataObject obj;
    private JToolBar toolbar = new JToolBar();
    private transient MultiViewElementCallback callback;
    private GUIEditor editor;
    private final Nifty nifty;
    private final J2DNiftyView view;
    private final JComboBox layers = new JComboBox();
    private final ExplorerManager nodesManager;
   

    public NiftyGuiVisualElement(Lookup lkp) {
        obj = lkp.lookup(NiftyGuiDataObject.class);
        assert obj != null;
        initComponents();
        view = new J2DNiftyView(800, 600);
        view.init();
        this.scrollArea.getViewport().addChangeListener(view);
        this.scrollArea.setViewportView(view);
        TrasferHandling tranf = new TrasferHandling();
        PaletteDropTarget tmp = new PaletteDropTarget();
        editor = obj.getLookup().lookup(GUIEditor.class);
        editor.addObserver(this);
        nodesManager = new ExplorerManager();
        nifty = view.getNifty();
        view.setTransferHandler(tranf);
        view.setDropTarget(tmp);
        editor.addObserver(view);
        editor.addObserver(tranf);
        editor.addObserver(tmp);
        this.createToolbar();
    }
 /**
 * Old code
 * @author normenhansen
 */
     private void createToolbar() {
        toolbar.setPreferredSize(new Dimension(10000, 24));
        toolbar.setMaximumSize(new Dimension(10000, 24));
        toolbar.setFloatable(false);
        JComboBox comboBox = new JComboBox(new String[]{"640x480", "480x800", "800x480", "800x600", "1024x768", "1280x720"});
        comboBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                String string = (String) e.getItem();
                if ("640x480".equals(string)) {
                    view.setResoltion(640, 480);
                } else if ("1024x768".equals(string)) {
                     view.setResoltion(1024, 768);
                } else if ("1280x720".equals(string)) {
                     view.setResoltion(1280, 720);
                } else if ("800x600".equals(string)) {
                     view.setResoltion(800, 600);
                } else if ("800x480".equals(string)) {
                     view.setResoltion(800, 480);
                } else if ("480x800".equals(string)) {
                     view.setResoltion(480, 800);
                } else {
                     view.setResoltion(800, 600);
                }
            }
        });
        toolbar.add(comboBox);
        comboBox.setSelectedItem("800x600");
        layers.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                GLayer item = (GLayer) e.getItem();
                editor.selectElement(item);
            }
        });
        toolbar.add(layers);
     }
    @Override
    public String getName() {
        return "NiftyGuiVisualElement";
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollArea = new javax.swing.JScrollPane();

        setLayout(new java.awt.BorderLayout());
        add(scrollArea, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollArea;
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
        return ExplorerUtils.createLookup(nodesManager, new ActionMap());
    }
    /**
     * Raw implementation , just to prototype the editor
     */
    @Override
    public void componentOpened() {
        try {
            ProgressHandle handle = ProgressHandleFactory.createHandle("Loading the gui file");
            String path = this.obj.getPrimaryFile().getPath();
            ProjectAssetManager mgr = obj.getLookup().lookup(ProjectAssetManager.class);
            String assetPath = mgr.getAssetFolder().getPath();
            handle.progress(50);
            this.editor.createNewGui(nifty,new File(path),new File(assetPath));
            nodesManager.setRootContext(new GUINode(this.editor.getGui()));
            Collection<GLayer> layers1 = this.editor.getGui().getLayers();
            DefaultComboBoxModel<GLayer> model = new DefaultComboBoxModel<GLayer>(layers1.toArray(new GLayer[0]));
            layers.setModel(model);
            layers.setSelectedItem(this.editor.getCurrentLayer());
            
            handle.finish();
            
        } catch (ParserConfigurationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (JAXBException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoProductException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SAXException ex) {
            Exceptions.printStackTrace(ex);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void componentClosed() {
    }
    /**
     * Raw implementation , just to prototype the editor
     */
    @Override
    public void componentShowing() {
        try {
            ProgressHandle handle = ProgressHandleFactory.createHandle("Loading the gui file");
            String path = this.obj.getPrimaryFile().getPath();
            ProjectAssetManager mgr = this.obj.getLookup().lookup(ProjectAssetManager.class);
            String assetPath = mgr.getAssetFolder().getPath();
            this.editor.createNewGui(nifty,new File(path),new File(assetPath));
             nodesManager.setRootContext(new GUINode(this.editor.getGui()));
            Collection<GLayer> layers1 = this.editor.getGui().getLayers();
            DefaultComboBoxModel<GLayer> model = new DefaultComboBoxModel<GLayer>(layers1.toArray(new GLayer[0]));
            layers.setModel(model);
            layers.setSelectedItem(this.editor.getCurrentLayer());
           
            handle.finish();
        } catch (ParserConfigurationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (JAXBException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoProductException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SAXException ex) {
            Exceptions.printStackTrace(ex);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        
    }
    /**
     * Raw implementation , just to prototype the editor
     */
    @Override
    public void componentHidden() {
        String path = this.obj.getPrimaryFile().getPath();
        try {
            this.editor.saveGui(path);
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (JAXBException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void componentActivated() {
    }

    @Override
    public void componentDeactivated() {
    }

    @Override
    public UndoRedo getUndoRedo() {
        return UndoRedo.NONE;
    }

    @Override
    public void setMultiViewCallback(MultiViewElementCallback callback) {
        this.callback = callback;
    }

    @Override
    public CloseOperationState canCloseElement() {
        return CloseOperationState.STATE_OK;
    }

    @Override
    public ExplorerManager getExplorerManager() {
       return nodesManager;
    }

    @Override
    public void update(Observable o, Object arg) {
        jada.ngeditor.listeners.actions.Action act = ( jada.ngeditor.listeners.actions.Action) arg;
       if(act.getType() == jada.ngeditor.listeners.actions.Action.SEL){
           ArrayList<String> path = new ArrayList<String>();
           GElement parent = act.getGUIElement();
           while(parent!=null){
               path.add(parent.getID());
               parent = parent.getParent();
           }
           
           Node result = nodesManager.getRootContext();
          
           for(int i=path.size()-1;i>=0 && result!=null;i--){
               result = result.getChildren().findChild(path.get(i));
           }
            try {
                if(result!=null){
                nodesManager.setSelectedNodes(new Node[]{result});
                }
            } catch (PropertyVetoException ex) {
                Exceptions.printStackTrace(ex);
            }
       }
    }
}
