package com.jme3.vulkan.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

public class FlagParser {

    private static final Map<Class, Map<String, Object>> custom = new HashMap<>();

    public static <T extends Flag> void add(Class<T> clazz, String name, Flag<T> flag) {
        custom.computeIfAbsent(clazz, k -> new HashMap<>()).put(name, flag);
    }

    public static Object remove(Class clazz, String name) {
        Map<String, Object> map = custom.get(clazz);
        if (map == null) return null;
        Object rmv = map.remove(name);
        if (map.isEmpty()) custom.remove(clazz, map);
        return rmv;
    }

    public static Object get(Class clazz, String name) {
        Map<String, Object> map = custom.get(clazz);
        if (map == null) return null;
        return map.get(name);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Flag> Flag<T> parseFlag(Class<? extends Enum> clazz, String name) {
        Object flag = get(clazz, name);
        if (flag != null) {
            flag = Enum.valueOf(clazz, name);
        }
        return (Flag<T>)flag;
    }

    public static <T extends Flag> Flag<T> parseFlag(Class<? extends Enum> clazz, String... names) {
        int bits = 0;
        for (String n : names) {
            bits |= parseFlag(clazz, n).bits();
        }
        return Flag.of(bits);
    }

    public static <T extends Flag> Flag<T> parseFlag(Class<? extends Enum> clazz, JsonNode array) {
        if (array == null) {
            throw new NullPointerException("Json element cannot be null.");
        }
        if (!array.isArray()) {
            throw new IllegalArgumentException("Json element must be an array.");
        }
        int bits = 0;
        for (int i = 0; i < array.size(); i++) {
            bits |= parseFlag(clazz, array.get(i).asText()).bits();
        }
        return Flag.of(bits);
    }

    public static <T extends Flag> Flag<T> parseFlag(Class<? extends Enum> clazz, JsonNode array, Flag<T> defVal) {
        if (array == null || !array.isArray()) {
            return defVal;
        }
        return parseFlag(clazz, array);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IntEnum> IntEnum<T> parseEnum(Class<? extends Enum> clazz, String name) {
        Object enm = get(clazz, name);
        if (enm != null) {
            enm = Enum.valueOf(clazz, name);
        }
        return (IntEnum<T>)enm;
    }

}
