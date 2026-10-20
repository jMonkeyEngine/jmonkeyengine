package com.jme3.vulkan.shadergen;

import com.jme3.backend.GraphicsAPI;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.shadergen.vars.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Shader {

    protected ShaderLogic logic;
    private final IfChain ifchain = new IfChain();
    private final String namespace = getClass().getName().replace('.', '_');
    private final Map<Integer, BitSet> bindings = new HashMap<>();

    public String compile(GraphicsAPI target, ShaderStage stage) {
        return compile(target, stage, false);
    }

    /**
     * Compiles the shader stage for the target graphics API.
     *
     * @param target target graphics API
     * @param stage stage to compile (must be present)
     * @param debug true to make the compiled shader code as debuggable as possible
     * @return shader code
     */
    public String compile(GraphicsAPI target, ShaderStage stage, boolean debug) {
        bindings.clear();
        logic = ShaderLogic.init(debug);
        ShadingLangVersion version = getClass().getAnnotation(ShadingLangVersion.class);
        logic.addHeaderLine("#version %d", version != null ? version.glsl() : 460);
        compileGlobals(target);
        Method entry = findEntryPoint(stage);
        Var[] args = compileFunction(entry, "main");
        if (args != null) {
            compileEntryPointArguments(entry, args);
        }
        return logic.compileBlocks();
    }

    private Method findEntryPoint(ShaderStage stage) {
        Method[] methods = getClass().getDeclaredMethods();
        for (Method m : methods) {
            EntryPoint entry = m.getAnnotation(EntryPoint.class);
            if (entry != null && entry.value() == stage) {
                return m;
            }
        }
        throw new NullPointerException("Entry point not found: " + stage);
    }

    private void compileEntryPointArguments(Method entry, Var[] args) {
        Parameter[] params = entry.getParameters();
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            In in = p.getAnnotation(In.class);
            Out out = p.getAnnotation(Out.class);
            if ((in != null || out != null) && BuiltIn.class.isAssignableFrom(p.getType())) {
                throw new IllegalStateException("Built-in variables cannot be in or out.");
            }
            if (in != null) {
                if (out != null) throw new IllegalStateException("Shader parameter cannot be both in and out.");
                if (BuiltIn.class.isAssignableFrom(p.getType())) {
                    throw new IllegalStateException("Built-in variables cannot be inputs.");
                }
                logic.addInputVarying(in.value(), args[i]);
            } else if (out != null) {
                if (BuiltIn.class.isAssignableFrom(p.getType())) {
                    throw new IllegalStateException("Built-in variables cannot be outputs.");
                }
                logic.addOutputVarying(out.value(), args[i]);
            }
        }
    }

    private Var[] compileFunction(Method func, String shaderName) {
        if (logic.startBlock(func)) {
            Parameter[] params = func.getParameters();
            Var[] args = new Var[params.length];
            StringBuilder signiture = new StringBuilder();
            signiture.append(Var.getVariableType(getFunctionReturnType(func)));
            signiture.append(' ').append(shaderName).append('(');
            for (int i = 0; i < params.length; i++) {
                Parameter p = params[i];
                args[i] = Var.createVariable(p.getType(), p.getName());
                if (i > 0) signiture.append(',');
                if (p.getAnnotation(Out.class) != null) {
                    if (p.getAnnotation(In.class) != null) {
                        signiture.append("inout ");
                    } else {
                        signiture.append("out ");
                    }
                }
                signiture.append(args[i].getType()).append(' ').append(p.getName());
            }
            signiture.append("){");
            logic.add(signiture.toString());
            func.setAccessible(true);
            try {
                func.invoke(this, (Object[])args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            logic.add("}");
            logic.endBlock();
            return args;
        }
        return null;
    }

    private Method getFunction(String name, Var... args) {
        Class[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }
        try {
            return getClass().getDeclaredMethod(name, types);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Function \"" + name + "\" does not exist.", e);
        }
    }

    private Class getFunctionReturnType(Method func) {
        Return ret = func.getAnnotation(Return.class);
        return ret != null ? ret.value() : void.class;
    }

    private String buildFunctionCallSite(String shaderName, Var... args) {
        StringBuilder callSite = new StringBuilder(shaderName).append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) callSite.append(',');
            callSite.append(args[i].toString());
        }
        return callSite.append(')').toString();
    }

    /**
     * Scans this shader's declared instance fields for ones annotated {@link Uniform}
     * or {@link Storage}, and declares each as a uniform or shader storage buffer,
     * assigning it a descriptor set/binding and re-pointing the field's current
     * {@link StructVar} value at the declared instance so ordinary field access (e.g.
     * {@code scene.someField}) reaches it directly, without an intermediate accessor.
     * <p>
     * A binding is only auto-assigned the next free index within its set (the same way
     * slang assigns bindings that aren't explicitly reserved) when {@link Binding} is
     * absent or its {@code binding} is left at its default of -1.
     */
    private void compileGlobals(GraphicsAPI target) {
        for (Field f : getClass().getDeclaredFields()) {
            Uniform uniform = f.getAnnotation(Uniform.class);
            Storage storage = f.getAnnotation(Storage.class);
            if (uniform == null && storage == null) {
                continue;
            }
            if (uniform != null && storage != null) {
                throw new IllegalStateException("Field \"" + f.getName() + "\" cannot be both @Uniform and @Storage.");
            }
            String kind = uniform != null ? "uniform" : "buffer";
            if (!StructVar.class.isAssignableFrom(f.getType())) {
                throw new IllegalStateException("Field \"" + f.getName() + "\" must be a StructVar to be a " + kind + " buffer.");
            }
            f.setAccessible(true);
            StructVar value;
            try {
                value = (StructVar)f.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (value == null) {
                throw new IllegalStateException("Field \"" + f.getName() + "\" must be initialized to declare it as a " + kind + " buffer.");
            }
            if (logic.startBlock(f)) {
                Binding location = f.getAnnotation(Binding.class);
                int set = location != null && target != GraphicsAPI.OpenGL ? location.set() : 0;
                int binding = getBindingNum(set, location != null ? location.binding() : -1);
                Layout layout = f.getAnnotation(Layout.class);
                String layoutType = layout != null ? layout.value() : StructLayout.std140.getIdentifier();
                String id = logic.generateVariableName();
                StringBuilder decl = new StringBuilder("layout(").append(layoutType);
                if (target != GraphicsAPI.OpenGL) {
                    decl.append(",set=").append(set);
                }
                // the block name is essentially lost here, which is fine since we're using the binding numbers instead
                decl.append(",binding=").append(binding).append(") ").append(kind)
                        .append(' ').append(logic.generateVariableName()).append('{');
                for (StructVar.Member m : value.getFields()) {
                    decl.append(m.getType()).append(' ').append(m.getName()).append(';');
                }
                logic.add(decl.append("} ").append(id).append(';').toString());
                logic.endBlock();
                value.bindAsBlock(id);
            }
        }
    }

    private int getBindingNum(int set, int binding) {
        BitSet bits = bindings.computeIfAbsent(set, k -> new BitSet());
        if (binding < 0 || bits.get(binding)) {
            binding = bits.nextClearBit(Math.max(binding, 0));
        }
        bits.set(binding);
        return binding;
    }

    /**
     * Compiles the function and emits a variable that captures the return type.
     * Only emits code at the call site if the function returns void.
     *
     * @param name name of the function to call
     * @param returnType return variable type
     * @param args arguments passed to the function
     * @return variable capturing the return value
     * @param <T> return variable type
     */
    protected <T extends Var> Result<T> call(String name, Class<T> returnType, Var... args) {
        return call(name, args);
    }

    protected <T extends Var> Result<T> call(String name, Var... args) {
        return call(getFunction(name), args);
    }

    /**
     * Compiles the function and emits a variable representing the function call site.
     *
     * @param func function to compile and call
     * @param args arguments to pass into the function
     * @return variable returned from the function, or null if the function returned void
     * @param <T> returned variable type
     */
    @SuppressWarnings("unchecked")
    private <T extends Var> Result<T> call(Method func, Var... args) {
        String shaderName = namespace + '_' + func.getName();
        compileFunction(func, shaderName);
        String callSite = buildFunctionCallSite(shaderName, args);
        Class ret = getFunctionReturnType(func);
        if (ret != void.class) {
            return new Result<>((T)Var.createVariable(ret, callSite));
        } else {
            logic.add(callSite);
            logic.add(";");
            return null;
        }
    }

    protected Int Int(int i) {
        return new Int(i);
    }

    protected Flt Float(float f) {
        return new Flt(f);
    }

    protected Vec2 vec2(float x, float y) {
        return new Vec2(x, y);
    }

    protected Vec3 vec3(float x, float y, float z) {
        return new Vec3(x, y, z);
    }

    protected Vec4 vec4(float x, float y, float z, float w) {
        return new Vec4(x, y, z, w);
    }

    protected IfChain If(Bool condition, Runnable body) {
        logic.add(String.format("if(%s){", condition));
        body.run();
        logic.add("}");
        return ifchain;
    }

    protected void While(Bool condition, Runnable body) {
        logic.add(String.format("while(%s){", condition));
        body.run();
        logic.add("}");
    }

    protected <T extends Var> void For(T iterator, Function<T, Bool> condition, Consumer<T> update, Consumer<T> body) {
        logic.add("for(");
        iterator.declare();
        logic.add(condition.apply(iterator).toString());
        logic.add(";){");
        body.accept(iterator);
        update.accept(iterator);
        logic.add("}");
    }

    protected <T extends Var> void For(Runnable body) {
        logic.add("for(;;){");
        body.run();
        logic.add("}");
    }

    protected void For(int start, int end, Consumer<Int> body) {
        logic.add("for(");
        Int i = new Int(start).declare();
        logic.add(i.lt(end).toString());
        logic.add(";");
        logic.add(i.getAndIncrement().toString());
        logic.add("){");
        body.accept(i);
        logic.add("}");
    }

    protected void For(int start, Int end, Consumer<Int> body) {
        logic.add("for(");
        Int i = new Int(start).declare();
        logic.add(i.lt(end).toString());
        logic.add(";");
        logic.add(i.getAndIncrement().toString());
        logic.add("){");
        body.accept(i);
        logic.add("}");
    }

    protected void For(Int start, Int end, Consumer<Int> body) {
        logic.add("for(");
        Int i = new Int(start).declare();
        logic.add(i.lt(end).toString());
        logic.add(";");
        logic.add(i.getAndIncrement().toString());
        logic.add("){");
        body.accept(i);
        logic.add("}");
    }

    protected void Discard() {
        logic.add("discard;");
    }

    protected void Break() {
        logic.add("break;");
    }

    protected void Continue() {
        logic.add("continue;");
    }

    protected void Return(Var ret) {
        logic.add(String.format("return %s;", ret));
    }

    /**
     * If in debug mode, inserts a line directive into the shader source code that more
     * accurately pinpoints where the following shader code is generated from in Java.
     *
     * <p>The call site to this method is assumed to be generating the shader code.
     * Otherwise, the line directive will be inaccurate.</p>
     */
    protected void insertDebugTrace() {
        if (logic.isDebug()) {
            StackTraceElement[] el = Thread.currentThread().getStackTrace();
            if (el.length <= 1) {
                throw new IllegalStateException("Not enough information to insert a proper trace here.");
            }
            logic.addDirective("#line %d \"%s.%s\"", el[1].getLineNumber(), el[1].getClassName(), el[1].getMethodName());
        }
    }

    protected class IfChain {

        private IfChain() {}

        public IfChain ElseIf(Bool condition, Runnable body) {
            logic.add("else if(");
            logic.add(condition.toString());
            logic.add("){");
            body.run();
            logic.add("}");
            return this;
        }

        public void Else(Runnable body) {
            logic.add("else{");
            body.run();
            logic.add("}");
        }

    }

    public class Result <T extends Var> {

        private final T var;

        private Result(T var) {
            this.var = var;
        }

        public T get() {
            return var;
        }

        public void Void() {
            logic.add("%s;", var);
        }

    }

    private interface BuiltIn { }

    public static class VertexId extends Int implements BuiltIn {

        public VertexId() {
            super("gl_VertexId");
        }

        @Override
        public Int declare(String variableName) {
            throw new UnsupportedOperationException("Build-in variables cannot be declared.");
        }

    }

    public static class ClipPosition extends Vec4 implements BuiltIn {

        public ClipPosition() {
            super("gl_Position");
        }

        @Override
        public Vec4 declare(String variableName) {
            throw new UnsupportedOperationException("Built-in variables cannot be declared.");
        }
    }

}
