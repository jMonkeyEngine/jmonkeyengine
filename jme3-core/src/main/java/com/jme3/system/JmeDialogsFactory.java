package com.jme3.system;

public interface JmeDialogsFactory {
    public boolean showSettingsDialog(AppSettings settings, boolean loadFromRegistry);    
    public void showErrorDialog(String message);  
}
