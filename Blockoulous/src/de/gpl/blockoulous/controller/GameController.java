package de.gpl.blockoulous.controller;

/*
 The game logic [GameController.java] is originally taken from Simple Tetris Clone which is under the MIT
 license (http://www.opensource.org/licenses/mit-license.php) and (c) 2011 Laurens Rodriguez Oscanoa.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import java.util.HashSet;
import java.util.Set;

import de.gpl.blockoulous.model.GameData;
import de.gpl.blockoulous.model.Stats;
import de.gpl.blockoulous.model.Tetromino;
import de.gpl.blockoulous.model.TetrominoType;

public class GameController {
	private static final int	ERROR_NONE						= 34245;
	public static int			default_BOARD_TILEMAP_WIDTH		= 10;
	public static int			default_BOARD_TILEMAP_HEIGHT	= 22;

	/*
	 * Score points given by filled rows (we use the original NES * 10)
	 * http://tetris.wikia.com/wiki/Scoring
	 */
	public int					SCORE_1_FILLED_ROW				= 400;
	public int					SCORE_2_FILLED_ROW				= 1000;
	public int					SCORE_3_FILLED_ROW				= 3000;
	public int					SCORE_4_FILLED_ROW				= 12000;

	/*
	 * The player gets points every time he accelerates downfall. The added
	 * points are equal to SCORE_2_FILLED_ROW divided by this value
	 */
	public int					SCORE_MOVE_DOWN_DIVISOR			= 1000;

	/*
	 * The player gets points every time he does a hard drop. The added points
	 * are equal to SCORE_2_FILLED_ROW divided by these values. If the player is
	 * not using the shadow he gets more points
	 */
	public int					SCORE_DROP_DIVISOR				= 20;
	public int					SCORE_DROP_WITH_SHADOW_DIVISOR	= 100;

	public int					SCORE_EXTENDED_BLOCK_FACTOR		= 2;

	/* Number of filled rows required to increase the game level */
	public int					FILLED_ROWS_FOR_LEVEL_UP		= 10;

	/*
	 * The falling delay is multiplied and divided by these factors with every
	 * level up
	 */
	public int					DELAY_FACTOR_FOR_LEVEL_UP		= 9;
	public int					DELAY_DIVISOR_FOR_LEVEL_UP		= 10;

	public GameController(Platform p, GameData d) {
		this.data = d;
		this.mPlatform = p;

		onTetrominoMoved();
	}

	public GameController(Platform p, int w, int h, boolean extendedSet) {
		this.data.setBOARD_TILEMAP_HEIGHT(h);
		this.data.setBOARD_TILEMAP_WIDTH(w);
		this.data.setExtendedSet(extendedSet);

		this.mPlatform = p;
		initGameParams();

		/* Initialize events */
		onTetrominoMoved();
	}

	public GameData			data	= new GameData(10, 22, 1000, new Stats());
	private final Platform	mPlatform;

	/*
	 * Rotate falling tetromino. If there are no collisions when the tetromino
	 * is rotated this modifies the tetromino's cell buffer.
	 */
	private void rotateTetromino(boolean clockwise) {
		int i, j;

		/* If TETROMINO_O is falling return immediately */
		if (data.getmFallingBlock().getType() == TetrominoType.O) {
			return; /* rotation doesn't require any changes */
		}

		/* Initialize rotated cells to blank */
		TetrominoType[][] rotated = new TetrominoType[data.getmFallingBlock().size()][data.getmFallingBlock().size()];

		/* Copy rotated cells to the temporary array */
		for (i = 0; i < data.getmFallingBlock().size(); ++i) {
			for (j = 0; j < data.getmFallingBlock().size(); ++j) {
				if (clockwise) {
					rotated[data.getmFallingBlock().size() - j - 1][i] = data.getmFallingBlock().getData()[i][j];
				} else {
					rotated[j][data.getmFallingBlock().size() - i - 1] = data.getmFallingBlock().getData()[i][j];
				}
			}
		}

		// are wallkicks enabled?!
		if (data.ismWallkicks()) {
			int wallDisplace = 0;

			/* Check collision with left wall */
			if (data.getmFallingBlock().getX() < 0) {
				for (i = 0; (wallDisplace == 0) && (i < -data.getmFallingBlock().getX()); ++i) {
					for (j = 0; j < data.getmFallingBlock().size(); ++j) {
						if (rotated[i][j] != null) {
							wallDisplace = i - data.getmFallingBlock().getX();
							break;
						}
					}
				}
			}
			/* Or check collision with right wall */
			else if (data.getmFallingBlock().getX() > data.getBOARD_TILEMAP_WIDTH() - data.getmFallingBlock().size()) {
				i = data.getmFallingBlock().size() - 1;
				for (; (wallDisplace == 0) && (i >= data.getBOARD_TILEMAP_WIDTH() - data.getmFallingBlock().getX()); --i) {
					for (j = 0; j < data.getmFallingBlock().size(); ++j) {
						if (rotated[i][j] != null) {
							wallDisplace = -data.getmFallingBlock().getX() - i + data.getBOARD_TILEMAP_WIDTH() - 1;
							break;
						}
					}
				}
			}

			/* Check collision with board floor and other cells on board */
			for (i = 0; i < data.getmFallingBlock().size(); ++i) {
				for (j = 0; j < data.getmFallingBlock().size(); ++j) {
					if (rotated[i][j] != null) {
						/* Check collision with bottom border of the map */
						if (data.getmFallingBlock().getY() + j >= data.getBOARD_TILEMAP_HEIGHT()) {
							return; /* there was collision therefore return */
						}
						/* Check collision with existing cells in the map */
						if (data.getGameBoard()[i + data.getmFallingBlock().getX() + wallDisplace][j + data.getmFallingBlock().getY()] != null) {
							return; /* there was collision therefore return */
						}
					}
				}
			}
			/*
			 * Move the falling piece if there was wall collision and it's a
			 * legal move
			 */
			if (wallDisplace != 0) {
				int newX = data.getmFallingBlock().getX() + wallDisplace;
				data.getmFallingBlock().setX(newX);
			}
		} else {

			/* Check collision of the temporary array */
			for (i = 0; i < data.getmFallingBlock().size(); ++i) {
				for (j = 0; j < data.getmFallingBlock().size(); ++j) {
					if (rotated[i][j] == null) {
						/*
						 * Check collision with left, right or bottom borders of
						 * the map
						 */
						if ((data.getmFallingBlock().getX() + i < 0) || (data.getmFallingBlock().getX() + i >= data.getBOARD_TILEMAP_WIDTH())
								|| (data.getmFallingBlock().getY() + j >= data.getBOARD_TILEMAP_HEIGHT())) {
							return; /* there was collision therefore return */
						}
						/* Check collision with existing cells in the map */
						if (data.getGameBoard()[i + data.getmFallingBlock().getX()][j + data.getmFallingBlock().getY()] != null) {
							return; /* there was collision therefore return */
						}
					}
				}
			}
		}

		/* There are no collisions, replace tetromino cells with rotated cells */
		for (i = 0; i < data.getmFallingBlock().size(); ++i) {
			for (j = 0; j < data.getmFallingBlock().size(); ++j) {
				data.getmFallingBlock().setData(rotated);
			}
		}
		onTetrominoMoved();
	}

	/**
	 * Check if tetromino will collide with something if it is moved in the
	 * requested direction. If there are collisions returns 1 else returns 0.
	 * 
	 * @param dx
	 * @param dy
	 * @return
	 */
	private boolean checkCollision(int dx, int dy) {

		int newx = data.getmFallingBlock().getX() + dx;
		int newy = data.getmFallingBlock().getY() + dy;

		for (int i = 0; i < data.getmFallingBlock().size(); ++i) {
			for (int j = 0; j < data.getmFallingBlock().size(); ++j) {
				if (data.getmFallingBlock().getData()[i][j] != null) {
					/*
					 * Check the tetromino would be inside the left, right and
					 * bottom borders
					 */
					if ((newx + i < 0) || (newx + i >= data.getBOARD_TILEMAP_WIDTH()) || (newy + j >= data.getBOARD_TILEMAP_HEIGHT())) {
						return true;
					}
					/*
					 * Check the tetromino won't collide with existing cells in
					 * the map
					 */
					if (data.getGameBoard()[newx + i][newy + j] != null) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/* Game scoring: http://tetris.wikia.com/wiki/Scoring */
	private void onFilledRows(int filledRows) {
		/* Update total number of filled rows */
		data.getmStats().setLines(data.getmStats().getLines() + filledRows);

		/* Increase score accordingly to the number of filled rows */
		int factor = 0;
		switch (filledRows) {
			case 1:
				factor = SCORE_1_FILLED_ROW;
				break;
			case 2:
				factor = SCORE_2_FILLED_ROW;
				break;
			case 3:
				factor = SCORE_3_FILLED_ROW;
				break;
			case 4:
				factor = SCORE_4_FILLED_ROW;
				break;
			default:
				/* This shouldn't happen, but if happens kill the game */
				throw new IllegalArgumentException("butz! soo ein feuerball, junge.");
		}

		// vibrate
		mPlatform.vibrate(filledRows);

		data.getmStats().setScore(data.getmStats().getScore() + factor * (data.getmStats().getLevel() + 1));

		/* Check if we need to update the level */
		if (data.getmStats().getLines() >= FILLED_ROWS_FOR_LEVEL_UP * (data.getmStats().getLevel() + 1)) {
			data.getmStats().setLevel(data.getmStats().getLevel() + 1);

			/* Increase speed for falling tetrominoes */
			data.setmFallingDelay((DELAY_FACTOR_FOR_LEVEL_UP * data.getmFallingDelay() / DELAY_DIVISOR_FOR_LEVEL_UP));
		}
	}

	/*
	 * Move tetromino in the direction specified by (x, y) (in tile units) This
	 * function detects if there are filled rows or if the move lands a falling
	 * tetromino, also checks for game over condition.
	 */
	private void moveTetromino(int x, int y) {
		int i, j;

		/* Check if the move would create a collision */
		if (checkCollision(x, y)) {
			/* In case of collision check if move was downwards (y == 1) */
			if (y == 1) {
				/*
				 * Check if collision occurs when the falling tetromino is on
				 * the 1st or 2nd row
				 */
				if (data.getmFallingBlock().getY() <= 1) {
					data.setmIsOver(true); /* if this happens the game is over */
					data.setmStateChanged(true);
					mPlatform.somethingChangedCallback();
				} else {
					/*
					 * The falling tetromino has reached the bottom, so we copy
					 * their cells to the board map
					 */
					for (i = 0; i < data.getmFallingBlock().size(); ++i) {
						for (j = 0; j < data.getmFallingBlock().size(); ++j) {
							if (data.getmFallingBlock().getData()[i][j] != null) {
								data.getGameBoard()[data.getmFallingBlock().getX() + i][data.getmFallingBlock().getY() + j] = data.getmFallingBlock().getData()[i][j];
							}
						}
					}

					/* Check if the landing tetromino has created full rows */
					int numFilledRows = 0;
					for (j = 1; j < data.getBOARD_TILEMAP_HEIGHT(); ++j) {
						boolean hasFullRow = true;
						for (i = 0; i < data.getBOARD_TILEMAP_WIDTH(); ++i) {
							if (data.getGameBoard()[i][j] == null) {
								hasFullRow = false;
								break;
							}
						}
						/*
						 * If we found a full row we need to remove that row
						 * from the map we do that by just moving all the above
						 * rows one row below
						 */
						if (hasFullRow) {
							for (x = 0; x < data.getBOARD_TILEMAP_WIDTH(); ++x) {
								for (y = j; y > 0; --y) {
									data.getGameBoard()[x][y] = data.getGameBoard()[x][y - 1];
								}
							}
							numFilledRows++; /* increase filled row counter */
						}
					}

					/* Update game statistics */
					if (numFilledRows > 0) {
						onFilledRows(numFilledRows);
					}
					data.getmStats().increasePieces();
					data.getmStats().increasePiece(data.getmFallingBlock().getType());

					/*
					 * Use preview tetromino as falling tetromino. Copy preview
					 * tetromino for falling tetromino
					 */
					for (i = 0; i < data.getmFallingBlock().size(); ++i) {
						for (j = 0; j < data.getmFallingBlock().size(); ++j) {
							data.setmFallingBlock(data.getmNextBlock());
						}
					}

					/* Reset position */
					data.getmFallingBlock().setY(0);
					data.getmFallingBlock().setX((data.getBOARD_TILEMAP_WIDTH() - data.getmFallingBlock().size()) / 2);
					onTetrominoMoved();

					/* Create next preview tetromino */
					TetrominoType t = mPlatform.getNextTetromino();
					data.setmNextBlock(Tetromino.factory.create(t));
				}
			}
		} else {
			/* There are no collisions, just move the tetromino */
			data.getmFallingBlock().setX(data.getmFallingBlock().getX() + x);
			data.getmFallingBlock().setY(data.getmFallingBlock().getY() + y);
		}
		onTetrominoMoved();
	}

	/* Hard drop */
	private void dropTetromino() {
		moveTetromino(0, data.getmShadowGap());
		moveTetromino(0, 1); /* Force lock */

		int extendedBlockFactor = (data.isExtendedSet()) ? SCORE_EXTENDED_BLOCK_FACTOR : 1;

		/* Update score */
		if (data.ismShowShadow()) {
			long toAdd = (SCORE_2_FILLED_ROW * extendedBlockFactor * (data.getmStats().getLevel() + 1) / SCORE_DROP_WITH_SHADOW_DIVISOR);
			data.getmStats().setScore(data.getmStats().getScore() + toAdd);
		} else {
			long toAdd = (SCORE_2_FILLED_ROW * extendedBlockFactor * (data.getmStats().getLevel() + 1) / SCORE_DROP_DIVISOR);
			data.getmStats().setScore(data.getmStats().getScore() + toAdd);
		}
	}

	/*
	 * Main function game called every frame
	 */
	public void update() {

		/* Update game state */
		if (data.ismIsOver()) {
			if (data.getmEvents().contains(Event.RESTART)) {
				data.setmIsOver(false);
				initGameParams();
			}
		} else {
			/* Always handle restart event */
			if (data.getmEvents().contains(Event.RESTART)) {
				initGameParams();
				return;
			}

			long currentTime = mPlatform.getSystemTime();

			/* Always handle pause event */
			if (data.getmEvents().contains(Event.PAUSE)) {
				data.setmIsPaused(!data.ismIsPaused());
				data.getmEvents().clear();
			}

			/* Check if the game is paused */
			if (data.ismIsPaused()) {
				/*
				 * We achieve the effect of pausing the game adding the last
				 * frame duration to lastFallTime
				 */
				data.setmLastFallTime(data.getmLastFallTime() + (currentTime - data.getmSystemTime()));
			} else {
				if (data.getmEvents().size() > 0) {

					/*
					 * if (data.getmEvents().contains(Event.EVENT_SHOW_NEXT)) {
					 * data.setmShowPreview(!data.ismShowPreview());
					 * data.setmStateChanged(true);
					 * mPlatform.somethingChangedCallback(); }
					 */
					if (data.getmEvents().contains(Event.DROP)) {
						dropTetromino();
					}
					if (data.getmEvents().contains(Event.ROTATE_CW)) {
						rotateTetromino(true);
					}
					/*
					 * if
					 * (data.getmEvents().contains(Event.EVENT_ROTATE_DOUBLE)) {
					 * rotateTetromino(true); rotateTetromino(true); }
					 */
					if (data.getmEvents().contains(Event.MOVE_RIGHT)) {
						moveTetromino(1, 0);
					} else if (data.getmEvents().contains(Event.MOVE_LEFT)) {
						moveTetromino(-1, 0);
					}
					if (data.getmEvents().contains(Event.MOVE_DOWN)) {
						/* Update score if the player accelerates downfall */
						int extendedBlockSetFactor = (data.isExtendedSet()) ? SCORE_EXTENDED_BLOCK_FACTOR : 1;

						long toAdd = (SCORE_2_FILLED_ROW * extendedBlockSetFactor * (data.getmStats().getLevel() + 1) / SCORE_MOVE_DOWN_DIVISOR);
						data.getmStats().setScore(data.getmStats().getScore() + toAdd);

						moveTetromino(0, 1);
					}
					data.getmEvents().clear();
				}
				/* Check if it's time to move downwards the falling tetromino */
				if (currentTime - data.getmLastFallTime() >= data.getmFallingDelay()) {
					moveTetromino(0, 1);
					data.setmLastFallTime(currentTime);
				}
			}
			/* Save current time for next game update */
			data.setmSystemTime(currentTime);
		}
	}

	/* This event is called when the falling tetromino is moved */
	private void onTetrominoMoved() {
		int y = 0;
		/* Calculate number of cells where shadow tetromino would be */
		while (!checkCollision(0, ++y))
			;
		data.setmShadowGap(y - 1);
		data.setmStateChanged(true);
		mPlatform.somethingChangedCallback();
	}

	/* Start a new game */
	private void initGameParams() {
		/* Initialize game data */
		data.setmErrorCode(ERROR_NONE);
		data.setmSystemTime(mPlatform.getSystemTime());
		data.setmLastFallTime(data.getmSystemTime());
		data.setmIsOver(false);
		data.setmIsPaused(false);
		data.setmEvents(new HashSet<Event>());
		data.setmFallingDelay(data.getINIT_DELAY_FALL());
		data.setmShowShadow(true);

		/* Initialize game statistics */
		data.setmStats(new Stats());

		/* Initialize random generator */
		// mPlatform.seedRandom(mSystemTime);

		/* Initialize game tile map */
		data.setGameBoard(new TetrominoType[data.getBOARD_TILEMAP_WIDTH()][data.getBOARD_TILEMAP_HEIGHT()]);

		/* Initialize falling tetromino */
		TetrominoType t = mPlatform.getNextTetromino();
		data.setmFallingBlock(Tetromino.factory.create(t));
		data.getmFallingBlock().setX((data.getBOARD_TILEMAP_WIDTH() - data.getmFallingBlock().size()) / 2);
		data.getmFallingBlock().setY(0);

		/* Initialize preview tetromino */
		TetrominoType t2 = mPlatform.getNextTetromino();
		data.setmNextBlock(Tetromino.factory.create(t2));
	}

	public Stats getmStats() {
		return data.getmStats();
	}

	public Tetromino getmNextBlock() {
		return data.getmNextBlock();
	}

	public void addAllToMEvents(Set<Event> mEvents) {
		this.data.getmEvents().addAll(mEvents);
	}

	public void addToMEvents(Event e) {
		this.data.getmEvents().add(e);
	}

	public TetrominoType[][] getGameBoard() {
		return data.getGameBoard();
	}

	public Tetromino getmFallingBlock() {
		return data.getmFallingBlock();
	}

	public boolean ismIsOver() {
		return data.ismIsOver();
	}

	public int getmFallingDelay() {
		return data.getmFallingDelay();
	}

	public void setmFallingDelay(int mFallingDelay) {
		this.data.setmFallingDelay(mFallingDelay);
	}

	public void setmStats(Stats mStats) {
		this.data.setmStats(mStats);
	}

	public void setmFallingBlock(Tetromino mFallingBlock) {
		this.data.setmFallingBlock(mFallingBlock);
	}

	public void setGameBoard(TetrominoType[][] gameBoard) {
		this.data.setGameBoard(gameBoard);
	}

	public void setmNextBlock(Tetromino mNextBlock) {
		this.data.setmNextBlock(mNextBlock);
	}

	public int getmShadowGap() {
		return data.getmShadowGap();
	}

	public boolean ismIsPaused() {
		return data.ismIsPaused();
	}

	public boolean ismShowShadow() {
		return data.ismShowShadow();
	}

	public void setmShowShadow(boolean mShowShadow) {
		this.data.setmShowShadow(mShowShadow);
	}

	public boolean ismShowPreview() {
		return data.ismShowPreview();
	}

	public void setmShowPreview(boolean mShowPreview) {
		this.data.setmShowPreview(mShowPreview);
	}

	public int getBOARD_TILEMAP_WIDTH() {
		return data.getBOARD_TILEMAP_WIDTH();
	}

	public void setBOARD_TILEMAP_WIDTH(int bOARD_TILEMAP_WIDTH) {
		data.setBOARD_TILEMAP_WIDTH(bOARD_TILEMAP_WIDTH);
	}

	public int getBOARD_TILEMAP_HEIGHT() {
		return data.getBOARD_TILEMAP_HEIGHT();
	}

	public void setBOARD_TILEMAP_HEIGHT(int bOARD_TILEMAP_HEIGHT) {
		data.setBOARD_TILEMAP_HEIGHT(bOARD_TILEMAP_HEIGHT);
	}

	public GameData getData() {
		return data;
	}

	public boolean ismStateChanged() {
		return data.ismStateChanged();
	}

	public void setmStateChanged(boolean a) {
		data.setmStateChanged(a);
	}

	public boolean ismWallkicks() {
		return data.ismWallkicks();
	}

	public void setmWallkicks(boolean a) {
		data.setmWallkicks(a);
	}

}
