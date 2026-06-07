#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 source = texture(InSampler, texCoord);
    float luminance = max(max(source.r, source.g), source.b);
    float mask = smoothstep(0.05, 0.85, luminance) * source.a;
    fragColor = vec4(source.rgb * mask, mask);
}
