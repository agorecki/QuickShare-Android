package com.gorecki.textmanager;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;

public class SettingsActivity extends Activity {

	// declare constants
	Preference twitterLogin;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment()).commit();

	}

}
