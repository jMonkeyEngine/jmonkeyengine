package com.jme3.material.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.material.NewMaterialDef;
import com.jme3.vulkan.material.technique.VulkanTechnique;
import com.jme3.vulkan.material.uniforms.BufferUniform;
import com.jme3.vulkan.material.uniforms.TextureUniform;
import com.jme3.vulkan.material.uniforms.VulkanUniform;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;

public class VulkanMaterialLoader implements AssetLoader {

    private static final Map<String, Function<JsonNode, VulkanUniform<?>>> uniformLoaders = new HashMap<>();
    private static final Map<String, ShaderStage> stages = new HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        addUniformLoader(TextureUniform::new,
                "texture",
                "texture1d",
                "texture2d",
                "texture3d",
                "texturecubemap",
                "texturearray");
        addUniformLoader(json -> new BufferUniform<>(json, Descriptor.UniformBuffer), "uniformbuffer", "uniform");
        addUniformLoader(json -> new BufferUniform<>(json, Descriptor.StorageBuffer), "storagebuffer", "storage");
        addShaderStage(ShaderStage.Vertex, "vertex", "vert");
        addShaderStage(ShaderStage.Fragment, "fragment", "frag");
    }

    public static void addUniformLoader(Function<JsonNode, VulkanUniform<?>> loader, String... names) {
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
        NewMaterialDef matdef = new NewMaterialDef();
        try (InputStream stream = assetInfo.openStream()) {
            JsonNode root = mapper.readTree(stream);
            Map<String, VulkanUniform<?>> uniforms = new HashMap<>();
            for (JsonNode param : root.get("parameters")) {
                String name = param.get("name").asText();
                String type = param.get("type").asText();
                VulkanUniform<?> u = Objects.requireNonNull(uniformLoaders.get(type),
                        "Unrecognized parameter type: " + type).apply(param);
                matdef.setUniform(name, u);
                uniforms.put(name, u);
            }
            for (JsonNode technique : root.get("techniques")) {
                VulkanTechnique tech = new VulkanTechnique();
                if (technique.has("bindings")) for (JsonNode member : technique.get("bindings")) {
                    TechniqueMember m = new TechniqueMember(member);
                    if (m.set >= 0 && m.binding >= 0) {
                        tech.setBinding(m.set, m.name, m.createBinding(Objects.requireNonNull(uniforms.get(m.name),
                                "Technique bindings member references uniform \"" + m.name + "\" which does not exist.")));
                    }
                    if (m.define != null) {
                        tech.linkDefine(m.define, m.name, m.scope);
                    }
                }
                String name = technique.get("name").asText();
                for (Iterator<Map.Entry<String, JsonNode>> it = technique.get("shaders").fields(); it.hasNext();) {
                    Map.Entry<String, JsonNode> shader = it.next();
                    if (shader.getValue().isTextual()) {
                        tech.setShaderSource(ShaderStage.valueOf(shader.getKey()), shader.getValue().asText());
                    } else {

                    }
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
                    tech.getRenderState().readJson(technique.get("renderState"));
                }
                if (technique.has("attributes")) {
                    for (Map.Entry<String, JsonNode> a : technique.get("attributes").properties()) {
                        tech.setAttributeLocation(a.getKey(), a.getValue().asInt());
                    }
                }
            }
        }
        return matdef;
    }

    private static class TechniqueMember {

        private String name, define;
        private Flag<ShaderStage> scope;
        private int set = -1;
        private int binding = -1;

        public TechniqueMember(JsonNode member) {
            if (member.has("name")) {
                name = member.get("name").asText();
            }
            if (member.has("location")) {
                JsonNode loc = member.get("location");
                set = loc.get(0).asInt();
                binding = loc.get(1).asInt();
            }
            if (member.has("set")) {
                set = member.get("set").asInt();
            }
            if (member.has("binding")) {
                binding = member.get("binding").asInt();
            }
            if (member.has("define")) {
                define = member.get("define").asText();
            }
            Objects.requireNonNull(name, "Technique member name not specified.");
            if (member.has("scope")) {
                scope = Flag.empty();
                for (JsonNode s : member.get("stages")) {
                    scope = scope.add(Objects.requireNonNull(VulkanMaterialLoader.stages.get(
                            s.asText().toLowerCase()),
                            "Stage \"" + s.asText() + "\" is not recognized."));
                }
            } else {
                scope = ShaderStage.All;
            }
        }

        public UniformBinding createBinding(VulkanUniform<?> u) {
            return u.createBinding(binding, scope);
        }

    }

}
