package jme3test.renderpath;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.DirectionalLightShadowFilter;

/**
 * Under the deferred rendering path, only screen space post processing shadows can be used.<br/>
 * @author JohnKkk
 */
public class TestDeferredShadingPathShadow extends SimpleApplication implements ActionListener {
    private RenderManager.RenderPath currentRenderPath;
    private BitmapText hitText;

    private void makeHudText() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hitText = new BitmapText(guiFont, false);
        hitText.setSize(guiFont.getCharSet().getRenderedSize());
        hitText.setText("RendererPath : "+ currentRenderPath.getInfo());
        hitText.setLocalTranslation(0, cam.getHeight(), 0);
        guiNode.attachChild(hitText);
    }

    @Override
    public void simpleInitApp() {
        Material boxMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Node tank = (Node) assetManager.loadModel("Models/HoverTank/Tank2.mesh.xml");
        tank.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        tank.setLocalScale(0.3f);
        rootNode.attachChild(tank);

        Quad plane = new Quad(10, 10);
        Geometry planeGeo = new Geometry("Plane", plane);
        planeGeo.setShadowMode(RenderQueue.ShadowMode.Receive);
        planeGeo.rotate(-45, 0, 0);
        planeGeo.setLocalTranslation(-5, -5, 0);
        Material planeMat = boxMat.clone();
        planeMat.setBoolean("UseMaterialColors", true);
        planeMat.setColor("Ambient", ColorRGBA.White);
        planeMat.setColor("Diffuse", ColorRGBA.Gray);
        planeGeo.setMaterial(planeMat);
        rootNode.attachChild(planeGeo);


        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
        DirectionalLightShadowFilter dlsf = new DirectionalLightShadowFilter(assetManager, 1024, 1);
        dlsf.setLight(sun);

        sun = new DirectionalLight();
        sun.setDirection((new Vector3f(0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
        DirectionalLightShadowFilter dlsf2 = new DirectionalLightShadowFilter(assetManager, 1024, 1);
        dlsf2.setLight(sun);

        sun = new DirectionalLight();
        sun.setDirection((new Vector3f(0.0f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);
        DirectionalLightShadowFilter dlsf3 = new DirectionalLightShadowFilter(assetManager, 1024, 1);
        dlsf3.setLight(sun);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(dlsf);
        fpp.addFilter(dlsf2);
        fpp.addFilter(dlsf3);
        viewPort.addProcessor(fpp);

        inputManager.addListener(this, "toggleRenderPath");
        inputManager.addMapping("toggleRenderPath", new KeyTrigger(KeyInput.KEY_SPACE));

        currentRenderPath = RenderManager.RenderPath.Forward;
        renderManager.setRenderPath(currentRenderPath);
        makeHudText();

        flyCam.setMoveSpeed(20.0f);
    }

    public static void main(String[] args) {
        TestDeferredShadingPathShadow testDeferredShadingPathShadow = new TestDeferredShadingPathShadow();
        testDeferredShadingPathShadow.start();
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if(name.equals("toggleRenderPath") && !isPressed){
            if(currentRenderPath == RenderManager.RenderPath.Deferred){
                currentRenderPath = RenderManager.RenderPath.TiledDeferred;
            }
            else if(currentRenderPath == RenderManager.RenderPath.TiledDeferred){
                currentRenderPath = RenderManager.RenderPath.Forward;
            }
            else{
                currentRenderPath = RenderManager.RenderPath.Deferred;
            }
            renderManager.setRenderPath(currentRenderPath);
            hitText.setText("RendererPath : "+ currentRenderPath.getInfo());
        }
    }
}
