#version 330

uniform sampler2D MainSampler;
uniform sampler2D BloomSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 base = texture(MainSampler, texCoord);
    vec4 bloom = texture(BloomSampler, texCoord);
    vec3 screened = 1.0 - (1.0 - base.rgb) * (1.0 - clamp(bloom.rgb * 1.35, 0.0, 1.0));
    fragColor = vec4(mix(base.rgb, screened, clamp(bloom.a + 0.72, 0.0, 1.0)), max(base.a, bloom.a));
}
