/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.gde.materialdefinition;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.materialdefinition.navigator.MatDefNavigatorPanel;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;

@Messages({
    "LBL_MatDef_LOADER=JME Material definition"
})
@MIMEResolver.ExtensionRegistration(
    displayName = "#LBL_MatDef_LOADER",
mimeType = "text/jme-materialdefinition",
extension = {"j3md", "J3MD"})
@DataObject.Registration(
    mimeType = "text/jme-materialdefinition",
iconBase = "com/jme3/gde/materialdefinition/icons/matdef.png",
displayName = "#LBL_MatDef_LOADER",
position = 300)
@ActionReferences({
    @ActionReference(
        path = "Loaders/text/jme-materialdefinition/Actions",
    id =
    @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
    position = 100,
    separatorAfter = 200),
    @ActionReference(
        path = "Loaders/text/jme-materialdefinition/Actions",
    id =
    @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
    position = 300),
    @ActionReference(
        path = "Loaders/text/jme-materialdefinition/Actions",
    id =
    @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
    position = 400,
    separatorAfter = 500),
    @ActionReference(
        path = "Loaders/text/jme-materialdefinition/Actions",
    id =
    @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
    position = 600),
    @ActionReference(
        path = "Loaders/text/jme-materialdefinition/Actions",
    id =
    @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
    position = 700,
    separatorAfter = 800),
    @ActionReference(
        path = "Loaders/text/jme-materialdefinition/Actions",
    id =
    @ActionID(category = "System", id = "org.openide.actions.SaveAsTemplateAction"),
    position = 900,
    separatorAfter = 1000),
    @ActionReference(
        path = "Loaders/text/jme-materialdefinition/Actions",
    id =
    @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
    position = 1100,
    separatorAfter = 1200),
    @ActionReference(
        path = "Loaders/text/jme-materialdefinition/Actions",
    id =
    @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
    position = 1300),
    @ActionReference(
        path = "Loaders/text/jme-materialdefinition/Actions",
    id =
    @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
    position = 1400)
})
public class MatDefDataObject extends MultiDataObject {

    protected final Lookup lookup;
    protected final InstanceContent lookupContents = new InstanceContent();
    protected AbstractLookup contentLookup;
    private EditableMatDefFile file = null;
    private boolean loaded = false;

    public MatDefDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        registerEditor("text/jme-materialdefinition", true);
        contentLookup = new AbstractLookup(lookupContents);
        lookupContents.add(this);
        lookup = new ProxyLookup(getCookieSet().getLookup(), contentLookup);
        findAssetManager();
        final MatDefMetaData metaData = new MatDefMetaData(this);
        lookupContents.add(metaData);
        pf.addFileChangeListener(new FileChangeAdapter() {
            @Override
            public void fileChanged(FileEvent fe) {
                super.fileChanged(fe);
                metaData.save();
                if (file.isDirty()) {
                    file.setLoaded(false);
                    file.setDirty(false);
                }
            }
        });

    }

    private void findAssetManager() {
        FileObject primaryFile = getPrimaryFile();
        ProjectManager pm = ProjectManager.getDefault();
        while (primaryFile != null) {
            if (primaryFile.isFolder() && pm.isProject(primaryFile)) {
                try {
                    Project project = ProjectManager.getDefault().findProject(primaryFile);
                    if (project != null) {
                        getLookupContents().add(project);
                        ProjectAssetManager mgr = project.getLookup().lookup(ProjectAssetManager.class);
                        if (mgr != null) {
                            getLookupContents().add(mgr);
                            return;
                        }
                    }
                } catch (IOException ex) {
                } catch (IllegalArgumentException ex) {
                }
            }
            primaryFile = primaryFile.getParent();
        }
//        getLookupContents().add(new ProjectAssetManager(file.getParent()));
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @MultiViewElement.Registration(
        displayName = "#LBL_MatDef_EDITOR",
    iconBase = "com/jme3/gde/materialdefinition/icons/matdef.png",
    mimeType = "text/jme-materialdefinition",
    persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
    preferredID = "MatDef",
    position = 1000)
    @Messages("LBL_MatDef_EDITOR=Text")
    public static MultiViewEditorElement createEditor(Lookup lkp) {
        final MatDefDataObject obj = lkp.lookup(MatDefDataObject.class);
        obj.loaded = true;
        MatDefNavigatorPanel nav = obj.getLookup().lookup(MatDefNavigatorPanel.class);
        if (nav != null) {
            nav.updateData(obj);
        }
        MultiViewEditorElement ed = new MultiViewEditorElement(lkp) {
            KeyListener listener = new KeyListener() {
                public void keyTyped(KeyEvent e) {
                }

                public void keyPressed(KeyEvent e) {
                    EditableMatDefFile f = obj.getEditableFile();
                    if (f != null) {
                        f.setDirty(true);
                    }
                }

                public void keyReleased(KeyEvent e) {                   
                }
            };

            @Override
            public void componentActivated() {
                super.componentActivated();
                getEditorPane().addKeyListener(listener);
            }

            @Override
            public void componentDeactivated() {
                super.componentDeactivated();
                getEditorPane().removeKeyListener(listener);
            }

            @Override
            public void componentClosed() {
                super.componentClosed();
                obj.unload();
            }
        };


        return ed;
    }

    @Override
    protected void handleDelete() throws IOException {
        MatDefMetaData metaData = lookup.lookup(MatDefMetaData.class);
        metaData.cleanup();
        super.handleDelete();
    }

    @Override
    protected FileObject handleRename(String name) throws IOException {
        MatDefMetaData metaData = lookup.lookup(MatDefMetaData.class);
        metaData.rename(null, name);
        return super.handleRename(name);
    }

    @Override
    protected FileObject handleMove(DataFolder df) throws IOException {
        MatDefMetaData metaData = lookup.lookup(MatDefMetaData.class);
        metaData.rename(df, null);
        return super.handleMove(df);
    }

    @Override
    protected DataObject handleCopy(DataFolder df) throws IOException {
        MatDefMetaData metaData = lookup.lookup(MatDefMetaData.class);
        metaData.duplicate(df, null);
        return super.handleCopy(df);
    }

    @Override
    protected DataObject handleCopyRename(DataFolder df, String name, String ext) throws IOException {
        MatDefMetaData metaData = lookup.lookup(MatDefMetaData.class);
        metaData.duplicate(df, name);
        return super.handleCopyRename(df, name, ext);
    }

    public EditableMatDefFile getEditableFile() {
        if (file == null) {
            file = new EditableMatDefFile(getLookup());
        }

        return file;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void unload() {
        if (loaded) {
            loaded = false;
            getLookup().lookup(MatDefNavigatorPanel.class).updateData(null);
        }
    }

    public InstanceContent getLookupContents() {
        return lookupContents;
    }
//    @Override
//    public synchronized void saveAsset() throws IOException {
//        
////        ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Saving File..");
////        progressHandle.start();
// //      BinaryExporter exp = BinaryExporter.getInstance();
//        FileLock lock = null;
//        OutputStream out = null;
//        try {
//             PrintWriter to = new PrintWriter(getPrimaryFile().getOutputStream(lock));
//            try {
//                to.print(getEditableFile().getMatDefStructure().toString());
//              
//            } finally {
//                to.close();
//            }
//        } finally {
//            if (lock != null) {
//                lock.releaseLock();
//            }
//            if (out != null) {
//                out.close();
//            }
//        }
//     //   progressHandle.finish();
//        StatusDisplayer.getDefault().setStatusText(getPrimaryFile().getNameExt() + " saved.");
//        setModified(false);
//        
////        getPrimaryFile().
////                getOutputStream().write(getEditableFile().getMatDefStructure().toString().getBytes());        
//       
//    }
}
