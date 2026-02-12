package com.jme3.vulkan.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.asset.AssetManager;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Deprecated
public class ReflectionArgs {

    private static final Map<String, Function<ReflectionArgs, Object>> natives = new HashMap<>();

    private final AssetManager assetManager;
    private final BufferGenerator<?> generator;
    private final String name;
    private final String type;
    private final JsonNode properties;
    private final JsonNode imports;

    public ReflectionArgs(AssetManager assetManager, BufferGenerator<?> generator, JsonNode imports) {
        this(assetManager, generator, null, null, null, imports);
    }

    public ReflectionArgs(AssetManager assetManager, BufferGenerator<?> generator, String name, JsonNode properties, JsonNode imports) {
        this(assetManager, generator, name, null, properties, imports);
    }

    public ReflectionArgs(AssetManager assetManager, BufferGenerator<?> generator, String name, String type, JsonNode properties, JsonNode imports) {
        this.assetManager = assetManager;
        this.generator = generator;
        this.name = name;
        this.properties = properties;
        this.imports = imports;
        if (properties != null) {
            JsonNode typeNode = properties.get("type");
            this.type = typeNode != null ? typeNode.asText() : type;
        } else {
            this.type = null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T instantiate() {
        try {
            JsonNode im = imports.get(type);
            if (im != null) {
                Class<?> clazz = Class.forName(im.asText());
                return (T)clazz.getDeclaredConstructor(ReflectionArgs.class).newInstance(this);
            }
            Function<ReflectionArgs, Object> factory = natives.get(type);
            if (factory != null) {
                return (T)factory.apply(this);
            } else {
                Class<?> clazz = Class.forName(type);
                return (T)clazz.getDeclaredConstructor(ReflectionArgs.class).newInstance(this);
            }
        } catch (ClassNotFoundException
                 | InvocationTargetException
                 | InstantiationException
                 | IllegalAccessException
                 | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public ReflectionArgs create(String name, JsonNode properties) {
        return new ReflectionArgs(assetManager, generator, name, properties, imports);
    }

    public ReflectionArgs create(String name, String type, JsonNode properties) {
        return new ReflectionArgs(assetManager, generator, type, name, properties, imports);
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public BufferGenerator<?> getGenerator() {
        return generator;
    }

    public String getName() {
        return name;
    }

    public JsonNode getProperties() {
        return properties;
    }

    public JsonNode getImports() {
        return imports;
    }

}
