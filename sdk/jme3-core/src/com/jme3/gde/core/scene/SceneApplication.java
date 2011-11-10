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
import com.jme3.bullet.BulletAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.gde.core.Installer;
import com.jme3.gde.core.assets.AssetData;
import com.jme3.gde.core.scene.controller.AbstractCameraController;
import com.jme3.gde.core.scene.processors.WireProcessor;
import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import com.jme3.gde.core.undoredo.SceneUndoRedoManager;
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
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.spi.project.LookupProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;
import org.openide.NotifyDescriptor.Message;
import org.openide.awt.StatusDisplayer;
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

    private PointLight camLight;
    private static SceneApplication application;

    public static SceneApplication getApplication() {
        if (application == null) {
            application = new SceneApplication();
        }
        return application;
    }
    protected Node rootNode = new Node("Root Node");
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
    private AwtPanel panel;
    private ViewPort overlayView;
    boolean useCanvas = false;
    private BulletAppState physicsState;
    private Thread thread;

    public SceneApplication() {
        progressHandle.start(7);
        useCanvas = "true".equals(NbPreferences.forModule(Installer.class).get("use_lwjgl_canvas", "false"));
        try {
            AppSettings newSetting = new AppSettings(true);
            newSetting.setFrameRate(30);
            if ("true".equals(NbPreferences.forModule(Installer.class).get("use_opengl_1", "false"))) {
                newSetting.setRenderer(AppSettings.LWJGL_OPENGL_ANY);
            }
            if (!useCanvas) {
                newSetting.setCustomRenderer(AwtPanelsContext.class);
            }
            setSettings(newSetting);

            Logger.getLogger("com.jme3").addHandler(logHandler);

            setPauseOnLostFocus(false);

            if (useCanvas) {
                createCanvas();
                startCanvas(true);
            }
            progressHandle.progress("initialize Base Application", 1);
        } catch (Exception e) {
            getProgressHandle().finish();
            SceneViewerTopComponent.showOpenGLError(e.toString());
            Exceptions.printStackTrace(e);
        } catch (Error e) {
            getProgressHandle().finish();
            SceneViewerTopComponent.showOpenGLError(e.toString());
            Exceptions.printStackTrace(e);
        }
        if (!useCanvas) {
            start();
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
        thread = Thread.currentThread();
        try {
            super.initialize();
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
        } catch (Exception e) {
            getProgressHandle().finish();
            Exceptions.printStackTrace(e);
            SceneViewerTopComponent.showOpenGLError(e.toString());
        } catch (Error e) {
            getProgressHandle().finish();
            Exceptions.printStackTrace(e);
            SceneViewerTopComponent.showOpenGLError(e.toString());
        }
    }

    @Override
    public void update() {
        if (speed == 0) {
            return;
        }
        try {
            super.update();
            float tpf = timer.getTimePerFrame();
            camLight.setPosition(cam.getLocation());
            secondCounter += tpf;
            int fps = (int) timer.getFrameRate();
            if (secondCounter >= 1.0f) {
                fpsText.setText("Frames per second: " + fps);
                secondCounter = 0.0f;
            }
            getStateManager().update(tpf);
            rootNode.updateLogicalState(tpf);
            guiNode.updateLogicalState(tpf);
            toolsNode.updateLogicalState(tpf);
            rootNode.updateGeometricState();
            guiNode.updateGeometricState();
            toolsNode.updateGeometricState();
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
     * @param request
     */
    public void openScene(final SceneRequest request) {
        closeScene(currentSceneRequest, request);
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                if (request == null) {
                    return;
                }
                currentSceneRequest = request;
                if (request.getDataObject() != null) {
                    setCurrentFileNode(request.getDataObject().getNodeDelegate());
                } else {
                    setCurrentFileNode(null);
                }
                setHelpContext(request.getHelpCtx());
                setWindowTitle(request.getWindowTitle());
                enqueue(new Callable() {

                    public Object call() throws Exception {
                        if (request.getManager() != null) {
                            assetManager = request.getManager();
                        }
                        if (request.getRequester() instanceof SceneApplication) {
                            camController.enable();
                        } else {
                            camController.disable();
                        }
                        Spatial model = request.getRootNode();
                        if (model == null) {
                            StatusDisplayer.getDefault().setStatusText("could not load Spatial from request: " + getCurrentSceneRequest().getWindowTitle());
                            return null;
                        }
                        rootNode.attachChild(model);
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
                if (newRequest == null || newRequest.getRootNode() != oldRequest.getRootNode()) {
                    checkSave(oldRequest);
                    SceneUndoRedoManager manager = Lookup.getDefault().lookup(SceneUndoRedoManager.class);
                    if (manager != null) {
                        manager.discardAllEdits();
                    }
                }
                setCurrentFileNode(null);
                setWindowTitle("OpenGL Window");
                setHelpContext(null);
                notifyClose(oldRequest);
                enqueue(new Callable() {

                    public Object call() throws Exception {
                        if (oldRequest.getRequester() instanceof SceneApplication) {
                            camController.disable();
                        }
                        if (physicsState != null) {
                            physicsState.getPhysicsSpace().removeAll(rootNode);
                            getStateManager().detach(physicsState);
                            physicsState = null;
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
        if ((request != null)
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
            return;
        }
        if (!started) {
            SceneViewerTopComponent.showOpenGLError(msg);
            Exceptions.printStackTrace(t);
        } else {
            if (lastError != null && !lastError.equals(msg)) {
                Message mesg = new NotifyDescriptor.Message(
                        "Error in scene!\n"
                        + "(" + t + ")",
                        NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notifyLater(mesg);
                Exceptions.printStackTrace(t);
                lastError = msg;
            }
        }
    }

    public RenderManager getRenderManager() {
        return renderManager;
    }

    public ViewPort getViewPort() {
        return viewPort;
    }

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
