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

package com.jme3.app;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.system.JmeSystem;
import java.applet.Applet;
import java.awt.Canvas;
import java.awt.Graphics;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * @author Kirill Vainer
 */
public class AppletHarness extends Applet {

    public static final HashMap<Application, Applet> appToApplet
                         = new HashMap<Application, Applet>();

    private JmeCanvasContext context;
    private Canvas canvas;
    private Application app;

    private String appClass;
    private URL appCfg = null;
    private URL assetCfg = null;

    public static Applet getApplet(Application app){
        return appToApplet.get(app);
    }

    private void createCanvas(){
        AppSettings settings = new AppSettings(true);

        // load app cfg
        if (appCfg != null){
            InputStream in = null;
            try {
                in = appCfg.openStream();
                settings.load(in);
                in.close();
            } catch (IOException ex){
                // Called before application has been created ....
                // Display error message through AWT
                JOptionPane.showMessageDialog(this, "An error has occured while "
                                                  + "loading applet configuration"
                                                  + ex.getMessage(),
                                              "jME3 Applet",
                                              JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } finally {
                if (in != null)
                    try {
                    in.close();
                } catch (IOException ex) {
                }
            }
        }

        if (assetCfg != null){
            settings.putString("AssetConfigURL", assetCfg.toString());
        }

        settings.setWidth(getWidth());
        settings.setHeight(getHeight());

        JmeSystem.setLowPermissions(true);

        try{
            Class<? extends Application> clazz = (Class<? extends Application>) Class.forName(appClass);
            app = clazz.newInstance();
        }catch (ClassNotFoundException ex){
            ex.printStackTrace();
        }catch (InstantiationException ex){
            ex.printStackTrace();
        }catch (IllegalAccessException ex){
            ex.printStackTrace();
        }

        appToApplet.put(app, this);
        app.setSettings(settings);
        app.createCanvas();

        context = (JmeCanvasContext) app.getContext();
        canvas = context.getCanvas();
        canvas.setSize(getWidth(), getHeight());

        add(canvas);
        app.startCanvas();
    }

    @Override
    public final void update(Graphics g) {
        canvas.setSize(getWidth(), getHeight());
    }

    @Override
    public void init(){
        appClass = getParameter("AppClass");
        if (appClass == null)
            throw new RuntimeException("The required parameter AppClass isn't specified!");

        try {
            appCfg = new URL(getParameter("AppSettingsURL"));
        } catch (MalformedURLException ex) {
            System.out.println(ex.getMessage());
            appCfg = null;
        }

        try {
            assetCfg = new URL(getParameter("AssetConfigURL"));
        } catch (MalformedURLException ex){
            System.out.println(ex.getMessage());
            assetCfg = getClass().getResource("/com/jme3/asset/Desktop.cfg");
        }

        createCanvas();
        System.out.println("applet:init");
    }

    @Override
    public void start(){
        context.setAutoFlushFrames(true);
        System.out.println("applet:start");
    }

    @Override
    public void stop(){
        context.setAutoFlushFrames(false);
        System.out.println("applet:stop");
    }

    @Override
    public void destroy(){
        System.out.println("applet:destroyStart");
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                removeAll();
                System.out.println("applet:destroyRemoved");
            }
        });
        app.stop(true);
        System.out.println("applet:destroyDone");

        appToApplet.remove(app);
    }

}
