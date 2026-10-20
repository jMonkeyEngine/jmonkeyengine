package com.jme3.vulkan.shadergen;

import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.shadergen.vars.*;

@ShadingLangVersion(glsl = 450)
public class MyShader extends Shader {

    public static class Something extends StructVar {
        public final Field<Float, Flt> b = new Field<>(0f, Flt::new);
        {
            add(b);
            setupLayout(StructLayout.std140);
        }
        public Something(Var... vars) {
            super(vars);
        }
        public Something(String exp) {
            super(exp);
        }
    }

    public static class SceneData extends StructVar {
        public final Field<Integer, Int> a = new Field<>(0, Int::new);
        public final SubStructArray<Something> s = new SubStructArray<>("s", 5, new Something());
        public final SubStruct<Something> thing = new SubStruct<>("thing", new Something());
        {
            add(a, s, thing);
            setupLayout(StructLayout.std140);
        }
        public SceneData(Var... vars) {
            super(vars);
        }
        public SceneData(String exp) {
            super(exp);
        }
    }

    @Uniform @Binding(set=0)
    private final SceneData scene = new SceneData();

    @EntryPoint(ShaderStage.Vertex)
    public void vertex(@In(0) Vec3 position, @In(1) Vec2 texCoord, @Out(0) Vec2 outTexCoord) {
        outTexCoord.set(texCoord);
        scene.a.var().add(1);
        scene.s.idx(3).b.var().add(1f);
        gl.clipPosition.swizzle('x', 'y', 'z').set(position);
    }

    @EntryPoint(ShaderStage.Fragment)
    public void fragment(@In(0) Vec2 texCoord, @Out(0) Vec4 color) {
        Vec2 vec = new Vec2(0.5f, 0.25f).declare();
        Int threshold = new Int(5);
        While(new Bool(false), () -> {
            texCoord.multLocal(vec.yx());
            Break();
        });
        For(0, 10, i -> {
            texCoord.x().addLocal(i.flt());
            If(i.gte(threshold), () -> {
                Break();
            }).ElseIf(i.lt(threshold.sub(3)), () -> {
                Continue();
            }).Else(() -> {
                // do something
            });
            texCoord.y().addLocal(i.flt());
        });
        doSomething(Float(2), Int(3), vec2(5, 6));
        sumIntegers(Int(3), Int(4)).Void();
        color.set(texCoord.zw(Float(0f), sumIntegers(Int(3), Int(4)).use().flt()));
    }

    @EntryPoint(ShaderStage.Compute)
    @Work(x=16, y=16)
    public void compute(@In @ArraySpec(type=Int.class, sizes=3) Array1D<Int> array) {
        MyShaderLib lib = Import(new MyShaderLib());
        SMath math = Import(new SMath());
        Int remainder = Int(0);
        lib.divideIntegers(array.index(0), array.index(1), remainder).Void();
        remainder.set(math.max(remainder, array.index(2)).use());
    }

    public Result<Int> sumIntegers(Int a, Int b) {
        return call("_sumIntegers", a, b);
    }
    private @Return(Int.class) void _sumIntegers(Int a, Int b) {
        Return(a.add(b));
    }

    public void doSomething(Flt a, Int b, @In @Out Vec2 c) {
        call("_doSomething", a, b, c);
    }
    private void _doSomething(Flt a, Int b, @In @Out Vec2 c) {
        a.addLocal(b);
        c.multLocal(a);
    }

}
