#version 150

uniform mat4 invViewMat;
uniform mat4 invProjMat;
uniform vec3 center;
uniform float radius;
uniform float ringWidth;
uniform float scanlineStrength;
uniform float use3dDistance;
uniform float distanceMode;
uniform float useBounds;
uniform vec3 boundMin;
uniform vec3 boundMax;
uniform sampler2D depthTex;

in vec2 texCoord0;

out vec4 fragColor;

const float sharpness = 10.0;
const vec4 outerColor = vec4(0.80, 1.00, 0.90, 1.0);
const vec4 midColor = vec4(0.40, 0.50, 0.70, 1.0);
const vec4 innerColor = vec4(0.10, 0.40, 0.90, 1.0);
const vec4 scanlineColor = vec4(0.60, 1.00, 0.20, 1.0);

float scanlines() {
    return sin(gl_FragCoord.y) * 0.5 + 0.5;
}

vec3 worldPos(float depth) {
    float z = depth * 2.0 - 1.0;
    vec4 clipSpacePos = vec4(texCoord0 * 2.0 - 1.0, z, 1.0);
    vec4 viewSpacePos = invProjMat * clipSpacePos;
    viewSpacePos /= viewSpacePos.w;
    vec4 worldSpacePos = invViewMat * viewSpacePos;
    return worldSpacePos.xyz;
}

void main() {
    vec4 color = vec4(0.0);
    float depth = texture(depthTex, texCoord0).r;
    vec3 wp = worldPos(depth);
    if (useBounds > 0.5) {
        if (wp.x < boundMin.x || wp.y < boundMin.y || wp.z < boundMin.z
                || wp.x > boundMax.x || wp.y > boundMax.y || wp.z > boundMax.z) {
            fragColor = vec4(0.0);
            return;
        }
    }
    float dist2d = distance(wp.xz, center.xz);
    float dist3d = distance(wp, center);
    vec3 d3 = abs(wp - center);
    float distVoxel = max(max(d3.x, d3.y), d3.z);
    float mode = distanceMode;
    float dist = dist2d;
    if (mode > 1.5) {
        dist = distVoxel;
    } else if (mode > 0.5) {
        dist = dist3d;
    } else {
        dist = mix(dist2d, dist3d, clamp(use3dDistance, 0.0, 1.0));
    }
    float width = max(1.0, ringWidth);

    if (depth < 0.9999) {
        if (mode > 1.5) {
            if (dist <= radius) {
                float edge = 1.0 - smoothstep(max(0.0, radius - width), radius, dist);
                float fill = 1.0 - smoothstep(0.0, radius, dist);
                vec3 base = mix(innerColor.rgb, midColor.rgb, 0.45 + 0.55 * fill);
                vec3 rim = mix(midColor.rgb, outerColor.rgb, pow(edge, 1.6));
                color.rgb = mix(base, rim, edge * 0.9);
                color.rgb += scanlines() * scanlineColor.rgb * (0.18 + edge * 0.5) * scanlineStrength;
                color.a = clamp(0.18 + fill * 0.35 + edge * 0.45, 0.0, 1.0);
            }
        } else if (dist < radius && dist > radius - width) {
            float diff = 1.0 - (radius - dist) / width;
            vec4 edge = mix(midColor, outerColor, pow(diff, sharpness));
            color = mix(innerColor, edge, diff);
            color.rgb += scanlines() * scanlineColor.rgb * diff * scanlineStrength;
            color *= diff;
            color.a = clamp(color.a, 0.0, 1.0);
        }
    }

    fragColor = color;
}
