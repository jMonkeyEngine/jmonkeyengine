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
package com.jme3.gde.core.sceneexplorer.nodes;

import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.properties.SceneExplorerProperty;
import com.jme3.gde.core.sceneexplorer.nodes.properties.ScenePropertyChangeListener;
import com.jme3.gde.core.util.PropertyUtils;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.awt.Image;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import org.openide.actions.DeleteAction;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author normenhansen
 */
public class JmeGenericControl extends AbstractNode implements ScenePropertyChangeListener {

    private Control control;
    private static Image smallImage =
            ImageUtilities.loadImage("com/jme3/gde/core/sceneexplorer/nodes/icons/animationcontrol.gif");
    private DataObject dobject;
    private InstanceContent lookupContents;

    public JmeGenericControl(Control control, DataObject dataObject) {
        //TODO: lookup content! (control etc)
        super(Children.LEAF, new ProxyLookup(dataObject.getLookup(), new SceneExplorerLookup(new InstanceContent())));
        lookupContents = getLookup().lookup(SceneExplorerLookup.class).getInstanceContent();
        this.control = control;
        this.dobject = dataObject;
        lookupContents.add(this);
        lookupContents.add(control);
        setName(control.getClass().getName());
    }

    @Override
    public Image getIcon(int type) {
        return super.getIcon(type);
//        return smallImage;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return super.getOpenedIcon(type);
//        return smallImage;
    }

    @Override
    protected Sheet createSheet() {
        //TODO: multithreading..
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("Control");
        set.setName(Control.class.getName());
        if (control == null) {
            return sheet;
        }

        createFields(control.getClass(), set, control);

        sheet.put(set);
        return sheet;

    }

    @Override
    public Action[] getActions(boolean context) {
        return new SystemAction[]{
                    //                    SystemAction.get(CopyAction.class),
                    //                    SystemAction.get(CutAction.class),
                    //                    SystemAction.get(PasteAction.class),
                    SystemAction.get(DeleteAction.class)
                };
    }

    @Override
    public boolean canDestroy() {
        return true;
    }

    @Override
    public void destroy() throws IOException {
        super.destroy();
        final Spatial spat = getParentNode().getLookup().lookup(Spatial.class);
        try {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    spat.removeControl(control);
                    return null;
                }
            }).get();
            ((AbstractSceneExplorerNode) getParentNode()).refresh(true);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    protected Property makeProperty(Object obj, Class returntype, String method, String name) {
        Property prop = null;
        try {
            prop = new SceneExplorerProperty(control.getClass().cast(obj), returntype, method, null);
            prop.setName(name);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        }
        return prop;
    }

    protected Property makeProperty(Object obj, Class returntype, String method, String setter, String name) {
        Property prop = null;
        try {
//            if (readOnly) {
//                prop = new SceneExplorerProperty(control.getClass().cast(obj), returntype, method, null);
//            } else {
                prop = new SceneExplorerProperty(control.getClass().cast(obj), returntype, method, setter, this);
//            }
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

    public void propertyChange(String property, Object oldValue, Object newValue) {
        dobject.setModified(true);
//        throw new UnsupportedOperationException("Not supported yet.");
    }
}
