package com.jme3.shaderc;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
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

    private static final Logger LOG = Logger.getLogger(ShadercLoader.class.getName());
    private static final Compiler compiler = new Compiler();

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
        synchronized (compiler) { try (MemoryStack stack = MemoryStack.stackPush()) {
            long preprocessed = Shaderc.shaderc_compile_into_preprocessed_text(compiler.getNativeObject(), code,
                    type.getShaderc(), name, entry, NULL);
            ByteBuffer bytecode = Objects.requireNonNull(Shaderc.shaderc_result_get_bytes(preprocessed));
            long compiled = Shaderc.shaderc_compile_into_spv(compiler.getNativeObject(), bytecode,
                    type.getShaderc(), stack.UTF8(name), stack.UTF8(entry), NULL);
            if (Shaderc.shaderc_result_get_compilation_status(compiled) != Shaderc.shaderc_compilation_status_success) {
                LOG.log(Level.SEVERE, "Bad compile of\n{0}", ShaderDebug.formatShaderSource(code));
                throw new RuntimeException("Failed to compile " + name + ":\n"
                        + Shaderc.shaderc_result_get_error_message(compiled));
            }
            long warnings = Shaderc.shaderc_result_get_num_warnings(compiled);
            if (warnings > 0) {
                LOG.warning("Compiled with " + warnings + " warning" + (warnings == 1 ? "" : "s") + ": " + name);
            } else {
                LOG.fine("Compiled with no warnings: " + name);
            }
            return Shaderc.shaderc_result_get_bytes(compiled);
        }}
    }

    public static AssetKey<ByteBuffer> key(String name, ShaderType type) {
        return new Key(name, type, "main");
    }

    public static AssetKey<ByteBuffer> key(String name, ShaderType type, String entry) {
        return new Key(name, type, entry);
    }

    private static class Compiler implements Native<Long> {

        private final NativeReference ref;
        private long id;

        private Compiler() {
            id = Shaderc.shaderc_compiler_initialize();
            if (id == NULL) {
                throw new NullPointerException("Unable to initialize Shaderc compiler.");
            }
            ref = Native.get().register(this);
        }

        @Override
        public Long getNativeObject() {
            return id;
        }

        @Override
        public Runnable createNativeDestroyer() {
            return () -> Shaderc.shaderc_compiler_release(id);
        }

        @Override
        public void prematureNativeDestruction() {
            id = NULL;
        }

        @Override
        public NativeReference getNativeReference() {
            return ref;
        }

    }

    public static class Key extends AssetKey<ByteBuffer> {

        private final ShaderType shaderType;
        private final String entryPoint;

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

    }

}
