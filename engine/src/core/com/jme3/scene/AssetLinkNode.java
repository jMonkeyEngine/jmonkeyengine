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

package com.jme3.scene;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.util.SafeArrayList;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The AssetLinkNode does not store its children when exported to file.
 * Instead, you can add a list of AssetKeys that will be loaded and attached
 * when the AssetLinkNode is restored.
 * 
 * @author normenhansen
 */
public class AssetLinkNode extends Node {

    protected ArrayList<ModelKey> assetLoaderKeys = new ArrayList<ModelKey>();
    protected Map<ModelKey, Spatial> assetChildren = new HashMap<ModelKey, Spatial>();

    public AssetLinkNode() {
    }

    public AssetLinkNode(ModelKey key) {
        this(key.getName(), key);
    }

    public AssetLinkNode(String name, ModelKey key) {
        super(name);
        assetLoaderKeys.add(key);
    }

    /**
     * Add a "linked" child. These are loaded from the assetManager when the
     * AssetLinkNode is loaded from a binary file.
     * @param key
     */
    public void addLinkedChild(ModelKey key) {
        if (assetLoaderKeys.contains(key)) {
            return;
        }
        assetLoaderKeys.add(key);
    }

    public void removeLinkedChild(ModelKey key) {
        assetLoaderKeys.remove(key);
    }

    public ArrayList<ModelKey> getAssetLoaderKeys() {
        return assetLoaderKeys;
    }

    public void attachLinkedChild(AssetManager manager, ModelKey key) {
        addLinkedChild(key);
        Spatial child = manager.loadAsset(key);
        assetChildren.put(key, child);
        attachChild(child);
    }

    public void attachLinkedChild(Spatial spat, ModelKey key) {
        addLinkedChild(key);
        assetChildren.put(key, spat);
        attachChild(spat);
    }

    public void detachLinkedChild(ModelKey key) {
        Spatial spatial = assetChildren.get(key);
        if (spatial != null) {
            detachChild(spatial);
        }
        removeLinkedChild(key);
        assetChildren.remove(key);
    }

    public void detachLinkedChild(Spatial child, ModelKey key) {
        removeLinkedChild(key);
        assetChildren.remove(key);
        detachChild(child);
    }

    /**
     * Loads the linked children AssetKeys from the AssetManager and attaches them to the Node<br>
     * If they are already attached, they will be reloaded.
     * @param manager
     */
    public void attachLinkedChildren(AssetManager manager) {
        detachLinkedChildren();
        for (Iterator<ModelKey> it = assetLoaderKeys.iterator(); it.hasNext();) {
            ModelKey assetKey = it.next();
            Spatial curChild = assetChildren.get(assetKey);
            if (curChild != null) {
                curChild.removeFromParent();
            }
            Spatial child = manager.loadAsset(assetKey);
            attachChild(child);
            assetChildren.put(assetKey, child);
        }
    }

    public void detachLinkedChildren() {
        Set<Entry<ModelKey, Spatial>> set = assetChildren.entrySet();
        for (Iterator<Entry<ModelKey, Spatial>> it = set.iterator(); it.hasNext();) {
            Entry<ModelKey, Spatial> entry = it.next();
            entry.getValue().removeFromParent();
            it.remove();
        }
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        BinaryImporter importer = BinaryImporter.getInstance();
        AssetManager loaderManager = e.getAssetManager();

        assetLoaderKeys = (ArrayList<ModelKey>) capsule.readSavableArrayList("assetLoaderKeyList", new ArrayList<ModelKey>());
        for (Iterator<ModelKey> it = assetLoaderKeys.iterator(); it.hasNext();) {
            ModelKey modelKey = it.next();
            AssetInfo info = loaderManager.locateAsset(modelKey);
            Spatial child = null;
            if (info != null) {
                child = (Spatial) importer.load(info);
            }
            if (child != null) {
                child.parent = this;
                children.add(child);
                assetChildren.put(modelKey, child);
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Cannot locate {0} for asset link node {1}", 
                                                                    new Object[]{ modelKey, key });
            }
        }
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        SafeArrayList<Spatial> childs = children;
        children = new SafeArrayList<Spatial>(Spatial.class);
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.writeSavableArrayList(assetLoaderKeys, "assetLoaderKeyList", null);
        children = childs;
    }
}
