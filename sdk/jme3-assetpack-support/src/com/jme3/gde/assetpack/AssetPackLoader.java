/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack;

import com.jme3.asset.ModelKey;
import com.jme3.gde.assetpack.actions.AddAssetAction;
import com.jme3.gde.assetpack.project.wizards.FileDescription;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.material.Material;
import com.jme3.material.MaterialList;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.ogre.OgreMeshKey;
import com.jme3.scene.plugins.ogre.matext.MaterialExtension;
import com.jme3.scene.plugins.ogre.matext.MaterialExtensionSet;
import com.jme3.scene.plugins.ogre.matext.OgreMaterialKey;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author normenhansen
 */
public class AssetPackLoader {

    public static Spatial loadAssetPackModel(ProjectAssetManager pm, AssetConfiguration config) {
        Element assetElement = config.getAssetElement();
        NodeList fileNodeList = assetElement.getElementsByTagName("file");
        Element fileElement = XmlHelper.findChildElementWithAttribute(assetElement, "file", "main", "true");
        if (fileElement == null) {
            fileElement = XmlHelper.findChildElement(assetElement, "file");
        }
        //find main files for this model
        List<Element> files = new ArrayList<Element>();
        while (fileElement != null) {
            files.add(fileElement);
            //TODO:doesnt work?
            fileElement = XmlHelper.findNextElementWithAttribute(fileElement, "file", "main", "true");
        }

        //find material varations for this model
        List<SelectionEntry> selList = new ArrayList<SelectionEntry>();
        Element matset = XmlHelper.findChildElement(assetElement, "materialvariations");
        if (matset != null) {
            Element part = XmlHelper.findChildElement(matset, "mesh");
            String partName = null;
            while (part != null) {
                partName = part.getAttribute("name");
                ArrayList<Element> variations = new ArrayList<Element>();
                selList.add(new SelectionEntry(partName, variations));
                Element variation = XmlHelper.findChildElement(part, "variation");
                while (variation != null) {
                    variations.add(variation);
                    variation = XmlHelper.findNextSiblingElement(variation);
                }
                part = XmlHelper.findNextSiblingElement(part);
            }
        }
        //let user select variation
        boolean selectable = false;
        for (Iterator<SelectionEntry> it = selList.iterator(); it.hasNext();) {
            SelectionEntry selectionEntry = it.next();
            if (selectionEntry.names.size() > 1) {
                selectable = true;
            }
        }
        if (selectable) {
            new VariationSelection(selList).setVisible(true);
        }
        for (Iterator<SelectionEntry> it = selList.iterator(); it.hasNext();) {
            SelectionEntry selectionEntry = it.next();
            config.addVariationAssets(selectionEntry.getSelected());
        }
        Spatial model = null;
        Node node = null;
        for (Element element : files) {
            Logger.getLogger(AssetPackLoader.class.getName()).log(Level.INFO, "Load main file {0}", element.getAttribute("path"));
            if (model != null && node == null) {
                node = new Node(assetElement.getAttribute("name"));
                node.attachChild(model);
            }
            model = AssetPackLoader.loadSingleMesh(element, fileNodeList, config.getVariationAssets(), pm);
            if (model != null && node != null) {
                node.attachChild(model);
            } else {
                Logger.getLogger(AssetPackLoader.class.getName()).log(Level.WARNING, "Error loading model");
            }
        }
        if (node != null) {
            return node;
        }
        return model;
    }

    private static Spatial loadSingleMesh(Element fileElement, NodeList fileNodeList, List<NodeList> variationNodeList, ProjectAssetManager pm) {
        ModelKey key = null;
        Material mat = null;
        Spatial model;
        MaterialList matList = null;
        String name = fileElement.getAttribute("path");
        String materialName = fileElement.getAttribute("material");
        if ("".equals(materialName)) {
            materialName = null;
        }

        //PREPARE MATEXT
        MaterialExtensionSet matExts = new MaterialExtensionSet();
        /**
         * /base/simple
         * /base/normalmap
         */
        MaterialExtension baseLightExt = new MaterialExtension("/base/normalmap/specular",
                "Common/MatDefs/Light/Lighting.j3md");
        baseLightExt.setTextureMapping("DiffuseMap", "DiffuseMap");
        baseLightExt.setTextureMapping("NormalHeightMap", "NormalMap");
        baseLightExt.setTextureMapping("SpecularMap", "SpecularMap");
        matExts.addMaterialExtension(baseLightExt);

        MaterialExtension baseLightExt2 = new MaterialExtension("/base/normalmap",
                "Common/MatDefs/Light/Lighting.j3md");
        baseLightExt2.setTextureMapping("DiffuseMap", "DiffuseMap");
        baseLightExt2.setTextureMapping("NormalHeightMap", "NormalMap");
        baseLightExt2.setTextureMapping("SpecularMap", "SpecularMap");
        matExts.addMaterialExtension(baseLightExt2);

        MaterialExtension baseLightExt3 = new MaterialExtension("/base/simple",
                "Common/MatDefs/Light/Lighting.j3md");
        baseLightExt3.setTextureMapping("DiffuseMap", "DiffuseMap");
        baseLightExt3.setTextureMapping("NormalHeightMap", "NormalMap");
        baseLightExt3.setTextureMapping("SpecularMap", "SpecularMap");
        matExts.addMaterialExtension(baseLightExt3);

        //TODO: mesh.xml!!
        if (hasExtension(name, "xml") || hasExtension(name, "scene")) {
            for (int i = 0; i < fileNodeList.getLength(); i++) {
                Element fileElem = (Element) fileNodeList.item(i);
                String type = fileElem.getAttribute("type");
                String path = fileElem.getAttribute("path");
                if ("material".equals(type) && (materialName == null || materialName.equals(path))) {
                    if (hasExtension(path, "j3m")) {
                        mat = pm.loadMaterial(path);
                    } else if (hasExtension(path, "material")) {
                        if (matList == null) {
                            Logger.getLogger(AssetPackLoader.class.getName()).log(Level.INFO, "Load Ogre Material");
                            OgreMaterialKey matKey = new OgreMaterialKey(path);
                            matKey.setMaterialExtensionSet(matExts);
                            matList = pm.loadAsset(matKey);
                            key = new OgreMeshKey(name, matList);
                        } else {
                            Logger.getLogger(AssetPackLoader.class.getName()).log(Level.INFO, "Add Ogre Material");
                            OgreMaterialKey matKey = new OgreMaterialKey(path);
                            matKey.setMaterialExtensionSet(matExts);
                            MaterialList newMatList = pm.loadAsset(matKey);
                            matList.putAll(newMatList);
                        }
                    }
                }
            }
        } else if (hasExtension(name, "obj")) {
            for (int i = 0; i < fileNodeList.getLength(); i++) {
                Element fileElem = (Element) fileNodeList.item(i);
                String type = fileElem.getAttribute("type");
                String path = fileElem.getAttribute("path");
                if ("material".equals(type) && (materialName == null || materialName.equals(path))) {
                    if (hasExtension(path, "j3m")) {
                        mat = pm.loadMaterial(path);
                    }
                }
            }
        } else if (hasExtension(name, "j3o")) {
            //should have all info inside
        }

        if (variationNodeList != null) {
            for (NodeList nodeList : variationNodeList) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element fileElem = (Element) nodeList.item(i);
                    String type = fileElem.getAttribute("type");
                    String path = fileElem.getAttribute("path");
                    if ("material".equals(type)) {
                        if (hasExtension(path, "j3m")) {
                            mat = pm.loadMaterial(path);
                        } else if (hasExtension(path, "material")) {
                            if (matList == null) {
                                Logger.getLogger(AssetPackLoader.class.getName()).log(Level.INFO, "Load Ogre Material");
                                OgreMaterialKey matKey = new OgreMaterialKey(path);
                                matKey.setMaterialExtensionSet(matExts);
                                matList = pm.loadAsset(matKey);
                                key = new OgreMeshKey(name, matList);
                            } else {
                                Logger.getLogger(AssetPackLoader.class.getName()).log(Level.INFO, "Add Ogre Material");
                                OgreMaterialKey matKey = new OgreMaterialKey(path);
                                matKey.setMaterialExtensionSet(matExts);
                                MaterialList newMatList = pm.loadAsset(matKey);
                                matList.putAll(newMatList);
                            }
                        }
                    }
                }

            }
        }

        if (key != null && mat != null) {
            Logger.getLogger(AddAssetAction.class.getName()).log(Level.WARNING, "j3m and ogre material defined for asset {0}.", name);
        }
        if (key != null) {
            model = pm.loadAsset(key);
        } else {
            model = pm.loadModel(name);
        }
        if (model == null) {
            Logger.getLogger(AddAssetAction.class.getName()).log(Level.SEVERE, "Could not load model {0}!", name);
            return null;
        }
        if (mat != null) {
            model.setMaterial(mat);
        }
        return model;
    }

    private static boolean hasExtension(String name, String extension) {
        int idx = name.lastIndexOf(".");
        if (idx < 0) {
            return false;
        }
        String ext = name.substring(idx + 1, name.length());
        if (ext.equalsIgnoreCase(extension)) {
            return true;
        }
        return false;
    }

    public static FileDescription getFileDescription(String path) {
        return getFileDescription(new File(path.replaceAll("/", File.separator)));
    }

    public static FileDescription getFileDescription(File file) {
        FileObject fileObject;
        try {
            fileObject = FileUtil.toFileObject(file.getCanonicalFile());
            if (fileObject == null) {
                Logger.getLogger(AssetPackLoader.class.getName()).log(Level.WARNING, "Cannot find asset {0}", file.getPath());
                return null;
            }
            return getFileDescription(fileObject);
        } catch (IOException ex) {
            Logger.getLogger(AssetPackLoader.class.getName()).log(Level.WARNING, "Cannot find asset {0}", file.getPath());
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    public static FileDescription getFileDescription(FileObject fileObject) {
        if (fileObject == null) {
            return null;
        }
        FileDescription description = new FileDescription();
        description.setFile(fileObject);
        if ("material".equals(fileObject.getExt())) {
            description.setType("material");
        } else if ("j3m".equals(fileObject.getExt())) {
            description.setType("material");
        } else if ("mat".equals(fileObject.getExt())) {
            description.setType("material");
        } else if ("scene".equals(fileObject.getExt())) {
            description.setType("scene");
            description.setMainFile(true);
        } else if ("obj".equals(fileObject.getExt())) {
            description.setType("mesh");
            description.setMainFile(true);
        } else if ("j3o".equals(fileObject.getExt())) {
            description.setType("scene");
            description.setMainFile(true);
        } else if ("xml".equals(fileObject.getExt())) {
            if (fileObject.getName().endsWith(".mesh")) {
                description.setType("mesh");
            }
            if (fileObject.getName().endsWith(".skeleton")) {
                description.setType("skeleton");
            }
        } else if ("png".equals(fileObject.getExt())) {
            description.setType("texture");
        } else if ("jpg".equals(fileObject.getExt())) {
            description.setType("texture");
        } else if ("jpeg".equals(fileObject.getExt())) {
            description.setType("texture");
        } else if ("gif".equals(fileObject.getExt())) {
            description.setType("texture");
        } else if ("dds".equals(fileObject.getExt())) {
            description.setType("texture");
        } else if (fileObject.getName().endsWith(".mesh")) {
            description.setType("mesh");
        } else if (fileObject.getName().endsWith(".skeleton")) {
            description.setType("skeleton");
        }
        return description;
    }

    public static void addAllFiles(ProjectAssetManager pm, AssetConfiguration config) {
        Element assetElement = config.getAssetElement();
        NodeList list = assetElement.getElementsByTagName("file");
        ProjectAssetManager proman = null;
        try {
            proman = SceneApplication.getApplication().getCurrentSceneRequest().getManager();
            if (proman == null) {
                Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not get project asset manager!");
                return;
            }
        } catch (Exception e) {
            Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not get project asset manager!");
            return;
        }
        for (int i = 0; i < list.getLength(); i++) {
            Element fileElem = (Element) list.item(i);
            try {
                String src = pm.getAbsoluteAssetPath(fileElem.getAttribute("path"));
                if (src == null) {
                    Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not find texture with manager!");
                    return;
                }
                FileObject srcFile = FileUtil.toFileObject(new File(src));
                String destName = proman.getAssetFolderName() + "/" + fileElem.getAttribute("path");
                String destFolder = destName.replace("\\", "/");
                destFolder = destFolder.substring(0, destFolder.lastIndexOf("/"));
                FileObject folder = FileUtil.createFolder(new File(destFolder));
                srcFile.copy(folder, srcFile.getName(), srcFile.getExt());
            } catch (IOException ex) {
                Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not copy texture: {0}", ex.getMessage());
            }
        }
        return;
    }

    public static void addModelFiles(ProjectAssetManager pm, AssetConfiguration config) {
        Element assetElement = config.getAssetElement();
        NodeList fileNodeList = assetElement.getElementsByTagName("file");
        ProjectAssetManager currentProjectAssetManager = null;
        try {
            currentProjectAssetManager = SceneApplication.getApplication().getCurrentSceneRequest().getManager();
            if (currentProjectAssetManager == null) {
                Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not get project asset manager!");
                return;
            }
        } catch (Exception e) {
            Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not get project asset manager!");
            return;
        }
        for (int i = 0; i < fileNodeList.getLength(); i++) {
            Element fileElem = (Element) fileNodeList.item(i);
            String type = fileElem.getAttribute("type");
            if ("texture".equals(type) || "sound".equals(type) || "materialdef".equals(type) || "shader".equals(type) || "other".equals(type)) {
                try {
                    String src = pm.getAbsoluteAssetPath(fileElem.getAttribute("path"));
                    if (src == null) {
                        Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not find texture with manager!");
                        return;
                    }
                    FileObject srcFile = FileUtil.toFileObject(new File(src));
                    String destName = currentProjectAssetManager.getAssetFolderName() + "/" + fileElem.getAttribute("path");
                    String destFolder = destName.replace("\\", "/");
                    destFolder = destFolder.substring(0, destFolder.lastIndexOf("/"));
                    FileObject folder = FileUtil.createFolder(new File(destFolder));
                    srcFile.copy(folder, srcFile.getName(), srcFile.getExt());
                } catch (IOException ex) {
                    Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not copy texture: {0}", ex.getMessage());
                }
            }
        }
        List<NodeList> varAssets = config.getVariationAssets();
        if (varAssets != null) {
            for (NodeList nodeList : varAssets) {
                addVariationFiles(nodeList, pm);
            }
        }
        return;
    }

    private static void addVariationFiles(NodeList fileNodeList, ProjectAssetManager pm) {
        ProjectAssetManager currentProjectAssetManager = null;
        try {
            currentProjectAssetManager = SceneApplication.getApplication().getCurrentSceneRequest().getManager();
            if (currentProjectAssetManager == null) {
                Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not get project asset manager!");
                return;
            }
        } catch (Exception e) {
            Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not get project asset manager!");
            return;
        }
        for (int i = 0; i < fileNodeList.getLength(); i++) {
            Element fileElem = (Element) fileNodeList.item(i);
            String type = fileElem.getAttribute("type");
            try {
                if ("texture".equals(type) || "sound".equals(type) || "materialdef".equals(type) || "shader".equals(type) || "other".equals(type)) {
                    String src = pm.getAbsoluteAssetPath(fileElem.getAttribute("path"));
                    if (src == null) {
                        Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not find texture with manager!");
                        return;
                    }
                    FileObject srcFile = FileUtil.toFileObject(new File(src));
                    String destName = currentProjectAssetManager.getAssetFolderName() + "/" + fileElem.getAttribute("path");
                    String destFolder = destName.replace("\\", "/");
                    destFolder = destFolder.substring(0, destFolder.lastIndexOf("/"));
                    FileObject folder = FileUtil.createFolder(new File(destFolder));
                    srcFile.copy(folder, srcFile.getName(), srcFile.getExt());
                }
            } catch (IOException ex) {
                Logger.getLogger(AssetPackLoader.class.getName()).log(Level.SEVERE, "Could not copy texture: {0}", ex.getMessage());
            }
        }
        return;
    }

    public static class SelectionEntry {

        String part;
        List<Element> names;
        int selected = 0;

        public SelectionEntry(String part, List<Element> names) {
            this.part = part;
            this.names = names;
        }

        public Element getSelected() {
            return names.get(selected);
        }

        public void setSelected(int selected) {
            this.selected = selected;
        }
    }
}
