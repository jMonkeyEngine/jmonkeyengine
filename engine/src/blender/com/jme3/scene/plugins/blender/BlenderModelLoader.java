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
package com.jme3.scene.plugins.blender;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.BlenderKey;
import com.jme3.light.Light;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the main loading class. Have in notice that asset manager needs to have loaders for resources like textures.
 * @author Marcin Roguski
 */
public class BlenderModelLoader extends BlenderLoader {

    private static final Logger LOGGER = Logger.getLogger(BlenderModelLoader.class.getName());

    @Override
    public Spatial load(AssetInfo assetInfo) throws IOException {
        try {
            this.setup(assetInfo);
            
            BlenderKey blenderKey = blenderContext.getBlenderKey();
            Node modelRoot = new Node(blenderKey.getName());
            
            for (FileBlockHeader block : blocks) {
                if (block.getCode() == FileBlockHeader.BLOCK_OB00) {
                    Object object = this.toObject(block.getStructure(blenderContext));
                    if (object instanceof Node) {
                        LOGGER.log(Level.INFO, "{0}: {1}--> {2}", new Object[]{((Node) object).getName(), ((Node) object).getLocalTranslation().toString(), ((Node) object).getParent() == null ? "null" : ((Node) object).getParent().getName()});
                        if (((Node) object).getParent() == null) {
                            modelRoot.attachChild( (Node) object );
                        }
                    }else if (object instanceof Light){
                        modelRoot.addLight( (Light) object );
                    }
                }
            }
            blenderContext.dispose();
            return modelRoot;
        } catch (BlenderFileException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }
}
