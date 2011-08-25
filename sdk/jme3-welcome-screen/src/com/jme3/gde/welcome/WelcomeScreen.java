/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.welcome;

//import atmosphere.Planet;
//import atmosphere.PlanetRendererState;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.PreviewRequest;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneListener;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.gde.core.sceneexplorer.nodes.NodeUtility;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.Caps;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Image;
import com.jme3.texture.TextureCubeMap;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.checkbox.CheckboxControl;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.net.URL;
import java.util.concurrent.Callable;
import org.netbeans.api.javahelp.Help;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 *
 * @author normenhansen
 */
public class WelcomeScreen implements ScreenController {

//    PlanetRendererState planetView;
    SceneRequest request;
    NiftyJmeDisplay niftyDisplay;
    Nifty nifty;
    Screen screen;
    Spatial skyBox;

    public void startScreen() {
        final Node rootNode = new Node("Welcome Screen");
        request = new SceneRequest(this, NodeUtility.createNode(rootNode), new ProjectAssetManager());
        request.setHelpCtx(new HelpCtx("com.jme3.gde.core.about"));
        request.setWindowTitle("Welcome to jMonkeyEngine");
        final WelcomeScreen welcomeScreen = this;
        final DirectionalLight dirLight = new DirectionalLight();
        dirLight.setDirection(new Vector3f(.1f, 1, .1f).normalizeLocal());
        dirLight.setColor(ColorRGBA.Gray);
        SceneApplication.getApplication().addSceneListener(new SceneListener() {

            @Override
            public void sceneRequested(SceneRequest request) {
                if (request.getRequester() == WelcomeScreen.this) {
                    //FIXME: planet location dont work?
                    if (SceneApplication.getApplication().getRenderer().getCaps().contains(Caps.OpenGL21)) {
//                        planetView = new PlanetRendererState(new Planet(100f, new Vector3f(0, 0, 0)), dirLight);
//                        SceneApplication.getApplication().getStateManager().attach(planetView);
                    }
                    SceneApplication.getApplication().getViewPort().getScenes().get(0).addLight(dirLight);
                    SceneApplication.getApplication().getCamera().setLocation(new Vector3f(0, 0, 400));
                    setupSkyBox();
                    niftyDisplay = new NiftyJmeDisplay(SceneApplication.getApplication().getAssetManager(),
                            SceneApplication.getApplication().getInputManager(),
                            SceneApplication.getApplication().getAudioRenderer(),
                            SceneApplication.getApplication().getGuiViewPort());
                    nifty = niftyDisplay.getNifty();
                    try {
                        nifty.fromXml("Interface/WelcomeScreen.xml", new URL("nbres:/Interface/WelcomeScreen.xml").openStream(), "start", welcomeScreen);
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }

                    // attach the nifty display to the gui view port as a processor
                    SceneApplication.getApplication().getGuiViewPort().addProcessor(niftyDisplay);
                }
            }

            @Override
            public boolean sceneClose(SceneRequest request) {
                SceneApplication.getApplication().getViewPort().getScenes().get(0).removeLight(dirLight);
                skyBox.removeFromParent();
                SceneApplication.getApplication().getGuiViewPort().removeProcessor(niftyDisplay);
                nifty.exit();
//                if (planetView != null) {
//                    SceneApplication.getApplication().getStateManager().detach(planetView);
//                }
                SceneApplication.getApplication().removeSceneListener(this);
                return true;
            }

            @Override
            public void previewRequested(PreviewRequest request) {
            }
        });
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                SceneApplication.getApplication().requestScene(request);
                return null;
            }
        });
    }

    private void setupSkyBox() {
        Mesh sphere = new Sphere(32, 32, 10f);
        sphere.setStatic();
        skyBox = new Geometry("SkyBox", sphere);
        skyBox.setQueueBucket(Bucket.Sky);
        skyBox.setShadowMode(ShadowMode.Off);

        Image cube = SceneApplication.getApplication().getAssetManager().loadTexture("Textures/blue-glow-1024.dds").getImage();
        TextureCubeMap cubemap = new TextureCubeMap(cube);

        Material mat = new Material(SceneApplication.getApplication().getAssetManager(), "Common/MatDefs/Misc/Sky.j3md");
        mat.setBoolean("SphereMap", false);
        mat.setTexture("Texture", cubemap);
        mat.setVector3("NormalScale", new Vector3f(1, 1, 1));
        skyBox.setMaterial(mat);

        ((Node) SceneApplication.getApplication().getViewPort().getScenes().get(0)).attachChild(skyBox);
    }

    public void setNoStartup() {
        NbPreferences.forModule(Installer.class).put("NO_WELCOME_SCREEN", "true");
    }

    public void startUpdating() {
        nifty.gotoScreen("updating");
    }

    public void startIntro() {
        nifty.gotoScreen("intro");
    }

    public void startPlanet() {
        nifty.gotoScreen("planet");
    }

    public void creatingProjects() {
        Lookup.getDefault().lookup(Help.class).showHelp(new HelpCtx("sdk.project_creation"));
    }

    public void importingModels() {
        Lookup.getDefault().lookup(Help.class).showHelp(new HelpCtx("sdk.model_loader_and_viewer"));
    }

    public void editingScenes() {
        Lookup.getDefault().lookup(Help.class).showHelp(new HelpCtx("sdk.scene_composer"));
    }

    public void editingCode() {
        Lookup.getDefault().lookup(Help.class).showHelp(new HelpCtx("sdk.code_editor"));
    }

    public void updatingJmp() {
        Lookup.getDefault().lookup(Help.class).showHelp(new HelpCtx("com.jme3.gde.core.updating"));
    }

    public void tutorials() {
        Lookup.getDefault().lookup(Help.class).showHelp(new HelpCtx("jme3.beginner.hello_simpleapplication"));
    }

    public void quit() {
        if (screen.findNiftyControl("checkbox", CheckboxControl.class).isChecked()) {
            setNoStartup();
        }
        SceneApplication.getApplication().closeScene(request);
    }

    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    public void onStartScreen() {
    }

    public void onEndScreen() {
    }
}
