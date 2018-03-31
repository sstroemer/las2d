//attributes from vertex shader
varying vec4 vColor;
varying vec2 vTexCoord;

//our texture samplers
uniform sampler2D u_texture;   //diffuse map
uniform sampler2D u_normals;   //normal map

//values used for shading algorithm...
uniform vec2 Resolution;      //resolution of screen
uniform vec3 LightPos;        //light position, normalized
uniform vec4 LightColor;      //light RGBA -- alpha is intensity
uniform vec4 AmbientColor;    //ambient RGBA -- alpha is intensity 
uniform vec3 Falloff;         //attenuation coefficients
uniform int spec_n;

#define M_PI 3.1415926535897932384626433832795

void main() {
	//RGBA of our diffuse color
	vec4 DiffuseColor = texture2D(u_texture, vTexCoord);
	
	//RGB of our normal map
	vec3 NormalMap = texture2D(u_normals, vTexCoord).rgb;
	
	//The delta position of light
	vec3 LightDir = vec3(LightPos.xy - (gl_FragCoord.xy / Resolution.xy), LightPos.z);

	//Correct for aspect ratio
	LightDir.x *= Resolution.x / Resolution.y;
	
	//Determine distance (used for attenuation) BEFORE we normalize our LightDir
	float D = length(LightDir);
	
	//normalize our vectors
	vec3 N = normalize(NormalMap * 2.0 - 1.0);
	vec3 L = normalize(LightDir);
	
	//Pre-multiply light color with intensity
	//Then perform "N dot L" to determine our diffuse term
	vec3 Diffuse = (LightColor.rgb * LightColor.a) * max(dot(N, L), 0.0);

	//pre-multiply ambient color with intensity
	vec3 Ambient = AmbientColor.rgb * AmbientColor.a;
	
	//calculate attenuation
	float Attenuation = 1.0 / ( Falloff.x + (Falloff.y*D) + (Falloff.z*D*D) );

    bool fastPhong = false;

    // ############# BLINN PHONG ###############
    // todo: see https://en.wikipedia.org/wiki/Blinn%E2%80%93Phong_shading_model for full parameters!!!
    vec3 Specular;
    if (fastPhong) {
   	    vec3 ViewDir  = vec3(vec2(0.5, 0.5) - (gl_FragCoord.xy / Resolution.xy), 0.1);
   	    vec3 V = normalize(ViewDir);
   	    vec3 H = normalize(V + L);
   	    Specular = (LightColor.rgb * LightColor.a) * pow(max(dot(N, H), 0.0), spec_n);
   	}
   	else {
   	    vec3 ViewDir  = vec3(vec2(0.5, 0.5) - (gl_FragCoord.xy / Resolution.xy), 0.1);
        vec3 V = -normalize(ViewDir);
   	    vec3 R = L - 2.0 * dot(L, N) * N;
   	    Specular = (LightColor.rgb * LightColor.a) * (spec_n+2.0)/(2.0*M_PI) * pow(max(dot(R, V), 0.0), spec_n);
   	}
    // ############# BLINN PHONG ###############

    // todo: try https://www.codeandweb.com/spriteilluminator
    // todo: try SpriteLamp

	//the calculation which brings it all together
	vec3 Intensity = Ambient + Diffuse * Attenuation + Specular;// * Attenuation;
	vec3 FinalColor = DiffuseColor.rgb * Intensity;
	gl_FragColor = vColor * vec4(FinalColor, DiffuseColor.a);


	// ANMERKUNG BASIC PHONG:
	// https://de.wikipedia.org/wiki/Phong-Beleuchtungsmodell
	// Ispec = (n+2)/(2pi)*(R dot V)^n
	// R reflexionsrichtung
	// V blickrichtung betrachter
	// rau: n < 32
	// glatt n > 32
	// basic blinn-phong: https://de.wikipedia.org/wiki/Blinn-Beleuchtungsmodell
	// V Punkt zu Betrachter
	// L Punkt zu Lichtquelle
	// phi = N dot (V+L)/(norm(V+L))
}