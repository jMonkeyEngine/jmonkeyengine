package com.jme3.vulkan.images;

import com.jme3.export.*;
import com.jme3.vulkan.formats.EnumInterpreter;

import java.io.IOException;

public class ColorSwizzle implements Savable {

    public enum Component {

        R, G, B, A, Zero, One, Identity;

        public int getEnum(EnumInterpreter interpreter) {
            return interpreter.getColorSwizzleEnum(this);
        }

    }

    public static final ColorSwizzle NORMAL = new ColorSwizzle();

    public Component r, g, b, a;

    public ColorSwizzle() {
        this(Component.R, Component.G, Component.B, Component.A);
    }

    public ColorSwizzle(Component r, Component g, Component b, Component a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public ColorSwizzle(ColorSwizzle c) {
        r = c.r;
        g = c.g;
        b = c.b;
        a = c.a;
    }

    public ColorSwizzle set(Component r, Component g, Component b, Component a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    public ColorSwizzle set(ColorSwizzle c) {
        r = c.r;
        g = c.g;
        b = c.b;
        a = c.a;
        return this;
    }

    public Component getR() {
        return r;
    }

    public Component getG() {
        return g;
    }

    public Component getB() {
        return b;
    }

    public Component getA() {
        return a;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(r, "r", Component.R);
        out.write(g, "g", Component.G);
        out.write(b, "b", Component.B);
        out.write(a, "a", Component.A);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        r = in.readEnum("r", Component.class, Component.R);
        g = in.readEnum("g", Component.class, Component.G);
        b = in.readEnum("b", Component.class, Component.B);
        a = in.readEnum("a", Component.class, Component.A);
    }
}
