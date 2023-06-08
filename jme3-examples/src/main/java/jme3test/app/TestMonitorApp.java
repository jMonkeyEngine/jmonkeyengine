/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package jme3test.app;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.MonitorInfo;
import com.jme3.system.Monitors;

/**
 * Tests the capability to change which monitor the window
 * will be created on.  Also,  shows that you can force
 * JME to center the window. Also, it shows to to force JME to
 * set the window to x,y coords.  Center window and window 
 * position doesn't apply if in fullscreen.
 * 
 * @author Kevin Bales
 */
public class TestMonitorApp extends SimpleApplication implements ActionListener {
    
    private BitmapText txt;
    private BitmapText selectedMonitorTxt;
    private BitmapText fullScreenTxt;
    private int monitorSelected = 0;
    private Monitors monitors = null;
    
    public static void main(String[] args){
        TestMonitorApp app = new TestMonitorApp();
        AppSettings settings = new AppSettings(true);
        settings.setResizable(false);
        app.setShowSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        settings.setMonitor(0);
        settings.setResolution(800, 600);
        
        settings.setFullscreen(true);

        //Force JME to center the window, this only applies if it is
        //not fullscreen.
        settings.setCenterWindow(true);

        //If center window is not turned on, you can force JME to
        //open the window at certain x,y coords.  These are ignored
        //if the screen is set to "fullscreen".
        settings.setWindowXPosition(0);
        settings.setWindowYPosition(0);
        
        try
        {
           //Let's try and load the AppSetting parameters back into memory
            InputStream out = new FileInputStream("TestMonitorApp.prefs");
            settings.load(out);
        }
        catch (IOException e)
        {
            System.out.println("failed to load settings, reverting to defaults");
        }
        app.setSettings(settings);

        app.start();
    }

    
    @Override
    public void simpleInitApp() {
        flyCam.setDragToRotate(true);
        int numMonitors = 1;

        //If monitor is define, Jme supports multiple monitors. Setup to keys
        if (monitors == null) {
           inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_DOWN));
           inputManager.addMapping("fullscreen", new KeyTrigger(KeyInput.KEY_F));
           inputManager.addListener(this, "down", "fullscreen");
        }

        //Get the selected monitor
        monitorSelected = settings.getMonitor();
        monitors  = context.getMonitors();
        if (monitors != null)
           numMonitors = monitors.size();
        


        //Let's define the labels for users to see what is going on with Multiple Monitor
        String labelValue = "";
        labelValue = "There are "+numMonitors+" monitor(s) hooked up to this computer.";
        txt = new BitmapText(loadGuiFont());
        txt.setText(labelValue);
        txt.setLocalTranslation(0, settings.getHeight(), 0);
        guiNode.attachChild(txt);

        txt = new BitmapText(loadGuiFont());
        if (!settings.isFullscreen())
           txt.setText("Window is on Monitor N/A (fullscreen only feature)");
        else
           txt.setText("Window is on Monitor "+settings.getMonitor());
           
        txt.setLocalTranslation(0, settings.getHeight() - 40, 0);
        guiNode.attachChild(txt);

        if (monitors != null) {
           selectedMonitorTxt  = new BitmapText(loadGuiFont());
           //Lets display information about selected monitor
           String label = "Selected Monitor "+ 
                    "Name: "+monitors.get(settings.getMonitor()).name+" "+
                    monitorSelected+ " Res: " + 
                    monitors.get(settings.getMonitor()).width+","+
                    monitors.get(settings.getMonitor()).height +
                    " refresh: "+monitors.get(settings.getMonitor()).rate;
           selectedMonitorTxt.setText(label);
           selectedMonitorTxt.setLocalTranslation(0, settings.getHeight() - 80, 0);
           guiNode.attachChild(selectedMonitorTxt);

           //Let's loop through all the monitors and display on the screen
           for(int i = 0; i < monitors.size(); i++) {
              MonitorInfo monitor = monitors.get(i);
              labelValue = "Mon : "+i+" "+monitor.name+" " + monitor.width +","+ monitor.height +" refresh: "+ monitor.rate;
              txt = new BitmapText(loadGuiFont());
              txt.setText(labelValue);
              txt.setLocalTranslation(0, settings.getHeight() - 160 - (40*i), 0);
              guiNode.attachChild(txt);
           }
        }

        //Lets put a label up there for FullScreen/Window toggle
        fullScreenTxt = new BitmapText(loadGuiFont());
        if (!settings.isFullscreen())
           fullScreenTxt.setText("(f) Window Screen");
        else
           fullScreenTxt.setText("(f) Fullscreen");
           
        fullScreenTxt.setLocalTranslation(00, settings.getHeight() - 240, 0);
        guiNode.attachChild(fullScreenTxt);

        BitmapText infoTxt = new BitmapText(loadGuiFont());
        infoTxt.setText("Restart is required to activate changes in settings.");
        infoTxt.setLocalTranslation(0, settings.getHeight() - 300, 0);
        guiNode.attachChild(infoTxt);

        
    }

    @Override
     public void onAction(String name, boolean isPressed, float tpf) {

      if (monitors == null)
         return;
      
      if (name.equals("down") && isPressed) {
         monitorSelected++;
         if (monitorSelected >= monitors.size())
            monitorSelected = 0;
         saveSettings();
      }
      else if (name.equals("up") && isPressed) {
         monitorSelected--;
         if (monitorSelected < 0)
            monitorSelected = monitors.size()-1;
         saveSettings();
      }
      else if (name.equals("fullscreen") && isPressed) {
         settings.setFullscreen(!settings.isFullscreen());
         saveSettings();
      }     
   }
   
    /**
     *  This function saves out the AppSettings into a file to be loaded back in
     *  on start of application.
     */
   public void saveSettings()
   {
      
       try
       {
          settings.setMonitor(monitorSelected);
          OutputStream out = new FileOutputStream("TestMonitorApp.prefs");
          settings.save(out);
          
          int monitorSelected = settings.getMonitor();
          String label = "Selected Monitor "+ monitorSelected+
                   " "+ monitors.get(monitorSelected).name+
                   " Res: " +monitors.get(monitorSelected).width+","+
                   monitors.get(monitorSelected).height +
                   "refresh: "+monitors.get(monitorSelected).rate;
          selectedMonitorTxt.setText(label);
          if (!settings.isFullscreen())
             fullScreenTxt.setText("(f) Window Screen");
          else
             fullScreenTxt.setText("(f) Fullscreen");
       }
       catch (FileNotFoundException e)
       {
           // TODO Auto-generated catch block
           e.printStackTrace();
       }
       catch (IOException e)
       {
           // TODO Auto-generated catch block
           e.printStackTrace();
       }

   }


}
