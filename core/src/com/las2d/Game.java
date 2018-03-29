package com.las2d;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.Random;

public class Game extends ApplicationAdapter implements ApplicationListener {
	SpriteBatch batch;
	Texture rock, rock_norm;

    ShaderProgram shader;

    //our constants...
    public static final float DEFAULT_LIGHT_Z = 0.075f;
    public static final float AMBIENT_INTENSITY = 0.2f;
    public static final float LIGHT_INTENSITY = 1f;

    public static final Vector3 LIGHT_POS = new Vector3(0f,0f,DEFAULT_LIGHT_Z);

    //Light RGB and intensity (alpha)
    public static final Vector3 LIGHT_COLOR = new Vector3(1f, 0.8f, 0.6f);

    //Ambient RGB and intensity (alpha)
    public static final Vector3 AMBIENT_COLOR = new Vector3(0.6f, 0.6f, 1f);

    //Attenuation coefficients for light falloff
    public static final Vector3 FALLOFF = new Vector3(.4f, 3f, 20f);


    @Override
	public void create () {
		rock      = new Texture(Gdx.files.internal("rock.png"));
		rock_norm = new Texture(Gdx.files.internal("rock_normal.png"));

        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(Gdx.files.internal("shader.vert").readString(), Gdx.files.internal("shader.frag").readString());
        //shader = new ShaderProgram(VERT, FRAG);

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

        batch = new SpriteBatch(1000, shader);
        batch.setShader(shader);

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

	// todo: IMPORTANT. READ THIS. COMIC STYLE SHADER.
    // http://prideout.net/blog/?p=22

    // todo: IMPORTANT. READ THIS. NORMAL MAP SHADING.
    // https://github.com/mattdesl/Lwjgl-basics/wiki/ShaderLesson6

    // general stuff:
    //      https://www.redblobgames.com/articles/visibility/
    //      http://www.wholehog-games.com/devblog/2013/06/07/lighting-in-a-2d-game/
    //      http://kingunderthemounta.in/dev-blog-2-let-there-be-light/


	@Override
	public void render () {
		if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
		    System.exit(0);
        }

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.begin();

        //update light position, normalized to screen resolution
        float x = Gdx.input.getX() / (float)Gdx.graphics.getWidth();
        float y = Gdx.input.getY() / (float)Gdx.graphics.getHeight();

        LIGHT_POS.x = x;
        LIGHT_POS.y = y;

        //send a Vector4f to GLSL
        shader.setUniformf("LightPos", LIGHT_POS);

        //bind normal map to texture unit 1
        rock_norm.bind(1);

        //bind diffuse color to texture unit 0
        //important that we specify 0 otherwise we'll still be bound to glActiveTexture(GL_TEXTURE1)
        rock.bind(0);

		renderBoxes();
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
