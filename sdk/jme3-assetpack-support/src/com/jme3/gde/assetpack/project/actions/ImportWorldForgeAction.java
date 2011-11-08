/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.project.actions;

import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetKey;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.gde.assetpack.AssetPackLoader;
import com.jme3.gde.assetpack.XmlHelper;
import com.jme3.gde.assetpack.project.AssetPackProject;
import com.jme3.gde.assetpack.project.wizards.FileDescription;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.ogretools.convert.OgreXMLConvert;
import com.jme3.gde.ogretools.convert.OgreXMLConvertOptions;
import com.jme3.material.MaterialList;
import com.jme3.scene.plugins.ogre.OgreMeshKey;
import com.jme3.scene.plugins.ogre.matext.MaterialExtension;
import com.jme3.scene.plugins.ogre.matext.MaterialExtensionSet;
import com.jme3.scene.plugins.ogre.matext.OgreMaterialKey;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Message;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public final class ImportWorldForgeAction implements Action {

    private final AssetPackProject project;
    private FileObject folder;
    private ProjectAssetManager pm;
    private DesktopAssetManager mgr;
    private HashMap<String, ArrayList<String>> matRefs = new HashMap<String, ArrayList<String>>();
    private List<String> modelNames = new ArrayList<String>();

    public ImportWorldForgeAction(AssetPackProject context) {
        this.project = context;
        pm = project.getProjectAssetManager();
        folder = pm.getAssetFolder();
        mgr = new DesktopAssetManager(true);
        mgr.registerLocator(folder.getPath(), "com.jme3.asset.plugins.FileLocator");
    }

    public void actionPerformed(ActionEvent ev) {
        matRefs.clear();
        modelNames.clear();
        // TODO use context
//        javax.swing.JFileChooser fr = new javax.swing.JFileChooser();
//        javax.swing.filechooser.FileSystemView fw = fr.getFileSystemView();
//        String projectDir = fw.getDefaultDirectory().getAbsolutePath();
//        FileChooserBuilder builder = new FileChooserBuilder(projectDir);
//        builder.setApproveText("Import");
//        builder.setTitle("Please select WorldForge checkout folder");
//        builder.setDirectoriesOnly(true);
//        final File file = builder.showOpenDialog();
//        if (file == null) {
//            return;
//        }
        //FileUtil.toFileObject(file);
        new Thread(new Runnable() {

            public void run() {
                ProgressHandle handle = ProgressHandleFactory.createHandle("Import WorldForge Models");
                handle.start();
                FileObject objects = folder.getFileObject("3d_objects");
                if (objects == null) {
                    showError("Cannot find worldforge content!\nPlease copy content of worldforge folder\ninto the assets folder of this project!");
                    handle.finish();
                    return;
                }
                List<String> binFiles = new ArrayList<String>();
                convertOgreBinary(folder.getPath(), binFiles, handle);
                deleteOgreBinary(binFiles);
                handle.progress("Scanning folder");
                scanFiles(folder);
                //iterate through found models and add asset items
                for (Iterator<String> it = modelNames.iterator(); it.hasNext();) {
                    String modelName = it.next();
                    handle.progress("Scanning " + modelName);
                    //TODO: fill data
                    Element elem = project.getConfiguration().createElement("asset");
                    elem.setAttribute("name", getFileName(modelName));
                    elem.setAttribute("type", "model");
                    elem.setAttribute("format", "ogrexml");
                    elem.setAttribute("categories", getCategory(modelName));
                    elem.setAttribute("tags", getTags(modelName));
                    Element description = project.getConfiguration().createElement("description");
                    elem.appendChild(description);


                    List<String> matNames = getModelMaterialNames(modelName);
                    Element variationsElement = null;
                    MaterialList keyMaterialList = new MaterialList();
                    //assemble material variation assets and add variations
                    for (String matName : matNames) {
                        MaterialList materialList = null;
                        Element partElement = null;
                        ArrayList<String> matFiles = matRefs.get(matName);
                        if (matFiles != null) {
                            for (String matFile : matFiles) {
                                Element variationElement = null;
                                if (variationsElement == null) {
                                    variationsElement = project.getConfiguration().createElement("materialvariations");
                                    elem.appendChild(variationsElement);
                                }
                                if (partElement == null) {
                                    partElement = project.getConfiguration().createElement("mesh");
                                    partElement.setAttribute("name", matName);
                                    variationsElement.appendChild(partElement);
                                }
                                List<AssetKey<?>> list = new ArrayList<AssetKey<?>>();
                                materialList = getMaterialAssetList(matFile, list);
                                for (Iterator<AssetKey<?>> it1 = list.iterator(); it1.hasNext();) {
                                    AssetKey<? extends Object> assetKey = it1.next();
                                    if (variationElement == null) {
                                        variationElement = project.getConfiguration().createElement("variation");
                                        variationElement.setAttribute("name", getFolderName(matFile));
                                        partElement.appendChild(variationElement);
                                    }
                                    FileDescription desc = AssetPackLoader.getFileDescription(getAbsoluteAssetPath(assetKey.getName()));
                                    if (desc != null) {
                                        Element file = project.getConfiguration().createElement("file");
                                        file.setAttribute("path", assetKey.getName());
                                        file.setAttribute("type", desc.getType());
                                        variationElement.appendChild(file);
                                    }
                                }
                            }
                        }
                        if (materialList != null) {
                            keyMaterialList.putAll(materialList);
                        }
                    }
                    //assemble main assets and add to file
                    if (keyMaterialList != null) {
                        OgreMeshKey meshKey = new OgreMeshKey(modelName, keyMaterialList);
                        List<AssetKey<?>> list = new ArrayList<AssetKey<?>>();
                        if (getModelAssetList(meshKey, list)) {
                            for (AssetKey<?> assetKey : list) {
                                Element file = project.getConfiguration().createElement("file");
                                if (assetKey.getName().endsWith(".mesh.xml")) {
                                    file.setAttribute("main", "true");
                                }
                                file.setAttribute("path", assetKey.getName());
                                FileDescription descr = AssetPackLoader.getFileDescription(getAbsoluteAssetPath(assetKey.getName()));
                                file.setAttribute("type", descr.getType());
                                elem.appendChild(file);
                            }
                            project.getProjectAssets().appendChild(elem);
                            project.saveSettings();
                            project.getAssetPackFolder().refresh();
                        }
                    }
                }
                handle.finish();
            }
        }).start();
    }

    private void scanFiles(FileObject folder) {
        FileObject[] files = folder.getChildren();
        for (FileObject fileObject : files) {
            if (fileObject.isFolder() && !fileObject.isVirtual()) {
                scanFiles(fileObject);
            } else if (fileObject.getPath().endsWith(".mesh.xml")) {
//                replaceMeshMatName(fileObject);
                //TODO: workaround
                if (!fileObject.getName().equals("campfire.mesh")) {
                    modelNames.add(getRelativeAssetPath(fileObject.getPath()));
                }
            } else if ("material".equals(fileObject.getExt())) {
//                replaceMaterialMatName(fileObject);
                String name = getMaterialName(fileObject);
                if (name != null) {
                    addMaterialRef(name, getRelativeAssetPath(fileObject.getPath()));
                }
            }
        }
    }

    private void replaceMaterialMatName(FileObject file) {
        try {
            List<String> lines = file.asLines();
            boolean changed = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("material") && line.contains(":")) {
                    int idx = line.indexOf(":");
                    String matName = line.substring(9, idx).trim();
                    String newName = removePastUnderScore(matName);
                    if (!matName.equals(newName)) {
                        Logger.getLogger(ImportWorldForgeAction.class.getName()).log(Level.INFO, "Change material name for {0}", file);
                        lines.set(i, line.replace(matName, newName));
                        changed = true;
                    }
                }
            }
            if (changed) {
                OutputStreamWriter out = new OutputStreamWriter(file.getOutputStream());
                for (String string : lines) {
                    out.write(string + "\n");
                }
                out.close();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void replaceMeshMatName(FileObject file) {
        InputStream stream = null;
        try {
            stream = file.getInputStream();
            Document doc = XMLUtil.parse(new InputSource(stream), false, false, null, null);
            stream.close();
            Element elem = doc.getDocumentElement();
            if (elem == null) {
                throw new IllegalStateException("Cannot find root mesh element");
            }
            Element submeshes = XmlHelper.findChildElement(elem, "submeshes");
            if (submeshes == null) {
                throw new IllegalStateException("Cannot find submeshes element");
            }
            Element submesh = XmlHelper.findChildElement(submeshes, "submesh");
            boolean changed = false;
            while (submesh != null) {
                String matName = submesh.getAttribute("material");
                String newName = removePastUnderScore(matName);
                if (!matName.equals(newName)) {
                    Logger.getLogger(ImportWorldForgeAction.class.getName()).log(Level.INFO, "Change material name for {0}", file);
                    submesh.setAttribute("material", newName);
                    submesh = XmlHelper.findNextSiblingElement(submesh);
                    changed = true;
                }
            }
            if (changed) {
                OutputStream out = file.getOutputStream();
                XMLUtil.write(doc, out, "UTF-8");
                out.close();
            }

        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        } finally {
        }
    }

    private String removePastUnderScore(String name) {
        int idx = name.lastIndexOf("/");
        if (idx == -1) {
            return name;
        }
        int idx2 = name.indexOf("_", idx);
        if (idx2 == -1) {
            return name;
        } else {
            return name.substring(0, idx2);
        }
    }

    private void addMaterialRef(String matName, String assetName) {
        ArrayList<String> list = matRefs.get(matName);
        if (list == null) {
            list = new ArrayList<String>();
            matRefs.put(matName, list);
        }
        list.add(assetName);
    }

    private void convertOgreBinary(String dir2scan, List<String> deleteFiles, ProgressHandle handle) {
        try {
            File zipDir = new File(dir2scan);
            String[] dirList = zipDir.list();
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    convertOgreBinary(filePath, deleteFiles, handle);
                    continue;
                }
                FileObject fobj = FileUtil.toFileObject(f);
                if (fobj.getExt().equalsIgnoreCase("mesh") || fobj.getExt().equalsIgnoreCase("skeleton")) {
                    OgreXMLConvertOptions options = new OgreXMLConvertOptions(fobj.getPath());
                    options.setBinaryFile(true);
                    OgreXMLConvert conv = new OgreXMLConvert();
                    conv.doConvert(options, handle);
                    deleteFiles.add(fobj.getPath());
                }
            }
        } catch (Exception e) {
            Logger.getLogger(ConvertOgreBinaryMeshesAction.class.getName()).log(Level.SEVERE, "Error scanning directory", e);
        } finally {
//            handle.finish();
        }
    }

    private void deleteOgreBinary(List<String> createdFiles) {
        for (String string : createdFiles) {
            new File(string).delete();
        }
    }

    private OgreMaterialKey getOgreMaterialKey(String materialName) {
        /**
         * /base/normalmap/specular
         * /base/normalmap
         * /base/simple
         */
        MaterialExtensionSet matExts = new MaterialExtensionSet();
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

        OgreMaterialKey key = new OgreMaterialKey(materialName);
        key.setMaterialExtensionSet(matExts);
        return key;
    }

    private MaterialList getMaterialAssetList(String key, final List<AssetKey<?>> assetKeys) {
        final AtomicBoolean good = new AtomicBoolean(true);
        mgr.clearCache();
        mgr.setAssetEventListener(new AssetEventListener() {

            public void assetLoaded(AssetKey ak) {
            }

            public void assetRequested(AssetKey ak) {
                if (!"j3md".equalsIgnoreCase(ak.getExtension())
                        && !"glsllib".equalsIgnoreCase(ak.getExtension())
                        && !"frag".equalsIgnoreCase(ak.getExtension())
                        && !"vert".equalsIgnoreCase(ak.getExtension())
                        && !"vert".equalsIgnoreCase(ak.getExtension())) {
                    assetKeys.add(ak);
                }
                if (ak.getName().equals("Common/Materials/RedColor.j3m")) {
                    good.set(false);
                }
            }

            public void assetDependencyNotFound(AssetKey ak, AssetKey ak1) {
            }
            
        });
        try {
            return mgr.loadAsset(getOgreMaterialKey(key));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean getModelAssetList(OgreMeshKey key, final List<AssetKey<?>> assetKeys) {
        final AtomicBoolean good = new AtomicBoolean(true);
        mgr.clearCache();
        mgr.setAssetEventListener(new AssetEventListener() {

            public void assetLoaded(AssetKey ak) {
            }

            public void assetRequested(AssetKey ak) {
                if (ak instanceof OgreMaterialKey) {
                    MaterialExtensionSet matExts = new MaterialExtensionSet();
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

                    ((OgreMaterialKey) ak).setMaterialExtensionSet(matExts);

                }
                if (!"j3md".equalsIgnoreCase(ak.getExtension())
                        && !"glsllib".equalsIgnoreCase(ak.getExtension())
                        && !"frag".equalsIgnoreCase(ak.getExtension())
                        && !"vert".equalsIgnoreCase(ak.getExtension())
                        && !"vert".equalsIgnoreCase(ak.getExtension())) {
                    assetKeys.add(ak);
                }
                if (ak.getName().equals("Common/Materials/RedColor.j3m")) {
                    good.set(false);
                }
            }

            public void assetDependencyNotFound(AssetKey ak, AssetKey ak1) {
            }
            
        });
        try {
            mgr.loadModel(key);
        } catch (Exception e) {
            return false;
        }
        if (!good.get()) {
            return false;
        }
        return true;
    }

    private List<String> getModelMaterialNames(String assetName) {
        List<String> materialNames = new ArrayList<String>();
        //TODO: check use of File
        FileObject file = FileUtil.toFileObject(new File(getAbsoluteAssetPath(assetName).replaceAll("/", File.separator)));//Repository.getDefault().findResource(getAbsoluteAssetPath(assetName));
        InputStream stream = null;
        try {
            stream = file.getInputStream();
            Document doc = XMLUtil.parse(new InputSource(stream), false, false, null, null);
            Element elem = doc.getDocumentElement();
            if (elem == null) {
                throw new IllegalStateException("Cannot find root mesh element");
            }
            Element submeshes = XmlHelper.findChildElement(elem, "submeshes");
            if (submeshes == null) {
                throw new IllegalStateException("Cannot find submeshes element");
            }
            Element submesh = XmlHelper.findChildElement(submeshes, "submesh");
            while (submesh != null) {
                String matName = submesh.getAttribute("material");
                if (!materialNames.contains(matName)) {
                    materialNames.add(matName);
                }
                submesh = XmlHelper.findNextSiblingElement(submesh);
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        return materialNames;
    }

    private String getMaterialName(FileObject file) {
        try {
            System.out.println("MaterialScan " + file);
            List<String> lines = file.asLines();
            for (String line : lines) {
                if (line.startsWith("material") && line.contains(":")) {
                    int idx = line.indexOf(":");
                    return line.substring(9, idx).trim();
                }
            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    private String getRelativeAssetPath(String absolutePath) {
        String prefix = folder.getPath();
        int idx = absolutePath.indexOf(prefix);
        if (idx == 0) {
            return absolutePath.substring(prefix.length() + 1).replace("./", "");
        }
        return null;
    }

    private String getAbsoluteAssetPath(String relatvePath) {
        String prefix = folder.getPath();
        return (prefix + "/" + relatvePath).replace("./", "");
    }

    private String getTags(String modelName) {
        String[] strings = modelName.split("/");
        String ret = null;
        for (String string : strings) {
            if (!"models".equals(string) && !"3d_objects".equals(string) && !string.contains(".mesh.xml")) {
                if (ret == null) {
                    ret = string;
                } else {
                    ret = ret + ", " + string;
                }
            }
        }
        return ret;
    }

    private String getCategory(String modelName) {
        if (!modelName.startsWith("3d_objects/")) {
            return "";
        }
        int idx = modelName.indexOf("/", 11);
        if (idx == -1) {
            return "";
        }
        return modelName.substring(11, idx);
    }

    private String getFolderName(String name) {
        int start = name.substring(0, name.lastIndexOf("/")).lastIndexOf("/") + 1;
        return name.substring(start, name.lastIndexOf("/"));
    }

    private String getFileName(String name) {
        return name.substring(name.lastIndexOf("/") + 1, name.indexOf("."));
    }

    private void showError(String e) {
        Message msg = new NotifyDescriptor.Message(
                e,
                NotifyDescriptor.ERROR_MESSAGE);
        DialogDisplayer.getDefault().notifyLater(msg);
    }

    public Object getValue(String key) {
        if (key.equals(NAME)) {
            return "Scan WorldForge..";
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
