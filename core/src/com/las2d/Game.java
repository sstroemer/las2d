package com.las2d;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.Random;

import static com.badlogic.gdx.math.MathUtils.clamp;

public class Game extends ApplicationAdapter implements ApplicationListener {
	SpriteBatch batch;
	Texture rock, rock_norm, drop, drop_alpha, waterFg, waterBg;

    ShaderProgram shader;

    //our constants...
    public static final float DEFAULT_LIGHT_Z = 0.075f;
    public static final float AMBIENT_INTENSITY = 0.5f;
    public static float LIGHT_INTENSITY = 0.5f;

    public static final Vector3 LIGHT_POS = new Vector3(0f,0f,DEFAULT_LIGHT_Z);

    //Light RGB and intensity (alpha)
//    public static final Vector3 LIGHT_COLOR = new Vector3(1f, 0.6f, 0.3f);
    public static final Vector3 LIGHT_COLOR = new Vector3(0.8f, 0.8f, 0.8f);

    //Ambient RGB and intensity (alpha)
    public static final Vector3 AMBIENT_COLOR = new Vector3(0.6f, 0.6f, 0.6f);

    //Attenuation coefficients for light falloff
    public static final Vector3 FALLOFF = new Vector3(.2f, 2f, 5f);
    private OrthographicCamera cam;
    private int spec_n = 1;
    private ShaderProgram s;

    @Override
	public void create () {
        rock       = new Texture(Gdx.files.internal("rock.png"));
        rock_norm  = new Texture(Gdx.files.internal("rock_normal.png"));
        drop       = new Texture(Gdx.files.internal("rain/drop-color.png"));
        drop_alpha = new Texture(Gdx.files.internal("rain/drop-alpha.png"));
        waterBg    = new Texture(Gdx.files.internal("rain/texture-bg.png"));
        waterFg    = new Texture(Gdx.files.internal("rain/texture-fg.png"));
		//rock      = new Texture(Gdx.files.internal("wall.jpg"));
		//rock_norm = new Texture(Gdx.files.internal("wall_normal.jpg"));

        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(Gdx.files.internal("shader.vert").readString(), Gdx.files.internal("light.frag").readString());

        if (!shader.isCompiled())
            throw new GdxRuntimeException("Could not compile shader: "+shader.getLog());
        //print any warnings
        if (shader.getLog().length()!=0)
            System.out.println(shader.getLog());

        //setup default uniforms
        shader.begin();

        //our normal map
        shader.setUniformi("u_normals", 1); //GL_TEXTURE1

        //light/ambient colors
        //LibGDX doesn't have Vector4 class at the moment, so we pass them individually...
        shader.setUniformf("LightColor", LIGHT_COLOR.x, LIGHT_COLOR.y, LIGHT_COLOR.z, LIGHT_INTENSITY);
        shader.setUniformf("AmbientColor", AMBIENT_COLOR.x, AMBIENT_COLOR.y, AMBIENT_COLOR.z, AMBIENT_INTENSITY);
        shader.setUniformf("Falloff", FALLOFF);

        //LibGDX likes us to end the shader program
        shader.end();

        batch = new SpriteBatch();
        batch.setShader(shader);

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

        shader.begin();
        shader.setUniformf("Resolution", width, height);
        shader.end();
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

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.begin();

        //update light position, normalized to screen resolution
        float x = Gdx.input.getX() / (float)Gdx.graphics.getWidth();
        float y = 1.0f - Gdx.input.getY() / (float)Gdx.graphics.getHeight();

        LIGHT_POS.x = x + (torchRand.nextFloat() - .5f) * 0.01f;
        LIGHT_POS.y = y + (torchRand.nextFloat() - .5f) * 0.01f;

        LIGHT_INTENSITY = (float)clamp(LIGHT_INTENSITY+(torchRand.nextFloat()-.5)/10,0.5, 1);

        //send a Vector4f to GLSL
        shader.setUniformf("LightPos", LIGHT_POS);
        shader.setUniformf("LightColor", LIGHT_COLOR.x, LIGHT_COLOR.y, LIGHT_COLOR.z, LIGHT_INTENSITY);
        shader.setUniformi("spec_n", spec_n);

        //bind normal map to texture unit 1
        rock_norm.bind(1);

        //bind diffuse color to texture unit 0
        //important that we specify 0 otherwise we'll still be bound to glActiveTexture(GL_TEXTURE1)
        rock.bind(0);

        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            s = (shader.equals(s) ? null : shader);
            batch.setShader(s);
        }

        batch.setShader(shader);
        batch.draw(rock, 0, 0, 800, 600);

        /*
        rock.bind(2);
        shader.setUniformi("u_textureBg", 2);

        waterFg.bind(1);
        shader.setUniformi("u_textureFg", 1);

        drop.bind(0);
        shader.setUniformi("u_waterMap", 0);

        //
        //
        batch.setShader(shader);

        batch.draw(drop_alpha, 200, 200);
        */

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
