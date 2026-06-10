/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.material;

import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderBufferBlock;
import com.jme3.shader.VarType;
import com.jme3.shader.bufferobject.BufferBindingPoints;
import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.shader.bufferobject.layout.Std140Layout;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the engine-owned {@code m_MatParams} UBO from ordinary material
 * parameters.
 * <p>
 * Materials still expose parameters through the normal {@link MatParam} API.
 * This helper detects whether the active shader declares a compatible
 * {@code layout(std140) uniform m_MatParams} block and, when it does, packs
 * matching material parameters into a single {@link BufferObject}. Parameters
 * that are not present in the block continue through the regular uniform path
 * in {@link Material}.
 * <p>
 * This is intentionally limited to simple std140 declarations. If the parser
 * cannot prove the layout from the shader source, the helper stays inactive so
 * that material updates fall back to individual uniforms.
 */
final class MatParamUniformBuffer {

    private static final Std140Layout STD140 = new Std140Layout();
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?s)/\\*.*?\\*/|//.*?(?=\\R|$)");
    private static final Pattern BLOCK_PATTERN = Pattern.compile(
            "(?s)(?:layout\\s*\\(([^)]*)\\)\\s*)?uniform\\s+(\\w+)\\s*\\{(.*?)\\}\\s*(\\w+)?\\s*;");
    private static final Pattern MEMBER_PATTERN = Pattern.compile(
            "(?s)(?:layout\\s*\\([^)]*\\)\\s*)?(?:highp\\s+|mediump\\s+|lowp\\s+)?(float|int|bool|vec2|vec3|vec4|mat3|mat4)\\s+(.+)");
    private static final Pattern DECLARATOR_PATTERN = Pattern.compile("(\\w+)\\s*(?:\\[\\s*(\\d+)\\s*\\])?\\s*");

    private final BufferObject bufferObject = new BufferObject();
    private Layout layout;
    private Shader shader;
    private Object[] values;
    private boolean[] set;
    private ByteBuffer tempBuffer;

    /**
     * Creates the reusable material-parameter UBO owned by one material
     * instance.
     */
    MatParamUniformBuffer() {
        bufferObject.setName(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME);
        bufferObject.setAccessHint(BufferObject.AccessHint.Dynamic);
        bufferObject.setNatureHint(BufferObject.NatureHint.Draw);
    }

    /**
     * Starts collecting values for the specified shader.
     * <p>
     * The parsed layout is cached per shader instance. When the same shader is
     * used again, only the collected values are reset for the next material
     * update.
     *
     * @param shader the active shader being updated
     */
    void begin(Shader shader) {
        if (this.shader != shader) {
            this.shader = shader;
            this.layout = parseLayout(shader);
            if (layout != null) {
                this.values = new Object[layout.members.size()];
                this.set = new boolean[layout.members.size()];
            } else {
                this.values = null;
                this.set = null;
            }
        } else if (set != null) {
            for (int i = 0; i < set.length; i++) {
                set[i] = false;
                values[i] = null;
            }
        }
    }

    /**
     * Tests whether the active shader contains a supported material-parameter
     * UBO declaration.
     *
     * @return true if matching parameters can be written into the UBO
     */
    boolean isActive() {
        return layout != null;
    }

    /**
     * Attempts to store a material parameter in the collected UBO data.
     *
     * @param param the material parameter or override to store
     * @param override true when {@code param} comes from an override list
     * @return true if the parameter belongs to the UBO and was consumed
     */
    boolean set(MatParam param, boolean override) {
        if (layout == null || param.getValue() == null) {
            return false;
        }

        Member member = layout.membersByParamName.get(param.getName());
        if (member == null || !member.accepts(param.getVarType())) {
            return false;
        }

        int index = member.index;
        if (!override && set[index]) {
            // Overrides are applied before material values and must keep
            // ownership when both target the same UBO member.
            return true;
        }

        Object converted = member.convert(param.getValue());
        if (converted == null) {
            return false;
        }

        values[index] = converted;
        set[index] = true;
        return true;
    }

    /**
     * Clears a UBO-backed parameter by writing its std140 zero value.
     *
     * @param param the disabled override that targets a material parameter
     * @return true if the parameter belongs to the UBO and was consumed
     */
    boolean clear(MatParam param) {
        if (layout == null) {
            return false;
        }

        Member member = layout.membersByParamName.get(param.getName());
        if (member == null || !member.accepts(param.getVarType())) {
            return false;
        }

        values[member.index] = member.zeroValue();
        set[member.index] = true;
        return true;
    }

    /**
     * Writes all collected values into the backing buffer object and attaches
     * it to the shader block.
     * <p>
     * Members that were not explicitly supplied this frame are written as
     * zeroes. This mirrors the behavior of clearing ordinary uniforms when an
     * override removes a value and avoids leaking stale UBO data from a
     * previous material update.
     *
     * @param shader the active shader being updated
     */
    void finish(Shader shader) {
        if (layout == null) {
            return;
        }

        ByteBuffer data = getTempBuffer(layout.size);
        for (Member member : layout.members) {
            Object value = set[member.index] ? values[member.index] : member.zeroValue();
            data.position(member.offset);
            STD140.write(data, value);
        }
        data.clear();

        // Avoid dirtying the BufferObject when the packed byte content did not
        // change; renderer-side uploads can then stay skipped.
        if (!contentEquals(bufferObject.getByteData(), data)) {
            bufferObject.setData(data);
        }

        ShaderBufferBlock block = shader.getBufferBlock(layout.blockName);
        block.setBufferObject(ShaderBufferBlock.BufferType.UniformBufferObject, bufferObject);
    }

    /**
     * Returns the backing buffer object.
     *
     * @return the UBO populated by this helper
     */
    BufferObject getBufferObject() {
        return bufferObject;
    }

    private ByteBuffer getTempBuffer(int size) {
        if (tempBuffer == null || tempBuffer.capacity() < size) {
            tempBuffer = BufferUtils.createByteBuffer(size);
        }
        tempBuffer.clear();
        tempBuffer.limit(size);
        return tempBuffer;
    }

    /**
     * Finds a supported material-parameter UBO declaration in the shader.
     *
     * @param shader the shader to inspect
     * @return the parsed layout, or null if none of the shader sources contains
     * a supported declaration
     */
    static Layout parseLayout(Shader shader) {
        for (Shader.ShaderSource source : shader.getSources()) {
            Layout layout = parseLayout(source.getSource());
            if (layout != null) {
                return layout;
            }
        }
        return null;
    }

    /**
     * Parses a GLSL source string for a supported {@code m_MatParams} block.
     *
     * @param source GLSL source code
     * @return the parsed layout, or null if the source does not declare a
     * supported std140 material-parameter block
     */
    static Layout parseLayout(String source) {
        if (source == null) {
            return null;
        }

        Matcher blockMatcher = BLOCK_PATTERN.matcher(stripComments(source));
        while (blockMatcher.find()) {
            String layoutQualifier = blockMatcher.group(1);
            String blockName = blockMatcher.group(2);
            String body = blockMatcher.group(3);
            String instanceName = blockMatcher.group(4);

            if (!BufferBindingPoints.MAT_PARAMS_BLOCK_NAME.equals(blockName)
                    && !BufferBindingPoints.MAT_PARAMS_BLOCK_NAME.equals(instanceName)) {
                continue;
            }
            if (layoutQualifier == null || !layoutQualifier.toLowerCase(Locale.ROOT).contains("std140")) {
                continue;
            }

            return parseMembers(blockName, body);
        }
        return null;
    }

    /**
     * Parses supported block members and computes their std140 offsets.
     */
    private static Layout parseMembers(String blockName, String body) {
        if (body.indexOf('#') >= 0) {
            // Preprocessor-dependent declarations cannot be sized reliably from
            // the raw source available here.
            return null;
        }

        ArrayList<Member> members = new ArrayList<>();
        int offset = 0;
        int maxAlignment = 0;

        for (String statement : body.split(";")) {
            statement = statement.trim();
            if (statement.isEmpty()) {
                continue;
            }
            if (statement.startsWith("layout")) {
                // Explicit member layouts, such as layout(offset = N), are not
                // interpreted here. Falling back avoids silently using the
                // wrong offset.
                return null;
            }

            Matcher memberMatcher = MEMBER_PATTERN.matcher(statement);
            if (!memberMatcher.matches()) {
                return null;
            }

            String glslType = memberMatcher.group(1);
            String declarations = memberMatcher.group(2);
            for (String declaration : declarations.split(",")) {
                Matcher declaratorMatcher = DECLARATOR_PATTERN.matcher(declaration.trim());
                if (!declaratorMatcher.matches()) {
                    return null;
                }

                String memberName = declaratorMatcher.group(1);
                String arrayLength = declaratorMatcher.group(2);
                Member member = Member.create(members.size(), memberName, glslType,
                        arrayLength == null ? 0 : Integer.parseInt(arrayLength));
                if (member == null) {
                    return null;
                }

                // Compute the same offsets that Std140Layout.write() expects.
                int alignment = STD140.getBasicAlignment(member.zeroValue());
                offset = STD140.align(offset, alignment);
                member.offset = offset;
                offset += STD140.estimateSize(member.zeroValue());
                maxAlignment = Math.max(maxAlignment, alignment);
                members.add(member);
            }
        }

        if (members.isEmpty()) {
            return null;
        }

        int size = STD140.align(offset, STD140.getStructureAlignment(maxAlignment));
        return new Layout(blockName, members, size);
    }

    /**
     * Compares full buffer contents without mutating the caller-visible
     * position or limit of either buffer.
     */
    private static boolean contentEquals(ByteBuffer current, ByteBuffer next) {
        if (current == null || next == null) {
            return current == next;
        }
        ByteBuffer a = current.duplicate();
        ByteBuffer b = next.duplicate();
        a.clear();
        b.clear();
        return a.equals(b);
    }

    /**
     * Removes GLSL block and line comments before applying the simple
     * declaration parser.
     */
    private static String stripComments(String source) {
        return COMMENT_PATTERN.matcher(source).replaceAll("");
    }

    /**
     * Parsed std140 layout for the material-parameter block in one shader.
     */
    static final class Layout {
        final String blockName;
        final ArrayList<Member> members;
        final Map<String, Member> membersByParamName = new HashMap<>();
        final int size;

        /**
         * Creates a parsed block layout.
         *
         * @param blockName the actual GLSL block name to bind
         * @param members ordered block members with computed offsets
         * @param size total std140 block size in bytes
         */
        Layout(String blockName, ArrayList<Member> members, int size) {
            this.blockName = blockName;
            this.members = members;
            this.size = size;
            for (Member member : members) {
                membersByParamName.put(member.paramName, member);
            }
        }

        /**
         * Finds a block member by material-parameter name.
         *
         * @param paramName unprefixed material parameter name
         * @return the matching member, or null if the parameter is not stored in
         * this block
         */
        Member getMember(String paramName) {
            return membersByParamName.get(paramName);
        }
    }

    /**
     * One scalar, vector, matrix, or array entry in the parsed block.
     */
    static final class Member {
        final int index;
        final String name;
        final String paramName;
        final String glslType;
        final int arrayLength;
        final Object zeroValue;
        final VarType varType;
        int offset;

        /**
         * Creates a parsed block member.
         */
        private Member(int index, String name, String glslType, int arrayLength, Object zeroValue, VarType varType) {
            this.index = index;
            this.name = name;
            this.paramName = name.startsWith("m_") ? name.substring(2) : name;
            this.glslType = glslType;
            this.arrayLength = arrayLength;
            this.zeroValue = zeroValue;
            this.varType = varType;
        }

        /**
         * Creates a member for a supported GLSL type.
         *
         * @param index declaration order within the block
         * @param name GLSL member name
         * @param glslType GLSL type token
         * @param arrayLength array length, or zero for non-array members
         * @return a member definition, or null for unsupported GLSL types
         */
        static Member create(int index, String name, String glslType, int arrayLength) {
            boolean array = arrayLength > 0;
            switch (glslType) {
                case "float":
                    return new Member(index, name, glslType, arrayLength,
                            array ? new Float[arrayLength] : Float.valueOf(0), array ? VarType.FloatArray : VarType.Float);
                case "int":
                    return new Member(index, name, glslType, arrayLength,
                            array ? new Integer[arrayLength] : Integer.valueOf(0), array ? VarType.IntArray : VarType.Int);
                case "bool":
                    return new Member(index, name, glslType, arrayLength,
                            array ? new Boolean[arrayLength] : Boolean.FALSE, array ? null : VarType.Boolean);
                case "vec2":
                    return new Member(index, name, glslType, arrayLength,
                            array ? new Vector2f[arrayLength] : new Vector2f(), array ? VarType.Vector2Array : VarType.Vector2);
                case "vec3":
                    return new Member(index, name, glslType, arrayLength,
                            array ? new Vector3f[arrayLength] : new Vector3f(), array ? VarType.Vector3Array : VarType.Vector3);
                case "vec4":
                    return new Member(index, name, glslType, arrayLength,
                            array ? new Vector4f[arrayLength] : new Vector4f(), array ? VarType.Vector4Array : VarType.Vector4);
                case "mat3":
                    return new Member(index, name, glslType, arrayLength,
                            array ? new Matrix3f[arrayLength] : new Matrix3f(Matrix3f.ZERO), array ? VarType.Matrix3Array : VarType.Matrix3);
                case "mat4":
                    return new Member(index, name, glslType, arrayLength,
                            array ? new Matrix4f[arrayLength] : new Matrix4f(Matrix4f.ZERO), array ? VarType.Matrix4Array : VarType.Matrix4);
                default:
                    return null;
            }
        }

        /**
         * Tests whether a material parameter can supply this member.
         *
         * @param type material parameter type
         * @return true if the VarType exactly matches the parsed GLSL member
         */
        boolean accepts(VarType type) {
            return type == varType;
        }

        /**
         * Creates a zero value suitable for writing this member with the std140
         * serializer.
         *
         * @return a scalar zero or a fresh zero-filled array
         */
        Object zeroValue() {
            if (!zeroValue.getClass().isArray()) {
                return zeroValue;
            }
            switch (glslType) {
                case "float":
                    return filledFloatArray();
                case "int":
                    return filledIntArray();
                case "bool":
                    return filledBoolArray();
                case "vec2":
                    return filledVector2Array();
                case "vec3":
                    return filledVector3Array();
                case "vec4":
                    return filledVector4Array();
                case "mat3":
                    return filledMatrix3Array();
                case "mat4":
                    return filledMatrix4Array();
                default:
                    return zeroValue;
            }
        }

        /**
         * Converts material values to the exact object shape expected by
         * {@link Std140Layout}. Array values are copied and padded with zeroes
         * so the backing buffer always receives the declared GLSL array length.
         *
         * @param value material parameter value
         * @return converted value, or null if this member cannot convert the
         * supplied Java value
         */
        Object convert(Object value) {
            if (arrayLength == 0) {
                return value;
            }
            switch (glslType) {
                case "float":
                    return copyFloatArray(value);
                case "int":
                    return copyIntArray(value);
                case "vec2":
                    return copyVector2Array((Vector2f[]) value);
                case "vec3":
                    return copyVector3Array((Vector3f[]) value);
                case "vec4":
                    return copyVector4Array((Vector4f[]) value);
                case "mat3":
                    return copyMatrix3Array((Matrix3f[]) value);
                case "mat4":
                    return copyMatrix4Array((Matrix4f[]) value);
                default:
                    return null;
            }
        }

        private Float[] copyFloatArray(Object value) {
            Float[] result = filledFloatArray();
            if (value instanceof float[]) {
                float[] source = (float[]) value;
                for (int i = 0; i < Math.min(source.length, result.length); i++) {
                    result[i] = source[i];
                }
            } else {
                Float[] source = (Float[]) value;
                System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
            }
            return result;
        }

        private Integer[] copyIntArray(Object value) {
            Integer[] result = filledIntArray();
            if (value instanceof int[]) {
                int[] source = (int[]) value;
                for (int i = 0; i < Math.min(source.length, result.length); i++) {
                    result[i] = source[i];
                }
            } else {
                Integer[] source = (Integer[]) value;
                System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
            }
            return result;
        }

        private Float[] filledFloatArray() {
            Float[] result = new Float[arrayLength];
            for (int i = 0; i < result.length; i++) result[i] = 0f;
            return result;
        }

        private Integer[] filledIntArray() {
            Integer[] result = new Integer[arrayLength];
            for (int i = 0; i < result.length; i++) result[i] = 0;
            return result;
        }

        private Boolean[] filledBoolArray() {
            Boolean[] result = new Boolean[arrayLength];
            for (int i = 0; i < result.length; i++) result[i] = false;
            return result;
        }

        private Vector2f[] filledVector2Array() {
            Vector2f[] result = new Vector2f[arrayLength];
            for (int i = 0; i < result.length; i++) result[i] = new Vector2f();
            return result;
        }

        private Vector3f[] filledVector3Array() {
            Vector3f[] result = new Vector3f[arrayLength];
            for (int i = 0; i < result.length; i++) result[i] = new Vector3f();
            return result;
        }

        private Vector4f[] filledVector4Array() {
            Vector4f[] result = new Vector4f[arrayLength];
            for (int i = 0; i < result.length; i++) result[i] = new Vector4f();
            return result;
        }

        private Matrix3f[] filledMatrix3Array() {
            Matrix3f[] result = new Matrix3f[arrayLength];
            for (int i = 0; i < result.length; i++) result[i] = new Matrix3f(Matrix3f.ZERO);
            return result;
        }

        private Matrix4f[] filledMatrix4Array() {
            Matrix4f[] result = new Matrix4f[arrayLength];
            for (int i = 0; i < result.length; i++) result[i] = new Matrix4f(Matrix4f.ZERO);
            return result;
        }

        private Vector2f[] copyVector2Array(Vector2f[] source) {
            Vector2f[] result = filledVector2Array();
            System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
            return result;
        }

        private Vector3f[] copyVector3Array(Vector3f[] source) {
            Vector3f[] result = filledVector3Array();
            System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
            return result;
        }

        private Vector4f[] copyVector4Array(Vector4f[] source) {
            Vector4f[] result = filledVector4Array();
            System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
            return result;
        }

        private Matrix3f[] copyMatrix3Array(Matrix3f[] source) {
            Matrix3f[] result = filledMatrix3Array();
            System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
            return result;
        }

        private Matrix4f[] copyMatrix4Array(Matrix4f[] source) {
            Matrix4f[] result = filledMatrix4Array();
            System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length));
            return result;
        }
    }
}
