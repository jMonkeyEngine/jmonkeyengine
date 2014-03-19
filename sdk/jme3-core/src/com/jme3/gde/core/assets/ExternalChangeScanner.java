/*
 * Copyright (c) 2003-2012 jMonkeyEngine
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
package com.jme3.gde.core.assets;

import com.jme3.export.Savable;
import com.jme3.gde.core.scene.ApplicationLogHandler;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.util.SpatialUtil;
import com.jme3.scene.Spatial;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;

/**
 * This class scans for external changes of a j3o models original file and tries
 * to update the data when it changed.
 *
 * @author normenhansen
 */
public class ExternalChangeScanner implements AssetDataPropertyChangeListener, FileChangeListener {

    private static final Logger logger = Logger.getLogger(ExternalChangeScanner.class.getName());
    private static final AtomicBoolean userNotified = new AtomicBoolean(false);
    protected final AssetDataObject assetDataObject;
    protected final AssetData assetData;
    protected FileObject originalObject;

    public ExternalChangeScanner(AssetDataObject assetDataObject) {
        this.assetDataObject = assetDataObject;
        assetData = assetDataObject.getLookup().lookup(AssetData.class);
        if (assetData != null) {
            String path = assetData.getProperty("ORIGINAL_PATH");
            if (path != null) {
                setObservedFilePath(path);
            }
            assetData.addPropertyChangeListener(this);
            final ExternalChangeScanner main = this;
            assetDataObject.getPrimaryFile().addFileChangeListener(new FileChangeAdapter() {
                @Override
                public void fileDeleted(FileEvent fe) {
                    logger.log(Level.INFO, "File {0} deleted, remove!", new Object[]{fe.getFile()});
                    assetData.removePropertyChangeListener(main);
                    fe.getFile().removeFileChangeListener(this);
                    if (originalObject != null) {
                        logger.log(Level.INFO, "Remove file change listener for {0}", originalObject);
                        originalObject.removeFileChangeListener(main);
                        originalObject = null;
                    }
                }
            });
        } else {
            logger.log(Level.WARNING, "Trying to observer changes for asset {0} which has no AssetData in Lookup.", assetDataObject.getName());
        }
    }

    private void notifyUser() {
        if (!userNotified.getAndSet(true)) {
            //TODO: execute on separate thread?
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    NotifyDescriptor.Confirmation mesg = new NotifyDescriptor.Confirmation("Original file for " + assetDataObject.getName() + " changed\nTry and reapply mesh data to j3o file?",
                            "Original file changed",
                            NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.QUESTION_MESSAGE);
                    DialogDisplayer.getDefault().notify(mesg);
                    if (mesg.getValue() != NotifyDescriptor.Confirmation.YES_OPTION) {
                        userNotified.set(false);
                        return;
                    }
                    SceneApplication.getApplication().enqueue(new Callable<Void>() {
                        public Void call() throws Exception {
                            applyExternalData();
                            return null;
                        }
                    });
                    userNotified.set(false);
                }
            });
        } else {
            logger.log(Level.INFO, "User already notified about change in {0}", assetDataObject.getName());
        }
    }

    private void applyExternalData() {
        ProgressHandle handle = ProgressHandleFactory.createHandle("Updating file data");
        handle.start();
        try {
            Spatial original = loadOriginalSpatial();
            Spatial spat = (Spatial) assetDataObject.loadAsset();
            SpatialUtil.updateMeshDataFromOriginal(spat, original);
            closeOriginalSpatial();
//            NotifyDescriptor.Confirmation mesg = new NotifyDescriptor.Confirmation("Model appears to have animations, try to import as well?\nCurrently this will unlink attachment Nodes and clear\nadded effects tracks.",
//                    "Animations Available",
//                    NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.QUESTION_MESSAGE);
//            DialogDisplayer.getDefault().notify(mesg);
//            if (mesg.getValue() == NotifyDescriptor.Confirmation.YES_OPTION) {
//                SpatialUtil.updateAnimControlDataFromOriginal(spat, original);
//            }
            assetDataObject.saveAsset();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception when trying to update external data.", e);
        } finally {
            handle.finish();
        }
    }

    private Spatial loadOriginalSpatial() {
        try {
            DataObject dobj = DataObject.find(originalObject);
            AssetData originalAssetData = dobj.getLookup().lookup(AssetData.class);
            if (originalAssetData != null) {
                Savable sav = originalAssetData.loadAsset();
                if (sav instanceof Spatial) {
                    return (Spatial) sav;
                } else {
                    logger.log(Level.SEVERE, "Trying to load original for {0} but it is not a Spatial: {1}", new Object[]{assetDataObject.getName(), originalObject});
                }
            } else {
                logger.log(Level.WARNING, "Could not get AssetData for {0}, original file {1}", new Object[]{assetDataObject.getName(), originalObject});
            }
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    private Spatial closeOriginalSpatial() {
        try {
            DataObject dobj = DataObject.find(originalObject);
            AssetData originalAssetData = dobj.getLookup().lookup(AssetData.class);
            if (originalAssetData != null) {
                originalAssetData.closeAsset();
            } else {
                logger.log(Level.WARNING, "Could not get AssetData for {0}, original file {1}", new Object[]{assetDataObject.getName(), originalObject});
            }
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    private void setObservedFilePath(String assetName) {
        ProjectAssetManager mgr = assetDataObject.getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            logger.log(Level.WARNING, "File is not part of a jME project but tries to find original model...");
            return;
        }
        FileObject fileObject = mgr.getAssetFileObject(assetName);
        //ignoring same file -> old properties files
        if (fileObject != null) {
            if (!fileObject.equals(assetDataObject.getPrimaryFile())) {
                if (originalObject != null) {
                    originalObject.removeFileChangeListener(this);
                    logger.log(Level.INFO, "{0} stops listening for external changes on {1}", new Object[]{assetDataObject.getName(), originalObject});
                }
                fileObject.addFileChangeListener(this);
                logger.log(Level.INFO, "{0} listening for external changes on {1}", new Object[]{assetDataObject.getName(), fileObject});
                originalObject = fileObject;
            } else {
                logger.log(Level.FINE, "Ignoring old reference to self for {0}", assetDataObject.getName());
            }
        } else {
            logger.log(Level.INFO, "Could not get FileObject for {0} when trying to opdate original data for {1}. Possibly deleted.", new Object[]{assetName, assetDataObject.getName()});
            //TODO: add folder listener for when recreated
        }
    }

    @Override
    public void assetDataPropertyChanged(String property, String before, String after) {
        if ("ORIGINAL_PATH".equals(property)) {
            logger.log(Level.INFO, "Notified about change in AssetData properties for {0}", assetDataObject.getName());
            setObservedFilePath(after);
        }
    }

    public void fileFolderCreated(FileEvent fe) {
    }

    public void fileDataCreated(FileEvent fe) {
    }

    public void fileChanged(FileEvent fe) {
        logger.log(Level.INFO, "External file {0} for {1} changed!", new Object[]{fe.getFile(), assetDataObject.getName()});
        notifyUser();
    }

    public void fileDeleted(FileEvent fe) {
        logger.log(Level.INFO, "External file {0} for {1} deleted!", new Object[]{fe.getFile(), assetDataObject.getName()});
        if (originalObject != null) {
            logger.log(ApplicationLogHandler.LogLevel.INFO, "Remove file change listener for deleted object on {0}", assetDataObject.getName());
            originalObject.removeFileChangeListener(this);
            originalObject = null;
        }
        //TODO: add folder listener for when recreated
    }

    public void fileRenamed(FileRenameEvent fe) {
        logger.log(Level.INFO, "External file {0} for {1} renamed!", new Object[]{fe.getFile(), assetDataObject.getName()});
        if (originalObject != null) {
            logger.log(Level.INFO, "Remove file change listener for renamed object on {0}", assetDataObject.getName());
            originalObject.removeFileChangeListener(this);
            originalObject = null;
        }
    }

    public void fileAttributeChanged(FileAttributeEvent fe) {
    }
}
