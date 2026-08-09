package com.jme3.math;

import com.jme3.export.*;

import java.io.IOException;

public final class Vector3i implements Savable {

    public static final Vector3i ZERO = new Vector3i();
    public static final Vector3i UNIT_X = new Vector3i(1, 0, 0);
    public static final Vector3i UNIT_Y = new Vector3i(0, 1, 0);
    public static final Vector3i UNIT_Z = new Vector3i(0, 0, 1);
    public static final Vector3i UNIT_XYZ = new Vector3i(1, 1, 1);
    public static final Vector3i MAX = new Vector3i(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    public int x, y, z;

    public Vector3i() {}

    public Vector3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3i(Vector3i v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public Vector3i set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3i set(Vector3i v) {
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
