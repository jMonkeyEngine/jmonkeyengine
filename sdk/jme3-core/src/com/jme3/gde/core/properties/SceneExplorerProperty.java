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

import com.jme3.effect.influencers.ParticleInfluencer;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.core.undoredo.SceneUndoRedoManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.nodes.PropertySupport;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Mutex;

/**
 *
 * @author normenhansen
 */
@SuppressWarnings("unchecked")
public class SceneExplorerProperty<T> extends PropertySupport.Reflection<T> {

    private static final Logger logger = Logger.getLogger(SceneExplorerProperty.class.getName());
    public static final String PROP_SCENE_CHANGE = "PROP_SCENE_CHANGE";
    public static final String PROP_USER_CHANGE = "PROP_USER_CHANGE";
    public static final String PROP_INIT_CHANGE = "PROP_INIT_CHANGE";
    private T objectLocal;
    private final boolean cloneable;
    private final boolean instantiable;
    private final boolean primitive;
    private final Mutex mutex = new Mutex();
    private boolean inited = false;
    private final boolean editable;
    protected LinkedList<ScenePropertyChangeListener> listeners = new LinkedList<ScenePropertyChangeListener>();

    public SceneExplorerProperty(T instance, Class valueType, String getter, String setter) throws NoSuchMethodException {
        this(instance, valueType, getter, setter, null);
    }

    public SceneExplorerProperty(T instance, Class valueType, String getter, String setter, ScenePropertyChangeListener listener) throws NoSuchMethodException {
        super(instance, valueType, getter, setter);
        primitive = isPrimitive(instance, getter);
        if (!primitive) {
            cloneable = canClone(instance, getter);
            instantiable = canRecreate(instance, getter);
        } else {
            cloneable = false;
            instantiable = false;
        }
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
        } else if (valueType == Vector2f.class) {
            setPropertyEditorClass(Vector2fPropertyEditor.class);
        } else if (valueType == ParticleInfluencer.class) {
            setPropertyEditorClass(ParticleInfluencerPropertyEditor.class);
        }

        for (SceneExplorerPropertyEditor di : Lookup.getDefault().lookupAll(SceneExplorerPropertyEditor.class)) {
            di.setEditor(valueType, this);
        }
        //TODO: instantiates editor?
        editable = getPropertyEditor() != null;
        logger.log(Level.FINE, "Created SceneExplorerProperty for {0},\n cloneable = {1},\n instantiatable = {2},\n primitive = {3},\n editable = {4}", new Object[]{valueType, cloneable, instantiable, primitive, editable});
        addPropertyChangeListener(listener);
    }

    /**
     * synchronizes the local and scene value
     */
    public void syncValue() {
        if (!editable) {
            return;
        }
        final T realValue = getSuperValue();
        if ((objectLocal == null) && !inited) {
            mutex.postWriteRequest(new Runnable() {
                public void run() {
                    inited = true;
                    objectLocal = duplicateObject(realValue);
                    notifyListeners(PROP_INIT_CHANGE, null, objectLocal);
                    logger.log(Level.FINE, "Got first sync duplicate for {0}", objectLocal);
                }
            });
        } else if ((objectLocal != null) && !objectLocal.equals(realValue)) {
            mutex.postWriteRequest(new Runnable() {
                public void run() {
                    T oldObject = objectLocal;
                    T newObject = duplicateObject(realValue);
                    objectLocal = newObject;
                    notifyListeners(PROP_SCENE_CHANGE, oldObject, objectLocal);
                    logger.log(Level.FINE, "Got update for {0} due to equals check", objectLocal);
                }
            });
        } else if ((objectLocal == null) && (realValue != null)) {
            mutex.postWriteRequest(new Runnable() {
                public void run() {
                    objectLocal = duplicateObject(realValue);
                    notifyListeners(PROP_SCENE_CHANGE, null, objectLocal);
                    logger.log(Level.FINE, "Got update for {0} due to change from null", objectLocal);
                }
            });
        }
    }

    @Override
    public T getValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return mutex.readAccess(new Mutex.Action<T>() {
            public T run() {
                logger.log(Level.FINE, "Return local value of {0}", objectLocal);
                return objectLocal;
            }
        });
    }

    @Override
    public void setValue(final T val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        mutex.postWriteRequest(new Runnable() {
            public void run() {
                logger.log(Level.FINE, "Set local value of {0}", objectLocal);
                final T oldObject = objectLocal;
                objectLocal = val;
                final T sceneObject = duplicateObject(val);
                notifyListeners(PROP_USER_CHANGE, oldObject, objectLocal);
                SceneApplication.getApplication().enqueue(new Callable<Void>() {
                    public Void call() throws Exception {
                        setSuperValue(sceneObject);
                        return null;
                    }
                });
            }
        });
    }

    private boolean isPrimitive(Object obj, String getter) {
        try {
            Class objClass = obj.getClass().getMethod(getter).getReturnType();
            if (objClass.isPrimitive()) {
                return true;
            }
        } catch (NoSuchMethodException ex) {
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    private boolean canClone(Object obj, String getter) {
        try {
            Class objClass = obj.getClass().getMethod(getter).getReturnType();
            if (Enum.class.isAssignableFrom(objClass)) {
                logger.log(Level.FINE, "Found enum, not cloneable");
                return false;
            }
            Method meth = objClass.getMethod("clone");
            if (meth != null) {
                logger.log(Level.FINE, "Found clone method");
                if (meth.getParameterTypes().length == 0
                        && meth.getReturnType().isAssignableFrom(objClass)) {
                    return true;
                } else {
                    logger.log(Level.FINE, "Wrong kind of clone method, parameter size {0}, returnType {1}", new Object[]{meth.getParameterTypes().length, meth.getReturnType()});
                }
            }
        } catch (NoSuchMethodException ex) {
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    private boolean canRecreate(Object obj, String getter) {
        try {
            Class objClass = obj.getClass().getMethod(getter).getReturnType();
            if (Enum.class.isAssignableFrom(objClass)) {
                logger.log(Level.FINE, "Found enum, not recreatable");
                return false;
            }
            Constructor[] constructors = objClass.getConstructors();
            for (Constructor constructor : constructors) {
                Class[] types = constructor.getParameterTypes();
                if (types.length == 1 && types[0].isAssignableFrom(objClass)) {
                    return true;
                }
            }
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    private T duplicateObject(T a) {
        if (a == null) {
            return null;
        }
        if (primitive) {
            logger.log(Level.FINE, "Returning primitive as duplicate");
            return a;
        }
        T obj = null;
        if (cloneable) {
            try {
                obj = (T) a.getClass().getMethod("clone").invoke(a);
                logger.log(Level.FINE, "Cloned object {0} to {1}", new Object[]{a, obj});
            } catch (IllegalAccessException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalArgumentException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            } catch (NoSuchMethodException ex) {
                Exceptions.printStackTrace(ex);
            } catch (SecurityException ex) {
                Exceptions.printStackTrace(ex);
            }
        } else if (instantiable) {
            try {
                obj = (T) a.getClass().getConstructor(a.getClass()).newInstance(a);
                logger.log(Level.FINE, "Instantiated new object from {0} to {1}", new Object[]{a, obj});
            } catch (InstantiationException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalAccessException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalArgumentException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            } catch (NoSuchMethodException ex) {
                Exceptions.printStackTrace(ex);
            } catch (SecurityException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        if (obj == null) {
            logger.log(Level.FINE, "Simply returning non-primitive {0} as duplicate", new Object[]{a});
            return a;
        }
        return obj;
    }

    private void setSuperValue(T val) {
        setSuperValue(val, true);
    }

    private T getSuperValue() {
        try {
            logger.log(Level.FINE, "Get super value thread {0}", Thread.currentThread().getName());
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

    private void setSuperValue(T val, boolean undo) {
        try {
            if (undo) {
                logger.log(Level.FINE, "Add undo for {0} on thread {1}");
                addUndo(duplicateObject(getSuperValue()), val);
            }
            logger.log(Level.FINE, "Set super value on thread {0}", Thread.currentThread().getName());
            super.setValue(val);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    protected void addUndo(final Object before, final Object after) {
        SceneUndoRedoManager undoRedo = Lookup.getDefault().lookup(SceneUndoRedoManager.class);
        if (undoRedo == null) {
            logger.log(Level.WARNING, "Cannot access SceneUndoRedoManager");
            return;
        }
        undoRedo.addEdit(this, new AbstractUndoableSceneEdit() {
            @Override
            public void sceneUndo() {
                logger.log(Level.FINE, "Do undo {0}", before);
                notifyListeners(PROP_USER_CHANGE, after, before);
                setSuperValue((T) before, false);
            }

            @Override
            public void sceneRedo() {
                logger.log(Level.FINE, "Do redo {0}", before);
                notifyListeners(PROP_USER_CHANGE, before, after);
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
        if (listener != null) {
            logger.log(Level.FINE, "Add property listener {0}", listener);
            listeners.add(listener);
        }
    }

    public void removePropertyChangeListener(ScenePropertyChangeListener listener) {
        if (listener != null) {
            logger.log(Level.FINE, "Remove property listener {0}", listener);
            listeners.remove(listener);
        }
    }

    private void notifyListeners(final String type, final Object before, final Object after) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                logger.log(Level.FINE, "Notify SceneExplorer listeners");
                for (Iterator<ScenePropertyChangeListener> it = listeners.iterator(); it.hasNext();) {
                    ScenePropertyChangeListener propertyChangeListener = it.next();
                    propertyChangeListener.propertyChange(type, getName(), before, after);
                }
            }
        });
    }
}
