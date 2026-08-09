package com.jme3.math;

import com.jme3.export.*;

import java.io.IOException;

public class Vector2i implements Savable {

    public static final Vector2i ZERO = new Vector2i();
    public static final Vector2i UNIT_X = new Vector2i(1, 0);
    public static final Vector2i UNIT_Y = new Vector2i(0, 1);
    public static final Vector2i UNIT_XY = new Vector2i(1, 1);

    public int x, y;

    public Vector2i() {}

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vector2i(Vector2i v) {
        x = v.x;
        y = v.y;
    }

    public Vector2i set(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2i set(Vector2i v) {
        x = v.x;
        y = v.y;
        return this;
    }

    public Vector3i toVector3(int z) {
        return new Vector3i(x, y, z);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(x, "x", 0);
        out.write(y, "y", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        x = in.readInt("x", 0);
        y = in.readInt("y", 0);
    }

}
