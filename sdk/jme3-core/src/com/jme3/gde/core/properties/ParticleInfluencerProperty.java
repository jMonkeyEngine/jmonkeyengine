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

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.influencers.ParticleInfluencer;
import com.jme3.gde.core.sceneexplorer.nodes.JmeParticleEmitter;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import org.netbeans.api.project.Project;
import org.openide.nodes.PropertySupport;

/**
 *
 * @author normenhansen
 */
public class ParticleInfluencerProperty extends PropertySupport.ReadWrite<ParticleInfluencer> {

    private LinkedList<ScenePropertyChangeListener> listeners = new LinkedList<ScenePropertyChangeListener>();
    private ParticleEmitter emitter;
    private JmeParticleEmitter jmePE;
    private Project project;

    public ParticleInfluencerProperty(ParticleEmitter emitter,JmeParticleEmitter jmePE, Project project) {
        super("ParticleInfluencer", ParticleInfluencer.class, "Particle Influencer", " ");
        this.project = project;
        this.emitter = emitter;
        this.jmePE = jmePE;

    }

    @Override
    public ParticleInfluencer getValue() throws IllegalAccessException, InvocationTargetException {
        return emitter.getParticleInfluencer();
    }

    @Override
    public void setValue(final ParticleInfluencer val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ParticleInfluencer pi = getValue();
        emitter.setParticleInfluencer(val);
        notifyListeners(pi, val);
    }

    @Override
    public PropertyEditor getPropertyEditor() {
        return new ParticleInfluencerPropertyEditor(jmePE, project);
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
