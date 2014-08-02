/*
Draws only the highlighted shape
*/

#ifdef HAS_HIGHLIGHT
  uniform bool m_Highlighted;
#endif


void main(){
	#ifdef HAS_HIGHLIGHT
		gl_FragColor = m_Highlighted ? vec4(1.0, 1.0, 1.0, 0.0) : vec4(0.0);
	#else
		gl_FragColor = vec4(0.0);
	#endif
}