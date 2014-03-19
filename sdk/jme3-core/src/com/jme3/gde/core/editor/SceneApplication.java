package com.jme3.gde.core.editor;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.gde.core.Installer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.jme3.system.awt.AwtPanel;
import com.jme3.system.awt.AwtPanelsContext;
import com.jme3.system.awt.PaintMode;
import java.util.concurrent.Callable;
import org.openide.util.NbPreferences;

/**
 *
 * @author normenhansen
 */
public class SceneApplication extends SimpleApplication {

    private AwtPanel panel;
    private ViewPort overlayView;

    public SceneApplication() {
        super(new StatsAppState());
        AppSettings newSetting = new AppSettings(true);
        newSetting.setFrameRate(30);
        if ("true".equals(NbPreferences.forModule(Installer.class).get("use_opengl_1", "false"))) {
            newSetting.setRenderer(AppSettings.LWJGL_OPENGL1);
        }
        newSetting.setCustomRenderer(AwtPanelsContext.class);
        setSettings(newSetting);
        setPauseOnLostFocus(false);
        setShowSettings(false);
    }

    public AwtPanel getPanel() {
        if (panel == null) {
             panel = ((AwtPanelsContext) getContext()).createPanel(PaintMode.Accelerated);
            ((AwtPanelsContext) getContext()).setInputSource(panel);
            attachPanel();
        }
        return panel;
    }

    private void attachPanel() {
        enqueue(new Callable() {
            public Object call() throws Exception {
                panel.attachTo(true, viewPort, /*overlayView, */guiViewPort);
                return null;
            }
        });
    }

    @Override
    public Node getRootNode() {
        return super.getRootNode();
    }
    
    @Override
    public void simpleInitApp() {
        overlayView = getRenderManager().createMainView("Overlay", cam);
        overlayView.setClearFlags(false, true, false);
    }

}
