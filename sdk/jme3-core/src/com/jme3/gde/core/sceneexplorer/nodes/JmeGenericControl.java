/*
 *  Copyright (c) 2009-2016 jMonkeyEngine
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

import com.jme3.gde.core.icons.IconList;
import com.jme3.scene.control.Control;
import java.awt.Image;
import org.openide.loaders.DataObject;
import org.openide.nodes.Sheet;

/**
 *
 * @author normenhansen
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class JmeGenericControl extends JmeControl {

    private static final Image smallImage = IconList.wheel.getImage();

    public JmeGenericControl(Control control, DataObject dataObject) {
        //TODO: lookup content! (control etc)
        super(dataObject);
        this.control = control;
        addToLookup(this);
        addToLookup(control);
        this.control = control;
        setName(control.getClass().getSimpleName());
    }

    @Override
    public Image getIcon(int type) {
        return smallImage;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return smallImage;
    }

    @Override
    protected Sheet createSheet() {
        //TODO: multithreading..
        Sheet sheet = Sheet.createDefault();
        
        if (control == null) {
            return sheet;
        }
        putSheetSets(sheet, control.getClass(), false);
        return sheet;

    }

    /**
     * Append one Sheet.Set of fields per class,
     * recursively to the root class of the hierarchie.
     *
     * @param sheet Sheet where to put.
     * @param c current class to start add.
     * @param hidden sheet for c is hidden (parent are hidden = true)
     */
    protected void putSheetSets(Sheet sheet, Class c, boolean hidden) {
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName(String.format("%s - %s", c.getSimpleName(), c.getPackage().getName()));
        set.setName(c.getName());
        set.setHidden(hidden);
        createFields(c, set, control);
        
        Class parent = c.getSuperclass();
        if (parent != null && !parent.equals(Object.class)) {
            putSheetSets(sheet, parent, true);
        }
        sheet.put(set);
    }

    @Override
    public boolean canDestroy() {
        return true;
    }
 
    @Override
    public Class getExplorerObjectClass() {
        return control.getClass();
    }
}
