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

import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.audio.Filter;
import com.jme3.gde.core.icons.IconList;
import com.jme3.gde.core.properties.AudioDataProperty;
import com.jme3.math.Vector3f;
import java.awt.Image;
import org.openide.loaders.DataObject;
import org.openide.nodes.Sheet;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service=SceneExplorerNode.class)
public class JmeAudioNode extends JmeNode {

    private static Image smallImage = IconList.sound.getImage();
    private AudioNode node;

    public JmeAudioNode() {
    }

    public JmeAudioNode(AudioNode spatial, JmeSpatialChildren children) {
        super(spatial, children);
        getLookupContents().add(spatial);
        this.node = spatial;
     //   setName(spatial.getName());
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
    protected Sheet createSheet() {
        //TODO: multithreading..
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("AudioNode");
        set.setName(AudioNode.class.getName());
        AudioNode obj = node;//getLookup().lookup(Spatial.class);
        if (obj == null) {
            return sheet;
        }

        set.put(new AudioDataProperty(obj));
        set.put(makeProperty(obj, Vector3f.class, "getDirection", "setDirection", "Direction"));
        set.put(makeProperty(obj, boolean.class, "isDirectional", "setDirectional", "Directional"));
        set.put(makeProperty(obj, float.class, "getInnerAngle", "setInnerAngle", "Inner Angle"));
        set.put(makeProperty(obj, float.class, "getOuterAngle", "setOuterAngle", "Outer Angle"));
        set.put(makeProperty(obj, Filter.class, "getDryFilter", "setDryFilter", "Dry Filter"));
        set.put(makeProperty(obj, boolean.class, "isLooping", "setLooping", "Looping"));
        set.put(makeProperty(obj, float.class, "getMaxDistance", "setMaxDistance", "Max Distance"));

        set.put(makeProperty(obj, float.class, "getPitch", "setPitch", "Audio Pitch"));
        set.put(makeProperty(obj, boolean.class, "isPositional", "setPositional", "Positional"));

        set.put(makeProperty(obj, boolean.class, "isReverbEnabled", "setReverbEnabled", "Reverb"));
        set.put(makeProperty(obj, Filter.class, "getReverbFilter", "setReverbFilter", "Reverb Filter"));
        set.put(makeProperty(obj, float.class, "getRefDistance", "setRefDistance", "Ref Distance"));
        set.put(makeProperty(obj, float.class, "getTimeOffset", "setTimeOffset", "Time Offset"));

        set.put(makeProperty(obj, AudioSource.Status.class, "getStatus", "setStatus", "Status"));

        set.put(makeProperty(obj, float.class, "getVolume", "setVolume", "Volume"));
        set.put(makeProperty(obj, Vector3f.class, "getVelocity", "setVelocity", "Velocity"));
        sheet.put(set);
        return sheet;

    }

    @Override
    public Class getExplorerObjectClass() {
        return AudioNode.class;
    }

    @Override
    public Class getExplorerNodeClass() {
        return JmeAudioNode.class;
    }

    @Override
    public org.openide.nodes.Node[] createNodes(Object key, DataObject key2, boolean cookie) {
        JmeSpatialChildren children=new JmeSpatialChildren((com.jme3.scene.Spatial)key);
        children.setReadOnly(cookie);
        children.setDataObject(key2);
        return new org.openide.nodes.Node[]{new JmeAudioNode((AudioNode) key, children).setReadOnly(cookie)};
    }
}
