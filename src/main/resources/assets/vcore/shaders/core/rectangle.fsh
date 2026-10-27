#version 150

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;

uniform vec2 uSize;
uniform vec2 uLocation;
uniform float radius;

in vec2 texCoord;
out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, float r) {
    return length(max(abs(center) - size + r, 0.0)) - r;
}

vec4 createGradient(vec2 coords, vec4 c1, vec4 c2, vec4 c3, vec4 c4) {
    vec4 color = mix(mix(c1, c2, coords.y), mix(c3, c4, coords.y), coords.x);
    color += mix(0.0019607843, -0.0019607843, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return color;
}

void main() {
    vec2 halfSize = uSize * 0.5;
    float distance = roundedBoxSDF(gl_FragCoord.xy - uLocation - halfSize, halfSize, radius);
    float smoothedAlpha = 1.0 - smoothstep(0.0, 2.0, distance);
    if (smoothedAlpha <= 0.001) {
        discard;
    }
    vec4 color = createGradient(texCoord, color1, color2, color3, color4);
    fragColor = vec4(color.rgb, color.a * smoothedAlpha);
}
