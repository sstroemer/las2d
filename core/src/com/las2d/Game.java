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

public class Game extends ApplicationAdapter implements ApplicationListener {
	SpriteBatch batch;
	Texture rock, rock_norm, drop, drop_alpha, waterFg, waterBg;

    ShaderProgram s_rain, s_light;

    //our constants...
    public static final float DEFAULT_LIGHT_Z = 0.075f;
    public static final float AMBIENT_INTENSITY = 0.9f;
    public static float LIGHT_INTENSITY = 0.2f;

    public static final Vector3 LIGHT_POS = new Vector3(0f,0f,DEFAULT_LIGHT_Z);

    //Light RGB and intensity (alpha)
//    public static final Vector3 LIGHT_COLOR = new Vector3(1f, 0.6f, 0.3f);
    public static final Vector3 LIGHT_COLOR = new Vector3(0.8f, 0.45f, 0.0f);

    //Ambient RGB and intensity (alpha)
    public static final Vector3 AMBIENT_COLOR = new Vector3(0.6f, 0.6f, 0.6f);

    //Attenuation coefficients for light falloff
    public static final Vector3 FALLOFF = new Vector3(.2f, 1f, 2f);
    private OrthographicCamera cam;
    private int spec_n = 1;
    private ShaderProgram s;

    FrameBuffer fbo;
    private Texture shine;

    @Override
	public void create () {
        rock       = new Texture(Gdx.files.internal("rock.png"));
        rock_norm  = new Texture(Gdx.files.internal("rock_normal.png"));
        drop       = new Texture(Gdx.files.internal("rain/drop-color.png"));
        drop_alpha = new Texture(Gdx.files.internal("rain/drop-alpha.png"));
        waterBg    = new Texture(Gdx.files.internal("rain/texture-bg.png"));
        waterFg    = new Texture(Gdx.files.internal("rain/texture-fg.png"));
        shine    = new Texture(Gdx.files.internal("rain/drop-shine.png"));
		//rock      = new Texture(Gdx.files.internal("wall.jpg"));
		//rock_norm = new Texture(Gdx.files.internal("wall_normal.jpg"));

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, 800, 600, false);

        ShaderProgram.pedantic = false;
        s_rain = new ShaderProgram(Gdx.files.internal("shader.vert").readString(), Gdx.files.internal("rain.frag").readString());
        s_light = new ShaderProgram(Gdx.files.internal("shader.vert").readString(), Gdx.files.internal("light.frag").readString());

        if (!s_rain.isCompiled())
            throw new GdxRuntimeException("Could not compile shader: "+s_rain.getLog());
        //print any warnings
        if (s_rain.getLog().length()!=0)
            System.out.println(s_rain.getLog());

        //setup default uniforms
        s_light.begin();

        //our normal map
        s_light.setUniformi("u_normals", 1); //GL_TEXTURE1

        //light/ambient colors
        //LibGDX doesn't have Vector4 class at the moment, so we pass them individually...
        s_light.setUniformf("LightColor", LIGHT_COLOR.x, LIGHT_COLOR.y, LIGHT_COLOR.z, LIGHT_INTENSITY);
        s_light.setUniformf("AmbientColor", AMBIENT_COLOR.x, AMBIENT_COLOR.y, AMBIENT_COLOR.z, AMBIENT_INTENSITY);
        s_light.setUniformf("Falloff", FALLOFF);

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
        s_light.setUniformf("Resolution", width, height);
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
            spec_n += 15;
            if (spec_n > 70) {
                spec_n = 1;
            }
        }




        //update light position, normalized to screen resolution
        float x = Gdx.input.getX() / (float)Gdx.graphics.getWidth();
        float y = 1.0f - Gdx.input.getY() / (float)Gdx.graphics.getHeight();

        LIGHT_POS.x = x + (torchRand.nextFloat() - .5f) * 0.01f;
        LIGHT_POS.y = y + (torchRand.nextFloat() - .5f) * 0.01f;

        LIGHT_INTENSITY = (float)clamp(LIGHT_INTENSITY+(torchRand.nextFloat()-.5)/10,0.5, 1);

        //send a Vector4f to GLSL
        s_light.begin();
        s_light.setUniformf("LightPos", LIGHT_POS);
        s_light.setUniformf("LightColor", LIGHT_COLOR.x, LIGHT_COLOR.y, LIGHT_COLOR.z, LIGHT_INTENSITY);
        s_light.setUniformi("spec_n", spec_n);
        s_light.end();

        //bind normal map to texture unit 1
        rock_norm.bind(1);

        //bind diffuse color to texture unit 0
        //important that we specify 0 otherwise we'll still be bound to glActiveTexture(GL_TEXTURE1)
        rock.bind(0);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        //batch.setShader(shader);
        fbo.begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            batch.begin();
                batch.setShader(s_light);
                batch.draw(rock, 0, 0, 800, 600);
            batch.end();
        fbo.end();


        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        batch.begin();

        TextureRegion tex = new TextureRegion(fbo.getColorBufferTexture());
        tex.flip(false, true);

        batch.setShader(null);
        batch.draw(tex, 0, 0);

        batch.setShader(s_rain);

        shine.bind(3);
        s_rain.setUniformi("u_shine", 3);

        // the texture doesn't get flipped!!
        tex.getTexture().bind(2);
        s_rain.setUniformi("u_background", 2);

        drop.bind(1);
        s_rain.setUniformi("u_drop", 1);

        drop_alpha.bind(0);

        //batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.draw(drop_alpha, 100, dy--);
        if (dy < 0) dy = 600;

		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
	}

	// -----------------------------------------------------------

    private void renderBoxes() {
        Random rand = new Random(1);
	    for (int i = 0; i < 10; i++) {
	        int size = (rand.nextInt(3)+1) * 90;
            batch.draw(rock, rand.nextInt(800/30) * 30, rand.nextInt(600/30) * 30, size, size);
        }
    }
}
