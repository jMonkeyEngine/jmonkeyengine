package com.jme3.vulkan.shadergen;

import com.jme3.backend.GraphicsAPI;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.devices.LogicalDevice;
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

    protected final GraphicsAPI target = GraphicsAPI.Vulkan;
    protected final String name;
    protected ShaderLogic logic;
    private final String namespace = getClass().getName().replace('.', '_');
    private final Map<Integer, BitSet> bindings = new HashMap<>();
    private final int version = getSourceVersion();
    private final Map<Location, Global<Var>> availableGlobals = new HashMap<>();
    private final Map<Integer, Map<Integer, Global<Var>>> usedGlobals = new HashMap<>();
    protected BuiltIn gl;
    protected final GlslMath math = new GlslMath();

    public Shader() {
        this("default");
    }

    public Shader(String name) {
        this.name = name;
    }

    private int getSourceVersion() {
        ShadingLangs v = getClass().getAnnotation(ShadingLangs.class);
        return v != null ? v.glsl() : 460;
    }

    public String compile(ShaderStage stage) {
        return compile(stage);
    }

    /**
     * Compiles the shader stage for the target graphics API.
     *
     * @param stage stage to compile (must be present)
     * @param debug true to make the compiled shader code as debuggable as possible
     * @return the compiled stage, including the GLSL source and the resource binding
     *         metadata a {@link com.jme3.vulkan.material.experimental.ShadingTechnique}
     *         needs to bind it
     */
    public String compile(ShaderStage stage, boolean debug) {
        bindings.clear();
        logic = ShaderLogic.init(this, target, stage, debug);
        logic.addHeaderLine("#version %d", version);
        gl = new BuiltIn();
        compileGlobals(this);
        Method entry = findEntryPoint(stage, name);
        compileComputeWork(entry, stage);
        Var[] args = compileFunction(entry, "main", false);
        if (args == null) {
            throw new IllegalStateException("Entry point was not compiled.");
        }
        compileEntryPointArguments(entry, args);
        return logic.compileBlocks();
    }

    public Map<Integer, DescriptorSetLayout> createLayouts(LogicalDevice<?> device) {
        Map<Integer, DescriptorSetLayout> results = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Global<Var>>> e : usedGlobals.entrySet()) {
            if (Location.isDescriptorSet(e.getKey())) {
                DescriptorSetLayout.Info info = DescriptorSetLayout.build();
                for (Global<Var> g : e.getValue().values()) {
                    info.addBinding(g.getLocation().getBinding(), g.getType(), g.getDescriptorCount(), g.getStages());
                }
                results.put(e.getKey(), info.build(device));
            }
        }
        return results;
    }

    protected void compileAsLibrary(Shader host) {
        if (host.version < version) {
            throw new IllegalStateException("Library uses a newer source version than its host.");
        }
        bindings.clear();
        logic = host.logic;
        gl = new BuiltIn();
        compileGlobals(host);
    }

    private void compileComputeWork(Method entry, ShaderStage stage) {
        Work work = entry.getAnnotation(Work.class);
        if (work != null) {
            if (stage != ShaderStage.Compute) {
                throw new IllegalStateException("@Work can only be attached to compute shaders.");
            }
            logic.addHeaderLine("layout(local_size_x=%d,local_size_y=%d,local_size_z=%d)in;",
                    work.x(), work.y(), work.z());
        } else if (stage == ShaderStage.Compute) {
            throw new IllegalStateException("Work group size not defined on compute shader.");
        }
    }

    private Method findEntryPoint(ShaderStage stage, String name) {
        Method[] methods = getClass().getDeclaredMethods();
        for (Method m : methods) {
            EntryPoint entry = m.getAnnotation(EntryPoint.class);
            if (entry != null && entry.value() == stage && name.equals(entry.name())) {
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

    @SuppressWarnings("unchecked")
    private void compileGlobals(Shader host) {
        try {
            for (Class clazz = getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (Uniform.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Global g = (Global)f.get(this);
                        host.availableGlobals.putIfAbsent(g.getLocation(), g);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
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

    public Global useGlobalByLocation(Location location) {
        Global<Var> g = availableGlobals.get(location);
        usedGlobals.computeIfAbsent(g.getLocation().getSet(), k -> new HashMap<>()).put(g.getLocation().getBinding(), g);
        return g;
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

    protected Bool Bool(boolean b) {
        return new Bool(b);
    }

    protected Bool True() {
        return new Bool(true);
    }

    protected Bool False() {
        return new Bool(false);
    }

    protected Int Int(int i) {
        return new Int(i);
    }

    protected UInt UInt(int i) {
        return new UInt(i);
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
        return new IfChain();
    }

    protected DirectiveIfChain IfDir(Bool condition, Runnable body) {
        logic.addDirective("#if %s", condition);
        body.run();
        return new DirectiveIfChain();
    }

    protected void IfDirEnd(Bool condition, Runnable body) {
        logic.addDirective("#if %s", condition);
        body.run();
        logic.addDirective("#endif");
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

    protected <T extends Var> void For(T iterator, Function<T, Bool> condition, Consumer<T> body) {
        logic.add("for(");
        iterator.declare();
        logic.add(condition.apply(iterator).toString());
        logic.add(";){");
        body.accept(iterator);
        logic.add("}");
    }

    protected <T extends Var> void For(T iterator, Bool condition, Consumer<T> update, Consumer<T> body) {
        logic.add("for(");
        iterator.declare();
        logic.add(condition.toString());
        logic.add(";){");
        body.accept(iterator);
        update.accept(iterator);
        logic.add("}");
    }

    protected <T extends Var> void For(T iterator, Bool condition, Consumer<T> body) {
        logic.add("for(");
        iterator.declare();
        logic.add(condition.toString());
        logic.add(";){");
        body.accept(iterator);
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

    protected <T extends Var> Switch<T> Switch(T value) {
        logic.add("switch(%s){", value);
        return new Switch<>();
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

    protected class DirectiveIfChain {

        private DirectiveIfChain() {}

        public DirectiveIfChain ElseIf(Bool condition, Runnable body) {
            logic.addDirective("#elseif %s", condition);
            body.run();
            return this;
        }

        public void ElseIfEnd(Bool condition, Runnable body) {
            logic.addDirective("#elseif %s", condition);
            body.run();
            logic.addDirective("#endif");
        }

        public void Else(Runnable body) {
            logic.addDirective("#else");
            body.run();
            logic.addDirective("#endif");
        }

        public void end() {
            logic.addDirective("#endif");
        }

    }

    protected class Switch <T extends Var> {

        private Switch() {}

        public Switch<T> Case(T value, Runnable body) {
            logic.add("case %s:{", value);
            body.run();
            logic.add("}break;");
            return this;
        }

        public void CaseEnd(T value, Runnable body) {
            logic.add("case %s:{", value);
            body.run();
            logic.add("}break;}");
        }

        public void Default(Runnable body) {
            logic.add("default:{");
            body.run();
            logic.add("}break;}");
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

    /**
     * GLSL's built-in math functions (angle/trigonometry, exponential, and common), exposed
     * as plain expression builders rather than through {@link #call}: these already exist in
     * every shader stage, so wrapping each one in a generated helper function - as the
     * removed {@code SMath} class used to - only adds an indirect call the driver has to
     * inline back out, and risks name clashes with the real built-in of the same name.
     */
    protected static class GlslMath {

        private GlslMath() {}

        public Int min(Int a, Int b) {
            return new Int(String.format("min(%s,%s)", a, b));
        }

        public Int max(Int a, Int b) {
            return new Int(String.format("max(%s,%s)", a, b));
        }

        public UInt min(UInt a, UInt b) {
            return new UInt(String.format("min(%s,%s)", a, b));
        }

        public UInt max(UInt a, UInt b) {
            return new UInt(String.format("max(%s,%s)", a, b));
        }

        public Flt min(Flt a, Flt b) {
            return new Flt(String.format("min(%s,%s)", a, b));
        }

        public Flt max(Flt a, Flt b) {
            return new Flt(String.format("max(%s,%s)", a, b));
        }

        public Int abs(Int a) {
            return new Int(String.format("abs(%s)", a));
        }

        public Int sign(Int a) {
            return new Int(String.format("sign(%s)", a));
        }

        public Int clamp(Int a, Int min, Int max) {
            return new Int(String.format("clamp(%s,%s,%s)", a, min, max));
        }

        public UInt clamp(UInt a, UInt min, UInt max) {
            return new UInt(String.format("clamp(%s,%s,%s)", a, min, max));
        }

        /*------------------------*\
        | ANGLE & TRIGONOMETRY     |
        \*------------------------*/

        public Flt radians(Flt degrees) {
            return new Flt(String.format("radians(%s)", degrees));
        }

        public Flt degrees(Flt radians) {
            return new Flt(String.format("degrees(%s)", radians));
        }

        public Flt sin(Flt a) {
            return new Flt(String.format("sin(%s)", a));
        }

        public Flt cos(Flt a) {
            return new Flt(String.format("cos(%s)", a));
        }

        public Flt tan(Flt a) {
            return new Flt(String.format("tan(%s)", a));
        }

        public Flt asin(Flt a) {
            return new Flt(String.format("asin(%s)", a));
        }

        public Flt acos(Flt a) {
            return new Flt(String.format("acos(%s)", a));
        }

        public Flt atan(Flt yOverX) {
            return new Flt(String.format("atan(%s)", yOverX));
        }

        public Flt atan(Flt y, Flt x) {
            return new Flt(String.format("atan(%s,%s)", y, x));
        }

        public Flt sinh(Flt a) {
            return new Flt(String.format("sinh(%s)", a));
        }

        public Flt cosh(Flt a) {
            return new Flt(String.format("cosh(%s)", a));
        }

        public Flt tanh(Flt a) {
            return new Flt(String.format("tanh(%s)", a));
        }

        public Flt asinh(Flt a) {
            return new Flt(String.format("asinh(%s)", a));
        }

        public Flt acosh(Flt a) {
            return new Flt(String.format("acosh(%s)", a));
        }

        public Flt atanh(Flt a) {
            return new Flt(String.format("atanh(%s)", a));
        }

        /*------------------------*\
        | EXPONENTIAL              |
        \*------------------------*/

        public Flt pow(Flt a, Flt exponent) {
            return new Flt(String.format("pow(%s,%s)", a, exponent));
        }

        public Flt exp(Flt a) {
            return new Flt(String.format("exp(%s)", a));
        }

        public Flt log(Flt a) {
            return new Flt(String.format("log(%s)", a));
        }

        public Flt exp2(Flt a) {
            return new Flt(String.format("exp2(%s)", a));
        }

        public Flt log2(Flt a) {
            return new Flt(String.format("log2(%s)", a));
        }

        public Flt sqrt(Flt a) {
            return new Flt(String.format("sqrt(%s)", a));
        }

        public Flt inversesqrt(Flt a) {
            return new Flt(String.format("inversesqrt(%s)", a));
        }

        /*------------------------*\
        | COMMON                   |
        \*------------------------*/

        public Flt abs(Flt a) {
            return new Flt(String.format("abs(%s)", a));
        }

        public Flt sign(Flt a) {
            return new Flt(String.format("sign(%s)", a));
        }

        public Flt floor(Flt a) {
            return new Flt(String.format("floor(%s)", a));
        }

        public Flt ceil(Flt a) {
            return new Flt(String.format("ceil(%s)", a));
        }

        public Flt trunc(Flt a) {
            return new Flt(String.format("trunc(%s)", a));
        }

        public Flt round(Flt a) {
            return new Flt(String.format("round(%s)", a));
        }

        public Flt roundEven(Flt a) {
            return new Flt(String.format("roundEven(%s)", a));
        }

        public Flt fract(Flt a) {
            return new Flt(String.format("fract(%s)", a));
        }

        public Flt mod(Flt a, Flt b) {
            return new Flt(String.format("mod(%s,%s)", a, b));
        }

        public Flt clamp(Flt a, Flt min, Flt max) {
            return new Flt(String.format("clamp(%s,%s,%s)", a, min, max));
        }

        public Flt mix(Flt a, Flt b, Flt t) {
            return new Flt(String.format("mix(%s,%s,%s)", a, b, t));
        }

        public Flt step(Flt edge, Flt a) {
            return new Flt(String.format("step(%s,%s)", edge, a));
        }

        public Flt smoothstep(Flt edge0, Flt edge1, Flt a) {
            return new Flt(String.format("smoothstep(%s,%s,%s)", edge0, edge1, a));
        }

        public Bool isnan(Flt a) {
            return new Bool(String.format("isnan(%s)", a));
        }

        public Bool isinf(Flt a) {
            return new Bool(String.format("isinf(%s)", a));
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

}
