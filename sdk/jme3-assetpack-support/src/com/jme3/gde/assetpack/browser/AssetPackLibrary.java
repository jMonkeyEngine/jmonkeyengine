/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.browser;

import com.jme3.gde.assetpack.browser.nodes.AssetPackBrowserFolder;
import com.jme3.gde.assetpack.Installer;
import com.jme3.gde.assetpack.XmlHelper;
import com.jme3.gde.core.assets.ProjectAssetManager;
import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 *
 * @author normenhansen
 */
public class AssetPackLibrary implements Project {

    FileObject library;
    Element[] assetElements = new Element[0];
    InstanceContent content = new InstanceContent();
    Lookup lookup = new AbstractLookup(content);
    private ProjectAssetManager projectAssetManager;
    private List<String> categories = new LinkedList<String>();

    public AssetPackLibrary() {
        content.add(this);
    }

    private void initLibrary() {
        String path = NbPreferences.forModule(Installer.class).get("assetpack_path", null);
        library = FileUtil.toFileObject(new File(path));
    }

    public Node getRootNode() {
        parseLibrary();
        AssetPackBrowserFolder multi = new AssetPackBrowserFolder(assetElements, this);
        return multi;
    }

    public Node getRootNode(String[] categories, String[] tags) {
        parseLibrary();
        AssetPackBrowserFolder multi = new AssetPackBrowserFolder(assetElements, this, categories, tags);
        return multi;
    }

    public Node getRootNode(String category) {
        parseLibrary();
        AssetPackBrowserFolder multi = new AssetPackBrowserFolder(assetElements, this, new String[]{category}, null);
        return multi;
    }

    public Node getRootNode(String[] tags) {
        parseLibrary();
        AssetPackBrowserFolder multi = new AssetPackBrowserFolder(assetElements, this, null, tags);
        return multi;
    }

    private void parseLibrary() {
        initLibrary();
        categories.clear();
        FileObject[] object = library.getChildren();
        List<Element> assetElements = new LinkedList<Element>();
        for (int i = 0; i < object.length; i++) {
            FileObject fileObject = object[i];
            if (fileObject.isFolder()) {
                FileObject config = fileObject.getFileObject("assetpack.xml");
                if (config != null) {

                    InputStream in;
                    try {
                        in = config.getInputStream();
                        Document doc = XMLUtil.parse(new InputSource(in), false, false, null, null);
                        in.close();
                        Element assets = XmlHelper.findChildElement(doc.getDocumentElement(), "assets");
                        if (assets != null) {
                            assetElements.add(assets);
                        }
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    if (projectAssetManager == null) {
                        projectAssetManager = new ProjectAssetManager(this, fileObject.getNameExt() + "/assets/");
                        content.add(projectAssetManager);
                    } else {
                        projectAssetManager.addFolderLocator(fileObject.getNameExt() + "/assets/");
                    }
                } else {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error in assetpack, could not load assetpack.xml!");
                }
            }
        }
        this.assetElements = new Element[assetElements.size()];
        for (int i = 0; i < assetElements.size(); i++) {
            Element element = assetElements.get(i);
            this.assetElements[i] = element;
            Element child = XmlHelper.findChildElement(element, "asset");
            while (child != null) {
                String cats = child.getAttribute("categories");
                String[] categs = cats.split(",");
                for (int j = 0; j < categs.length; j++) {
                    String string = categs[j];
                    string = string.trim();
                    if (!categories.contains(string)) {
                        categories.add(string);
                    }
                }
                child = XmlHelper.findNextElement(child, "asset");
            }
        }
    }

    public FileObject getProjectDirectory() {
        return library;
    }

    public Lookup getLookup() {
        return lookup;
    }

    /**
     * @return the projectAssetManager
     */
    public ProjectAssetManager getProjectAssetManager() {
        return projectAssetManager;
    }

    /**
     * @return the categories
     */
    public List<String> getCategories() {
        return categories;
    }
}
