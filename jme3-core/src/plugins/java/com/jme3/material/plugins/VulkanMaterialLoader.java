package com.jme3.material.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.material.NewMaterial;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.material.VulkanTechnique;
import com.jme3.vulkan.material.uniforms.BufferUniform;
import com.jme3.vulkan.material.uniforms.TextureUniform;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.material.uniforms.VulkanUniform;
import com.jme3.vulkan.pipeline.PipelineLayout;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;

public class VulkanMaterialLoader implements AssetLoader {

    private static final Map<String, Function<JsonNode, Uniform<?>>> uniformLoaders = new HashMap<>();
    private static final Map<String, ShaderStage> stages = new HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        addUniformLoader(n -> new TextureUniform(VulkanImage.Layout.valueOf(n.get("layout").asText())),
                "texture", "texture1d", "texture2d", "texture3d", "texturecubemap", "texturearray");
        addUniformLoader(n -> new BufferUniform(), "uniformbuffer");
        addShaderStage(ShaderStage.Vertex, "vertex", "vert");
        addShaderStage(ShaderStage.Fragment, "fragment", "frag");
    }

    public static void addUniformLoader(Function<JsonNode, Uniform<?>> loader, String... names) {
        for (String n : names) {
            uniformLoaders.put(n.toLowerCase(), loader);
        }
    }

    public static void addShaderStage(ShaderStage stage, String... names) {
        for (String n : names) {
            stages.put(n.toLowerCase(), stage);
        }
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        VulkanMaterial material = new NewMaterial();
        try (InputStream stream = assetInfo.openStream()) {
            JsonNode root = mapper.readTree(stream);
            for (JsonNode param : root.get("parameters")) {
                String name = param.get("name").asText();
                String type = param.get("type").asText();
                material.setUniform(name, Objects.requireNonNull(uniformLoaders.get(type),
                        "Unrecognized parameter type: " + type).apply(param));
            }
            for (JsonNode technique : root.get("techniques")) {
                VulkanTechnique tech = new VulkanTechnique();
                for (JsonNode member : technique.get("layout")) {
                    TechniqueMember m = new TechniqueMember(member);
                    tech.addBinding(m.set, m.name, m.createBinding(Objects.requireNonNull(material.getUniform(m.name),
                            "Technique layout member references uniform \"" + m.name + "\" which does not exist.")));
                }
                String name = technique.get("name").asText();
                for (JsonNode shader : Objects.requireNonNull(technique.get("shaders"), "Technique \"" + name + "\" has no shader entries.")) {
                    if (shader.has("shader")) {
                        String[] params = shader.get("shader").asText().split(":");
                        if (params.length < 2) {
                            throw new IOException("Technique shader string must contain at least 2 parameters.");
                        }
                        tech.setShaderSource(Objects.requireNonNull(stages.get(params[0]),
                                "Unrecognized shader stage: " + params[0]), params[1]);
                    } else {
                        String stage = shader.get("stage").asText();
                        tech.setShaderSource(Objects.requireNonNull(stages.get(stage),
                                "Unrecognized shader stage: " + stage), shader.get("file").asText());
                    }
                }
                if (technique.has("renderState")) {
                    tech.getRenderState().applyJson(technique.get("renderState"));
                }
            }
        }
        return material;
    }

    private static class TechniqueMember {

        private String name, type;
        private int set = -1;
        private int binding = -1;
        private Flag<ShaderStage> stages;

        public TechniqueMember(JsonNode member) throws IOException {
            if (member.has("layout")) {
                String[] params = member.get("layout").asText().split(":");
                if (params.length < 3) {
                    throw new IOException("Technique member layout string must contain at least 3 parameters.");
                }
                type = params[0].toLowerCase();
                name = params[1];
                String[] setBinding = params[2].split("\\.");
                set = Integer.parseInt(setBinding[0]);
                binding = Integer.parseInt(setBinding[1]);
            }
            if (member.has("type")) {
                type = member.get("type").asText().toLowerCase();
            }
            if (member.has("name")) {
                name = member.get("name").asText();
            }
            if (member.has("set")) {
                set = member.get("set").asInt();
            }
            if (member.has("binding")) {
                binding = member.get("binding").asInt();
            }
            Objects.requireNonNull(name, "Technique member name not specified.");
            Objects.requireNonNull(type, "Technique member \"" + name + "\": type not specified.");
            if (set < 0) throw new IOException("Technique member \"" + name + "\": set not specified.");
            if (binding < 0) throw new IOException("Technique member \"" + name + "\": binding not specified.");
            if (member.has("stages")) {
                if (member.isArray()) {
                    stages = Flag.empty();
                    for (JsonNode s : member.get("stages")) {
                        stages = stages.add(Objects.requireNonNull(VulkanMaterialLoader.stages.get(
                                s.asText().toLowerCase()),
                                "Stage \"" + s.asText() + "\" is not recognized."));
                    }
                } else {
                    String stageName = member.get("stages").asText().toLowerCase();
                    stages = Objects.requireNonNull(VulkanMaterialLoader.stages.get(stageName),
                            "Stage \"" + stageName + "\" is not recognized.");
                }
            } else {
                stages = ShaderStage.All;
            }
        }

        public SetLayoutBinding createBinding(VulkanUniform uniform) {
            return uniform.createBinding(
                    Objects.requireNonNull(Descriptor.valueOf(type), "Type \"" + type + "\" is not recognized."),
                    binding, stages);
        }

    }

}
