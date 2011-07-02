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
package com.jme3.gde.core.filters;

import com.jme3.gde.core.filters.actions.EnableFiterAction;
import com.jme3.gde.core.properties.SceneExplorerProperty;
import com.jme3.gde.core.properties.ScenePropertyChangeListener;
import com.jme3.gde.core.util.PropertyUtils;
import com.jme3.post.Filter;

import java.awt.Image;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import javax.swing.Action;
import org.openide.actions.DeleteAction;
import org.openide.actions.MoveDownAction;
import org.openide.actions.MoveUpAction;
import org.openide.awt.Actions;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author normenhansen
 */
@SuppressWarnings("unchecked")
public abstract class AbstractFilterNode extends AbstractNode implements FilterNode, ScenePropertyChangeListener {

    protected boolean readOnly = false;
    protected DataObject dataObject;
    protected Filter filter;
    private static Image icon;
    private static final String ICON_ENABLED = "com/jme3/gde/core/filters/icons/eye.gif";
    private static final String ICON_DISABLED = "com/jme3/gde/core/filters/icons/crossedEye.gif";

    @Override
    public Image getIcon(int type) {
        return icon;

    }

    @Override
    public Image getOpenedIcon(int type) {
        return icon;
    }

    public void toggleIcon(boolean enabled) {
        if (enabled) {
            icon = ImageUtilities.loadImage(ICON_ENABLED);

        } else {
            icon = ImageUtilities.loadImage(ICON_DISABLED);

        }
        fireIconChange();
    }

    public AbstractFilterNode() {
        super(Children.LEAF);
    }

    public AbstractFilterNode(Filter filter) {
        super(Children.LEAF);
        this.filter = filter;
        setName(filter.getName());
        icon = ImageUtilities.loadImage(ICON_ENABLED);
        setIconBaseWithExtension(ICON_ENABLED);

    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[]{
                    Actions.alwaysEnabled(new EnableFiterAction(this), "Toggle enabled", "", false),
                    SystemAction.get(MoveUpAction.class),
                    SystemAction.get(MoveDownAction.class),
                    null,
                    SystemAction.get(DeleteAction.class),
                    
                };
    }

    @Override
    public Action getPreferredAction() {
        return Actions.alwaysEnabled(new EnableFiterAction(this), "Toggle enabled", "", false);
    }

    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    public void destroy() throws IOException {
        super.destroy();
        FilterPostProcessorNode nod = (FilterPostProcessorNode) getParentNode();
        nod.removeFilter(filter);
        fireSave(true);
    }

    protected void fireSave(boolean modified) {
        if (dataObject != null) {
            dataObject.setModified(true);
        }
    }

    /**
     * returns the PropertySet with the given name (mostly Class.name)
     * @param name
     * @return The PropertySet or null if no PropertySet by that name exists
     */
    public PropertySet getPropertySet(String name) {
        for (int i = 0; i < getPropertySets().length; i++) {
            PropertySet propertySet = getPropertySets()[i];
            if (propertySet.getName().equals(name)) {
                return propertySet;
            }
        }
        return null;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("Filter");
        set.setName("Filter");
        Filter obj = filter;
        if (obj == null) {
            return sheet;
        }
        createFields(Filter.class, set, obj);
        sheet.put(set);
        return sheet;

    }

    /**
     * @param saveCookie the saveCookie to set
     */
    public AbstractFilterNode setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public void refreshProperties() {
        setSheet(createSheet());
    }

    protected Property<?> makeProperty(Object obj, Class returntype, String method, String name) {
        Property<?> prop = null;
        try {
            prop = new SceneExplorerProperty(getExplorerObjectClass().cast(obj), returntype, method, null);
            prop.setName(name);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }
        return prop;
    }

    protected Property<?> makeProperty(Object obj, Class returntype, String method, String setter, String name) {
        Property<?> prop = null;
        try {
            if (readOnly) {
                prop = new SceneExplorerProperty(getExplorerObjectClass().cast(obj), returntype, method, null);
            } else {
                prop = new SceneExplorerProperty(getExplorerObjectClass().cast(obj), returntype, method, setter, this);
            }
            prop.setName(name);

        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }
        return prop;
    }

    protected void createFields(Class c, Sheet.Set set, Object obj) throws SecurityException {
        for (Field field : c.getDeclaredFields()) {
            PropertyDescriptor prop = PropertyUtils.getPropertyDescriptor(c, field);
            if (prop != null) {
                set.put(makeProperty(obj, prop.getPropertyType(), prop.getReadMethod().getName(), prop.getWriteMethod().getName(), prop.getDisplayName()));
            }
        }
    }

    public void propertyChange(final String name, final Object before, final Object after) {
        if (name.equals("Enabled")) {
            toggleIcon((Boolean) after);
        }
        fireSave(true);
        firePropertyChange(name, before, after);
    }

    public Filter getFilter() {
        return filter;
    }

    public abstract Class<?> getExplorerObjectClass();

    public abstract Node[] createNodes(Object key, DataObject dataObject, boolean readOnly);
}
