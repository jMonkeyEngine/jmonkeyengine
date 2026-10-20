package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.shadergen.vars.Int;

public class MyShaderLib extends Shader {

    public Result<Int> divideIntegers(Int a, Int b, @Out Int remainder) {
        return call("_divideIntegers", a, b, remainder);
    }
    private @Return(Int.class) void _divideIntegers(Int a, Int b, @Out Int remainder) {
        remainder.set(a.mod(b));
        Return(a.div(b));
    }

}
