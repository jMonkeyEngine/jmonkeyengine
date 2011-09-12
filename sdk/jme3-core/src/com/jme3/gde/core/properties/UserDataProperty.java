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

import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.scene.Spatial;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.nodes.PropertySupport;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class UserDataProperty extends PropertySupport.ReadWrite<String> {

    private Spatial spatial;
    private String name = "null";
    private int type = 0;
    private List<ScenePropertyChangeListener> listeners = new LinkedList<ScenePropertyChangeListener>();

    public UserDataProperty(Spatial node, String name) {
        super(name, String.class, name, "");
        this.spatial = node;
        this.name = name;
        this.type = getObjectType(node.getUserData(name));
    }

    public static int getObjectType(Object type) {
        if (type instanceof Integer) {
            return 0;
        } else if (type instanceof Float) {
            return 1;
        } else if (type instanceof Boolean) {
            return 2;
        } else if (type instanceof String) {
            return 3;
        } else if (type instanceof Long) {
            return 4;
        } else {
            Logger.getLogger(UserDataProperty.class.getName()).log(Level.WARNING, "UserData not editable" + type.getClass());
            return -1;
        }
    }

    @Override
    public String getValue() throws IllegalAccessException, InvocationTargetException {
        return spatial.getUserData(name) + "";
    }

    @Override
    public void setValue(final String val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (spatial == null) {
            return;
        }
        try {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    switch (type) {
                        case 0:
                            spatial.setUserData(name, Integer.parseInt(val));
                            break;
                        case 1:
                            spatial.setUserData(name, Float.parseFloat(val));
                            break;
                        case 2:
                            spatial.setUserData(name, Boolean.parseBoolean(val));
                            break;
                        case 3:
                            spatial.setUserData(name, val);
                            break;
                        case 4:
                            spatial.setUserData(name, Long.parseLong(val));
                            break;
                        default:
//                            throw new UnsupportedOperationException();
                    }
                    return null;
                }
            }).get();
            notifyListeners(null, val);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public PropertyEditor getPropertyEditor() {
        return null;
//        return new AnimationPropertyEditor(control);
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
            propertyChangeListener.propertyChange(getName(), before, after);
        }
    }
}
