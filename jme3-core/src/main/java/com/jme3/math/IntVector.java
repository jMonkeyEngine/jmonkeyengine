package com.jme3.math;

import com.jme3.export.*;

import java.io.IOException;

public final class IntVector implements Savable {

    public static final IntVector ZERO = new IntVector();
    public static final IntVector UNIT_X = new IntVector(1, 0, 0);
    public static final IntVector UNIT_Y = new IntVector(0, 1, 0);
    public static final IntVector UNIT_Z = new IntVector(0, 0, 1);
    public static final IntVector UNIT_XY = new IntVector(1, 1, 0);
    public static final IntVector UNIT_XYZ = new IntVector(1, 1, 1);
    public static final IntVector MAX = new IntVector(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    public int x, y, z;

    public IntVector() {}

    public IntVector(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public IntVector(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public IntVector(IntVector v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public IntVector set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public IntVector set(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public IntVector set(IntVector v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(x, "x", 0);
        out.write(y, "y", 0);
        out.write(z, "z", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        x = in.readInt("x", 0);
        y = in.readInt("y", 0);
        z = in.readInt("z", 0);
    }

}
