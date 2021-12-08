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

/*
 * Used by ContrastAdjustment.j3md to adjust the color channels.
 *
 * First, the input range is normalized to upper and lower limits.
 * Then a power law is applied, using the exponent for each channel.
 * Finally, the output value is scaled linearly, using the scaling factor for each channel.
 *
 * Supports GLSL100 GLSL110 GLSL120 GLSL130.
 */

//constant inputs from java source
uniform float m_redChannelExponent;
uniform float m_greenChannelExponent;
uniform float m_blueChannelExponent;

//final scale values
uniform float m_redChannelScale;
uniform float m_greenChannelScale;
uniform float m_blueChannelScale;

//input range
uniform float m_lowerLimit;
uniform float m_upperLimit;

//container for the input from post.vert
uniform sampler2D m_Texture;

//varying input from post.vert vertex shader
varying vec2 texCoord;

void main() {
    //get the color from a 2d sampler.
    vec4 color = texture2D(m_Texture, texCoord);

    //apply the color transfer function.

    //1) adjust the channels input range
    color.rgb = (color.rgb - vec3(m_lowerLimit)) / (vec3(m_upperLimit) - vec3(m_lowerLimit));

    // avoid negative levels
    color.r = max(color.r, 0.0);
    color.g = max(color.g, 0.0);
    color.b = max(color.b, 0.0);

    //2) apply transfer functions on different channels.
    color.r = pow(color.r, m_redChannelExponent);
    color.g = pow(color.g, m_greenChannelExponent);
    color.b = pow(color.b, m_blueChannelExponent);

    //3) scale the output levels
    color.r = color.r * m_redChannelScale;
    color.b = color.b * m_blueChannelScale;
    color.g = color.g * m_greenChannelScale;

    //4) process the textures colors.
    gl_FragColor = color;
}