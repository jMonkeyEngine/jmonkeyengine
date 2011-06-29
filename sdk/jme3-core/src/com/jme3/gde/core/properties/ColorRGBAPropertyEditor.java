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

import com.jme3.math.ColorRGBA;
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
public class ColorRGBAPropertyEditor implements PropertyEditor {

    private LinkedList<PropertyChangeListener> listeners = new LinkedList<PropertyChangeListener>();
    private ColorRGBA color = new ColorRGBA();

    public void setValue(Object value) {
        if (value instanceof ColorRGBA) {
            color.set((ColorRGBA) value);
        }
    }

    public Object getValue() {
        return color;
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
        return "[" + color.r + ", " + color.g + ", " + color.b + ", " + color.a + "]";
    }

    public void setAsText(String text) throws IllegalArgumentException {
        text = text.replace('[', ' ');
        text = text.replace(']', ' ');
        String[] values = text.split(",");
        ColorRGBA old=color;
        if (values.length != 4) {
            throw (new IllegalArgumentException("String not correct"));
        }
        float[] floats = new float[4];
        for (int i = 0; i < values.length; i++) {
            String string = values[i];
            floats[i] = Float.parseFloat(string);
        }
        color.set(floats[0], floats[1], floats[2], floats[3]);
        notifyListeners(old, color);
    }

    public String[] getTags() {
        return null;
    }

    public Component getCustomEditor() {
        ColorRGBADialog dialog = new ColorRGBADialog(null, true, this);
        dialog.setLocationRelativeTo(null);
        return dialog;
    }

    public boolean supportsCustomEditor() {
        return true;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.add(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    public void notifyListeners(ColorRGBA before, ColorRGBA after) {
        for (Iterator<PropertyChangeListener> it = listeners.iterator(); it.hasNext();) {
            PropertyChangeListener propertyChangeListener = it.next();
            //TODO: check what the "programmatic name" is supposed to be here.. for now its ColorRGBA
            propertyChangeListener.propertyChange(new PropertyChangeEvent(this, null, before, after));
        }
    }
}
