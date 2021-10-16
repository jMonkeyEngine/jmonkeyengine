/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.texture.plugins;

/**
 * Holds the constants for DXGI format defined in DDS file
 * 
 * @author Toni Helenius
 */
final class DXGIFormat {

    static final int DXGI_FORMAT_UNKNOWN = 0x00;
    static final int DXGI_FORMAT_R32G32B32A32_TYPELESS = 0x01;
    static final int DXGI_FORMAT_R32G32B32A32_FLOAT = 0x02;
    static final int DXGI_FORMAT_R32G32B32A32_UINT = 0x03;
    static final int DXGI_FORMAT_R32G32B32A32_SINT = 0x04;
    static final int DXGI_FORMAT_R32G32B32_TYPELESS = 0x05;
    static final int DXGI_FORMAT_R32G32B32_FLOAT = 0x06;
    static final int DXGI_FORMAT_R32G32B32_UINT = 0x07;
    static final int DXGI_FORMAT_R32G32B32_SINT = 0x08;
    static final int DXGI_FORMAT_R16G16B16A16_TYPELESS = 0x09;
    static final int DXGI_FORMAT_R16G16B16A16_FLOAT = 0x0A;
    static final int DXGI_FORMAT_R16G16B16A16_UNORM = 0x0B;
    static final int DXGI_FORMAT_R16G16B16A16_UINT = 0x0C;
    static final int DXGI_FORMAT_R16G16B16A16_SNORM = 0x0D;
    static final int DXGI_FORMAT_R16G16B16A16_SINT = 0x0E;
    static final int DXGI_FORMAT_R32G32_TYPELESS = 0x0F;
    static final int DXGI_FORMAT_R32G32_FLOAT = 0x10;
    static final int DXGI_FORMAT_R32G32_UINT = 0x11;
    static final int DXGI_FORMAT_R32G32_SINT = 0x12;
    static final int DXGI_FORMAT_R32G8X24_TYPELESS = 0x13;
    static final int DXGI_FORMAT_D32_FLOAT_S8X24_UINT = 0x14;
    static final int DXGI_FORMAT_R32_FLOAT_X8X24_TYPELESS = 0x15;
    static final int DXGI_FORMAT_X32_TYPELESS_G8X24_UINT = 0x16;
    static final int DXGI_FORMAT_R10G10B10A2_TYPELESS = 0x17;
    static final int DXGI_FORMAT_R10G10B10A2_UNORM = 0x18;
    static final int DXGI_FORMAT_R10G10B10A2_UINT = 0x19;
    static final int DXGI_FORMAT_R11G11B10_FLOAT = 0x1A;
    static final int DXGI_FORMAT_R8G8B8A8_TYPELESS = 0x1B;
    static final int DXGI_FORMAT_R8G8B8A8_UNORM = 0x1C;
    static final int DXGI_FORMAT_R8G8B8A8_UNORM_SRGB = 0x1D;
    static final int DXGI_FORMAT_R8G8B8A8_UINT = 0x1E;
    static final int DXGI_FORMAT_R8G8B8A8_SNORM = 0x1F;
    static final int DXGI_FORMAT_R8G8B8A8_SINT = 0x20;
    static final int DXGI_FORMAT_R16G16_TYPELESS = 0x21;
    static final int DXGI_FORMAT_R16G16_FLOAT = 0x22;
    static final int DXGI_FORMAT_R16G16_UNORM = 0x23;
    static final int DXGI_FORMAT_R16G16_UINT = 0x24;
    static final int DXGI_FORMAT_R16G16_SNORM = 0x25;
    static final int DXGI_FORMAT_R16G16_SINT = 0x26;
    static final int DXGI_FORMAT_R32_TYPELESS = 0x27;
    static final int DXGI_FORMAT_D32_FLOAT = 0x28;
    static final int DXGI_FORMAT_R32_FLOAT = 0x29;
    static final int DXGI_FORMAT_R32_UINT = 0x2A;
    static final int DXGI_FORMAT_R32_SINT = 0x2B;
    static final int DXGI_FORMAT_R24G8_TYPELESS = 0x2C;
    static final int DXGI_FORMAT_D24_UNORM_S8_UINT = 0x2D;
    static final int DXGI_FORMAT_R24_UNORM_X8_TYPELESS = 0x2E;
    static final int DXGI_FORMAT_X24_TYPELESS_G8_UINT = 0x2F;
    static final int DXGI_FORMAT_R8G8_TYPELESS = 0x30;
    static final int DXGI_FORMAT_R8G8_UNORM = 0x31;
    static final int DXGI_FORMAT_R8G8_UINT = 0x32;
    static final int DXGI_FORMAT_R8G8_SNORM = 0x33;
    static final int DXGI_FORMAT_R8G8_SINT = 0x34;
    static final int DXGI_FORMAT_R16_TYPELESS = 0x35;
    static final int DXGI_FORMAT_R16_FLOAT = 0x36;
    static final int DXGI_FORMAT_D16_UNORM = 0x37;
    static final int DXGI_FORMAT_R16_UNORM = 0x38;
    static final int DXGI_FORMAT_R16_UINT = 0x39;
    static final int DXGI_FORMAT_R16_SNORM = 0x3A;
    static final int DXGI_FORMAT_R16_SINT = 0x3B;
    static final int DXGI_FORMAT_R8_TYPELESS = 0x3C;
    static final int DXGI_FORMAT_R8_UNORM = 0x3D;
    static final int DXGI_FORMAT_R8_UINT = 0x3E;
    static final int DXGI_FORMAT_R8_SNORM = 0x3F;
    static final int DXGI_FORMAT_R8_SINT = 0x40;
    static final int DXGI_FORMAT_A8_UNORM = 0x41;
    static final int DXGI_FORMAT_R1_UNORM = 0x42;
    static final int DXGI_FORMAT_R9G9B9E5_SHAREDEXP = 0x43;
    static final int DXGI_FORMAT_R8G8_B8G8_UNORM = 0x44;
    static final int DXGI_FORMAT_G8R8_G8B8_UNORM = 0x45;
    static final int DXGI_FORMAT_BC1_TYPELESS = 0x46;
    static final int DXGI_FORMAT_BC1_UNORM = 0x47;
    static final int DXGI_FORMAT_BC1_UNORM_SRGB = 0x48;
    static final int DXGI_FORMAT_BC2_TYPELESS = 0x49;
    static final int DXGI_FORMAT_BC2_UNORM = 0x4A;
    static final int DXGI_FORMAT_BC2_UNORM_SRGB = 0x4B;
    static final int DXGI_FORMAT_BC3_TYPELESS = 0x4C;
    static final int DXGI_FORMAT_BC3_UNORM = 0x4D;
    static final int DXGI_FORMAT_BC3_UNORM_SRGB = 0x4E;
    static final int DXGI_FORMAT_BC4_TYPELESS = 0x4F;
    static final int DXGI_FORMAT_BC4_UNORM = 0x50;
    static final int DXGI_FORMAT_BC4_SNORM = 0x51;
    static final int DXGI_FORMAT_BC5_TYPELESS = 0x52;
    static final int DXGI_FORMAT_BC5_UNORM = 0x53;
    static final int DXGI_FORMAT_BC5_SNORM = 0x54;
    static final int DXGI_FORMAT_B5G6R5_UNORM = 0x55;
    static final int DXGI_FORMAT_B5G5R5A1_UNORM = 0x56;
    static final int DXGI_FORMAT_B8G8R8A8_UNORM = 0x57;
    static final int DXGI_FORMAT_B8G8R8X8_UNORM = 0x58;
    static final int DXGI_FORMAT_R10G10B10_XR_BIAS_A2_UNORM = 0x59;
    static final int DXGI_FORMAT_B8G8R8A8_TYPELESS = 0x5A;
    static final int DXGI_FORMAT_B8G8R8A8_UNORM_SRGB = 0x5B;
    static final int DXGI_FORMAT_B8G8R8X8_TYPELESS = 0x5C;
    static final int DXGI_FORMAT_B8G8R8X8_UNORM_SRGB = 0x5D;
    static final int DXGI_FORMAT_BC6H_TYPELESS = 0x5E;
    static final int DXGI_FORMAT_BC6H_UF16 = 0x5F;
    static final int DXGI_FORMAT_BC6H_SF16 = 0x60;
    static final int DXGI_FORMAT_BC7_TYPELESS = 0x61;
    static final int DXGI_FORMAT_BC7_UNORM = 0x62;
    static final int DXGI_FORMAT_BC7_UNORM_SRGB = 0x63;
    static final int DXGI_FORMAT_AYUV = 0x64;
    static final int DXGI_FORMAT_Y410 = 0x65;
    static final int DXGI_FORMAT_Y416 = 0x66;
    static final int DXGI_FORMAT_NV12 = 0x67;
    static final int DXGI_FORMAT_P010 = 0x68;
    static final int DXGI_FORMAT_P016 = 0x69;
    static final int DXGI_FORMAT_420_OPAQUE = 0x6A;
    static final int DXGI_FORMAT_YUY2 = 0x6B;
    static final int DXGI_FORMAT_Y210 = 0x6C;
    static final int DXGI_FORMAT_Y216 = 0x6D;
    static final int DXGI_FORMAT_NV11 = 0x6E;
    static final int DXGI_FORMAT_AI44 = 0x6F;
    static final int DXGI_FORMAT_IA44 = 0x70;
    static final int DXGI_FORMAT_P8 = 0x71;
    static final int DXGI_FORMAT_A8P8 = 0x72;
    static final int DXGI_FORMAT_B4G4R4A4_UNORM = 0x73;
    static final int DXGI_FORMAT_P208 = 0x74;
    static final int DXGI_FORMAT_V208 = 0x75;
    static final int DXGI_FORMAT_V408 = 0x76;
    static final int DXGI_FORMAT_SAMPLER_FEEDBACK_MIN_MIP_OPAQUE = 0x77;
    static final int DXGI_FORMAT_SAMPLER_FEEDBACK_MIP_REGION_USED_OPAQUE = 0x78;
    static final int DXGI_FORMAT_FORCE_UINT = 0x79;

    static int getBitsPerPixel(int dxgiFormat) {
        switch (dxgiFormat) {
            case DXGI_FORMAT_R32G32B32A32_TYPELESS:
            case DXGI_FORMAT_R32G32B32A32_FLOAT:
            case DXGI_FORMAT_R32G32B32A32_UINT:
            case DXGI_FORMAT_R32G32B32A32_SINT:
                return 128;

            case DXGI_FORMAT_R32G32B32_TYPELESS:
            case DXGI_FORMAT_R32G32B32_FLOAT:
            case DXGI_FORMAT_R32G32B32_UINT:
            case DXGI_FORMAT_R32G32B32_SINT:
                return 96;

            case DXGI_FORMAT_R16G16B16A16_TYPELESS:
            case DXGI_FORMAT_R16G16B16A16_FLOAT:
            case DXGI_FORMAT_R16G16B16A16_UNORM:
            case DXGI_FORMAT_R16G16B16A16_UINT:
            case DXGI_FORMAT_R16G16B16A16_SNORM:
            case DXGI_FORMAT_R16G16B16A16_SINT:
            case DXGI_FORMAT_R32G32_TYPELESS:
            case DXGI_FORMAT_R32G32_FLOAT:
            case DXGI_FORMAT_R32G32_UINT:
            case DXGI_FORMAT_R32G32_SINT:
            case DXGI_FORMAT_R32G8X24_TYPELESS:
            case DXGI_FORMAT_D32_FLOAT_S8X24_UINT:
            case DXGI_FORMAT_R32_FLOAT_X8X24_TYPELESS:
            case DXGI_FORMAT_X32_TYPELESS_G8X24_UINT:
                return 64;

            case DXGI_FORMAT_R10G10B10A2_TYPELESS:
            case DXGI_FORMAT_R10G10B10A2_UNORM:
            case DXGI_FORMAT_R10G10B10A2_UINT:
            case DXGI_FORMAT_R11G11B10_FLOAT:
            case DXGI_FORMAT_R8G8B8A8_TYPELESS:
            case DXGI_FORMAT_R8G8B8A8_UNORM:
            case DXGI_FORMAT_R8G8B8A8_UNORM_SRGB:
            case DXGI_FORMAT_R8G8B8A8_UINT:
            case DXGI_FORMAT_R8G8B8A8_SNORM:
            case DXGI_FORMAT_R8G8B8A8_SINT:
            case DXGI_FORMAT_R16G16_TYPELESS:
            case DXGI_FORMAT_R16G16_FLOAT:
            case DXGI_FORMAT_R16G16_UNORM:
            case DXGI_FORMAT_R16G16_UINT:
            case DXGI_FORMAT_R16G16_SNORM:
            case DXGI_FORMAT_R16G16_SINT:
            case DXGI_FORMAT_R32_TYPELESS:
            case DXGI_FORMAT_D32_FLOAT:
            case DXGI_FORMAT_R32_FLOAT:
            case DXGI_FORMAT_R32_UINT:
            case DXGI_FORMAT_R32_SINT:
            case DXGI_FORMAT_R24G8_TYPELESS:
            case DXGI_FORMAT_D24_UNORM_S8_UINT:
            case DXGI_FORMAT_R24_UNORM_X8_TYPELESS:
            case DXGI_FORMAT_X24_TYPELESS_G8_UINT:
            case DXGI_FORMAT_R9G9B9E5_SHAREDEXP:
            case DXGI_FORMAT_R8G8_B8G8_UNORM:
            case DXGI_FORMAT_G8R8_G8B8_UNORM:
            case DXGI_FORMAT_B8G8R8A8_UNORM:
            case DXGI_FORMAT_B8G8R8X8_UNORM:
            case DXGI_FORMAT_R10G10B10_XR_BIAS_A2_UNORM:
            case DXGI_FORMAT_B8G8R8A8_TYPELESS:
            case DXGI_FORMAT_B8G8R8A8_UNORM_SRGB:
            case DXGI_FORMAT_B8G8R8X8_TYPELESS:
            case DXGI_FORMAT_B8G8R8X8_UNORM_SRGB:
                return 32;

            case DXGI_FORMAT_R8G8_TYPELESS:
            case DXGI_FORMAT_R8G8_UNORM:
            case DXGI_FORMAT_R8G8_UINT:
            case DXGI_FORMAT_R8G8_SNORM:
            case DXGI_FORMAT_R8G8_SINT:
            case DXGI_FORMAT_R16_TYPELESS:
            case DXGI_FORMAT_R16_FLOAT:
            case DXGI_FORMAT_D16_UNORM:
            case DXGI_FORMAT_R16_UNORM:
            case DXGI_FORMAT_R16_UINT:
            case DXGI_FORMAT_R16_SNORM:
            case DXGI_FORMAT_R16_SINT:
            case DXGI_FORMAT_B5G6R5_UNORM:
            case DXGI_FORMAT_B5G5R5A1_UNORM:
            case DXGI_FORMAT_B4G4R4A4_UNORM:
                return 16;

            case DXGI_FORMAT_R8_TYPELESS:
            case DXGI_FORMAT_R8_UNORM:
            case DXGI_FORMAT_R8_UINT:
            case DXGI_FORMAT_R8_SNORM:
            case DXGI_FORMAT_R8_SINT:
            case DXGI_FORMAT_A8_UNORM:
                return 8;

            case DXGI_FORMAT_R1_UNORM:
                return 1;

            case DXGI_FORMAT_BC1_TYPELESS:
            case DXGI_FORMAT_BC1_UNORM:
            case DXGI_FORMAT_BC1_UNORM_SRGB:
            case DXGI_FORMAT_BC4_TYPELESS:
            case DXGI_FORMAT_BC4_UNORM:
            case DXGI_FORMAT_BC4_SNORM:
                return 4;

            case DXGI_FORMAT_BC2_TYPELESS:
            case DXGI_FORMAT_BC2_UNORM:
            case DXGI_FORMAT_BC2_UNORM_SRGB:
            case DXGI_FORMAT_BC3_TYPELESS:
            case DXGI_FORMAT_BC3_UNORM:
            case DXGI_FORMAT_BC3_UNORM_SRGB:
            case DXGI_FORMAT_BC5_TYPELESS:
            case DXGI_FORMAT_BC5_UNORM:
            case DXGI_FORMAT_BC5_SNORM:
            case DXGI_FORMAT_BC6H_TYPELESS:
            case DXGI_FORMAT_BC6H_UF16:
            case DXGI_FORMAT_BC6H_SF16:
            case DXGI_FORMAT_BC7_TYPELESS:
            case DXGI_FORMAT_BC7_UNORM:
            case DXGI_FORMAT_BC7_UNORM_SRGB:
                return 8;

            default:
                return 0;
        }
    }
    
    static int getBlockSize(int dxgiFormat) {
        switch (dxgiFormat) {
            case DXGI_FORMAT_BC1_UNORM:
            case DXGI_FORMAT_BC4_UNORM:
            case DXGI_FORMAT_BC4_SNORM:
                return 8;
        }
        
        return 16;
    }

    }
