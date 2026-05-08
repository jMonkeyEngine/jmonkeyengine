package com.jme3.vulkan.shaderc;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.spvc.SpvcCompiler;
import com.jme3.vulkan.spvc.SpvcContext;
import jme3tools.shader.ShaderDebug;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.lwjgl.system.MemoryUtil.NULL;

public class ShadercLoader implements AssetLoader {

    public enum Optimization {

        None(Shaderc.shaderc_optimization_level_zero),
        Size(Shaderc.shaderc_optimization_level_size),
        Performance(Shaderc.shaderc_optimization_level_performance);

        private final int optimization;

        Optimization(int optimization) {
            this.optimization = optimization;
        }

        public int getEnum() {
            return optimization;
        }

    }

    private static final Logger LOG = Logger.getLogger(ShadercLoader.class.getName());
    private static final Compiler shaderc = new Compiler();
    private static final SpvcContext spvc = new SpvcContext();

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        if (!(assetInfo.getKey() instanceof Key)) {
            throw new IllegalArgumentException("Requires " + Key.class.getName());
        }
        Key key = (Key)assetInfo.getKey();
        try (InputStream in = assetInfo.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder code = new StringBuilder();
            for (String line; (line = reader.readLine()) != null; ) {
                code.append(line).append('\n');
            }
            return new SpvcCompiler(spvc, key.getBackend(), compile(key, code.toString()));
        }
    }

    public static ByteBuffer compile(Key key, String code) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long preprocessed = Shaderc.shaderc_compile_into_preprocessed_text(shaderc.getNativeObject(), code,
                    key.getType().getEnum(ShadercEnums.instance), key.getName(), key.getEntryPoint(), NULL);
            handleCompileCodes(preprocessed, key.getName(), code);
            ByteBuffer bytecode = Objects.requireNonNull(Shaderc.shaderc_result_get_bytes(preprocessed));
            long options = Shaderc.shaderc_compile_options_initialize();
            //Shaderc.shaderc_compile_options_add_macro_definition(options, "MACRO_NAME", "156");
            Shaderc.shaderc_compile_options_set_optimization_level(options, key.getOptimization().getEnum());
            long compiled = Shaderc.shaderc_compile_into_spv(shaderc.getNativeObject(), bytecode,
                    key.getType().getEnum(ShadercEnums.instance), stack.UTF8(key.getName()), stack.UTF8(key.getName()), options);
            Shaderc.shaderc_compile_options_release(options);
            handleCompileCodes(compiled, key.getName(), code);
            return Shaderc.shaderc_result_get_bytes(compiled);
        }
    }

    private static void handleCompileCodes(long result, String name, String code) {
        int status = Shaderc.shaderc_result_get_compilation_status(result);
        if (status != Shaderc.shaderc_compilation_status_success) {
            LOG.log(Level.SEVERE, "Bad compile of {0}:\n{1}", new Object[] {name, ShaderDebug.formatShaderSource(code)});
            throw new RuntimeException("Failed to compile " + name + ":\n" + Shaderc.shaderc_result_get_error_message(result));
        }
        long warnings = Shaderc.shaderc_result_get_num_warnings(result);
        if (warnings > 0) {
            LOG.log(Level.WARNING, "Compiled with {0} warning{1}: {2}", new Object[]{warnings, (warnings == 1 ? "" : "s"), name});
        } else {
            LOG.log(Level.FINER, "Compiled with no warnings: {0}", name);
        }
    }

    public static Key key(String name, ShaderType stage) {
        return new Key(name, stage, "main");
    }

    public static Key key(String name, ShaderType stage, String entry) {
        return new Key(name, stage, entry);
    }

    private static class Compiler extends AbstractNative<Long> {

        private Compiler() {
            object = Shaderc.shaderc_compiler_initialize();
            if (object == NULL) {
                throw new NullPointerException("Unable to initialize Shaderc compiler.");
            }
            ref = DisposableManager.reference(this);
        }

        @Override
        public Runnable createDestroyer() {
            return () -> Shaderc.shaderc_compiler_release(object);
        }

    }

    public static class Key extends AssetKey<SpvcCompiler> {

        private final ShaderType type;
        private final String entryPoint;
        private Optimization optimization = Optimization.Performance;
        private SpvcCompiler.Backend backend = SpvcCompiler.Backend.None;

        public Key(String name, ShaderType type, String entryPoint) {
            super(name);
            this.type = type;
            this.entryPoint = entryPoint;
        }

        public void setOptimization(Optimization optimization) {
            this.optimization = optimization;
        }

        public void setBackend(SpvcCompiler.Backend backend) {
            this.backend = backend;
        }

        public ShaderType getType() {
            return type;
        }

        public String getEntryPoint() {
            return entryPoint;
        }

        public Optimization getOptimization() {
            return optimization;
        }

        public SpvcCompiler.Backend getBackend() {
            return backend;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Key key = (Key) o;
            return type == key.type && Objects.equals(entryPoint, key.entryPoint) && Objects.equals(optimization, key.optimization);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), type, entryPoint, optimization);
        }

    }

}
