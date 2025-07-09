package jme3test.model;

import com.jme3.app.*;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.plugins.gltf.GltfModelKey;
import com.jme3.system.AppSettings;
import com.jme3.util.TangentBinormalGenerator;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

public class TestGltfNormal extends SimpleApplication {
    
    Node probeNode;
    PointLight light;
    boolean flipTextures = !false;

    public static void main(String[] args) {
        AppSettings sett = new AppSettings(true);
        sett.setWidth(1024);
        sett.setHeight(768);
        TestGltfNormal app = new TestGltfNormal();
        app.setSettings(sett);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        
        assetManager.registerLocator(System.getProperty("user.home")+"/Downloads", FileLocator.class);
        assetManager.registerLocator(System.getProperty("user.home")+"/java/prj/JmeUtils/assets/Models", FileLocator.class);
        
        rootNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        probeNode = (Node) assetManager.loadModel("Scenes/defaultProbe.j3o");
        //probeNode = new Node();
        rootNode.attachChild(probeNode);

        setPauseOnLostFocus(false);

        //flyCam.setEnabled(false);
        flyCam.setMoveSpeed(20);
        viewPort.setBackgroundColor(new ColorRGBA().setAsSrgb(0.2f, 0.2f, 0.2f, 1.0f));
        
        loadModel("NormalTangentMirrorTest_FromObj.j3o", new Vector3f(0, 0, 0), 3);
        
        probeNode.addLight(new AmbientLight(ColorRGBA.Gray));
        light = new PointLight(new Vector3f(-1f, 1f, 10f), ColorRGBA.White, 1000f);
        rootNode.addLight(light);
        
    }
    @Override
    public void simpleUpdate(float tpf) {
        light.setPosition(cam.getLocation());
    }

    private void loadModel(String path, Vector3f offset, float scale) {
        GltfModelKey k = new GltfModelKey(path);
        Spatial s = assetManager.loadModel(k);
        s.scale(scale);
        s.move(offset);
        s.setMaterial(createPBRLightingMat());
        //s.setMaterial(createSPLightingMat());
        //TangentBinormalGenerator.generate(s);
        MikktspaceTangentGenerator.generate(s);
        probeNode.attachChild(s);
    }
    
    private Material createPBRLightingMat() {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setTexture("BaseColorMap", assetManager.loadTexture(new TextureKey("NormalTangentMirrorTest_BaseColor.png", flipTextures)));
        mat.setTexture("NormalMap", assetManager.loadTexture(new TextureKey("NormalTangentTest_Normal.png", flipTextures)));
        return mat;
    }
    
    private Material createSPLightingMat() {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap", assetManager.loadTexture(new TextureKey("NormalTangentMirrorTest_BaseColor.png", flipTextures)));
        mat.setTexture("NormalMap", assetManager.loadTexture(new TextureKey("NormalTangentTest_Normal.png", flipTextures)));
        mat.setBoolean("VertexLighting", false);
        return mat;
    }

}
