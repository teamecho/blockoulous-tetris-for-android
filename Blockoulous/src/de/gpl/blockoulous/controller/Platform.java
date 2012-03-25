package de.gpl.blockoulous.controller;

import de.gpl.blockoulous.model.TetrominoType;

public interface Platform {

	TetrominoType getNextTetromino();

	long getSystemTime();

	void somethingChangedCallback();

	public void vibrate(int num);
}
