package de.gpl.blockoulous.activities;

import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.gpl.blockoulous.R;
import de.gpl.blockoulous.controller.Event;
import de.gpl.blockoulous.controller.GameController;
import de.gpl.blockoulous.controller.HighscoreSubmission;
import de.gpl.blockoulous.controller.Platform;
import de.gpl.blockoulous.model.GameData;
import de.gpl.blockoulous.model.Stats;
import de.gpl.blockoulous.model.Tetromino;
import de.gpl.blockoulous.model.TetrominoType;
import de.gpl.blockoulous.view.GameBoardView;

public class TetrisActivity extends Activity implements Platform, OnSharedPreferenceChangeListener {
	private GestureDetector		mGestureDetector;
	private static final long	WAIT_DELAY									= 30;
	private static final long	WAIT_PAUSE_DELAY							= 300;
	private static final int	REQUEST_PREFERENCES							= 3424145;
	private final Random		mRand										= new Random(System.currentTimeMillis());
	private GameController		mGame;

	private ProgressDialog		progressDialog;

	private static final float	DROP_ZONE_X									= 0.75f;
	private static final float	DROP_ZONE_Y									= 0.5f;

	private static final float	LOCK_MOVE_ZONE_X							= 0.1f;
	private static final float	LOCK_MOVE_ZONE_Y							= 0.4f;

	private static final float	CONTROL_CONTINUOS_TOUCH_NO_MOVE_THRESHOLD	= 15;
	private static final int	CONTROL_CONTINOUS_DELAY_MOVE				= 100;
	private static final int	CONTROL_CONTINOUS_DELAY_ROTATE				= 250;
	private static final int	CONTROL_CONTINOUS_DELAY_DROP				= 400;

	private static final int	HANDLER_REPAINT								= 45787;
	public static final int		HANDLER_STATUS_OK							= 200;
	public static final int		HANDLER_STATUS_FAIL							= 500;
	private static final int	HANDLER_SUBMIT								= 234523;
	private static final int	HANDLER_GAME_OVER							= 3542;
	private static final int	MAX_NAME_LENGTH								= 20;
	public static final String	DEBUG_TAG									= "tetris";
	private static final long	VIBRATION_TIME								= 100;
	// private static final long VIBRATION_TIME_GAP = 100;
	private static final float	TETROMINO_EXTENDED_THRES					= 0.1f;

	private boolean				mPausedFlag									= false;

	private final Set<Event>	mUserInputActionEvents						= new HashSet<Event>();

	private GameBoardView		mGameScreen;
	private GameLooper			mGameLooper;

	public void vibrate(int num) {
		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("preferences_interface_vibrate_on_clear", true)) {
			return;
		}
		// Get instance of Vibrator from current Context
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		/*
		 * long[] pattern = { 0, VIBRATION_TIME, VIBRATION_TIME_GAP,
		 * VIBRATION_TIME, VIBRATION_TIME_GAP };
		 * 
		 * // Only perform this pattern one time (-1 means "do not repeat")
		 * v.vibrate(pattern, -1);
		 */
		v.vibrate(VIBRATION_TIME);
	}

	private class GestureDetection extends GestureDetector.SimpleOnGestureListener {
		private boolean inDropZone(MotionEvent e) {
			float x = e.getHistoricalX(0, 0);
			int w = getWindowManager().getDefaultDisplay().getWidth();

			if ((x / w) > DROP_ZONE_X) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// log.i(DEBUG_TAG, "Gesture: onDoubleTap");

			lockPause(false);

			// NO DOUBLE ROTATE SINCE THE ONTAPUP WILL ROTATE AS WELL
			if (!inDropZone(e)) {
				mUserInputActionEvents.add(Event.ROTATE_CW);
			}
			return super.onDoubleTap(e);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			// log.i(DEBUG_TAG, "Gesture: onFling");

			lockPause(false);

			if (Math.abs(velocityY) > Math.abs(velocityX)) {
				// the velocity in x-axis is less than Y => DOWN
				mUserInputActionEvents.add(Event.MOVE_DOWN);
			} else if (velocityX > 0) {
				// the x-velocity is greater 0 => RIGHT
				mUserInputActionEvents.add(Event.MOVE_RIGHT);
			} else {
				// the x-velocity is less 0 => RIGHT
				mUserInputActionEvents.add(Event.MOVE_LEFT);
			}

			return super.onFling(e1, e2, velocityX, velocityY);
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// log.i(DEBUG_TAG, "Gesture: onSingleTap");

			lockPause(false);

			// is this a tap to rotate or drop?
			if (inDropZone(e)) {
				mUserInputActionEvents.add(Event.DROP);
			} else {
				mUserInputActionEvents.add(Event.ROTATE_CW);
			}
			return super.onSingleTapConfirmed(e);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mGameLooper != null) {
			mGameLooper.mHandler.obtainMessage().sendToTarget();
		}

		lockPause(true);

		try {
			// !!!!
			if (!mGame.ismIsOver()) {
				while (!mGame.ismIsPaused()) {
					Thread.sleep(40);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// the game will probably be paused, toggle this if this pause was not
		// set explicitly
		syncPause();

		startGame();
	}

	private void syncPause() {
		if (mPausedFlag) {
			if (!mGame.ismIsPaused()) {
				mUserInputActionEvents.add(Event.PAUSE);
			}
		} else {
			if (mGame.ismIsPaused()) {
				mUserInputActionEvents.add(Event.PAUSE);
			}
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// log.i(DEBUG_TAG, "onCreate");

		/*
		 * StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		 * .detectAll().penaltyLog().penaltyDeath().build());
		 * StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
		 * .penaltyLog().penaltyDeath().build());
		 */

		getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		setupPreferences();

		// setup highscore-submit progressbar
		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(R.string.highscore_submit_dialog_title);
		progressDialog.setMessage(getResources().getString(R.string.highscore_submit_dialog_message));
		progressDialog.setCancelable(false);
		progressDialog.setIndeterminate(false);

		// then layout
		setContentView(R.layout.game_layout);

		// add touch controls for the buttons
		initControls();

		// add
		mGestureDetector = new GestureDetector(new GestureDetection());

		// setup game -> soted instance will be restored at onRestoreInstance
		if (savedInstanceState == null) {
			defaultGameSetup();
		} else {
			savedInstanceState.setClassLoader(GameData.class.getClassLoader());
			GameData d = (GameData) savedInstanceState.getSerializable("data");
			mPausedFlag = savedInstanceState.getBoolean("pauseFlag");
			if (d != null) {
				mGame = new GameController(this, d);
			} else {
				defaultGameSetup();
			}
		}

		super.onCreate(savedInstanceState);
		// startGame();
	}

	private long	mTrackballLastTimestamp;

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.i(DEBUG_TAG, "onKeyUp() keyCode=" + keyCode);

		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("preferences_control_trackball", true)) {
				mUserInputActionEvents.add(Event.DROP);
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		float x = event.getHistoricalX(0);
		float y = event.getHistoricalY(0);
		int action = event.getAction();

		// only consider move events
		if (action != MotionEvent.ACTION_MOVE) {
			return super.onTrackballEvent(event);
		}
		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.preferences_control_trackball), true)) {
			return super.onTrackballEvent(event);
		}
		lockPause(false);

		// some other event, only process if delay is high enough
		long now = SystemClock.uptimeMillis();
		long delta = now - mTrackballLastTimestamp;

		Event e;
		boolean move_event = true;
		if (x < 0 && y == 0) {
			e = Event.MOVE_LEFT;
		} else if (x > 0 && y == 0) {
			e = Event.MOVE_RIGHT;
		} else if (x == 0 && y < 0) {
			e = Event.ROTATE_CW;
			move_event = false;
		} else if (x == 0 && y > 0) {
			e = Event.MOVE_DOWN;
		} else {
			// probably a click
			return false;
		}

		if (move_event && delta < CONTROL_CONTINOUS_DELAY_MOVE) {
			return true;
		} else if (!move_event && delta < CONTROL_CONTINOUS_DELAY_ROTATE) {
			return true;
		} else {
			mTrackballLastTimestamp = now;
			Log.i(DEBUG_TAG, "onTrackballEvent() delta=" + delta + " x=" + x + " y=" + y + " action=" + action);
		}

		mUserInputActionEvents.add(e);
		return true;
	}

	private void setupPreferences() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		// trigger loading all entries to get their defaults
		// TODO any ideas?!

		// finally
		// register on prefs changed listener
		sp.registerOnSharedPreferenceChangeListener(TetrisActivity.this);

	}

	private void initControls() {
		// check is there is any method activated, if not, enabled buttons

		boolean gestures = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.preferences_control_gestures), true);
		boolean trackball = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.preferences_control_trackball), true);
		boolean buttons = !PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.preferences_control_buttons), true);

		if ((buttons || gestures || trackball) == false) {
			// all false, do not disable
			disableButtons(false);
			Toast.makeText(this, getResources().getString(R.string.preferences_controls_cannot_disable_buttons), Toast.LENGTH_SHORT).show();
		} else {
			disableButtons(!buttons);
		}

		initButtonControls();
		initTouchControls();
	}

	private void initTouchControls() {
		boolean gestures = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.preferences_control_gestures), true);
		boolean continous_enabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getResources().getString(R.string.preferences_control_buttons_continous), false);

		View v = findViewById(R.id.game_layout_outer_linear_layout);
		if (gestures) {
			if (continous_enabled) {
				v.setOnTouchListener(mOnScreenTouch);
			} else {
				v.setOnTouchListener(null);
				// unfortunately done by activities onTouchEvent()
				// View v = findViewById(R.id.gameBoardViewLayout);
				// v.setOnTouchListener(mOnScreenGestureTouch);
			}
		} else {
			v.setOnTouchListener(null);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		boolean continous_enabled = sp.getBoolean(getResources().getString(R.string.preferences_control_buttons_continous), false);
		boolean gestures = sp.getBoolean(getResources().getString(R.string.preferences_control_gestures), true);

		if (!continous_enabled && gestures) {
			return mGestureDetector.onTouchEvent(event);
		} else {
			return super.onTouchEvent(event);
		}
	}

	private void initButtonControls() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		// log.i(DEBUG_TAG, "initButtonConrols()");
		boolean continous_enabled = sp.getBoolean(getResources().getString(R.string.preferences_control_buttons_continous), false);
		Button btnDown = ((Button) findViewById(R.id.buttonControlDown));
		Button btnLeft = ((Button) findViewById(R.id.buttonControlLeft));
		Button btnRight = ((Button) findViewById(R.id.buttonControlRight));
		Button btnRotate = ((Button) findViewById(R.id.buttonControlRotate));
		Button btnDrop = ((Button) findViewById(R.id.buttonControlDrop));

		if (continous_enabled == true) {
			// log.i(DEBUG_TAG, "continous control");
			btnDown.setOnTouchListener(mOnBtnTouch);
			btnLeft.setOnTouchListener(mOnBtnTouch);
			btnRight.setOnTouchListener(mOnBtnTouch);
			btnRotate.setOnTouchListener(mOnBtnTouch);
			btnDrop.setOnTouchListener(mOnBtnTouch);

			btnDown.setOnClickListener(null);
			btnLeft.setOnClickListener(null);
			btnRight.setOnClickListener(null);
			btnRotate.setOnClickListener(null);
			btnDrop.setOnClickListener(null);
		} else {
			// log.i(DEBUG_TAG, "click control");
			btnLeft.setOnClickListener(mOnBtnClick);
			btnRight.setOnClickListener(mOnBtnClick);
			btnRotate.setOnClickListener(mOnBtnClick);
			btnDown.setOnClickListener(mOnBtnClick);
			btnDrop.setOnClickListener(mOnBtnClick);

			btnDown.setOnTouchListener(null);
			btnLeft.setOnTouchListener(null);
			btnRight.setOnTouchListener(null);
			btnRotate.setOnTouchListener(null);
			btnDrop.setOnTouchListener(null);
		}
	}

	private void defaultGameSetup() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		int h = Integer.parseInt(sp.getString("preferences_game_board_y", Integer.toString(GameController.default_BOARD_TILEMAP_HEIGHT)));
		int w = Integer.parseInt(sp.getString("preferences_game_board_x", Integer.toString(GameController.default_BOARD_TILEMAP_WIDTH)));
		boolean extended = sp.getBoolean(getResources().getString(R.string.preferences_extended_block_set), false);
		setupGame(w, h, extended);
	}

	private void restart() {
		mGameLooper.mHandler.obtainMessage().sendToTarget();
		defaultGameSetup();
		startGame();
		/*
		 * Intent intent = getIntent(); overridePendingTransition(0, 0);
		 * intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION); finish();
		 * 
		 * overridePendingTransition(0, 0); startActivity(intent);
		 */
	}

	private void setupGame(int w, int h, boolean extended) {
		// game setup
		mGame = new GameController(this, w, h, extended);

		// init settings
		setupSettings();

		// initial pause
		// togglePause()
	}

	public static String getFormattedNumber(double d) {
		// Locale loc = Locale.getDefault();
		return NumberFormat.getNumberInstance().format(d);
	}

	private boolean startGame() {
		setupBoardView();

		repaint();

		// start game looper
		mGameLooper = new GameLooper();
		mGameLooper.start();

		return true;
	}

	private void lockPause(boolean lockPause) {
		mPausedFlag = lockPause;

		if (lockPause == true) {
			// ensure pause game
			if (!mGame.ismIsPaused()) {
				mUserInputActionEvents.add(Event.PAUSE);
			}
		} else {
			// unpause game
			if (mGame.ismIsPaused()) {
				mUserInputActionEvents.add(Event.PAUSE);
			}
		}
	}

	private void setupSettings() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		boolean preferences_show_next = sp.getBoolean(getResources().getString(R.string.preferences_show_next), Boolean.parseBoolean(getResources().getString(R.string.preferences_show_next_default)));
		boolean preferences_show_shadow = sp.getBoolean(getResources().getString(R.string.preferences_show_shadow),
				Boolean.parseBoolean(getResources().getString(R.string.preferences_show_shadow_default)));

		mGame.setmShowPreview(preferences_show_next);
		mGame.setmShowShadow(preferences_show_shadow);
	}

	private void setupBoardView() {
		View oldView = findViewById(R.id.view1);

		// get params of view
		ViewGroup.LayoutParams params = oldView.getLayoutParams();
		// get its parents
		ViewGroup v = (ViewGroup) oldView.getParent();
		// int viewX = params.width;
		// int viewY = params.height;
		// and remove it
		v.removeView(oldView);

		// now create our own one
		mGameScreen = new GameBoardView(this, mGame);
		// set the params
		mGameScreen.setLayoutParams(params);
		mGameScreen.setId(R.id.view1);

		// and add it to our design
		v.addView(mGameScreen);
	}

	/*
	 * private final OnTouchListener mOnScreenGestureTouch = new
	 * OnTouchListener() {
	 * 
	 * @Override public boolean onTouch(View v, MotionEvent event) {
	 * Log.i(DEBUG_TAG, "mOnScreenGestureTouch.onTouch()"); if
	 * (mGestureDetector.onTouchEvent(event)) { return true; } return false; }
	 * };
	 */

	private long					mTouchBtnTimestamp	= 0;
	private final OnTouchListener	mOnBtnTouch			= new OnTouchListener() {
															public boolean onTouch(View v, MotionEvent event) {
																lockPause(false);

																// some other
																// event, only
																// process if
																// delay is high
																// enough
																long now = SystemClock.uptimeMillis();
																long delta = now - mTouchBtnTimestamp;

																// log.i(DEBUG_TAG,
																// "mTouchBtn.onTouch(): action="
																// +
																// event.getAction()
																// + " delta=" +
																// delta);

																// up event,
																// clear
																if (event.getAction() == MotionEvent.ACTION_UP) {
																	mTouchBtnTimestamp = 0;
																	return true;
																}

																if ((v.getId() == R.id.buttonControlLeft || v.getId() == R.id.buttonControlRight) && delta < CONTROL_CONTINOUS_DELAY_MOVE) {
																	return true;
																} else if (v.getId() == R.id.buttonControlRotate && delta < CONTROL_CONTINOUS_DELAY_ROTATE) {
																	return true;
																} else if (v.getId() == R.id.buttonControlDrop && delta < CONTROL_CONTINOUS_DELAY_DROP) {
																	return true;
																} else {
																	mTouchBtnTimestamp = now;
																}

																switch (event.getAction()) {
																	case MotionEvent.ACTION_DOWN:
																	case MotionEvent.ACTION_MOVE:
																		switch (v.getId()) {
																			case R.id.buttonControlLeft:
																				mUserInputActionEvents.add(Event.MOVE_LEFT);
																				break;
																			case R.id.buttonControlRight:
																				mUserInputActionEvents.add(Event.MOVE_RIGHT);
																				break;
																			case R.id.buttonControlRotate:
																				mUserInputActionEvents.add(Event.ROTATE_CW);
																				break;
																			case R.id.buttonControlDown:
																				mUserInputActionEvents.add(Event.MOVE_DOWN);
																				break;
																			case R.id.buttonControlDrop:
																				mUserInputActionEvents.add(Event.DROP);
																				break;
																			default:
																				return false;
																		}
																		break;
																	default:
																		return false;
																}
																return false;
															}
														};

	private float					mTouchContinousLastX;
	// private float mTouchContinousLastY;
	private long					mTouchContinousTimestamp;

	private final OnTouchListener	mOnScreenTouch		= new OnTouchListener() {
															private boolean inDropZone(float x, float y) {
																int w = getWindowManager().getDefaultDisplay().getWidth();
																int h = getWindowManager().getDefaultDisplay().getHeight();

																// Log.i(DEBUG_TAG,
																// "zonecheck thresh="
																// + DROP_ZONE +
																// " w=" + w +
																// " h=" + h +
																// " x=" + x +
																// " y=" + y +
																// " (x/w)=" +
																// (x / w)+
																// " (y/h)=" + y
																// / h);

																if ((x / w) > DROP_ZONE_X && (y / h) > DROP_ZONE_Y) {
																	return true;
																} else {
																	return false;
																}
															}

															private boolean inRotateZone(float x, float y) {
																int w = getWindowManager().getDefaultDisplay().getWidth();
																int h = getWindowManager().getDefaultDisplay().getHeight();

																// Log.i(DEBUG_TAG,
																// "zonecheck thresh="
																// + DROP_ZONE +
																// " w=" + w +
																// " h=" + h +
																// " x=" + x +
																// " y=" + y +
																// " (x/w)=" +
																// (x / w)+
																// " (y/h)=" + y
																// / h);

																if ((x / w) > DROP_ZONE_X && (y / h) < DROP_ZONE_Y) {
																	return true;
																} else {
																	return false;
																}
															}

															public boolean onTouch(View v, MotionEvent event) {
																Event eventToDo = null;
																int hsize = event.getHistorySize();
																float x1 = event.getHistoricalX((hsize > 1) ? hsize - 1 : 0);
																float y1 = event.getHistoricalY((hsize > 1) ? hsize - 1 : 0);

																// some other
																// event, only
																// process if
																// delay is high
																// enough
																long now = SystemClock.uptimeMillis();
																long delta = now - mTouchContinousTimestamp;

																boolean inDropZone = inDropZone(x1, y1);

																if (delta < CONTROL_CONTINOUS_DELAY_MOVE) {
																	// Log.i(DEBUG_TAG,
																	// "delta="
																	// + delta);
																	return true;
																} else if (inDropZone) {
																	if (delta >= CONTROL_CONTINOUS_DELAY_DROP) {
																		// Log.i(DEBUG_TAG,
																		// "drop; x1="
																		// + x1
																		// +
																		// " y1="
																		// +
																		// y1);
																		eventToDo = Event.DROP;
																	} else {
																		// Log.i(DEBUG_TAG,
																		// "dropzone; x1="
																		// + x1
																		// +
																		// " y1="
																		// +
																		// y1);
																	}
																} else if (inRotateZone(x1, y1)) {
																	if (delta >= CONTROL_CONTINOUS_DELAY_ROTATE) {
																		// Log.i(DEBUG_TAG,
																		// "drop; x1="
																		// + x1
																		// +
																		// " y1="
																		// +
																		// y1);
																		eventToDo = Event.ROTATE_CW;
																	} else {
																		// Log.i(DEBUG_TAG,
																		// "dropzone; x1="
																		// + x1
																		// +
																		// " y1="
																		// +
																		// y1);
																	}
																} else if (event.getAction() == MotionEvent.ACTION_DOWN) {
																	// first
																	// occurence,
																	// set fresh
																	// reference
																	// point
																	// mTouchContinousLastX
																	// =
																	// Math.round(getWindowManager().getDefaultDisplay().getWidth()
																	// *
																	// DROP_ZONE
																	// / 2);
																	mTouchContinousLastX = x1;
																	mTouchContinousLastX = y1;
																	// Log.i(DEBUG_TAG,
																	// "first event lastX="
																	// +
																	// mTouchContinousLastX+
																	// " lastY="
																	// +
																	// mTouchContinousLastY);
																	return true;
																} else {
																	// calculate
																	// position
																	// & change

																	float delta_x = x1 - mTouchContinousLastX;
																	// float
																	// delta_y =
																	// y1 -
																	// mTouchContinousLastY;

																	// check is
																	// we want
																	// to move
																	if (Math.abs(delta_x) > CONTROL_CONTINUOS_TOUCH_NO_MOVE_THRESHOLD) {
																		if (delta_x > 0) {
																			// Log.i(DEBUG_TAG,
																			// "RIGHT delta_X="
																			// +
																			// delta_x+
																			// " delta_Y="
																			// +
																			// delta_y);
																			eventToDo = Event.MOVE_RIGHT;
																		} else {
																			// Log.i(DEBUG_TAG,
																			// "LEFT delta_X="
																			// +
																			// delta_x+
																			// " delta_Y="
																			// +
																			// delta_y);
																			eventToDo = Event.MOVE_LEFT;
																		}
																	} else if (inLeftZone(x1)) {
																		// Log.i(DEBUG_TAG,
																		// "locked LEFT delta_X="
																		// +
																		// delta_x+
																		// " delta_Y="
																		// +
																		// delta_y);
																		eventToDo = Event.MOVE_LEFT;
																	} else if (inRightZone(x1)) {
																		// Log.i(DEBUG_TAG,
																		// "locked RIGHT delta_X="
																		// +
																		// delta_x+
																		// " delta_Y="
																		// +
																		// delta_y);
																		eventToDo = Event.MOVE_RIGHT;
																	} else if (inDownZone(y1)) {
																		// Log.i(DEBUG_TAG,
																		// "locked DOWN delta_X="
																		// +
																		// delta_x+
																		// " delta_Y="
																		// +
																		// delta_y);
																		eventToDo = Event.MOVE_DOWN;
																	} else {
																		// Log.i(DEBUG_TAG,
																		// "x="
																		// + x1
																		// +
																		// " y="
																		// + y1
																		// +
																		// " delta_X="+
																		// delta_x
																		// +
																		// " delta_Y="
																		// +
																		// delta_y);
																	}
																}

																// add event to
																// queue
																if (eventToDo != null) {
																	lockPause(false);

																	// update
																	// last
																	// event
																	mTouchContinousLastX = x1;
																	// mTouchContinousLastY
																	// = y1;
																	// update
																	// timestamp
																	mTouchContinousTimestamp = now;

																	mUserInputActionEvents.add(eventToDo);
																}
																return true;
															}

															private boolean inDownZone(float y1) {
																int h = getWindowManager().getDefaultDisplay().getHeight();
																if (y1 > h * (1 - LOCK_MOVE_ZONE_Y)) {
																	return true;
																}
																return false;
															}

															private boolean inRightZone(float x1) {
																int w = getWindowManager().getDefaultDisplay().getWidth();
																if (x1 > w * (DROP_ZONE_X - LOCK_MOVE_ZONE_X)) {
																	return true;
																}
																return false;
															}

															private boolean inLeftZone(float x1) {
																int w = getWindowManager().getDefaultDisplay().getWidth();
																if (x1 < w * LOCK_MOVE_ZONE_X) {
																	return true;
																}
																return false;
															}

														};

	private final OnClickListener	mOnBtnClick			= new OnClickListener() {

															public void onClick(View v) {
																// Log.i(DEBUG_TAG,
																// "mOnBtnClick.onClick()");

																lockPause(false);

																switch (v.getId()) {
																	case R.id.buttonControlDrop:
																		// Log.i(DEBUG_TAG,
																		// "button drop");
																		mUserInputActionEvents.add(Event.DROP);
																		break;
																	case R.id.buttonControlLeft:
																		// Log.i(DEBUG_TAG,
																		// "button left");
																		mUserInputActionEvents.add(Event.MOVE_LEFT);
																		break;
																	case R.id.buttonControlRight:
																		// Log.i(DEBUG_TAG,
																		// "button right");
																		mUserInputActionEvents.add(Event.MOVE_RIGHT);
																		break;
																	case R.id.buttonControlRotate:
																		// Log.i(DEBUG_TAG,
																		// "button rotate");
																		mUserInputActionEvents.add(Event.ROTATE_CW);
																		break;
																	case R.id.buttonControlDown:
																		// Log.i(DEBUG_TAG,
																		// "button down");
																		mUserInputActionEvents.add(Event.MOVE_DOWN);
																		break;
																	default:
																}
															}
														};

	public long getSystemTime() {
		return System.currentTimeMillis();
	}

	private final Handler	mHandler	= new Handler() {

											public void handleMessage(Message msg) {
												switch (msg.what) {
													case HANDLER_GAME_OVER:
														if (mGameLooper != null) {
															mGameLooper.mHandler.obtainMessage().sendToTarget();
														}
														break;
													case HANDLER_REPAINT:
														repaint();
														break;
													case HANDLER_STATUS_FAIL:
														String msgtxt = msg.getData().getString("msg");
														Toast.makeText(getApplicationContext(), getResources().getString(R.string.highscore_submit_failed) + ": " + msgtxt, Toast.LENGTH_LONG).show();
														if (progressDialog != null) {
															progressDialog.dismiss();
														}
														break;
													case HANDLER_STATUS_OK:
														Toast.makeText(getApplicationContext(), getResources().getString(R.string.highscore_submit_success) + msg.arg1, Toast.LENGTH_LONG).show();
														if (progressDialog != null) {
															progressDialog.dismiss();
														}
														break;
													case HANDLER_SUBMIT:
														// submit
														progressDialog.show();
														HighscoreSubmission m = new HighscoreSubmission();
														// name again
														String storedName = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(
																getResources().getString(R.string.preferences_username), getResources().getString(R.string.preferences_username_default));
														m.submitScore(getApplicationContext(), mGame.getData(), mHandler, storedName);
														break;
													default:
														break;
												}
											}
										};

	private void submitScore() {
		if (!mGame.ismIsOver()) {
			Toast.makeText(this, getResources().getString(R.string.highscore_submit_game_not_over), Toast.LENGTH_SHORT).show();
			return;
		}

		// check name
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.preferences_username_title);
		alert.setMessage(R.string.preferences_username_summary);
		alert.setCancelable(false);
		final EditText input = new EditText(this);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String storedName = sp.getString(getResources().getString(R.string.preferences_username), getResources().getString(R.string.preferences_username_default));
		input.setText(storedName);

		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();

				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				String storedName = sp.getString(getResources().getString(R.string.preferences_username), getResources().getString(R.string.preferences_username_default));
				input.setText(storedName);

				if (!value.equals(storedName)) {
					Editor e = sp.edit();
					e.putString(getResources().getString(R.string.preferences_username), value);
					e.commit();
				}

				// trigger submitting
				mHandler.obtainMessage(HANDLER_SUBMIT).sendToTarget();
			}
		});

		alert.show();
	}

	private class GameLooper extends Thread {
		public Handler	mHandler	= new Handler() {

										public void handleMessage(Message msg) {
											super.handleMessage(msg);
											stop = true;
										}
									};
		public boolean	stop		= false;

		public void run() {
			/* Game loop */
			while (!mGame.ismIsOver() && !stop) {
				try {
					/* Update game */
					mGame.addAllToMEvents(mUserInputActionEvents);
					mUserInputActionEvents.clear();
					mGame.update();

					/* Resting game */
					if (!mGame.ismIsPaused()) {
						Thread.sleep(WAIT_DELAY);
					} else {
						Thread.sleep(WAIT_PAUSE_DELAY);
					}
				} catch (java.lang.InterruptedException e) {
					/* Ignore */
				}
			}
			// Log.i(DEBUG_TAG, "GameLooper: while finished.");
		}
	}

	private void repaint() {
		// stats
		Stats stats = mGame.getmStats();
		((TextView) findViewById(R.id.playgroundLevel)).setText(getFormattedNumber(stats.getLevel()));
		((TextView) findViewById(R.id.playgroundLines)).setText(getFormattedNumber(stats.getLines()));
		((TextView) findViewById(R.id.playgroundPieces)).setText(getFormattedNumber(stats.getTotalPieces()));
		((TextView) findViewById(R.id.playgroundScore)).setText(getFormattedNumber(stats.getScore()));

		// next piece
		if (mGame.ismShowPreview()) {
			SurfaceView v = (SurfaceView) findViewById(R.id.surfaceView1);
			float w = v.getWidth() / Tetromino.dimension;
			float h = v.getHeight() / Tetromino.dimension;

			Canvas canvas = v.getHolder().lockCanvas();
			if (canvas != null) {

				canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

				Tetromino tetromino = mGame.getmNextBlock();
				TetrominoType[][] data = tetromino.getData();
				Paint p = GameBoardView.getPaintForColor(mGame.getmNextBlock().getType().getColor());

				for (int x = 0; x < tetromino.size(); x++) {
					for (int y = 0; y < tetromino.size(); y++) {
						if (data[x][y] != null) {
							float l = x * w + 1;
							float r = l + w - 1;
							float t = y * h + 1;
							float b = t + h - 1;
							canvas.drawRect(l, t, r, b, p);
						}// if
					}// for
				}// for

				v.getHolder().unlockCanvasAndPost(canvas);
			}
			/*
			 * ((TextView) findViewById(R.id.playgroundNext)).setText(mGame
			 * .getmNextBlock().getType().toString());
			 */
		} /*
		 * else { ((TextView) findViewById(R.id.playgroundNext))
		 * .setText(getResources().getString(R.string.game_view_dummy)); }
		 */

		// invalidate canvas
		findViewById(R.id.view1).invalidate();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	public boolean onMenuOpened(int featureId, Menu menu) {
		lockPause(true);

		menu.findItem(R.id.menuSubmitScore).setEnabled(mGame.ismIsOver());

		return super.onMenuOpened(featureId, menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (mGame.ismIsPaused() && !mPausedFlag) {
			mUserInputActionEvents.add(Event.PAUSE);
		}
		Intent i;
		switch (item.getItemId()) {
			case R.id.menuAbout:
				i = new Intent(this, About.class);
				startActivity(i);
				break;
			case R.id.menuHighscore:
				i = new Intent(this, Highscores.class);
				startActivity(i);
				break;
			case R.id.menuSubmitScore:
				submitScore();
				break;
			case R.id.menuDetailedStats:
				i = new Intent(this, DetailedStats.class);
				i.putExtra("data", (Parcelable) mGame.getmStats());
				i.putExtra("extended_set", mGame.getData().isExtendedSet());

				startActivity(i);
				break;
			case R.id.menuRestart:
				// deprecated
				// mUserInputActionEvents.add(Event.EVENT_RESTART);
				restart();

				break;
			case R.id.menuPreferences:
				// start intent
				i = new Intent(this, PreferencesActivity.class);
				startActivityForResult(i, REQUEST_PREFERENCES);
			default:
				// nothing
		}
		return super.onMenuItemSelected(featureId, item);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("preferences_game_board_y") || key.equals("preferences_game_board_x")) {
			/*
			 * SharedPreferences sp = PreferenceManager
			 * .getDefaultSharedPreferences(this); int h =
			 * Integer.parseInt(sp.getString("preferences_game_board_y",
			 * Integer.toString(Game.default_BOARD_TILEMAP_HEIGHT))); int w =
			 * Integer.parseInt(sp.getString("preferences_game_board_x",
			 * Integer.toString(Game.default_BOARD_TILEMAP_WIDTH)));
			 * 
			 * setupGame(w, h);
			 */
			Toast.makeText(this, getResources().getString(R.string.preferences_game_board_changed_info), Toast.LENGTH_LONG).show();
		} else if (key.equals(getResources().getString(R.string.preferences_wallkicks_enabled))) {
			boolean val = sharedPreferences.getBoolean(key, false);
			mGame.setmWallkicks(val);
		} else if (key.equals(getResources().getString(R.string.preferences_show_shadow))) {
			boolean val = sharedPreferences.getBoolean(key, false);
			mGame.setmShowShadow(val);
		} else if (key.equals(getResources().getString(R.string.preferences_show_next))) {
			boolean val = sharedPreferences.getBoolean(key, false);
			mGame.setmShowPreview(val);
		} else if (key.equals(getResources().getString(R.string.preferences_extended_block_set))) {
			Toast.makeText(this, getResources().getString(R.string.preferences_game_board_changed_info), Toast.LENGTH_LONG).show();
		} else if (key.equals(getResources().getString(R.string.preferences_username))) {
			String name = sharedPreferences.getString(key, getResources().getString(R.string.preferences_username_default)).trim();
			if (name.length() > MAX_NAME_LENGTH) {
				name = name.substring(0, MAX_NAME_LENGTH - 1);
				Editor e = sharedPreferences.edit();
				e.putString(key, name);
				e.commit();
			}
		} else if (key.equals(getResources().getString(R.string.preferences_control_buttons))) {
			initControls();
		} else if (key.equals(getResources().getString(R.string.preferences_control_buttons_continous))) {
			initControls();
		} else if (key.equals(getResources().getString(R.string.preferences_control_gestures))) {
			initControls();
		} else if (key.equals(getResources().getString(R.string.preferences_control_trackball))) {
			initControls();
		}
	}

	/**
	 * @param v
	 *            <code>true</code> to DISABLE, <code>false</code> to ENABLE
	 */
	private void disableButtons(boolean v) {
		if (v == true) {
			((ViewGroup) findViewById(R.id.game_layout_control_relative_layout)).setVisibility(View.GONE);
		} else {
			((ViewGroup) findViewById(R.id.game_layout_control_relative_layout)).setVisibility(View.VISIBLE);
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		// log.i(DEBUG_TAG, "onSaveInstanceState");
		// super.onSaveInstanceState(outState);
		outState.putSerializable("data", mGame.getData());
		outState.putBoolean("pauseFlag", mPausedFlag);
		super.onSaveInstanceState(outState);

		mGameLooper.mHandler.obtainMessage().sendToTarget();
	}

	public void onBackPressed() {
		if (!mGame.ismIsPaused()) {
			mUserInputActionEvents.add(Event.PAUSE);
		}

		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		boolean ask = sp.getBoolean(getResources().getString(R.string.preferences_interface_get_asked_at_back), true);

		if (ask == false) {
			finish();
			return;
		}

		AlertDialog.Builder quitDialog = new AlertDialog.Builder(this);
		quitDialog.setTitle(R.string.quit_confirm_title);
		// quitDialog.setMessage(R.string.quit_confirm_message);

		LayoutInflater eulaInflater = LayoutInflater.from(this);
		View eulaLayout = eulaInflater.inflate(R.layout.layout_dialog_with_checkbox, null);
		CheckBox dontShowAgain = (CheckBox) eulaLayout.findViewById(R.id.layout_dialog_with_checkbox_checkbox);

		dontShowAgain.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor e = sp.edit();
				e.putBoolean(getResources().getString(R.string.preferences_interface_get_asked_at_back), !isChecked);
				e.commit();
			}
		});

		dontShowAgain.setText(R.string.global_do_not_show_again);

		((TextView) eulaLayout.findViewById(R.id.layout_dialog_with_checkbox_textview)).setText(R.string.quit_confirm_message);
		quitDialog.setView(eulaLayout);

		quitDialog.setPositiveButton(R.string.global_yes, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});

		quitDialog.setNegativeButton(R.string.global_no, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				if (!mPausedFlag) {
					mUserInputActionEvents.add(Event.PAUSE);
				}
			}
		});

		quitDialog.show();
	}

	public void somethingChangedCallback() {
		if (mGame == null) {
			return;
		}

		/* Request redraw of screen */
		if (mGame.ismStateChanged()) {
			if (mGame.ismIsOver()) {
				// game over
				mHandler.obtainMessage(HANDLER_GAME_OVER).sendToTarget();
			} else {
				// repaint
				mHandler.obtainMessage(HANDLER_REPAINT).sendToTarget();
			}
			mGame.setmStateChanged(false);
		}
	}

	private final TetrominoType[]	tetrominoTypeStandard	= TetrominoType.getStandardSet(true);
	private final TetrominoType[]	tetrominoTypeExtended	= TetrominoType.getStandardSet(false);

	public TetrominoType getNextTetromino() {
		if (mGame == null || mGame.getData() == null || !mGame.getData().isExtendedSet()) {
			// no extended set, get standard
			int num = mRand.nextInt(tetrominoTypeStandard.length);
			return tetrominoTypeStandard[num];
		} else {
			float thres = mRand.nextFloat();
			if (thres <= TETROMINO_EXTENDED_THRES) {
				int num = mRand.nextInt(tetrominoTypeExtended.length);
				return tetrominoTypeExtended[num];
			} else {
				// extended set but thres below value, get standard
				int num = mRand.nextInt(tetrominoTypeStandard.length);
				return tetrominoTypeStandard[num];
			}
		}
	}
}
