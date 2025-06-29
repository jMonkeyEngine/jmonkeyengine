/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.app;

import com.jme3.app.state.AppState;
import com.jme3.app.state.ConstantVerifierState;
import com.jme3.audio.AudioListenerState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.FlyByCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.profile.AppStep;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext.Type;
import com.jme3.system.JmeSystem;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * `SimpleApplication` is the foundational base class for all jMonkeyEngine 3 (jME3) applications.
 * It provides a streamlined setup for common game development tasks, including scene management,
 * camera controls, and performance monitoring.
 *
 * <p>By default, `SimpleApplication` attaches several essential {@link com.jme3.app.state.AppState} instances:
 * <ul>
 * <li>{@link com.jme3.app.StatsAppState}: Displays real-time frames-per-second (FPS) and
 * detailed performance statistics on-screen.</li>
 * <li>{@link com.jme3.app.FlyCamAppState}: Provides a convenient first-person fly-by camera
 * controller, allowing easy navigation within the scene.</li>
 * <li>{@link com.jme3.audio.AudioListenerState}: Manages the audio listener, essential for 3D sound.</li>
 * <li>{@link com.jme3.app.DebugKeysAppState}: Enables debug functionalities like displaying
 * camera position and memory usage in the console.</li>
 * <li>{@link com.jme3.app.state.ConstantVerifierState}: A utility state for verifying constant
 * values, primarily for internal engine debugging.</li>
 * </ul>
 *
 * <p><b>Default Key Bindings:</b></p>
 * <ul>
 * <li><b>Esc:</b> Closes and exits the application.</li>
 * <li><b>F5:</b> Toggles the visibility of the statistics view (FPS and debug stats).</li>
 * <li><b>C:</b> Prints the current camera position and rotation to the console.</li>
 * <li><b>M:</b> Prints memory usage statistics to the console.</li>
 * </ul>
 *
 * <p>Applications extending `SimpleApplication` should implement the
 * {@link #simpleInitApp()} method to set up their initial scene and game logic.
 */
public abstract class SimpleApplication extends LegacyApplication {

    protected static final Logger logger = Logger.getLogger(SimpleApplication.class.getName());

    public static final String INPUT_MAPPING_EXIT = "SIMPLEAPP_Exit";
    public static final String INPUT_MAPPING_CAMERA_POS = DebugKeysAppState.INPUT_MAPPING_CAMERA_POS;
    public static final String INPUT_MAPPING_MEMORY = DebugKeysAppState.INPUT_MAPPING_MEMORY;
    public static final String INPUT_MAPPING_HIDE_STATS = "SIMPLEAPP_HideStats";

    protected Node rootNode = new Node("Root Node");
    protected Node guiNode = new Node("Gui Node");
    protected BitmapText fpsText;
    protected BitmapFont guiFont;
    protected FlyByCamera flyCam;
    protected boolean showSettings = true;
    private final AppActionListener actionListener = new AppActionListener();

    private class AppActionListener implements ActionListener {

        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (!isPressed) {
                return;
            }

            if (name.equals(INPUT_MAPPING_EXIT)) {
                stop();
            } else if (name.equals(INPUT_MAPPING_HIDE_STATS)) {
                StatsAppState statsState = stateManager.getState(StatsAppState.class);
                if (statsState != null) {
                    statsState.toggleStats();
                }
            }
        }
    }

    /**
     * Constructs a `SimpleApplication` with a predefined set of default
     * {@link com.jme3.app.state.AppState} instances.
     * These states provide common functionalities like statistics display,
     * fly camera control, audio listener, debug keys, and constant verification.
     */
    public SimpleApplication() {
        this(new StatsAppState(),
                new FlyCamAppState(),
                new AudioListenerState(),
                new DebugKeysAppState(),
                new ConstantVerifierState());
    }

    /**
     * Constructs a `SimpleApplication` with a custom array of initial
     * {@link com.jme3.app.state.AppState} instances.
     *
     * @param initialStates An array of `AppState` instances to be attached
     * to the `stateManager` upon initialization.
     */
    public SimpleApplication(AppState... initialStates) {
        super(initialStates);
    }

    @Override
    public void start() {
        // set some default settings in-case
        // settings dialog is not shown
        boolean loadSettings = false;
        if (settings == null) {
            logger.log(Level.INFO, "AppSettings not set, creating default settings.");
            setSettings(new AppSettings(true));
            loadSettings = true;
        }

        // show settings dialog
        if (showSettings) {
            if (!JmeSystem.showSettingsDialog(settings, loadSettings)) {
                return;
            }
        }
        //re-setting settings they can have been merged from the registry.
        setSettings(settings);
        super.start();
    }

    /**
     * Returns the current speed multiplier of the application.
     * This value affects how quickly the game world updates relative to real time.
     * A value of 1.0f means normal speed, 0.5f means half speed, 2.0f means double speed.
     *
     * @return The current speed of the application.
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Changes the application's speed multiplier.
     * A `speed` of 0.0f effectively pauses the application's update cycle.
     *
     * @param speed The desired speed multiplier. A value of 1.0f is normal speed.
     * Must be non-negative.
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Retrieves the `FlyByCamera` instance associated with this application.
     * This camera allows free-form navigation within the 3D scene.
     *
     * @return The `FlyByCamera` object, or `null` if `FlyCamAppState` is not attached
     * or has not yet initialized the camera.
     */
    public FlyByCamera getFlyByCamera() {
        return flyCam;
    }

    /**
     * Retrieves the `Node` dedicated to 2D graphical user interface (GUI) elements.
     * Objects attached to this node are rendered on top of the 3D scene,
     * typically without perspective effects, suitable for HUDs and UI.
     *
     * @return The `Node` object representing the GUI root.
     */
    public Node getGuiNode() {
        return guiNode;
    }

    /**
     * Retrieves the root `Node` of the 3D scene graph.
     * All main 3D spatial objects and models should be attached to this node
     * to be part of the rendered scene.
     *
     * @return The `Node` object representing the 3D scene root.
     */
    public Node getRootNode() {
        return rootNode;
    }

    /**
     * Checks whether the settings dialog is configured to be shown at application startup.
     *
     * @return `true` if the settings dialog will be displayed, `false` otherwise.
     */
    public boolean isShowSettings() {
        return showSettings;
    }

    /**
     * Sets whether the jME3 settings dialog should be displayed before the application starts.
     *
     * @param showSettings `true` to show the settings dialog, `false` to suppress it.
     */
    public void setShowSettings(boolean showSettings) {
        this.showSettings = showSettings;
    }

    /**
     *  Creates the font that will be set to the guiFont field
     *  and subsequently set as the font for the stats text.
     *
     * @return the loaded BitmapFont
     */
    protected BitmapFont loadGuiFont() {
        return assetManager.loadFont("Interface/Fonts/Default.fnt");
    }

    @Override
    public void initialize() {
        super.initialize();

        // Load the default GUI font. This is essential for rendering text like FPS.
        guiFont = loadGuiFont();

        guiNode.setQueueBucket(Bucket.Gui);
        guiNode.setCullHint(CullHint.Never);

        viewPort.attachScene(rootNode);
        guiViewPort.attachScene(guiNode);

        if (inputManager != null) {
            // Special handling for FlyCamAppState:
            // Although FlyCamAppState manages the FlyByCamera, SimpleApplication
            // historically initializes and configures a default FlyByCamera instance
            // and sets its initial speed. This allows subclasses to directly access
            // 'flyCam' early in simpleInitApp().

            FlyCamAppState flyCamState = stateManager.getState(FlyCamAppState.class);
            if (flyCamState != null) {
                flyCam = new FlyByCamera(cam);
                flyCam.setMoveSpeed(1f); // Set a default movement speed for the camera
                flyCamState.setCamera(flyCam); // Link the FlyCamAppState to this camera instance
            }

            // Register the "Exit" input mapping for the Escape key, but only for Display contexts.
            if (context.getType() == Type.Display) {
                inputManager.addMapping(INPUT_MAPPING_EXIT, new KeyTrigger(KeyInput.KEY_ESCAPE));
            }

            // Register the "Hide Stats" input mapping for the F5 key, if StatsAppState is active.
            StatsAppState statsState = stateManager.getState(StatsAppState.class);
            if (statsState != null) {
                inputManager.addMapping(INPUT_MAPPING_HIDE_STATS, new KeyTrigger(KeyInput.KEY_F5));
                inputManager.addListener(actionListener, INPUT_MAPPING_HIDE_STATS);
            }

            // Attach the action listener to the "Exit" mapping.
            inputManager.addListener(actionListener, INPUT_MAPPING_EXIT);
        }

        // Configure the StatsAppState if it exists.
        StatsAppState statsState = stateManager.getState(StatsAppState.class);
        if (statsState != null) {
            statsState.setFont(guiFont);
            fpsText = statsState.getFpsText();
        }

        // Call the user's application initialization code.
        simpleInitApp();
    }

    @Override
    public void update() {
        if (prof != null) {
            prof.appStep(AppStep.BeginFrame);
        }

        // Executes AppTasks from the main thread
        super.update();

        // Skip updates if paused or speed is zero
        if (speed == 0 || paused) {
            return;
        }

        float tpf = timer.getTimePerFrame() * speed;

        // Update AppStates
        if (prof != null) {
            prof.appStep(AppStep.StateManagerUpdate);
        }
        stateManager.update(tpf);

        // Call user's per-frame update method
        simpleUpdate(tpf);

        // Update scene graph nodes (logical and geometric states)
        if (prof != null) {
            prof.appStep(AppStep.SpatialUpdate);
        }
        rootNode.updateLogicalState(tpf);
        guiNode.updateLogicalState(tpf);

        rootNode.updateGeometricState();
        guiNode.updateGeometricState();

        // Render AppStates and the scene
        if (prof != null) {
            prof.appStep(AppStep.StateManagerRender);
        }
        stateManager.render(renderManager);

        if (prof != null) {
            prof.appStep(AppStep.RenderFrame);
        }
        renderManager.render(tpf, context.isRenderable());
        // Call user's custom render method
        simpleRender(renderManager);
        stateManager.postRender();

        if (prof != null) {
            prof.appStep(AppStep.EndFrame);
        }
    }

    /**
     * Controls the visibility of the frames-per-second (FPS) display on the screen.
     *
     * @param show `true` to display the FPS, `false` to hide it.
     */
    public void setDisplayFps(boolean show) {
        StatsAppState statsState = stateManager.getState(StatsAppState.class);
        if (statsState != null) {
            statsState.setDisplayFps(show);
        }
    }

    /**
     * Controls the visibility of the comprehensive statistics view on the screen.
     * This view typically includes details about memory, triangles, and other performance metrics.
     *
     * @param show `true` to display the statistics view, `false` to hide it.
     */
    public void setDisplayStatView(boolean show) {
        StatsAppState statsState = stateManager.getState(StatsAppState.class);
        if (statsState != null) {
            statsState.setDisplayStatView(show);
        }
    }

    public abstract void simpleInitApp();

    /**
     * An optional method that can be overridden by subclasses for per-frame update logic.
     * This method is called during the application's update loop, after AppStates are updated
     * and before the scene graph's logical state is updated.
     *
     * @param tpf The time per frame (in seconds), adjusted by the application's speed.
     */
    public void simpleUpdate(float tpf) {
        // Default empty implementation; subclasses can override
    }

    /**
     * An optional method that can be overridden by subclasses for custom rendering logic.
     * This method is called during the application's render loop, after the main scene
     * has been rendered and before post-rendering for states.
     * Useful for drawing overlays or specific rendering tasks outside the main scene graph.
     *
     * @param rm The `RenderManager` instance, which provides access to rendering functionalities.
     */
    public void simpleRender(RenderManager rm) {
        // Default empty implementation; subclasses can override
    }
}
