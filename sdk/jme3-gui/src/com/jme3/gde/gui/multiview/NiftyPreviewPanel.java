/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.multiview;

import com.jme3.audio.AudioRenderer;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.OffScenePanel;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.gui.NiftyGuiDataObject;
import com.jme3.renderer.ViewPort;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.tools.resourceloader.FileSystemLocation;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import org.netbeans.modules.xml.multiview.Error;
import org.netbeans.modules.xml.multiview.ui.PanelView;
import org.netbeans.modules.xml.multiview.ui.ToolBarDesignEditor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Message;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import uk.co.mandolane.midi.e;

/**
 *
 * @author normenhansen
 */
public class NiftyPreviewPanel extends PanelView {

    private NiftyGuiDataObject niftyObject;
    private OffScenePanel offPanel;
    private Nifty nifty;
    private Document doc;
    private ToolBarDesignEditor comp;
    private String screen = "";
    private NiftyPreviewInputHandler inputHandler;
    private NiftyJmeDisplay niftyDisplay;
    private JScrollPane scrollPanel;

    public NiftyPreviewPanel(NiftyGuiDataObject niftyObject, ToolBarDesignEditor comp) {
        super();
        setRoot(Node.EMPTY);
        this.niftyObject = niftyObject;
        this.comp = comp;
        comp.setContentView(this);
        preparePreview();
        updatePreView();
    }

    private void createToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setPreferredSize(new Dimension(10000, 24));
        toolBar.setMaximumSize(new Dimension(10000, 24));
        toolBar.setFloatable(false);
        JComboBox comboBox = new JComboBox(new String[]{"640x480", "800x600", "1024x768", "1280x720"});
        comboBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                String string = (String) e.getItem();
                final int width;
                final int height;
                if ("640x480".equals(string)) {
                    width = 640;
                    height = 480;
                } else if ("1024x768".equals(string)) {
                    width = 1024;
                    height = 768;
                } else if ("1280x720".equals(string)) {
                    width = 1280;
                    height = 720;
                } else if ("800x600".equals(string)) {
                    width = 800;
                    height = 600;
                } else{
                    width = 640;
                    height = 480;
                }
                offPanel.resizeGLView(width, height);
                SceneApplication.getApplication().enqueue(new Callable<Object>() {

                    public Object call() throws Exception {
                        niftyDisplay.reshape(offPanel.getViewPort(), width, height);
                        return null;
                    }
                });
                updatePreView(screen);
            }
        });
        toolBar.add(comboBox);
        toolBar.add(new JPanel());
        add(toolBar);
    }


    public void updatePreView() {
        updatePreView(screen);
    }

    public void updatePreView(final String screen) {
        final ProjectAssetManager pm = niftyObject.getLookup().lookup(ProjectAssetManager.class);
        if (pm == null) {
            Logger.getLogger(NiftyPreviewPanel.class.getName()).log(Level.WARNING, "No Project AssetManager found!");
        }
        try {
            doc = XMLUtil.parse(new InputSource(niftyObject.getPrimaryFile().getInputStream()), false, false, null, null);
            NiftyFileNode rootContext = new NiftyFileNode(doc.getDocumentElement());
            setRoot(rootContext);
            comp.setRootContext(rootContext);
        } catch (Exception ex) {
            Message msg = new NotifyDescriptor.Message(
                    "Error parsing File:" + ex,
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notifyLater(msg);
            Exceptions.printStackTrace(ex);
            return;
        }
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                nifty.fromXml(pm.getRelativeAssetPath(niftyObject.getPrimaryFile().getPath()), screen);
                return null;
            }
        });
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                validateTree();
            }
        });
    }

    @Override
    public void initComponents() {
        super.initComponents();
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.PAGE_AXIS));
        createToolbar();
        scrollPanel = new JScrollPane();
        offPanel = new OffScenePanel(640, 480);
        scrollPanel.getViewport().add(offPanel);
        add(scrollPanel);
        offPanel.startPreview();
        prepareInputHandler();
    }

    private void prepareInputHandler() {
        inputHandler = new NiftyPreviewInputHandler();
        offPanel.addMouseMotionListener(new MouseMotionListener() {

            public void mouseDragged(MouseEvent e) {
                inputHandler.addMouseEvent(e.getX(), e.getY(), e.getButton() == MouseEvent.NOBUTTON ? false : true);
            }

            public void mouseMoved(MouseEvent e) {
                inputHandler.addMouseEvent(e.getX(), e.getY(), e.getButton() == MouseEvent.NOBUTTON ? false : true);
            }
        });
        offPanel.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
                inputHandler.addMouseEvent(e.getX(), e.getY(), e.getButton() == MouseEvent.NOBUTTON ? false : true);
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });
        offPanel.addKeyListener(new KeyListener() {

            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                inputHandler.addKeyEvent(e.getKeyCode(), e.getKeyChar(), true, e.isShiftDown(), e.isControlDown());
            }

            public void keyReleased(KeyEvent e) {
            }
        });
    }

    private void preparePreview() {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                ViewPort guiViewPort = offPanel.getViewPort();
                ProjectAssetManager pm = niftyObject.getLookup().lookup(ProjectAssetManager.class);
                if (pm == null) {
                    Logger.getLogger(NiftyPreviewPanel.class.getName()).log(Level.WARNING, "No Project AssetManager found!");
                    return null;
                }
                AudioRenderer audioRenderer = SceneApplication.getApplication().getAudioRenderer();
                niftyDisplay = new NiftyJmeDisplay(pm,
                        inputHandler,
                        audioRenderer,
                        guiViewPort);
                nifty = niftyDisplay.getNifty();
                de.lessvoid.nifty.tools.resourceloader.ResourceLoader.addResourceLocation(new FileSystemLocation(new File(pm.getAssetFolderName())));

                // attach the nifty display to the gui view port as a processor
                guiViewPort.addProcessor(niftyDisplay);
                return null;
            }
        });
    }

    @Override
    protected Error validateView() {
        return null;
    }

    @Override
    public void showSelection(Node[] nodes) {
        this.screen = nodes[0].getName();
        updatePreView();
    }

    public void cleanup() {
        offPanel.stopPreview();
        nifty.exit();
    }
}
