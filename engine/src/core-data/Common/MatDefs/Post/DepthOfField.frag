uniform sampler2D m_Texture;
uniform sampler2D m_DepthTexture;
varying vec2 texCoord;

uniform float m_FocusRange;
uniform float m_FocusDistance;
uniform float m_XScale;
uniform float m_YScale;

vec2 m_NearFar = vec2( 0.1, 1000.0 );

void main() {

    vec4 texVal = texture2D( m_Texture, texCoord );

    float zBuffer = texture2D( m_DepthTexture, texCoord ).r;

    //
    // z_buffer_value = a + b / z;
    //
    // Where:
    //  a = zFar / ( zFar - zNear )
    //  b = zFar * zNear / ( zNear - zFar )
    //  z = distance from the eye to the object
    //
    // Which means:
    // zb - a = b / z;
    // z * (zb - a) = b
    // z = b / (zb - a)
    //
    float a = m_NearFar.y / (m_NearFar.y - m_NearFar.x);
    float b = m_NearFar.y * m_NearFar.x / (m_NearFar.x - m_NearFar.y);
    float z = b / (zBuffer - a);

    // Above could be the same for any depth-based filter

    // We want to be purely focused right at
    // m_FocusDistance and be purely unfocused
    // at +/- m_FocusRange to either side of that.
    float unfocus = min( 1.0, abs( z - m_FocusDistance ) / m_FocusRange );

    if( unfocus < 0.2 ) {
        // If we are mostly in focus then don't bother with the
        // convolution filter
        gl_FragColor = texVal;
    } else {
    // Perform a wide convolution filter and we scatter it
    // a bit to avoid some texture look-ups.  Instead of
    // a full 5x5 (25-1 lookups) we'll skip every other one
    // to only perform 12.
    // 1  0  1  0  1
    // 0  1  0  1  0
    // 1  0  x  0  1
    // 0  1  0  1  0
    // 1  0  1  0  1
    //
    // You can get away with 8 just around the outside but
    // it looks more jittery to me.

    vec4 sum = vec4(0.0);

    float x = texCoord.x;
    float y = texCoord.y;

    float xScale = m_XScale;
    float yScale = m_YScale;

    // In order from lower left to right, depending on how you look at it
    sum += texture2D( m_Texture, vec2(x - 2.0 * xScale, y - 2.0 * yScale) );
    sum += texture2D( m_Texture, vec2(x - 0.0 * xScale, y - 2.0 * yScale) );
    sum += texture2D( m_Texture, vec2(x + 2.0 * xScale, y - 2.0 * yScale) );
    sum += texture2D( m_Texture, vec2(x - 1.0 * xScale, y - 1.0 * yScale) );
    sum += texture2D( m_Texture, vec2(x + 1.0 * xScale, y - 1.0 * yScale) );
    sum += texture2D( m_Texture, vec2(x - 2.0 * xScale, y - 0.0 * yScale) );
    sum += texture2D( m_Texture, vec2(x + 2.0 * xScale, y - 0.0 * yScale) );
    sum += texture2D( m_Texture, vec2(x - 1.0 * xScale, y + 1.0 * yScale) );
    sum += texture2D( m_Texture, vec2(x + 1.0 * xScale, y + 1.0 * yScale) );
    sum += texture2D( m_Texture, vec2(x - 2.0 * xScale, y + 2.0 * yScale) );
    sum += texture2D( m_Texture, vec2(x - 0.0 * xScale, y + 2.0 * yScale) );
    sum += texture2D( m_Texture, vec2(x + 2.0 * xScale, y + 2.0 * yScale) );

    sum = sum / 12.0;

    gl_FragColor = mix( texVal, sum, unfocus );

    // I used this for debugging the range
   // gl_FragColor.r = unfocus;
}
}