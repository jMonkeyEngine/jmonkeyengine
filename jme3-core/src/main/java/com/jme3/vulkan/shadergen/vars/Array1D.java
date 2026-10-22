package com.jme3.vulkan.shadergen.vars;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.vulkan.shadergen.ArraySpec;
import com.jme3.vulkan.shadergen.ShaderLogic;

import java.lang.reflect.AnnotatedElement;

public class Array1D <T extends Var> implements Var {

    public static Array1D<Int> ints(int... elements) {
        Array1D<Int> a = new Array1D<>(Int.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) expression.append(',');
            expression.append(elements[i]);
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<UInt> uints(int... elements) {
        Array1D<UInt> a = new Array1D<>(UInt.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) expression.append(',');
            expression.append(elements[i]);
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<Flt> floats(float... elements) {
        Array1D<Flt> a = new Array1D<>(Flt.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) expression.append(',');
            expression.append(elements[i]);
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<Vec2> vec2(Vector2f... elements) {
        Array1D<Vec2> a = new Array1D<>(Vec2.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) expression.append(',');
            expression.append("vec2(")
                    .append(elements[i].x).append(',')
                    .append(elements[i].y).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<Vec2> vec2(float... elements) {
        Array1D<Vec2> a = new Array1D<>(Vec2.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i += 2) {
            if (i > 0) expression.append(',');
            expression.append("vec2(")
                    .append(elements[i]).append(',')
                    .append(elements[i + 1]).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<IVec2> ivec2(int... elements) {
        Array1D<IVec2> a = new Array1D<>(IVec2.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i += 2) {
            if (i > 0) expression.append(',');
            expression.append("ivec2(")
                    .append(elements[i]).append(',')
                    .append(elements[i + 1]).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<UVec2> uvec2(int... elements) {
        Array1D<UVec2> a = new Array1D<>(UVec2.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i += 2) {
            if (i > 0) expression.append(',');
            expression.append("uvec2(")
                    .append(elements[i]).append(',')
                    .append(elements[i + 1]).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<Vec3> vec3(Vector3f... elements) {
        Array1D<Vec3> a = new Array1D<>(Vec3.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) expression.append(',');
            expression.append("vec3(")
                    .append(elements[i].x).append(',')
                    .append(elements[i].y).append(',')
                    .append(elements[i].z).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<Vec3> vec3(float... elements) {
        Array1D<Vec3> a = new Array1D<>(Vec3.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i += 3) {
            if (i > 0) expression.append(',');
            expression.append("vec3(")
                    .append(elements[i]).append(',')
                    .append(elements[i + 1]).append(',')
                    .append(elements[i + 2]).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<IVec3> ivec3(int... elements) {
        Array1D<IVec3> a = new Array1D<>(IVec3.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i += 3) {
            if (i > 0) expression.append(',');
            expression.append("ivec3(")
                    .append(elements[i]).append(',')
                    .append(elements[i + 1]).append(',')
                    .append(elements[i + 2]).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<UVec3> uvec3(int... elements) {
        Array1D<UVec3> a = new Array1D<>(UVec3.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i += 3) {
            if (i > 0) expression.append(',');
            expression.append("uvec3(")
                    .append(elements[i]).append(',')
                    .append(elements[i + 1]).append(',')
                    .append(elements[i + 2]).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<Vec4> vec4(Vector4f... elements) {
        Array1D<Vec4> a = new Array1D<>(Vec4.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) expression.append(',');
            expression.append("vec4(")
                    .append(elements[i].x).append(',')
                    .append(elements[i].y).append(',')
                    .append(elements[i].z).append(',')
                    .append(elements[i].w).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<Vec4> vec4(float... elements) {
        Array1D<Vec4> a = new Array1D<>(Vec4.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) expression.append(',');
            expression.append("vec4(")
                    .append(elements[i]).append(',')
                    .append(elements[i + 1]).append(',')
                    .append(elements[i + 2]).append(',')
                    .append(elements[i + 3]).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<IVec4> ivec4(int... elements) {
        Array1D<IVec4> a = new Array1D<>(IVec4.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) expression.append(',');
            expression.append("ivec4(")
                    .append(elements[i]).append(',')
                    .append(elements[i + 1]).append(',')
                    .append(elements[i + 2]).append(',')
                    .append(elements[i + 3]).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    public static Array1D<UVec4> uvec4(int... elements) {
        Array1D<UVec4> a = new Array1D<>(UVec4.class, elements.length);
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) expression.append(',');
            expression.append("uvec4(")
                    .append(elements[i]).append(',')
                    .append(elements[i + 1]).append(',')
                    .append(elements[i + 2]).append(',')
                    .append(elements[i + 3]).append(')');
        }
        a.exp = expression.append('}').toString();
        return a;
    }

    private final ShaderLogic logic = ShaderLogic.get();
    private final Class<T> component;
    private final String componentType;
    private final int length;
    private String exp;

    @SuppressWarnings("unchecked")
    public Array1D(Var... elements) {
        component = (Class<T>)elements[0].getClass();
        componentType = Var.getVariableType(component);
        length = elements.length;
        StringBuilder expression = new StringBuilder("{");
        for (int i = 0; i < elements.length; i++) {
            if (elements[i].getClass() != component) {
                throw new IllegalArgumentException("Each array element must have the same type.");
            }
            if (i > 0) {
                expression.append(',');
            }
            expression.append(elements[i]);
        }
        exp = expression.append('}').toString();
    }

    public Array1D(Class<T> component, int length) {
        this.exp = "";
        this.component = component;
        this.componentType = Var.getVariableType(component);
        this.length = length;
    }

    public Array1D(String exp, Class<T> component, int length) {
        this.exp = exp;
        this.component = component;
        this.componentType = Var.getVariableType(component);
        this.length = length;
    }

    @SuppressWarnings("unchecked")
    public Array1D(AnnotatedElement el, String exp) {
        this.exp = exp;
        ArraySpec args = Var.requireAnnotation(el, ArraySpec.class);
        component = (Class<T>)args.type();
        componentType = Var.getVariableType(component);
        length = args.sizes()[0];
    }

    @Override
    public String getType() {
        return componentType + "[]";
    }

    @Override
    public Array1D<T> declare() {
        return declare(logic.generateUniqueName());
    }

    @Override
    public Array1D<T> declare(String variableName) {
        String exp = this.exp;
        if (exp.isEmpty()) {
            logic.add("%s %s[%d];", componentType, this.exp = variableName, length);
        } else {
            logic.add("%s %s[%d]=%s;", componentType, this.exp = variableName, length, exp);
        }
        return null;
    }

    @Override
    public Array1D<T> create(String exp) {
        return new Array1D<>(exp, component, length);
    }

    @SuppressWarnings("unchecked")
    public T index(int i) {
        if (length >= 0 && i >= length) {
            throw new IndexOutOfBoundsException("Index " + i + " out of bounds for length " + length);
        }
        return (T)Var.createVariable(component, String.format("%s[%d]", exp, i));
    }

    @SuppressWarnings("unchecked")
    public T index(Int i) {
        return (T)Var.createVariable(component, String.format("%s[%s]", exp, i));
    }

    @SuppressWarnings("unchecked")
    public T index(UInt i) {
        return (T)Var.createVariable(component, String.format("%s[%s]", exp, i));
    }

    public Int length() {
        return new Int(String.format("%s.length()", exp));
    }

}
