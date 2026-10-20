package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.shadergen.vars.Flt;
import com.jme3.vulkan.shadergen.vars.Int;
import com.jme3.vulkan.shadergen.vars.UInt;

public class SMath extends Shader {

    public Result<Int> min(Int a, Int b) {
        return call("_min", a, b);
    }
    private @Return(Int.class) void _min(Int a, Int b) {
        Return(new Int(String.format("min(%s,%s)", a, b)));
    }

    public Result<Int> max(Int a, Int b) {
        return call("_max", a, b);
    }
    private @Return(Int.class) void _max(Int a, Int b) {
        Return(new Int(String.format("max(%s,%s)", a, b)));
    }

    public Result<UInt> min(UInt a, UInt b) {
        return call("_min", a, b);
    }
    private @Return(UInt.class) void _min(UInt a, UInt b) {
        Return(new UInt(String.format("min(%s,%s)", a, b)));
    }

    public Result<UInt> max(UInt a, UInt b) {
        return call("_max", a, b);
    }
    private @Return(UInt.class) void _max(UInt a, UInt b) {
        Return(new UInt(String.format("max(%s,%s)", a, b)));
    }

    public Result<Flt> min(Flt a, Flt b) {
        return call("_min", a, b);
    }
    private @Return(Flt.class) void _min(Flt a, Flt b) {
        Return(new Flt(String.format("min(%s,%s)", a, b)));
    }

    public Result<Flt> max(Flt a, Flt b) {
        return call("_max", a, b);
    }
    private @Return(Flt.class) void _max(Flt a, Flt b) {
        Return(new Flt(String.format("max(%s,%s)", a, b)));
    }

}
