package com.jme3.vulkan.shadergen;

import com.jme3.backend.GraphicsAPI;
import com.jme3.vulkan.shadergen.vars.StructVar;
import com.jme3.vulkan.shadergen.vars.Var;

import java.util.*;

public class ShaderLogic {

    private static final Map<Thread, ShaderLogic> logic = new HashMap<>();

    public static ShaderLogic init(GraphicsAPI target, boolean debug) {
        ShaderLogic l = new ShaderLogic(target, debug);
        logic.put(Thread.currentThread(), l);
        return l;
    }

    public static ShaderLogic get() {
        return logic.get(Thread.currentThread());
    }

    private final Deque<Block> activeBlocks = new ArrayDeque<>();
    private final Map<Object, Block> blocks = new HashMap<>();
    private final Map<Integer, Var> inputVaryings = new HashMap<>();
    private final Map<Integer, Var> outputVaryings = new HashMap<>();
    private final Map<Object, Global> possibleGlobals = new HashMap<>();
    private final Map<Object, Global> usedGlobals = new HashMap<>();
    private final Header header = new Header();
    private final GraphicsAPI target;
    private final boolean debug;
    private int nextVarId = 0;
    private int nextUniformId = 0;
    private int nextStorageId = 0;

    private ShaderLogic(GraphicsAPI target, boolean debug) {
        this.target = target;
        this.debug = debug;
    }

    public String compileBlocks() {
        StringBuilder glsl = new StringBuilder();
        glsl.append(header).append('\n');
        blocks.values().stream().sorted().forEach(b -> {
            glsl.append(b.code).append('\n');
        });
        return glsl.toString();
    }

    public String generateUniqueName() {
        return "_v" + nextVarId++;
    }

    public boolean registerPossibleGlobal(Global global) {
        return possibleGlobals.putIfAbsent(global.getKey(), global) == null;
    }

    public Global getPossibleGlobal(Object key) {
        return possibleGlobals.get(key);
    }

    public void registerUsedGlobal(Object key, Global global) {
        if (usedGlobals.putIfAbsent(key, global) != null) {
            throw new IllegalStateException("Globals collision on " + key);
        }
    }

    public boolean startBlock(Object signiture) {
        if (!activeBlocks.isEmpty()) {
            activeBlocks.peek().addReferenceTo(signiture);
        }
        if (blocks.containsKey(signiture)) {
            return false;
        }
        Block b = new Block(signiture);
        activeBlocks.push(b);
        blocks.put(b.signiture, b);
        return true;
    }

    public void endBlock() {
        activeBlocks.pop();
    }

    public void add(String code) {
        if (!activeBlocks.isEmpty()) {
            activeBlocks.peek().add(code);
        }
    }

    public void add(String code, Object... args) {
        if (!activeBlocks.isEmpty()) {
            activeBlocks.peek().add(String.format(code, args));
        }
    }

    public void addHeaderLine(String code) {
        header.add(code);
    }

    public void addHeaderLine(String code, Object... args) {
        header.add(String.format(code, args));
    }

    public void addDirective(String code) {
        if (!activeBlocks.isEmpty()) {
            activeBlocks.peek().addDirective(code);
        }
    }

    public void addDirective(String code, Object... args) {
        if (!activeBlocks.isEmpty()) {
            activeBlocks.peek().addDirective(String.format(code, args));
        }
    }

    public void addInputVarying(int location, Var varying) {
        if (location < 0) {
            throw new IllegalArgumentException("Varying location must be non-negative.");
        }
        if (inputVaryings.putIfAbsent(location, varying) != null) {
            throw new IllegalArgumentException("Only one input varying can occupy a location.");
        }
    }

    public void addOutputVarying(int location, Var varying) {
        if (location < 0) {
            throw new IllegalArgumentException("Varying location must be non-negative.");
        }
        if (outputVaryings.putIfAbsent(location, varying) != null) {
            throw new IllegalArgumentException("Only one output varying can occupy a location.");
        }
    }

    public Binding addUniformDeclaration(Binding binding, StructVar struct, String refName) {
        if (target == GraphicsAPI.Vulkan) {
            add("layout(%s,set=%d,binding=%d) uniform %s{", struct.getLayout().getIdentifier(),
                    binding.getSet(), binding.getBinding(), generateUniqueName());
        } else {
            binding = new Binding(nextUniformId++);
            add("layout(%s,binding=%d) uniform %s{", struct.getLayout().getIdentifier(),
                    binding.getBinding(), generateUniqueName());
        }
        for (StructVar.Member f : struct.getFields()) {
            add("%s %s;", f.getType(), f.getName());
        }
        add("}%s;", refName);
        return binding;
    }

    public Binding addStorageDeclaration(Binding binding, StructVar struct, String refName) {
        if (target == GraphicsAPI.Vulkan) {
            add("layout(%s,set=%d,binding=%d) buffer %s{", struct.getLayout().getIdentifier(),
                    binding.getSet(), binding.getBinding(), generateUniqueName());
        } else {
            binding = new Binding(nextStorageId++);
            add("layout(%s,binding=%d) buffer %s{", struct.getLayout().getIdentifier(),
                    binding.getBinding(), generateUniqueName());
        }
        for (StructVar.Member f : struct.getFields()) {
            add("%s %s;", f.getType(), f.getName());
        }
        add("}%s;", refName);
        return binding;
    }

    public Optional<Binding> addPushConstantsDeclaration(StructVar struct, String refName) {
        Optional<Binding> key;
        if (target == GraphicsAPI.Vulkan) {
            key = Optional.empty();
            add("layout(push_constant,%s) uniform %s{", struct.getLayout().getIdentifier(), generateUniqueName());
        } else {
            key = Optional.of(new Binding(nextUniformId++));
            add("layout(%s,binding=%d) uniform %s{", struct.getLayout().getIdentifier(), key.get().getBinding(), generateUniqueName());
        }
        for (StructVar.Member f : struct.getFields()) {
            add("%s %s;", f.getType(), f.getName());
        }
        add("}%s;", refName);
        return key;
    }

    public GraphicsAPI getTarget() {
        return target;
    }

    public boolean isDebug() {
        return debug;
    }

    private class Block implements Comparable<Block> {

        private final Object signiture;
        private final StringBuilder code = new StringBuilder();
        private final Set<Object> references = new HashSet<>();
        private int indent = 0;

        public Block(Object signiture) {
            this.signiture = signiture;
            if (debug) {
                // shaderc supports filenames directly in the line directive
                code.append("\n#line 1 \"").append(signiture).append("\"\n");
            }
        }

        public void add(String s) {
            if (debug) {
                StringBuilder newline = new StringBuilder("\n");
                for (int i = 0; i < indent; i++) {
                    newline.append('\t');
                }
                if (s.endsWith("{")) {
                    indent++;
                } else if (s.endsWith("}")) {
                    indent--;
                }
                s = s.replace(";", ";" + newline);
                s = s.replace("{", "{" + newline);
                s = s.replace("}", "}" + newline);
            }
            code.append(s);
        }

        public void addDirective(String s) {
            code.append('\n').append(s).append('\n');
        }

        public void addReferenceTo(Object signiture) {
            references.add(signiture);
        }

        @Override
        public int compareTo(Block o) {
            if (references.contains(o.signiture)) return 1;
            else if (o.references.contains(signiture)) return -1;
            else return 0;
        }

    }

    private class Header extends Block {

        public Header() {
            super(Header.class);
        }

        @Override
        public void add(String s) {
            super.add(s);
            super.add("\n");
        }

        @SuppressWarnings("ComparatorMethodParameterNotUsed")
        @Override
        public int compareTo(Block o) {
            return -1;
        }
    }

}
