// ####################
// https://www.shadertoy.com/view/MdfBRX
// https://tympanus.net/codrops/2015/11/04/rain-water-effect-experiments/

#version 120

//attributes from vertex shader
varying vec4 vColor;
varying vec2 vTexCoord;

uniform sampler2D u_texture;
uniform sampler2D u_drop;
uniform sampler2D u_background;
uniform sampler2D u_shine;


uniform vec2 u_resolution           = vec2(800, 600);
uniform vec2 u_parallax             = vec2(10.0, 10.0);
uniform float u_parallaxFg          = 20.0;
uniform float u_parallaxBg          = 5.0;
uniform float u_textureRatio        = 800.0 / 600.0;
uniform bool u_renderShine          = false;
uniform bool u_renderShadow         = false;
uniform float u_minRefraction       = 256.0;
uniform float u_refractionDelta     = 256.0;
uniform float u_brightness          = 0.75;
uniform float u_alphaMultiply       = 20.0;
uniform float u_alphaSubtract       = 5.0;


vec4 blend(vec4 bg, vec4 fg);
vec2 pixel();
vec2 parallax(float v);
vec2 texCoord();
vec2 scaledTexCoord();
vec4 fgColor(float x, float y);


void main() {
    vec4 map = texture2D(u_drop, vTexCoord);
    float x = map.g;        // x mirrored
    float y = 1.0-map.r;    // y mirrored

/*
    vec2 bg_coords = vec2(x, y);
    vec4 bg_col = texture2D(u_background, bg_coords);
    gl_FragColor = bg_col;
*/
    float a = clamp(map.a*u_alphaMultiply - u_alphaSubtract, 0.0, 1.0);
    vec2 refraction = (vec2(x, y) - 0.5) * 2.0;
    vec2 refractionParallax = parallax(u_parallaxBg - u_parallaxFg);
    vec2 refractionPos = scaledTexCoord()
                            + (pixel() * refraction * (u_minRefraction + (map.b*u_refractionDelta)))
                            + refractionParallax;
    vec4 tex = texture2D(u_background, refractionPos);

    float maxShine=490.0;
    float minShine=maxShine*0.18;
    vec2 shinePos=vec2(0.5,0.5) + ((1.0/512.0)*refraction)* -(minShine+((maxShine-minShine)*map.b));
    vec4 shine=texture2D(u_shine,shinePos);
    tex=blend(tex,shine);

    vec4 fg = vec4(tex.rgb*u_brightness, a);

    float borderAlpha = fgColor(0.,0.-(map.b*6.0)).a;
    borderAlpha=borderAlpha*u_alphaMultiply-(u_alphaSubtract+0.5);
    borderAlpha=clamp(borderAlpha,0.,1.);
    borderAlpha*=0.2;
    vec4 border=vec4(0.,0.,0.,borderAlpha);
    fg=blend(border,fg);

    gl_FragColor = fg;
    gl_FragColor.a = texture2D(u_texture, vTexCoord).a;
    return;
/*
    vec4 bg = texture2D(u_textureBg, scaledTexCoord() + parallax(u_parallaxBg));

    vec4 cur = fgColor(0.0, 0.0);

    float d = cur.b; // "thickness"
    float x = cur.g;
    float y = cur.r;

    float a = clamp(cur.a*u_alphaMultiply - u_alphaSubtract, 0.0, 1.0);

    vec2 refraction = (vec2(x, y) - 0.5) * 2.0;
    vec2 refractionParallax = parallax(u_parallaxBg - u_parallaxFg);
    vec2 refractionPos = scaledTexCoord()
                            + (pixel() * refraction * (u_minRefraction + (d*u_refractionDelta)))
                            + refractionParallax;

    vec4 tex = texture2D(u_textureFg, refractionPos);


  if(u_renderShine){
    float maxShine=490.0;
    float minShine=maxShine*0.18;
    vec2 shinePos=vec2(0.5,0.5) + ((1.0/512.0)*refraction)* -(minShine+((maxShine-minShine)*d));
    vec4 shine=texture2D(u_textureShine,shinePos);
    tex=blend(tex,shine);
  }

  vec4 fg = vec4(tex.rgb*u_brightness, a);

  if(u_renderShadow){
    float borderAlpha = fgColor(0.,0.-(d*6.0)).a;
    borderAlpha=borderAlpha*u_alphaMultiply-(u_alphaSubtract+0.5);
    borderAlpha=clamp(borderAlpha,0.,1.);
    borderAlpha*=0.2;
    vec4 border=vec4(0.,0.,0.,borderAlpha);
    fg=blend(border,fg);
  }


    //gl_FragColor = vColor * texture2D(u_texture, vTexCoord);
    gl_FragColor = bg;//blend(bg, fg);

    */
}

vec4 blend(vec4 bg, vec4 fg) {
    vec3 bgm = bg.rgb*bg.a;
    vec3 fgm = fg.rgb*fg.a;

    float ia = 1.0 - fg.a;
    float a  = (fg.a + bg.a * ia);

    vec3 rgb = vec3(0.0, 0.0, 0.0);
    if (a != 0.0) {
        rgb = (fgm + bgm * ia) / a;
    }

    return vec4(rgb, a);
}

vec2 pixel() {
    return vec2(1.0, 1.0) / u_resolution;
}

vec2 parallax(float v) {
    return u_parallax * pixel() * v;
}

vec2 texCoord() {
    return vec2(gl_FragCoord.x, u_resolution.y - gl_FragCoord.y) / u_resolution;
}

vec2 scaledTexCoord() {
  float ratio=u_resolution.x/u_resolution.y;
  vec2 scale=vec2(1.0,1.0);
  vec2 offset=vec2(0.0,0.0);
  float ratioDelta=ratio-u_textureRatio;
  if(ratioDelta>=0.0){
    scale.y=(1.0+ratioDelta);
    offset.y=ratioDelta/2.0;
  }else{
    scale.x=(1.0-ratioDelta);
    offset.x=-ratioDelta/2.0;
  }
  //return texCoord();
  return (texCoord()+offset)/scale;
}

vec4 fgColor(float x, float y) {
    float p2 = u_parallaxFg * 2.0;
    vec2 scale = vec2(
        (u_resolution.x + p2) / u_resolution.x,
        (u_resolution.y + p2) / u_resolution.y
    );

    vec2 scaledTexCoord = texCoord() / scale;
    vec2 offset = vec2(
        (1.0 - (1.0/scale.x)) / 2.0,
        (1.0 - (1.0/scale.y)) / 2.0
    );

    return texture2D(
        u_background,
        (scaledTexCoord+offset) + (pixel()*vec2(x,y)) + parallax(u_parallaxFg)
    );
}
