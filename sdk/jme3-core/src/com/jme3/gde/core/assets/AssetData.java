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
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileAlreadyLockedException;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Mutex;
import org.openide.util.Mutex.Action;

/**
 * Global object to access actual jME3 data within an AssetDataObject, available
 * through the Lookup of any AssetDataObject. AssetDataObjects that wish to use
 *
 * @author normenhansen
 */
@SuppressWarnings("unchecked")
public class AssetData {

    private static final Logger logger = Logger.getLogger(AssetData.class.getName());
    private final List<AssetDataPropertyChangeListener> listeners = new ArrayList<AssetDataPropertyChangeListener>();
    private final Mutex propsMutex = new Mutex();
    private final Properties props = new Properties();
    private AssetDataObject file;
    private String extension = "jmpdata";
    private Date lastLoaded;

    public AssetData(AssetDataObject file) {
        this.file = file;
        FileObject primaryFile = file.getPrimaryFile();
        if (primaryFile != null) {
            extension = primaryFile.getExt() + "data";
        }
    }

    public AssetData(AssetDataObject file, String extension) {
        this.file = file;
        this.extension = extension;
    }

    /**
     * Sets the extension of the assetData properties file, normally not
     * necessary to use this, it will be .[originalsuffix]data, for example
     * .j3odata.
     *
     * @param extension
     */
//    public void setExtension(String extension) {
//        this.extension = extension;
//    }

    public AssetKey<?> getAssetKey() {
        return file.getAssetKey();
    }

    /**
     * Applies the supplied keys data to the assets assetKey so it will be
     * loaded with these settings next time loadAsset is actually loading the
     * asset from the ProjectAssetManager.
     *
     * @param key
     */
    public void setAssetKey(AssetKey key) {
        file.setAssetKeyData(key);
    }

    public void setModified(boolean modified) {
        file.setModified(modified);
    }

    public void setSaveCookie(SaveCookie cookie) {
        file.setSaveCookie(cookie);
    }

    /**
     * Loads the asset from the DataObject via the ProjectAssetManager in the
     * lookup. Returns the currently loaded asset when it has been loaded
     * already, close the asset using closeAsset().
     *
     * @return
     */
    public Savable loadAsset() {
        return file.loadAsset();
    }

    /**
     * Saves this asset, when a saveExtension is set, saves it as a brother file
     * with that extension.
     *
     * @throws IOException
     */
    public void saveAsset() throws IOException {
        file.saveAsset();
    }

    public void closeAsset() {
        file.closeAsset();
    }

    public List<FileObject> getAssetList() {
        return file.getAssetList();
    }

    public List<AssetKey> getAssetKeyList() {
        return file.getAssetKeyList();
    }

    public List<AssetKey> getFailedList() {
        return file.getFailedList();
    }

    public synchronized String getProperty(final String key) {
        readProperties();
        return propsMutex.readAccess(new Action<String>() {
            public String run() {
                return props.getProperty(key);
            }
        });
    }

    public synchronized String getProperty(final String key, final String defaultValue) {
        readProperties();
        return propsMutex.readAccess(new Action<String>() {
            public String run() {
                return props.getProperty(key, defaultValue);
            }
        });
    }

    public synchronized String setProperty(final String key, final String value) {
        readProperties();
        String ret = propsMutex.writeAccess(new Action<String>() {
            public String run() {
                String ret = (String) props.setProperty(key, value);
                return ret;
            }
        });
        writeProperties();
        notifyListeners(key, ret, value);
        return ret;
    }

    @Deprecated
    public void loadProperties() {
    }

    @Deprecated
    public void saveProperties() throws FileAlreadyLockedException, IOException {
    }

    private void readProperties() {
        propsMutex.writeAccess(new Runnable() {
            public void run() {
                final FileObject myFile = FileUtil.findBrother(file.getPrimaryFile(), extension);
                if (myFile == null) {
                    return;
                }
                final Date lastMod = myFile.lastModified();
                if (!lastMod.equals(lastLoaded)) {
                    props.clear();
                    lastLoaded = lastMod;
                    InputStream in = null;
                    try {
                        in = new BufferedInputStream(myFile.getInputStream());
                        try {
                            props.load(in);
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    } catch (FileNotFoundException ex) {
                        Exceptions.printStackTrace(ex);
                    } finally {
                        try {
                            in.close();
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    logger.log(Level.FINE, "Read AssetData properties for {0}", file);
                }
            }
        });
    }

    private void writeProperties() {
        //writeAccess because we write lastMod date, not because we write to the file
        //the mutex protects the properties object, not the file
        propsMutex.writeAccess(new Runnable() {
            public void run() {
                OutputStream out = null;
                FileLock lock = null;
                try {
                    FileObject pFile = file.getPrimaryFile();
                    FileObject myFile = FileUtil.findBrother(pFile, extension);
                    if (myFile == null) {
                        myFile = FileUtil.createData(pFile.getParent(), pFile.getName() + "." + extension);
                    }
                    lock = myFile.lock();
                    out = new BufferedOutputStream(myFile.getOutputStream(lock));
                    props.store(out, "");
                    out.flush();
                    lastLoaded = myFile.lastModified();
                    logger.log(Level.FINE, "Written AssetData properties for {0}", file);
                } catch (IOException e) {
                    Exceptions.printStackTrace(e);
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    if (lock != null) {
                        lock.releaseLock();
                    }
                }
            }
        });
    }

    protected void notifyListeners(String property, String before, String after) {
        synchronized (listeners) {
            for (Iterator<AssetDataPropertyChangeListener> it = listeners.iterator(); it.hasNext();) {
                AssetDataPropertyChangeListener assetDataPropertyChangeListener = it.next();
                assetDataPropertyChangeListener.assetDataPropertyChanged(property, before, after);
            }
        }
    }

    public void addPropertyChangeListener(AssetDataPropertyChangeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removePropertyChangeListener(AssetDataPropertyChangeListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
}
