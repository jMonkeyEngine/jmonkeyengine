package jme3test.renderpath;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;

public class RenderPathHelper implements ActionListener {
    private RenderManager.RenderPath currentRenderPath;
    private BitmapText hitText;
    private RenderManager renderManager;
    private Vector3f hitLocation = new Vector3f(0, 0, 0);
    private String keyHit = "SPACE";
    private int keyInput = KeyInput.KEY_SPACE;
    public RenderPathHelper(SimpleApplication simpleApplication){
        renderManager = simpleApplication.getRenderManager();
        currentRenderPath = renderManager.getRenderPath();
        makeHudText(simpleApplication);
        hitLocation.set(0, simpleApplication.getCamera().getHeight(), 0);
        simpleApplication.getInputManager().addListener(this, "toggleRenderPath");
        simpleApplication.getInputManager().addMapping("toggleRenderPath", new KeyTrigger(keyInput));
    }

    public RenderPathHelper(SimpleApplication simpleApplication, Vector3f hitLocation, int keyInput, String keyHit){
        renderManager = simpleApplication.getRenderManager();
        currentRenderPath = renderManager.getRenderPath();
        this.hitLocation.set(hitLocation);
        this.keyInput = keyInput;
        this.keyHit = keyHit;
        makeHudText(simpleApplication);
        simpleApplication.getInputManager().addListener(this, "toggleRenderPath");
        simpleApplication.getInputManager().addMapping("toggleRenderPath", new KeyTrigger(keyInput));
    }

    private void makeHudText(SimpleApplication simpleApplication) {
        BitmapFont guiFont = simpleApplication.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        hitText = new BitmapText(guiFont, false);
        hitText.setSize(guiFont.getCharSet().getRenderedSize());
        hitText.setText("RendererPath : "+ currentRenderPath.getInfo());
        hitText.setLocalTranslation(hitLocation);
        simpleApplication.getGuiNode().attachChild(hitText);

        // hit text
        BitmapText title = new BitmapText(guiFont, false);
        title.setSize(guiFont.getCharSet().getRenderedSize());
        title.setText("Please press the " + keyHit + " to toggle the render path");
        title.setLocalTranslation(hitLocation);
        title.getLocalTranslation().y -= 20;
        simpleApplication.getGuiNode().attachChild(title);
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
