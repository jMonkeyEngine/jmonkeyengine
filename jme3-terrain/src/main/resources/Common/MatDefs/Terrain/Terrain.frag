uniform sampler2D m_Alpha;
uniform sampler2D m_Tex1;
uniform sampler2D m_Tex2;
uniform sampler2D m_Tex3;
uniform float m_Tex1Scale;
uniform float m_Tex2Scale;
uniform float m_Tex3Scale;

varying vec2 texCoord;

#ifdef TRI_PLANAR_MAPPING
  varying vec4 vVertex;
  varying vec3 vNormal;
#endif

void main(void)
{

    // get the alpha value at this 2D texture coord
    vec4 alpha   = texture2D( m_Alpha, texCoord.xy );

#ifdef TRI_PLANAR_MAPPING
    // tri-planar texture bending factor for this fragment's normal
    vec3 blending = abs( vNormal );
    blending = (blending -0.2) * 0.7;
    blending = normalize(max(blending, 0.00001));      // Force weights to sum to 1.0 (very important!)
    float b = (blending.x + blending.y + blending.z);
    blending /= vec3(b, b, b);

    // texture coords
    vec4 coords = vVertex;

    vec4 col1 = texture2D( m_Tex1, coords.yz * m_Tex1Scale );
    vec4 col2 = texture2D( m_Tex1, coords.xz * m_Tex1Scale );
    vec4 col3 = texture2D( m_Tex1, coords.xy * m_Tex1Scale );
    // blend the results of the 3 planar projections.
    vec4 tex1 = col1 * blending.x + col2 * blending.y + col3 * blending.z;

    col1 = texture2D( m_Tex2, coords.yz * m_Tex2Scale );
    col2 = texture2D( m_Tex2, coords.xz * m_Tex2Scale );
    col3 = texture2D( m_Tex2, coords.xy * m_Tex2Scale );
    // blend the results of the 3 planar projections.
    vec4 tex2 = col1 * blending.x + col2 * blending.y + col3 * blending.z;

    col1 = texture2D( m_Tex3, coords.yz * m_Tex3Scale );
    col2 = texture2D( m_Tex3, coords.xz * m_Tex3Scale );
    col3 = texture2D( m_Tex3, coords.xy * m_Tex3Scale );
    // blend the results of the 3 planar projections.
    vec4 tex3 = col1 * blending.x + col2 * blending.y + col3 * blending.z;

#else
	vec4 tex1    = texture2D( m_Tex1, texCoord.xy * m_Tex1Scale ); // Tile
	vec4 tex2    = texture2D( m_Tex2, texCoord.xy * m_Tex2Scale ); // Tile
	vec4 tex3    = texture2D( m_Tex3, texCoord.xy * m_Tex3Scale ); // Tile
	
#endif

    vec4 outColor = tex1 * alpha.r; // Red channel
	outColor = mix( outColor, tex2, alpha.g ); // Green channel
	outColor = mix( outColor, tex3, alpha.b ); // Blue channel
	gl_FragColor = outColor;
}

