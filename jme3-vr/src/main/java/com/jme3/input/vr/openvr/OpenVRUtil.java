package com.jme3.input.vr.openvr;

import static org.lwjgl.openvr.VR.*;

public class OpenVRUtil {

    /**
     * Get the description of the given {@link EColorSpace color space}.
     * @param eColorSpace the color space.
     * @return the description of the given color space.
     */
    public static String getEColorSpaceString(int eColorSpace){
        String str = "";

        switch(eColorSpace){
            case EColorSpace_ColorSpace_Auto:
                str = "Auto";
                break;
            case EColorSpace_ColorSpace_Gamma:
                str = "Gamma";
                break;
            case EColorSpace_ColorSpace_Linear:
                str = "Linear";
                break;
            default:
                str = "Unknown ("+eColorSpace+")";
        }

        return str;
    }

    /**
     * Get the description of the given {@link ETextureType texture type}.
     * @param type the texture type
     * @return the description of the given texture type.
     */
    public static String getETextureTypeString(int type){

        String str = "";

        switch(type){
            case ETextureType_TextureType_DirectX:
                str = "DirectX";
                break;
            case ETextureType_TextureType_OpenGL:
                str = "OpenGL";
                break;
            case ETextureType_TextureType_Vulkan:
                str = "Vulkan";
                break;
            case ETextureType_TextureType_IOSurface:
                str = "IOSurface";
                break;
            case ETextureType_TextureType_DirectX12:
                str = "DirectX12";
                break;
            default:
                str = "Unknown ("+type+")";
        }

        return str;
    }
}
