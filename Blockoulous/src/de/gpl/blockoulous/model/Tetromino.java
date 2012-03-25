package de.gpl.blockoulous.model;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class Tetromino implements Parcelable, Serializable {
	private static final long serialVersionUID = -3190240273646231066L;

	public final static int dimension = 4;

	private static final int NULL = -1;

	/**
	 * first dimension is X second dimension is Y
	 */
	private TetrominoType[][] data = new TetrominoType[dimension][dimension];

	private final TetrominoType type;

	private int x, y;

	private Tetromino(Parcel in) {
		int[] data = new int[dimension * dimension];

		in.readIntArray(data);
		for (int i = 0; i < dimension; i++) {
			for (int j = 0; j < dimension; j++) {
				int id = data[i * dimension + j];
				if (id != NULL) {
					this.data[i][j] = TetrominoType.getTypeForId(id);
				}
			}
		}

		type = TetrominoType.getTypeForId(in.readInt());
		x = in.readInt();
		y = in.readInt();
	}

	/**
	 * use factory.
	 */
	private Tetromino(TetrominoType t, TetrominoType[][] data) {
		if (data.length != dimension || data[0].length != dimension) {
			throw new IllegalArgumentException("dimensions do not match");
		}

		this.data = data;
		this.type = t;
	}

	public static class factory {
		public static Tetromino create(TetrominoType type) {
			TetrominoType[][] data = new TetrominoType[dimension][dimension];

			switch (type) {
			/**
			 * <pre>
			 * <code>
			 * ....<br>
			 * ####<br>
			 * ....<br>
			 * ....
			 * </code>
			 * </pre>
			 */
			case I:
				data[0][1] = type;
				data[1][1] = type;
				data[2][1] = type;
				data[3][1] = type;
				break;
			/**
			 * <pre>
			 * <code>
			 * ##..<br>
			 * ##..<br>
			 * ....<br>
			 * ....
			 * </code>
			 * </pre>
			 */
			case O:
				data[0][0] = type;
				data[0][1] = type;
				data[1][0] = type;
				data[1][1] = type;
				break;
			/**
			 * <pre>
			 * <code>
			 * .#..<br>
			 * ###.<br>
			 * ....<br>
			 * ....
			 * </code>
			 * </pre>
			 */
			case T:
				data[1][0] = type;
				data[0][1] = type;
				data[1][1] = type;
				data[2][1] = type;
				break;
			/**
			 * <pre>
			 * <code>
			 * ##..<br>
			 * .##.<br>
			 * ....<br>
			 * ....
			 * </code>
			 * </pre>
			 */
			case Z:
				data[0][0] = type;
				data[1][0] = type;
				data[1][1] = type;
				data[2][1] = type;
				break;
			/**
			 * <pre>
			 * <code>
			 * .##.<br>
			 * ##..<br>
			 * ....<br>
			 * ....
			 * </code>
			 * </pre>
			 */
			case S:
				data[1][0] = type;
				data[2][0] = type;
				data[0][1] = type;
				data[1][1] = type;
				break;
			/**
			 * <pre>
			 * <code>
			 * #...<br>
			 * ###.<br>
			 * ....<br>
			 * ....
			 * </code>
			 * </pre>
			 */
			case J:
				data[0][0] = type;
				data[0][1] = type;
				data[1][1] = type;
				data[2][1] = type;
				break;
			/**
			 * <pre>
			 * <code>
			 * ..#.<br>
			 * ###.<br>
			 * ....<br>
			 * ....
			 * </code>
			 * </pre>
			 */
			case L:
				data[2][0] = type;
				data[0][1] = type;
				data[1][1] = type;
				data[2][1] = type;
				break;

			case UU:
				data[0][0] = type;
				data[0][1] = type;
				data[1][1] = type;
				data[2][0] = type;
				data[2][1] = type;
				break;
			case XX:
				data[0][0] = type;
				data[0][1] = type;
				data[1][1] = type;
				data[2][1] = type;
				data[2][2] = type;
				break;
			case HH:
				data[0][0] = type;
				data[2][0] = type;
				data[0][1] = type;
				data[1][1] = type;
				data[2][1] = type;
				data[0][2] = type;
				data[2][2] = type;
				break;
			case MM:
				data[1][0] = type;
				data[2][0] = type;
				data[0][1] = type;
				data[1][1] = type;
				data[2][1] = type;
				data[3][1] = type;
				break;
			case SS:
				data[2][0] = type;
				data[0][1] = type;
				data[1][1] = type;
				data[2][1] = type;
				data[0][2] = type;
				break;
			case TT:
				data[0][0] = type;
				data[1][0] = type;
				data[2][0] = type;
				data[1][1] = type;
				data[1][2] = type;
				data[1][3] = type;
				break;
			case VV:
				data[0][0] = type;
				data[1][0] = type;
				data[2][0] = type;
				data[0][1] = type;
				data[0][2] = type;
				break;
			case ZZ:
				data[0][0] = type;
				data[0][1] = type;
				data[1][1] = type;
				data[2][1] = type;
				data[2][2] = type;
				break;

			default:
				// fucked up.
				throw new IllegalArgumentException("unknown tetromino type");
			}// switch

			return new Tetromino(type, data);
		}// create
	}// class

	@Override
	public String toString() {
		String val = "";
		for (int y = 0; y < dimension; y++) {
			for (int x = 0; x < dimension; x++) {
				val += (data[x][y] != null) ? "#" : ".";
			}// for
			val += "\n";
		}// for

		return val;
	}// toString

	/**
	 * @return
	 */
	public TetrominoType[][] getData() {
		return data;
	}

	/**
	 * @param data
	 */
	public void setData(TetrominoType[][] data) {
		this.data = data;
	}

	/**
	 * @return
	 */
	public TetrominoType getType() {
		return type;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int size() {
		switch (type) {
		case MM:
		case TT:
		case I:
			return 4;
		case O:
			return 2;

		case HH:
		case SS:
		case UU:
		case VV:
		case XX:
		case ZZ:
		case J:
		case L:
		case S:
		case T:
		case Z:
		default:
			return 3;
		}
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		int[] dataArray = new int[dimension * dimension];
		for (int i = 0; i < dimension; i++) {
			for (int j = 0; j < dimension; j++) {
				if (data[i][j] != null) {
					dataArray[i * dimension + j] = data[i][j].getId();
				} else {
					dataArray[i * dimension + j] = NULL;
				}
			}
		}

		dest.writeIntArray(dataArray);
		dest.writeInt(type.getId());
		dest.writeInt(x);
		dest.writeInt(y);
	}

	public static final Parcelable.Creator<Tetromino> CREATOR = new Parcelable.Creator<Tetromino>() {
		public Tetromino createFromParcel(Parcel in) {
			return new Tetromino(in);
		}

		public Tetromino[] newArray(int size) {
			return new Tetromino[size];
		}
	};
}// class
