package com.jme3.vulkan.shaderc;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.util.IntEnum;
import jme3tools.shader.ShaderDebug;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.lwjgl.system.MemoryUtil.NULL;

public class ShadercLoader implements AssetLoader {

    public enum Optimization implements IntEnum<Optimization> {

        None(Shaderc.shaderc_optimization_level_zero),
        Size(Shaderc.shaderc_optimization_level_size),
        Performance(Shaderc.shaderc_optimization_level_performance);

        private final int shadercEnum;

        Optimization(int shadercEnum) {
            this.shadercEnum = shadercEnum;
        }

        @Override
        public int getEnum() {
            return shadercEnum;
        }

    }

    private static final Logger LOG = Logger.getLogger(ShadercLoader.class.getName());
    private static final Compiler compiler = new Compiler();
    private static final Map<Key, ShaderModule> shaders = new HashMap<>();

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
            return compile(key.getName(), code.toString(), key.getShaderType(), key.getEntryPoint());
        }
    }

    public static ByteBuffer compile(String name, String code, ShaderType type, String entry) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long preprocessed = Shaderc.shaderc_compile_into_preprocessed_text(compiler.getNativeObject(), code,
                    type.getShaderc(), name, entry, NULL);
            handleCompileCodes(preprocessed, name, code);
            ByteBuffer bytecode = Objects.requireNonNull(Shaderc.shaderc_result_get_bytes(preprocessed));
            long options = Shaderc.shaderc_compile_options_initialize();
            //Shaderc.shaderc_compile_options_add_macro_definition(options, "MACRO_NAME", "156");
            Shaderc.shaderc_compile_options_set_optimization_level(options, Shaderc.shaderc_optimization_level_performance);
            long compiled = Shaderc.shaderc_compile_into_spv(compiler.getNativeObject(), bytecode,
                    type.getShaderc(), stack.UTF8(name), stack.UTF8(entry), options);
            Shaderc.shaderc_compile_options_release(options);
            handleCompileCodes(compiled, name, code);
            return Shaderc.shaderc_result_get_bytes(compiled);
        }
    }

    private static void handleCompileCodes(long result, String name, String code) {
        int status = Shaderc.shaderc_result_get_compilation_status(result);
        if (status != Shaderc.shaderc_compilation_status_success) {
            LOG.log(Level.SEVERE, "Bad compile of\n{0}", ShaderDebug.formatShaderSource(code));
            throw new RuntimeException("Failed to compile " + name + ":\n"
                    + Shaderc.shaderc_result_get_error_message(result));
        }
        long warnings = Shaderc.shaderc_result_get_num_warnings(result);
        if (warnings > 0) {
            LOG.warning("Compiled with " + warnings + " warning" + (warnings == 1 ? "" : "s") + ": " + name);
        } else {
            LOG.fine("Compiled with no warnings: " + name);
        }
    }

    public static AssetKey<ByteBuffer> key(String name, ShaderType type) {
        return new Key(name, type, "main");
    }

    public static AssetKey<ByteBuffer> key(String name, ShaderType type, String entry) {
        return new Key(name, type, entry);
    }

    private static class Compiler extends AbstractNative<Long> {

        private Compiler() {
            object = Shaderc.shaderc_compiler_initialize();
            if (object == NULL) {
                throw new NullPointerException("Unable to initialize Shaderc compiler.");
            }
            ref = Native.get().register(this);
        }

        @Override
        public Runnable createNativeDestroyer() {
            return () -> Shaderc.shaderc_compiler_release(object);
        }

    }

    public static class Key extends AssetKey<ByteBuffer> {

        private final ShaderType shaderType;
        private final String entryPoint;
        private IntEnum<Optimization> optimization = Optimization.None;

        public Key(String name, ShaderType shaderType, String entryPoint) {
            super(name);
            this.shaderType = shaderType;
            this.entryPoint = entryPoint;
        }

        public ShaderType getShaderType() {
            return shaderType;
        }

        public String getEntryPoint() {
            return entryPoint;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Key key = (Key) o;
            return shaderType == key.shaderType && Objects.equals(entryPoint, key.entryPoint) && Objects.equals(optimization, key.optimization);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), shaderType, entryPoint, optimization);
        }

    }

}
