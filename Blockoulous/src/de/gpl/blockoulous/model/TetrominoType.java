package de.gpl.blockoulous.model;

import java.util.ArrayList;

import de.gpl.blockoulous.view.TetrColor;

public enum TetrominoType {
	/*
	 * Cyan I Yellow O Purple T Green S Red Z Blue J Orange L
	 */
	L(6, TetrColor.ORANGE), O(1, TetrColor.YELLOW), Z(4, TetrColor.RED), I(0,
			TetrColor.CYAN), J(5, TetrColor.BLUE), S(3, TetrColor.GREEN), T(2,
			TetrColor.PURPLE),

	UU(10, TetrColor.WHITE), XX(11, TetrColor.WHITE), VV(12, TetrColor.WHITE), MM(
			13, TetrColor.WHITE), TT(14, TetrColor.WHITE), ZZ(15,
			TetrColor.WHITE), HH(16, TetrColor.WHITE), SS(17, TetrColor.WHITE);
	private int id;
	private TetrColor c;

	public boolean isExtended() {
		if (id >= 10) {
			return true;
		}
		return false;
	}

	private TetrominoType(int id, TetrColor c) {
		this.id = id;
		this.c = c;
	}

	public TetrColor getColor() {
		return c;
	}

	public int getId() {
		return id;
	}

	public static TetrominoType[] getStandardSet(boolean yes) {
		ArrayList<TetrominoType> x = new ArrayList<TetrominoType>();
		int threshhold = 10;

		for (int i = 0; i < values().length; i++) {
			if (yes) {
				if (values()[i].getId() < threshhold) {
					x.add(values()[i]);
				}
			} else {
				if (values()[i].getId() >= threshhold) {
					x.add(values()[i]);
				}
			}
		}

		TetrominoType[] val = new TetrominoType[x.size()];
		return x.toArray(val);
	}

	public static TetrominoType getTypeForId(int id) {
		for (TetrominoType t : TetrominoType.values()) {
			if (t.getId() == id)
				return t;
		}

		return null;
	}
}
