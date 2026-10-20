package com.jme3.vulkan.shadergen;

import com.jme3.backend.GraphicsAPI;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.shadergen.vars.*;

import java.lang.annotation.Annotation;
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
    private final int version = getSourceVersion();
    protected BuiltIn gl;

    private int getSourceVersion() {
        ShadingLangVersion v = getClass().getAnnotation(ShadingLangVersion.class);
        return v != null ? v.glsl() : 460;
    }

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
        logic = ShaderLogic.init(target, debug);
        logic.addHeaderLine("#version %d", version);
        gl = new BuiltIn();
        compileGlobals();
        Method entry = findEntryPoint(stage);
        Work work = entry.getAnnotation(Work.class);
        if (work != null) {
            logic.addHeaderLine("layout(local_size_x=%d,local_size_y=%d,local_size_z=%d)in;",
                    work.x(), work.y(), work.z());
        }
        Var[] args = compileFunction(entry, "main", false);
        if (args == null) {
            throw new IllegalStateException("Entry point was not compiled.");
        }
        compileEntryPointArguments(entry, args);
        return logic.compileBlocks();
    }

    protected void compileAsLibrary(Shader host) {
        if (host.version < version) {
            throw new IllegalStateException("Library uses a newer source version than its host.");
        }
        bindings.clear();
        logic = host.logic;
        gl = new BuiltIn();
        compileGlobals();
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
            if (in != null) {
                if (out != null) throw new IllegalStateException("Shader parameter cannot be both in and out.");
                logic.addInputVarying(in.value(), args[i]);
            } else if (out != null) {
                logic.addOutputVarying(out.value(), args[i]);
            }
        }
    }

    private Var[] compileFunction(Method func, String shaderName, boolean writeArguments) {
        if (logic.startBlock(func)) {
            Parameter[] params = func.getParameters();
            Var[] args = new Var[params.length];
            StringBuilder signiture = new StringBuilder();
            signiture.append(Var.getVariableType(getFunctionReturnType(func)));
            signiture.append(' ').append(shaderName).append('(');
            for (int i = 0; i < params.length; i++) {
                Parameter p = params[i];
                args[i] = Var.createElementVariable(p.getType(), p, p.getName());
                if (writeArguments) {
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
    private void compileGlobals() {
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
                int set = location != null && logic.getTarget() != GraphicsAPI.OpenGL ? location.set() : 0;
                int binding = getBindingNum(set, location != null ? location.binding() : -1);
                Layout layout = f.getAnnotation(Layout.class);
                String layoutType = layout != null ? layout.value() : StructLayout.std140.getIdentifier();
                String id = logic.generateVariableName();
                StringBuilder decl = new StringBuilder("layout(").append(layoutType);
                if (logic.getTarget() != GraphicsAPI.OpenGL) {
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
        if (func.getAnnotation(EntryPoint.class) != null) {
            throw new IllegalArgumentException("Cannot invoke entry point from a shader.");
        }
        String shaderName = namespace + '_' + func.getName();
        compileFunction(func, shaderName, true);
        String callSite = buildFunctionCallSite(shaderName, args);
        Class ret = getFunctionReturnType(func);
        if (ret != void.class) {
            return new Result<>((T)Var.createElementVariable(ret, func, callSite));
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

    protected void DoWhile(Runnable body, Bool condition) {
        logic.add("do{");
        body.run();
        logic.add("}while(%s);", condition);
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

    protected <T extends Shader> T Import(T library) {
        library.compileAsLibrary(this);
        return library;
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

        public T use() {
            return var;
        }

        public void Void() {
            logic.add("%s;", var);
        }

    }

    protected class BuiltIn {

        private BuiltIn() {}

        public final Int vertexIndex = new Int(logic.getTarget() == GraphicsAPI.Vulkan ? "gl_VertexIndex" : "gl_VertexID");
        public final Int instanceIndex = new Int(logic.getTarget() == GraphicsAPI.Vulkan ? "gl_InstanceIndex" : "gl_InstanceID");
        public final Flt pointSize = new Flt("gl_PointSize");
        public final Int drawId = new Int("gl_DrawID");
        public final Int baseVertex = new Int("gl_BaseVertex");
        public final Vec4 clipPosition = new Vec4("gl_Position");

    }

}
