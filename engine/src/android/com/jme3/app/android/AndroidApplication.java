/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.app.android;

import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.jme3.app.Application;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.android.AndroidInput;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.util.FastInteger;
import com.jme3.app.AndroidHarness;


/**
 * <code>AndroidApplication</code> extends the {@link com.jme3.app.Application}
 * class to provide default functionality like a first-person camera,
 * and an accessible root node that is updated and rendered regularly.
 * It will display the current frames-per-second value on-screen.
 * 
 * @deprecated Please use {@link AndroidHarness} instead.
 */
 @Deprecated
public abstract class AndroidApplication extends Application implements DialogInterface.OnClickListener 
{
    protected final static Logger logger = Logger.getLogger(AndroidApplication.class.getName());

    protected Node rootNode = new Node("Root Node");
    protected Node guiNode = new Node("Gui Node");
    protected float secondCounter = 0.0f;
    protected BitmapText fpsText;
    protected CharBuffer textBuf = CharBuffer.allocate(50);
    protected char[] fpsBuf = new char[16];
    protected BitmapFont guiFont;
    
    protected Activity activity;
    protected AndroidInput input;
    protected final AtomicBoolean loadingFinished;
    
    public AndroidApplication() 
    {
        this(null, null);
    }

    public AndroidApplication(Activity activity, AndroidInput input) 
    {
        super();
        this.activity = activity;
        this.input = input;
        
        loadingFinished = new AtomicBoolean(false);
    }

    @Override
    public void start() 
    {
        // Set the correct xml parser driver for android
        System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver");
        
        if (settings == null) 
        {
            settings = new AppSettings(true);
        }

        // Use vertex arrays for rendering
        settings.putBoolean("USE_VA", true);
        // Verbose logging off
        settings.putBoolean("VERBOSE_LOGGING", false);
       
        //re-setting settings they can have been merged from the registry.
        setSettings(settings);
        super.start();
    }


    /**
     * Retrieves guiNode
     * @return guiNode Node object
     *
     */
    public Node getGuiNode() {
        return guiNode;
    }

    /**
     * Retrieves rootNode
     * @return rootNode Node object
     *
     */
    public Node getRootNode() {
        return rootNode;
    }


    /**
     * Attaches FPS statistics to guiNode and displays it on the screen.
     *
     */
    public void loadFPSText() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        fpsText = new BitmapText(guiFont, false);
        fpsText.setLocalTranslation(0, fpsText.getLineHeight(), 0);
        fpsText.setText("Frames per second");
        guiNode.attachChild(fpsText);
    }

    @Override
    public void initialize() 
    {
        // Create a default Android assetmanager before Application can create one in super.initialize();
        assetManager = JmeSystem.newAssetManager(null);        
        super.initialize();

        guiNode.setQueueBucket(Bucket.Gui);
        guiNode.setCullHint(CullHint.Never);
        loadFPSText();
        viewPort.attachScene(rootNode);
        guiViewPort.attachScene(guiNode);

        // call user code
        init();
        
        // Start thread for async load
        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run ()
            {
                try
                {
                    // call user code
                    asyncload();
                }
                catch (Exception e)
                {
                    handleError("AsyncLoad failed", e);
                }
                loadingFinished.set(true);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void update() {
        super.update(); // makes sure to execute AppTasks
        if (speed == 0 || paused) {
            return;
        }

        float tpf = timer.getTimePerFrame() * speed;

        secondCounter += timer.getTimePerFrame();
        int fps = (int) timer.getFrameRate();
        if (secondCounter >= 5.0f) {
            textBuf.clear();
            textBuf.put("Frames per second: ");            
            FastInteger.toCharArray(fps, fpsBuf);
            textBuf.put(fpsBuf);
            textBuf.flip();
            fpsText.setText(textBuf);
            secondCounter = 0.0f;
        }

        // update states
        stateManager.update(tpf);

        // simple update and root node
        update(tpf);
        rootNode.updateLogicalState(tpf);
        guiNode.updateLogicalState(tpf);
        rootNode.updateGeometricState();
        guiNode.updateGeometricState();

        // render states
        stateManager.render(renderManager);
        renderManager.render(tpf);
        render(renderManager);
        stateManager.postRender();
    }

    public abstract void init();

    public void update(float tpf) {
    }

    public void render(RenderManager rm) {
    }
    
    
    /**
     * Gets called by a different thread to allow
     * async loading of assets.
     * 
     * This means that update and rendering can already
     * happen while some assets are still loading.
     */
    public void asyncload()
    {
        
    }
    
    /**
     * Called when an error has occured. This is typically
     * invoked when an uncought exception is thrown in the render thread.
     * @param errorMsg The error message, if any, or null.
     * @param t Throwable object, or null.
     */
    @Override
    public void handleError(final String errorMsg, final Throwable t)
    {
        
        String s = "";
        if (t != null && t.getStackTrace() != null)
        {
            for (StackTraceElement ste: t.getStackTrace())
            {
                s +=  ste.getClassName() + "." + ste.getMethodName() + "(" + + ste.getLineNumber() + ") ";
            }
        }
        final String sTrace = s;
        
        logger.severe(t != null ? t.toString() : "Failed");
        logger.severe((errorMsg != null ? errorMsg + ": " : "") + sTrace);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() 
            {                                
                AlertDialog dialog = new AlertDialog.Builder(activity)
               // .setIcon(R.drawable.alert_dialog_icon)
                .setTitle(t != null ? t.toString() : "Failed")
                .setPositiveButton("Kill", AndroidApplication.this)
                .setMessage((errorMsg != null ? errorMsg + ": " : "") + sTrace)
                .create();    
                dialog.show();                
            }
        });
        
    }

    
    /**
     * Called by the android alert dialog, terminate the activity and OpenGL rendering
     * @param dialog
     * @param whichButton
     */
    public void onClick(DialogInterface dialog, int whichButton) 
    {
        this.stop();
        activity.finish();
    }

}
