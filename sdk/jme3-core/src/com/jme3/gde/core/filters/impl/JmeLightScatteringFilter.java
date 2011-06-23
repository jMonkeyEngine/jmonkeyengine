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
package com.jme3.gde.core.filters.impl;

import com.jme3.gde.core.filters.AbstractFilterNode;
import com.jme3.gde.core.filters.FilterNode;
import com.jme3.math.Vector3f;
import com.jme3.post.filters.LightScatteringFilter;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = FilterNode.class)
public class JmeLightScatteringFilter extends AbstractFilterNode {

    public JmeLightScatteringFilter() {
    }

    public JmeLightScatteringFilter(LightScatteringFilter filter, DataObject object, boolean readOnly) {
        super(filter);
        this.dataObject = object;
        this.readOnly = readOnly;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("Blur");
        set.setName(Node.class.getName());
        LightScatteringFilter obj = (LightScatteringFilter) filter;
        if (obj == null) {
            return sheet;
        }
        set.put(makeProperty(obj, float.class, "getBlurStart", "setBlurStart", "Blur Start"));
        set.put(makeProperty(obj, float.class, "getBlurWidth", "setBlurWidth", "Blur Width"));
        set.put(makeProperty(obj, float.class, "getLightDensity", "setLightDensity", "Light Density"));
        set.put(makeProperty(obj, Vector3f.class, "getLightPosition", "setLightPosition", "Light Position"));
        set.put(makeProperty(obj, int.class, "getNbSamples", "setNbSamples", "Sample Number"));
        sheet.put(set);
        return sheet;

    }

    @Override
    public Class<?> getExplorerObjectClass() {
        return LightScatteringFilter.class;
    }

    @Override
    public Node[] createNodes(Object key, DataObject dataObject, boolean readOnly) {
        return new Node[]{new JmeLightScatteringFilter((LightScatteringFilter) key, dataObject, readOnly)};
    }
}
