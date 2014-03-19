/*
 * Copyright (c) 2003-2012 jMonkeyEngine
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.scene;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioContext;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.Listener;
import com.jme3.gde.core.appstates.AppStateManagerNode;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.Control;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.Timer;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;

/**
 * TODO: Temporary implementation before new scene system!
 *
 * @author normenhansen
 */
public class FakeApplication extends SimpleApplication {
//    private Lookup lookup;

    private Node rootNode;
    private Node guiNode;
    private AssetManager assetManager;
    private Camera cam;
    private FakeAppStateManager appStateManager;
    private FakeRenderManager renderManager;

    public FakeApplication(Node guiNode, AssetManager assetManager, Camera cam) {
        this.guiNode = guiNode;
        this.assetManager = assetManager;
        this.cam = cam;
        this.appStateManager = new FakeAppStateManager(this);
    }

    public FakeApplication(Node rootNode, Node guiNode, AssetManager assetManager, Camera cam) {
        this.rootNode = rootNode;
        this.guiNode = guiNode;
        this.assetManager = assetManager;
        this.cam = cam;
        this.appStateManager = new FakeAppStateManager(this);
    }
    
    public void setAudioRenderer(AudioRenderer audioRenderer){
        this.audioRenderer = audioRenderer;
    }

    @Override
    public void createCanvas() {
        defaultFakeError(true);
    }

    @Override
    public void destroy() {
        defaultFakeError(true);
    }

    @Override
    protected void destroyInput() {
        defaultFakeError(true);
    }

    @Override
    public <V> Future<V> enqueue(Callable<V> callable) {
        return super.enqueue(callable);
    }

    @Override
    public void gainFocus() {
        defaultFakeError(true);
    }

    @Override
    public AssetManager getAssetManager() {
        return assetManager;
    }

    @Override
    public AudioRenderer getAudioRenderer() {
        defaultFakeError();
        return null;
    }

    @Override
    public Camera getCamera() {
        return cam;
    }

    @Override
    public JmeContext getContext() {
        defaultFakeError();
        return null;
    }

    @Override
    public ViewPort getGuiViewPort() {
        //TODO
        defaultFakeError();
        return null;
    }

    @Override
    public InputManager getInputManager() {
        defaultFakeError();
        return null;
    }

    @Override
    public Listener getListener() {
        defaultFakeError();
        return null;
    }

    @Override
    public RenderManager getRenderManager() {
        defaultFakeError();
        return null;
    }

    @Override
    public Renderer getRenderer() {
        defaultFakeError();
        return null;
    }

    @Override
    public FakeAppStateManager getStateManager() {
        return appStateManager;
    }

    @Override
    public Timer getTimer() {
        defaultFakeError();
        return null;
    }

    @Override
    public ViewPort getViewPort() {
        //TODO
        defaultFakeError();
        return null;
    }

    @Override
    public boolean isPauseOnLostFocus() {
        return true;
    }

    @Override
    public void loseFocus() {
        defaultFakeError(true);
    }

    @Override
    public void initialize() {
        defaultFakeError(true);
    }

    @Override
    public void handleError(String errMsg, Throwable t) {
        defaultFakeError(true);
    }

    @Override
    public void requestClose(boolean esc) {
        defaultFakeError();
    }

    @Override
    public void reshape(int w, int h) {
        defaultFakeError();
    }

    @Override
    public void restart() {
        defaultFakeError();
    }

    @Override
    public void setAssetManager(AssetManager assetManager) {
        defaultFakeError(true);
    }

    @Override
    public void setSettings(AppSettings settings) {
        defaultFakeError();
    }

    @Override
    public void setPauseOnLostFocus(boolean pauseOnLostFocus) {
        defaultFakeError();
    }

    @Override
    public void start() {
        defaultFakeError();
    }

    @Override
    public void start(Type contextType) {
        defaultFakeError();
    }

    @Override
    public void startCanvas() {
        defaultFakeError();
    }

    @Override
    public void startCanvas(boolean waitFor) {
        defaultFakeError();
    }

    @Override
    public void stop() {
        defaultFakeError();
    }

    @Override
    public void stop(boolean waitFor) {
        defaultFakeError();
    }

    @Override
    public void update() {
        defaultFakeError(true);
    }

    /*
     * SimpleApplication
     */
    @Override
    public void simpleInitApp() {
        defaultFakeError(true);
    }

    @Override
    public FlyByCamera getFlyByCamera() {
        defaultFakeError();
        return null;
    }

    @Override
    public Node getGuiNode() {
        return guiNode;
    }

    @Override
    public Node getRootNode() {
        return rootNode;
    }

    @Override
    public boolean isShowSettings() {
        return true;
    }

    @Override
    public void setDisplayFps(boolean show) {
        defaultFakeError();
    }

    @Override
    public void setDisplayStatView(boolean show) {
        defaultFakeError();
    }

    @Override
    public void setTimer(Timer timer) {
        defaultFakeError();
    }

    @Override
    public void setShowSettings(boolean showSettings) {
        defaultFakeError();
    }

    @Override
    public void simpleRender(RenderManager rm) {
        defaultFakeError();
    }

    @Override
    public void simpleUpdate(float tpf) {
        defaultFakeError();
    }

    private class FakeRenderManager extends RenderManager {

        public FakeRenderManager(Renderer renderer) {
            super(null);
        }
        //TODO: also nice messages
    }

    public static class FakeAppStateManager extends AppStateManager {

        private AppStateManagerNode node;
        ArrayList<AppState> states = new ArrayList<AppState>();

        public FakeAppStateManager(Application app) {
            super(app);
        }

        public List<AppState> getAddedStates() {
            return states;
        }

        //TODO: make thread safe
        @Override
        public boolean attach(AppState state) {
            boolean ret = super.attach(state);
            if (ret) {
                states.add(state);
            }
            if (node != null) {
                node.refresh();
            }
            return ret;
        }

        @Override
        public boolean detach(AppState state) {
            boolean ret = super.detach(state);
            if (ret) {
                states.remove(state);
            }
            if (node != null) {
                node.refresh();
            }
            return ret;
        }

        public void setNode(AppStateManagerNode node) {
            this.node = node;
        }
    }
    /*
     * Internal
     */
    private ScheduledThreadPoolExecutor fakeAppThread = new ScheduledThreadPoolExecutor(1);

    public void removeCurrentStates() {
        for (Iterator<AppState> it = new ArrayList(appStateManager.getAddedStates()).iterator(); it.hasNext();) {
            AppState appState = it.next();
            try {
                appStateManager.detach(appState);
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            }
        }
    }

    public void cleanupFakeApp() {
        runQueuedFake();
        if (guiNode != null) {
            clearNode(guiNode);
        }
        if (rootNode != null) {
            clearNode(rootNode);
        }
    }

    public void startFakeApp() {
        fakeAppThread = new ScheduledThreadPoolExecutor(1);
    }

    public void stopFakeApp() {
        cleanupFakeApp();
        fakeAppThread.shutdown();
    }

    public void newAssetManager(AssetManager manager) {
        this.assetManager = manager;
    }

    private void defaultFakeError() {
        defaultFakeError(false);
    }

    private void defaultFakeError(boolean severe) {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        new Throwable().printStackTrace(new PrintStream(new BufferedOutputStream(str)));
        String trace = "No stack trace available.";
        try {
            trace = str.toString("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Exceptions.printStackTrace(ex);
        }
        int idx = trace.indexOf("com.jme3.gde");
        while (idx != -1) {
            if (idx < 50) {
                continue;
            } else {
                trace = trace.substring(0, idx);
                break;
            }
        }
        showError("Fake app is fake!" + (severe ? "\nAnd WTF are you trying to do anyway?\n" : "\n") + trace);
    }

    private void showError(String msg) {
        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(
                msg,
                NotifyDescriptor.WARNING_MESSAGE));
    }

    public boolean runQueuedFake() {
        Future fut = fakeAppThread.submit(new Callable<Void>() {
            public Void call() throws Exception {
                runQueuedTasks();
                return null;
            }
        });
        try {
            fut.get(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Exception in queued Tasks."));
            return false;
        } catch (TimeoutException ex) {
            fut.cancel(true);
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Update loop was blocked for too long, task execution halted."));
            return false;
        }
        return true;
    }

    public boolean updateFake(final float tpf) {
        Future fut = fakeAppThread.submit(new Callable<Void>() {
            public Void call() throws Exception {
                AudioContext.setAudioRenderer(audioRenderer);
                appStateManager.update(tpf);
                return null;
            }
        });
        try {
            fut.get(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            removeAllStates();
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Exception in AppState, all AppStates removed."));
            return false;
        } catch (TimeoutException ex) {
            fut.cancel(true);
            removeAllStates();
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Update loop was blocked for too long, all AppStates removed."));
            return false;
        }
        return true;
    }

    public boolean renderFake() {
        Future fut = fakeAppThread.submit(new Callable<Void>() {
            public Void call() throws Exception {
                appStateManager.render(renderManager);
                return null;
            }
        });
        try {
            fut.get(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            removeAllStates();
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Exception in AppState, all AppStates removed."));
            return false;
        } catch (TimeoutException ex) {
            fut.cancel(true);
            removeAllStates();
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Render loop was blocked for too long, all AppStates removed."));
            return false;
        }
        return true;
    }

    public boolean updateExternalLogicalState(final Node externalNode, final float tpf) {
        Future fut = fakeAppThread.submit(new Callable<Void>() {
            public Void call() throws Exception {               
                externalNode.updateLogicalState(tpf);
                return null;
            }
        });
        try {
            fut.get(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            clearNode(externalNode);
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Exception in Control, scene content removed.\n" + ex.getMessage()));
            return false;
        } catch (TimeoutException ex) {
            fut.cancel(true);
            clearNode(externalNode);
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Render loop was blocked for too long, scene content removed."));
            return false;
        }
        return true;
    }

    public boolean updateExternalGeometricState(final Node externalNode) {
        Future fut = fakeAppThread.submit(new Callable<Void>() {
            public Void call() throws Exception {
                externalNode.updateGeometricState();
                return null;
            }
        });
        try {
            fut.get(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            clearNode(externalNode);
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Exception in Control, scene content removed.\n" + ex.getMessage()));
            return false;
        } catch (TimeoutException ex) {
            fut.cancel(true);
            clearNode(externalNode);
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Render loop was blocked for too long, scene content removed."));
            return false;
        }
        return true;
    }

    public Class getClassByName(String className) {
        Class clazz = null;
        try {
            clazz = getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException ex) {
        }
        for (ClassLoader classLoader : assetManager.getClassLoaders()) {
            if (clazz == null) {
                try {
                    clazz = classLoader.loadClass(className);
                } catch (ClassNotFoundException ex) {
                }
            }
        }
        return clazz;
    }

    private void removeAllStates() {
        try {
            try {
                for (Iterator<AppState> it = new ArrayList(appStateManager.getAddedStates()).iterator(); it.hasNext();) {
                    AppState appState = it.next();
                    appStateManager.detach(appState);
                }
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            }
            AppState state = appStateManager.getState(AppState.class);
            while (state != null) {
                try {
                    appStateManager.update(0);
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                }
                state = appStateManager.getState(AppState.class);
            }
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
    }

    private void clearNode(final Node externalNode) {
        while (!externalNode.getChildren().isEmpty()) {
            try {
                externalNode.detachAllChildren();
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            } catch (Error e) {
                Exceptions.printStackTrace(e);
            }
        }
        Control control = externalNode.getControl(Control.class);
        while (control != null) {
            try {
                externalNode.removeControl(control);
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            } catch (Error e) {
                Exceptions.printStackTrace(e);
            }
            control = externalNode.getControl(Control.class);
        }
        Collection<String> keys = externalNode.getUserDataKeys();
        if (keys != null) {
            for (Iterator<String> it = keys.iterator(); it.hasNext();) {
                String string = it.next();
                externalNode.setUserData(string, null);
            }
        }
        LightList llist = null;
        try {
            llist = externalNode.getLocalLightList();
            for (Iterator<Light> it = llist.iterator(); it.hasNext();) {
                Light light = it.next();
                try {
                    externalNode.removeLight(light);
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                } catch (Error e) {
                    Exceptions.printStackTrace(e);
                }
            }
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        } catch (Error e) {
            Exceptions.printStackTrace(e);
        }
    }
}
