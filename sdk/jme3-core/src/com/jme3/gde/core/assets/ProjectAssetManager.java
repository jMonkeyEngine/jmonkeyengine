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

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.XMLFileSystem;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author normenhansen
 */
@SuppressWarnings("unchecked")
public class ProjectAssetManager extends DesktopAssetManager {

    private Project project;
    private List<String> folderNames = new LinkedList<String>();

    public ProjectAssetManager(Project prj, String folderName) {
        super(true);
        this.project = prj;
        for (AssetManagerConfigurator di : Lookup.getDefault().lookupAll(AssetManagerConfigurator.class)) {
            di.prepareManager(this);
        }
        addFolderLocator(folderName);
    }

    public ProjectAssetManager(FileObject path) {
        super(true);
        if (path == null) {
            this.project = new DummyProject(this);
        } else {
            this.project = new DummyProject(this, path);
        }
        String string = project.getProjectDirectory().getPath();
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Add locator:{0}", string);
        registerLocator(string,
                "com.jme3.asset.plugins.FileLocator");
        for (AssetManagerConfigurator di : Lookup.getDefault().lookupAll(AssetManagerConfigurator.class)) {
            di.prepareManager(this);
        }
    }

    public ProjectAssetManager() {
        this(null);
    }

    /**
     * Adds a locator to a folder within the main project directory
     */
    public void addFolderLocator(String relativePath) {
        String string = project.getProjectDirectory().getPath() + "/" + relativePath + "/";
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Add locator:{0}", string);
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
            System.out.println("absolute/prefix:"+absolutePath+" / "+prefix);
            return absolutePath.substring(prefix.length() + 1);
        }
        return absolutePath;
    }

    @Deprecated
    public AssetManager getManager() {
        return this;
    }

    public String[] getMaterials() {
        FileObject assetsFolder = getAssetFolder();
        if (assetsFolder == null) {
            return new String[]{};
        }
        Enumeration<FileObject> assets = (Enumeration<FileObject>) assetsFolder.getChildren(true);
        ArrayList<String> list = new ArrayList<String>();
        while (assets.hasMoreElements()) {
            FileObject asset = assets.nextElement();
            if (asset.getExt().equalsIgnoreCase("j3m")) {
                list.add(getRelativeAssetPath(asset.getPath()));
            }
        }
        return list.toArray(new String[list.size()]);
    }

    public String[] getSounds() {
        FileObject assetsFolder = getAssetFolder();
        if (assetsFolder == null) {
            return new String[]{};
        }
        Enumeration<FileObject> assets = (Enumeration<FileObject>) assetsFolder.getChildren(true);
        ArrayList<String> list = new ArrayList<String>();
        while (assets.hasMoreElements()) {
            FileObject asset = assets.nextElement();
            if (asset.getExt().equalsIgnoreCase("wav") || asset.getExt().equalsIgnoreCase("ogg")) {
                list.add(getRelativeAssetPath(asset.getPath()));
            }
        }
        return list.toArray(new String[list.size()]);
    }

    public String[] getTextures() {
        FileObject assetsFolder = getAssetFolder();
        if (assetsFolder == null) {
            return new String[]{};
        }
        Enumeration<FileObject> assets = (Enumeration<FileObject>) assetsFolder.getChildren(true);
        ArrayList<String> list = new ArrayList<String>();
        while (assets.hasMoreElements()) {
            FileObject asset = assets.nextElement();
            if (asset.getExt().equalsIgnoreCase("jpg") || asset.getExt().equalsIgnoreCase("jpeg") || asset.getExt().equalsIgnoreCase("gif") || asset.getExt().equalsIgnoreCase("png") || asset.getExt().equalsIgnoreCase("dds") || asset.getExt().equalsIgnoreCase("pfm") || asset.getExt().equalsIgnoreCase("hdr") || asset.getExt().equalsIgnoreCase("tga")) {
                list.add(getRelativeAssetPath(asset.getPath()));
            }
        }
        return list.toArray(new String[list.size()]);
    }

    public String[] getMatDefs() {
        FileObject assetsFolder = getAssetFolder();
        if (assetsFolder == null) {
            return new String[]{};
        }
        Enumeration<FileObject> assets = (Enumeration<FileObject>) assetsFolder.getChildren(true);
        ArrayList<String> list = new ArrayList<String>();
        while (assets.hasMoreElements()) {
            FileObject asset = assets.nextElement();
            if (asset.getExt().equalsIgnoreCase("j3md")) {
                list.add(getRelativeAssetPath(asset.getPath()));
            }
        }
        return list.toArray(new String[list.size()]);
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

    /**
     * For situations with no Project
     */
    private class DummyProject implements Project {

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
}
