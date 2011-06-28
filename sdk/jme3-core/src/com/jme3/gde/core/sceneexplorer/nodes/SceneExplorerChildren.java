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

import com.jme3.effect.ParticleEmitter;
import com.jme3.font.BitmapText;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.ui.Picture;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author normenhansen
 */
public class SceneExplorerChildren extends Children.Keys<Object> {

    protected Spatial spatial;
    protected boolean readOnly = true;
    protected HashMap<Object, Node> map = new HashMap<Object, Node>();
    private DataObject dataObject;

    public SceneExplorerChildren() {
    }

    public SceneExplorerChildren(Spatial spatial) {
        this.spatial = spatial;
    }

    public void refreshChildren(boolean immediate) {
        setKeys(createKeys());
        refresh();
    }

    public void setReadOnly(boolean cookie) {
        this.readOnly = cookie;
    }

    @Override
    protected void addNotify() {
        super.addNotify();
        setKeys(createKeys());
    }

    protected List<Object> createKeys() {
        try {
            return SceneApplication.getApplication().enqueue(new Callable<List<Object>>() {

                public List<Object> call() throws Exception {
                    List<Object> keys = new LinkedList<Object>();
                    if (spatial != null && spatial instanceof com.jme3.scene.Node) {
                        keys.addAll(((com.jme3.scene.Node) spatial).getChildren());
//                        return keys;
                    }
                    if (spatial instanceof Geometry) {
                        Geometry geom = (Geometry) spatial;
                        Mesh mesh = geom.getMesh();
                        if (mesh != null) {
                            keys.add(new MeshGeometryPair(mesh, geom));
                        }
                    }
                    LightList lights = spatial.getLocalLightList();
                    for (int i = 0; i < lights.size(); i++) {
                        Light light = lights.get(i);
                        if (light != null) {
                            keys.add(new LightSpatialPair(light, spatial));
                        }
                    }
                    for (int i = 0; i < spatial.getNumControls(); i++) {
                        Control control = spatial.getControl(i);
                        keys.add(control);
                    }
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
    protected Node[] createNodes(Object key) {
        for (SceneExplorerNode di : Lookup.getDefault().lookupAll(SceneExplorerNode.class)) {
            if (di.getExplorerObjectClass().getName().equals(key.getClass().getName())) {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Found {0}", di.getExplorerNodeClass());
                Node[] ret = di.createNodes(key, dataObject, readOnly);
                if (ret != null) {
                    return ret;
                }
            }
        }

        //TODO: go down in class hierarchy if class was not found, for now old checks are fallback
        if (key instanceof Spatial) {
            SceneExplorerChildren children = new SceneExplorerChildren((Spatial) key);
            children.setReadOnly(readOnly);
            children.setDataObject(dataObject);
            if (key instanceof com.jme3.audio.AudioNode) {
                return new Node[]{new JmeAudioNode((com.jme3.audio.AudioNode) key, children).setReadOnly(readOnly)};
            }
            if (key instanceof com.jme3.scene.Node) {
                return new Node[]{new JmeNode((com.jme3.scene.Node) key, children).setReadOnly(readOnly)};
            }
            if (key instanceof BitmapText) {
                return new Node[]{new JmeBitmapText((BitmapText) key, children).setReadOnly(readOnly)};
            }
            if (key instanceof Picture) {
                return new Node[]{new JmePicture((Picture) key, children).setReadOnly(readOnly)};
            }
            if (key instanceof ParticleEmitter) {
                return new Node[]{new JmeParticleEmitter((ParticleEmitter) key, children).setReadOnly(readOnly)};
            }
            if (key instanceof com.jme3.scene.Geometry) {
                return new Node[]{new JmeGeometry((Geometry) key, children).setReadOnly(readOnly)};
            }
            return new Node[]{new JmeSpatial((Spatial) key, children).setReadOnly(readOnly)};
        } else if (key instanceof LightSpatialPair) {
            LightSpatialPair pair = (LightSpatialPair) key;
            if (pair.getLight() instanceof PointLight) {
                return new Node[]{new JmePointLight(pair.getSpatial(), (PointLight) pair.getLight()).setReadOnly(readOnly)};
            }
            if (pair.getLight() instanceof DirectionalLight) {
                return new Node[]{new JmeDirectionalLight(pair.getSpatial(), (DirectionalLight) pair.getLight()).setReadOnly(readOnly)};
            }
            if (pair.getLight() instanceof AmbientLight) {
                return new Node[]{new JmeAmbientLight(pair.getSpatial(), (AmbientLight) pair.getLight()).setReadOnly(readOnly)};
            }
            return new Node[]{new JmeLight(pair.getSpatial(), pair.getLight()).setReadOnly(readOnly)};
        } else if (key instanceof MeshGeometryPair) {
            MeshGeometryPair pair = (MeshGeometryPair) key;
            return new Node[]{new JmeMesh(pair.getGeometry(), pair.getMesh()).setReadOnly(readOnly)};
        } else if (key instanceof Control) {
            return new Node[]{new JmeGenericControl((Control) key, dataObject)};
        }
        return new Node[]{Node.EMPTY};
    }

    public DataObject getDataObject() {
        return dataObject;
    }

    public void setDataObject(DataObject dataObject) {
        this.dataObject = dataObject;
    }
}
