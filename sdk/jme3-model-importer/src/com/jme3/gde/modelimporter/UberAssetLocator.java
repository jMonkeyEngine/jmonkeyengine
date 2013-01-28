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
package com.jme3.gde.modelimporter;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 * Asset locator that tries to find a file across the filesystem or get user
 * input on where the file is.
 *
 * @author normenhansen
 */
public class UberAssetLocator implements AssetLocator {
    //ugly static due to Locator instantiation

    private static final Logger logger = Logger.getLogger(UberAssetLocator.class.getName());
    private static final List<UberAssetInfo> locatedAssets = new ArrayList<UberAssetInfo>();
    private static boolean findMode = true;
    private static String assetBaseFolder;

    public static boolean isFindMode() {
        return findMode;
    }

    public static void setFindMode(boolean aFindMode) {
        findMode = aFindMode;
    }

    public static String getAssetBaseFolder() {
        return assetBaseFolder;
    }

    public static void setAssetBaseFolder(String aAssetBaseFolder) {
        assetBaseFolder = aAssetBaseFolder;
    }
    private String rootPath;

    public static void resetLocatedList() {
        logger.log(Level.INFO, "Clearing asset List");
        locatedAssets.clear();
    }

    public static List<UberAssetInfo> getLocatedList() {
        return new ArrayList<UberAssetInfo>(locatedAssets);
    }

    public static UberAssetInfo getInfo(AssetKey key) {
        if (locatedAssets.isEmpty()) {
            logger.log(Level.INFO, "Looking in empty list for {0}", key.getName());
        }
        for (UberAssetInfo uberAssetInfo : locatedAssets) {
            String normalName = uberAssetInfo.getKey().getName();
            if (assetBaseFolder != null) {
                //sanitize filename by creating new asset key
                String extendedName = new AssetKey(assetBaseFolder + normalName).getName();
                logger.log(Level.INFO, "Looking for extended name {0}", extendedName);
                if (extendedName.equals(key.getName())) {
                    logger.log(Level.INFO, "Found extended name {0}", extendedName);
                    return uberAssetInfo;
                }
            }
            logger.log(Level.INFO, "Looking for normal name {0}", normalName);
            if (normalName.equals(key.getName())) {
                logger.log(Level.INFO, "Found normal name {0}", normalName);
                return uberAssetInfo;
            }
        }
        return null;
    }

    public UberAssetLocator() {
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public AssetInfo locate(AssetManager manager, AssetKey key) {
        //we only locate texture keys atm
        if (!(key instanceof TextureKey)) {
            return null;
        }
        AssetInfo existing = getInfo(key);
        if (existing != null) {
            return existing;
        }
        if (findMode) {
            FileObject file = findFile(key);
            if (file == null) {
                return null;
            }
            logger.log(Level.INFO, "Storing location for {0}", key.getName());
            UberAssetInfo info = new UberAssetInfo(file, manager, key);
            locatedAssets.add(info);
            return info;
        }
        return null;
    }

    private FileObject findFile(AssetKey key) {
        //TODO: better attempt to actually find file.. :)
        String rootPath = this.rootPath != null ? this.rootPath.replace("\\", "/") : null;
        if (rootPath != null) {
            File file = new File(rootPath + key.getName());
            file = FileUtil.normalizeFile(file);
            FileObject fileObject = FileUtil.toFileObject(file);
            if (fileObject != null) {
                logger.log(Level.INFO, "Found file {0}" + fileObject);
                return fileObject;
            }
        }
        File file = new File(key.getName());
        file = FileUtil.normalizeFile(file);
        FileObject fileObject = FileUtil.toFileObject(file);
        if (fileObject != null) {
            logger.log(Level.INFO, "Found file {0}" + fileObject);
            return fileObject;
        }
        return getUserPath(key);
    }

    private FileObject getUserPath(AssetKey key) {
        NotifyDescriptor.Confirmation msg = new NotifyDescriptor.Confirmation(
                "Referenced file " + key.getName() + " cannot be found!\nDo you want to look for it?",
                NotifyDescriptor.YES_NO_OPTION,
                NotifyDescriptor.WARNING_MESSAGE);
        Object result = DialogDisplayer.getDefault().notify(msg);
        if (!NotifyDescriptor.YES_OPTION.equals(result)) {
            return null;
        }
        final String ext = key.getExtension();
        FileChooserBuilder fcb = new FileChooserBuilder(this.getClass());
        fcb.setTitle("Locate " + key.getName());
        fcb.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.getName().endsWith(ext)) {
                    return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Filter for " + ext;
            }
        });
        fcb.setFilesOnly(true);
        fcb.setApproveText("Select file");
        File file = fcb.showOpenDialog();
        if (file == null) {
            return null;
        }
        logger.log(Level.INFO, "Got user file input");
        file = FileUtil.normalizeFile(file);
        return FileUtil.toFileObject(file);
    }

    public static class UberAssetInfo extends AssetInfo {

        final FileObject file;
        String newAssetName;

        public UberAssetInfo(FileObject file, AssetManager manager, AssetKey key) {
            super(manager, key);
            this.file = file;
        }

        public FileObject getFileObject() {
            return file;
        }

        public String getNewAssetName() {
            return newAssetName;
        }

        public void setNewAssetName(String newAssetName) {
            this.newAssetName = newAssetName;
        }

        @Override
        public InputStream openStream() {
            try {
                return file.getInputStream();
            } catch (FileNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
            return null;
        }
    }
}
