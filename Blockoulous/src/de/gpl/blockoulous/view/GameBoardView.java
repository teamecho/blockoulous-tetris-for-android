package de.gpl.blockoulous.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;
import de.gpl.blockoulous.R;
import de.gpl.blockoulous.controller.GameController;
import de.gpl.blockoulous.model.Tetromino;
import de.gpl.blockoulous.model.TetrominoType;

public class GameBoardView extends View {
	private final GameController gameRef;

	// private static final float globalXOffset = 5;

	private final int gameX;
	private final int gameY;

	private float fieldX;
	private float fieldY;

	public GameBoardView(Context context, GameController gameRef) {
		super(context);

		this.gameRef = gameRef;

		// game & view settings
		this.gameX = gameRef.getBOARD_TILEMAP_WIDTH();
		this.gameY = gameRef.getBOARD_TILEMAP_HEIGHT();

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		if (w == 0 || h == 0) {
			return;
		}

		// this will be the width & height of 1 game field
		this.fieldX = w / gameX;
		this.fieldY = h / gameY;

		if (fieldX < fieldY) {
			this.fieldY = fieldX;
		} else {
			this.fieldX = fieldY;
		}
	}

	/**
	 * @param canvas
	 */
	private void drawGrid(Canvas canvas) {
		Paint mGrid = new Paint();
		mGrid.setAntiAlias(true);
		mGrid.setStrokeWidth(1);
		mGrid.setColor(Color.GRAY);

		float toX = fieldX * gameRef.getBOARD_TILEMAP_WIDTH();
		float toY = fieldY * gameRef.getBOARD_TILEMAP_HEIGHT();

		// vertical lines
		for (float x = 0; x <= toX; x += fieldX) {
			canvas.drawLine(x, 0, x, toY, mGrid);
		}

		// horizontal lines
		for (float y = 0; y <= toY; y += fieldY) {
			canvas.drawLine(0, y, toX, y, mGrid);
		}

		// drar bigger rect.
		Paint p = new Paint();
		p.setStrokeWidth(4);
		p.setColor(Color.GRAY);
		p.setStyle(Style.STROKE);
		float l = 0;
		float r = gameX * fieldX;
		float t = 2 * fieldY;
		float b = gameY * fieldY;
		canvas.drawRect(l, t, r, b, p);
	}

	public static Paint getPaintForColor(TetrColor c) {
		Paint p = new Paint();
		p.setARGB(255, c.r, c.g, c.b);

		if (c != TetrColor.SHADOW) {
			p.setStyle(Style.FILL);
		} else {
			p.setStyle(Style.STROKE);
			p.setStrokeWidth(2f);
		}
		return p;
	}

	private void drawBoard(Canvas canvas) {
		TetrominoType[][] data = gameRef.getGameBoard();

		float w = fieldX;
		float h = fieldY;
		for (int x = 0; x < gameX; x++) {
			for (int y = 0; y < gameY; y++) {
				if (data[x][y] != null) {
					float l = x * w + 1;
					float r = l + w - 1;
					float t = y * h + 1;
					float b = t + h - 1;
					canvas.drawRect(l, t, r, b,
							getPaintForColor(data[x][y].getColor()));
				}// if
			}// for
		}// for
	}

	private void drawTetromino(Canvas canvas, Tetromino current, int gap,
			TetrColor c) {
		TetrominoType[][] data = current.getData();
		float w = fieldX;
		float h = fieldY;

		float x_offset = w * current.getX();
		float y_offset = h * current.getY();

		Paint p = getPaintForColor(c);

		for (int x = 0; x < current.size(); x++) {
			for (int y = 0; y < current.size(); y++) {
				if (data[x][y] != null) {
					float l = x_offset + x * w + 1;
					float r = l + w - 1;
					float t = y_offset + (y + gap) * h + 1;
					float b = t + h - 1;
					canvas.drawRect(l, t, r, b, p);
				}// if
			}// for
		}// for
	}

	private void drawCurrentTetromino(Canvas canvas, Tetromino current) {
		drawTetromino(canvas, current, 0, current.getType().getColor());
	}

	private void drawShadow(Canvas canvas, Tetromino current, int gap) {
		if (gameRef.ismShowShadow())
			drawTetromino(canvas, current, gap, TetrColor.SHADOW);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawColor(Color.TRANSPARENT);

		drawGrid(canvas);

		drawBoard(canvas);

		drawCurrentTetromino(canvas, gameRef.getmFallingBlock());

		drawShadow(canvas, gameRef.getmFallingBlock(), gameRef.getmShadowGap());

		drawMessage(canvas);

		invalidate();
	}

	private void drawMessage(Canvas canvas) {
		// check state
		String msg = null;
		if (gameRef.ismIsOver()) {
			msg = getResources().getString(R.string.game_state_over);
		} else if (gameRef.ismIsPaused()) {
			msg = getResources().getString(R.string.game_state_pause);
		}

		// draw
		if (msg != null) {
			Paint paint = getPaintForColor(TetrColor.WHITE);
			paint.setTextSize(30);
			paint.setFlags(Paint.ANTI_ALIAS_FLAG);
			canvas.drawText(msg, 10, 25, paint);
		}
	}

	public float getFieldX() {
		return fieldX;
	}

	public float getFieldY() {
		return fieldY;
	}
}
