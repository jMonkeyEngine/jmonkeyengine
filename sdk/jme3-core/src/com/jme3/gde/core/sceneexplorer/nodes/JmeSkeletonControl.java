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

import com.jme3.animation.SkeletonControl;
import com.jme3.gde.core.icons.IconList;
import java.awt.Image;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;

/**
 *
 * @author normenhansen
 */
@org.openide.util.lookup.ServiceProvider(service = SceneExplorerNode.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public class JmeSkeletonControl extends JmeControl {

    private SkeletonControl skeletonControl;
    private static Image smallImage = IconList.skeletonControl.getImage();

    public JmeSkeletonControl() {
    }

    public JmeSkeletonControl(SkeletonControl skeletonControl, JmeBoneChildren children) {
        super(children);
        this.skeletonControl = skeletonControl;
        control = skeletonControl;
        lookupContents.add(this);
        lookupContents.add(skeletonControl);
        setName("SkeletonControl");
        children.setSkeltonControl(this);
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
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("SkeletonControl");
        set.setName(SkeletonControl.class.getName());
        if (skeletonControl == null) {
            return sheet;
        }

        //  set.put(new AnimationProperty(animControl));

        sheet.put(set);
        return sheet;

    }

    @Override
    public Class getExplorerObjectClass() {
        return SkeletonControl.class;
    }

    @Override
    public Class getExplorerNodeClass() {
        return JmeSkeletonControl.class;
    }

    public SkeletonControl getSkeletonControl() {
        return skeletonControl;
    }

    @Override
    public Node[] createNodes(Object key, DataObject key2, boolean cookie) {
        JmeBoneChildren children = new JmeBoneChildren(null, null);
        JmeSkeletonControl jsc = new JmeSkeletonControl((SkeletonControl) key, children);
        return new Node[]{jsc};
    }
}
