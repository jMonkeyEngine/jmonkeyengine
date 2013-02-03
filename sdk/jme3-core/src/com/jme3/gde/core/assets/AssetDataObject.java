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

import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetKey;
import com.jme3.export.Savable;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.gde.core.scene.ApplicationLogHandler.LogLevel;
import com.jme3.gde.core.scene.SceneApplication;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.BeanUtils;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
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
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author normenhansen
 */
@SuppressWarnings("unchecked")
public class AssetDataObject extends MultiDataObject {

    protected static final Logger logger = Logger.getLogger(AssetDataObject.class.getName());
    protected final InstanceContent lookupContents = new InstanceContent();
    protected final AbstractLookup contentLookup;
    protected final Lookup lookup;
    protected final AssetData assetData;
    protected final ProjectAssetManager assetManager;
    protected final AssetListListener listListener;
    protected final List<FileObject> assetList = new LinkedList<FileObject>();
    protected final List<AssetKey> assetKeyList = new LinkedList<AssetKey>();
    protected final List<AssetKey> failedList = new LinkedList<AssetKey>();
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
    protected AssetKey assetKey;
    protected Savable savable;
    protected String saveExtension;

    public AssetDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        contentLookup = new AbstractLookup(lookupContents);
        assetData = new AssetData(this);
        lookupContents.add(assetData);
        lookup = new ProxyLookup(getCookieSet().getLookup(), contentLookup);
        listListener = new AssetListListener(this, assetList, assetKeyList, failedList);
        assetManager = findAssetManager();
        //assign savecookie (same as method)
        setSaveCookie(saveCookie);
    }

    private ProjectAssetManager findAssetManager() {
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
                            return mgr;
                        }
                    }
                } catch (IOException ex) {
                } catch (IllegalArgumentException ex) {
                }
            }
            file = file.getParent();
        }
        return null;
    }

    @Override
    protected Node createNodeDelegate() {
        AssetDataNode node = new AssetDataNode(this, Children.LEAF, new ProxyLookup(getCookieSet().getLookup(), contentLookup));
        node.setIconBaseWithExtension("com/jme3/gde/core/icons/jme-logo.png");
        return node;
    }

    @Override
    public synchronized void setModified(boolean modif) {
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

    public synchronized void setSaveCookie(SaveCookie cookie) {
        this.saveCookie = cookie;
        getCookieSet().assign(SaveCookie.class, saveCookie);
        setModified(false);
    }

    /**
     * Loads the asset from the DataObject via the ProjectAssetManager in the
     * lookup. Returns the currently loaded asset when it has been loaded
     * already, close the asset using closeAsset().
     *
     * @return
     */
    public synchronized Savable loadAsset() {
        if (savable != null) {
            return savable;
        }
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("File is not part of a project!\nCannot load without ProjectAssetManager."));
            return null;
        }
        //make sure its actually closed and all data gets reloaded
        closeAsset();
        FileLock lock = null;
        try {
            lock = getPrimaryFile().lock();
            listListener.start();
            Savable spatial = (Savable) mgr.loadAsset(getAssetKey());
            listListener.stop();
            lock.releaseLock();
            savable = spatial;
            logger.log(Level.INFO, "Loaded asset {0}", getName());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
        return savable;
    }

    /**
     * Saves this asset, when a saveExtension is set, saves it as a brother file
     * with that extension.
     *
     * @throws IOException
     */
    public synchronized void saveAsset() throws IOException {
        if (savable == null) {
            logger.log(Level.WARNING, "Trying to write asset failed, asset data null!\nImport failed?");
            return;
        }
        final Savable savable = this.savable;
        ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Saving File..");
        progressHandle.start();
        BinaryExporter exp = BinaryExporter.getInstance();
//        FileLock lock = null;
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
//            if (lock != null) {
//                lock.releaseLock();
//            }
            if (out != null) {
                out.close();
            }
        }
        progressHandle.finish();
        setModified(false);
        logger.log(Level.INFO, "File {0} saved successfully", getPrimaryFile().getNameExt());
    }

    /**
     * Closes this asset so that loadAsset will cause it to be loaded
     */
    public synchronized void closeAsset() {
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr != null && savable != null) {
            //delete referenced assets too
            for (Iterator<AssetKey> it = assetKeyList.iterator(); it.hasNext();) {
                AssetKey assetKey1 = it.next();
                logger.log(Level.INFO, "Removing asset {0}, from cache via main asset {1}.", new Object[]{assetKey1.getName(), getName()});
                mgr.deleteFromCache(assetKey1);
            }
            savable = null;
        } else if (mgr == null) {
            logger.log(Level.WARNING, "Closing asset {0} with no ProjectAssetManager assigned..?", getName());
        }
    }

    /**
     * Returns the AssetKey of this asset type. When extending AssetDataObject
     * or a subtype the class should override this so the key type and
     * properties can be recognized properly:
     * <pre>
     * public synchronized MyKeyType getAssetKey() {
     *     //return key if already set
     *     if(super.getAssetKey() instanceof MyKeyType){
     *         return (MyKeyType)assetKey;
     *     }
     *     //set own key type and return
     *     assetKey = new MyKeyType(super.getAssetKey().getName());
     *     return (MyKeyType)assetKey;
     * }
     * </pre>
     *
     * @return
     */
    public synchronized AssetKey<?> getAssetKey() {
        if (assetKey == null) {
            ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
            if (mgr == null) {
                return null;
            }
            String assetKey = mgr.getRelativeAssetPath(getPrimaryFile().getPath());
            this.assetKey = new AssetKey<Object>(assetKey);
        }
        return assetKey;
    }

    /**
     * Applies the supplied keys data to the assets assetKey so it will be
     * loaded with these settings next time loadAsset is actually loading the
     * asset from the ProjectAssetManager.
     *
     * @param key
     */
    public synchronized void setAssetKeyData(AssetKey key) {
        try {
            BeanUtils.copyProperties(getAssetKey(), key);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Gets a list of FileObjects that represent all files that have been loaded
     * along this asset. This includes textures for models as well as materials
     * and other files.
     *
     * @return
     */
    public synchronized List<FileObject> getAssetList() {
        return new LinkedList<FileObject>(assetList);
    }

    /**
     * Gets a list of AssetKeys that represent all files that have been loaded
     * along this asset. This includes textures for models as well as materials
     * and other files.
     *
     * @return
     */
    public synchronized List<AssetKey> getAssetKeyList() {
        return new LinkedList<AssetKey>(assetKeyList);
    }

    /**
     * Gets a list of AssetKeys that represent all files that failed to load for
     * this asset. These were tried to be located but could not be found by the
     * import AssetManager.
     *
     * @return
     */
    public synchronized List<AssetKey> getFailedList() {
        return new LinkedList<AssetKey>(failedList);
    }

    protected static class AssetListListener implements AssetEventListener {

        private AssetDataObject obj;
        private List<FileObject> assetList;
        private List<AssetKey> assetKeyList;
        private List<AssetKey> failedList;
        private Thread loadingThread;

        public AssetListListener(AssetDataObject obj, List<FileObject> assetList, List<AssetKey> assetKeyList, List<AssetKey> failedList) {
            this.obj = obj;
            this.assetList = assetList;
            this.assetKeyList = assetKeyList;
            this.failedList = failedList;
        }

        public void assetLoaded(AssetKey ak) {
        }

        public void assetRequested(AssetKey ak) {
            ProjectAssetManager pm = obj.getLookup().lookup(ProjectAssetManager.class);
            if (pm == null || loadingThread != Thread.currentThread()) {
                return;
            }
            FileObject fObj = pm.getAssetFileObject(ak);
            if (fObj != null && !assetList.contains(fObj)) {
                assetList.add(fObj);
                assetKeyList.add(ak);
            }
        }

        public void assetDependencyNotFound(AssetKey ak, AssetKey ak1) {
            ProjectAssetManager pm = obj.getLookup().lookup(ProjectAssetManager.class);
            if (pm == null || loadingThread != Thread.currentThread()) {
                return;
            }
            FileObject fObj = pm.getAssetFileObject(ak1);
            if (fObj != null && assetList.contains(fObj)) {
                assetList.remove(fObj);
                assetKeyList.remove(ak1);
            }
            if (!failedList.contains(ak1)) {
                failedList.add(ak1);
            }
        }

        public void start() {
            ProjectAssetManager pm = obj.getLookup().lookup(ProjectAssetManager.class);
            loadingThread = Thread.currentThread();
            assetList.clear();
            assetKeyList.clear();
            failedList.clear();
            if (pm == null) {
                return;
            }
            pm.addAssetEventListener(this);
        }

        public void stop() {
            ProjectAssetManager pm = obj.getLookup().lookup(ProjectAssetManager.class);
            if (pm == null) {
                return;
            }
            pm.removeAssetEventListener(this);
        }
    };
}
