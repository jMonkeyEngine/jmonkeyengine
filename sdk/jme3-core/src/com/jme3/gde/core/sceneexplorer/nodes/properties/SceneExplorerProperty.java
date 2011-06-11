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
package com.jme3.gde.core.sceneexplorer.nodes.properties;

import com.jme3.effect.shapes.EmitterShape;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.core.undoredo.SceneUndoRedoManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.nodes.PropertySupport;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author normenhansen
 */
public class SceneExplorerProperty<T> extends PropertySupport.Reflection<T> {

    protected LinkedList<ScenePropertyChangeListener> listeners = new LinkedList<ScenePropertyChangeListener>();

    public SceneExplorerProperty(T instance, Class valueType, String getter, String setter) throws NoSuchMethodException {
        this(instance, valueType, getter, setter, null);
    }

    public SceneExplorerProperty(T instance, Class valueType, String getter, String setter, ScenePropertyChangeListener listener) throws NoSuchMethodException {
        super(instance, valueType, getter, setter);
        addPropertyChangeListener(listener);
        if (valueType == Vector3f.class) {
            setPropertyEditorClass(Vector3fPropertyEditor.class);
        } else if (valueType == Quaternion.class) {
            setPropertyEditorClass(QuaternionPropertyEditor.class);
        } else if (valueType == Matrix3f.class) {
            setPropertyEditorClass(Matrix3fPropertyEditor.class);
        } else if (valueType == ColorRGBA.class) {
            setPropertyEditorClass(ColorRGBAPropertyEditor.class);
        } else if (valueType == EmitterShape.class) {
            setPropertyEditorClass(EmitterShapePropertyEditor.class);
        }
        for (SceneExplorerPropertyEditor di : Lookup.getDefault().lookupAll(SceneExplorerPropertyEditor.class)) {
            di.setEditor(valueType, this);
        }
    }

    @Override
    public T getValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return super.getValue();
//        try {
//            return SceneApplication.getApplication().enqueue(new Callable<T>() {
//
//                public T call() throws Exception {
//                    return getSuperValue();
//                }
//            }).get();
//        } catch (InterruptedException ex) {
//            Exceptions.printStackTrace(ex);
//        } catch (ExecutionException ex) {
//            Exceptions.printStackTrace(ex);
//        }
//        return null;
    }

    private T getSuperValue() {
        try {
            return super.getValue();
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    @Override
    public void setValue(final T val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        try {
            notifyListeners(getSuperValue(), val);
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    setSuperValue(val);
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void setSuperValue(T val, boolean undo) {
        try {
            if (undo) {
                try {
                    Object oldValue = getSuperValue();
                    if (oldValue.getClass().getMethod("clone", null) != null) {
                        addUndo(oldValue.getClass().getMethod("clone", null).invoke(oldValue, null), val);
                        Logger.getLogger(SceneExplorerProperty.class.getName()).log(Level.INFO, "Add cloned undo {0}", oldValue.getClass().getMethod("clone", null).invoke(oldValue, null));
                    }
                } catch (Exception e) {
                    addUndo(getSuperValue(), val);
                    Logger.getLogger(SceneExplorerProperty.class.getName()).log(Level.INFO, "Add undo {0}", getSuperValue());
                }
            }
            super.setValue(val);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void setSuperValue(T val) {
        setSuperValue(val, true);
    }

    protected void addUndo(final Object before, final Object after) {
        Lookup.getDefault().lookup(SceneUndoRedoManager.class).addEdit(this, new AbstractUndoableSceneEdit() {

            @Override
            public void sceneUndo() {
                Logger.getLogger(SceneExplorerProperty.class.getName()).log(Level.INFO, "Do undo {0}", before);
                setSuperValue((T) before, false);
            }

            @Override
            public void sceneRedo() {
                setSuperValue((T) after, false);
            }

            @Override
            public void awtUndo() {
            }

            @Override
            public void awtRedo() {
            }
        });
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
