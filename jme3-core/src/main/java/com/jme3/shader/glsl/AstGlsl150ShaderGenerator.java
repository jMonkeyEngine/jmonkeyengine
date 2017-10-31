package com.jme3.shader.glsl;

import com.jme3.asset.AssetManager;
import com.jme3.material.ShaderGenerationInfo;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderType;
import com.jme3.shader.ShaderNodeVariable;

/**
 * The implementation of a shader generator with using AST GLSL to generate a result shader.
 *
 * @author JavaSaBr
 */
public class AstGlsl150ShaderGenerator extends AstGlsl100ShaderGenerator {

    private static final String MAIN_COMPATIBILITY =
            "#if defined GL_ES\n" +
            "#  define hfloat highp float\n" +
            "#  define hvec2  highp vec2\n" +
            "#  define hvec3  highp vec3\n" +
            "#  define hvec4  highp vec4\n" +
            "#  define lfloat lowp float\n" +
            "#  define lvec2 lowp vec2\n" +
            "#  define lvec3 lowp vec3\n" +
            "#  define lvec4 lowp vec4\n" +
            "#else\n" +
            "#  define hfloat float\n" +
            "#  define hvec2  vec2\n" +
            "#  define hvec3  vec3\n" +
            "#  define hvec4  vec4\n" +
            "#  define lfloat float\n" +
            "#  define lvec2  vec2\n" +
            "#  define lvec3  vec3\n" +
            "#  define lvec4  vec4\n" +
            "#endif\n" +
            "#define texture1D texture\n" +
            "#define texture2D texture\n" +
            "#define texture3D texture\n" +
            "#define textureCube texture\n" +
            "#define texture2DLod textureLod\n" +
            "#define textureCubeLod textureLod\n";

    private static final String VERTEX_COMPATIBILITY =
            "#define varying out\n" +
            "#define attribute in\n";

    private static final String FRAGMENT_COMPATIBILITY =
            "#define varying in\n";

    public AstGlsl150ShaderGenerator(final AssetManager assetManager) {
        super(assetManager);
    }

    @Override
    protected String getLanguageAndVersion(final ShaderType type) {
        return "GLSL150";
    }

    @Override
    protected void declareVarying(final StringBuilder source, final ShaderNodeVariable var, final boolean input) {
        declareVariable(source, var, true, input ? "in" : "out");
    }

    @Override
    protected void declareAttribute(final StringBuilder source, final ShaderNodeVariable var) {
        declareVariable(source, var, false, "in");
    }

    @Override
    protected void generateStartOfMainSection(StringBuilder source, ShaderGenerationInfo info, Shader.ShaderType type) {
        source.append("\n");

        if (type == Shader.ShaderType.Fragment) {
            for (ShaderNodeVariable global : info.getFragmentGlobals()) {
                declareVariable(source, global, null, true, "out");
            }
        }
        source.append("\n");

        appendIndent(source);
        source.append("void main() {\n");
        indent();

        if (type == Shader.ShaderType.Vertex) {
            declareGlobalPosition(info, source);
        } else if (type == Shader.ShaderType.Fragment) {
            for (ShaderNodeVariable global : info.getFragmentGlobals()) {
                initVariable(source, global, "vec4(1.0)");
            }
        }
    }

    @Override
    protected void generateEndOfMainSection(StringBuilder source, ShaderGenerationInfo info, Shader.ShaderType type) {

        if (type == Shader.ShaderType.Vertex) {
            appendOutput(source, "gl_Position", info.getVertexGlobal());
        }

        unIndent();
        appendIndent(source);
        source.append("}\n");
    }

    @Override
    protected void generateCompatibilityDefines(final StringBuilder headerSource, final ShaderType type) {
        super.generateCompatibilityDefines(headerSource, type);
        headerSource.append(MAIN_COMPATIBILITY);

        if (type == ShaderType.Fragment) {
            headerSource.append(FRAGMENT_COMPATIBILITY);
        } else if (type == ShaderType.Vertex) {
            headerSource.append(VERTEX_COMPATIBILITY);
        }

        headerSource.append('\n');
    }

    /**
     * Append a variable initialization to the code
     *
     * @param source the StringBuilder to use
     * @param var the variable to initialize
     * @param initValue the init value to assign to the variable
     */
    protected void initVariable(StringBuilder source, ShaderNodeVariable var, String initValue) {
        appendIndent(source);
        source.append(var.getNameSpace());
        source.append("_");
        source.append(var.getName());
        source.append(" = ");
        source.append(initValue);
        source.append(";\n");
    }
}
