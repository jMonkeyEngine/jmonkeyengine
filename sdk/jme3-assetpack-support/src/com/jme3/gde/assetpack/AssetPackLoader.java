/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack;

import com.jme3.animation.AnimControl;
import com.jme3.animation.BoneAnimation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.SkeletonControl;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author normenhansen
 */
public class AssetPackLoader {

    public static Spatial loadAssetPackModel(Element assetElement, ProjectAssetManager pm) {
        NodeList fileNodeList = assetElement.getElementsByTagName("file");
        Element fileElement = XmlHelper.findChildElementWithAttribute(assetElement, "file", "main", "true");
        if (fileElement == null) {
            fileElement = XmlHelper.findChildElement(assetElement, "file");
        }
        Spatial model = null;
        Node node = null;
        while (fileElement != null) {
            Logger.getLogger(AssetPackLoader.class.getName()).log(Level.INFO, "Load main file {0}", fileElement.getAttribute("path"));
            if (model != null && node == null) {
                node = new Node(assetElement.getAttribute("name"));
                node.attachChild(model);
            }
            model = AssetPackLoader.loadSingleMesh(fileElement, fileNodeList, pm);
            if (model != null && node != null) {
                node.attachChild(model);
            } else {
                Logger.getLogger(AssetPackLoader.class.getName()).log(Level.WARNING, "Error loading model");
            }
            //TODO:doesnt work?
            fileElement = XmlHelper.findNextElementWithAttribute(fileElement, "file", "main", "true");
        }
        if (node != null) {
            return node;
        }
        return model;
    }

    // TODO: merge animation controls for multi meshes
    private static void moveControls(Spatial from, Node to) {
        AnimControl control = to.getControl(AnimControl.class);
        AnimControl control2 = from.getControl(AnimControl.class);
        if (control == null) {
            SkeletonControl fromSkeletonControl = from.getControl(SkeletonControl.class);
            control = new AnimControl(control2.getSkeleton());
            SkeletonControl toSkeletonControl = to.getControl(SkeletonControl.class);
            if (toSkeletonControl == null) {
                toSkeletonControl = new SkeletonControl(fromSkeletonControl.getTargets(), control.getSkeleton());
            }
            to.addControl(control);
            to.addControl(toSkeletonControl);
        }
        Collection<String> names = control.getAnimationNames();
        Collection<String> names2 = new LinkedList<String>(control2.getAnimationNames());
        //add tracks from anims interface second that exist in first
        for (Iterator<String> it = names.iterator(); it.hasNext();) {
            String string = it.next();
            names2.remove(string);
            BoneAnimation anim = control.getAnim(string);
            BoneTrack[] tracks = anim.getTracks();
            BoneAnimation anim2 = control2.getAnim(string);
            if (anim2 != null) {
                BoneTrack[] tracks2 = anim2.getTracks();
                BoneTrack[] newTracks = new BoneTrack[tracks.length + tracks2.length];
                for (int i = 0; i < tracks.length; i++) {
                    newTracks[i] = tracks[i];
                }
                for (int i = tracks.length; i < tracks2.length; i++) {
                    newTracks[i] = tracks2[i - tracks.length];
                }
                anim.setTracks(newTracks);
            }
        }
        //add tracks from anims in second to first
        for (Iterator<String> it = names2.iterator(); it.hasNext();) {
            String string = it.next();
            BoneAnimation anim2 = control2.getAnim(string);
            BoneTrack[] tracks2 = anim2.getTracks();
            BoneAnimation anim = control.getAnim(string);
            if (anim != null) {
                BoneTrack[] tracks = anim.getTracks();
                BoneTrack[] newTracks = new BoneTrack[tracks.length + tracks2.length];
                for (int i = 0; i < tracks.length; i++) {
                    newTracks[i] = tracks[i];
                }
                for (int i = tracks.length; i < tracks2.length; i++) {
                    newTracks[i] = tracks2[i - tracks.length];
                }
                anim.setTracks(newTracks);
            } else {
                control.addAnim(anim2);
            }
        }
    }

    private static Spatial loadSingleMesh(Element fileElement, NodeList fileNodeList, ProjectAssetManager pm) {
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
                        mat = pm.getManager().loadMaterial(path);
                    } else if (hasExtension(path, "material")) {
                        if (matList == null) {
                            Logger.getLogger(AssetPackLoader.class.getName()).log(Level.INFO, "Load Ogre Material");
                            OgreMaterialKey matKey = new OgreMaterialKey(path);
                            matKey.setMaterialExtensionSet(matExts);
                            matList = pm.getManager().loadAsset(matKey);
                            key = new OgreMeshKey(name, matList);
                        } else {
                            Logger.getLogger(AssetPackLoader.class.getName()).log(Level.INFO, "Add Ogre Material");
                            OgreMaterialKey matKey = new OgreMaterialKey(path);
                            matKey.setMaterialExtensionSet(matExts);
                            MaterialList newMatList = pm.getManager().loadAsset(matKey);
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
                        mat = pm.getManager().loadMaterial(path);
                    }
                }
            }
        } else if (hasExtension(name, "j3o")) {
            //should have all info inside
        }
        if (key != null && mat != null) {
            Logger.getLogger(AddAssetAction.class.getName()).log(Level.WARNING, "j3m and ogre material defined for asset {0}.", name);
        }
        if (key != null) {
            model = pm.getManager().loadAsset(key);
        } else {
            model = pm.getManager().loadModel(name);
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

    public static FileDescription getFileDescription(File file) {
        FileObject fileObject = FileUtil.toFileObject(file);
        return getFileDescription(fileObject);
    }

    public static FileDescription getFileDescription(FileObject fileObject) {
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

    public static void addAllFiles(Element assetElement, ProjectAssetManager pm) {
        //TODO: not good :/
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

    public static void addModelFiles(Element assetElement, ProjectAssetManager pm) {
        //TODO: not good :/
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
        return;
    }
}
