#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float ScanLimitY;
uniform float ScanTime;

in vec2 texCoord0;
in vec4 vertexColor;
in float localY;

out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.05) discard;
    if (localY > ScanLimitY) discard;

    float edge = smoothstep(ScanLimitY - 0.44, ScanLimitY, localY);
    float band = 1.0 - smoothstep(0.0, 0.16, abs(localY - ScanLimitY));
    float scanline = 0.5 + 0.5 * sin((localY + ScanTime * 0.55) * 38.0);
    float n = hash12(texCoord0 * 300.0 + vec2(ScanTime * 8.0, localY * 7.0));
    vec3 baseTint = vec3(0.10, 0.70, 1.00);
    vec3 edgeTint = vec3(0.85, 1.00, 0.95);
    vec3 scanTint = mix(baseTint, edgeTint, edge);
    scanTint += vec3(0.25, 0.95, 0.75) * band * 0.55;
    scanTint += vec3(0.10, 0.28, 0.22) * scanline * 0.35;
    scanTint += vec3(0.07, 0.18, 0.20) * n * 0.55;

    vec3 color = tex.rgb * vertexColor.rgb;
    color = mix(color, color * scanTint, 0.92);
    float alpha = tex.a * vertexColor.a * ColorModulator.a * (0.78 + 0.34 * edge + 0.52 * band + 0.12 * n);
    fragColor = vec4(color * ColorModulator.rgb, clamp(alpha, 0.0, 1.0));
}
