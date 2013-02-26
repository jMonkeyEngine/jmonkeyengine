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
package com.jme3.gde.core.assets;

import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.XMLFileSystem;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author normenhansen
 */
@SuppressWarnings("unchecked")
public class ProjectAssetManager extends DesktopAssetManager {

    private static final Logger logger = Logger.getLogger(ProjectAssetManager.class.getName());
    private final Mutex mutex = new Mutex();
    private final Project project;
    private final List<ClassPathChangeListener> classPathListeners = Collections.synchronizedList(new LinkedList<ClassPathChangeListener>());
    private final List<ClassPath> classPaths = Collections.synchronizedList(new LinkedList<ClassPath>());
    private final List<ClassPathItem> classPathItems = Collections.synchronizedList(new LinkedList<ClassPathItem>());
    private final List<AssetEventListener> assetEventListeners = Collections.synchronizedList(new LinkedList<AssetEventListener>());
    private final List<String> folderNames = new LinkedList<String>();
    private final List<FileObject> jarItems = new LinkedList<FileObject>();
    private URLClassLoader loader;

    public ProjectAssetManager(Project prj, String folderName) {
        super(true);
        this.project = prj;
        for (AssetManagerConfigurator di : Lookup.getDefault().lookupAll(AssetManagerConfigurator.class)) {
            di.prepareManager(this);
        }
        addFolderLocator(folderName);
        updateClassLoader();
        prepAssetEventListeners();
    }

    /**
     * Creates
     * <code>ProjectAssetManager</code> for dummy projects.
     *
     * @param path Path on disk to find assets from and the filename is loaded
     * from the path argument directly.
     */
    public ProjectAssetManager(FileObject path) {
        super(true);
        if (path == null) {
            this.project = new DummyProject(this);
        } else {
            this.project = new DummyProject(this, path);
        }
        String projectRootPath = project.getProjectDirectory().getPath();
        logger.log(Level.FINE, "Add locator: {0}", projectRootPath);
        registerLocator(projectRootPath, com.jme3.gde.core.assets.RootLockingFileLocator.class);
        for (AssetManagerConfigurator di : Lookup.getDefault().lookupAll(AssetManagerConfigurator.class)) {
            di.prepareManager(this);
        }
        prepAssetEventListeners();
    }

    public ProjectAssetManager() {
        this(null);
    }

    private void clearClassLoader() {
        if (jarItems.isEmpty() && classPathItems.isEmpty()) {
            return;
        }
        logger.log(Level.FINE, "Clear {0} classpath entries and {1} url locators for project {2}", new Object[]{classPathItems.size(), jarItems.size(), project.toString()});
        for (FileObject fileObject : jarItems) {
            logger.log(Level.FINE, "Remove locator:{0}", fileObject.toURL());
            unregisterLocator(fileObject.toURL().toExternalForm(),
                    com.jme3.asset.plugins.UrlLocator.class);
        }
        jarItems.clear();
        for (ClassPathItem fileObject : classPathItems) {
            logger.log(Level.FINE, "Remove classpath:{0}", fileObject.object);
            fileObject.object.removeRecursiveListener(fileObject.listener);
        }
        classPathItems.clear();
        for (ClassPath classPath : classPaths) {
            classPath.removePropertyChangeListener(classPathListener);
        }
        classPaths.clear();
    }

    private void loadClassLoader() {
        Sources sources = ProjectUtils.getSources(project);
        if (sources != null) {
            if (loader != null) {
                removeClassLoader(loader);
            }
            SourceGroup[] groups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
            List<URL> urls = new LinkedList<URL>();
            for (SourceGroup sourceGroup : groups) {
                ClassPath path = ClassPath.getClassPath(sourceGroup.getRootFolder(), ClassPath.EXECUTE);
                if (path != null) {
                    classPaths.add(path);
                    path.addPropertyChangeListener(classPathListener);
                    FileObject[] roots = path.getRoots();
                    for (FileObject fileObject : roots) {
                        if (!fileObject.equals(getAssetFolder())) {
                            fileObject.addRecursiveListener(listener);
                            logger.log(Level.FINE, "Add classpath:{0}", fileObject);
                            classPathItems.add(new ClassPathItem(fileObject, listener));
                            urls.add(fileObject.toURL());
                        }
                        if (fileObject.toURL().toExternalForm().startsWith("jar")) {
                            logger.log(Level.FINE, "Add locator:{0}", fileObject.toURL());
                            jarItems.add(fileObject);
                            registerLocator(fileObject.toURL().toExternalForm(),
                                    "com.jme3.asset.plugins.UrlLocator");
                        }
                    }
                }
            }
            loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
            addClassLoader(loader);
            logger.log(Level.FINE, "Updated {0} classpath entries and {1} url locators for project {2}", new Object[]{classPathItems.size(), jarItems.size(), project.toString()});
        }
    }
    FileChangeListener listener = new FileChangeListener() {
        public void fileFolderCreated(FileEvent fe) {
            fireChange(fe);
        }

        public void fileDataCreated(FileEvent fe) {
            fireChange(fe);
        }

        public void fileChanged(FileEvent fe) {
            fireChange(fe);
        }

        public void fileDeleted(FileEvent fe) {
        }

        public void fileRenamed(FileRenameEvent fe) {
            fireChange(fe);
        }

        public void fileAttributeChanged(FileAttributeEvent fae) {
        }

        private void fireChange(FileEvent fe) {
            logger.log(Level.FINE, "Classpath item changed: {0}", fe);
            updateClassLoader();
        }
    };
    private PropertyChangeListener classPathListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            logger.log(Level.FINE, "Classpath event: {0}", evt);
            if (ClassPath.PROP_ROOTS.equals(evt.getPropertyName())) {
                updateClassLoader();
            } else if (ClassPath.PROP_ENTRIES.equals(evt.getPropertyName())) {
                updateClassLoader();
            } else if (ClassPath.PROP_INCLUDES.equals(evt.getPropertyName())) {
                updateClassLoader();
            }
        }
    };

    private void updateClassLoader() {
        ProjectManager.mutex().postWriteRequest(new Runnable() {
            public void run() {
                synchronized (classPathItems) {
                    clearClassLoader();
                    loadClassLoader();
                }
            }
        });
        notifyClassPathListeners();
    }

    @Override
    public void setAssetEventListener(AssetEventListener listener) {
        throw new UnsupportedOperationException("Setting the asset event listener is not allowed for ProjectAssetManager, use addAssetEventListener instead");
    }

    private void prepAssetEventListeners() {
        super.setAssetEventListener(new AssetEventListener() {
            public void assetLoaded(AssetKey ak) {
                synchronized (assetEventListeners) {
                    for (AssetEventListener assetEventListener : assetEventListeners) {
                        assetEventListener.assetLoaded(ak);
                    }
                }
            }

            public void assetRequested(AssetKey ak) {
                synchronized (assetEventListeners) {
                    for (AssetEventListener assetEventListener : assetEventListeners) {
                        assetEventListener.assetRequested(ak);
                    }
                }
            }

            public void assetDependencyNotFound(AssetKey ak, AssetKey ak1) {
                synchronized (assetEventListeners) {
                    for (AssetEventListener assetEventListener : assetEventListeners) {
                        assetEventListener.assetDependencyNotFound(ak, ak1);
                    }
                }
            }
        });
    }

    /**
     * Returns the
     * <code>FileObject</code> for a given asset path, or null if no such asset
     * exists. First looks in the asset folder(s) for the file, then proceeds to
     * scan the classpath folders and jar files for it.The returned FileObject
     * might be inside a jar file and not writeable!
     *
     * @param assetKey The asset key to get the file object for
     * @return Either a FileObject for the asset or null if not found.
     */
    public FileObject getAssetFileObject(AssetKey assetKey) {
        String name = assetKey.getName();
        return getAssetFileObject(name);
    }

    /**
     * Returns the
     * <code>FileObject</code> for a given asset path, or null if no such asset
     * exists. First looks in the asset folder(s) for the file, then proceeds to
     * scan the classpath folders and jar files for it.The returned FileObject
     * might be inside a jar file and not writeable!
     *
     * @param name The asset name to get the file object for
     * @return Either a FileObject for the asset or null if not found.
     */
    public FileObject getAssetFileObject(String name) {
        assert (name != null);
        FileObject file = getAssetFolder().getFileObject(name);
        if (file != null) {
            return file;
        }
        synchronized (classPathItems) {
            // TODO I need to find out if classPathItems contains all jars added to a project
            Iterator<ClassPathItem> classPathItemsIter = classPathItems.iterator();
            while (classPathItemsIter.hasNext()) {
                ClassPathItem classPathItem = classPathItemsIter.next();
                FileObject jarFile = classPathItem.object;

                Enumeration<FileObject> jarEntry = (Enumeration<FileObject>) jarFile.getChildren(true);
                while (jarEntry.hasMoreElements()) {
                    FileObject jarEntryAsset = jarEntry.nextElement();
                    String path = jarEntryAsset.getPath();
                    if (!path.startsWith("/") && path.equals(name)) {
                        return jarEntryAsset;
                    }
                }
            }
        }
        return null;
    }

    public FileObject createAsset(String path) {
        return createAsset(path, null);
    }

    public FileObject createAsset(String path, FileObject source) {
        FileObject assetFolder = getAssetFolder();
        FileObject dest = getAssetFolder().getFileObject(path);
        if (dest == null) {
            try {
                dest = FileUtil.createData(assetFolder, path);
                if (source != null) {
                    FileObject parent = dest.getParent();
                    dest.delete();
                    return source.copy(parent, source.getName(), source.getExt());
                } else {
                    return dest;
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        } else {
            NotifyDescriptor.Confirmation msg = new NotifyDescriptor.Confirmation(
                    "File " + source.getNameExt() + " exists, overwrite?",
                    NotifyDescriptor.YES_NO_OPTION,
                    NotifyDescriptor.WARNING_MESSAGE);
            Object result = DialogDisplayer.getDefault().notify(msg);
            if (NotifyDescriptor.YES_OPTION.equals(result)) {
                try {
                    if (source != null) {
                        FileObject parent = dest.getParent();
                        dest.delete();
                        return source.copy(parent, source.getName(), source.getExt());
                    } else {
                        dest.delete();
                        return FileUtil.createData(assetFolder, path);
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        return null;
    }

    /**
     * Adds a locator to a folder within the main project directory
     */
    public void addFolderLocator(String relativePath) {
        String string = project.getProjectDirectory().getPath() + "/" + relativePath + "/";
        logger.log(Level.FINE, "Add locator:{0}", string);
        registerLocator(string,
                "com.jme3.asset.plugins.FileLocator");
        folderNames.add(relativePath);
    }

    public Project getProject() {
        return project;
    }

    public String getRelativeAssetPath(String absolutePath) {
        String prefix = getAssetFolderName();
        int idx = absolutePath.indexOf(prefix);
        if (idx == 0) {
            return stripFirstSlash(absolutePath.substring(prefix.length()));
        }
        return absolutePath;
    }

    private String stripFirstSlash(String input) {
        if (input.startsWith("/")) {
            return input.substring(1);
        }
        return input;
    }

    @Deprecated
    public AssetManager getManager() {
        return this;
    }

    public String[] getModels() {
        return filesWithSuffix("j3o");
    }

    public String[] getMaterials() {
        return filesWithSuffix("j3m");
    }

    public String[] getSounds() {
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(collectFilesWithSuffix("wav"));
        list.addAll(collectFilesWithSuffix("ogg"));
        return list.toArray(new String[list.size()]);
    }

    public String[] getTextures() {
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(collectFilesWithSuffix("jpg"));
        list.addAll(collectFilesWithSuffix("jpeg"));
        list.addAll(collectFilesWithSuffix("gif"));
        list.addAll(collectFilesWithSuffix("png"));
        list.addAll(collectFilesWithSuffix("dds"));
        list.addAll(collectFilesWithSuffix("pfm"));
        list.addAll(collectFilesWithSuffix("hdr"));
        list.addAll(collectFilesWithSuffix("tga"));
        return list.toArray(new String[list.size()]);
    }

    public String[] getMatDefs() {
        return filesWithSuffix("j3md");
    }

    public List<String>  getProjectShaderNodeDefs() {       
        return collectProjectFilesWithSuffix("j3sn", new LinkedList<String>());
    }

    public  List<String> getDependenciesShaderNodeDefs() {        
        return collectDependenciesFilesWithSuffix("j3sn", new LinkedList<String>());
    }

    public String[] getAssetsWithSuffix(String string) {
        return filesWithSuffix(string);
    }

    private String[] filesWithSuffix(String string) {
        List<String> list = collectFilesWithSuffix(string);
        return list.toArray(new String[list.size()]);
    }

    private List<String> collectFilesWithSuffix(String suffix) {
        List<String> list = new LinkedList<String>();
        collectProjectFilesWithSuffix(suffix, list);
        collectDependenciesFilesWithSuffix(suffix, list);
        return list;
    }

    private List<String> collectProjectFilesWithSuffix(String suffix, List<String> list) {
        FileObject assetsFolder = getAssetFolder();
        if (assetsFolder != null) {
            Enumeration<FileObject> assets = (Enumeration<FileObject>) assetsFolder.getChildren(true);
            while (assets.hasMoreElements()) {
                FileObject asset = assets.nextElement();
                if (asset.getExt().equalsIgnoreCase(suffix)) {
                    list.add(getRelativeAssetPath(asset.getPath()));
                }
            }
        }
        return list;
    }

    private List<String> collectDependenciesFilesWithSuffix(String suffix, List<String> list) {
        synchronized (classPathItems) {
            // TODO I need to find out if classPathItems contains all jars added to a project
            Iterator<ClassPathItem> classPathItemsIter = classPathItems.iterator();
            while (classPathItemsIter.hasNext()) {
                ClassPathItem classPathItem = classPathItemsIter.next();
                FileObject jarFile = classPathItem.object;

                Enumeration<FileObject> jarEntry = (Enumeration<FileObject>) jarFile.getChildren(true);
                while (jarEntry.hasMoreElements()) {
                    FileObject jarEntryAsset = jarEntry.nextElement();
                    if (jarEntryAsset.getExt().equalsIgnoreCase(suffix)) {
                        if (!jarEntryAsset.getPath().startsWith("/")) {
                            list.add(jarEntryAsset.getPath());
                        }
                    }
                }
            }
            return list;
        }
    }

    public InputStream getResourceAsStream(String name) {
        InputStream in = null;//JmeSystem.getResourceAsStream(name);
        synchronized (classPathItems) {
            // TODO I need to find out if classPathItems contains all jars added to a project
            Iterator<ClassPathItem> classPathItemsIter = classPathItems.iterator();
            while (classPathItemsIter.hasNext()) {
                ClassPathItem classPathItem = classPathItemsIter.next();
                FileObject jarFile = classPathItem.object;

                Enumeration<FileObject> jarEntry = (Enumeration<FileObject>) jarFile.getChildren(true);
                while (jarEntry.hasMoreElements()) {
                    FileObject jarEntryAsset = jarEntry.nextElement();
                    if (jarEntryAsset.getPath().equalsIgnoreCase(name)) {
                        try {
                            in = jarEntryAsset.getInputStream();
                        } catch (FileNotFoundException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                        break;
                    }
                }
            }
        }
        return in;
    }

    /**
     * @return the folderName
     */
    private String getFolderName() {
        if (folderNames.isEmpty()) {
            return "";
        } else {
            return folderNames.get(0);
        }
    }

    /**
     * @return the folderName
     */
    public String getAssetFolderName() {
        if (folderNames.isEmpty()) {
            return project.getProjectDirectory().getPath();
        } else {
            return project.getProjectDirectory().getFileObject(getFolderName()).getPath();
        }
    }

    public FileObject getAssetFolder() {
        if (folderNames.isEmpty()) {
            return project.getProjectDirectory();
        } else {
            return project.getProjectDirectory().getFileObject(getFolderName());
        }
    }

    public String getAbsoluteAssetPath(String path) {
        if (folderNames.isEmpty()) {
        } else {
            for (Iterator<String> it = folderNames.iterator(); it.hasNext();) {
                FileObject string = project.getProjectDirectory().getFileObject(it.next() + "/" + path);
                if (string != null) {
                    return string.getPath();
                }
            }
        }
        return null;
    }

    /**
     * @param folderName the folderName to set
     */
    public void setFolderName(String folderName) {
        if (folderNames.size() > 0) {
            this.folderNames.remove(0);
        }
        this.folderNames.add(0, folderName);
    }

    @Override
    public void addAssetEventListener(AssetEventListener listener) {
        synchronized (assetEventListeners) {
            assetEventListeners.add(listener);
        }
    }

    @Override
    public void removeAssetEventListener(AssetEventListener listener) {
        synchronized (assetEventListeners) {
            assetEventListeners.remove(listener);
        }
    }

    public void addClassPathEventListener(ClassPathChangeListener listener) {
        synchronized (classPathListeners) {
            classPathListeners.add(listener);
        }
    }

    public void removeClassPathEventListener(ClassPathChangeListener listener) {
        synchronized (classPathListeners) {
            classPathListeners.remove(listener);
        }
    }

    private void notifyClassPathListeners() {
        final ProjectAssetManager pm = this;
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                synchronized (classPathListeners) {
                    for (ClassPathChangeListener classPathChangeListener : classPathListeners) {
                        classPathChangeListener.classPathChanged(pm);
                    }
                }
            }
        });
    }

    public Mutex mutex() {
        return mutex;
    }

    /**
     * For situations with no Project
     */
    private static class DummyProject implements Project {

        ProjectAssetManager pm;
        FileObject folder;
        XMLFileSystem fileSystem = new XMLFileSystem();

        public DummyProject(ProjectAssetManager pm, FileObject folder) {
            this.folder = folder;
            this.pm = pm;
        }

        public DummyProject(ProjectAssetManager pm) {
            this.pm = pm;
        }

        public Lookup getLookup() {
            return Lookups.fixed(this, pm);
        }

        public FileObject getProjectDirectory() {
            if (folder != null) {
                return folder;
            }
            return fileSystem.getRoot();
        }
    }

    private static class ClassPathItem {

        ClassPath path;
        FileObject object;
        FileChangeListener listener;

        public ClassPathItem() {
        }

        public ClassPathItem(FileObject object, FileChangeListener listener) {
            this.object = object;
            this.listener = listener;
        }
    }

    public static interface ClassPathChangeListener {

        public void classPathChanged(ProjectAssetManager manager);
    }
}
