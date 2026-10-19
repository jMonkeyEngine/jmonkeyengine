package com.jme3.vulkan.shadergen.vars;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public interface Var {

    @SuppressWarnings("unchecked")
    static Var createVariable(Class type, String expression) {
        if (!Var.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Type must inherit from " + Var.class);
        }
        try {
            Constructor c = type.getDeclaredConstructor(String.class);
            c.setAccessible(true);
            return (Var)c.newInstance(expression);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static String getVariableType(Class type) {
        if (type == void.class) return "void";
        try {
            Field f = type.getDeclaredField("TYPE");
            if (!Modifier.isStatic(type.getModifiers())) {
                throw new IllegalStateException("\"TYPE\" field must be static in " + type);
            }
            return (String)f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    String getType();

    Var declare();

    Var declare(String variableName);

    Var create(String exp);

}
