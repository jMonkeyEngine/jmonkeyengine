/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.gui.nodes.GUINode;
import de.lessvoid.nifty.Nifty;
import jada.ngeditor.controller.CommandProcessor;
import jada.ngeditor.controller.GUIEditor;
import jada.ngeditor.guiviews.DND.PaletteDropTarget;
import jada.ngeditor.guiviews.DND.TrasferHandling;
import jada.ngeditor.guiviews.J2DNiftyView;
import jada.ngeditor.listeners.events.SelectionChanged;
import jada.ngeditor.model.GuiEditorModel;
import jada.ngeditor.model.elements.GElement;
import jada.ngeditor.model.elements.GLayer;
import jada.ngeditor.model.exception.NoProductException;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
import org.openide.awt.UndoRedo;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.xml.sax.SAXException;
import sun.rmi.runtime.Log;

@MultiViewElement.Registration(
        displayName = "#LBL_NiftyGui_VISUAL",
        iconBase = "com/jme3/gde/gui/Computer_File_043.gif",
        mimeType = "text/x-niftygui+xml",
        persistenceType = TopComponent.PERSISTENCE_NEVER,
        preferredID = "NiftyGuiVisual",
        position = 2000)
@Messages("LBL_NiftyGui_VISUAL=Visual")
public final class NiftyGuiVisualElement extends JPanel implements MultiViewElement , ExplorerManager.Provider,Observer, PropertyChangeListener {

    private NiftyGuiDataObject obj;
    private JToolBar toolbar = new JToolBar();
    private transient MultiViewElementCallback callback;
    private GUIEditor editor;
    private final Nifty nifty;
    private final J2DNiftyView view;
    private final JComboBox layers = new JComboBox();
    private final ExplorerManager nodesManager;
    private final UndoRedo.Manager undoSupport;
    private int guiID;
   

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
        nodesManager = new ExplorerManager();
        nifty = view.getNifty();
        view.setTransferHandler(tranf);
        view.setDropTarget(tmp);
       // editor.addObserver(view);
       // editor.addObserver(tranf);
        this.obj.addPropertyChangeListener(this);
        this.createToolbar();
        this.undoSupport = new UndoRedo.Manager();
        CommandProcessor.getInstance().setUndoManager(undoSupport);
    }
 /**
 * Old code
 * @author normenhansen
 */
     private void createToolbar() {
        toolbar.setPreferredSize(new Dimension(10000, 24));
        toolbar.setMaximumSize(new Dimension(10000, 24));
        toolbar.setFloatable(false);
        toolbar.add(new JLabel("Change Resolution"));
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
        toolbar.add(new JLabel("Current Layer"));
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
       loadGui();
    }

    @Override
    public void componentClosed() {
        
    }
    /**
     * Raw implementation , just to prototype the editor
     */
    @Override
    public void componentShowing() {
        if(!this.obj.isModified()){
            return;
        }
        loadGui();
        
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
        } catch (NullPointerException ex){
             Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void componentActivated() {
        GuiEditorModel model = (GuiEditorModel) CommandProcessor.getInstance().getObservable();
        model.setCurrentGUI(guiID);
        CommandProcessor.getInstance().setUndoManager(undoSupport);
    }

    @Override
    public void componentDeactivated() {
    }

    @Override
    public UndoRedo getUndoRedo() {
        return this.undoSupport;
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
       if(arg instanceof SelectionChanged ){
           SelectionChanged event = (SelectionChanged) arg;
           if(event.getNewSelection().isEmpty()){
               return;
           }
           ArrayList<String> path = new ArrayList<String>();
           GElement parent = event.getElement();
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals(DataObject.PROP_MODIFIED)){
            boolean old = (Boolean)evt.getOldValue();
            boolean nev = (Boolean)evt.getNewValue();
            if(old && !nev){
                this.loadGui();
            }
        }
      
    }

    private void loadGui() {
        try {
            ProgressHandle handle = ProgressHandleFactory.createHandle("Loading the gui file");
            InputStream is = this.obj.getPrimaryFile().getInputStream();
            
            ProjectAssetManager mgr = this.obj.getLookup().lookup(ProjectAssetManager.class);
            String assetPath = mgr.getAssetFolder().getPath();
            this.editor.createNewGui(nifty,is,new File(assetPath));
            this.view.newGui(this.editor.getGui());
            nodesManager.setRootContext(new GUINode(this.editor.getGui()));
            this.editor.getGui().getSelection().addObserver(this);
            Collection<GLayer> layers1 = this.editor.getGui().getLayers();
            guiID = this.editor.getGui().getGUIid();
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
}
