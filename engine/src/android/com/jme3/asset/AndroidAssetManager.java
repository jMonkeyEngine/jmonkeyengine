package com.jme3.asset;

import com.jme3.asset.plugins.AndroidLocator;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.font.BitmapFont;
import com.jme3.font.plugins.BitmapFontLoader;
import com.jme3.material.Material;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.scene.Spatial;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderKey;
import com.jme3.shader.plugins.GLSLLoader;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.plugins.AndroidImageLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AssetManager for Android
 *
 * @author Kirill Vainer
 */
public final class AndroidAssetManager implements AssetManager {

    private static final Logger logger = Logger.getLogger(AndroidAssetManager.class.getName());

    private final AndroidLocator locator = new AndroidLocator();
    private final AndroidImageLoader imageLoader = new AndroidImageLoader();
    private final BinaryImporter modelLoader = new BinaryImporter();
    private final BitmapFontLoader fontLoader = new BitmapFontLoader();
    private final J3MLoader j3mLoader = new J3MLoader();
    private final J3MLoader j3mdLoader = new J3MLoader();
    private final GLSLLoader glslLoader = new GLSLLoader();

    private final BinaryExporter exporter = new BinaryExporter();
    private final HashMap<AssetKey, Object> cache = new HashMap<AssetKey, Object>();

    public AndroidAssetManager(){
        this(false);
    }

    public AndroidAssetManager(boolean loadDefaults){
        if (loadDefaults){
//            AssetConfig cfg = new AssetConfig(this);
//            InputStream stream = AssetManager.class.getResourceAsStream("Desktop.cfg");
//            try{
//                cfg.loadText(stream);
//            }catch (IOException ex){
//                logger.log(Level.SEVERE, "Failed to load asset config", ex);
//            }finally{
//                if (stream != null)
//                    try{
//                        stream.close();
//                    }catch (IOException ex){
//                    }
//            }
        }
        logger.info("AndroidAssetManager created.");
    }

    public void registerLoader(String loaderClass, String ... extensions){
    }

    public void registerLocator(String rootPath, String locatorClass, String ... extensions){
    }

    private Object tryLoadFromHD(AssetKey key){
        if (!key.getExtension().equals("fnt"))
            return null;

        File f = new File("/sdcard/" + key.getName() + ".opt");
        if (!f.exists())
            return null;

        try {
            InputStream stream = new FileInputStream(f);
            BitmapFont font = (BitmapFont) modelLoader.load(stream, null, null);
            stream.close();
            return font;
        } catch (IOException ex){
        }

        return null;
    }

    private void tryPutToHD(AssetKey key, Object data){
        if (!key.getExtension().equals("fnt"))
            return;

        File f = new File("/sdcard/" + key.getName() + ".opt");

        try {
            BitmapFont font = (BitmapFont) data;
            OutputStream stream = new FileOutputStream(f);
            exporter.save(font, stream);
            stream.close();
        } catch (IOException ex){
        }
    }

    public Object loadAsset(AssetKey key){
	logger.info("loadAsset(" + key + ")");
        Object asset;
//        Object asset = tryLoadFromHD(key);
//        if (asset != null)
//            return asset;

        if (key.shouldCache()){
            asset = cache.get(key);
            if (asset != null)
                return key.createClonedInstance(asset);
        }
        // find resource
        AssetInfo info = locator.locate(this, key);
        if (info == null){
            logger.log(Level.WARNING, "Cannot locate resource: "+key.getName());
            return null;
        }

        String ex = key.getExtension();
        logger.log(Level.INFO, "Loading asset: "+key.getName());
        try{
            if (ex.equals("png") || ex.equals("jpg") 
             || ex.equals("jpeg") || ex.equals("j3i")){
                Image image;
                if (ex.equals("j3i")){
                    image = (Image) modelLoader.load(info);
                }else{
                    image = (Image) imageLoader.load(info);
                }
                TextureKey tkey = (TextureKey) key;
                asset = image;
                Texture tex = (Texture) tkey.postProcess(asset);
                tex.setMagFilter(Texture.MagFilter.Nearest);
                tex.setAnisotropicFilter(0);
                if (tex.getMinFilter().usesMipMapLevels()){
                    tex.setMinFilter(Texture.MinFilter.NearestNearestMipMap);
                }else{
                    tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                }
                asset = tex;
            }else if (ex.equals("j3o")){
                asset = modelLoader.load(info);
            }else if (ex.equals("fnt")){
                asset = fontLoader.load(info);
            }else if (ex.equals("j3md")){
                asset = j3mdLoader.load(info);
            }else if (ex.equals("j3m")){
                asset = j3mLoader.load(info);
            }else{
		logger.info("loading asset as glsl shader ...");
		asset = glslLoader.load(info);
               // logger.log(Level.WARNING, "No loader registered for type: "+ex);
               // return null;
            }

            if (key.shouldCache())
                cache.put(key, asset);

//            tryPutToHD(key, asset);

            return key.createClonedInstance(asset);
        } catch (Exception e){
            logger.log(Level.WARNING, "Failed to load resource: "+key.getName(), e);
        }
        return null;
    }

    public AssetInfo locateAsset(AssetKey<?> key){
        AssetInfo info = locator.locate(this, key);
        if (info == null){
            logger.log(Level.WARNING, "Cannot locate resource: "+key.getName());
            return null;
        }
	return info;
    }




    public Object loadAsset(String name) {
        return loadAsset(new AssetKey(name));
    }

    public Spatial loadModel(String name) {
        return (Spatial) loadAsset(name);
    }

    public Material loadMaterial(String name) {
        return (Material) loadAsset(name);
    }

    public BitmapFont loadFont(String name){
        return (BitmapFont) loadAsset(name);
    }

    public Texture loadTexture(TextureKey key){
        return (Texture) loadAsset(key);
    }

    public Texture loadTexture(String name){
        return loadTexture(new TextureKey(name, false));
    }

	public Shader loadShader(ShaderKey key){
		logger.info("loadShader(" + key + ")");

		String vertName = key.getVertName();
		String fragName = key.getFragName();

		String vertSource = (String) loadAsset(new AssetKey(vertName));
		String fragSource = (String) loadAsset(new AssetKey(fragName));

		Shader s = new Shader(key.getLanguage());
		s.addSource(Shader.ShaderType.Vertex,   vertName, vertSource, key.getDefines().getCompiled());
		s.addSource(Shader.ShaderType.Fragment, fragName, fragSource, key.getDefines().getCompiled());

		logger.info("returing shader: [" + s + "]");
		return s;
	}


    public void registerLocator(String rootPath, String locatorClassName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public AudioData loadAudio(AudioKey key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public AudioData loadAudio(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Spatial loadModel(ModelKey key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	/* new */

    private AssetEventListener eventListener = null;

    public void setAssetEventListener(AssetEventListener listener){
        eventListener = listener;
    }

    public void registerLocator(String rootPath, Class<? extends AssetLocator> locatorClass){
	logger.warning("not implemented.");
    }

    public void registerLoader(Class<? extends AssetLoader> loader, String ... extensions){
	logger.warning("not implemented.");
    }

  


}
