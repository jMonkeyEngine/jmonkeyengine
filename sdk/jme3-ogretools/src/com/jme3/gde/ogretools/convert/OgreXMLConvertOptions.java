/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.ogretools.convert;

import java.io.File;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author normenhansen
 */
public class OgreXMLConvertOptions {

    private String sourceFile = "";
    private String destFile = "";
    private int lodLevels = 0;
    private int lodValue = 250000;
    private int lodPercent = 20;
    private String lodStrategy = "Distance";
    private boolean generateTangents = true;
    private boolean generateEdgeLists = false;
    private boolean binaryFile = false;

    public OgreXMLConvertOptions() {
    }

    public OgreXMLConvertOptions(String sourceFile) {
        this.sourceFile = sourceFile;
        this.destFile = sourceFile;
    }

    public OgreXMLConvertOptions(String sourceFile, String destFile) {
        this.sourceFile = sourceFile;
        this.destFile = destFile;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getBinaryFileName() {
        if (binaryFile) {
            return sourceFile;
        } else {
            FileObject fobj = FileUtil.toFileObject(new File(sourceFile));
            return fobj.getParent().getPath() + "/" + fobj.getName();
        }
    }

    public String getDestFile() {
        if(binaryFile){
            return sourceFile+".xml";
        }
        return destFile;
    }

    public void setDestFile(String destFile) {
        this.destFile = destFile;
    }

    public int getLodLevels() {
        return lodLevels;
    }

    public void setLodLevels(int lodLevels) {
        this.lodLevels = lodLevels;
    }

    public int getLodValue() {
        return lodValue;
    }

    public void setLodValue(int lodValue) {
        this.lodValue = lodValue;
    }

    public int getLodPercent() {
        return lodPercent;
    }

    public void setLodPercent(int lodPercent) {
        this.lodPercent = lodPercent;
    }

    public String getLodStrategy() {
        return lodStrategy;
    }

    public void setLodStrategy(String lodStrategy) {
        this.lodStrategy = lodStrategy;
    }

    public boolean isGenerateTangents() {
        return generateTangents;
    }

    public void setGenerateTangents(boolean generateTangents) {
        this.generateTangents = generateTangents;
    }

    public boolean isGenerateEdgeLists() {
        return generateEdgeLists;
    }

    public void setGenerateEdgeLists(boolean generateEdgeLists) {
        this.generateEdgeLists = generateEdgeLists;
    }

    public boolean isBinaryFile() {
        return binaryFile;
    }

    public void setBinaryFile(boolean binaryFile) {
        this.binaryFile = binaryFile;
    }
}
