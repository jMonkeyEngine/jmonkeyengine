uniform vec3 m_region1;
uniform vec3 m_region2;
uniform vec3 m_region3;
uniform vec3 m_region4;

uniform sampler2D m_region1ColorMap;
uniform sampler2D m_region2ColorMap;
uniform sampler2D m_region3ColorMap;
uniform sampler2D m_region4ColorMap;
uniform sampler2D m_slopeColorMap;

uniform float m_slopeTileFactor;
uniform float m_terrainSize;

varying vec3 normal;
varying vec4 position;

vec4 GenerateTerrainColor() {
    float height = position.y;
    vec4 p = position / m_terrainSize;

    vec3 blend = abs( normal );
    blend = (blend -0.2) * 0.7;
    blend = normalize(max(blend, 0.00001));      // Force weights to sum to 1.0 (very important!)
    float b = (blend.x + blend.y + blend.z);
    blend /= vec3(b, b, b);

    vec4 terrainColor = vec4(0.0, 0.0, 0.0, 1.0);

    float m_regionMin = 0.0;
    float m_regionMax = 0.0;
    float m_regionRange = 0.0;
    float m_regionWeight = 0.0;

 	vec4 slopeCol1 = texture2D(m_slopeColorMap, p.yz * m_slopeTileFactor);
 	vec4 slopeCol2 = texture2D(m_slopeColorMap, p.xy * m_slopeTileFactor);

    // Terrain m_region 1.
    m_regionMin = m_region1.x;
    m_regionMax = m_region1.y;
    m_regionRange = m_regionMax - m_regionMin;
    m_regionWeight = (m_regionRange - abs(height - m_regionMax)) / m_regionRange;
    m_regionWeight = max(0.0, m_regionWeight);
  	terrainColor += m_regionWeight * texture2D(m_region1ColorMap, p.xz * m_region1.z);

    // Terrain m_region 2.
    m_regionMin = m_region2.x;
    m_regionMax = m_region2.y;
    m_regionRange = m_regionMax - m_regionMin;
    m_regionWeight = (m_regionRange - abs(height - m_regionMax)) / m_regionRange;
    m_regionWeight = max(0.0, m_regionWeight);
    terrainColor += m_regionWeight * (texture2D(m_region2ColorMap, p.xz * m_region2.z));

    // Terrain m_region 3.
    m_regionMin = m_region3.x;
    m_regionMax = m_region3.y;
    m_regionRange = m_regionMax - m_regionMin;
    m_regionWeight = (m_regionRange - abs(height - m_regionMax)) / m_regionRange;
    m_regionWeight = max(0.0, m_regionWeight);
	terrainColor += m_regionWeight * texture2D(m_region3ColorMap, p.xz * m_region3.z);

    // Terrain m_region 4.
    m_regionMin = m_region4.x;
    m_regionMax = m_region4.y;
    m_regionRange = m_regionMax - m_regionMin;
    m_regionWeight = (m_regionRange - abs(height - m_regionMax)) / m_regionRange;
    m_regionWeight = max(0.0, m_regionWeight);
    terrainColor += m_regionWeight * texture2D(m_region4ColorMap, p.xz * m_region4.z);

    return (blend.y * terrainColor + blend.x * slopeCol1 + blend.z * slopeCol2);
}

void main() {
	vec4 color = GenerateTerrainColor();
    gl_FragColor = color;
}
