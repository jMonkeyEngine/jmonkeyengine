void main(){

    vec4 dark = inColor * 0.3;
    vec4 bright = min(inColor * 4.0, 1.0);
    normal = normalize(normal);
    vec3 dir = vec3(0,0,1);
    float factor = dot(dir, normal);
    outColor = mix(dark, bright, factor);
}
