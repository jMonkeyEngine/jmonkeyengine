uniform sampler2D m_Texture;

varying vec2 UVred;
varying vec2 UVgreen;
varying vec2 UVblue;

void main() {
    // performance & FOV experiment by removing bounds check
    //float fBoundsCheck = ( (dot( vec2( lessThan( UVgreen.xy, vec2(0.05, 0.05)) ), vec2(1.0, 1.0))+dot( vec2( greaterThan( UVgreen.xy, vec2( 0.95, 0.95)) ), vec2(1.0, 1.0))) );
    //
    //if( fBoundsCheck > 1.0 ) {
    //  gl_FragColor = vec4( 0.0, 0.0, 0.0, 1.0 );
    //} else {

        float red = texture2D(m_Texture, UVred).x;
        float green = texture2D(m_Texture, UVgreen).y;
        float blue = texture2D(m_Texture, UVblue).z;
        gl_FragColor = vec4( red, green, blue, 1.0 );

    //}
}
