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
package com.jme3.gde.core.properties;

import com.jme3.animation.EffectTrack;
import com.jme3.effect.ParticleEmitter;
import com.jme3.scene.Spatial;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import org.openide.nodes.PropertySupport;

/**
 *
 * @author normenhansen
 */
public class EffectTrackEmitterProperty extends PropertySupport.ReadWrite<ParticleEmitter> {

    private LinkedList<ScenePropertyChangeListener> listeners = new LinkedList<ScenePropertyChangeListener>();
    private EffectTrack track;
    private Spatial rootNode;

    public EffectTrackEmitterProperty(EffectTrack track, Spatial rootNode) {
        super("Emitter", ParticleEmitter.class, "Particle Emitter", " ");
        this.rootNode = rootNode;
        this.track = track;

    }

    @Override
    public ParticleEmitter getValue() throws IllegalAccessException, InvocationTargetException {
        return track.getEmitter();
    }

    @Override
    public void setValue(final ParticleEmitter val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ParticleEmitter em = getValue();
        track.setEmitter(val);
        notifyListeners(em, val);
    }

    @Override
    public PropertyEditor getPropertyEditor() {
        return new EffectTrackEmitterPropertyEditor(rootNode, track.getEmitter());
    }

    public void addPropertyChangeListener(ScenePropertyChangeListener listener) {
        listeners.add(listener);
    }

    public void removePropertyChangeListener(ScenePropertyChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Object before, Object after) {
        for (Iterator<ScenePropertyChangeListener> it = listeners.iterator(); it.hasNext();) {
            ScenePropertyChangeListener propertyChangeListener = it.next();
            propertyChangeListener.propertyChange("PROP_USER_CHANGE", getName(), before, after);
        }

    }
}
