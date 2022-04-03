package com.jme3.system;

import com.jme3.awt.AWTErrorDialog;
import com.jme3.awt.AWTSettingsDialog;

public class JmeDialogsFactoryImpl implements JmeDialogsFactory {
    public boolean showSettingsDialog(AppSettings settings, boolean loadFromRegistry){
        return AWTSettingsDialog.showDialog(settings,loadFromRegistry);
    }


    public void showErrorDialog(String message){
        AWTErrorDialog.showDialog(message);
    }
    
}
