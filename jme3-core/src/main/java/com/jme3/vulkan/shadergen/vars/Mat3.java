package com.jme3.vulkan.shadergen.vars;

public final class Mat3 extends MathVar<Mat3> {

    public static final String TYPE = "mat3";

    public Mat3() {
        this("mat3()");
    }

    public Mat3(float diagonal) {
        this(fmt("mat3(%f)", diagonal));
    }

    public Mat3(Flt diagonal) {
        this(fmt("mat3(%s)", diagonal));
    }

    public Mat3(Vec3 col0, Vec3 col1, Vec3 col2) {
        this(fmt("mat3(%s,%s,%s)", col0, col1, col2));
    }

    public Mat3(Mat3 m) {
        this(fmt("mat3(%s)", m));
    }

    /**
     * Constructs the upper-left 3x3 submatrix of {@code m}.
     */
    public Mat3(Mat4 m) {
        this(fmt("mat3(%s)", m));
    }

    public Mat3(String exp) {
        super(TYPE, exp);
    }

    @Override
    public Mat3 create(String exp) {
        return new Mat3(exp);
    }

    public Vec3 mult(Vec3 v) {
        return new Vec3(fmt("(%s*%s)", exp, v));
    }

    /**
     * Gets column {@code i} (0-2) of this matrix.
     */
    public Vec3 col(int i) {
        return new Vec3(fmt("%s[%d]", exp, i));
    }

    public Mat3 transpose() {
        return create(fmt("transpose(%s)", exp));
    }

    public Mat3 inverse() {
        return create(fmt("inverse(%s)", exp));
    }

    public Flt determinant() {
        return new Flt(fmt("determinant(%s)", exp));
    }

    /**
     * Expands this matrix into a 4x4 matrix, with the new row and column set to the
     * identity matrix's (0,0,0,1).
     */
    public Mat4 mat4() {
        return new Mat4(fmt("mat4(%s)", exp));
    }

}
