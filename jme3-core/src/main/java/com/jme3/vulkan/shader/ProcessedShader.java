package com.jme3.vulkan.shader;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.shaderc.ShaderType;
import com.jme3.vulkan.shaderc.ShadercEnums;
import jme3tools.shader.ShaderDebug;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.lwjgl.system.MemoryUtil.NULL;

public class ProcessedShader {

    private static final Logger LOG = Logger.getLogger(ProcessedShader.class.getName());
    private static final Compiler shaderc = new Compiler();

    private final ByteBuffer code;

    public ProcessedShader(ShaderAsset shader, ShaderType type, String entryPoint) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long options = Shaderc.shaderc_compile_options_initialize();
            //Shaderc.shaderc_compile_options_add_macro_definition(options, "MACRO_NAME", "156");
            Shaderc.shaderc_compile_options_set_optimization_level(options, key.getOptimization().getEnum());
            long preprocessed = Shaderc.shaderc_compile_into_preprocessed_text(shaderc.getNativeObject(), shader.getCode(),
                    type.getEnum(ShadercEnums.instance), shader.getAssetName(), entryPoint, NULL);
            handleCompileCodes(preprocessed, key.getName(), code);
            ByteBuffer bytecode = Objects.requireNonNull(Shaderc.shaderc_result_get_bytes(preprocessed));
            long compiled = Shaderc.shaderc_compile_into_spv(shaderc.getNativeObject(), bytecode,
                    key.getType().getEnum(ShadercEnums.instance), stack.UTF8(key.getName()), stack.UTF8(key.getName()), options);
            Shaderc.shaderc_compile_options_release(options);
            handleCompileCodes(compiled, key.getName(), code);
            code = Shaderc.shaderc_result_get_bytes(compiled);
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
            LOG.warning("Compiled with " + warnings + " warning" + (warnings == 1 ? "" : "s") + ": " + name);
        } else {
            LOG.finer("Compiled with no warnings: " + name);
        }
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

    public static class Builder {

        private final ProcessedShader shader;

        public Builder() {

        }

    }

}
