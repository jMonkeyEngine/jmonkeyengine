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

import com.jme3.effect.shapes.EmitterShape;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.influencers.ParticleInfluencer;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import java.awt.Image;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = SceneExplorerNode.class)
public class JmeParticleEmitter extends JmeGeometry {

    public JmeParticleEmitter() {
    }
    private static Image smallImage =
            ImageUtilities.loadImage("com/jme3/gde/core/sceneexplorer/nodes/icons/particleemitter.gif");
    private ParticleEmitter geom;

    public JmeParticleEmitter(ParticleEmitter spatial, JmeSpatialChildren children) {
        super(spatial, children);
        getLookupContents().add(spatial);
        this.geom = spatial;
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
        set.setDisplayName("ParticleEmitter");
        set.setName(ParticleEmitter.class.getName());
        ParticleEmitter obj = geom;//getLookup().lookup(Spatial.class);
        if (obj == null) {
            return sheet;
        }


        set.put(createButtonProperty());
        set.put(makeProperty(obj, boolean.class, "isEnabled", "setEnabled", "Enabled"));
        set.put(makeProperty(obj, ParticleMesh.Type.class, "getMeshType", "setMeshType", "Mesh Type"));
        set.put(makeProperty(obj, EmitterShape.class, "getShape", "setShape", "Emitter Shape"));
        set.put(makeProperty(obj, int.class, "getMaxNumParticles", "setNumParticles", "Num Particles"));
        set.put(makeProperty(obj, float.class, "getParticlesPerSec", "setParticlesPerSec", "Particles Per Sec"));
        set.put(makeProperty(obj, ColorRGBA.class, "getStartColor", "setStartColor", "Start Color"));
        set.put(makeProperty(obj, ColorRGBA.class, "getEndColor", "setEndColor", "End Color"));
        set.put(makeProperty(obj, float.class, "getStartSize", "setStartSize", "Start Size"));
        set.put(makeProperty(obj, float.class, "getEndSize", "setEndSize", "End Size"));
        set.put(makeProperty(obj, float.class, "getHighLife", "setHighLife", "High Life"));
        set.put(makeProperty(obj, float.class, "getLowLife", "setLowLife", "Low Life"));
        set.put(makeProperty(obj, Vector3f.class, "getGravity", "setGravity", "Gravity"));
        set.put(makeEmbedProperty(obj.getParticleInfluencer(), ParticleInfluencer.class, Vector3f.class, "getInitialVelocity", "setInitialVelocity", "Initial Velocity"));
        set.put(makeEmbedProperty(obj.getParticleInfluencer(), ParticleInfluencer.class, float.class, "getVelocityVariation", "setVelocityVariation", "Velocity Variation"));
        set.put(makeProperty(obj, Vector3f.class, "getFaceNormal", "setFaceNormal", "Face Normal"));
        set.put(makeProperty(obj, boolean.class, "isFacingVelocity", "setFacingVelocity", "Facing Velocity"));
        set.put(makeProperty(obj, boolean.class, "isRandomAngle", "setRandomAngle", "Random Angle"));
        set.put(makeProperty(obj, boolean.class, "isInWorldSpace", "setInWorldSpace", "World Space"));
        set.put(makeProperty(obj, float.class, "getRotateSpeed", "setRotateSpeed", "Rotate Speed"));
        set.put(makeProperty(obj, boolean.class, "isSelectRandomImage", "setSelectRandomImage", "Select Random Image"));
        set.put(makeProperty(obj, int.class, "getImagesX", "setImagesX", "Images X"));
        set.put(makeProperty(obj, int.class, "getImagesY", "setImagesY", "Images Y"));
        sheet.put(set);

        return sheet;

    }

    @Override
    public void propertyChange(String name, Object before, Object after) {
        super.propertyChange(name, before, after);
        if (!name.equals("Emit all particles")) {
            fireSave(true);
            firePropertyChange(name, before, after);
        }

    }

    @Override
    public Class getExplorerObjectClass() {
        return ParticleEmitter.class;
    }

    @Override
    public Class getExplorerNodeClass() {
        return JmeParticleEmitter.class;
    }

    @Override
    public Node[] createNodes(Object key, DataObject key2, boolean cookie) {
        JmeSpatialChildren children = new JmeSpatialChildren((com.jme3.scene.Spatial) key);
        children.setReadOnly(cookie);
        children.setDataObject(key2);
        return new Node[]{new JmeParticleEmitter((ParticleEmitter) key, children).setReadOnly(cookie)};
    }

    public ParticleEmitter getEmitter() {
        return geom;
    }

    private Property createButtonProperty() {
        return new PropertySupport.ReadWrite<Object>("emit", Object.class, "Emit all particles", "Click here to emit all particles of this emitter ") {

            JmeParticleEmitterButtonProperty pe;

            @Override
            public Object getValue() throws IllegalAccessException, InvocationTargetException {
                return "";
            }

            @Override
            public PropertyEditor getPropertyEditor() {
                if (pe == null) {
                    pe = new JmeParticleEmitterButtonProperty(JmeParticleEmitter.this);
                    pe.attachEnv(pe.env);
                }
                return pe;
            }

            @Override
            public void setValue(Object t) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            }
        };
    }
}
