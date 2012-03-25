package de.gpl.blockoulous.view;

public enum TetrColor {
	EMPTY(0, 0, 0), CYAN(0, 255, 255), RED(255, 0, 0), BLUE(0, 0, 255), ORANGE(
			255, 122, 0), GREEN(0, 255, 0), YELLOW(255, 255, 0), PURPLE(255, 0,
			255), WHITE(255, 255, 255), GRID(150, 150, 150), SHADOW(200, 200,
			200);

	public int r, g, b;

	TetrColor(int r, int g, int b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}
}
