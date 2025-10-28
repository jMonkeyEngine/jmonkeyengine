package com.jme3.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.asset.AssetManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class SavableData {

    public static final String TYPE = "type";

    private static final Map<String, Function<Info, Object>> natives = new HashMap<>();

    private final AssetManager assetManager;
    private final JsonNode imports;

    public SavableData(AssetManager assetManager, JsonNode imports) {
        this.assetManager = assetManager;
        this.imports = imports;
    }

    public <T> T load(String name, JsonNode properties) {
        return load(name, null, properties);
    }

    @SuppressWarnings("unchecked")
    public <T> T load(String name, String defaultType, JsonNode properties) {
        String type = getType(name, defaultType, properties);
        Info i = new Info(name, properties);
        if (imports != null && imports.hasNonNull(type)) {
            return createFromClass(imports.get(type).asText(), i);
        }
        Function<SavableData.Info, Object> nativeFunc = natives.get(type);
        if (nativeFunc != null) {
            return (T)nativeFunc.apply(i);
        }
        return createFromClass(type, i);
    }

    @SuppressWarnings("unchecked")
    private <T> T createFromClass(String className, Info i) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> c = clazz.getDeclaredConstructor();
            Object obj = c.newInstance();
            if (obj instanceof JsonReadable) {
                ((JsonReadable)obj).read(i);
            }
            return (T)obj;
        } catch (ClassNotFoundException
                 | InvocationTargetException
                 | InstantiationException
                 | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Class \"" + className + "\" does not have a parameterless constructor.", e);
        }
    }

    private String getType(String name, String defaultType, JsonNode properties) {
        JsonNode typeNode = properties.get(TYPE);
        if (typeNode == null || !typeNode.isTextual()) {
            return Objects.requireNonNull(defaultType, "Type is not defined for \"" + name + "\".");
        }
        else return typeNode.asText();
    }

    public class Info {

        private final String name;
        private final JsonNode properties;

        private Info(String name, JsonNode properties) {
            this.name = name;
            this.properties = properties;
        }

        public <T> T load(String name, JsonNode properties) {
            return SavableData.this.load(name, properties);
        }

        public <T> T load(String name, String defaultType, JsonNode properties) {
            return SavableData.this.load(name, defaultType, properties);
        }

        public AssetManager getAssetManager() {
            return assetManager;
        }

        public String getName() {
            return name;
        }

        public JsonNode getProperties() {
            return properties;
        }

    }

}
