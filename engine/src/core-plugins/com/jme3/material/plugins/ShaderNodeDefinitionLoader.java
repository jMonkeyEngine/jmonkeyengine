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
package com.jme3.material.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.ShaderNodeDefinitionKey;
import com.jme3.util.blockparser.BlockLanguageParser;
import com.jme3.util.blockparser.Statement;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * ShaderNodeDefnition file loader (.j3sn)
 *
 * a j3sn file is a block style file like j3md or j3m. It must contain one
 * ShaderNodeDefinition{} block that contains several ShaderNodeDefinition{}
 * blocks
 *
 * @author Nehon
 */
public class ShaderNodeDefinitionLoader implements AssetLoader {

    private ShaderNodeLoaderDelegate loaderDelegate;

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey k = assetInfo.getKey();
        if (!(k instanceof ShaderNodeDefinitionKey)) {
            throw new IOException("ShaderNodeDefinition file must be loaded via ShaderNodeDefinitionKey");
        }
        ShaderNodeDefinitionKey key = (ShaderNodeDefinitionKey) k;
        loaderDelegate = new ShaderNodeLoaderDelegate();

        InputStream in = assetInfo.openStream();
        List<Statement> roots = BlockLanguageParser.parse(in);

        if (roots.size() == 2) {
            Statement exception = roots.get(0);
            String line = exception.getLine();
            if (line.startsWith("Exception")) {
                throw new AssetLoadException(line.substring("Exception ".length()));
            } else {
                throw new MatParseException("In multiroot shader node definition, expected first statement to be 'Exception'", exception);
            }
        } else if (roots.size() != 1) {
            throw new MatParseException("Too many roots in J3SN file", roots.get(0));
        }

        return loaderDelegate.readNodesDefinitions(roots.get(0).getContents(), key);

    }
}
