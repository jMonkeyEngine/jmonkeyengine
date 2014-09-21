/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.view;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.gui.NiftyGuiDataObject;
import com.jme3.gde.gui.nodes.GElementNode;
import com.jme3.gde.gui.nodes.GUINode;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.tools.resourceloader.ResourceLocation;
import jada.ngeditor.controller.CommandProcessor;
import jada.ngeditor.controller.GUIEditor;
import jada.ngeditor.guiviews.DND.PaletteDropTarget;
import jada.ngeditor.guiviews.DND.TrasferHandling;
import jada.ngeditor.guiviews.J2DNiftyView;
import jada.ngeditor.listeners.events.SelectionChanged;
import jada.ngeditor.model.GUI;
import jada.ngeditor.model.GuiEditorModel;
import jada.ngeditor.model.elements.GElement;
import jada.ngeditor.model.elements.GLayer;
import jada.ngeditor.model.exception.NoProductException;
import jada.ngeditor.persistence.GUIWriter;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
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
import org.netbeans.spi.actions.AbstractSavable;
import org.openide.awt.UndoRedo;
import org.openide.explorer.ExplorerManager;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.windows.TopComponent;
import org.xml.sax.SAXException;

@MultiViewElement.Registration(
        displayName = "#LBL_NiftyGui_VISUAL",
        iconBase = "com/jme3/gde/gui/multiview/icons/game-monitor.png",
        mimeType = "text/x-niftygui+xml",
        persistenceType = TopComponent.PERSISTENCE_NEVER,
        preferredID = "NiftyGuiVisual",
        position = 2000)
@Messages("LBL_NiftyGui_VISUAL=Visual")
public final class NiftyGuiVisualElement extends JPanel implements MultiViewElement , ExplorerManager.Provider,Observer, PropertyChangeListener {
    private static final Logger logger = Logger.getLogger(NiftyGuiVisualElement.class.getName());
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
     private final InstanceContent content = new InstanceContent();
     private Lookup lookup;
     private AssetManager assetManager;

    protected class ResourceLocationJmp implements ResourceLocation {

        public InputStream getResourceAsStream(String path) {
            AssetKey<Object> key = new AssetKey<Object>(path);
            AssetInfo info = assetManager.locateAsset(key);
            if (info != null){
                return info.openStream();
            }else{
                throw new AssetNotFoundException(path);
            }
        }

        public URL getResource(String path) {
            throw new UnsupportedOperationException();
        }
    }
    
    private ResourceLocation resourceLocation = new ResourceLocationJmp();
    
        
    public NiftyGuiVisualElement(Lookup lkp) {
        obj = lkp.lookup(NiftyGuiDataObject.class);
        assert obj != null;
        assetManager = obj.getLookup().lookup(AssetManager.class);
        assert assetManager != null;
        System.out.println("AssetManagerNifty " + assetManager);
        initComponents();
        view = new J2DNiftyView(800, 600);
        view.init(resourceLocation);
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
        this.content.set(Collections.singleton(obj.getNodeDelegate()), null);
        lookup = new AbstractLookup(content);
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
        return this.lookup ;
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
        try{
        GuiEditorModel model = (GuiEditorModel) CommandProcessor.getInstance().getObservable();
        model.setCurrentGUI(guiID);
        model.getCurrent().addObserver(this);
        CommandProcessor.getInstance().setUndoManager(undoSupport);
        }catch(java.lang.IllegalArgumentException ex){
            logger.log(Level.SEVERE,"Can't load your gui", ex);
        }
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
           GElement parent = event.getElement();
           GElementNode node = new GElementNode(parent);
           this.content.set(Collections.singleton(node), null);
           
       }else if(o instanceof GUI){
           //Add a save. We don't add multible savable because they cointains the same
           //information about editing.
           GuiSavable savable = this.lookup.lookup(GuiSavable.class);
           if(savable == null){
           String path = this.obj.getPrimaryFile().getPath();
           final GuiSavable guiSavable = new NiftyGuiVisualElement.GuiSavable(((GUI)o),path);
           this.content.add(guiSavable);
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
        ProgressHandle handle = ProgressHandleFactory.createHandle("Loading the gui file");
        try {
            
            InputStream is = this.obj.getPrimaryFile().getInputStream();
            handle.start();
            ProjectAssetManager mgr = this.obj.getLookup().lookup(ProjectAssetManager.class);
            String assetPath = mgr.getAssetFolder().getPath();
            this.editor.createNewGui(nifty,is,new File(assetPath));
            this.view.newGui(this.editor.getGui());
            nodesManager.setRootContext(new GUINode(this.editor.getGui()));
            this.editor.getGui().getSelection().addObserver(this);
            Collection<GLayer> layers1 = this.editor.getGui().getLayers();
            guiID = this.editor.getGui().getGUIid();
            this.editor.getGui().addObserver(this);
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
        }finally{
           handle.finish();
        }
        
        
        
    }
    
    private class GuiSavable extends AbstractSavable {
    private final GUI gui;
   
    private final String filename;
    
    public GuiSavable(GUI gui,String filename){
        
        this.gui = gui;
        this.filename = filename;
        this.register();
        
        
    }
    @Override
    protected String findDisplayName() {
        return "Save "+ this.gui + " changes";
    }

    @Override
    protected void handleSave() throws IOException {
        try {
            GUIWriter writer = new GUIWriter(this.gui);
            writer.writeGUI(filename);
            NiftyGuiVisualElement.this.content.remove(this);
          
            
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (JAXBException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof com.jme3.gde.gui.view.NiftyGuiVisualElement.GuiSavable){
            return this.gui.equals(((com.jme3.gde.gui.view.NiftyGuiVisualElement.GuiSavable)obj).gui);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.gui.hashCode();
    }
    
}
}
