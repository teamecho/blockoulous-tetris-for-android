package de.gpl.blockoulous.model;

import java.io.Serializable;
import java.util.Random;
import java.util.Set;

import android.os.Parcel;
import android.os.Parcelable;
import de.gpl.blockoulous.controller.Event;

public class GameData implements Serializable {
	private static final long serialVersionUID = -7001173870865862341L;
	private static final int NULL = -1;
	private final long nonce;
	private int BOARD_TILEMAP_WIDTH;
	private int BOARD_TILEMAP_HEIGHT;
	private int INIT_DELAY_FALL;
	private Stats mStats = new Stats();
	private Tetromino mFallingBlock = Tetromino.factory.create(TetrominoType.O); // default
	private TetrominoType[][] gameBoard;
	private int mFallingDelay;
	private boolean mIsOver;
	private Tetromino mNextBlock = Tetromino.factory.create(TetrominoType.O); // default
	private boolean mShowShadow;
	private int mShadowGap;
	private Set<Event> mEvents; // skipped for parcel
	private long mSystemTime;
	private boolean mIsPaused;
	private long mLastFallTime;
	private boolean mShowPreview;
	private boolean mStateChanged;
	private int mErrorCode;
	private boolean mWallkicks;
	private boolean extendedSet;

	/*	public static final Parcelable.Creator<GameData> CREATOR = new Parcelable.Creator<GameData>() {
		@Override
		public GameData createFromParcel(Parcel in) {
			return new GameData(in);
		}

		@Override
		public GameData[] newArray(int size) {
			return new GameData[size];
		}
	};*/

	public GameData(Parcel in) {
		initGameBoard();
		BOARD_TILEMAP_WIDTH = in.readInt();
		BOARD_TILEMAP_HEIGHT = in.readInt();
		INIT_DELAY_FALL = in.readInt();
		mStats = in.readParcelable(Stats.class.getClassLoader());
		mFallingBlock = in.readParcelable(Tetromino.class.getClassLoader());

		int[] data = new int[BOARD_TILEMAP_WIDTH * BOARD_TILEMAP_HEIGHT];
		in.readIntArray(data);
		for (int i = 0; i < BOARD_TILEMAP_WIDTH; i++) {
			for (int j = 0; j < BOARD_TILEMAP_HEIGHT; j++) {
				int val = data[i * BOARD_TILEMAP_WIDTH + BOARD_TILEMAP_HEIGHT];
				if (val != NULL) {
					gameBoard[i][j] = TetrominoType.getTypeForId(val);
				}
			}
		}

		mFallingDelay = in.readInt();
		mIsOver = (in.readInt() == 1) ? true : false;
		mNextBlock = in.readParcelable(Tetromino.class.getClassLoader());
		mShowShadow = (in.readInt() == 1) ? true : false;
		mShadowGap = in.readInt();
		// events
		mSystemTime = in.readLong();
		mIsPaused = (in.readInt() == 1) ? true : false;
		mLastFallTime = in.readLong();
		mShowPreview = (in.readInt() == 1) ? true : false;
		mStateChanged = (in.readInt() == 1) ? true : false;
		mErrorCode = in.readInt();
		mWallkicks = (in.readInt() == 1) ? true : false;
		nonce = in.readLong();
	}

	public GameData(int bOARD_TILEMAP_WIDTH, int bOARD_TILEMAP_HEIGHT,
			int iNIT_DELAY_FALL, Stats mStats) {
		initGameBoard();
		BOARD_TILEMAP_WIDTH = bOARD_TILEMAP_WIDTH;
		BOARD_TILEMAP_HEIGHT = bOARD_TILEMAP_HEIGHT;
		INIT_DELAY_FALL = iNIT_DELAY_FALL;
		this.mStats = mStats;

		Random r = new Random(System.currentTimeMillis());
		this.nonce = r.nextLong();
	}

	private void initGameBoard() {
		for (int i = 0; i < BOARD_TILEMAP_WIDTH; i++) {
			for (int j = 0; j < BOARD_TILEMAP_HEIGHT; j++) {
				gameBoard[i][j] = null;
			}
		}
	}

	public int getBOARD_TILEMAP_WIDTH() {
		return BOARD_TILEMAP_WIDTH;
	}

	public void setBOARD_TILEMAP_WIDTH(int bOARD_TILEMAP_WIDTH) {
		BOARD_TILEMAP_WIDTH = bOARD_TILEMAP_WIDTH;
	}

	public int getBOARD_TILEMAP_HEIGHT() {
		return BOARD_TILEMAP_HEIGHT;
	}

	public void setBOARD_TILEMAP_HEIGHT(int bOARD_TILEMAP_HEIGHT) {
		BOARD_TILEMAP_HEIGHT = bOARD_TILEMAP_HEIGHT;
	}

	public int getINIT_DELAY_FALL() {
		return INIT_DELAY_FALL;
	}

	public void setINIT_DELAY_FALL(int iNIT_DELAY_FALL) {
		INIT_DELAY_FALL = iNIT_DELAY_FALL;
	}

	public Stats getmStats() {
		return mStats;
	}

	public void setmStats(Stats mStats) {
		this.mStats = mStats;
	}

	public Tetromino getmFallingBlock() {
		return mFallingBlock;
	}

	public void setmFallingBlock(Tetromino mFallingBlock) {
		this.mFallingBlock = mFallingBlock;
	}

	public TetrominoType[][] getGameBoard() {
		return gameBoard;
	}

	public void setGameBoard(TetrominoType[][] gameBoard) {
		this.gameBoard = gameBoard;
	}

	public int getmFallingDelay() {
		return mFallingDelay;
	}

	public void setmFallingDelay(int mFallingDelay) {
		this.mFallingDelay = mFallingDelay;
	}

	public boolean ismIsOver() {
		return mIsOver;
	}

	public void setmIsOver(boolean mIsOver) {
		this.mIsOver = mIsOver;
	}

	public Tetromino getmNextBlock() {
		return mNextBlock;
	}

	public void setmNextBlock(Tetromino mNextBlock) {
		this.mNextBlock = mNextBlock;
	}

	public boolean ismShowShadow() {
		return mShowShadow;
	}

	public void setmShowShadow(boolean mShowShadow) {
		this.mShowShadow = mShowShadow;
	}

	public int getmShadowGap() {
		return mShadowGap;
	}

	public void setmShadowGap(int mShadowGap) {
		this.mShadowGap = mShadowGap;
	}

	public Set<Event> getmEvents() {
		return mEvents;
	}

	public void setmEvents(Set<Event> mEvents) {
		this.mEvents = mEvents;
	}

	public long getmSystemTime() {
		return mSystemTime;
	}

	public void setmSystemTime(long mSystemTime) {
		this.mSystemTime = mSystemTime;
	}

	public boolean ismIsPaused() {
		return mIsPaused;
	}

	public void setmIsPaused(boolean mIsPaused) {
		this.mIsPaused = mIsPaused;
	}

	public long getmLastFallTime() {
		return mLastFallTime;
	}

	public void setmLastFallTime(long mLastFallTime) {
		this.mLastFallTime = mLastFallTime;
	}

	public boolean ismShowPreview() {
		return mShowPreview;
	}

	public void setmShowPreview(boolean mShowPreview) {
		this.mShowPreview = mShowPreview;
	}

	public boolean ismStateChanged() {
		return mStateChanged;
	}

	public void setmStateChanged(boolean mStateChanged) {
		this.mStateChanged = mStateChanged;
	}

	public int getmErrorCode() {
		return mErrorCode;
	}

	public void setmErrorCode(int mErrorCode) {
		this.mErrorCode = mErrorCode;
	}

	public boolean ismWallkicks() {
		return mWallkicks;
	}

	public void setmWallkicks(boolean mWallkicks) {
		this.mWallkicks = mWallkicks;
	}

	// @Override
	public int describeContents() {
		return 0;
	}

	// @Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(BOARD_TILEMAP_WIDTH);
		dest.writeInt(BOARD_TILEMAP_HEIGHT);
		dest.writeInt(INIT_DELAY_FALL);
		mStats.writeToParcel(dest, flags);
		mFallingBlock.writeToParcel(dest, flags);

		// board
		int[] board = new int[BOARD_TILEMAP_HEIGHT * BOARD_TILEMAP_WIDTH];
		for (int i = 0; i < BOARD_TILEMAP_WIDTH; i++) {
			for (int j = 0; j < BOARD_TILEMAP_HEIGHT; j++) {
				if (gameBoard[i][j] != null) {
					board[i * BOARD_TILEMAP_WIDTH + j] = gameBoard[i][j]
							.getId();
				} else {
					board[i * BOARD_TILEMAP_WIDTH + j] = NULL;
				}
			}
		}
		dest.writeIntArray(board);

		dest.writeInt(mFallingDelay);
		dest.writeInt((mIsOver) ? 1 : 0);
		mNextBlock.writeToParcel(dest, flags);
		dest.writeInt((mShowShadow) ? 1 : 0);
		dest.writeInt(mShadowGap);
		// events
		dest.writeLong(mSystemTime);
		dest.writeInt((mIsPaused) ? 1 : 0);
		dest.writeLong(mLastFallTime);
		dest.writeInt((mShowPreview) ? 1 : 0);
		dest.writeInt((mStateChanged) ? 1 : 0);
		dest.writeInt(mErrorCode);
		dest.writeInt((mWallkicks) ? 1 : 0);
		dest.writeLong(nonce);
	}

	public void setExtendedSet(boolean extendedSet) {
		this.extendedSet = extendedSet;
	}

	public boolean isExtendedSet() {
		return this.extendedSet;
	}

	public long getNonce() {
		return nonce;
	}
}