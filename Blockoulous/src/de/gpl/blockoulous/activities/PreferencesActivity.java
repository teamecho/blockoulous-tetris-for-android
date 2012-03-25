package de.gpl.blockoulous.activities;

import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import de.gpl.blockoulous.R;

public class PreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		new Handler().post(new Runnable() {
			public void run() {
				addPreferencesFromResource(R.xml.preferences);
			}
		});
	}

	@Override
	protected void onStop() {
		super.onStop();
		finish();
	}

}
