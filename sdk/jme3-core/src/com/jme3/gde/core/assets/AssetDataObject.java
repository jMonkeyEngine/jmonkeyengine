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
package com.jme3.gde.core.assets;

import com.jme3.asset.AssetKey;
import com.jme3.export.Savable;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.gde.core.scene.SceneApplication;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author normenhansen
 */
public class AssetDataObject extends MultiDataObject {

    protected final Lookup lookup;
    protected final InstanceContent lookupContents = new InstanceContent();
    protected SaveCookie saveCookie = new SaveCookie() {

        public void save() throws IOException {
            //TODO: On OpenGL thread? -- safest way.. with get()?
            SceneApplication.getApplication().enqueue(new Callable() {

                public Object call() throws Exception {
                    saveAsset();
                    return null;
                }
            });
        }
    };
    protected DataNode dataNode;
    protected Savable savable;
    protected String saveExtension;

    public AssetDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        lookup = new ProxyLookup(getCookieSet().getLookup(), new AbstractLookup(getLookupContents()), Lookups.fixed(new AssetData(this)));
        setSaveCookie(saveCookie);
        findAssetManager();
    }

    protected void findAssetManager() {
        FileObject file = getPrimaryFile();
        ProjectManager pm = ProjectManager.getDefault();
        while (file != null) {
            if (file.isFolder() && pm.isProject(file)) {
                try {
                    Project project = ProjectManager.getDefault().findProject(file);
                    if (project != null) {
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
            file = file.getParent();
        }
    }

    @Override
    protected Node createNodeDelegate() {
        DataNode node = new DataNode(this, Children.LEAF, getLookup());
        node.setIconBaseWithExtension("com/jme3/gde/core/assets/jme-logo.png");
        return node;
    }

    @Override
    public void setModified(boolean modif) {
        super.setModified(modif);
        if (modif && saveCookie != null) {
            getCookieSet().assign(SaveCookie.class, saveCookie);
        } else {
            getCookieSet().assign(SaveCookie.class);
        }
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    public InstanceContent getLookupContents() {
        return lookupContents;
    }

    public void setSaveCookie(SaveCookie cookie) {
        this.saveCookie = cookie;
        getCookieSet().assign(SaveCookie.class, saveCookie);
        setModified(false);
    }

    //TODO: make save as j3o
    public Savable loadAsset() {
        if (isModified() && savable != null) {
            return savable;
        }
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            return null;
        }
        String assetKey = mgr.getRelativeAssetPath(getPrimaryFile().getPath());
        FileLock lock = null;
        try {
            lock = getPrimaryFile().lock();
            Savable spatial = (Savable) mgr.loadAsset(new AssetKey(assetKey));
            savable = spatial;
            lock.releaseLock();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
        return savable;
    }

    public void saveAsset() throws IOException {
        if (savable == null) {
            Logger.getLogger(AssetDataObject.class.getName()).log(Level.WARNING, "Trying to save asset that has not been loaded before or does not support saving!");
            return;
        }
        final Savable savable = this.savable;
        ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Saving File..");
        progressHandle.start();
        BinaryExporter exp = BinaryExporter.getInstance();
        FileLock lock = null;
        OutputStream out = null;
        try {
            if (saveExtension == null) {
                out = getPrimaryFile().getOutputStream();
            } else {
                FileObject outFileObject = getPrimaryFile().getParent().getFileObject(getPrimaryFile().getName(), saveExtension);
                if (outFileObject == null) {
                    outFileObject = getPrimaryFile().getParent().createData(getPrimaryFile().getName(), saveExtension);
                }
                out = outFileObject.getOutputStream();
                outFileObject.getParent().refresh();
            }
            exp.save(savable, out);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
            if (out != null) {
                out.close();
            }
        }
        progressHandle.finish();
        StatusDisplayer.getDefault().setStatusText(getPrimaryFile().getNameExt() + " saved.");
        setModified(false);
    }

    public AssetKey<?> getAssetKey() {
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            return null;
        }
        String assetKey = mgr.getRelativeAssetPath(getPrimaryFile().getPath());
        return new AssetKey<Object>(assetKey);
    }
}
