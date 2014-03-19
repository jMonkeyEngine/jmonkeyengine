/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.sceneexplorer.nodes;


import com.jme3.gde.core.icons.IconList;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.geomipmap.lodcalc.PerspectiveLodCalculator;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author Brent Owens
 */
@org.openide.util.lookup.ServiceProvider(service=SceneExplorerNode.class)
public class JmeTerrainLodControl extends AbstractSceneExplorerNode {
    
    private static Image smallImage = IconList.wheel.getImage();
    private TerrainLodControl terrainLodControl;
    
    public JmeTerrainLodControl() {
    }

    public JmeTerrainLodControl(TerrainLodControl control, Children children) {
        super(children);
        getLookupContents().add(control);
        getLookupContents().add(this);
        this.terrainLodControl = control;
        setName("TerrainLodControl");
    }
    
    @Override
    public Image getIcon(int type) {
        return smallImage;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return smallImage;
    }

    @Override
    public Action[] getActions(boolean context) {
        return new SystemAction[]{
                    //SystemAction.get(CopyAction.class),
                    //SystemAction.get(CutAction.class),
                    //SystemAction.get(PasteAction.class),
                    //SystemAction.get(DeleteAction.class)
                };
    }

    @Override
    public boolean canDestroy() {
        return !readOnly;
    }

    @Override
    public void destroy() throws IOException {
        super.destroy();
        final Spatial spat = getParentNode().getLookup().lookup(Spatial.class);
        try {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    spat.removeControl(terrainLodControl);
                    return null;
                }
            }).get();
            ((AbstractSceneExplorerNode) getParentNode()).refresh(true);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("TerrainLodControl");
        set.setName(TerrainLodControl.class.getName());
        TerrainLodControl obj = terrainLodControl;
        if (obj == null) {
            return sheet;
        }
        
        set.put(makeProperty(obj, boolean.class, "isEnabled", "setEnabled", "Enabled"));

        sheet.put(set);
        return sheet;

    }

    public Class getExplorerObjectClass() {
        return TerrainLodControl.class;
    }

    @Override
    public Class getExplorerNodeClass() {
        return JmeTerrainLodControl.class;
    }
    
    @Override
    public org.openide.nodes.Node[] createNodes(Object key, DataObject key2, boolean cookie) {
        TerrainLodControlChildren children = new TerrainLodControlChildren((TerrainLodControl) key);
        children.setReadOnly(cookie);
        children.setDataObject(key2);
        return new org.openide.nodes.Node[]{new JmeTerrainLodControl((TerrainLodControl) key, children).setReadOnly(cookie)};
    }
    
    public static class TerrainLodControlChildren extends JmeSpatialChildren {

        TerrainLodControl control;

        public TerrainLodControlChildren(TerrainLodControl control) {
            this.control = control;
        }

        @Override
        public void refreshChildren(boolean immediate) {
            setKeys(createKeys());
            refresh();
        }

        @Override
        protected List<Object> createKeys() {
            try {
                return SceneApplication.getApplication().enqueue(new Callable<List<Object>>() {

                    public List<Object> call() throws Exception {
                        List<Object> keys = new ArrayList<Object>();
                        if (control.getLodCalculator() != null)
                            keys.add(control.getLodCalculator());
                        return keys;
                    }
                }).get();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ExecutionException ex) {
                Exceptions.printStackTrace(ex);
            }
            return null;
        }

        @Override
        public void setReadOnly(boolean cookie) {
            this.readOnly = cookie;
        }

        @Override
        protected void addNotify() {
            super.addNotify();
            setKeys(createKeys());
        }

        @Override
        protected Node[] createNodes(Object key) {
            if (key instanceof DistanceLodCalculator) {
                DistanceLodCalculator assetKey = (DistanceLodCalculator) key;
                return new Node[]{new JmeDistanceLodCalculator(control, assetKey, getDataObject())};
            } else if (key instanceof PerspectiveLodCalculator) {
                //PerspectiveLodCalculator assetKey = (PerspectiveLodCalculator) key;
                //return new Node[]{new JmePerspectiveLodCalculator(control, assetKey)};
            }
            return null;
        }

    }
}
