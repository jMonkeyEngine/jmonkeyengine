package com.jme3.vulkan.shadergen.vars;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

public interface Var {

    static Var createVariable(Class type, String expression) {
        if (!Var.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Type must inherit from " + Var.class);
        }
        Object var = newInstance(type, new Class[] {String.class}, expression);
        if (var == null) {
            throw new RuntimeException("Failed to reflectively create " + type);
        }
        return (Var)var;
    }

    static Var createElementVariable(Class type, AnnotatedElement element, String expression) {
        if (!Var.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Type must inherit from " + Var.class);
        }
        Object var = newInstance(type, new Class[] {AnnotatedElement.class, String.class}, element, expression);
        if (var == null) {
            var = newInstance(type, new Class[] {String.class}, expression);
        }
        if (var == null) {
            throw new RuntimeException("Failed to reflectively create " + type);
        }
        return (Var)var;
    }

    static <T extends Annotation> T requireAnnotation(AnnotatedElement p, Class<T> type) {
        T a = p.getAnnotation(type);
        if (a == null) {
            throw new IllegalStateException("Element \"" + p + "\" must be annotated with " + type.getSimpleName());
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    static Object newInstance(Class type, Class[] types, Object... args) {
        try {
            Constructor c = type.getDeclaredConstructor(types);
            c.setAccessible(true);
            return c.newInstance(args);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to reflectively create object.", e);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    static String getVariableType(Class type) {
        if (type == void.class) {
            return "void";
        }
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
