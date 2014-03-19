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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.AudioTrack;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.EffectTrack;
import com.jme3.animation.Track;
import com.jme3.gde.core.icons.IconList;
import com.jme3.gde.core.properties.AudioTrackProperty;
import com.jme3.gde.core.properties.EffectTrackEmitterProperty;
import com.jme3.gde.core.properties.SceneExplorerProperty;
import com.jme3.gde.core.properties.SliderPropertyEditor;
import com.jme3.gde.core.scene.SceneApplication;
import java.awt.Image;
import java.beans.PropertyEditor;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import org.openide.actions.DeleteAction;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = SceneExplorerNode.class)
public class JmeTrack extends AbstractSceneExplorerNode {
    
    private static Image iconBoneTrack = IconList.boneTrack.getImage();
    private static Image iconEffectTrack = IconList.effectTrack.getImage();
    private static Image iconAudioTrack = IconList.audioTrack.getImage();
    private static Image iconTrack = IconList.track.getImage();
    private Track track;
    private AnimControl control;
    
    public JmeTrack() {
    }
    
    public JmeTrack(Track track, AnimControl control, DataObject obj) {
        super(Children.LEAF);
        dataObject = obj;
        
        getLookupContents().add(track);
        getLookupContents().add(this);
        this.track = track;
        this.control = control;
        setName();
    }

    private void setName() {
        if (track instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            super.setName("BoneTrack : " + control.getSkeleton().getBone(boneTrack.getTargetBoneIndex()).getName());
        } else if (track instanceof EffectTrack) {
            EffectTrack effectTrack = (EffectTrack) track;
            super.setName("EffectTrack : " + effectTrack.getEmitter().getName());            
        } else if (track instanceof AudioTrack) {
            AudioTrack audioTrack = (AudioTrack) track;
            super.setName("AudioTrack : " + audioTrack.getAudio().getName());
            
        } else {
            super.setName(track.getClass().getSimpleName());
        }
    }
    
    
    
    @Override
    public Image getIcon(int type) {
        if (track instanceof BoneTrack) {
            return iconBoneTrack;
        } else if (track instanceof EffectTrack) {
            return iconEffectTrack;
        } else if (track instanceof AudioTrack) {
            return iconAudioTrack;
        }
        return iconTrack;
        
    }
    
    @Override
    public Image getOpenedIcon(int type) {
        if (track instanceof BoneTrack) {
            return iconBoneTrack;
        } else if (track instanceof EffectTrack) {
            return iconEffectTrack;
        } else if (track instanceof AudioTrack) {
            return iconAudioTrack;
        }
        return iconTrack;
    }
    
    public class dum {
        
        float val;
        
        public float getVal() {
            return val;
        }
        
        public void setVal(float val) {
            this.val = val;
        }
    }
    
    @Override
    protected Sheet createSheet() {
        //TODO: multithreading..
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("Track");
        set.setName(track.getClass().getSimpleName());
        Track obj = track;//getLookup().lookup(Spatial.class);
        if (obj == null) {
            return sheet;
        }
        set.put(makeProperty(obj, float.class, "getLength", "Track length"));
        if (track instanceof EffectTrack) {
            EffectTrackEmitterProperty prop = new EffectTrackEmitterProperty((EffectTrack) track, control.getSpatial());
            prop.addPropertyChangeListener(this);
            set.put(prop);
        }
        if (track instanceof AudioTrack) {
            AudioTrackProperty prop = new AudioTrackProperty((AudioTrack) track, control.getSpatial());
            prop.addPropertyChangeListener(this);
            set.put(prop);
        }
        if (track instanceof EffectTrack || track instanceof AudioTrack) {
            try {
                // set.put(createSliderProperty(track));

                SceneExplorerProperty prop = new SceneExplorerProperty(track, float.class, "getStartOffset", "setStartOffset", this) {
                    
                    SliderPropertyEditor editor = null;
                    
                    @Override
                    public PropertyEditor getPropertyEditor() {
                        if (editor == null) {
                            if (track instanceof EffectTrack) {
                                editor = new SliderPropertyEditor(0f, ((EffectTrack) track).getLength());
                                
                            } else {
                                editor = new SliderPropertyEditor(0f, ((AudioTrack) track).getLength());
                                
                            }
                        }
                        return editor;
                    }
                };
                set.put(prop);
            } catch (NoSuchMethodException ex) {
                Exceptions.printStackTrace(ex);
            }
            
        }

        //  set.put(makeProperty(obj, boolean.class, "isEnabled", "setEnabled", "Enabled"));
        //set.put(makeProperty(obj, boolean.class, "isEnabled", "setEnabled", "Enabled"));
//        set.put(makeProperty(obj, ParticleMesh.Type.class, "getMeshType", "setMeshType", "Mesh Type"));
//        set.put(makeProperty(obj, EmitterShape.class, "getShape", "setShape", "Emitter Shape"));
        sheet.put(set);
        return sheet;
        
    }
    
    @Override
    public Action[] getActions(boolean context) {
        
        return new Action[]{
                    SystemAction.get(DeleteAction.class)
                };
    }
    
    @Override
    public boolean canDestroy() {
        return !(track instanceof BoneTrack);
    }
    
    @Override
    public void destroy() throws IOException {
        super.destroy();
        fireSave(true);
        final Animation anim = getParentNode().getLookup().lookup(Animation.class);
        try {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {
                
                public Void call() throws Exception {
                    anim.removeTrack(track);
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
    
    public Class getExplorerObjectClass() {
        return Track.class;
    }
    
    @Override
    public Class getExplorerNodeClass() {
        return JmeTrack.class;
    }
    
    @Override
    public org.openide.nodes.Node[] createNodes(Object key, DataObject key2, boolean cookie) {
        return new org.openide.nodes.Node[]{new JmeTrack((Track) key, control, key2).setReadOnly(cookie)};
    }

    @Override
    public void propertyChange(String type, String name, Object before, Object after) {
        super.propertyChange(type, name, before, after);
        setName();
    }
    
    
    
}
