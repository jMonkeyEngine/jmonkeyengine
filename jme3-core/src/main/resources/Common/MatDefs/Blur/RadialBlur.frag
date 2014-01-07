uniform sampler2D m_Texture;
uniform float m_SampleDist;
uniform float m_SampleStrength;
uniform float m_Samples[10];
varying vec2 texCoord;

void main(void)
{
   // some sample positions
   //float samples[10] =   float[](-0.08,-0.05,-0.03,-0.02,-0.01,0.01,0.02,0.03,0.05,0.08);

    // 0.5,0.5 is the center of the screen
    // so substracting texCoord from it will result in
    // a vector pointing to the middle of the screen
    vec2 dir = 0.5 - texCoord;

    // calculate the distance to the center of the screen
    float dist = sqrt(dir.x*dir.x + dir.y*dir.y);

    // normalize the direction (reuse the distance)
    dir = dir/dist;

    // this is the original colour of this fragment
    // using only this would result in a nonblurred version
    vec4 colorRes = texture2D(m_Texture,texCoord);

    vec4 sum = colorRes;

    // take 10 additional blur samples in the direction towards
    // the center of the screen
    for (int i = 0; i < 10; i++)
    {
      sum += texture2D( m_Texture, texCoord + dir * m_Samples[i] * m_SampleDist );
    }

    // we have taken eleven samples
    sum *= 1.0/11.0;

    // weighten the blur effect with the distance to the
    // center of the screen ( further out is blurred more)
    float t = dist * m_SampleStrength;
    t = clamp( t ,0.0,1.0); //0 &lt;= t &lt;= 1

    //Blend the original color with the averaged pixels
    gl_FragColor =mix( colorRes, sum, t );
     
}