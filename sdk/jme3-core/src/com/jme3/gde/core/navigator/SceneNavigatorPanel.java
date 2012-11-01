package com.jme3.gde.core.navigator;

import com.jme3.gde.core.editor.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.sceneexplorer.nodes.NodeUtility;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.DefaultEditorKit;
import org.netbeans.spi.navigator.NavigatorPanel;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;

/**
 *
 * @author normenhansen
 */
@NavigatorPanel.Registration(displayName = "SceneExplorer", mimeType = "application/jme3model")
public class SceneNavigatorPanel extends JPanel implements NavigatorPanel, LookupListener, ExplorerManager.Provider {

    private final BeanTreeView beanTreeView = new BeanTreeView();
    private final Lookup lookup;
    private transient ExplorerManager explorerManager = new ExplorerManager();
    private Lookup.Result<SceneApplication> applicationResult;
    private final Result<AbstractSceneExplorerNode> nodeSelectionResult;
    //ExplorerNode selection listener, does nothing
    private LookupListener listener = new LookupListener() {
        private Node selectedNode;

        public void resultChanged(LookupEvent ev) {
            Collection collection = nodeSelectionResult.allInstances();
            for (Iterator it = collection.iterator(); it.hasNext();) {
                Object obj = it.next();
                if (obj instanceof AbstractSceneExplorerNode) {
                    AbstractSceneExplorerNode node = (AbstractSceneExplorerNode) obj;
                    if (node != null) {
                        if (selectedNode != null) {
                            selectedNode = null;
                        }
                        selectedNode = node;
                        return;
                    }
                }
            }
            if (selectedNode != null) {
                selectedNode = null;
            }
        }
    };

    public SceneNavigatorPanel() {
        ActionMap map = getActionMap();
        Action copyAction = ExplorerUtils.actionCopy(explorerManager);
        map.put(DefaultEditorKit.copyAction, copyAction);
        map.put(DefaultEditorKit.cutAction, ExplorerUtils.actionCut(explorerManager));
        map.put(DefaultEditorKit.pasteAction, ExplorerUtils.actionPaste(explorerManager));
        map.put("delete", ExplorerUtils.actionDelete(explorerManager, true)); // or false

        lookup = ExplorerUtils.createLookup(explorerManager, map);

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));
        add(beanTreeView);

        nodeSelectionResult = Utilities.actionsGlobalContext().lookupResult(AbstractSceneExplorerNode.class);
    }

    public Lookup getLookup() {
        return lookup;
    }

    public String getDisplayName() {
        return "SceneExplorer";
    }

    public String getDisplayHint() {
        return "Hint";
    }

    public JComponent getComponent() {
        return this;
    }

    public void panelActivated(Lookup lkp) {
        ExplorerUtils.activateActions(explorerManager, true);
        applicationResult = lkp.lookupResult(SceneApplication.class);
        applicationResult.addLookupListener(this);
        nodeSelectionResult.addLookupListener(listener);
    }

    public void panelDeactivated() {
        ExplorerUtils.activateActions(explorerManager, false);
        applicationResult.removeLookupListener(this);
        nodeSelectionResult.removeLookupListener(listener);
        explorerManager.setRootContext(Node.EMPTY);
    }

    /**
     * result listener for application start
     */
    public void resultChanged(LookupEvent ev) {
//        System.out.println("Select Thread: " + Thread.currentThread().getName());
        Collection collection = applicationResult.allInstances();
        for (Iterator it = collection.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof SceneApplication) {
                SceneApplication app = (SceneApplication) obj;
                if (app != null) {
                    Node node = NodeUtility.createNode(((SceneApplication) app).getRootNode());
                    explorerManager.setRootContext(node);
                    explorerManager.getRootContext().setDisplayName(node.getName());
                    return;
                }
            }
        }
        explorerManager.setRootContext(Node.EMPTY);
    }

    public ExplorerManager getExplorerManager() {
        return explorerManager;
    }
}