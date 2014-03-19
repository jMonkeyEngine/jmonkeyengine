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
 * This class allows editing jME scene values in property sheets in a threadsafe
 * manner. The getter and setter are called via reflection and changes in the
 * properties can be listed for.
 *
 * @author normenhansen
 */
@SuppressWarnings("unchecked")
public class SceneExplorerProperty<T> extends PropertySupport.Reflection<T> {

    protected static final Logger logger = Logger.getLogger(SceneExplorerProperty.class.getName());
    /**
     * Change that was caused by the user editing a property value.
     */
    public static final String PROP_USER_CHANGE = "PROP_USER_CHANGE";
    /**
     * Change that was caused by the scene as opposed to user input.
     */
    public static final String PROP_SCENE_CHANGE = "PROP_SCENE_CHANGE";
    /**
     * Change that was caused by the initial data scan on the object. Note that
     * this should normally not trigger any real "changes" but only make any
     * display items rescan the data so its displaying the actual scene value
     * and not the initialiation value.
     */
    public static final String PROP_INIT_CHANGE = "PROP_INIT_CHANGE";
    protected T objectLocal;
    protected boolean changing = false;
    protected final boolean cloneable;
    protected final boolean instantiable;
    protected final boolean primitive;
    protected final Mutex mutex = new Mutex();
    protected boolean inited = false;
    protected final boolean editable;
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
        editable = this.canWrite();
        logger.log(Level.FINE, "Created SceneExplorerProperty for {0},\n cloneable = {1},\n instantiatable = {2},\n primitive = {3},\n editable = {4}", new Object[]{valueType, cloneable, instantiable, primitive, editable});
        addPropertyChangeListener(listener);
    }

    /**
     * Synchronizes the local and scene value, has to be called on render
     * thread.
     */
    public void syncValue() {
        final T realValue = getSuperValue();
        mutex.readAccess(new Runnable() {
            public void run() {
                if (changing) {
                    return;
                }
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
        });
    }

    /**
     * Gets the current value, its a duplicate of the actual scene value. Note
     * that the value is most probably not initialized yet when the Propety is
     * created! Listen for PROP_INIT_CHANGE events to get a callback when the
     * value is first read from the scene.
     *
     * @return
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Override
    public T getValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return mutex.readAccess(new Mutex.Action<T>() {
            public T run() {
                logger.log(Level.FINE, "Return local value of {0}", objectLocal);
                return objectLocal;
            }
        });
    }

    /**
     * Stores the given value locally and applies a duplicate to the scene
     * object on the render thread.
     *
     * @param val
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Override
    public void setValue(final T val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        mutex.postWriteRequest(new Runnable() {
            public void run() {
                logger.log(Level.FINE, "Set local value to {0}", val);
                final T oldObject = objectLocal;
                changing = true;
                objectLocal = val;
                final T sceneObject = duplicateObject(val);
                SceneApplication.getApplication().enqueue(new Callable<Void>() {
                    public Void call() throws Exception {
                        mutex.postWriteRequest(new Runnable() {
                            public void run() {
                                setSuperValue(sceneObject);
                                changing = false;
                            }
                        });
                        return null;
                    }
                });
                //call listeners after enqueueing our own change.
                notifyListeners(PROP_USER_CHANGE, oldObject, objectLocal);
            }
        });
    }

    /**
     * Checks if a getters return object is a primitive.
     *
     * @param obj
     * @param getter
     * @return
     */
    protected boolean isPrimitive(Object obj, String getter) {
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

    /**
     * Checks if a getters return object can be cloned
     *
     * @param obj
     * @param getter
     * @return
     */
    protected boolean canClone(Object obj, String getter) {
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

    /**
     * Checks if a getters return object can be recreated by calling a
     * constructor of the object with the old object as parameter.
     *
     * @param obj
     * @param getter
     * @return
     */
    protected boolean canRecreate(Object obj, String getter) {
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

    /**
     * Duplicates an object based on the gathered info about cloneability etc.
     *
     * @param a
     * @return
     */
    protected T duplicateObject(T a) {
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

    /**
     * Calls the actual scene objects setter with the passed object. Adds an
     * undo step automatically.
     *
     * @param val
     */
    protected void setSuperValue(T val) {
        setSuperValue(val, true);
    }

    /**
     * Calls the actual scene objects getter and returns the return value.
     *
     * @return
     */
    protected T getSuperValue() {
        try {
            logger.log(Level.FINER, "Get super value thread {0}", Thread.currentThread().getName());
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

    /**
     * Calls the actual scene objects setter with the passed object.
     *
     * @param val
     * @param undo If an undo item should be created or not
     */
    protected void setSuperValue(T val, boolean undo) {
        try {
            if (undo) {
                T dupe = duplicateObject(getSuperValue());
                logger.log(Level.FINE, "Add undo for {0}", dupe);
                addUndo(dupe, val);
            }
            logger.log(Level.FINER, "Set super value on thread {0}", Thread.currentThread().getName());
            super.setValue(val);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Adds an undo item for a change in this value.
     *
     * @param before
     * @param after
     */
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

    /**
     * Add a ScenePropertyChangeListener to listen for changes of this propety.
     * See the "PROP_XXX" properties for a list of change callback types.
     *
     * @param listener
     */
    public void addPropertyChangeListener(ScenePropertyChangeListener listener) {
        if (listener != null) {
            logger.log(Level.FINE, "Add property listener {0}", listener);
            listeners.add(listener);
        }
    }

    /**
     * Removes a ScenePropertyChangeListener that was listening for changes of
     * this propety.
     */
    public void removePropertyChangeListener(ScenePropertyChangeListener listener) {
        if (listener != null) {
            logger.log(Level.FINE, "Remove property listener {0}", listener);
            listeners.remove(listener);
        }
    }

    /**
     * Notify all ScenePropertyChangeListeners about a change in the property
     * @param type
     * @param before
     * @param after 
     */
    protected void notifyListeners(final String type, final Object before, final Object after) {
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
