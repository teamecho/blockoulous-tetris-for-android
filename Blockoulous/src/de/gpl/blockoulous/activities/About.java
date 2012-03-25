package de.gpl.blockoulous.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebView;
import de.gpl.blockoulous.R;

public class About extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.about);
		setTitle(R.string.about_window_title);

		WebView wv = new WebView(this);

		wv.loadUrl("https://github.com/tuedelue/blockoulous");

		ViewGroup v = (ViewGroup) findViewById(R.id.about_layout);
		v.addView(wv);

	}
}
