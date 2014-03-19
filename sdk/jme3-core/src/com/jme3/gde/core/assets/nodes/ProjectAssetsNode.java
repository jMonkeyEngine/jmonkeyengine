/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.gde.core.assets.nodes;

import com.jme3.gde.core.assets.AssetDataObject;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.icons.IconList;
import java.awt.Image;
import org.netbeans.api.project.Project;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author normenhansen
 */
public class ProjectAssetsNode extends FilterNode {

    private static Image smallImage = IconList.asset.getImage();

    public ProjectAssetsNode(ProjectAssetManager manager, Project proj, Node node) throws DataObjectNotFoundException {
        super(node, new AssetChildren(manager, node), createLookupProxy(manager, node));
//        for (Iterator<Class<? extends Object>> it = proj.getLookup().lookupResult(Object.class).allClasses().iterator(); it.hasNext();) {
//            Class<? extends Object> class1 = it.next();
//            System.out.println("Clazz: "+class1.getName());
//        }
//        proj.getLookup().lookup(ProjectClassPathProvider.class).findClassPath(null, ClassPath.EXECUTE);
//        proj.getLookup().lookup(AntProjectHelper.class).getProperties(PROP_NAME);
//        proj.getLookup().lookup(ProjectClassPathModifier.class).#
        enableDelegation(DELEGATE_GET_ACTIONS);
        enableDelegation(DELEGATE_GET_CONTEXT_ACTIONS);
        setDisplayName("Project Assets");
    }

    public static Lookup createLookupProxy(ProjectAssetManager manager, Node node) {
        DataObject obj = node.getLookup().lookup(DataObject.class);
        if (obj instanceof AssetDataObject && obj.getLookup().lookup(ProjectAssetManager.class) == null) {
            ((AssetDataObject) obj).getLookupContents().add(manager);
        }
        return new ProxyLookup(
                new Lookup[]{
                    node.getLookup()
                    /*,Lookups.fixed(manager)*/
                });
    }

    @Override
    public Image getIcon(int type) {
        return smallImage;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return smallImage;
    }
}
