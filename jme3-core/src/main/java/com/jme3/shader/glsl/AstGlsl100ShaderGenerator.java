package com.jme3.shader.glsl;

import com.jme3.asset.AssetManager;

/**
 * The implementation of a shader generator with using AST GLSL to generate a result shader.
 *
 * @author JavaSaBr
 */
public class AstGlsl100ShaderGenerator extends AstShaderGenerator {

    public AstGlsl100ShaderGenerator(final AssetManager assetManager) {
        super(assetManager);
    }
}
