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

import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.SceneExplorerTopComponent;
import com.jme3.gde.core.sceneexplorer.nodes.SceneExplorerNode;
import com.jme3.gde.core.sceneexplorer.MaterialChangeListener;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import java.awt.Image;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.openide.loaders.DataObject;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = SceneExplorerNode.class)
public class JmeGeometry extends JmeSpatial implements MaterialChangeListener {

    private static Image smallImage =
            ImageUtilities.loadImage("com/jme3/gde/core/sceneexplorer/nodes/icons/geometry.gif");
    private Geometry geom;

    public JmeGeometry() {
    }

    public JmeGeometry(Geometry spatial, JmeSpatialChildren children) {
        super(spatial, children);
        getLookupContents().add(spatial);
        this.geom = spatial;
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    SceneExplorerTopComponent.findInstance().addMaterialChangeListener(JmeGeometry.this);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        setName(spatial.getName());
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
    public void destroy() throws IOException {
        super.destroy();
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    SceneExplorerTopComponent.findInstance().removeMaterialChangeListener(JmeGeometry.this);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    protected Sheet createSheet() {
        //TODO: multithreading..
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("Geometry");
        set.setName(Geometry.class.getName());
        Geometry obj = geom;//getLookup().lookup(Spatial.class);
        if (obj == null) {
            return sheet;
        }

        set.put(makeProperty(obj, int.class, "getLodLevel", "setLodLevel", "Lod Level"));
        set.put(makeProperty(obj, Material.class, "getMaterial", "setMaterial", "Material"));
        set.put(makeProperty(obj, Mesh.class, "getMesh", "Mesh"));

        sheet.put(set);
        return sheet;

    }

    @Override
    public Class getExplorerObjectClass() {
        return Geometry.class;
    }

    @Override
    public Class getExplorerNodeClass() {
        return JmeGeometry.class;
    }

    @Override
    public org.openide.nodes.Node[] createNodes(Object key, DataObject key2, boolean cookie) {
        JmeSpatialChildren children = new JmeSpatialChildren((com.jme3.scene.Spatial) key);
        children.setReadOnly(cookie);
        children.setDataObject(key2);
        return new org.openide.nodes.Node[]{new JmeGeometry((Geometry) key, children).setReadOnly(cookie)};
    }

    @Override
    public void propertyChange(String name, final Object before, final Object after) {
        super.propertyChange(name, before, after);
          System.out.println(name);
        if (name.equals("Material")) {
            System.out.println(before.toString()+" "+after.toString());
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    try {
                        SceneExplorerTopComponent.findInstance().swapMaterialChangeListener(JmeGeometry.this, ((Material) before).getAssetName(), ((Material) after).getAssetName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });


        }
    }

    public void setMaterial(final Material material) {
        if (material.getAssetName().equals(getKey())) {
            SceneApplication.getApplication().enqueue(new Callable<Object>() {

                public Object call() throws Exception {
                    geom.setMaterial(material);
                    return null;
                }
            });
        }
    }

    public Geometry getGeometry() {
        return geom;
    }

    public String getKey() {
        return geom.getMaterial().getAssetName();
    }
}
