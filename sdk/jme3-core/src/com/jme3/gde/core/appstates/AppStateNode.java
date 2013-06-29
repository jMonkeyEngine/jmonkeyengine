/*
 * Copyright (c) 2003-2012 jMonkeyEngine
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.appstates;

import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.gde.core.icons.IconList;
import com.jme3.gde.core.properties.SceneExplorerProperty;
import com.jme3.gde.core.properties.ScenePropertyChangeListener;
import com.jme3.gde.core.scene.SceneSyncListener;
import com.jme3.gde.core.util.PropertyUtils;
import java.awt.Image;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node.Property;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class AppStateNode extends AbstractNode implements ScenePropertyChangeListener, SceneSyncListener {

    protected AppState appState;
    protected AppStateManager parent;
//    private static Image icon;
//    private static final String ICON_ENABLED = "com/jme3/gde/core/filters/icons/eye.gif";
//    private static final String ICON_DISABLED = "com/jme3/gde/core/filters/icons/crossedEye.gif";

    public AppStateNode(AppState appState, AppStateManager parent) {
        super(Children.LEAF);
        this.parent = parent;
        this.appState = appState;
        setName(appState.getClass().getName());
        setDisplayName(appState.getClass().getSimpleName());
//        icon = ImageUtilities.loadImage(ICON_ENABLED);
//        setIconBaseWithExtension(ICON_ENABLED);
    }

    @Override
    public Image getIcon(int type) {
        return IconList.wheel.getImage();

    }

    @Override
    public Image getOpenedIcon(int type) {
        return IconList.wheel.getImage();
    }
//
//    public void toggleIcon(boolean enabled) {
//        if (enabled) {
//            icon = ImageUtilities.loadImage(ICON_ENABLED);
//
//        } else {
//            icon = ImageUtilities.loadImage(ICON_DISABLED);
//
//        }
//        fireIconChange();
//    }
//    @Override
//    public Action[] getActions(boolean context) {
//        return new Action[]{
//                    //Actions.alwaysEnabled(new EnableFiterAction(this), "Toggle enabled", "", false),
//                    //                    SystemAction.get(MoveUpAction.class),
//                    //                    SystemAction.get(MoveDownAction.class),
//                    null, //                    SystemAction.get(DeleteAction.class),
//                };
//    }
//
//    @Override
//    public Action getPreferredAction() {
////        return Actions.alwaysEnabled(new EnableFiterAction(this), "Toggle enabled", "", false);
//        return null;
//    }
    public void syncSceneData(float tpf) {
        //TODO: precache structure to avoid locks? Do it backwards, sending the actual bean value?
        for (PropertySet propertySet : getPropertySets()) {
            for (Property<?> property : propertySet.getProperties()) {
                if (property instanceof SceneExplorerProperty) {
                    SceneExplorerProperty prop = (SceneExplorerProperty) property;
                    prop.syncValue();
                }
            }
        }
    }

    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    public void destroy() throws IOException {
        AppStateManagerNode parentNode = (AppStateManagerNode) getParentNode();
        super.destroy();
        parent.detach(appState);
        //TODO:hack
        if (parentNode != null) {
            parentNode.refresh();
        }
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Object obj = appState;
        if (obj == null) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(
                    "AppState unexpectedly null",
                    NotifyDescriptor.WARNING_MESSAGE));
            return sheet;
        }

        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("AppState");
        set.setName(appState.getClass().getName());
        createFields(appState.getClass(), set, obj);
        sheet.put(set);

        return sheet;
    }

    protected Property<?> makeProperty(Object obj, Class returntype, String method, String name) {
        Property<?> prop = null;
        try {
            prop = new SceneExplorerProperty(appState.getClass().cast(obj), returntype, method, null, this);
            prop.setName(name);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }
        return prop;
    }

    protected Property<?> makeProperty(Object obj, Class returntype, String method, String setter, String name) {
        Property<?> prop = null;
        try {
            prop = new SceneExplorerProperty(appState.getClass().cast(obj), returntype, method, setter, this);
            prop.setName(name);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }
        return prop;
    }

    protected void createMethods(Class c, Sheet.Set set, Object obj) throws SecurityException {
        for (Method method : c.getDeclaredMethods()) {
            PropertyDescriptor prop = PropertyUtils.getPropertyDescriptor(c, method);
            if (prop != null) {
                set.put(makeProperty(obj, prop.getPropertyType(), prop.getReadMethod().getName(), prop.getWriteMethod().getName(), prop.getDisplayName()));
            }
        }
    }

    protected void createFields(Class c, Sheet.Set set, Object obj) throws SecurityException {
        for (Field field : c.getDeclaredFields()) {
            PropertyDescriptor prop = PropertyUtils.getPropertyDescriptor(c, field);
            if (prop != null) {
                set.put(makeProperty(obj, prop.getPropertyType(), prop.getReadMethod().getName(), prop.getWriteMethod().getName(), prop.getDisplayName()));
            }
        }
    }

    public void propertyChange(final String type, final String name, final Object before, final Object after) {
        if (SceneExplorerProperty.PROP_USER_CHANGE.equals(type)) {
            firePropertyChange(name, before, after);
        } else if (SceneExplorerProperty.PROP_SCENE_CHANGE.equals(type)) {
            firePropertyChange(name, before, after);
        } else if (SceneExplorerProperty.PROP_INIT_CHANGE.equals(type)) {
            firePropertyChange(name, before, after);
        }
    }
}
