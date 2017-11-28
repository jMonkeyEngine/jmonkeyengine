package com.jme3.shader.glsl;

import static com.jme3.shader.glsl.parser.ast.util.AstUtils.findAllByType;
import static java.lang.System.getProperty;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.cache.AssetCache;
import com.jme3.material.ShaderGenerationInfo;
import com.jme3.material.TechniqueDef;
import com.jme3.shader.*;
import com.jme3.shader.Shader.ShaderType;
import com.jme3.shader.glsl.parser.GlslLang;
import com.jme3.shader.glsl.parser.GlslParser;
import com.jme3.shader.glsl.parser.ast.AstNode;
import com.jme3.shader.glsl.parser.ast.NameAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.ExternalFieldDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.FileDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.LocalVarDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.MethodDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.preprocessor.ExtensionPreprocessorAstNode;
import com.jme3.shader.glsl.parser.ast.preprocessor.ImportPreprocessorAstNode;
import com.jme3.shader.glsl.parser.ast.util.AstUtils;
import com.jme3.shader.glsl.parser.ast.util.CharPredicate;
import com.jme3.shader.glsl.parser.ast.value.DefineValueAstNode;
import com.jme3.shader.glsl.parser.ast.value.StringValueAstNode;
import com.jme3.shader.plugins.ShaderAssetKey;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The base implementation of a shader generator with using AST GLSL to generate a result shader.
 *
 * @author JavaSaBr
 */
public abstract class AstShaderGenerator extends Glsl100ShaderGenerator {

    public static final String PROP_USE_CASE = "AstShaderGenerator.useCache";

    private static final boolean USE_AST_CACHE;

    private static final String INPUT_VAR_DEFINITION_PREFIX = "HAS_INPUT_";
    private static final String OUTPUT_VAR_DEFINITION_PREFIX = "HAS_OUTPUT_";

    static {
        USE_AST_CACHE = Boolean.parseBoolean(getProperty(PROP_USE_CASE, "true"));
    }

    protected static final char[] EMPTY_CHARS = new char[0];

    private class ImportedShaderKey extends AssetKey<Reader> {

        private ImportedShaderKey(final String name) {
            super(name);
        }

        @Override
        public Class<? extends AssetCache> getCacheType() {
            return null;
        }
    }

    protected static final CharPredicate PREVIOUS_VAR_CHAR_CHECKER = new CharPredicate() {
        @Override
        public boolean test(final char value) {
            switch (value) {
                case ' ':
                case '\n':
                case '\r':
                case ',':
                case '=':
                case '+':
                case '*':
                case '-':
                case '/':
                case '(': {
                    return true;
                }
                default:
                    return false;
            }
        }
    };

    protected static final CharPredicate NEXT_VAR_CHAR_CHECKER = new CharPredicate() {
        @Override
        public boolean test(final char value) {
            switch (value) {
                case ' ':
                case '\n':
                case '\r':
                case ',':
                case '=':
                case '.':
                case '+':
                case '*':
                case '-':
                case '/':
                case ';':
                case ')': {
                    return true;
                }
                default:
                    return false;
            }
        }
    };

    protected static final CharPredicate PREVIOUS_METHOD_CHAR_CHECKER = new CharPredicate() {
        @Override
        public boolean test(final char value) {
            return PREVIOUS_VAR_CHAR_CHECKER.test(value);
        }
    };

    protected static final CharPredicate NEXT_METHOD_CHAR_CHECKER = new CharPredicate() {
        @Override
        public boolean test(final char value) {
            switch (value) {
                case ' ':
                case '\n':
                case '\r':
                case ',':
                case '=':
                case '.':
                case '+':
                case '*':
                case '-':
                case '/':
                case ';':
                case '(': {
                    return true;
                }
                default:
                    return false;
            }
        }
    };

    protected static final CharPredicate PREVIOUS_DEFINE_CHAR_CHECKER = new CharPredicate() {
        @Override
        public boolean test(final char value) {
            switch (value) {
                case ' ':
                case '\n':
                case '\r':
                case '(': {
                    return true;
                }
                default:
                    return false;
            }
        }
    };

    protected static final CharPredicate NEXT_DEFINE_CHAR_CHECKER = new CharPredicate() {
        @Override
        public boolean test(final char value) {
            switch (value) {
                case ' ':
                case '\n':
                case '\r':
                case ')': {
                    return true;
                }
                default:
                    return false;
            }
        }
    };

    /**
     * Calculate the indent using space characters.
     *
     * @param level the level.
     * @return the result indent.
     */
    protected static char[] getIndent(final int level) {

        if (level == 0) {
            return EMPTY_CHARS;
        }

        final int characters = level * 4;
        final char[] result = new char[characters];

        for (int i = 0; i < result.length; i++) {
            result[i] = ' ';
        }

        return result;
    }

    /**
     * The thread local state of this generator.
     */
    protected static final ThreadLocal<AstShaderGeneratorState> LOCAL_STATE = new ThreadLocal<AstShaderGeneratorState>() {

        @Override
        protected AstShaderGeneratorState initialValue() {
            return new AstShaderGeneratorState();
        }
    };

    public AstShaderGenerator(final AssetManager assetManager) {
        super(assetManager);
    }

    @Override
    protected void indent() {
        final AstShaderGeneratorState state = LOCAL_STATE.get();
        state.setIndent(state.getIndent() + 1);
    }

    @Override
    protected void unIndent() {
        final AstShaderGeneratorState state = LOCAL_STATE.get();
        if (state.getIndent() < 0) return;
        state.setIndent(state.getIndent() - 1);
    }

    @Override
    protected void appendIndent(final StringBuilder source) {
        final AstShaderGeneratorState state = LOCAL_STATE.get();
        source.append(getIndent(state.getIndent()));
    }

    @Override
    public void initialize(final TechniqueDef techniqueDef) {
        super.initialize(techniqueDef);

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        state.setTechniqueDef(techniqueDef);
        state.setIndent(0);
        state.getImportedGlobalUniforms().clear();

        prepareShaderNodeSources(techniqueDef.getShaderNodes());
    }

    @Override
    public Shader generateShader(final String definesSourceCode) {

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        final TechniqueDef techniqueDef = state.getTechniqueDef();
        final Shader result = super.generateShader(definesSourceCode);

        // we need to add uniform bindings from imported shaders, because it can be unpresented in shader nodes.
        final List<UniformBinding> worldBindings = techniqueDef.getWorldBindings();
        final List<ExternalFieldDeclarationAstNode> globalUniforms = state.getImportedGlobalUniforms();

        AstUtils.removeExists(globalUniforms, worldBindings);

        if (!globalUniforms.isEmpty()) {
            for (final ExternalFieldDeclarationAstNode field : globalUniforms) {

                final NameAstNode nameNode = field.getName();
                final String name = nameNode.getName();

                final UniformBinding binding = UniformBinding.valueOf(name.substring(2, name.length()));
                result.addUniformBinding(binding);
            }
        }

        return result;
    }

    @Override
    protected String buildShader(final List<ShaderNode> shaderNodes, final ShaderGenerationInfo info,
                                 final ShaderType type) {

        if (type != ShaderType.Vertex && type != ShaderType.Fragment) {
            return null;
        }

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        final Map<ShaderNode, FileDeclarationAstNode> shaderNodeSources = state.getShaderNodeSources();
        final List<ExtensionPreprocessorAstNode> extensionNodes = state.getExtensionNodes();
        extensionNodes.clear();

        final List<ImportPreprocessorAstNode> importNodes = state.getImportNodes();
        importNodes.clear();

        findImportsAndExtensions(shaderNodes, type, shaderNodeSources, extensionNodes, importNodes);

        AstUtils.removeExtensionDuplicates(extensionNodes);
        AstUtils.removeImportDuplicates(importNodes);

        final StringBuilder headerSource = clear(state.getHeaderSource());
        final StringBuilder importsSource = clear(state.getImportsSource());
        final StringBuilder uniformsSource = clear(state.getUniformsSource());
        final StringBuilder methodsSource = clear(state.getMethodsSource());
        final StringBuilder mainSource = clear(state.getMainSource());

        generateExtensions(extensionNodes, headerSource);

        final List<ExternalFieldDeclarationAstNode> importedUniforms = state.getImportedUnforms();
        importedUniforms.clear();

        generateImports(importNodes, importedUniforms, importsSource);

        AstUtils.copyGlobalUniforms(importedUniforms, state.getImportedGlobalUniforms());

        generateUniforms(uniformsSource, info, type);

        if (type == ShaderType.Vertex) {
            generateAttributes(uniformsSource, info);
        }

        generateVaryings(uniformsSource, info, type);
        generateMethods(shaderNodes, type, methodsSource);
        generateStartOfMainSection(mainSource, info, type);
        generateDeclarationAndMainBody(shaderNodes, null, mainSource, info, type);
        generateEndOfMainSection(mainSource, info, type);
        generateVariableDefinitions(headerSource, state.getResultUsedVariableDefinitions());
        generateCompatibilityDefines(headerSource, type);
        generateShaderNodeHeaders(shaderNodes, info, type, headerSource);

        final StringBuilder result = new StringBuilder();

        if (headerSource.length() > 0) {
            result.append(headerSource).append('\n');
        }

        if (importsSource.length() > 0) {
            result.append(importsSource);
        }

        if (uniformsSource.length() > 0) {
            result.append(uniformsSource).append('\n');
        }

        if (methodsSource.length() > 0) {
            result.append(methodsSource).append('\n');
        }

        return result.append(mainSource).toString();
    }

    /**
     * Generates shader nodes headers.
     *
     * @param shaderNodes  the list of shader nodes.
     * @param info         the generating information.
     * @param type         the shader type.
     * @param headerSource the header source.
     */
    private void generateShaderNodeHeaders(final List<ShaderNode> shaderNodes, final ShaderGenerationInfo info,
                                           final ShaderType type, final StringBuilder headerSource) {

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        final Map<ShaderNode, FileDeclarationAstNode> shaderNodeSources = state.getShaderNodeSources();
        final List<DefineValueAstNode> defineValueNodes = state.getDefineValueNodes();
        final List<String> unusedNodes = info.getUnusedNodes();

        for (final ShaderNode shaderNode : shaderNodes) {

            if (unusedNodes.contains(shaderNode.getName())) {
                continue;
            }

            final ShaderNodeDefinition definition = shaderNode.getDefinition();
            if (definition.getType() != type) {
                continue;
            }

            final FileDeclarationAstNode shaderFile = shaderNodeSources.get(shaderNode);
            final List<AstNode> children = shaderFile.getChildren();

            for (final AstNode child : children) {

                if (child instanceof MethodDeclarationAstNode) {
                    continue;
                } else if (child instanceof ImportPreprocessorAstNode) {
                    continue;
                }

                defineValueNodes.clear();
                findAllByType(child, defineValueNodes, DefineValueAstNode.class);

                final String code = updateDefinitionNames(shaderNode, child.getText(), defineValueNodes);

                headerSource.append(code).append('\n').append('\n');
            }
        }
    }

    /**
     * Generates compatibility defines.
     *
     * @param headerSource the header source.
     * @param type         the shader type.
     */
    protected void generateCompatibilityDefines(final StringBuilder headerSource, final ShaderType type) {
    }

    /**
     * Generates variable definitions.
     *
     * @param headerSource  the header source.
     * @param result the result definition list.
     */
    private void generateVariableDefinitions(final StringBuilder headerSource, final List<String> result) {

        if (result.isEmpty()) {
            return;
        }

        for (final String define : result) {
            headerSource.append("#define ").append(define).append(" 1").append('\n');
        }

        headerSource.append('\n');
    }

    /**
     * Finds imports and extensionNodes from the shader nodes.
     *
     * @param shaderNodes       the shader nodes.
     * @param type              the current type.
     * @param shaderNodeSources the shader node sources.
     * @param extensionNodes    the extension nodes.
     * @param importNodes       the import nodes.
     */
    private void findImportsAndExtensions(final List<ShaderNode> shaderNodes, final ShaderType type,
                                          final Map<ShaderNode, FileDeclarationAstNode> shaderNodeSources,
                                          final List<ExtensionPreprocessorAstNode> extensionNodes,
                                          final List<ImportPreprocessorAstNode> importNodes) {

        for (final ShaderNode shaderNode : shaderNodes) {

            final ShaderNodeDefinition definition = shaderNode.getDefinition();
            if (definition.getType() != type) {
                continue;
            }

            final FileDeclarationAstNode fileDeclarationASTNode = shaderNodeSources.get(shaderNode);
            findAllByType(fileDeclarationASTNode, extensionNodes, ExtensionPreprocessorAstNode.class);
            findAllByType(fileDeclarationASTNode, importNodes, ImportPreprocessorAstNode.class);
        }
    }

    @Override
    protected void generateDeclarationAndMainBody(final List<ShaderNode> shaderNodes,
                                                  final StringBuilder sourceDeclaration, final StringBuilder source,
                                                  final ShaderGenerationInfo info, final ShaderType type) {

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        final Map<ShaderNode, FileDeclarationAstNode> shaderNodeSources = state.getShaderNodeSources();
        final List<LocalVarDeclarationAstNode> localVariables = state.getLocalVars();
        final List<MethodDeclarationAstNode> methods = state.getMethods();
        final List<String> unusedNodes = info.getUnusedNodes();

        final List<DefineValueAstNode> defineValueNodes = state.getDefineValueNodes();
        final List<String> usedVariableDefinitions = state.getUsedVariableDefinitions();
        final List<String> resultUsedVariableDefinitions = state.getResultUsedVariableDefinitions();
        resultUsedVariableDefinitions.clear();

        for (final ShaderNode shaderNode : shaderNodes) {

            if (unusedNodes.contains(shaderNode.getName())) {
                continue;
            }

            final ShaderNodeDefinition definition = shaderNode.getDefinition();
            if (definition.getType() != type) {
                continue;
            }

            methods.clear();
            usedVariableDefinitions.clear();
            defineValueNodes.clear();

            final FileDeclarationAstNode shaderFile = shaderNodeSources.get(shaderNode);
            findAllByType(shaderFile, methods, MethodDeclarationAstNode.class);
            findAllByType(shaderFile, defineValueNodes, DefineValueAstNode.class);

            AstUtils.removeDefineValueDuplicates(defineValueNodes);

            copyVariableDefinitions(defineValueNodes, usedVariableDefinitions);
            findAvailableDefinesToDefine(shaderNode, shaderNodes, usedVariableDefinitions, resultUsedVariableDefinitions);

            final MethodDeclarationAstNode mainMethod = findMainMethod(methods);

            if (mainMethod == null) {
                generateNodeMainSection(source, shaderNode, null, info);
                continue;
            }

            findAllByType(mainMethod, localVariables, LocalVarDeclarationAstNode.class);

            String methodBodySource = updateMethodCalls(shaderNode, mainMethod, methods);
            methodBodySource = updateLocalVarNames(shaderNode, methodBodySource, localVariables);
            methodBodySource = updateDefinitionNames(shaderNode, methodBodySource, defineValueNodes);

            generateNodeMainSection(source, shaderNode, methodBodySource, info);
        }
    }

    @Override
    protected void generateNodeMainSection(final StringBuilder source, final ShaderNode shaderNode, String nodeSource,
                                           final ShaderGenerationInfo info) {

        if (nodeSource == null) {
            comment(source, shaderNode, "Begin");
            comment(source, shaderNode, "End");
            return;
        }

        comment(source, shaderNode, "Begin");
        startCondition(shaderNode.getCondition(), source);

        final ShaderNodeDefinition definition = shaderNode.getDefinition();
        final List<String> declaredVariables = new ArrayList<>();

        for (final VariableMapping mapping : shaderNode.getInputMapping()) {

            final ShaderNodeVariable rightVariable = mapping.getRightVariable();
            final ShaderNodeVariable leftVariable = mapping.getLeftVariable();

            // Variables fed with a sampler matparam or world param are replaced by the matparam itself
            // It avoids issue with samplers that have to be uniforms.
            if (isWorldOrMaterialParam(rightVariable) && rightVariable.getType().startsWith("sampler")) {
                nodeSource = replace(nodeSource, leftVariable, rightVariable.getPrefix() + rightVariable.getName());
            } else {

                if (leftVariable.getType().startsWith("sampler")) {
                    throw new IllegalArgumentException("a Sampler must be a uniform");
                }

                map(mapping, source);
            }

            String newName = shaderNode.getName() + "_" + leftVariable.getName();

            if (!declaredVariables.contains(newName)) {
                nodeSource = replace(nodeSource, leftVariable, newName);
                declaredVariables.add(newName);
            }
        }

        for (final ShaderNodeVariable var : definition.getInputs()) {

            if (var.getDefaultValue() == null) {
                continue;
            }

            final String fullName = shaderNode.getName() + "_" + var.getName();

            if (declaredVariables.contains(fullName)) {
                continue;
            }

            final ShaderNodeVariable variable = new ShaderNodeVariable(var.getType(), shaderNode.getName(),
                    var.getName(), var.getMultiplicity());

            if (!isVarying(info, variable)) {
                declareVariable(source, variable, var.getDefaultValue(), true, null);
            }
            nodeSource = replaceVariableName(nodeSource, variable);
            declaredVariables.add(fullName);
        }

        for (final ShaderNodeVariable var : definition.getOutputs()) {

            if (declaredVariables.contains(shaderNode.getName() + "_" + var.getName())) {
                continue;
            }

            final ShaderNodeVariable variable = new ShaderNodeVariable(var.getType(), shaderNode.getName(),
                    var.getName(), var.getMultiplicity());

            if (!isVarying(info, variable)) {
                declareVariable(source, variable);
            }

            nodeSource = replaceVariableName(nodeSource, variable);
        }

        appendIndent(source);

        source.append(nodeSource);
        source.append('\n');

        for (final VariableMapping mapping : shaderNode.getOutputMapping()) {
            map(mapping, source);
        }

        endCondition(shaderNode.getCondition(), source);
        comment(source, shaderNode, "End");
    }

    @Override
    protected String replace(final String source, final ShaderNodeVariable var, final String newName) {

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        final StringBuilder original = clear(state.getOriginalSource());
        original.append(source);

        final StringBuilder result = clear(state.getUpdatedSource());
        return replaceVar(original, var.getName(), newName, result).toString();
    }

    /**
     * Updates the method calls from the main method.
     *
     * @param shaderNode the shader node.
     * @param mainMethod the main method.
     * @param methods    the list of all methods.
     * @return the updated source.
     */
    private String updateMethodCalls(final ShaderNode shaderNode, final MethodDeclarationAstNode mainMethod,
                                     final List<MethodDeclarationAstNode> methods) {

        String methodBodySource = mainMethod.getBody().getText();

        if (methods.size() < 2) {
            return methodBodySource;
        }

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        StringBuilder original = clear(state.getOriginalSource());
        StringBuilder result = clear(state.getUpdatedSource());

        for (final MethodDeclarationAstNode methodDeclaration : methods) {

            final NameAstNode methodName = methodDeclaration.getName();
            final String name = methodName.getName();

            if (name.equals("main")) {
                continue;
            }

            if (result.length() > 0) {
                final StringBuilder swap = original;
                original = result;
                result = clear(swap);
            } else {
                original.append(methodBodySource);
            }

            // replace calls of the declared methods.
            replaceMethod(original, name, shaderNode.getName() + "_" + name, result);
        }

        return result.toString();
    }

    /**
     * Updates the names of local variables in the main method.
     *
     * @param shaderNode the shader node.
     * @param source     the current source.
     * @param localVars  the list of local variables.
     * @return the updated source.
     */
    private String updateLocalVarNames(final ShaderNode shaderNode, final String source,
                                       final List<LocalVarDeclarationAstNode> localVars) {

        if (localVars.isEmpty()) {
            return source;
        }

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        StringBuilder original = clear(state.getOriginalSource());
        StringBuilder result = clear(state.getUpdatedSource());

        for (final LocalVarDeclarationAstNode localVar : localVars) {

            final NameAstNode methodName = localVar.getName();
            final String name = methodName.getName();

            if (result.length() > 0) {
                final StringBuilder swap = original;
                original = result;
                result = clear(swap);
            } else {
                original.append(source);
            }

            replaceVar(original, name, shaderNode.getName() + "_" + name, result);
        }

        return result.toString();
    }

    /**
     * Calculate used definition names in the shader source which need to define in the top of the result shader.
     *
     * @param shaderNode                    the shader node.
     * @param shaderNodes                   the list of shader nodes.
     * @param usedVariableDefinitions       the used variable definitions.
     * @param resultUsedVariableDefinitions the result used variable definitions.
     */
    private void findAvailableDefinesToDefine(final ShaderNode shaderNode, final List<ShaderNode> shaderNodes,
                                              final List<String> usedVariableDefinitions,
                                              final List<String> resultUsedVariableDefinitions) {

        if (usedVariableDefinitions.isEmpty()) {
            return;
        }

        final List<VariableMapping> inputMapping = shaderNode.getInputMapping();
        final List<VariableMapping> outputMapping = shaderNode.getOutputMapping();

        for (final String definitionName : usedVariableDefinitions) {

            if (!isShaderNodeInputVarDefinition(definitionName)) {
                continue;
            }

            for (final VariableMapping mapping : inputMapping) {

                final ShaderNodeVariable variable = mapping.getLeftVariable();
                final String variableName = variable.getName();

                final int resultLength = INPUT_VAR_DEFINITION_PREFIX.length() + variableName.length();

                if (definitionName.length() == resultLength && definitionName.endsWith(variableName)) {
                    resultUsedVariableDefinitions.add(toResultShaderNodeInputVarDefinition(shaderNode, definitionName));
                    break;
                }
            }
        }

        for (final String definitionName : usedVariableDefinitions) {

            if (!isShaderNodeOutputVarDefinition(definitionName)) {
                continue;
            }

            boolean exists = false;

            for (final VariableMapping mapping : outputMapping) {

                final ShaderNodeVariable variable = mapping.getRightVariable();
                final String variableName = variable.getName();

                final int resultLength = OUTPUT_VAR_DEFINITION_PREFIX.length() + variableName.length();

                if (definitionName.length() == resultLength && definitionName.endsWith(variableName)) {
                    resultUsedVariableDefinitions.add(toResultShaderNodeOutputVarDefinition(shaderNode, definitionName));
                    exists = true;
                    break;
                }
            }

            if (exists) continue;

            for (final ShaderNode otherNode : shaderNodes) {
                if(otherNode == shaderNode) continue;

                final List<VariableMapping> otherInputMapping = otherNode.getInputMapping();

                for (final VariableMapping mapping : otherInputMapping) {

                    final ShaderNodeVariable variable = mapping.getRightVariable();
                    if (!shaderNode.getName().equals(variable.getNameSpace())) {
                        continue;
                    }

                    final String variableName = variable.getName();
                    final int resultLength = OUTPUT_VAR_DEFINITION_PREFIX.length() + variableName.length();

                    if (definitionName.length() == resultLength && definitionName.endsWith(variableName)) {
                        resultUsedVariableDefinitions.add(toResultShaderNodeInputVarDefinition(shaderNode, definitionName));
                        break;
                    }
                }
            }
        }
    }

    /**
     * Updates the definition names in the source code.
     *
     * @param shaderNode       the shader node.
     * @param source           the current source.
     * @param defineValueNodes the define value nodes.
     * @return the updated source.
     */
    private String updateDefinitionNames(final ShaderNode shaderNode, String source,
                                         final List<DefineValueAstNode> defineValueNodes) {

        if (defineValueNodes.isEmpty()) {
            return source;
        }

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        StringBuilder original = clear(state.getOriginalSource());
        StringBuilder result = clear(state.getUpdatedSource());

        for (final DefineValueAstNode defineValueNode : defineValueNodes) {

            final String define = defineValueNode.getValue();
            if (!isShaderNodeDefinition(define)) {
                continue;
            }

            if (result.length() > 0) {
                final StringBuilder swap = original;
                original = result;
                result = clear(swap);
            } else {
                original.append(source);
            }

            replaceDefine(original, define, shaderNode.getName() + "_" + define, result);
        }

        if (result.length() > 0) {
            return result.toString();
        }

        return source;
    }

    /**
     * Finds the main method.
     *
     * @param methods the list of methods.
     * @return the main method or null.
     */
    private MethodDeclarationAstNode findMainMethod(final List<MethodDeclarationAstNode> methods) {

        if (methods.isEmpty()) {
            return null;
        }

        for (final MethodDeclarationAstNode methodDeclaration : methods) {

            final NameAstNode methodName = methodDeclaration.getName();
            final String name = methodName.getName();

            if (name.equals("main")) {
                return methodDeclaration;
            }
        }

        return null;
    }

    /**
     * Generate all not main methods of all shader nodes.
     *
     * @param shaderNodes the shader nodes.
     * @param type        the shader type.
     * @param builder     the target builder.
     */
    protected void generateMethods(final List<ShaderNode> shaderNodes, final ShaderType type,
                                   final StringBuilder builder) {

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        final List<MethodDeclarationAstNode> methods = state.getMethods();
        methods.clear();

        final Map<ShaderNode, FileDeclarationAstNode> shaderNodeSources = state.getShaderNodeSources();

        for (final ShaderNode shaderNode : shaderNodes) {

            if (shaderNode.getDefinition().getType() != type) {
                continue;
            }

            methods.clear();

            final FileDeclarationAstNode shaderFile = shaderNodeSources.get(shaderNode);
            findAllByType(shaderFile, methods, MethodDeclarationAstNode.class);

            if (methods.size() < 2) {
                continue;
            }

            for (final MethodDeclarationAstNode method : methods) {

                final NameAstNode name = method.getName();
                final String methodName = name.getName();

                if ("main".equals(methodName)) {
                    continue;
                }

                final String methodContent = method.getText();
                final String resultContent = methodContent.replace(methodName, shaderNode.getName() + "_" + methodName);

                builder.append(resultContent).append('\n');
            }
        }
    }

    /**
     * Generates all importNodes.
     *
     * @param importNodes      the list of import nodes.
     * @param importedUniforms the list of imported uniforms.
     * @param builder          the target builder.
     */
    protected void generateImports(final List<ImportPreprocessorAstNode> importNodes,
                                   final List<ExternalFieldDeclarationAstNode> importedUniforms,
                                   final StringBuilder builder) {

        if (importNodes.isEmpty()) {
            return;
        }

        for (final ImportPreprocessorAstNode importNode : importNodes) {
            final StringValueAstNode importValue = importNode.getValue();
            final FileDeclarationAstNode shaderFile = parseShaderSource(importValue.getValue());
            findAllByType(shaderFile, importedUniforms, ExternalFieldDeclarationAstNode.class);
            builder.append(shaderFile.getText()).append('\n');
        }

        builder.append('\n');
    }

    /**
     * Generates all extensionNodes.
     *
     * @param extensionNodes the list of extension nodes.
     * @param builder        the target builder.
     */
    protected void generateExtensions(final List<ExtensionPreprocessorAstNode> extensionNodes,
                                      final StringBuilder builder) {

        if (extensionNodes.isEmpty()) {
            return;
        }

        for (final ExtensionPreprocessorAstNode extension : extensionNodes) {
            builder.append(extension.getText()).append('\n');
        }

        builder.append('\n');
    }

    @Override
    protected void generateUniforms(final StringBuilder source, final ShaderGenerationInfo info,
                                    final ShaderType type) {
        switch (type) {
            case Vertex:
                generateUniforms(source, info.getVertexUniforms());
                break;
            case Fragment:
                generateUniforms(source, info.getFragmentUniforms());
                break;
        }
    }

    @Override
    protected void generateUniforms(final StringBuilder source, final List<ShaderNodeVariable> uniforms) {

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        final List<ExternalFieldDeclarationAstNode> importedFields = state.getImportedUnforms();

        for (final ShaderNodeVariable var : uniforms) {
            if (isExist(var, importedFields)) continue;
            declareVariable(source, var, false, "uniform");
        }
    }

    @Override
    protected void generateAttributes(final StringBuilder source, final ShaderGenerationInfo info) {

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        final List<ExternalFieldDeclarationAstNode> importedFields = state.getImportedUnforms();

        boolean inPosition = false;

        for (final ShaderNodeVariable var : info.getAttributes()) {
            if (var.getName().equals("inPosition")) {
                inPosition = true;
                var.setCondition(null);
                fixInPositionType(var);
                //keep track on the InPosition variable to avoid iterating through attributes again
                inPosTmp = var;
            }
            if (isExist(var, importedFields)) continue;
            declareAttribute(source, var);
        }

        if (!inPosition) {
            inPosTmp = new ShaderNodeVariable("vec3", "inPosition");
            if (isExist(inPosTmp, importedFields)) return;
            declareAttribute(source, inPosTmp);
        }
    }

    /**
     * Check of existing the variable in the imported shaders.
     *
     * @param variable       the variable.
     * @param importedFields the list of fields from imported shader.
     * @return true if the variable is exists.
     */
    private boolean isExist(final ShaderNodeVariable variable,
                            final List<ExternalFieldDeclarationAstNode> importedFields) {

        if (importedFields.isEmpty()) {
            return false;
        }

        final String name = variable.getName();
        final String prefix = variable.getPrefix();

        final int length = prefix.length() + name.length();

        for (final ExternalFieldDeclarationAstNode field : importedFields) {
            final NameAstNode nameASTNode = field.getName();
            final String fieldName = nameASTNode.getName();
            if (fieldName.length() != length || !fieldName.startsWith(prefix)) {
                continue;
            } else if (fieldName.endsWith(name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Prepares the map with shader source path - parsed AST files.
     *
     * @param shaderNodes the list of shader nodes.
     */
    protected void prepareShaderNodeSources(final List<ShaderNode> shaderNodes) {

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        final Map<ShaderNode, FileDeclarationAstNode> shaderNodeSources = state.getShaderNodeSources();
        shaderNodeSources.clear();

        for (final ShaderNode shaderNode : shaderNodes) {

            final ShaderNodeDefinition definition = shaderNode.getDefinition();

            final int index = findShaderIndexFromVersion(shaderNode, definition.getType());
            final String shaderSourcePath = definition.getShadersPath().get(index);

            shaderNodeSources.put(shaderNode, parseShaderSource(shaderSourcePath));
        }
    }

    /**
     * Parses the shader source by the shader source path.
     *
     * @param shaderSourcePath the path to the shader source file.
     * @return the parsed shader source as AST file.
     */
    protected FileDeclarationAstNode parseShaderSource(final String shaderSourcePath) {

        final AstShaderGeneratorState state = LOCAL_STATE.get();
        final Map<String, FileDeclarationAstNode> cache = state.getAstCache();
        final FileDeclarationAstNode cached = USE_AST_CACHE ? cache.get(shaderSourcePath) : null;

        if (cached != null) {
            return cached;
        }

        final Map<String, String> sourceMap;

        if (shaderSourcePath.endsWith("glsllib")) {

            final StringBuilder builder = new StringBuilder();

            try (final Reader reader = assetManager.loadAsset(new ImportedShaderKey(shaderSourcePath))) {
                int ch;
                while ((ch = reader.read()) != -1) {
                    builder.append((char) ch);
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            sourceMap = new HashMap<>();
            sourceMap.put("[main]", builder.toString());

        } else {

            final Object loadedResource = assetManager.loadAsset(new ShaderAssetKey(shaderSourcePath, false));

            if (loadedResource instanceof Map) {
                sourceMap = (Map<String, String>) loadedResource;
            } else {
                throw new RuntimeException("Unexpected the loaded resource for the path " + shaderSourcePath +
                        ", expected Map<String, String>, received " + loadedResource.getClass());
            }
        }

        final GlslParser parser = GlslParser.newInstance();
        final FileDeclarationAstNode result = parser.parseFileDeclaration(shaderSourcePath, sourceMap.get("[main]"));

        for (final Map.Entry<String, String> entry : sourceMap.entrySet()) {

            final String key = entry.getKey();
            if ("[main]".equals(key)) {
                continue;
            }

            final StringValueAstNode importValue = new StringValueAstNode();
            importValue.setValue(key);
            importValue.setText(key);

            final ImportPreprocessorAstNode importNode = new ImportPreprocessorAstNode();
            importNode.setValue(importValue);
            importNode.setText("#" + GlslLang.PR_TYPE_IMPORT + "\"" + key + "\"");

            result.getChildren().add(0, importNode);

            if (USE_AST_CACHE) {
                cache.put(key, parseShaderSource(entry.getValue()));
            }
        }

        if (USE_AST_CACHE) {
            cache.put(shaderSourcePath, result);
        }

        return result;
    }

    /* ======== UTILITY METHODS ============== */

    /**
     * Checks the name of definition.
     *
     * @param definitionName the definition name.
     * @return true if this definition is shader node definition.
     */
    protected boolean isShaderNodeDefinition(final String definitionName) {
        return definitionName.startsWith(INPUT_VAR_DEFINITION_PREFIX) ||
                definitionName.startsWith(OUTPUT_VAR_DEFINITION_PREFIX);
    }

    /**
     * Checks the name of define.
     *
     * @param definitionName the definition name.
     * @return true if this definition is input variable shader node definition.
     */
    protected boolean isShaderNodeInputVarDefinition(final String definitionName) {
        return definitionName.startsWith(INPUT_VAR_DEFINITION_PREFIX);
    }

    /**
     * Checks the name of define.
     *
     * @param definitionName the definition name.
     * @return true if this definition is output variable shader node definition.
     */
    protected boolean isShaderNodeOutputVarDefinition(final String definitionName) {
        return definitionName.startsWith(OUTPUT_VAR_DEFINITION_PREFIX);
    }

    /**
     * Converts the definition name to the result definition name.
     *
     * @param shaderNode the shader node.
     * @param definitionName the definition name.
     * @return the result definition name.
     */
    protected String toResultShaderNodeInputVarDefinition(final ShaderNode shaderNode, final String definitionName) {
        return shaderNode.getName() + "_" + definitionName;
    }

    /**
     * Converts the definition name to the result definition name.
     *
     * @param shaderNode the shader node.
     * @param definitionName the definition name.
     * @return the result definition name.
     */
    protected String toResultShaderNodeOutputVarDefinition(final ShaderNode shaderNode, final String definitionName) {
        return shaderNode.getName() + "_" + definitionName;
    }

    /**
     * Copies variable definitions from the list of define value nodes.
     *
     * @param defineValueNodes        the list of all define value nodes.
     * @param usedVariableDefinitions the lit of used variable definitions.
     */
    protected void copyVariableDefinitions(final List<DefineValueAstNode> defineValueNodes,
                                           final List<String> usedVariableDefinitions) {

        if (defineValueNodes.isEmpty()) {
            return;
        }

        for (final DefineValueAstNode defineValueNode : defineValueNodes) {
            final String value = defineValueNode.getValue();
            if (isShaderNodeInputVarDefinition(value) || isShaderNodeOutputVarDefinition(value)) {
                usedVariableDefinitions.add(value);
            }
        }
    }

    /**
     * Replaces a name of a variable in the source code.
     *
     * @param source  the source code.
     * @param oldName the old name.
     * @param newName the new name.
     * @param result  the result code.
     * @return the builder with result code.
     */
    protected StringBuilder replaceVar(final StringBuilder source, final String oldName, final String newName,
                                       final StringBuilder result) {
        return replace(source, oldName, newName, PREVIOUS_VAR_CHAR_CHECKER, NEXT_VAR_CHAR_CHECKER, result);
    }

    /**
     * Replaces a name of a define in the source code.
     *
     * @param source  the source code.
     * @param oldName the old name.
     * @param newName the new name.
     * @param result  the result code.
     * @return the builder with result code.
     */
    protected StringBuilder replaceDefine(final StringBuilder source, final String oldName, final String newName,
                                          final StringBuilder result) {
        return replace(source, oldName, newName, PREVIOUS_DEFINE_CHAR_CHECKER, NEXT_DEFINE_CHAR_CHECKER, result);
    }

    /**
     * Replaces a name of a method in the source code.
     *
     * @param source  the source code.
     * @param oldName the old name.
     * @param newName the new name.
     * @param result  the result code.
     * @return the builder with result code.
     */
    protected StringBuilder replaceMethod(final StringBuilder source, final String oldName, final String newName,
                                          final StringBuilder result) {
        return replace(source, oldName, newName, PREVIOUS_METHOD_CHAR_CHECKER, NEXT_METHOD_CHAR_CHECKER, result);
    }

    /**
     * Replaces a name in the source code.
     *
     * @param source          the source code.
     * @param oldName         the old name.
     * @param newName         the new name.
     * @param prevCharChecker the checker of a previous char.
     * @param nextCharChecker the checker of a next char.
     * @param result          the result code.
     * @return the builder with result code.
     */
    protected StringBuilder replace(final StringBuilder source, final String oldName, final String newName,
                                    final CharPredicate prevCharChecker, final CharPredicate nextCharChecker,
                                    final StringBuilder result) {

        if (source.indexOf(oldName) == -1) {
            result.append(source);
            return result;
        }

        //String debug = "";

        boolean copyOriginal = false;

        for (int i = 0, first = -1, current = 0, last = -1, length = source.length(); i < length; i++) {

            final char ch = source.charAt(i);

            if (first == -1) {

                if (oldName.charAt(0) != ch) {
                    result.append(ch);
                    continue;
                }

                first = i;
                current = 1;
                //debug = String.valueOf(ch);
                continue;
            }

            //debug += ch;

            if (current < oldName.length() && ch == oldName.charAt(current)) {
                current++;

                if (current >= oldName.length()) {
                    last = i;
                }

            } else if (current == oldName.length()) {
                i--;
                last = i;
            } else {
                last = i;
                copyOriginal = true;
            }

            if (last == -1) {
                continue;
            }

            if (copyOriginal) {
                result.append(source, first, last + 1);
                copyOriginal = false;
                first = -1;
                last = -1;
                continue;
            }

            char prevChar = ' ';
            char afterChar = ' ';

            if (first > 0) {
                prevChar = source.charAt(first - 1);
            }

            if (last < source.length() - 1) {
                afterChar = source.charAt(last + 1);
            }

            boolean canBeReplaced = prevCharChecker.test(prevChar);
            canBeReplaced = canBeReplaced && nextCharChecker.test(afterChar);

            if (canBeReplaced) {
                result.append(newName);
            } else {
                result.append(oldName);
            }

            first = -1;
            last = -1;
        }

        return result;
    }

    /**
     * Clears the string builder.
     *
     * @param builder the string builder.
     * @return the cleared string builder.
     */
    protected StringBuilder clear(final StringBuilder builder) {

        final int length = builder.length();

        if (length < 1) {
            return builder;
        }

        return builder.delete(0, length);
    }
}
