package com.jme3.vulkan.shadergen.vars;

import com.jme3.util.struct.SingletonStructArray;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructArray;
import com.jme3.util.struct.StructField;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;
import com.jme3.vulkan.shadergen.ShaderLogic;

import java.util.function.Function;

public abstract class StructVar extends Struct<StructVar.Member> implements Var {

    private final ShaderLogic logic = ShaderLogic.get();
    private final String type = getClass().getName().replace('.', '_');
    protected String exp;
    private boolean isBlock = false;

    public StructVar(Var... vars) {
        StringBuilder dec = new StringBuilder(type).append('(');
        for (int i = 0; i < vars.length; i++) {
            if (i > 0) dec.append(',');
            dec.append(vars[i]);
        }
        exp = dec.append(')').toString();
    }

    public StructVar(String exp) {
        this.exp = exp;
    }

    @Override
    public String toString() {
        return exp;
    }

    @Override
    public String getType() {
        declareType();
        return type;
    }

    @Override
    public StructVar declare() {
        return declare(logic.generateVariableName());
    }

    @Override
    public StructVar declare(String variableName) {
        declareType();
        StructVar var = create(variableName);
        logic.add("%s %s=%s;", type, var.exp, exp);
        return var;
    }

    @Override
    public StructVar create(String exp) {
        return (StructVar)Var.createVariable(getClass(), exp);
    }

    public void setRef(String ref) {
        this.exp = ref;
    }

    /**
     * Points this struct at an already-declared uniform or shader storage buffer
     * instance named {@code ref}, and marks this struct so {@link #declareType()} is
     * never triggered for it. A uniform/storage block is declared through its own
     * {@code layout(...) uniform/buffer Name{...} ref;} syntax rather than a
     * {@code struct} declaration, so this struct's type must never be separately
     * emitted once it's bound to one.
     *
     * @param ref the block instance's declared name
     */
    public void bindAsBlock(String ref) {
        this.exp = ref;
        this.isBlock = true;
    }

    /**
     * Emits this struct's GLSL {@code struct Name{...};} declaration the first time it
     * is needed, so declaring a variable of this type, or referencing it as a member of
     * another struct, always has a valid type already visible. Does nothing on later
     * calls for the same struct class, and does nothing at all once this struct has been
     * {@link #bindAsBlock(String) bound as a uniform or storage block}, since such a
     * struct's type is never separately declared.
     */
    private void declareType() {
        if (isBlock) {
            return;
        }
        if (logic.startBlock(type)) {
            StringBuilder decl = new StringBuilder("struct ").append(type).append('{');
            for (Member f : getFields()) {
                decl.append(f.getType()).append(' ').append(f.getName()).append(';');
            }
            logic.add(decl.append("};").toString());
            logic.endBlock();
        }
    }

    /**
     * A named member of a {@link StructVar}: a {@link Var} expression reachable as
     * {@code parent.name}, that also describes how its value is laid out for
     * {@link #bind(GpuBuffer, int) buffer binding} purposes.
     */
    public interface Member extends StructField {

        /**
         * Gets the text this member is declared under inside its struct's body, e.g.
         * {@code "position"} or, for an array member, {@code "position[4]"}.
         *
         * @return struct body declarator
         */
        String getName();

        /**
         * Gets the GLSL type of this member.
         *
         * @return GLSL type name
         */
        String getType();

    }

    /**
     * A leaf struct member: a plain value (such as a float or vector) both describable
     * for buffer binding (through the inherited {@link Struct.Field} alias) and usable
     * as a {@link Var} expression (through {@code var}).
     *
     * @param <T> CPU-side value type
     * @param <V> GLSL variable type
     */
    public class Field <T, V extends Var> extends Struct.Field<T> implements Var, Member {

        private final String name;
        private final V var;

        public Field(T alias, Function<String, V> factory) {
            this(logic.generateVariableName(), alias, factory);
        }

        public Field(String name, T alias, Function<String, V> factory) {
            super(alias);
            this.name = name;
            this.var = factory.apply(exp + '.' + name);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return StructVar.this.exp + '.' + name;
        }

        @Override
        public String getType() {
            return var.getType();
        }

        @Override @SuppressWarnings("unchecked")
        public V declare() {
            return (V) var.create(toString()).declare();
        }

        @Override @SuppressWarnings("unchecked")
        public V declare(String variableName) {
            return (V) var.create(toString()).declare(variableName);
        }

        @Override @SuppressWarnings("unchecked")
        public V create(String exp) {
            return (V) var.create(exp);
        }

        public V var() {
            return var;
        }

    }

    /**
     * A nested struct member: a sub-{@link StructVar} reachable as {@code parent.name}.
     *
     * @param <T> nested struct type
     */
    public class SubStruct <T extends StructVar> implements Var, Member {

        private final String name;
        private final T struct;

        public SubStruct(T struct) {
            this(logic.generateVariableName(), struct);
        }

        public SubStruct(String name, T struct) {
            this.name = name;
            this.struct = struct;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getType() {
            return struct.getType();
        }

        @Override
        public String toString() {
            return StructVar.this.exp + '.' + name;
        }

        @Override @SuppressWarnings("unchecked")
        public T declare() {
            return (T) struct.create(toString()).declare();
        }

        @Override @SuppressWarnings("unchecked")
        public T declare(String variableName) {
            return (T) struct.create(toString()).declare(variableName);
        }

        @Override @SuppressWarnings("unchecked")
        public T create(String exp) {
            return (T) struct.create(exp);
        }

        /**
         * Gets the nested struct, re-aliased to this field's current expression so it
         * can be used directly, e.g. {@code subStruct.struct().someField}.
         *
         * @return the nested struct
         */
        public T struct() {
            struct.setRef(toString());
            return struct;
        }

        @Override
        public void setupLayout(StructLayout layout) {
            struct.setupLayout(layout);
        }

        @Override
        public void bind(GpuBuffer buffer, int offset) {
            struct.bind(buffer, offset);
        }

        @Override
        public void unbind() {
            struct.unbind();
        }

        @Override
        public DataBuffer cache() {
            return struct.cache();
        }

        @Override
        public int alignment() {
            return struct.alignment();
        }

        @Override
        public int size() {
            return struct.size();
        }

    }

    /**
     * A fixed-length array of nested struct members, reachable as {@code parent.name}.
     *
     * @param <T> nested struct element type
     */
    public class SubStructArray <T extends StructVar> implements Var, Member {

        private final String name;
        private final int length;
        private final T struct;
        private final StructArray<T> array;

        public SubStructArray(String name, int length, T struct) {
            this.name = name;
            this.length = length;
            this.struct = struct;
            this.array = new SingletonStructArray<>(length, struct);
        }

        @Override
        public String getName() {
            return name + '[' + length + ']';
        }

        @Override
        public String getType() {
            return struct.getType();
        }

        @Override
        public String toString() {
            return StructVar.this.exp + '.' + name;
        }

        @Override
        public StructArrayVar<T> declare() {
            return declare(logic.generateVariableName());
        }

        @Override
        public StructArrayVar<T> declare(String variableName) {
            logic.add("%s %s[%d]=%s;", struct.getType(), variableName, length, toString());
            return new StructArrayVar<>(variableName, struct, length);
        }

        @Override
        public StructArrayVar<T> create(String exp) {
            return new StructArrayVar<>(exp, struct, length);
        }

        /**
         * Gets the array element at compile-time index {@code i} as a struct expression,
         * e.g. {@code parent.name[2]}.
         *
         * @param i element index
         * @return struct expression for the element at {@code i}
         */
        @SuppressWarnings("unchecked")
        public T idx(int i) {
            return (T) struct.create(toString() + '[' + i + ']');
        }

        /**
         * Gets the underlying {@link StructArray}, for binding this member's elements to
         * buffer memory and reading or writing their values.
         *
         * @return buffer-side struct array
         */
        public StructArray<T> array() {
            return array;
        }

        public Int length() {
            return new Int(length);
        }

        @Override
        public void setupLayout(StructLayout layout) {
            array.setupLayout(layout);
        }

        @Override
        public void bind(GpuBuffer buffer, int offset) {
            array.bind(buffer, offset);
        }

        @Override
        public void unbind() {
            array.unbind();
        }

        @Override
        public DataBuffer cache() {
            return array.cache();
        }

        @Override
        public int alignment() {
            return array.alignment();
        }

        @Override
        public int size() {
            return array.size();
        }

    }

    /**
     * A standalone GLSL variable holding a fixed-length array of struct values, such as
     * from {@link SubStructArray#declare()} or {@link SubStructArray#create(String)}.
     *
     * @param <T> struct element type
     */
    public class StructArrayVar <T extends StructVar> implements Var {

        private final String ref;
        private final T struct;
        private final int length;

        protected StructArrayVar(String ref, T struct, int length) {
            this.ref = ref;
            this.struct = struct;
            this.length = length;
        }

        @Override
        public String toString() {
            return ref;
        }

        @Override
        public String getType() {
            return struct.getType();
        }

        @Override
        public StructArrayVar<T> declare() {
            return declare(logic.generateVariableName());
        }

        @Override
        public StructArrayVar<T> declare(String variableName) {
            logic.add("%s %s[%d]=%s;", struct.getType(), variableName, length, ref);
            return new StructArrayVar<>(variableName, struct, length);
        }

        @Override
        public StructArrayVar<T> create(String exp) {
            return new StructArrayVar<>(exp, struct, length);
        }

        /**
         * Gets the array element at compile-time index {@code i} as a struct expression,
         * e.g. {@code arrayVar[2]}.
         *
         * @param i element index
         * @return struct expression for the element at {@code i}
         */
        @SuppressWarnings("unchecked")
        public T idx(int i) {
            return (T) struct.create(ref + '[' + i + ']');
        }

        public Int length() {
            return new Int(length);
        }

    }

}
