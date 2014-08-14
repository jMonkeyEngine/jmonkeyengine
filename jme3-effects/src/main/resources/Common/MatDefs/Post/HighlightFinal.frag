uniform sampler2D m_Texture;
uniform sampler2D m_HighlightTex;
uniform float m_StepX;
uniform float m_StepY;
uniform bool m_Debug;
uniform vec4 m_HighlightColor;

varying vec2 texCoord;

void main(){
   vec4 colorRes = texture2D(m_Texture, texCoord);
   float highlight = texture2D(m_HighlightTex, texCoord).r;
   if (m_Size==1 && highlight < 0.1 &&
		(texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y + m_StepY)).r > 0.9)) {
			gl_FragColor = m_HighlightColor;

	} else if (m_Size==2 && highlight < 0.1 &&
		(texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y + m_StepY)).r > 0.9

		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y + m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y + m_StepY)).r > 0.9

		|| texture2D(m_HighlightTex, vec2(texCoord.x + 2*m_StepX, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + 2*m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + 2*m_StepX, texCoord.y + m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - 2*m_StepX, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - 2*m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - 2*m_StepX, texCoord.y + m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y + 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y + 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y + 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y - 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y - 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y - 2*m_StepY)).r > 0.9)) {
			gl_FragColor = m_HighlightColor;

	} else if (m_Size==3 && highlight < 0.1 &&
		(texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y + m_StepY)).r > 0.9

		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y + m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y + m_StepY)).r > 0.9

		|| texture2D(m_HighlightTex, vec2(texCoord.x + 2*m_StepX, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + 2*m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + 2*m_StepX, texCoord.y + m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - 2*m_StepX, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - 2*m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - 2*m_StepX, texCoord.y + m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y + 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y + 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y + 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y - 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y - 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y - 2*m_StepY)).r > 0.9

		|| texture2D(m_HighlightTex, vec2(texCoord.x + 3*m_StepX, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + 3*m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + 3*m_StepX, texCoord.y + m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - 3*m_StepX, texCoord.y - m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - 3*m_StepX, texCoord.y)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - 3*m_StepX, texCoord.y + m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y + 3*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y + 3*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y + 3*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - m_StepX, texCoord.y - 3*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x, texCoord.y - 3*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + m_StepX, texCoord.y - 3*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + 2*m_StepX, texCoord.y - 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x + 2*m_StepX, texCoord.y + 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - 2*m_StepX, texCoord.y - 2*m_StepY)).r > 0.9
		|| texture2D(m_HighlightTex, vec2(texCoord.x - 2*m_StepX, texCoord.y + 2*m_StepY)).r > 0.9
		)) {
			gl_FragColor = m_HighlightColor;

	} else {
		gl_FragColor = colorRes;
	}

   if (m_Debug) {
       gl_FragColor = vec4(highlight, highlight, highlight, 0);
   }
}

