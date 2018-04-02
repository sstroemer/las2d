varying vec4 vColor;
varying vec2 vTexCoord;
uniform sampler2D u_texture;

void main( void )
{
    float dy = -200;
    float dx =   20;

    // y = kx + d
    // k = dy/dx
    float k = -dy/dx;
    // 5 = k*5 + d
    // d = 5-5k
    float d = 0.5 - 0.5*k;
    float eps = 0.1;

    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
    if (abs(vTexCoord.y - (vTexCoord.x*k + d)) < eps) {
    	gl_FragColor = vec4(0.3, 0.3, 1, 1.0);
    }

}