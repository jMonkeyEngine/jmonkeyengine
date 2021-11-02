/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.util;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.UrlAssetInfo;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.Trigger;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.post.Filter.Pass;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.RendererException;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shader.Shader;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This appState is for debug purpose only, and was made to provide an easy way 
 * to test shaders, with a live update capability.
 * 
 * This class provides and easy way to reload a material and catches compilation
 * errors when needed and displays the error in the console.
 * 
 * If no error occur on compilation, the material is reloaded in the scene.
 * 
 * You can either trigger the reload when pressing a key (or whatever input is 
 * supported by Triggers you can attach to the input manager), or trigger it 
 * when a specific file (the shader source) has been changed on the hard drive.
 * 
 * Usage : 
 * 
 * MaterialDebugAppState matDebug = new MaterialDebugAppState();
 * stateManager.attach(matDebug);
 * matDebug.registerBinding(new KeyTrigger(KeyInput.KEY_R), whateverGeometry);  
 * 
 * this will reload the material of whateverGeometry when pressing the R key.
 * 
 * matDebug.registerBinding("Shaders/distort.frag", whateverGeometry); 
 * 
 * this will reload the material of whateverGeometry when the given file is 
 * changed on the hard drive.
 * 
 * you can also register bindings to the appState with a post process Filter
 *  
 * @author Nehon
 */
public class MaterialDebugAppState extends AbstractAppState {

    private RenderManager renderManager;
    private AssetManager assetManager;
    private InputManager inputManager;
    final private List<Binding> bindings = new ArrayList<>();
    final private Map<Trigger,List<Binding>> fileTriggers = new HashMap<> ();
    

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        renderManager = app.getRenderManager();
        assetManager = app.getAssetManager();
        inputManager = app.getInputManager();
        for (Binding binding : bindings) {
            bind(binding);
        }
        super.initialize(stateManager, app);
    }

    /**
     * Will reload the spatial's materials whenever the trigger is fired
     * @param trigger the trigger
     * @param spat the spatial to reload
     */
    public void registerBinding(Trigger trigger, final Spatial spat) {
        if(spat instanceof Geometry){
            GeometryBinding binding = new GeometryBinding(trigger, (Geometry)spat);           
            bindings.add(binding);
            if (isInitialized()) {
                bind(binding);
            }
        }else if (spat instanceof Node){
            for (Spatial child : ((Node)spat).getChildren()) {
                registerBinding(trigger, child);
            }
        }
    }
    
    

    /**
     * Will reload the filter's materials whenever the trigger is fired.
     * @param trigger the trigger
     * @param filter the filter to reload
     */
    public void registerBinding(Trigger trigger, final Filter filter) {
        FilterBinding binding = new FilterBinding(trigger, filter);
        bindings.add(binding);
        if (isInitialized()) {
            bind(binding);
        }
    }

    
    /**
     * Will reload the filter's materials whenever the shader file is changed 
     * on the hard drive
     * @param shaderName the shader name (relative path to the asset folder or 
     * to a registered asset path)
     * @param filter the filter to reload
     */
    public void registerBinding(String shaderName, final Filter filter) {
        registerBinding(new FileChangedTrigger(shaderName), filter);
    }

    /**
     * Will reload the spatial's materials whenever the shader file is changed
     * on the hard drive
     * @param shaderName the shader name (relative path to the asset folder or 
     * to a registered asset path)
     * @param spat the spatial to reload
     */
    public void registerBinding(String shaderName, final Spatial spat) {
        registerBinding(new FileChangedTrigger(shaderName), spat);
    }

    private void bind(final Binding binding) {
        if (binding.getTrigger() instanceof FileChangedTrigger) {
            FileChangedTrigger t = (FileChangedTrigger) binding.getTrigger();
            List<Binding> b = fileTriggers.get(t);
            if(b == null){
                t.init();
                b = new ArrayList<Binding>();
                fileTriggers.put(t, b);
            }
            b.add(binding);
        } else {
            final String actionName = binding.getActionName();
            inputManager.addListener(new ActionListener() {
                @Override
                public void onAction(String name, boolean isPressed, float tpf) {
                    if (actionName.equals(name) && isPressed) {
                        //reloading the material
                        binding.reload();
                    }
                }
            }, actionName);

            inputManager.addMapping(actionName, binding.getTrigger());
        }
    }

    public Material reloadMaterial(Material mat) {
        //clear the entire cache, there might be more clever things to do, like clearing only the matdef, and the associated shaders.
        assetManager.clearCache();

        //creating a dummy mat with the mat def of the mat to reload
        // Force the reloading of the asset, otherwise the new shader code will not be applied.
        Material dummy = new Material(assetManager, mat.getMaterialDef().getAssetName());

        for (MatParam matParam : mat.getParams()) {
            dummy.setParam(matParam.getName(), matParam.getVarType(), matParam.getValue());
        }
        
        dummy.getAdditionalRenderState().set(mat.getAdditionalRenderState());        

        //creating a dummy geom and assigning the dummy material to it
        Geometry dummyGeom = new Geometry("dummyGeom", new Box(1f, 1f, 1f));
        dummyGeom.setMaterial(dummy);

        try {
            //preloading the dummyGeom, this call will compile the shader again
            renderManager.preloadScene(dummyGeom);
        } catch (RendererException e) {
            //compilation error, the shader code will be output to the console
            //the following code will output the error
            //System.err.println(e.getMessage());
            Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.SEVERE, e.getMessage());
            return null;
        }

        Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.INFO, "Material successfully reloaded");
        //System.out.println("Material successfully reloaded");
        return dummy;
    }
   
    @Override
    public void update(float tpf) {
        super.update(tpf); //To change body of generated methods, choose Tools | Templates.
        for (Trigger trigger : fileTriggers.keySet()) {
            if (trigger instanceof FileChangedTrigger) {
                FileChangedTrigger t = (FileChangedTrigger) trigger;
                if (t.shouldFire()) {
                    List<Binding> b = fileTriggers.get(t);
                    for (Binding binding : b) {
                        binding.reload();
                    }
                }
            }
        }       
    }

    private interface Binding {

        public String getActionName();

        public void reload();

        public Trigger getTrigger();
    }

    private class GeometryBinding implements Binding {

        Trigger trigger;
        Geometry geom;

        public GeometryBinding(Trigger trigger, Geometry geom) {
            this.trigger = trigger;
            this.geom = geom;

        }

        @Override
        public void reload() {
            Material reloadedMat = reloadMaterial(geom.getMaterial());
            //if the reload is successful, we re setup the material with its params and reassign it to the box
            if (reloadedMat != null) {
                // setupMaterial(reloadedMat);
                geom.setMaterial(reloadedMat);
            }
        }

        @Override
        public String getActionName() {
            return geom.getName() + "Reload";

        }

        @Override
        public Trigger getTrigger() {
            return trigger;
        }
    }

    private class FilterBinding implements Binding {

        Trigger trigger;
        Filter filter;

        public FilterBinding(Trigger trigger, Filter filter) {
            this.trigger = trigger;
            this.filter = filter;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void reload() {
            Field[] fields1 = filter.getClass().getDeclaredFields();
            Field[] fields2 = filter.getClass().getSuperclass().getDeclaredFields();

            List<Field> fields = new ArrayList<>();
            fields.addAll(Arrays.asList(fields1));
            fields.addAll(Arrays.asList(fields2));
            Material m = new Material();
            Filter.Pass p = filter.new Pass();
            try {
                for (Field field : fields) {
                    if (field.getType().isInstance(m)) {
                        field.setAccessible(true);
                        Material mat = reloadMaterial((Material) field.get(filter));
                        if (mat == null) {
                            return;
                        } else {
                            field.set(filter, mat);
                        }

                    }
                    if (field.getType().isInstance(p)) {
                        field.setAccessible(true);
                        p = (Filter.Pass) field.get(filter);
                        if (p!= null && p.getPassMaterial() != null) {
                            Material mat = reloadMaterial(p.getPassMaterial());
                            if (mat == null) {
                                return;
                            } else {
                                p.setPassMaterial(mat);
                            }
                        }
                    }
                    if (field.getName().equals("postRenderPasses")) {
                        field.setAccessible(true);
                        List<Pass> passes = new ArrayList<>();
                        passes = (List<Pass>) field.get(filter);
                        if (passes != null) {
                            for (Pass pass : passes) {
                                Material mat = reloadMaterial(pass.getPassMaterial());
                                if (mat == null) {
                                    return;
                                } else {
                                    pass.setPassMaterial(mat);
                                }
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        @Override
        public String getActionName() {
            return filter.getName() + "Reload";
        }

        @Override
        public Trigger getTrigger() {
            return trigger;
        }
    }

    private class FileChangedTrigger implements Trigger {

        String fileName;
        File file;
        Long fileLastM;

        public FileChangedTrigger(String fileName) {
            this.fileName = fileName;
        }

        public void init() {
            AssetInfo info = assetManager.locateAsset(new AssetKey<Shader>(fileName));
            if (info != null && info instanceof UrlAssetInfo) {
                try {
                    Field f = info.getClass().getDeclaredField("url");
                    f.setAccessible(true);
                    URL url = (URL) f.get(info);
                    file = new File(url.getFile());
                    fileLastM = file.lastModified();

                } catch (NoSuchFieldException
                        | SecurityException
                        | IllegalArgumentException
                        | IllegalAccessException ex) {
                    Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        public boolean shouldFire() {
            if (file.lastModified() != fileLastM) {
                fileLastM = file.lastModified();
                return true;
            }
            return false;
        }

        @Override
        public String getName() {
            return fileName;
        }

        @Override
        public int triggerHashCode() {
            return 0;
        }
    }
}
