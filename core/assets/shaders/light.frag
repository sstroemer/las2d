/********************************
* DEFINES                       *
********************************/
#define M_PI (3.1415926535897932384626433832795)
#define FAST_PHONG_CORR     (4.0)
#define DIFFUSE_CUTOFF      (0.01)

/********************************
* ATTRIBUTES                    *
********************************/
// inputs from vertex shader
varying vec4 vColor;
varying vec2 vTexCoord;

// texture inputs
uniform sampler2D u_texture;   //diffuse map
uniform sampler2D u_normals;   //normal map

/********************************
* INPUT PARAMETERS              *
********************************/
uniform vec2 inResolution;      // resolution of screen
uniform vec3 inLightPos;        // light position, normalized
uniform vec4 inLightColor;      // light RGBA
uniform vec4 inAmbientColor;    // ambient RGBA
uniform vec3 inFalloff;         // attenuation coefficients (quadratic equation)
uniform int inSpecularExponent; // smoothness factor
// todo: separate settings for sepcular component are mssing

/********************************
* FUNCTIONS                     *
********************************/
float diffuse(vec3 N, vec3 L);
float phong(vec3 N, vec3 L, bool fastMode);
// todo: implement emissive light


void main() {
    /********************************
    * PRE - CALULATIONS             *
    ********************************/
	vec4 diffuseColor = texture2D(u_texture, vTexCoord);
	vec3 normalMap = texture2D(u_normals, vTexCoord).rgb;

	vec3 lightDir = vec3(inLightPos.xy - (gl_FragCoord.xy / inResolution.xy), inLightPos.z);
	lightDir.x *= inResolution.x / inResolution.y;

	float D = length(lightDir);
	vec3  N = normalize(normalMap * 2.0 - 1.0);
	vec3  L = normalize(lightDir);

    /********************************
    * INTENSITIES                   *
    ********************************/
	float attenuation = 1.0 / (inFalloff.x + (inFalloff.y * D) + (inFalloff.z * D*D));

	vec3 ambient = inAmbientColor.rgb * inAmbientColor.a;

	float diffuseIntensity = diffuse(N, L);
	vec3 diffuse  = (inLightColor.rgb * inLightColor.a) * diffuseIntensity;
	vec3 specular = vec3(0, 0, 0);

	if (diffuseIntensity > DIFFUSE_CUTOFF) {
	    specular = (inLightColor.rgb * inLightColor.a) * phong(N, L, true);
	}

    /********************************
    * MERGE EVERYTHING              *
    ********************************/
	vec3 intensity  = ambient + diffuse * attenuation + specular * attenuation;
	vec3 finalColor = diffuseColor.rgb * intensity;
	gl_FragColor    = vColor * vec4(finalColor, diffuseColor.a);
}

/**
 * @param N         surface normal of fragment (normalized!)
 * @param L         direction from fragment to light (normalized!)
 * @return          intensity of diffuse component (excluding attenuation)
 */
float diffuse(vec3 N, vec3 L) {
    return max(dot(N, L), 0.0);
}

/**
 * @param N         surface normal of fragment (normalized!)
 * @param L         direction from fragment to light (normalized!)
 * @param fastMode  use blinn-phong approximation
 * @return          intensity of specular component (excluding attenuation)
 */
float phong(vec3 N, vec3 L, bool fastMode) {
    if (fastMode) {
        // use blinn-phong approximation
        vec3 V = vec3(0, 0, 1);
        vec3 H = normalize(V + L);
        return pow(max(dot(N, H), 0.0), float(inSpecularExponent) * FAST_PHONG_CORR);
    }
    else {
        vec3 V = vec3(0, 0, -1);
        vec3 R = normalize(L - 2.0 * dot(L, N) * N);
        return (float(inSpecularExponent)+2.0)/(2.0*M_PI) * pow(max(dot(R, V), 0.0), float(inSpecularExponent));
    }
}