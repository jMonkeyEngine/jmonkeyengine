/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.scene.plugins.blender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.BlenderKey;
import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.scene.LightNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;

/**
 * This is the main loading class. Have in notice that asset manager needs to have loaders for resources like textures.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class BlenderModelLoader extends BlenderLoader {

    private static final Logger LOGGER = Logger.getLogger(BlenderModelLoader.class.getName());

    @Override
    public Spatial load(AssetInfo assetInfo) throws IOException {
        try {
            this.setup(assetInfo);

            BlenderKey blenderKey = blenderContext.getBlenderKey();
            List<Node> rootObjects = new ArrayList<Node>();
            for (FileBlockHeader block : blocks) {
                if (block.getCode() == FileBlockHeader.BLOCK_OB00) {
                    ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
                    Object object = objectHelper.toObject(block.getStructure(blenderContext), blenderContext);
                    if (object instanceof LightNode && (blenderKey.getFeaturesToLoad() & FeaturesToLoad.LIGHTS) != 0) {
                        rootObjects.add((LightNode) object);
                    } else if (object instanceof Node && (blenderKey.getFeaturesToLoad() & FeaturesToLoad.OBJECTS) != 0) {
                        LOGGER.log(Level.FINE, "{0}: {1}--> {2}", new Object[] { ((Node) object).getName(), ((Node) object).getLocalTranslation().toString(), ((Node) object).getParent() == null ? "null" : ((Node) object).getParent().getName() });
                        if (((Node) object).getParent() == null) {
                            rootObjects.add((Node) object);
                        }
                    }
                }
            }

            // bake constraints after everything is loaded
            ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
            constraintHelper.bakeConstraints(blenderContext);

            // attach the nodes to the root node at the very end so that the root objects have no parents during constraint applying
            LOGGER.fine("Creating the root node of the model and applying loaded nodes of the scene to it.");
            Node modelRoot = new Node(blenderKey.getName());
            for (Node node : rootObjects) {
                if (node instanceof LightNode) {
                    modelRoot.addLight(((LightNode) node).getLight());
                }
                modelRoot.attachChild(node);
            }

            return modelRoot;
        } catch (BlenderFileException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            throw new IOException("Unexpected importer exception occured: " + e.getLocalizedMessage(), e);
        } finally {
            this.clear();
        }
    }
}
