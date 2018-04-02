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
import java.awt.geom.Point2D;
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

    ArrayList<Point2D.Float> drops;

    @Override
    public void create () {
        rects = new ArrayList<>();
        batch = new SpriteBatch();
        drops = new ArrayList<>();

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
    final float dropTimer = 1.0f / 20.0f;
    final float dx = 20.0f;
    final float dy = -200.0f;

    @Override
    public void render () {
        dropCreationCounter += Gdx.graphics.getDeltaTime();
        if (dropCreationCounter >= dropTimer) {
            dropCreationCounter -= dropTimer;
            drops.add(new Point2D.Float(rand.nextInt(800), 630));
        }

        ArrayList<Integer> del = new ArrayList<>();
        for (int i = 0; i < drops.size(); i++) {
            Point2D.Float pt = drops.get(i);
            pt.x += dx * Gdx.graphics.getDeltaTime();
            pt.y += dy * Gdx.graphics.getDeltaTime();
            if (pt.y < 0) {
                del.add(i);
            }
            else {
                for (Rectangle rect : rects) {
                    if (pt.x < rect.x || pt.y > rect.y + rect.height || pt.x > rect.x + rect.width || pt.y < rect.y) {
                        continue;
                    }
                    del.add(i);
                    break;
                }
            }
        }
        for (int i = del.size()-1; i >= 0; i--) {
            drops.remove((int)del.get(i));
        }

        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            System.exit(0);
        }

        Gdx.gl.glClearColor( 0.7f, 0.7f, 0.7f, 1 );
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        batch.setShader(shBlack);
        for (Rectangle rect : rects) {
            batch.draw(texRect, rect.x, rect.y, rect.width, rect.height);
        }

        batch.setShader(shRain);
        for (Point2D.Float pt : drops) {
            batch.draw(texRect, pt.x, pt.y, 10, 10);
        }

        batch.end();


    }
}
