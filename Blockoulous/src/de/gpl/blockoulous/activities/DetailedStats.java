package de.gpl.blockoulous.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.gpl.blockoulous.R;
import de.gpl.blockoulous.model.Stats;
import de.gpl.blockoulous.model.Tetromino;
import de.gpl.blockoulous.model.TetrominoType;
import de.gpl.blockoulous.view.GameBoardView;

public class DetailedStats extends Activity {
	public float w = 50;
	public float h = 50;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.detailed_stats);
		setTitle(R.string.detailed_stats_window_title);

		// get data
		Stats s = getIntent().getParcelableExtra("data");

		// update view
		((TextView) findViewById(R.id.detailed_stats_level))
				.setText(TetrisActivity.getFormattedNumber(s.getLevel()));
		((TextView) findViewById(R.id.detailed_stats_lines))
				.setText(TetrisActivity.getFormattedNumber(s.getLines()));
		((TextView) findViewById(R.id.detailed_stats_score))
				.setText(TetrisActivity.getFormattedNumber(s.getScore()));
		((TextView) findViewById(R.id.detailed_stats_pieces))
				.setText(TetrisActivity.getFormattedNumber(s.getTotalPieces()));

		TableLayout table = (TableLayout) findViewById(R.id.detailed_stats_table_layout_blocks);

		// setup list to display
		boolean extended = getIntent().getBooleanExtra("extended_set", true);

		// calculate percentages
		Map<TetrominoType, Float> percentages = getPercentages(s.getPieces(),
				extended);

		// get by value sorted keys
		@SuppressWarnings("unchecked")
		List<TetrominoType> sortedList = sortByValue(percentages);
		Collections.reverse(sortedList);

		TableRow.LayoutParams columnParam0 = new TableRow.LayoutParams(0);
		TableRow.LayoutParams columnParam1 = new TableRow.LayoutParams(1);

		for (TetrominoType type : sortedList) {
			int num = 0;
			if (s.getPieces().containsKey(type)) {
				num = s.getPieces().get(type);
			}

			// row
			TableRow row = new TableRow(this);
			table.addView(row);

			// linear layout in row
			LinearLayout layout = new LinearLayout(getApplicationContext());
			layout.setLayoutParams(columnParam0);
			LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layout
					.getLayoutParams();
			params.setMargins(0, 0, 20, 0);

			// text
			TextView tv = new TextView(this);
			float percentVal = 0f;
			if (percentages.containsKey(type)) {
				percentVal = percentages.get(type);
			}
			String percentageString = Float.toString(Math
					.round(percentVal * 1000) / 10f);
			String text = TetrisActivity.getFormattedNumber(num) + " ("
					+ percentageString + "%)";
			tv.setText(text);
			tv.setLayoutParams(columnParam1);

			// tetromino-graphic
			TetroMinoView sv = new TetroMinoView(getApplicationContext());
			sv.setLayoutParams(new ViewGroup.LayoutParams(new Float(w)
					.intValue(), new Float(h).intValue()));
			sv.setType(type);

			// add to layout
			layout.addView(sv);

			row.addView(layout);
			row.addView(tv);
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static List sortByValue(Map map) {
		List<Entry> list = new ArrayList<Entry>(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		List returnval = new ArrayList();
		for (Entry e : list) {
			returnval.add(e.getKey());
		}
		return returnval;
	}

	/**
	 * return complete tetrominotype map with its percentages
	 * 
	 * @param pieces
	 * @param extended
	 * @return
	 */
	private Map<TetrominoType, Float> getPercentages(
			Map<TetrominoType, Integer> pieces, boolean extended) {
		Map<TetrominoType, Float> returnval = new HashMap<TetrominoType, Float>();

		// sum
		float total = 0;
		for (int v : pieces.values()) {
			total += v;
		}

		// percentages

		for (Entry<TetrominoType, Integer> e : pieces.entrySet()) {
			// we are not using extended set, skip.
			if (!extended && e.getKey().isExtended()) {
				continue;
			}

			if (total > 0) {
				returnval.put(e.getKey(), e.getValue() / total);
			} else {
				returnval.put(e.getKey(), 0f);
			}
		}

		// fill in those, that are not involved
		List<TetrominoType> types = new ArrayList<TetrominoType>(
				Arrays.asList((extended) ? TetrominoType.values()
						: TetrominoType.getStandardSet(true)));
		for (TetrominoType t : types) {
			if (!returnval.containsKey(t)) {
				returnval.put(t, 0f);
			}
		}

		return returnval;
	}

	private static class TetroMinoView extends View {
		public TetrominoType t;
		private final float w = 50 / Tetromino.dimension;
		private final float h = 50 / Tetromino.dimension;

		public TetroMinoView(Context context) {
			super(context);
		}

		public void setType(TetrominoType tetrominoType) {
			t = tetrominoType;
			invalidate();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			drawTetroToCanvas(canvas, t, w, h);
		}

		private void drawTetroToCanvas(Canvas c, TetrominoType type, float w,
				float h) {
			Tetromino tetro = Tetromino.factory.create(type);
			TetrominoType[][] data = tetro.getData();
			Paint p = GameBoardView.getPaintForColor(type.getColor());

			for (int x = 0; x < tetro.size(); x++) {
				for (int y = 0; y < tetro.size(); y++) {
					if (data[x][y] != null) {
						float l = x * w + 1;
						float r = l + w - 1;
						float t = y * h + 1;
						float b = t + h - 1;
						c.drawRect(l, t, r, b, p);
					}// if
				}// for
			}// for
		}
	}

}
