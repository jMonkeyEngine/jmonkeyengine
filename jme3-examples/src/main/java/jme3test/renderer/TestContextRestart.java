package jme3test.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

public class TestContextRestart extends SimpleApplication implements ActionListener {
    public static void main(String[] args) {
        TestContextRestart app = new TestContextRestart();
        AppSettings settings = new AppSettings(true);
        settings.setGammaCorrection(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        for (int i = 0, l = 256; i < l; i += 8) {
            Geometry box = new Geometry("Box" + i, new Box(10, 200, 10));
            Material mat = new Material(this.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", new ColorRGBA((float) i / 255F, 0, 0, 1));
            box.setMaterial(mat);
            box.setLocalTranslation(-2.5F * (l / 2 - i), 0, -700);
            this.rootNode.attachChild(box);
        }

        this.viewPort.setBackgroundColor(ColorRGBA.Yellow);

        this.flyCam.setEnabled(false);
        this.inputManager.setCursorVisible(true);

        this.inputManager.addMapping("restart", new KeyTrigger(KeyInput.KEY_TAB));
        this.inputManager.addListener(this, "restart");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) {
            // We need this in order to trigger the bug on jme3-lwjgl
            this.settings.setSamples(8);
            this.context.restart();
        }
    }
}
