package de.gpl.blockoulous.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import de.gpl.blockoulous.R;

public class Highscores extends Activity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.menuSubmitScore).setEnabled(false);
		menu.findItem(R.id.menuDetailedStats).setEnabled(false);
		menu.findItem(R.id.menuRestart).setEnabled(false);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
			case R.id.menuAbout:
				i = new Intent(this, About.class);
				startActivity(i);
				break;
			case R.id.menuHighscore:
				onResume();
				break;
			case R.id.menuPreferences:
				// start intent
				i = new Intent(this, PreferencesActivity.class);
				startActivity(i);
			default:
				// nothing
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		setContentView(R.layout.highscore);
		setTitle(R.string.highscore_window_title);

		WebView webview = (WebView) findViewById(R.id.webView1);

		webview.getSettings().setJavaScriptEnabled(true);

		final Activity activity = this;
		webview.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int progress) {
				// Activities and WebViews measure progress with different
				// scales.
				// The progress meter will automatically disappear when we reach
				// 100%
				activity.setProgress(progress * 1000);
			}
		});
		webview.setWebViewClient(new WebViewClient() {
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
			}
		});

		String username = PreferenceManager.getDefaultSharedPreferences(this).getString("preferences_username", getResources().getString(R.string.preferences_username_default));
		webview.loadUrl(getResources().getString(R.string.highscore_scores_url) + username);
	}

	@Override
	protected void onResume() {
		super.onResume();
		WebView webview = (WebView) findViewById(R.id.webView1);
		webview.reload();
	}

}
