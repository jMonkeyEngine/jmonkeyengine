package com.jme3.vulkan;

import com.jme3.material.RenderState.*;
import static org.lwjgl.vulkan.VK10.*;

public class RenderStateToVulkan {

    private static RuntimeException unrecognized(Object state) {
        return new UnsupportedOperationException("Unrecognized: " + state);
    }

    public static int blendEquation(BlendEquation eq) {
        switch (eq) {
            case Add: return VK_BLEND_OP_ADD;
            case Subtract: return VK_BLEND_OP_SUBTRACT;
            case ReverseSubtract: return VK_BLEND_OP_REVERSE_SUBTRACT;
            case Min: return VK_BLEND_OP_MIN;
            case Max: return VK_BLEND_OP_MAX;
            default: throw unrecognized(eq);
        }
    }

    public static int blendEquationAlpha(BlendEquationAlpha eqA, BlendEquation eq) {
        switch (eqA) {
            case InheritColor: return blendEquation(eq);
            case Add: return VK_BLEND_OP_ADD;
            case Subtract: return VK_BLEND_OP_SUBTRACT;
            case ReverseSubtract: return VK_BLEND_OP_REVERSE_SUBTRACT;
            case Min: return VK_BLEND_OP_MIN;
            case Max: return VK_BLEND_OP_MAX;
            default: throw unrecognized(eqA);
        }
    }

    public static int blendFunc(BlendFunc func) {
        switch (func) {
            case Zero: return VK_BLEND_FACTOR_ZERO;
            case One: return VK_BLEND_FACTOR_ONE;
            case Src_Color: return VK_BLEND_FACTOR_SRC_COLOR;
            case One_Minus_Src_Color: return VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
            case Dst_Color: return VK_BLEND_FACTOR_DST_COLOR;
            case One_Minus_Dst_Color: return VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
            case Src_Alpha: return VK_BLEND_FACTOR_SRC_ALPHA;
            case One_Minus_Src_Alpha: return VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            case Dst_Alpha: return VK_BLEND_FACTOR_DST_ALPHA;
            case One_Minus_Dst_Alpha: return VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
            case Src_Alpha_Saturate: return VK_BLEND_FACTOR_SRC_ALPHA_SATURATE;
            default: throw unrecognized(func);
        }
    }

    public static int faceCull(FaceCullMode mode) {
        switch (mode) {
            case Off: return VK_CULL_MODE_NONE;
            case Front: return VK_CULL_MODE_FRONT_BIT;
            case Back: return VK_CULL_MODE_BACK_BIT;
            case FrontAndBack: return VK_CULL_MODE_FRONT_AND_BACK;
            default: throw unrecognized(mode);
        }
    }

    public static int wireframe(boolean wireframe, int def) {
        return wireframe ? VK_POLYGON_MODE_LINE : def;
    }

    public static int wireframe(boolean wireframe) {
        return wireframe(wireframe, VK_POLYGON_MODE_FILL);
    }

}
