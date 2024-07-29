/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.export;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.export.SavableObject;
import com.jme3.renderer.framegraph.FrameGraph;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Holds savable data for a FrameGraph.
 * 
 * @author codex
 */
public class FrameGraphData implements Savable {
    
    private String name;
    private ModuleGraphData modules;
    private ArrayList<SavableObject> settings = new ArrayList<>();
    
    public FrameGraphData() {}
    public FrameGraphData(FrameGraph frameGraph) {
        this.name = frameGraph.getName();
        this.modules = frameGraph.createModuleData();
        HashMap<String, Object> settingsMap = frameGraph.getSettingsMap();
        for (String key : settingsMap.keySet()) {
            settings.add(new SavableObject(key, settingsMap.get(key)));
        }
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(name, "name", "FrameGraph");
        out.write(modules, "modules", null);
        out.writeSavableArrayList(settings, "settings", new ArrayList<>());
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", "FrameGraph");
        modules = in.readSavable("modules", ModuleGraphData.class, null);
        settings = in.readSavableArrayList("settings", new ArrayList<>());
    }

    public String getName() {
        return name;
    }
    public ModuleGraphData getModules() {
        return modules;
    }
    public ArrayList<SavableObject> getSettings() {
        return settings;
    }
    
}
