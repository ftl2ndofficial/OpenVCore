#version 150

uniform sampler2D InputSampler;
uniform vec2 InputResolution;
uniform float Quality;
uniform float Opacity;
uniform vec4 color1;
uniform vec2 uSize;
uniform vec2 uLocation;
uniform float radius;

in vec2 texCoord;
out vec4 fragColor;

const vec2 DIRS[12] = vec2[12](
    vec2(1.0, 0.0),
    vec2(0.8660254, 0.5),
    vec2(0.5, 0.8660254),
    vec2(0.0, 1.0),
    vec2(-0.5, 0.8660254),
    vec2(-0.8660254, 0.5),
    vec2(-1.0, 0.0),
    vec2(-0.8660254, -0.5),
    vec2(-0.5, -0.8660254),
    vec2(0.0, -1.0),
    vec2(0.5, -0.8660254),
    vec2(0.8660254, -0.5)
);

float roundedBoxSDF(vec2 center, vec2 size, float r) {
    return length(max(abs(center) - size + r, 0.0)) - r;
}

vec3 blurSample() {
    if (Quality <= 0.01) {
        return texture(InputSampler, gl_FragCoord.xy / InputResolution).rgb;
    }
    vec2 invRes = 1.0 / InputResolution;
    vec2 uv = gl_FragCoord.xy * invRes;
    vec2 rad = Quality * invRes;
    vec3 acc = texture(InputSampler, uv).rgb;
    for (int i = 0; i < 12; i++) {
        vec2 dir = DIRS[i] * rad;
        acc += texture(InputSampler, uv + dir * 0.3333333).rgb;
        acc += texture(InputSampler, uv + dir * 0.6666667).rgb;
        acc += texture(InputSampler, uv + dir).rgb;
    }
    return acc * (1.0 / 37.0);
}

void main() {
    vec2 halfSize = uSize * 0.5;
    float distance = roundedBoxSDF(gl_FragCoord.xy - uLocation - halfSize, halfSize, radius);
    float smoothedAlpha = 1.0 - smoothstep(-1.0, 1.0, distance);
    if (smoothedAlpha <= 0.001) {
        discard;
    }
    vec3 blurred = blurSample();
    vec3 finalColor = mix(blurred, color1.rgb, Opacity);
    fragColor = vec4(finalColor, smoothedAlpha);
}
