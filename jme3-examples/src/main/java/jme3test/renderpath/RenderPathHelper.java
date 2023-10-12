package jme3test.renderpath;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.renderer.RenderManager;

public class RenderPathHelper implements ActionListener {
    private RenderManager.RenderPath currentRenderPath;
    private BitmapText hitText;
    private RenderManager renderManager;
    public RenderPathHelper(SimpleApplication simpleApplication){
        renderManager = simpleApplication.getRenderManager();
        currentRenderPath = renderManager.getRenderPath();
        makeHudText(simpleApplication);
        simpleApplication.getInputManager().addListener(this, "toggleRenderPath");
        simpleApplication.getInputManager().addMapping("toggleRenderPath", new KeyTrigger(KeyInput.KEY_SPACE));
    }

    private void makeHudText(SimpleApplication simpleApplication) {
        BitmapFont guiFont = simpleApplication.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        hitText = new BitmapText(guiFont, false);
        hitText.setSize(guiFont.getCharSet().getRenderedSize());
        hitText.setText("RendererPath : "+ currentRenderPath.getInfo());
        hitText.setLocalTranslation(0, simpleApplication.getCamera().getHeight(), 0);
        simpleApplication.getGuiNode().attachChild(hitText);

        // hit text
        BitmapText title = new BitmapText(guiFont, false);
        title.setSize(guiFont.getCharSet().getRenderedSize());
        title.setText("Please press the SPACE to toggle the render path");
        title.setLocalTranslation(0, simpleApplication.getCamera().getHeight() - 20, 0);
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
