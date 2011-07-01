/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.project.actions;

import com.jme3.gde.assetpack.project.AssetPackProject;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@SuppressWarnings("unchecked")
public final class CleanupProjectAction implements Action {

    private final AssetPackProject context;

    public CleanupProjectAction(AssetPackProject context) {
        this.context = context;
    }

    private void scanFiles(final FileObject folder, final ArrayList<String> paths) {
        FileObject[] children = folder.getChildren();
        for (FileObject fileObject : children) {
            boolean stay = false;
            if (fileObject.isFolder() && !fileObject.isVirtual()) {
                scanFiles(fileObject, paths);
                stay = true;
            } else if (fileObject.isData()) {
                if ("png".equalsIgnoreCase(fileObject.getExt())
                        || "jpg".equalsIgnoreCase(fileObject.getExt())
                        || "dds".equalsIgnoreCase(fileObject.getExt())
                        || "dae".equalsIgnoreCase(fileObject.getExt())
                        || "bmp".equalsIgnoreCase(fileObject.getExt())) {
                    for (String path : paths) {
                        if (fileObject.getPath().endsWith(path)) {
                            stay = true;
                        }
                    }
                    if (!stay) {
                        try {
                            Logger.getLogger(CleanupProjectAction.class.getName()).log(Level.INFO, "Delete unused file {0}", fileObject);
                            fileObject.delete();
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
            }
        }
    }

    private ArrayList<String> scanProjectFiles(Element elem, ArrayList<String> elements) {
        if (elem.getTagName().equals("file")) {
//            Logger.getLogger(CleanupProjectAction.class.getName()).log(Level.INFO, "Found element {0}", elem.getAttribute("path"));
            elements.add(elem.getAttribute("path"));
        }
        NodeList list = elem.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i) instanceof Element) {
                Element asset = (Element) list.item(i);
                scanProjectFiles(asset, elements);
            }
        }
        return elements;
    }

    public void actionPerformed(ActionEvent ev) {
        final ArrayList<String> files = scanProjectFiles(context.getConfiguration().getDocumentElement(), new ArrayList<String>());
        new Thread(new Runnable() {

            public void run() {
                ProgressHandle handle = ProgressHandleFactory.createHandle("Cleanup unused assets..");
                handle.start();
                scanFiles(context.getAssetsFolder(), files);
                handle.finish();
            }
        }).start();

    }

    public Object getValue(String key) {
        if (key.equals(NAME)) {
            return "Clean unused images";
        }
        return null;
    }

    public void putValue(String key, Object value) {
    }

    public void setEnabled(boolean b) {
    }

    public boolean isEnabled() {
        return true;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }
}
