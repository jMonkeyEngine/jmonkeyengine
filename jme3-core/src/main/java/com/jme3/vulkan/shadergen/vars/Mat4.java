package com.jme3.vulkan.shadergen.vars;

public final class Mat4 extends MathVar<Mat4> {

    public static final String TYPE = "mat4";

    public Mat4() {
        this("mat4()");
    }

    public Mat4(float diagonal) {
        this(fmt("mat4(%f)", diagonal));
    }

    public Mat4(Flt diagonal) {
        this(fmt("mat4(%s)", diagonal));
    }

    public Mat4(Vec4 col0, Vec4 col1, Vec4 col2, Vec4 col3) {
        this(fmt("mat4(%s,%s,%s,%s)", col0, col1, col2, col3));
    }

    public Mat4(Mat4 m) {
        this(fmt("mat4(%s)", m));
    }

    /**
     * Expands {@code m} into a 4x4 matrix, with the new row and column set to the
     * identity matrix's (0,0,0,1).
     */
    public Mat4(Mat3 m) {
        this(fmt("mat4(%s)", m));
    }

    public Mat4(String exp) {
        super(TYPE, exp);
    }

    @Override
    public Mat4 create(String exp) {
        return new Mat4(exp);
    }

    public Vec4 mult(Vec4 v) {
        return new Vec4(fmt("(%s*%s)", exp, v));
    }

    /**
     * Gets column {@code i} (0-3) of this matrix.
     */
    public Vec4 col(int i) {
        return new Vec4(fmt("%s[%d]", exp, i));
    }

    public Mat4 transpose() {
        return create(fmt("transpose(%s)", exp));
    }

    public Mat4 inverse() {
        return create(fmt("inverse(%s)", exp));
    }

    public Flt determinant() {
        return new Flt(fmt("determinant(%s)", exp));
    }

    /**
     * Gets the upper-left 3x3 submatrix of this matrix.
     */
    public Mat3 mat3() {
        return new Mat3(fmt("mat3(%s)", exp));
    }

}
