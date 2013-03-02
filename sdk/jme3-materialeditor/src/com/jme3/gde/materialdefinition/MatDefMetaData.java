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
package com.jme3.gde.materialdefinition;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
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
public class MatDefMetaData {

    private static final Logger logger = Logger.getLogger(MatDefMetaData.class.getName());
    private final List<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();
    private final Mutex propsMutex = new Mutex();
    private final Properties props = new Properties();
    private static final Properties defaultProps = new Properties();

    static {
        defaultProps.put("Default/position", "0,120");
        defaultProps.put("Default/MatParam.Color", "438,351");
        defaultProps.put("Default/Default/ColorMult", "605,372");
        defaultProps.put("Default/WorldParam.WorldViewProjectionMatrix", "33,241");
        defaultProps.put("Default/color", "0,478");
        defaultProps.put("Default/Default/CommonVert", "211,212");
    }
    private MatDefDataObject file;
    private String extension = "jmpdata";
    private Date lastLoaded;
    private FileObject folder;
    private FileObject root;

//    private XMLFileSystem fs;
    public MatDefMetaData(MatDefDataObject file) {
        try {
            this.file = file;
            getFolder();
            FileObject primaryFile = file.getPrimaryFile();

            if (primaryFile != null) {
                extension = primaryFile.getExt() + "data";
            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public MatDefMetaData(MatDefDataObject file, String extension) {
        this.file = file;
        this.extension = extension;
    }

    public synchronized String getProperty(final String key) {
        readProperties();
        return propsMutex.readAccess(new Action<String>() {
            public String run() {
                String prop = props.getProperty(key);
                if (prop == null) {
                    return defaultProps.getProperty(key);
                }
                return prop;
            }
        });
    }

    public synchronized String getProperty(final String key, final String defaultValue) {
        readProperties();
        return propsMutex.readAccess(new Action<String>() {
            public String run() {
                String prop = props.getProperty(key);
                if (prop == null) {
                    return defaultProps.getProperty(key);
                }
                return prop;
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
        // writeProperties();
        notifyListeners(key, ret, value);
        return ret;
    }

    private void readProperties() {
        propsMutex.writeAccess(new Runnable() {
            public void run() {
                try {
                    FileObject pFile = file.getPrimaryFile();
                    FileObject storageFolder = getFolder();
                    if (storageFolder == null) {
                        return;
                    }
                    final FileObject myFile = storageFolder.getFileObject(getFileFullName(pFile)); //fs.findResource(fs.getRoot().getPath()+"/"+ file.getPrimaryFile().getName() + "." + extension);//

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
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
    }

    public void cleanup() {
        propsMutex.writeAccess(new Runnable() {
            public void run() {
                OutputStream out = null;
                FileLock lock = null;
                try {
                    FileObject pFile = file.getPrimaryFile();
                    FileObject storageFolder = getFolder();
                    if (storageFolder == null) {
                        return;
                    }

                    FileObject myFile = storageFolder.getFileObject(getFileFullName(pFile));//FileUtil.findBrother(pFile, extension);//fs.findResource(fs.getRoot().getPath()+"/"+ pFile.getName() + "." + extension);//
                    if (myFile == null) {
                        return;
                    }
                    lock = myFile.lock();
                    myFile.delete(lock);
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

    public void rename(final DataFolder df, final String name) {
        propsMutex.writeAccess(new Runnable() {
            public void run() {
                OutputStream out = null;
                FileLock lock = null;
                try {
                    FileObject pFile = file.getPrimaryFile();
                    FileObject storageFolder = getFolder();
                    if (storageFolder == null) {
                        return;
                    }

                    FileObject myFile = storageFolder.getFileObject(getFileFullName(pFile));//FileUtil.findBrother(pFile, extension);//fs.findResource(fs.getRoot().getPath()+"/"+ pFile.getName() + "." + extension);//
                    if (myFile == null) {
                        return;
                    }
                    lock = myFile.lock();
                    if (df == null) {
                        myFile.rename(lock, getFileFullName(pFile).replaceAll(file.getName() + "." + extension, name + "." + extension), "");
                    } else {
                        myFile.rename(lock, FileUtil.getRelativePath(root, df.getPrimaryFile()).replaceAll("/", ".") + "." + pFile.getName(), extension);
                    }
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

    public void duplicate(final DataFolder df, final String name) {
        propsMutex.writeAccess(new Runnable() {
            public void run() {
                OutputStream out = null;
                FileLock lock = null;
                try {
                    FileObject pFile = file.getPrimaryFile();
                    FileObject storageFolder = getFolder();
                    if (storageFolder == null) {
                        return;
                    }
                    String newName = name;
                    if (newName == null) {
                        newName = file.getName();
                    }
                    String path = FileUtil.getRelativePath(root, df.getPrimaryFile()).replaceAll("/", ".") + "." + newName;
                    FileObject myFile = storageFolder.getFileObject(getFileFullName(pFile));
                    if (myFile == null) {
                        return;
                    }

                    myFile.copy(storageFolder, path, extension);

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

    public void save() {
        if (!props.isEmpty()) {
            writeProperties();
        }
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
                    FileObject storageFolder = getFolder();
                    if (storageFolder == null) {
                        return;
                    }

                    FileObject myFile = storageFolder.getFileObject(getFileFullName(pFile));//FileUtil.findBrother(pFile, extension);//fs.findResource(fs.getRoot().getPath()+"/"+ pFile.getName() + "." + extension);//
                    if (myFile == null) {
                        myFile = FileUtil.createData(storageFolder, getFileFullName(pFile));
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

    private String getFileFullName(FileObject pFile) {
        return FileUtil.getRelativePath(root, pFile).replaceAll("/", ".").replaceAll("j3md", extension);
    }

    protected void notifyListeners(String property, String before, String after) {
        synchronized (listeners) {
            for (Iterator<PropertyChangeListener> it = listeners.iterator(); it.hasNext();) {
                PropertyChangeListener propertyChangeListener = it.next();
                propertyChangeListener.propertyChange(new PropertyChangeEvent(this, property, before, after));
            }
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private FileObject getFolder() throws IOException {
        if (folder == null) {
            Project p = file.getLookup().lookup(Project.class);
            if (p != null) {
                root = p.getProjectDirectory();

                FileObject jmedataFolder = root.getFileObject("/nbproject/jme3Data");
                if (jmedataFolder == null) {
                    jmedataFolder = root.getFileObject("/nbproject");
                    jmedataFolder = jmedataFolder.createFolder("jme3Data");
                }
                return jmedataFolder;
            }
        }
        return folder;
    }
}
