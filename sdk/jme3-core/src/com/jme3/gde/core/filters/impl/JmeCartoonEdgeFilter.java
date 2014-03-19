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
import com.jme3.post.filters.CartoonEdgeFilter;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = FilterNode.class)
public class JmeCartoonEdgeFilter extends AbstractFilterNode {

    public JmeCartoonEdgeFilter() {
    }

    public JmeCartoonEdgeFilter(CartoonEdgeFilter filter, DataObject object, boolean readOnly) {
        super(filter);
        this.dataObject = object;
        this.readOnly = readOnly;
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("CartoonEdge");
        set.setName(Node.class.getName());
        CartoonEdgeFilter obj = (CartoonEdgeFilter) filter;
        if (obj == null) {
            return sheet;
        }
//        set.put(makeProperty(obj, float.class, "getDepthSensitivity", "setDepthSensitivity", "Depth Sensitivity"));
//        set.put(makeProperty(obj, float.class, "getDepthThreshold", "setDepthThreshold", "Depth Threshold"));
//        set.put(makeProperty(obj, ColorRGBA.class, "getEdgeColor", "setEdgeColor", "Edge Color"));
//        set.put(makeProperty(obj, float.class, "getEdgeIntensity", "setEdgeIntensity", "Edge Intensity"));
//        set.put(makeProperty(obj, float.class, "getEdgeWidth", "setEdgeWidth", "Edge Width"));
//        set.put(makeProperty(obj, float.class, "getNormalSensitivity", "setNormalSensitivity", "Normal Sensitivity"));
//        set.put(makeProperty(obj, float.class, "getNormalThreshold", "setNormalThreshold", "Normal Threshold"));
        createFields(CartoonEdgeFilter.class, set, obj);
        sheet.put(set);
        return sheet;

    }

    @Override
    public Class<?> getExplorerObjectClass() {
        return CartoonEdgeFilter.class;
    }

    @Override
    public Node[] createNodes(Object key, DataObject dataObject, boolean readOnly) {
        return new Node[]{new JmeCartoonEdgeFilter((CartoonEdgeFilter) key, dataObject, readOnly)};
    }
}
