package com.jme3.scene.plugins.fbx.objects;

import java.io.File;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import com.jme3.scene.plugins.fbx.ContentTextureKey;
import com.jme3.scene.plugins.fbx.ContentTextureLocator;
import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxImage extends FbxObject {

    String filename;
    String relativeFilename;
    byte[] content;
    String imageType;

    public Image image;

    public FbxImage(SceneLoader scene, FbxElement element) {
        super(scene, element);
        if(type.equals("Clip")) {
            for(FbxElement e : element.children) {
                switch(e.id) {
                case "Type":
                    imageType = (String) e.properties.get(0);
                    break;
                case "Filename":
                case "FileName":
                    filename = (String) e.properties.get(0);
                    break;
                case "RelativeFilename":
                    relativeFilename = (String) e.properties.get(0);
                    break;
                case "Content":
                    if(e.properties.size() > 0)
                        content = (byte[]) e.properties.get(0);
                    break;
                }
            }
            image = createImage();
        }
    }


    private Image createImage() {
        AssetManager assetManager = scene.assetManager;
        Image image = null;
        if(filename != null) {
            // Try load by absolute path
            File file = new File(filename);
            if(file.exists() && file.isFile()) {
                File dir = new File(file.getParent());
                String locatorPath = dir.getAbsolutePath();
                Texture tex = null;
                try {
                    assetManager.registerLocator(locatorPath, com.jme3.asset.plugins.FileLocator.class);
                    tex = assetManager.loadTexture(file.getName());
                } catch(Exception e) {} finally {
                    assetManager.unregisterLocator(locatorPath, com.jme3.asset.plugins.FileLocator.class);
                }
                if(tex != null)
                    image = tex.getImage();
            }
        }
        if(image == null && relativeFilename != null) {
            // Try load by relative path
            File dir = new File(scene.sceneFolderName);
            String locatorPath = dir.getAbsolutePath();
            Texture tex = null;
            try {
                assetManager.registerLocator(locatorPath, com.jme3.asset.plugins.FileLocator.class);
                tex = assetManager.loadTexture(relativeFilename);
            } catch(Exception e) {} finally {
                assetManager.unregisterLocator(locatorPath, com.jme3.asset.plugins.FileLocator.class);
            }
            if(tex != null)
                image = tex.getImage();
        }
        if(image == null && content != null) {
            // Try load from content
            String filename = null;
            if(this.filename != null)
                filename = new File(this.filename).getName();
            if(filename != null && this.relativeFilename != null)
                filename = this.relativeFilename;
            // Filename is required to acquire asset loader by extension
            if(filename != null) {
                String locatorPath = scene.sceneFilename;
                filename = scene.sceneFilename + File.separatorChar + filename; // Unique path
                Texture tex = null;
                try {
                    assetManager.registerLocator(locatorPath, ContentTextureLocator.class);
                    tex = assetManager.loadTexture(new ContentTextureKey(filename, content));
                } catch(Exception e) {} finally {
                    assetManager.unregisterLocator(locatorPath, ContentTextureLocator.class);
                }
                if(tex != null)
                    image = tex.getImage();
            }
        }
        if(image == null) {
            // Try to load from files near
            if(relativeFilename != null) {
                String[] split = relativeFilename.split("[\\\\/]");
                String filename = split[split.length - 1];
                Texture tex = null;
                try {
                    tex = assetManager.loadTexture(new ContentTextureKey(scene.currentAssetInfo.getKey().getFolder() + filename, content));
                } catch(Exception e) {}
                if(tex != null)
                    image = tex.getImage();
            }
        }
        if(image == null)
            return new Image(Image.Format.RGB8, 1, 1, BufferUtils.createByteBuffer((int) (Image.Format.RGB8.getBitsPerPixel() / 8L)), ColorSpace.Linear);
        return image;
    }
}
