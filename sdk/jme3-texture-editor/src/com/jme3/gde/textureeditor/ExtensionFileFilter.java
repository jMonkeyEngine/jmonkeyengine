package com.jme3.gde.textureeditor;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public class ExtensionFileFilter extends FileFilter {

    public static ExtensionFileFilter create(String ext, String desc) {
        return new ExtensionFileFilter(ext, desc);
    }
    private final String EXTENSION;
    private final String DESCRIPTION;

    protected ExtensionFileFilter(String ext, String desc) {
        EXTENSION = ext;
        DESCRIPTION = desc;
    }

    @Override
    public boolean accept(File f) {
        return f.isDirectory() || f.getName().toLowerCase().endsWith(EXTENSION);
    }

    public String getExtension() {
        return EXTENSION;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
