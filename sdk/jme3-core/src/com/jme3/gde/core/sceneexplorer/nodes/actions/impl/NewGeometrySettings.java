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
package com.jme3.gde.core.sceneexplorer.nodes.actions.impl;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 *
 * @author david.bernard.31
 */


public class NewGeometrySettings implements Serializable, PreferenceChangeListener {
    
    private transient final PropertyChangeSupport propertySupport;
    private transient final Preferences pref;
    
    public NewGeometrySettings() {
        propertySupport = new PropertyChangeSupport(this);
        pref = NbPreferences.forModule(NewGeometrySettings.class);
    }
    
    public void open() {
        pref.addPreferenceChangeListener(this);
    }
    
    public static final String PROP_BoxX = "BoxX";

    public float getBoxX() {
        return pref.getFloat(PROP_BoxX, 1.0f);
    }

    public void setBoxX(float value) {
        pref.putFloat(PROP_BoxX, value);
    }

    public static final String PROP_BoxY = "BoxY";

    public float getBoxY() {
        return pref.getFloat(PROP_BoxY, 1.0f);
    }

    public void setBoxY(float value) {
        pref.putFloat(PROP_BoxY, value);
    }

    public static final String PROP_BoxZ = "BoxZ";

    public float getBoxZ() {
        return pref.getFloat(PROP_BoxZ, 1.0f);
    }

    public void setBoxZ(float value) {
        pref.putFloat(PROP_BoxZ, value);
    }

    public static final String PROP_SphereZSamples = "SphereZSamples";

    public int getSphereZSamples() {
        return pref.getInt(PROP_SphereZSamples, 10);
    }

    public void setSphereZSamples(int value) {
        pref.putInt(PROP_SphereZSamples, value);
    }
    
    public static final String PROP_SpherRadialSamples = "SpherRadialSamples";

    public int getSpherRadialSamples() {
        return pref.getInt(PROP_SpherRadialSamples, 10);
    }

    public void setSpherRadialSamples(int value) {
        pref.putInt(PROP_SpherRadialSamples, value);
    }
    
    public static final String PROP_SphereRadius = "SphereRadius";

    public float getSphereRadius() {
        return pref.getFloat(PROP_SphereRadius, 1.0f);
    }

    public void setSphereRadius(float value) {
        pref.putFloat(PROP_SphereRadius, value);
    }
    
    public static final String PROP_SphereUseEvenSlices = "SphereUseEvenSlices";

    public boolean getSphereUseEvenSlices() {
        return pref.getBoolean(PROP_SphereUseEvenSlices, false);
    }

    public void setSphereUseEvenSlices(boolean value) {
        pref.putBoolean(PROP_SphereUseEvenSlices, value);
    }
    
    public static final String PROP_SphereInterior = "SphereInterior";

    public boolean getSphereInterior() {
        return pref.getBoolean(PROP_SphereInterior, false);
    }

    public void setSphereInterior(boolean value) {
        pref.putBoolean(PROP_SphereInterior, value);
    }


            
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    public void preferenceChange(PreferenceChangeEvent evt) {
        propertySupport.firePropertyChange(evt.getKey(), null, evt.getNewValue());
    }

    public void close() {
        pref.removePreferenceChangeListener(this);
    }
    
    
}
