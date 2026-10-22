package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.shadergen.vars.Bool;
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

    public Result<Int> abs(Int a) {
        return call("_abs", a);
    }
    private @Return(Int.class) void _abs(Int a) {
        Return(new Int(String.format("abs(%s)", a)));
    }

    public Result<Int> sign(Int a) {
        return call("_sign", a);
    }
    private @Return(Int.class) void _sign(Int a) {
        Return(new Int(String.format("sign(%s)", a)));
    }

    public Result<Int> clamp(Int a, Int min, Int max) {
        return call("_clamp", a, min, max);
    }
    private @Return(Int.class) void _clamp(Int a, Int min, Int max) {
        Return(new Int(String.format("clamp(%s,%s,%s)", a, min, max)));
    }

    public Result<UInt> clamp(UInt a, UInt min, UInt max) {
        return call("_clamp", a, min, max);
    }
    private @Return(UInt.class) void _clamp(UInt a, UInt min, UInt max) {
        Return(new UInt(String.format("clamp(%s,%s,%s)", a, min, max)));
    }

    /*------------------------*\
    | ANGLE & TRIGONOMETRY     |
    \*------------------------*/

    public Result<Flt> radians(Flt degrees) {
        return call("_radians", degrees);
    }
    private @Return(Flt.class) void _radians(Flt degrees) {
        Return(new Flt(String.format("radians(%s)", degrees)));
    }

    public Result<Flt> degrees(Flt radians) {
        return call("_degrees", radians);
    }
    private @Return(Flt.class) void _degrees(Flt radians) {
        Return(new Flt(String.format("degrees(%s)", radians)));
    }

    public Result<Flt> sin(Flt a) {
        return call("_sin", a);
    }
    private @Return(Flt.class) void _sin(Flt a) {
        Return(new Flt(String.format("sin(%s)", a)));
    }

    public Result<Flt> cos(Flt a) {
        return call("_cos", a);
    }
    private @Return(Flt.class) void _cos(Flt a) {
        Return(new Flt(String.format("cos(%s)", a)));
    }

    public Result<Flt> tan(Flt a) {
        return call("_tan", a);
    }
    private @Return(Flt.class) void _tan(Flt a) {
        Return(new Flt(String.format("tan(%s)", a)));
    }

    public Result<Flt> asin(Flt a) {
        return call("_asin", a);
    }
    private @Return(Flt.class) void _asin(Flt a) {
        Return(new Flt(String.format("asin(%s)", a)));
    }

    public Result<Flt> acos(Flt a) {
        return call("_acos", a);
    }
    private @Return(Flt.class) void _acos(Flt a) {
        Return(new Flt(String.format("acos(%s)", a)));
    }

    public Result<Flt> atan(Flt yOverX) {
        return call("_atan", yOverX);
    }
    private @Return(Flt.class) void _atan(Flt yOverX) {
        Return(new Flt(String.format("atan(%s)", yOverX)));
    }

    public Result<Flt> atan(Flt y, Flt x) {
        return call("_atan", y, x);
    }
    private @Return(Flt.class) void _atan(Flt y, Flt x) {
        Return(new Flt(String.format("atan(%s,%s)", y, x)));
    }

    public Result<Flt> sinh(Flt a) {
        return call("_sinh", a);
    }
    private @Return(Flt.class) void _sinh(Flt a) {
        Return(new Flt(String.format("sinh(%s)", a)));
    }

    public Result<Flt> cosh(Flt a) {
        return call("_cosh", a);
    }
    private @Return(Flt.class) void _cosh(Flt a) {
        Return(new Flt(String.format("cosh(%s)", a)));
    }

    public Result<Flt> tanh(Flt a) {
        return call("_tanh", a);
    }
    private @Return(Flt.class) void _tanh(Flt a) {
        Return(new Flt(String.format("tanh(%s)", a)));
    }

    public Result<Flt> asinh(Flt a) {
        return call("_asinh", a);
    }
    private @Return(Flt.class) void _asinh(Flt a) {
        Return(new Flt(String.format("asinh(%s)", a)));
    }

    public Result<Flt> acosh(Flt a) {
        return call("_acosh", a);
    }
    private @Return(Flt.class) void _acosh(Flt a) {
        Return(new Flt(String.format("acosh(%s)", a)));
    }

    public Result<Flt> atanh(Flt a) {
        return call("_atanh", a);
    }
    private @Return(Flt.class) void _atanh(Flt a) {
        Return(new Flt(String.format("atanh(%s)", a)));
    }

    /*------------------------*\
    | EXPONENTIAL              |
    \*------------------------*/

    public Result<Flt> pow(Flt a, Flt exponent) {
        return call("_pow", a, exponent);
    }
    private @Return(Flt.class) void _pow(Flt a, Flt exponent) {
        Return(new Flt(String.format("pow(%s,%s)", a, exponent)));
    }

    public Result<Flt> exp(Flt a) {
        return call("_exp", a);
    }
    private @Return(Flt.class) void _exp(Flt a) {
        Return(new Flt(String.format("exp(%s)", a)));
    }

    public Result<Flt> log(Flt a) {
        return call("_log", a);
    }
    private @Return(Flt.class) void _log(Flt a) {
        Return(new Flt(String.format("log(%s)", a)));
    }

    public Result<Flt> exp2(Flt a) {
        return call("_exp2", a);
    }
    private @Return(Flt.class) void _exp2(Flt a) {
        Return(new Flt(String.format("exp2(%s)", a)));
    }

    public Result<Flt> log2(Flt a) {
        return call("_log2", a);
    }
    private @Return(Flt.class) void _log2(Flt a) {
        Return(new Flt(String.format("log2(%s)", a)));
    }

    public Result<Flt> sqrt(Flt a) {
        return call("_sqrt", a);
    }
    private @Return(Flt.class) void _sqrt(Flt a) {
        Return(new Flt(String.format("sqrt(%s)", a)));
    }

    public Result<Flt> inversesqrt(Flt a) {
        return call("_inversesqrt", a);
    }
    private @Return(Flt.class) void _inversesqrt(Flt a) {
        Return(new Flt(String.format("inversesqrt(%s)", a)));
    }

    /*------------------------*\
    | COMMON                   |
    \*------------------------*/

    public Result<Flt> abs(Flt a) {
        return call("_abs", a);
    }
    private @Return(Flt.class) void _abs(Flt a) {
        Return(new Flt(String.format("abs(%s)", a)));
    }

    public Result<Flt> sign(Flt a) {
        return call("_sign", a);
    }
    private @Return(Flt.class) void _sign(Flt a) {
        Return(new Flt(String.format("sign(%s)", a)));
    }

    public Result<Flt> floor(Flt a) {
        return call("_floor", a);
    }
    private @Return(Flt.class) void _floor(Flt a) {
        Return(new Flt(String.format("floor(%s)", a)));
    }

    public Result<Flt> ceil(Flt a) {
        return call("_ceil", a);
    }
    private @Return(Flt.class) void _ceil(Flt a) {
        Return(new Flt(String.format("ceil(%s)", a)));
    }

    public Result<Flt> trunc(Flt a) {
        return call("_trunc", a);
    }
    private @Return(Flt.class) void _trunc(Flt a) {
        Return(new Flt(String.format("trunc(%s)", a)));
    }

    public Result<Flt> round(Flt a) {
        return call("_round", a);
    }
    private @Return(Flt.class) void _round(Flt a) {
        Return(new Flt(String.format("round(%s)", a)));
    }

    public Result<Flt> roundEven(Flt a) {
        return call("_roundEven", a);
    }
    private @Return(Flt.class) void _roundEven(Flt a) {
        Return(new Flt(String.format("roundEven(%s)", a)));
    }

    public Result<Flt> fract(Flt a) {
        return call("_fract", a);
    }
    private @Return(Flt.class) void _fract(Flt a) {
        Return(new Flt(String.format("fract(%s)", a)));
    }

    public Result<Flt> mod(Flt a, Flt b) {
        return call("_mod", a, b);
    }
    private @Return(Flt.class) void _mod(Flt a, Flt b) {
        Return(new Flt(String.format("mod(%s,%s)", a, b)));
    }

    public Result<Flt> clamp(Flt a, Flt min, Flt max) {
        return call("_clamp", a, min, max);
    }
    private @Return(Flt.class) void _clamp(Flt a, Flt min, Flt max) {
        Return(new Flt(String.format("clamp(%s,%s,%s)", a, min, max)));
    }

    public Result<Flt> mix(Flt a, Flt b, Flt t) {
        return call("_mix", a, b, t);
    }
    private @Return(Flt.class) void _mix(Flt a, Flt b, Flt t) {
        Return(new Flt(String.format("mix(%s,%s,%s)", a, b, t)));
    }

    public Result<Flt> step(Flt edge, Flt a) {
        return call("_step", edge, a);
    }
    private @Return(Flt.class) void _step(Flt edge, Flt a) {
        Return(new Flt(String.format("step(%s,%s)", edge, a)));
    }

    public Result<Flt> smoothstep(Flt edge0, Flt edge1, Flt a) {
        return call("_smoothstep", edge0, edge1, a);
    }
    private @Return(Flt.class) void _smoothstep(Flt edge0, Flt edge1, Flt a) {
        Return(new Flt(String.format("smoothstep(%s,%s,%s)", edge0, edge1, a)));
    }

    public Result<Bool> isnan(Flt a) {
        return call("_isnan", a);
    }
    private @Return(Bool.class) void _isnan(Flt a) {
        Return(new Bool(String.format("isnan(%s)", a)));
    }

    public Result<Bool> isinf(Flt a) {
        return call("_isinf", a);
    }
    private @Return(Bool.class) void _isinf(Flt a) {
        Return(new Bool(String.format("isinf(%s)", a)));
    }

}
