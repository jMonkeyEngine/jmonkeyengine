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

/**
 * Utilized by the ColorContrast.j3md to adjust the textures color contrast and brightness
 * based on a transfer function that uses a simple power law on color.rgb before processing those colors as a fragment
 * shader by the rasterizer, the color channels can be scaled independently again at the final pass using
 * a scale factor from 0 to 1.0.
 *
 * Supports glsl150 and glsl150+ including android GLES glsl300, glsl310 and glsl320.
 */

#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

//constant inputs from java source
uniform float m_exp_r;
uniform float m_exp_g;
uniform float m_exp_b;

//final scale values
uniform float m_scale_r;
uniform float m_scale_g;
uniform float m_scale_b;

//min and max brightness
uniform float m_minBrightness;
uniform float m_maxBrightness;

//container for the input from post15.vert
uniform COLORTEXTURE m_Texture;

//varying input from post15.vert vertex shader
in vec2 texCoord;

//the output color
out vec4 fragColor;

void main() {
    //get the color from a 2d sampler.
    vec4 color = texture2D(m_Texture, texCoord);

    //apply the color transfer function.

    //1) apply brightness to color.rgb.
    color.rgb = (color.rgb - vec3(m_minBrightness)) / (vec3(m_maxBrightness) - vec3(m_minBrightness));
    //limit the chromaticity space into the +ve quadrant
    color.rgb = abs(color.rgb);

    //2) apply transfer functions on different channels.
    color.r = pow(color.r, m_exp_r);
    color.g = pow(color.g, m_exp_g);
    color.b = pow(color.b, m_exp_b);

    //3) apply a final scale factor, between 0.0 and 1.0.
    color.r = color.r * min(max(m_scale_r, 0.0), 1.0);
    color.b = color.b * min(max(m_scale_b, 0.0), 1.0);
    color.g = color.g * min(max(m_scale_g, 0.0), 1.0);

    //4) process the textures colors.
    fragColor = color;
}