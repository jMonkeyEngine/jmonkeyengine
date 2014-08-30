/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui;

import com.jme3.gde.core.assets.ProjectAssetManager;
import jada.ngeditor.controller.CommandProcessor;
import jada.ngeditor.controller.GUIEditor;
import java.io.IOException;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@Messages({
    "LBL_NiftyGui_LOADER=Files of NiftyGui"
})
@MIMEResolver.Registration(
        displayName = "#LBL_NiftyGui_LOADER",
        resource = "NiftyGuiResolver.xml")
@DataObject.Registration(
        mimeType = "text/x-niftygui+xml",
        iconBase = "com/jme3/gde/gui/multiview/icons/gui-icon.png",
        displayName = "#LBL_NiftyGui_LOADER",
        position = 300)
@ActionReferences({
    @ActionReference(
            path = "Loaders/text/x-niftygui+xml/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200),
    @ActionReference(
            path = "Loaders/text/x-niftygui+xml/Actions",
            id =
            @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
            position = 300),
    @ActionReference(
            path = "Loaders/text/x-niftygui+xml/Actions",
            id =
            @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
            position = 400,
            separatorAfter = 500),
    @ActionReference(
            path = "Loaders/text/x-niftygui+xml/Actions",
            id =
            @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600),
    @ActionReference(
            path = "Loaders/text/x-niftygui+xml/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
            position = 700,
            separatorAfter = 800),
    @ActionReference(
            path = "Loaders/text/x-niftygui+xml/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"),
            position = 900,
            separatorAfter = 1000),
    @ActionReference(
            path = "Loaders/text/x-niftygui+xml/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
            position = 1100,
            separatorAfter = 1200),
    @ActionReference(
            path = "Loaders/text/x-niftygui+xml/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1300),
    @ActionReference(
            path = "Loaders/text/x-niftygui+xml/Actions",
            id =
            @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400)
})
public class NiftyGuiDataObject extends MultiDataObject {

    public NiftyGuiDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        registerEditor("text/x-niftygui+xml", true);
        findAssetManager();
        this.getCookieSet().assign(GUIEditor.class, CommandProcessor.getInstance().getGuiEditor());
    }

    protected final void findAssetManager() {
        FileObject file = getPrimaryFile();
        ProjectManager pm = ProjectManager.getDefault();
        while (file != null) {
            if (file.isFolder() && pm.isProject(file)) {
                try {
                    Project project = ProjectManager.getDefault().findProject(file);
                    if (project != null) {
                        ProjectAssetManager mgr = project.getLookup().lookup(ProjectAssetManager.class);
                        if (mgr != null) {
                            this.getCookieSet().assign(mgr.getClass(),mgr);
                            return;
                        }
                    }
                } catch (IOException ex) {
                } catch (IllegalArgumentException ex) {
                }
            }
            file = file.getParent();
        }
    }

    @Override
    protected Node createNodeDelegate() {
        DataNode node = new DataNode(this, Children.LEAF, getLookup());
        node.setIconBaseWithExtension("com/jme3/gde/gui/multiview/icons/gui-icon.png");
        return node;
    }
    
    @Override
    protected int associateLookup() {
        return 1;
    }

    @MultiViewElement.Registration(
            displayName = "#LBL_NiftyGui_EDITOR",
            iconBase = "com/jme3/gde/gui/multiview/icons/gui-icon.png",
            mimeType = "text/x-niftygui+xml",
            persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
            preferredID = "NiftyGui",
            position = 1000)
    @Messages("LBL_NiftyGui_EDITOR=XML")
    public static MultiViewEditorElement createEditor(Lookup lkp) {
        final MultiViewEditorElement multiViewEditorElement = new MultiViewEditorElement(lkp);
        return multiViewEditorElement;
    }
}
