/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.util.SafeArrayList;
import com.jme3.util.clone.Cloner;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
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

    protected ArrayList<ModelKey> assetLoaderKeys = new ArrayList<>();
    protected Map<ModelKey, Spatial> assetChildren = new HashMap<>();

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
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        // This is a change in behavior because the old version did not clone
        // this list... changes to one clone would be reflected in all.
        // I think that's probably undesirable. -pspeed
        this.assetLoaderKeys = cloner.clone(assetLoaderKeys);
        this.assetChildren = new HashMap<ModelKey, Spatial>();
    }

    /**
     * Add a "linked" child. These are loaded from the assetManager when the
     * AssetLinkNode is loaded from a binary file.
     *
     * @param key the ModelKey to add
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
     *
     * @param manager for loading assets
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
    @SuppressWarnings("unchecked")
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);

        final InputCapsule capsule = importer.getCapsule(this);
        final AssetManager assetManager = importer.getAssetManager();

        assetLoaderKeys = capsule.readSavableArrayList("assetLoaderKeyList", new ArrayList<>());

        for (final Iterator<ModelKey> iterator = assetLoaderKeys.iterator(); iterator.hasNext(); ) {

            final ModelKey modelKey = iterator.next();
            final Spatial child = assetManager.loadAsset(modelKey);

            if (child != null) {
                child.parent = this;
                children.add(child);
                assetChildren.put(modelKey, child);
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
                        "Cannot locate {0} for asset link node {1}", new Object[]{modelKey, key});
            }
        }
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        SafeArrayList<Spatial> childList = children;
        children = new SafeArrayList<>(Spatial.class);
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.writeSavableArrayList(assetLoaderKeys, "assetLoaderKeyList", null);
        children = childList;
    }
}
