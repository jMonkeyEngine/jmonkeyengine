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
package com.jme3.gde.core.assets;

import com.jme3.asset.AssetKey;
import com.jme3.gde.core.util.PropertyUtils;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author normenhansen
 */
public class AssetDataNode extends DataNode {

    public AssetDataNode(DataObject obj, Children ch) {
        super(obj, ch);
    }

    public AssetDataNode(DataObject obj, Children ch, Lookup lookup) {
        super(obj, ch, lookup);
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        AssetData data = getLookup().lookup(AssetData.class);
        if (data == null) {
            return sheet;
        }
        AssetKey<?> key = data.getAssetKey();
        if (key == null) {
            return sheet;
        }
        
        Sheet.Set set = sheet.createPropertiesSet();
        set.setName("AssetKey");
        set.setDisplayName("Conversion Settings");
        for (Field field : key.getClass().getDeclaredFields()) {
            PropertyDescriptor prop = PropertyUtils.getPropertyDescriptor(key.getClass(), field);
            if (prop != null) {
                try {
                    Property sup = new PropertySupport.Reflection(key, prop.getPropertyType(), prop.getReadMethod(), prop.getWriteMethod());
                    sup.setName(prop.getName());
                    sup.setDisplayName(prop.getDisplayName());
                    set.put(sup);
                } catch (Exception e) {
                    Exceptions.printStackTrace(e);
                }
            }
        }
        sheet.put(set);
        return sheet;
    }
}
