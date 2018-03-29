package com.las2d;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.Random;

public class Game extends ApplicationAdapter implements ApplicationListener {
	SpriteBatch batch;
	Texture img;

	Texture box;
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		img = new Texture("badlogic.jpg");

		box = new Texture("box.png");
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

        int mouseX = Gdx.input.getX();
		int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.begin();
		renderBoxes();
		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
	}

	// -----------------------------------------------------------

    private void renderBoxes() {
        Random rand = new Random(10);
	    for (int i = 0; i < 10; i++) {
	        int size = (rand.nextInt(3)+1) * 30;
            batch.draw(box, rand.nextInt(800/30) * 30, rand.nextInt(600/30) * 30, size, size);
        }
    }
}
