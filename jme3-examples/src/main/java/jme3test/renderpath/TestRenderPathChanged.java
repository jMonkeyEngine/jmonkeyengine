package jme3test.renderpath;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

/**
 * This demonstrates how to switch the rendering path at runtime.<br/>
 * @author JohnKkk
 */
public class TestRenderPathChanged extends SimpleApplication implements ActionListener {
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
        Node scene = (Node) assetManager.loadModel("Scenes/ManyLights/Main.scene");
        rootNode.attachChild(scene);
        Node n = (Node) rootNode.getChild(0);
        final LightList lightList = n.getWorldLightList();
        final Geometry g = (Geometry) n.getChild("Grid-geom-1");

        g.getMaterial().setColor("Ambient", new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));
        g.getMaterial().setBoolean("VertexLighting", false);

        int nb = 0;
        for (Light light : lightList) {
            nb++;
            PointLight p = (PointLight) light;
            if (nb > 60) {
                n.removeLight(light);
            } else {
                int rand = FastMath.nextRandomInt(0, 3);
                switch (rand) {
                    case 0:
                        light.setColor(ColorRGBA.Red);
                        break;
                    case 1:
                        light.setColor(ColorRGBA.Yellow);
                        break;
                    case 2:
                        light.setColor(ColorRGBA.Green);
                        break;
                    case 3:
                        light.setColor(ColorRGBA.Orange);
                        break;
                }
            }
        }

        cam.setLocation(new Vector3f(-180.61f, 64, 7.657533f));
        cam.lookAtDirection(new Vector3f(0.93f, -0.344f, 0.044f), Vector3f.UNIT_Y);

        cam.setLocation(new Vector3f(-26.85569f, 15.701239f, -19.206047f));
        cam.lookAtDirection(new Vector3f(0.13871355f, -0.6151029f, 0.7761488f), Vector3f.UNIT_Y);


        inputManager.addListener(this, "toggleRenderPath");
        inputManager.addMapping("toggleRenderPath", new KeyTrigger(KeyInput.KEY_SPACE));

        // set RenderPath
        currentRenderPath = RenderManager.RenderPath.Forward;
        renderManager.setRenderPath(currentRenderPath);
        makeHudText();

        flyCam.setMoveSpeed(20.0f);
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

    public static void main(String[] args) {
        TestRenderPathChanged testRenderPathChanged = new TestRenderPathChanged();
        testRenderPathChanged.start();
    }
}
