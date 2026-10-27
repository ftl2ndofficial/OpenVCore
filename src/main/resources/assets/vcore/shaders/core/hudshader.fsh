#version 150

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform float blend;
uniform float outline;
uniform float glow;
uniform float alpha;
uniform vec2 uSize;
uniform vec2 uLocation;
uniform float radius;

in vec2 texCoord;
out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, float r) {
    return length(max(abs(center) - size + r, 0.0)) - r;
}

vec3 createGradient(vec2 coords, vec3 c1, vec3 c2, vec3 c3, vec3 c4) {
    vec3 color = mix(mix(c1, c2, coords.y), mix(c3, c4, coords.y), coords.x);
    color += mix(0.0019607843, -0.0019607843, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return color;
}

void main() {
    vec2 halfSize = uSize * 0.5;
    float distance = roundedBoxSDF(gl_FragCoord.xy - uLocation - halfSize, halfSize, radius);
    float smoothedAlpha = (1.0 - smoothstep(-1.0, 2.0, distance)) * color1.a;
    if (smoothedAlpha <= 0.001) {
        discard;
    }
    float smoothedAlpha2 = (1.0 - smoothstep(-glow, 1.0, distance)) * color1.a;
    vec3 gradient = createGradient(texCoord, color1.rgb, color2.rgb, color3.rgb, color4.rgb);
    vec4 insideColor = vec4(gradient * alpha, smoothedAlpha);
    vec4 outlineColor = vec4(gradient, smoothedAlpha2);
    fragColor = mix(outlineColor, insideColor, clamp(-distance - outline, 0.0, 1.0));
}
