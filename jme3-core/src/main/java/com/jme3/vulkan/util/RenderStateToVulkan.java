package com.jme3.vulkan.util;

import com.jme3.material.RenderState.*;
import com.jme3.vulkan.pipeline.*;

import static org.lwjgl.vulkan.VK10.*;

public class RenderStateToVulkan {

    private static RuntimeException unrecognized(Object state) {
        return new UnsupportedOperationException("Unrecognized: " + state);
    }

    public static IntEnum<CompareOp> depthFunc(TestFunction func) {
        switch (func) {
            case Always: return CompareOp.Always;
            case Equal: return CompareOp.Equal;
            case Greater: return CompareOp.Greater;
            case Less: return CompareOp.Less;
            case LessOrEqual: return CompareOp.LessOrEqual;
            case GreaterOrEqual: return CompareOp.GreaterOrEqual;
            case Never: return CompareOp.Never;
            case NotEqual: return CompareOp.NotEqual;
            default: throw unrecognized(func);
        }
    }

    public static IntEnum<BlendOp> blendEquation(BlendEquation eq) {
        switch (eq) {
            case Add: return BlendOp.Add;
            case Subtract: return BlendOp.Subtract;
            case ReverseSubtract: return BlendOp.ReverseSubtract;
            case Min: return BlendOp.Min;
            case Max: return BlendOp.Max;
            default: throw unrecognized(eq);
        }
    }

    public static IntEnum<BlendOp> blendEquationAlpha(BlendEquationAlpha eqA, BlendEquation eq) {
        switch (eqA) {
            case InheritColor: return blendEquation(eq);
            case Add: return BlendOp.Add;
            case Subtract: return BlendOp.Subtract;
            case ReverseSubtract: return BlendOp.ReverseSubtract;
            case Min: return BlendOp.Min;
            case Max: return BlendOp.Max;
            default: throw unrecognized(eqA);
        }
    }

    public static IntEnum<BlendFactor> blendFunc(BlendFunc func) {
        switch (func) {
            case Zero: return BlendFactor.Zero;
            case One: return BlendFactor.One;
            case Src_Color: return BlendFactor.SrcColor;
            case One_Minus_Src_Color: return BlendFactor.OneMinusSrcColor;
            case Dst_Color: return BlendFactor.DstColor;
            case One_Minus_Dst_Color: return BlendFactor.OneMinusDstColor;
            case Src_Alpha: return BlendFactor.SrcAlpha;
            case One_Minus_Src_Alpha: return BlendFactor.OneMinusSrcAlpha;
            case Dst_Alpha: return BlendFactor.DstAlpha;
            case One_Minus_Dst_Alpha: return BlendFactor.OneMinusDstAlpha;
            case Src_Alpha_Saturate: return BlendFactor.SrcAlphaSaturate;
            default: throw unrecognized(func);
        }
    }

    public static Flag<CullMode> faceCull(FaceCullMode mode) {
        switch (mode) {
            case Off: return CullMode.None;
            case Front: return CullMode.Front;
            case Back: return CullMode.Back;
            case FrontAndBack: return CullMode.FrontAndBack;
            default: throw unrecognized(mode);
        }
    }

    public static IntEnum<PolygonMode> wireframe(boolean wireframe, IntEnum<PolygonMode> def) {
        return wireframe ? PolygonMode.Line : def;
    }

}
