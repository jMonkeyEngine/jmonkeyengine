/*
 * Copyright (c) 2009-2010 jMonkeyEngine All rights reserved. <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. <p/> * Redistributions
 * in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. <p/> * Neither the name of
 * 'jMonkeyEngine' nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission. <p/> THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.scene;

import com.jme3.app.Application;
import com.jme3.app.StatsView;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioContext;
import com.jme3.bullet.BulletAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.gde.core.Installer;
import com.jme3.gde.core.assets.AssetData;
import com.jme3.gde.core.assets.AssetDataObject;
import com.jme3.gde.core.scene.controller.AbstractCameraController;
import com.jme3.gde.core.scene.processors.WireProcessor;
import com.jme3.gde.core.sceneexplorer.nodes.NodeUtility;
import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import com.jme3.gde.core.undoredo.SceneUndoRedoManager;
import com.jme3.gde.core.util.notify.MessageType;
import com.jme3.gde.core.util.notify.NotifyUtil;
import com.jme3.input.FlyByCamera;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.system.awt.AwtPanel;
import com.jme3.system.awt.AwtPanelsContext;
import com.jme3.system.awt.PaintMode;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.spi.project.LookupProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;
import org.openide.awt.HtmlBrowser;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author normenhansen
 */
@SuppressWarnings("unchecked")
public class SceneApplication extends Application implements LookupProvider {

    private static final Logger logger = Logger.getLogger(SceneApplication.class.getName());
    private static boolean failMessageShown = false;
    private PointLight camLight;
    private static SceneApplication application;

    public static SceneApplication getApplication() {
        if (application == null) {
            application = new SceneApplication();
        }
        return application;
    }
    protected Node rootNode = new Node("Root Node") {
        @Override
        public boolean removeFromParent() {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Trying to remove main RootNode!"));
            return false;
        }
    };
    protected Node guiNode = new Node("Gui Node");
    private Node statsGuiNode = new Node("Stats Gui Node");
    protected Node toolsNode = new Node("Tools Node");
    private SceneCameraController camController;
    private AbstractCameraController activeCamController = null;
    //preview variables
    protected float secondCounter = 0.0f;
    protected BitmapText fpsText;
    protected StatsView statsView;
    protected FlyByCamera flyCam;
    protected boolean showSettings = true;
    private SceneRequest currentSceneRequest;
    private ConcurrentLinkedQueue<SceneListener> listeners = new ConcurrentLinkedQueue<SceneListener>();
    private ScenePreviewProcessor previewProcessor;
    private ApplicationLogHandler logHandler = new ApplicationLogHandler();
    private WireProcessor wireProcessor;
    private ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Opening SceneViewer..");
    private String lastError = "";
    private boolean started = false;
    private boolean initFailed = false;
    private AwtPanel panel;
    private ViewPort overlayView;
    boolean useCanvas = false;
    private BulletAppState physicsState;
    private Thread thread;
    private NodeSyncAppState nodeSync;
    private FakeApplication fakeApp;

    public SceneApplication() {
        Logger.getLogger("com.jme3").addHandler(logHandler);
        progressHandle.start(7);
        useCanvas = "true".equals(NbPreferences.forModule(Installer.class).get("use_lwjgl_canvas", "false"));
        try {
            AppSettings newSetting = new AppSettings(true);
            newSetting.setFrameRate(30);
            if ("true".equals(NbPreferences.forModule(Installer.class).get("use_opengl_1", "false"))) {
                newSetting.setRenderer(AppSettings.LWJGL_OPENGL1);
            }
            if (!useCanvas) {
                newSetting.setCustomRenderer(AwtPanelsContext.class);
            }
            setSettings(newSetting);

            setPauseOnLostFocus(false);

            if (useCanvas) {
                createCanvas();
                startCanvas(true);
            }
            fakeApp = new FakeApplication(rootNode, guiNode, assetManager, cam);
            nodeSync = new NodeSyncAppState();
            stateManager.attach(nodeSync);
            progressHandle.progress("initialize Base Application", 1);
            if (!useCanvas) {
                start();
            }
        } catch (Exception e) {
            showStartupErrorMessage(e);
        } catch (Error e) {
            showStartupErrorMessage(e);
        } finally {
            getProgressHandle().finish();
        }
    }

    public Component getMainPanel() {
        if (useCanvas) {
            return ((JmeCanvasContext) getContext()).getCanvas();
        } else {
            if (panel == null) {
                panel = ((AwtPanelsContext) getContext()).createPanel(PaintMode.Accelerated);
                ((AwtPanelsContext) getContext()).setInputSource(panel);
                attachPanel();
            }
            return panel;
        }
    }

    private void attachPanel() {
        enqueue(new Callable() {
            public Object call() throws Exception {
                panel.attachTo(true, viewPort, overlayView, guiViewPort);
                return null;
            }
        });
    }

    public ViewPort getOverlayView() {
        return overlayView;
    }

    private void loadFPSText() {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        fpsText = new BitmapText(font, false);
        fpsText.setSize(font.getCharSet().getRenderedSize());
        fpsText.setLocalTranslation(0, fpsText.getLineHeight(), 0);
        fpsText.setText("Frames per second");
        statsGuiNode.attachChild(fpsText);
    }

    public void loadStatsView() {
        statsView = new StatsView("Statistics View", assetManager, renderer.getStatistics());
        // move it up so it appears above fps text
        statsView.setLocalTranslation(0, fpsText.getLineHeight(), 0);
        statsGuiNode.attachChild(statsView);
//        guiNode.attachChild(statsGuiNode);
    }

    @Override
    public void initialize() {
        if (!initFailed) {
            try {
                super.initialize();
                thread = Thread.currentThread();
                fakeApp.setAudioRenderer(audioRenderer);
                fakeApp.startFakeApp();                
                {
                    overlayView = getRenderManager().createMainView("Overlay", cam);
                    overlayView.setClearFlags(false, true, false);
                    guiViewPort.setClearFlags(false, false, false);
                }
                getProgressHandle().progress("Setup Camera Controller", 2);
                //create camera controller
                camController = new SceneCameraController(cam, inputManager);
                //create preview view
                getProgressHandle().progress("Setup Preview Scene", 3);

                previewProcessor = new ScenePreviewProcessor();
                previewProcessor.setupPreviewView();

                getProgressHandle().progress("Prepare Camera", 4);
                camLight = new PointLight();
                camLight.setColor(ColorRGBA.White);

                getProgressHandle().progress("Prepare Stats View", 5);
                guiNode.setQueueBucket(Bucket.Gui);
                guiNode.setCullHint(CullHint.Never);
                loadFPSText();
                loadStatsView();
                getProgressHandle().progress("Attach Scene to Viewport", 6);
                viewPort.attachScene(rootNode);
                viewPort.attachScene(toolsNode);
                guiViewPort.attachScene(guiNode);
                cam.setLocation(new Vector3f(0, 0, 10));

                getProgressHandle().progress("Create", 6);
                wireProcessor = new WireProcessor(assetManager);
                getProgressHandle().finish();

                inputManager.addMapping("MouseAxisX", new MouseAxisTrigger(MouseInput.AXIS_X, false));
                inputManager.addMapping("MouseAxisY", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
                inputManager.addMapping("MouseAxisX-", new MouseAxisTrigger(MouseInput.AXIS_X, true));
                inputManager.addMapping("MouseAxisY-", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
                inputManager.addMapping("MouseWheel", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
                inputManager.addMapping("MouseWheel-", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
                inputManager.addMapping("MouseButtonLeft", new MouseButtonTrigger(0));
                inputManager.addMapping("MouseButtonMiddle", new MouseButtonTrigger(2));
                inputManager.addMapping("MouseButtonRight", new MouseButtonTrigger(1));
                started = true;
            } catch (Throwable e) {
                showStartupErrorMessage(e);
                initFailed = true;
                if (fakeApp != null) {
                    fakeApp.stopFakeApp();
                }
            } finally {
                getProgressHandle().finish();
            }
        }
    }

    @Override
    public void destroy() {
        fakeApp.stopFakeApp();
        initFailed = false;
        super.destroy();
    }

    @Override
    public void update() {
        if (!started) {
            try {
                runQueuedTasks();
                getStateManager().update(0);
            } catch (Exception e) {
                logger.log(Level.INFO, "Exception calling Tasks:", e);
            }
        } else {
            try {
                super.update();
                FakeApplication fakap = fakeApp;
                if (fakap != null) {
                    fakap.runQueuedFake();
                }
                float tpf = timer.getTimePerFrame();
                camLight.setPosition(cam.getLocation());
                secondCounter += tpf;
                int fps = (int) timer.getFrameRate();
                if (secondCounter >= 1.0f) {
                    fpsText.setText("Frames per second: " + fps);
                    secondCounter = 0.0f;
                }
                getStateManager().update(tpf);
                toolsNode.updateLogicalState(tpf);
                if (fakap != null) {
                    fakap.updateFake(tpf);
                    fakap.updateExternalLogicalState(rootNode, tpf);
                    fakap.updateExternalLogicalState(guiNode, tpf);
                    fakap.updateExternalGeometricState(rootNode);
                    fakap.updateExternalGeometricState(guiNode);
                } else {
                    rootNode.updateLogicalState(tpf);
                    guiNode.updateLogicalState(tpf);
                    rootNode.updateGeometricState();
                    guiNode.updateGeometricState();
                }
                toolsNode.updateGeometricState();
                if (fakap != null) {
                    fakap.renderFake();
                }
                getStateManager().render(renderManager);
                renderManager.render(tpf, context.isRenderable());
                getStateManager().postRender();
            } catch (NullPointerException e) {
                handleError("NullPointerException: " + e.getMessage(), e);
            } catch (Exception e) {
                handleError(e.getMessage(), e);
            } catch (Error e) {
                handleError(e.getMessage(), e);
            }
        }
    }

    //TODO: Lookup for Application
    public Lookup createAdditionalLookup(Lookup baseContext) {
        return Lookups.fixed(getApplication());
    }

    //TODO: replace with Lookup functionality
    public void addSceneListener(SceneListener listener) {
        listeners.add(listener);
    }

    public void removeSceneListener(SceneListener listener) {
        listeners.remove(listener);
    }

    private void notifyOpen(final SceneRequest opened) {
        for (Iterator<SceneListener> it = listeners.iterator(); it.hasNext();) {
            SceneListener sceneViewerListener = it.next();
            sceneViewerListener.sceneOpened(opened);
        }
    }

    private void notifyClose(final SceneRequest closed) {
        for (Iterator<SceneListener> it = listeners.iterator(); it.hasNext();) {
            SceneListener sceneViewerListener = it.next();
            sceneViewerListener.sceneClosed(closed);
        }
    }

    public void notifyPreview(final PreviewRequest request) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                for (Iterator<SceneListener> it = listeners.iterator(); it.hasNext();) {
                    SceneListener sceneViewerListener = it.next();
                    sceneViewerListener.previewCreated(request);
                }
            }
        });
    }

    public void createPreview(final PreviewRequest request) {
        previewProcessor.addRequest(request);
    }

    /**
     * method to display the node tree of a plugin (threadsafe)
     *
     * @param request
     */
    public void openScene(final SceneRequest request) {
        if (!started) {
            NotifyUtil.show("OpenGL context not started!", "Click here to go to troubleshooting web page.", MessageType.WARNING, lst, 0);
            return;
        }
        closeScene(currentSceneRequest, request);
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (request == null) {
                    return;
                }
                currentSceneRequest = request;
                if (request.getDataNode() != null) {
                    setCurrentFileNode(request.getDataNode());
                } else {
                    setCurrentFileNode(null);
                }
                //TODO: handle this differently (no opened file)
                if (request.getRootNode() == null && request.getJmeNode() == null) {
                    DataObject dobj = request.getDataObject();
                    if (dobj != null) {
                        request.setJmeNode(NodeUtility.createNode(rootNode, dobj));
                    } else {
                        request.setJmeNode(NodeUtility.createNode(rootNode, false));
                    }
                }
                setHelpContext(request.getHelpCtx());
                setWindowTitle(request.getWindowTitle());
                if (request.getRequester() instanceof SceneApplication) {
                    camController.enable();
                } else {
                    camController.disable();
                }
                final AssetManager manager = request.getManager();
                request.setFakeApp(fakeApp);
                fakeApp.newAssetManager(manager);
                enqueue(new Callable() {
                    public Object call() throws Exception {
                        if (manager != null) {
                            assetManager = manager;
                        }
                        Spatial model = request.getRootNode();
                        //TODO: use FakeApp internal root node, don't handle model vs clean scene here
                        if (model != null && model != rootNode) {
                            rootNode.attachChild(model);
                        }
                        if (request.getToolNode() != null) {
                            toolsNode.attachChild(request.getToolNode());
                        }
                        request.setDisplayed(true);
                        return null;
                    }
                });
                notifyOpen(request);
            }
        });
    }

    /**
     * method to close a scene displayed by a scene request (threadsafe)
     *
     * @param request
     */
    public void closeScene(final SceneRequest request) {
        closeScene(request, null);
    }

    private void closeScene(final SceneRequest oldRequest, final SceneRequest newRequest) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (oldRequest == null) {
                    return;
                }
                notifyClose(oldRequest);
                if (newRequest == null || newRequest.getDataObject() != oldRequest.getDataObject()) {
                    checkSave(oldRequest);
                    SceneUndoRedoManager manager = Lookup.getDefault().lookup(SceneUndoRedoManager.class);
                    if (manager != null) {
                        manager.discardAllEdits();
                    }
                }
                if (newRequest == null) {
                    setCurrentFileNode(null);
                    setWindowTitle("OpenGL Window");
                    setHelpContext(null);
                }
                if (oldRequest.getRequester() instanceof SceneApplication) {
                    camController.disable();
                }
                enableCamLight(false);
                //TODO: state list is not thread safe..
                fakeApp.removeCurrentStates();
                enqueue(new Callable() {
                    public Object call() throws Exception {
                        if (physicsState != null) {
                            physicsState.getPhysicsSpace().removeAll(rootNode);
                            getStateManager().detach(physicsState);
                            physicsState = null;
                        }
                        //TODO: possibly dangerous (new var is created in EDT
                        if (fakeApp != null) {
                            fakeApp.cleanupFakeApp();
                        }
                        toolsNode.detachAllChildren();
                        rootNode.detachAllChildren();
                        // resetCam();
                        lastError = "";
                        oldRequest.setDisplayed(false);
                        return null;
                    }
                });
            }
        });
    }

    private void checkSave(SceneRequest request) {
        if ((request != null) && request.getDataObject() != null
                && request.getDataObject().isModified()) {
            final DataObject req = request.getDataObject();
            Confirmation mesg = new NotifyDescriptor.Confirmation("Scene has not been saved,\ndo you want to save it?",
                    "Not Saved",
                    NotifyDescriptor.YES_NO_OPTION);
            DialogDisplayer.getDefault().notify(mesg);
            if (mesg.getValue() == Confirmation.YES_OPTION) {
                try {
                    req.getLookup().lookup(AssetData.class).saveAsset();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            } else if (mesg.getValue() == Confirmation.CANCEL_OPTION) {
                return;
            } else if (mesg.getValue() == Confirmation.NO_OPTION) {
                req.setModified(false);
            }
        }
        if ((request != null) && (request.getDataObject() instanceof AssetDataObject)) {
            AssetDataObject obj = (AssetDataObject) request.getDataObject();
            obj.closeAsset();
        }
    }

    private void resetCam() {
        cam.setLocation(new Vector3f(0, 0, 10));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    private void setWindowTitle(final String string) {
        SceneViewerTopComponent.findInstance().setDisplayName(string);
    }

    private void setCurrentFileNode(final org.openide.nodes.Node node) {
        if (node == null) {
            SceneViewerTopComponent.findInstance().setActivatedNodes(new org.openide.nodes.Node[]{});
            SceneViewerTopComponent.findInstance().close();
        } else {
            SceneViewerTopComponent.findInstance().setActivatedNodes(new org.openide.nodes.Node[]{node});
            SceneViewerTopComponent.findInstance().open();
            SceneViewerTopComponent.findInstance().requestVisible();
        }
    }

    private void setHelpContext(final HelpCtx helpContext) {
        if (helpContext == null) {
            SceneViewerTopComponent.findInstance().setHelpContext(new HelpCtx("com.jme3.gde.core.sceneviewer"));
        } else {
            SceneViewerTopComponent.findInstance().setHelpContext(helpContext);
        }
    }

    public void enableCamLight(final boolean enabled) {
        enqueue(new Callable() {
            public Object call() throws Exception {
                if (enabled) {
                    rootNode.removeLight(camLight);
                    rootNode.addLight(camLight);
                } else {
                    rootNode.removeLight(camLight);
                }
                return null;
            }
        });
    }

    public void enableStats(final boolean enabled) {
        enqueue(new Callable() {
            public Object call() throws Exception {
                if (enabled) {
                    guiNode.attachChild(statsGuiNode);
                } else {
                    guiNode.detachChild(statsGuiNode);
                }
                return null;
            }
        });
    }

    public void enableWireFrame(final boolean selected) {
        enqueue(new Callable() {
            public Object call() throws Exception {
                if (selected) {
                    viewPort.addProcessor(wireProcessor);
                } else {
                    viewPort.removeProcessor(wireProcessor);
                }
                return null;
            }
        });
    }

    public void setPhysicsEnabled(final boolean enabled) {
        enqueue(new Callable() {
            public Object call() throws Exception {
                if (enabled) {
                    if (physicsState == null) {
                        physicsState = new BulletAppState();
                        getStateManager().attach(physicsState);
                        physicsState.getPhysicsSpace().addAll(rootNode);
                    }
                } else {
                    if (physicsState != null) {
                        physicsState.getPhysicsSpace().removeAll(rootNode);
                        getStateManager().detach(physicsState);
                        physicsState = null;
                    }
                }
                return null;
            }
        });
    }

    /**
     * @return the currentSceneRequest
     */
    public SceneRequest getCurrentSceneRequest() {
        return currentSceneRequest;
    }

    @Override
    public void handleError(String msg, Throwable t) {
        progressHandle.finish();
        if (msg == null) {
            msg = t.getMessage();
        }
        if (!started) {
            showStartupErrorMessage(t);
        } else if (lastError != null && !lastError.equals(msg)) {
            logger.log(Level.SEVERE, msg, t);
            lastError = msg;
        }
    }

    public static void showStartupErrorMessage(Throwable exception) {
        if (failMessageShown) {
            logger.log(Level.INFO, exception.getMessage(), exception);
            return;
        }
        failMessageShown = true;
        NotifyUtil.show("Error starting OpenGL context!", "Click here to go to troubleshooting web page.", MessageType.EXCEPTION, lst, 0);
        logger.log(Level.INFO, exception.getMessage(), exception);
    }
    private static ActionListener lst = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
                HtmlBrowser.URLDisplayer.getDefault().showURL(new URL("http://jmonkeyengine.org/wiki/doku.php/sdk:troubleshooting"));
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    };

    @Override
    public RenderManager getRenderManager() {
        return renderManager;
    }

    @Override
    public ViewPort getViewPort() {
        return viewPort;
    }

    @Override
    public ViewPort getGuiViewPort() {
        return guiViewPort;
    }

    public Node getGuiNode() {
        return guiNode;
    }

    public ProgressHandle getProgressHandle() {
        return progressHandle;
    }

    public AbstractCameraController getActiveCameraController() {
        return activeCamController;
    }

    public void setActiveCameraController(AbstractCameraController activeCamController) {
        this.activeCamController = activeCamController;
    }

    public boolean isOgl() {
        return Thread.currentThread() == thread;
    }

    public boolean isAwt() {
        return java.awt.EventQueue.isDispatchThread();
    }
}
