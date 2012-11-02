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

import com.jme3.effect.shapes.EmitterBoxShape;
import com.jme3.effect.shapes.EmitterPointShape;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.math.Vector3f;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.util.Iterator;
import java.util.LinkedList;
import org.openide.awt.StatusDisplayer;

/**
 *
 * @author normenhansen
 */
public class EmitterShapePropertyEditor implements PropertyEditor {

    private LinkedList<PropertyChangeListener> listeners = new LinkedList<PropertyChangeListener>();
    private EmitterShape emitter;// = new EmitterPointShape(Vector3f.ZERO);

    public void setValue(Object value) {
        if (value instanceof EmitterShape) {
            emitter = (EmitterShape) value;
        }
    }

    public Object getValue() {
        return emitter;
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
        if (emitter == null) {
            return "null";
        }
        if (emitter instanceof EmitterBoxShape) {
            EmitterBoxShape shape = (EmitterBoxShape) emitter;
            return "[Box, "
                    + shape.getMin().x
                    + ", "
                    + shape.getMin().y
                    + ", "
                    + shape.getMin().z
                    + ", "
                    + (shape.getMin().x + shape.getLen().x)
                    + ", "
                    + (shape.getMin().y + shape.getLen().y)
                    + ", "
                    + (shape.getMin().z + shape.getLen().z)
                    + "]";
        } else if (emitter instanceof EmitterPointShape) {
            EmitterPointShape shape = (EmitterPointShape) emitter;
            return "[Point, "
                    + shape.getPoint().x
                    + ", "
                    + shape.getPoint().y
                    + ", "
                    + shape.getPoint().z
                    + "]";
        } else if (emitter instanceof EmitterSphereShape) {
            EmitterSphereShape shape = (EmitterSphereShape) emitter;
            return "[Sphere, "
                    + shape.getCenter().x
                    + ", "
                    + shape.getCenter().y
                    + ", "
                    + shape.getCenter().z
                    + ", "
                    + shape.getRadius()
                    + "]";
        }
        return emitter.toString();
    }

    public void setAsText(String text) throws IllegalArgumentException {
        text = text.replace('[', ' ').trim();
        text = text.replace(']', ' ').trim();
        String[] strings = text.split("\\s*(,|\\s)\\s*");
        EmitterShape old=emitter;
        if (strings.length == 0) {
            return;
        }
        if ("box".equals(strings[0].trim().toLowerCase())) {
            if (strings.length == 7) {
                StatusDisplayer.getDefault().setStatusText("try set parameterized box shape");
                emitter = new EmitterBoxShape(
                        new Vector3f(Float.parseFloat(strings[1]), Float.parseFloat(strings[2]), Float.parseFloat(strings[3])), new Vector3f(Float.parseFloat(strings[4]), Float.parseFloat(strings[5]), Float.parseFloat(strings[6])));
            } else {
                StatusDisplayer.getDefault().setStatusText("try set standard box shape");
                emitter = new EmitterBoxShape(new Vector3f(-.5f, -.5f, -.5f), new Vector3f(.5f, .5f, .5f));
            }
        } else if ("point".equals(strings[0].trim().toLowerCase())) {
            if (strings.length == 4) {
                emitter = new EmitterPointShape(
                        new Vector3f(Float.parseFloat(strings[1]), Float.parseFloat(strings[2]), Float.parseFloat(strings[3])));
            } else {
                emitter = new EmitterPointShape(Vector3f.ZERO);
            }
        } else if ("sphere".equals(strings[0].trim().toLowerCase())) {
            if (strings.length == 5) {
                emitter = new EmitterSphereShape(
                        new Vector3f(Float.parseFloat(strings[1]), Float.parseFloat(strings[2]), Float.parseFloat(strings[3])), Float.parseFloat(strings[4]));
            } else {
                emitter = new EmitterSphereShape(Vector3f.ZERO, .5f);
            }
        }
        notifyListeners(old, emitter);
    }

    public String[] getTags() {
//        String[] mats = new String[]{"[Point]", "[Sphere]", "[Box]"};
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

    private void notifyListeners(EmitterShape before, EmitterShape after) {
        for (Iterator<PropertyChangeListener> it = listeners.iterator(); it.hasNext();) {
            PropertyChangeListener propertyChangeListener = it.next();
            //TODO: check what the "programmatic name" is supposed to be here.. for now its Quaternion
            propertyChangeListener.propertyChange(new PropertyChangeEvent(this, null, before, after));
        }
    }
}
