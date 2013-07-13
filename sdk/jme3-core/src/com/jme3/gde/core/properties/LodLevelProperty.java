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
import com.jme3.scene.Geometry;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 *
 * @author Nehon
 */
public class LodLevelProperty extends SceneExplorerProperty {

    ComboBoxPropertyEditor editor = null;
    int hash = 0;
    private Geometry geometry;

    public LodLevelProperty(Object instance, Class valueType, String getter, String setter) throws NoSuchMethodException {
        super(instance, valueType, getter, setter);
        geometry = (Geometry) instance;
    }

    public LodLevelProperty(Object instance, Class valueType, String getter, String setter, ScenePropertyChangeListener listener) throws NoSuchMethodException {
        super(instance, valueType, getter, setter, listener);
        geometry = (Geometry) instance;
    }

    @Override
    public PropertyEditor getPropertyEditor() {
        if (editor == null) {
            List<String> list = makeList();
            editor = new ComboBoxPropertyEditor(list);
        }
        return editor;
    }

    private void refresh() {
        List<String> list = makeList();
        editor.setList(list);
    }

    @Override
    public String getName() {
        return "Lod Level";
    }

    @Override
    public Object getValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (hash != computeHash()) {
            refresh();
        }
        return geometry.getLodLevel();
    }

    @Override
    public void setValue(Object val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final int level = Integer.parseInt(((String) val).split(" - ")[0]);
        SceneApplication.getApplication().enqueue(new Callable<Void>() {
            public Void call() throws Exception {
                if (geometry.getMesh().getNumLodLevels() > level) {
                    geometry.setLodLevel(level);
                }
                return null;
            }
        });

    }

    private List<String> makeList() {
        List<String> list = new ArrayList<String>();
        if (geometry.getMesh().getNumLodLevels() > 0) {
            for (int i = 0; i < geometry.getMesh().getNumLodLevels(); i++) {
                int triNum = geometry.getMesh().getLodLevel(i).getNumElements();
                list.add(i + " - " + triNum + " triangles");
            }
        } else {
            list.add("0 - " + geometry.getMesh().getTriangleCount() + " triangles");
        }

        hash = computeHash();
        return list;
    }

    private int computeHash() {
        if (geometry.getMesh().getNumLodLevels() > 0) {
            return geometry.getMesh().getLodLevel(geometry.getMesh().getNumLodLevels() - 1).hashCode();
        }
        return 0;
    }
}
