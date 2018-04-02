package com.las2d;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class GameRainIngame extends ApplicationAdapter implements ApplicationListener {
    Random rand = new Random();

    ArrayList<Rectangle> rects;

    SpriteBatch batch;
    Texture texRect;

    ShaderProgram shBlack, shRain;
    FrameBuffer fbo;

    long startTime;

    LinkedList<Point> drops;

    @Override
    public void create () {
        rects = new ArrayList<>();
        batch = new SpriteBatch();
        drops = new LinkedList<>();

        shBlack.pedantic = false;
        shBlack = new ShaderProgram(Gdx.files.internal("shaders/passthrough.vert").readString(), Gdx.files.internal("shaders/black.frag").readString());
        System.out.println(shBlack.getLog());

        shRain.pedantic = false;
        shRain = new ShaderProgram(Gdx.files.internal("shaders/passthrough.vert").readString(), Gdx.files.internal("shaders/rain_ingame.frag").readString());
        System.out.println(shRain.getLog());

        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, 800, 600, false);


        for (int i = 0; i < 7; i++) {
            rects.add(new Rectangle(
                    rand.nextInt(600) + 100,
                    rand.nextInt(400) + 100,
                    rand.nextInt(180) + 20,
                    rand.nextInt(40) + 10
                    )
            );
        }

        texRect = new Texture(Gdx.files.internal("box.png"));

        startTime = System.currentTimeMillis();
    }

    float dropCreationCounter = 0;
    final float dropTimer = 1.0f / 10.0f;
    final float dx = 50.0f;
    final float dy = 200.0f;

    @Override
    public void render () {
        dropCreationCounter += Gdx.graphics.getDeltaTime();
        if (dropCreationCounter >= dropTimer) {
            dropCreationCounter -= dropTimer;
            drops.add(new Point(rand.nextInt(800), 630));
        }
        for (Point pt : drops) {
            pt.x += dx * Gdx.graphics.getDeltaTime();
            pt.y -= dy * Gdx.graphics.getDeltaTime();
            if (pt.y < 0) {
                //drops.remove(pt);
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            System.exit(0);
        }

        Gdx.gl.glClearColor( 0.7f, 0.7f, 0.7f, 1 );
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ZERO);

        batch.begin();

        batch.setShader(shBlack);
        for (Rectangle rect : rects) {
            batch.draw(texRect, rect.x, rect.y, rect.width, rect.height);
        }

        batch.setShader(shRain);
        for (Point pt : drops) {
            batch.draw(texRect, pt.x, pt.y, 10, 10);
        }

        batch.end();


    }
}
