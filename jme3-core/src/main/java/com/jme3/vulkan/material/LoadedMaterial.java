package com.jme3.vulkan.material;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.material.uniforms.BufferUniform;
import com.jme3.vulkan.material.uniforms.TextureUniform;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.pipelines.GraphicsPipeline;
import com.jme3.vulkan.util.ReflectionArgs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LoadedMaterial extends NewMaterial {

    private static final Map<String, Function<ReflectionArgs, Object>> natives = new HashMap<>();

    public LoadedMaterial(DescriptorPool pool, String fileName) {
        super(pool);
        ObjectMapper mapper = YAMLMapper.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build();
        InputStream stream = null;
        Map<Integer, List<Uniform<?>>> sets = new HashMap<>();
        try {
            JsonNode root = mapper.readTree(stream);
            JsonNode imports = root.get("imports");
            JsonNode parameters = root.get("parameters");
            parameters.fields();
            for (Iterator<Map.Entry<String, JsonNode>> it = parameters.fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> param = it.next();
                int set = Objects.requireNonNull(param.getValue().get("set"),
                        "Set index not defined in \"" + param.getKey() + "\"").asInt();
                sets.computeIfAbsent(set, n -> new ArrayList<>()).add(
                        instantiate(param.getKey(), param.getValue(), imports));
            }
            // add uniforms to sets
            for (Map.Entry<Integer, List<Uniform<?>>> set : sets.entrySet()) {
                addSet(set.getKey(), set.getValue().toArray(new Uniform[0]));
            }
            // load pipelines
            JsonNode pipelines = root.get("pipelines");
            for (Iterator<Map.Entry<String, JsonNode>> it = pipelines.fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> pipeline = it.next();
                Object state = instantiate(pipeline.getKey(), pipeline.getValue(), imports);
            }
        } catch (IOException
                 | ClassNotFoundException
                 | NoSuchMethodException
                 | InvocationTargetException
                 | InstantiationException
                 | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
