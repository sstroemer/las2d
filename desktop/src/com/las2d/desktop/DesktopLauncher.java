package com.las2d.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.las2d.GameLightning;
import com.las2d.GameRainIngame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width  = 800;
		config.height = 600;
		config.vSyncEnabled = false;
		new LwjglApplication(new GameLightning(), config);
	}
}
