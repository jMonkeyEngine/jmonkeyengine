/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.project.wizards;

import org.openide.filesystems.FileObject;

/**
 *
 * @author normenhansen
 */
public class FileDescription {

    private FileObject file;
    private String type = "scene";
    private String path = "";
    private boolean mainFile = false;
    private boolean existing = false;
    private String material = "default";
    private String[] extraPropsNames = new String[0];
    private String[] extraPropsValues = new String[0];

    /**
     * @return the file
     */
    public FileObject getFile() {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(FileObject file) {
        this.file = file;
    }

    public String getName() {
        if (file == null) {
            return "not available";
        }
        return file.getNameExt();
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the extraPropsNames
     */
    public String[] getExtraPropsNames() {
        return extraPropsNames;
    }

    /**
     * @param extraPropsNames the extraPropsNames to set
     */
    public void setExtraPropsNames(String[] extraPropsNames) {
        this.extraPropsNames = extraPropsNames;
    }

    /**
     * @return the extraPropsValues
     */
    public String[] getExtraPropsValues() {
        return extraPropsValues;
    }

    /**
     * @param extraPropsValues the extraPropsValues to set
     */
    public void setExtraPropsValues(String[] extraPropsValues) {
        this.extraPropsValues = extraPropsValues;
    }

    /**
     * @return the mainFile
     */
    public boolean isMainFile() {
        return mainFile;
    }

    /**
     * @param mainFile the mainFile to set
     */
    public void setMainFile(boolean mainFile) {
        this.mainFile = mainFile;
    }

    /**
     * @return the existing
     */
    public boolean isExisting() {
        return existing;
    }

    /**
     * @param existing the existing to set
     */
    public void setExisting(boolean existing) {
        this.existing = existing;
    }

    /**
     * @return the material name
     */
    public String getMaterial() {
        return material;
    }

    /**
     * @param target the material name to set
     */
    public void setMaterial(String material) {
        this.material = material;
    }
}
