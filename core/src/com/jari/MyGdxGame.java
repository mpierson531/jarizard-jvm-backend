package com.jari;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.utils.ScreenUtils;
import geo.ui.Label;

public class MyGdxGame extends Game {

	@Override
	public void create() {
		setScreen(new Frontend());
	}
}
