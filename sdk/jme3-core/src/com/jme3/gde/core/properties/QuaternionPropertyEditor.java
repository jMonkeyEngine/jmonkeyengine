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

import com.jme3.math.Quaternion;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author normenhansen
 */
public class QuaternionPropertyEditor implements PropertyEditor {

    private LinkedList<PropertyChangeListener> listeners = new LinkedList<PropertyChangeListener>();
    private Quaternion quaternion = new Quaternion();

    public void setValue(Object value) {
        if (value instanceof Quaternion) {
            quaternion.set((Quaternion) value);
        }
    }

    public Object getValue() {
        return quaternion;
    }

    public boolean isPaintable() {
        return false;
    }

    public void paintValue(Graphics gfx, Rectangle box) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getJavaInitializationString() {
        return null;
    }

    public String getAsText() {
        float[] angles = quaternion.toAngles(new float[3]);
        return "[" + (float) Math.toDegrees(angles[0]) + ", " + (float) Math.toDegrees(angles[1]) + ", " + (float) Math.toDegrees(angles[2]) + "]";
    }

    private void parseInto(String text, Quaternion res) throws IllegalArgumentException {
        text = text.replace('[', ' ');
        text = text.replace(']', ' ').trim();
        String[] a = text.split("\\s*(,|\\s)\\s*");


        if (a.length == 1) {
            if (text.trim().toLowerCase().equals("nan")) {
                res.set(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
                return;
            }
            float f = Float.parseFloat(text);
            f = (float) Math.toRadians(f);
            res.fromAngles(f, f, f);
            return;
        }

        if (a.length == 3) {
            float[] floats = new float[3];
            for (int i = 0; i < a.length; i++) {
                floats[i] = (float) Math.toRadians(Float.parseFloat(a[i]));
            }
            res.fromAngles(floats);
            return;
        }
        throw new IllegalArgumentException("String not correct");
    }

    public void setAsText(String text) throws IllegalArgumentException {
        Quaternion old = new Quaternion();
        old.set(quaternion);
        parseInto(text, quaternion);
        notifyListeners(old, quaternion);
    }

    public String[] getTags() {
        return null;
    }

    public Component getCustomEditor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean supportsCustomEditor() {
        return false;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.add(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Quaternion before, Quaternion after) {
        for (Iterator<PropertyChangeListener> it = listeners.iterator(); it.hasNext();) {
            PropertyChangeListener propertyChangeListener = it.next();
            //TODO: check what the "programmatic name" is supposed to be here.. for now its Quaternion
            propertyChangeListener.propertyChange(new PropertyChangeEvent(this, null, before, after));
        }
    }
}
