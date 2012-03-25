package de.gpl.blockoulous.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class Stats implements Parcelable, Serializable {
	private static final long serialVersionUID = -8243596282366424342L;
	private long score;
	private int level;
	private int lines;
	private int totalPieces;
	private Map<TetrominoType, Integer> pieces = new HashMap<TetrominoType, Integer>();

	private Stats(Parcel in) {
		this.score = in.readLong();
		this.level = in.readInt();
		this.lines = in.readInt();
		this.totalPieces = in.readInt();

		Bundle piecesBundle = in.readBundle();
		TetrominoType[] types = TetrominoType.values();
		for (int i = 0; i < types.length; i++) {
			String key = Integer.toString(types[i].getId());
			int val = piecesBundle.getInt(key);
			pieces.put(types[i], val);
		}
	}

	public Stats() {

	}

	public int getTotalPieces() {
		return totalPieces;
	}

	public void setTotalPieces(int totalPieces) {
		this.totalPieces = totalPieces;
	}

	public Map<TetrominoType, Integer> getPieces() {
		return pieces;
	}

	public void setPieces(Map<TetrominoType, Integer> pieces) {
		this.pieces = pieces;
	}

	public long getScore() {
		return score;
	}

	public void setScore(long score) {
		this.score = score;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getLines() {
		return lines;
	}

	public void setLines(int lines) {
		this.lines = lines;
	}

	public void increasePieces() {
		totalPieces++;
	}

	public void increasePiece(TetrominoType type) {
		int val = 1;
		if (pieces.containsKey(type)) {
			val += pieces.get(type);
		}

		pieces.put(type, val);
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(score);
		dest.writeInt(level);
		dest.writeInt(lines);
		dest.writeInt(totalPieces);

		// pieces, more involved
		Bundle piecesBundle = new Bundle();
		TetrominoType[] types = TetrominoType.values();
		for (int i = 0; i < types.length; i++) {
			String key = Integer.toString(types[i].getId());
			if (pieces.containsKey(types[i])) {
				piecesBundle.putInt(key, pieces.get(types[i]));
			}
		}

		dest.writeBundle(piecesBundle);
	}

	public static final Parcelable.Creator<Stats> CREATOR = new Parcelable.Creator<Stats>() {
		public Stats createFromParcel(Parcel in) {
			return new Stats(in);
		}

		public Stats[] newArray(int size) {
			return new Stats[size];
		}
	};
}
