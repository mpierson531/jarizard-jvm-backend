package com.jari;

import com.badlogic.gdx.Game;

public class MyGdxGame extends Game {

	@Override
	public void create() {
		setScreen(new Frontend());
	}
}
