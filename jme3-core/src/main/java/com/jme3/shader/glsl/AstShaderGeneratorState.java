package com.jme3.shader.glsl;

import com.jme3.material.TechniqueDef;
import com.jme3.shader.ShaderNode;
import com.jme3.shader.glsl.parser.ast.declaration.ExternalFieldDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.FileDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.LocalVarDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.MethodDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.preprocessor.ExtensionPreprocessorAstNode;
import com.jme3.shader.glsl.parser.ast.preprocessor.ImportPreprocessorAstNode;
import com.jme3.shader.glsl.parser.ast.value.DefineValueAstNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class with state of shader generator.
 *
 * @author JavaSaBr
 */
public class AstShaderGeneratorState {

    /**
     * The cache of AST of shader sources.
     */
    private final Map<String, FileDeclarationAstNode> astCache;

    /**
     * The mapping a shader node source path to a AST presentation.
     */
    private final Map<ShaderNode, FileDeclarationAstNode> shaderNodeSources;

    /**
     * The list of extension nodes.
     */
    private final List<ExtensionPreprocessorAstNode> extensionNodes;

    /**
     * The list of import nodes.
     */
    private final List<ImportPreprocessorAstNode> importNodes;

    /**
     * The list of defined variables.
     */
    private final List<String> definedVariables;

    /**
     * The list of result defines.
     */
    private final List<String> resultDefines;

    /**
     * The list of define value nodes.
     */
    private final List<DefineValueAstNode> defineValueNodes;

    /**
     * The list of declared uniforms in imported shaders.
     */
    private final List<ExternalFieldDeclarationAstNode> importedUnforms;

    /**
     * The result list of declared uniforms in imported shaders.
     */
    private final List<ExternalFieldDeclarationAstNode> importedGlobalUniforms;

    /**
     * The list of methods.
     */
    private final List<MethodDeclarationAstNode> methods;

    /**
     * The list of local variables.
     */
    private final List<LocalVarDeclarationAstNode> localVars;

    /**
     * The builder of header shader code.
     */
    private final StringBuilder headerSource;

    /**
     * The builder of imported shader code.
     */
    private final StringBuilder importsSource;

    /**
     * The builder of uniforms code.
     */
    private final StringBuilder uniformsSource;

    /**
     * The builder of methods code.
     */
    private final StringBuilder methodsSource;

    /**
     * The builder of main source code.
     */
    private final StringBuilder mainSource;

    /**
     * The builder with original source.
     */
    private final StringBuilder originalSource;

    /**
     * The builder with updated source.
     */
    private final StringBuilder updatedSource;

    /**
     * The technique definition.
     */
    private TechniqueDef techniqueDef;

    /**
     * The indent.
     */
    private int indent;

    public AstShaderGeneratorState() {
        this.astCache = new HashMap<>();
        this.shaderNodeSources = new HashMap<>();
        this.extensionNodes = new ArrayList<>();
        this.importNodes = new ArrayList<>();
        this.definedVariables = new ArrayList<>();
        this.resultDefines = new ArrayList<>();
        this.defineValueNodes = new ArrayList<>();
        this.importedUnforms = new ArrayList<>();
        this.importedGlobalUniforms = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.localVars = new ArrayList<>();
        this.headerSource = new StringBuilder();
        this.importsSource = new StringBuilder();
        this.uniformsSource = new StringBuilder();
        this.methodsSource = new StringBuilder();
        this.mainSource = new StringBuilder();
        this.originalSource = new StringBuilder();
        this.updatedSource = new StringBuilder();
    }

    /**
     * Gets the cache of AST of shader sources.
     *
     * @return the cache of AST of shader sources.
     */
    public Map<String, FileDeclarationAstNode> getAstCache() {
        return astCache;
    }

    /**
     * Gets the mapping a shader node source path to a AST presentation.
     *
     * @return the mapping a shader node source path to a AST presentation.
     */
    public Map<ShaderNode, FileDeclarationAstNode> getShaderNodeSources() {
        return shaderNodeSources;
    }

    /**
     * Gets the list of define value nodes.
     *
     * @return the list of define value nodes.
     */
    public List<DefineValueAstNode> getDefineValueNodes() {
        return defineValueNodes;
    }

    /**
     * Gets the list of extension nodes.
     *
     * @return the list of extension nodes.
     */
    public List<ExtensionPreprocessorAstNode> getExtensionNodes() {
        return extensionNodes;
    }

    /**
     * Gets the list of import nodes.
     *
     * @return the list of import nodes.
     */
    public List<ImportPreprocessorAstNode> getImportNodes() {
        return importNodes;
    }

    /**
     * Gets the result list of declared uniforms in imported shaders.
     *
     * @return the result list of declared uniforms in imported shaders.
     */
    public List<ExternalFieldDeclarationAstNode> getImportedGlobalUniforms() {
        return importedGlobalUniforms;
    }

    /**
     * Gets the list of declared uniforms in imported shaders.
     *
     * @return the list of declared uniforms in imported shaders.
     */
    public List<ExternalFieldDeclarationAstNode> getImportedUnforms() {
        return importedUnforms;
    }

    /**
     * Gets the list of local variables.
     *
     * @return the list of local variables.
     */
    public List<LocalVarDeclarationAstNode> getLocalVars() {
        return localVars;
    }

    /**
     * Gets the list of methods.
     *
     * @return the list of methods.
     */
    public List<MethodDeclarationAstNode> getMethods() {
        return methods;
    }

    /**
     * Gets the list of defined variables.
     *
     * @return the list of defined variables.
     */
    public List<String> getDefinedVariables() {
        return definedVariables;
    }

    /**
     * Gets the list of result defines.
     *
     * @return the list of result defines.
     */
    public List<String> getResultDefines() {
        return resultDefines;
    }

    /**
     * Gets the technique definition.
     *
     * @return the technique definition.
     */
    public TechniqueDef getTechniqueDef() {
        return techniqueDef;
    }

    /**
     * Sets the technique definition.
     *
     * @param techniqueDef the technique definition.
     */
    public void setTechniqueDef(final TechniqueDef techniqueDef) {
        this.techniqueDef = techniqueDef;
    }

    /**
     * Gets the current indent.
     *
     * @return the current indent.
     */
    public int getIndent() {
        return indent;
    }

    /**
     * Sets the new indent.
     *
     * @param indent the new indent.
     */
    public void setIndent(final int indent) {
        this.indent = indent;
    }

    /**
     * Gets the builder of header shader code.
     *
     * @return the builder of header shader code.
     */
    public StringBuilder getHeaderSource() {
        return headerSource;
    }

    /**
     * Gets the builder of imported shader code.
     *
     * @return the builder of imported shader code.
     */
    public StringBuilder getImportsSource() {
        return importsSource;
    }

    /**
     * Gets the builder of main source code.
     *
     * @return the builder of main source code.
     */
    public StringBuilder getMainSource() {
        return mainSource;
    }

    /**
     * Gets the builder of methods code.
     *
     * @return the builder of methods code.
     */
    public StringBuilder getMethodsSource() {
        return methodsSource;
    }

    /**
     * Gets the builder of uniforms code.
     *
     * @return the builder of uniforms code.
     */
    public StringBuilder getUniformsSource() {
        return uniformsSource;
    }

    /**
     * Gets the builder with original source.
     *
     * @return the builder with original source.
     */
    public StringBuilder getOriginalSource() {
        return originalSource;
    }

    /**
     * Gets the builder with updated source.
     *
     * @return the builder with updated source.
     */
    public StringBuilder getUpdatedSource() {
        return updatedSource;
    }
}
