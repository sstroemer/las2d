package com.las2d;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.Random;

import static com.badlogic.gdx.math.MathUtils.clamp;

public class GameLightning extends ApplicationAdapter implements ApplicationListener {
	SpriteBatch batch;
	Texture wall, wall_norm, player, player_norm, light;

    ShaderProgram s_rain, s_light;

    //our constants...
    public static final float DEFAULT_LIGHT_Z = 0.075f;
    public static final float AMBIENT_INTENSITY = 0.4f;
    public static float LIGHT_INTENSITY = 1.0f;

    public static final Vector3 LIGHT_POS = new Vector3(0f,0f,DEFAULT_LIGHT_Z);

    //Light RGB and intensity (alpha)
//    public static final Vector3 LIGHT_COLOR = new Vector3(1f, 0.6f, 0.3f);
    public static final Vector3 LIGHT_COLOR = new Vector3(0.8f, 0.8f, 0.8f);

    //Ambient RGB and intensity (alpha)
    public static final Vector3 AMBIENT_COLOR = new Vector3(0.9f, 0.9f, 0.9f);

    //Attenuation coefficients for light falloff
    public static final Vector3 FALLOFF = new Vector3(.2f, 1f, 2f);
    private OrthographicCamera cam;
    private int spec_n = 5;
    private boolean asMixMode = false;
    private ShaderProgram s;

    FrameBuffer fbo;
    private Texture shine;

    @Override
	public void create () {
        wall       = new Texture(Gdx.files.internal("wall.png"));
        wall_norm  = new Texture(Gdx.files.internal("wall_normal.png"));
        player     = new Texture(Gdx.files.internal("player.png"));
        player_norm= new Texture(Gdx.files.internal("player_normal.png"));
        light       = new Texture(Gdx.files.internal("light.png"));

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, 800, 600, false);

        ShaderProgram.pedantic = false;
        s_rain = new ShaderProgram(Gdx.files.internal("shaders/passthrough.vert").readString(), Gdx.files.internal("shaders/rain.frag").readString());
        s_light = new ShaderProgram(Gdx.files.internal("shaders/passthrough.vert").readString(), Gdx.files.internal("shaders/light.frag").readString());

        System.out.println(s_rain.getLog());
        System.out.println(s_light.getLog());

        //setup default uniforms
        s_light.begin();

        //our normal map
        s_light.setUniformi("u_normals", 1); //GL_TEXTURE1

        //light/ambient colors
        //LibGDX doesn't have Vector4 class at the moment, so we pass them individually...
        s_light.setUniformf("inLightColor", LIGHT_COLOR.x, LIGHT_COLOR.y, LIGHT_COLOR.z, LIGHT_INTENSITY);
        s_light.setUniformf("inAmbientColor", AMBIENT_COLOR.x, AMBIENT_COLOR.y, AMBIENT_COLOR.z, AMBIENT_INTENSITY);
        s_light.setUniformf("inFalloff", FALLOFF);

        //LibGDX likes us to end the shader program
        s_light.end();

        batch = new SpriteBatch();

        cam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.setToOrtho(false);


        //handle mouse wheel
        Gdx.input.setInputProcessor(new InputAdapter() {
            public boolean scrolled(int delta) {
                //LibGDX mouse wheel is inverted compared to lwjgl-basics
                LIGHT_POS.z = Math.max(0f, LIGHT_POS.z - (delta * 0.005f));
                System.out.println("New light Z: "+LIGHT_POS.z);
                return true;
            }
        });
    }


    @Override
    public void resize(int width, int height) {
        cam.setToOrtho(false, width, height);
        batch.setProjectionMatrix(cam.combined);

        s_light.begin();
        s_light.setUniformf("inResolution", width, height);
        s_light.end();
    }

	// todo: IMPORTANT. READ THIS. COMIC STYLE SHADER.
    // http://prideout.net/blog/?p=22

    // todo: IMPORTANT. READ THIS. NORMAL MAP SHADING.
    // https://github.com/mattdesl/Lwjgl-basics/wiki/ShaderLesson6

    // general stuff:
    //      https://www.redblobgames.com/articles/visibility/
    //      http://www.wholehog-games.com/devblog/2013/06/07/lighting-in-a-2d-game/
    //      http://kingunderthemounta.in/dev-blog-2-let-there-be-light/

    Random torchRand = new Random();
    float dy = 800;
	@Override
	public void render () {
		if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
		    System.exit(0);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            asMixMode = !asMixMode;
        }

        //update light position, normalized to screen resolution
        float x = Gdx.input.getX() / (float)Gdx.graphics.getWidth();
        float y = 1.0f - Gdx.input.getY() / (float)Gdx.graphics.getHeight();

        LIGHT_POS.x = x + (torchRand.nextFloat() - .5f) * 0.01f;
        LIGHT_POS.y = y + (torchRand.nextFloat() - .5f) * 0.01f;

        //LIGHT_INTENSITY = (float)clamp(LIGHT_INTENSITY+(torchRand.nextFloat()-.5)/10,0.5, 1);

        //send a Vector4f to GLSL
        s_light.begin();
        //LIGHT_POS.x = 200 / (float)Gdx.graphics.getWidth();
        //LIGHT_POS.y = 300 / (float)Gdx.graphics.getHeight();
        //LIGHT_POS.z = 0.01f;
        s_light.setUniformf("inLightPos", LIGHT_POS);
        s_light.setUniformf("inLightColor", LIGHT_COLOR.x, LIGHT_COLOR.y, LIGHT_COLOR.z, LIGHT_INTENSITY);
        s_light.setUniformi("inSpecularExponent", spec_n);
        s_light.end();

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
            batch.setShader(null);
            batch.draw(light, 180, 290, 40, 20);
        batch.end();
        batch.begin();
            batch.setShader(s_light);
            renderFloor();
        batch.end();
        batch.begin();
            player_norm.bind(1);
            player.bind(0);
            batch.draw(player, 180, 120, 40, 40);
        batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
	}

	// -----------------------------------------------------------

    // ideas for textures:
    // https://www.google.at/imgres?imgurl=https%3A%2F%2Fcmkt-image-prd.global.ssl.fastly.net%2F0.1.0%2Fps%2F25208%2F1160%2F772%2Fm1%2Ffpnw%2Fwm0%2Fmetal-beam-platform-.jpg%3F1372614949%26s%3Ddf8851901039f752ad35623d44de875d&imgrefurl=https%3A%2F%2Fcreativemarket.com%2FGraphics4Games%2F7998-Platform-metal-beam-asset&docid=vF-DqDJCz1nD7M&tbnid=H3WRjL0jBJQ2TM%3A&vet=10ahUKEwjKhKPaiqHaAhVCPVAKHQ2VBpQQMwhBKAQwBA..i&w=1160&h=772&bih=939&biw=1278&q=platformer%20metal&ved=0ahUKEwjKhKPaiqHaAhVCPVAKHQ2VBpQQMwhBKAQwBA&iact=mrc&uact=8
    // https://www.google.at/imgres?imgurl=https%3A%2F%2Fi.pinimg.com%2F236x%2F2b%2Fc7%2Fc6%2F2bc7c6e44813f1e79b8265062f9736d6--ds-games-game-design.jpg&imgrefurl=https%3A%2F%2Fwww.pinterest.com%2Fpin%2F549931804471130009%2F&docid=TP6iBzjdOoLtpM&tbnid=y7M9D6LPxkSTNM%3A&vet=10ahUKEwjfpd_hiqHaAhWBL1AKHTPYBp0QMwg9KAAwAA..i&w=236&h=236&itg=1&bih=939&biw=1278&q=platformer%20metal%20tile&ved=0ahUKEwjfpd_hiqHaAhWBL1AKHTPYBp0QMwg9KAAwAA&iact=mrc&uact=8
    // https://www.google.at/imgres?imgurl=https%3A%2F%2Fopengameart.org%2Fsites%2Fdefault%2Ffiles%2FMetal_tiles1_extra_4_colors.png&imgrefurl=https%3A%2F%2Fopengameart.org%2Fcontent%2Fcolored-metal-tiles-1b&docid=yTeOWS4Y_ZeV4M&tbnid=zWp6oVOvHJL59M%3A&vet=10ahUKEwjfpd_hiqHaAhWBL1AKHTPYBp0QMwhAKAMwAw..i&w=1024&h=768&bih=939&biw=1278&q=platformer%20metal%20tile&ved=0ahUKEwjfpd_hiqHaAhWBL1AKHTPYBp0QMwhAKAMwAw&iact=mrc&uact=8
    // https://www.google.at/imgres?imgurl=https%3A%2F%2Fi.pinimg.com%2F736x%2F84%2Fdf%2F7e%2F84df7ea05f993d765f31b3a9830ada4d--graphic-design-illustration-design-illustrations.jpg&imgrefurl=https%3A%2F%2Fwww.pinterest.com%2Fkristenraerocks%2Findie-team-assets%2F&docid=Ll7g6kCcooEtqM&tbnid=RABvOEFsMv1ZqM%3A&vet=10ahUKEwjfpd_hiqHaAhWBL1AKHTPYBp0QMwhDKAYwBg..i&w=590&h=500&bih=939&biw=1278&q=platformer%20metal%20tile&ved=0ahUKEwjfpd_hiqHaAhWBL1AKHTPYBp0QMwhDKAYwBg&iact=mrc&uact=8
    // https://www.google.at/imgres?imgurl=https%3A%2F%2Fopengameart.org%2Fsites%2Fdefault%2Ffiles%2Fpreview_145.png&imgrefurl=https%3A%2F%2Fopengameart.org%2Fcontent%2Fplatformer-art-request-pack&docid=BC0uPIoRW0r6SM&tbnid=MiNPxRO7IXlOJM%3A&vet=10ahUKEwjfpd_hiqHaAhWBL1AKHTPYBp0QMwhcKBUwFQ..i&w=800&h=480&bih=939&biw=1278&q=platformer%20metal%20tile&ved=0ahUKEwjfpd_hiqHaAhWBL1AKHTPYBp0QMwhcKBUwFQ&iact=mrc&uact=8
    // https://www.google.at/imgres?imgurl=https%3A%2F%2Fi.pinimg.com%2Foriginals%2F14%2F60%2Feb%2F1460eb8bbaa6b64119142715683b6cff.jpg&imgrefurl=https%3A%2F%2Fwww.pinterest.com%2Fpin%2F409898003576781453%2F&docid=J7GwBZWUiJuLuM&tbnid=O-ikNEuVeP2ttM%3A&vet=10ahUKEwjfpd_hiqHaAhWBL1AKHTPYBp0QMwhxKCowKg..i&w=800&h=800&bih=939&biw=1278&q=platformer%20metal%20tile&ved=0ahUKEwjfpd_hiqHaAhWBL1AKHTPYBp0QMwhxKCowKg&iact=mrc&uact=8
    // https://www.google.at/search?q=sprite+metal+wall+with+normals&source=lnms&tbm=isch&sa=X&ved=0ahUKEwjk6ruymaHaAhUDLVAKHfq6B6EQ_AUICigB&biw=1278&bih=939#imgrc=FRfmoI62Hp9inM:

    // light ideas:
    // https://www.google.at/search?q=opengl+volumetric+lighting+from+point+light&source=lnms&tbm=isch&sa=X&ved=0ahUKEwjC4pKhjaTaAhUDqaQKHUSgDPgQ_AUICigB&biw=2560&bih=959


    final float floorX[] = { 50, 100, 150, 200, 250, 300, 350 };
	final float floorY[] = { 50,  50,  50,  50,  50,  50,  50 };
	final float floorSize = 50;

    private void renderFloor() {
        wall_norm.bind(1);
        wall.bind(0);
	    for (int i = 0; i < floorX.length; i++) {
            batch.draw(wall, floorX[i], floorY[i], floorSize, floorSize);
        }
    }
}
