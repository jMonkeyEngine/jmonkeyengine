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

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh.Mode;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 *
 * @author david.bernard.31
 */


public class NewGeometrySettings implements Serializable, PreferenceChangeListener {
    public static enum Plan {
        XY, XZ, YZ
    }
    
    private transient final PropertyChangeSupport propertySupport;
    private transient final Preferences pref;
    
    public NewGeometrySettings() {
        propertySupport = new PropertyChangeSupport(this);
        pref = NbPreferences.forModule(NewGeometrySettings.class);
    }

    // -- Listeners management

    public void open() {
        pref.addPreferenceChangeListener(this);
    }

    public void close() {
        pref.removePreferenceChangeListener(this);
    }

    public void preferenceChange(PreferenceChangeEvent evt) {
        propertySupport.firePropertyChange(evt.getKey(), null, evt.getNewValue());
    }
    
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    //-- Material info

    public static final String PROP_MatRandom = "MatRandom";

    public boolean getMatRandom() {
        return pref.getBoolean(PROP_MatRandom, true);
    }

    public void setMatRandom(boolean value) {
        pref.putBoolean(PROP_MatRandom, value);
    }

    public static final String PROP_MatColor = "MatColor";

    public ColorRGBA getMatColor() {
        ColorRGBA b = new ColorRGBA();
        b.fromIntRGBA(pref.getInt(PROP_MatColor, ColorRGBA.Orange.asIntRGBA()));
        return b;
    }

    public void setMatColor(ColorRGBA value) {
        pref.putInt(PROP_MatColor, value.asIntRGBA());
    }
    


    //-- Box

    public static final String PROP_BoxName = "BoxName";

    public String getBoxName() {
        return pref.get(PROP_BoxName, "Box");
    }

    public void setBoxName(String value) {
        pref.put(PROP_BoxName, value);
    }

    public static final String PROP_BoxMode = "BoxMode";

    public Mode getBoxMode() {
        return getMode(PROP_BoxMode);
    }

    public void setBoxMode(Mode value) {
        putMode(PROP_BoxMode, value);
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

    //-- Sphere
    
    public static final String PROP_SphereName = "SphereName";

    public String getSphereName() {
        return pref.get(PROP_SphereName, "Sphere");
    }

    public void setSphereName(String value) {
        pref.put(PROP_SphereName, value);
    }

    public static final String PROP_SphereMode = "SphereMode";

    public Mode getSphereMode() {
        return getMode(PROP_SphereMode);
    }

    public void setSphereMode(Mode value) {
        putMode(PROP_SphereMode, value);
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

    //-- Line
    public static final String PROP_LineName = "LineName";

    public String getLineName() {
        return pref.get(PROP_LineName, "Line");
    }

    public void setLineName(String value) {
        pref.put(PROP_LineName, value);
    }
        
    public static final String PROP_LineMode = "LineMode";

    public Mode getLineMode() {
        return getMode(PROP_LineMode);
    }

    public void setLineMode(Mode value) {
        putMode(PROP_LineMode, value);
    }

    public static final String PROP_LineStart = "LineStart";

    public Vector3f getLineStart() {
        return getVector3f(PROP_LineStart, new Vector3f(0,0,0));
    }

    public void setLineStart(Vector3f value) {
        putVector3f(PROP_LineStart, value);
    }

    public static final String PROP_LineEnd = "LineEnd";

    public Vector3f getLineEnd() {
        return getVector3f(PROP_LineEnd, new Vector3f(2f,0,2f));
    }

    public void setLineEnd(Vector3f value) {
        putVector3f(PROP_LineEnd, value);
    }

    //-- Quad
    public static final String PROP_QuadName = "QuadName";

    public String getQuadName() {
        return pref.get(PROP_QuadName, "Quad");
    }

    public void setQuadName(String value) {
        pref.put(PROP_QuadName, value);
    }

    public static final String PROP_QuadMode = "QuadMode";

    public Mode getQuadMode() {
        return getMode(PROP_QuadMode);
    }

    public void setQuadMode(Mode value) {
        putMode(PROP_QuadMode, value);
    }

    public static final String PROP_QuadWidth = "QuadWidth";

    public float getQuadWidth() {
        return pref.getFloat(PROP_QuadWidth, 1.0f);
    }

    public void setQuadWidth(float value) {
        pref.putFloat(PROP_QuadWidth, value);
    }

    public static final String PROP_QuadHeight = "QuadHeight";

    public float getQuadHeight() {
        return pref.getFloat(PROP_QuadHeight, 1.0f);
    }

    public void setQuadHeight(float value) {
        pref.putFloat(PROP_QuadHeight, value);
    }

    public static final String PROP_QuadFlipCoords = "QuadFlipCoords";

    public boolean getQuadFlipCoords() {
        return pref.getBoolean(PROP_QuadFlipCoords, false);
    }

    public void setQuadFlipCoords(boolean value) {
        pref.putBoolean(PROP_QuadFlipCoords, value);
    }

    public static final String PROP_QuadPlan = "QuadPlan";

    public Plan getQuadPlan() {
        return Plan.values()[pref.getInt(PROP_QuadPlan, Plan.XZ.ordinal())];
    }

    public void setQuadPlan(Plan value) {
        pref.putInt(PROP_QuadPlan, value.ordinal());
    }

    //-- Tools
        
    protected Vector3f getVector3f(String baseName, Vector3f def) {
        return new Vector3f(
            pref.getFloat(baseName + "X", def.x)
            ,pref.getFloat(baseName + "Y", def.y)
            ,pref.getFloat(baseName + "Z", def.z)
        );
        
    }

    protected void putVector3f(String baseName, Vector3f value) {
        pref.putFloat(baseName + "X", value.x);
        pref.putFloat(baseName + "Y", value.y);
        pref.putFloat(baseName + "Z", value.z);
    }

    protected Mode getMode(String baseName) {
        return Mode.values()[pref.getInt(baseName, Mode.Triangles.ordinal())];
    }

    public void putMode(String baseName, Mode value) {
        pref.putInt(baseName, value.ordinal());
    }
    
    public List<Mode> getModes() {
        return Arrays.asList(Mode.values());
    }
    
    public List<Plan> getPlans() {
        return Arrays.asList(Plan.values());
    }
    
}
