/*
 * An app that will manage all text on the device. By entering text
 * into the app, it can be sent anywhere.
 * Libraries used: twitter4j & Facebook SDK
 */

package com.gorecki.textmanager;

import java.util.ArrayList;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.BaseRequestListener;
import com.facebook.android.Facebook;
import com.facebook.android.SessionStore;
import com.hintdesk.core.util.StringUtil;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class MainActivity extends Activity {

	// Declare constants
	static final String LOG = "MainActivity";
	private TabHost myTabHost;
	EditText mainEditText;
	ListView shareListView, draftListView;
	private Facebook mFacebook;
	private Handler mRunOnUi = new Handler();
	private static final String APP_ID = "APP_ID";
	ArrayList<String> listItems = new ArrayList<String>();
	ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// set up environment
		mainEditText = (EditText) findViewById(R.id.mainEditText);
		shareListView = (ListView) findViewById(R.id.shareListView);
		draftListView = (ListView) findViewById(R.id.draftsListView);

		// facebook init
		mFacebook = new Facebook(APP_ID);
		SessionStore.restore(mFacebook, this);

		// set screen tabs
		initTabs();
		
		// set editview screen size
		setScreenSize();

		// set the list view for share list
		setShareListView();
		// setDraftsListView();

		setEditTextToKeepFocus();

		// if twitter is authenticated, run initControl()
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		if (sharedPreferences.getBoolean(
				ConstantValues.PREFERENCE_TWITTER_IS_LOGGED_IN, true)) {
			initControl();

		}
		
		// share viewlist clickable
		shareListView.setClickable(true);
		shareListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				switch (position) {
				case 0:
					copyToClipboard();
					break;
				case 1:
					textMessage();
					break;
				case 2:
					email();
					break;
				case 3:
					if (checkInternetConnection())
						updateStatus();
					break;
				case 4:
					String status = mainEditText.getText().toString();
					if (checkInternetConnection())
						postToFacebook(status);

				}

			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// Add the two tabs, share and drafts
	public void initTabs() {
		myTabHost = (TabHost) findViewById(R.id.TabHost01);
		myTabHost.setup();
		TabSpec spec = myTabHost.newTabSpec("tab_creation");
		spec.setIndicator("Share",
				getResources().getDrawable(android.R.drawable.ic_menu_add));
		spec.setContent(R.id.shareTab);
		myTabHost.addTab(spec);
		/*
		 * myTabHost.addTab(myTabHost .newTabSpec("tab_inser") .setIndicator(
		 * "Drafts", getResources().getDrawable(
		 * android.R.drawable.ic_menu_edit)) .setContent(R.id.draftsTab));
		 */
	}

	// set edit text screen size
	public void setScreenSize() {
		// Get the device height and set the main text edit to half that
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int height = size.y;
		int half = height / 2;
		mainEditText.setHeight(half);
	}

	// set share list view
	public void setShareListView() {
		// share listview
		String shareArray[] = { "Copy To Clipboard", "Text Message", "E-Mail",
				"Tweet", "Facebook" };

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				getApplicationContext(), android.R.layout.simple_list_item_1,
				shareArray);
		shareListView.setAdapter(adapter);

	}

	// set draft list view
	public void setDraftsListView() {
		// drafts list view
		String arr2[] = { "option 1", "option 2" };

		ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(
				getApplicationContext(), android.R.layout.simple_list_item_1,
				arr2);
		draftListView.setAdapter(adapter2);
	}

	// hack to squash bug to keep focus on edit text
	public void setEditTextToKeepFocus() {
		// set the edittext to keep focus
		myTabHost
				.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {

					@Override
					public void onViewDetachedFromWindow(View v) {
					}

					@Override
					public void onViewAttachedToWindow(View v) {
						myTabHost.getViewTreeObserver()
								.removeOnTouchModeChangeListener(myTabHost);
					}
				});
	}

	// copy to clipboard
	public void copyToClipboard() {
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("label", mainEditText.getText()
				.toString());
		clipboard.setPrimaryClip(clip);
		Toast.makeText(getApplicationContext(), "Text copied to clipboard",
				Toast.LENGTH_SHORT).show();
	}

	// text message
	public void textMessage() {
		Intent sendIntent = new Intent(Intent.ACTION_VIEW);
		sendIntent.putExtra("sms_body", mainEditText.getText().toString());
		sendIntent.setType("vnd.android-dir/mms-sms");
		startActivity(sendIntent);
	}

	// email
	public void email() {
		Intent email = new Intent(Intent.ACTION_SEND);
		email.putExtra(Intent.EXTRA_TEXT, mainEditText.getText().toString());
		email.setType("message/rfc822");
		startActivity(Intent.createChooser(email, "Choose an Email client :"));
	}

	// facebook status update
	public void facebookUpdate() {
		String statusUpdate = mainEditText.getText().toString();

		if (statusUpdate.equals(""))
			return;
		else
			postToFacebook(statusUpdate);

	}

	// check if device has internet
	public boolean checkInternetConnection() {
		boolean status = false;
		try {
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = cm.getNetworkInfo(0);
			if (netInfo != null
					&& netInfo.getState() == NetworkInfo.State.CONNECTED) {
				status = true;
			} else {
				netInfo = cm.getNetworkInfo(1);
				if (netInfo != null
						&& netInfo.getState() == NetworkInfo.State.CONNECTED)
					status = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		// display in long period of time
		return status;
	}

	// twitter api calls
	public void initControl() {
		Uri uri = getIntent().getData();
		if (uri != null
				&& uri.toString().startsWith(
						ConstantValues.TWITTER_CALLBACK_URL)) {
			String verifier = uri
					.getQueryParameter(ConstantValues.URL_PARAMETER_TWITTER_OAUTH_VERIFIER);
			new TwitterGetAccessTokenTask().execute(verifier);
		} else
			new TwitterGetAccessTokenTask().execute("");
	}

	// update twitter status
	public void updateStatus() {
		String status = mainEditText.getText().toString();
		if (!StringUtil.isNullOrWhitespace(status)) {
			new TwitterUpdateStatusTask().execute(status);
		} else {
			Toast.makeText(getApplicationContext(), "Please enter a status",
					Toast.LENGTH_SHORT).show();
		}

	}

	// asynctask to get twitter access token
	class TwitterGetAccessTokenTask extends AsyncTask<String, String, String> {

		@Override
		protected void onPostExecute(String userName) {
			// textViewUserName.setText(Html.fromHtml("<b> Welcome " + userName
			// + "</b>"));
		}

		@Override
		protected String doInBackground(String... params) {
			Twitter twitter = TwitterUtil.getInstance().getTwitter();
			RequestToken requestToken = TwitterUtil.getInstance()
					.getRequestToken();
			Log.v(LOG, "requestToken " + requestToken);
			if (!StringUtil.isNullOrWhitespace(params[0])) {
				try {
					AccessToken accessToken = twitter.getOAuthAccessToken(
							requestToken, params[0]);
					SharedPreferences sharedPreferences = PreferenceManager
							.getDefaultSharedPreferences(getApplicationContext());
					SharedPreferences.Editor editor = sharedPreferences.edit();
					editor.putString(
							ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN,
							accessToken.getToken());
					editor.putString(
							ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN_SECRET,
							accessToken.getTokenSecret());
					editor.putBoolean(
							ConstantValues.PREFERENCE_TWITTER_IS_LOGGED_IN,
							true);
					editor.commit();
					return twitter.showUser(accessToken.getUserId()).getName();
				} catch (TwitterException e) {
					e.printStackTrace(); // To change body of catch statement
											// use File | Settings | File
											// Templates.
				}
			} else {
				SharedPreferences sharedPreferences = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				String accessTokenString = sharedPreferences.getString(
						ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN, "");
				String accessTokenSecret = sharedPreferences.getString(
						ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN_SECRET,
						"");
				AccessToken accessToken = new AccessToken(accessTokenString,
						accessTokenSecret);
				try {
					TwitterUtil.getInstance().setTwitterFactory(accessToken);
					return TwitterUtil.getInstance().getTwitter()
							.showUser(accessToken.getUserId()).getName();
				} catch (TwitterException e) {
					e.printStackTrace();
				}
			}

			return null;
		}
	}

	// asynctask for twitter update status
	class TwitterUpdateStatusTask extends AsyncTask<String, String, Boolean> {

		@Override
		protected void onPostExecute(Boolean result) {
			if (result)
				Toast.makeText(getApplicationContext(), "Tweet successfully",
						Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(getApplicationContext(), "Tweet failed",
						Toast.LENGTH_SHORT).show();
		}

		@Override
		protected Boolean doInBackground(String... params) {
			try {
				SharedPreferences sharedPreferences = PreferenceManager
						.getDefaultSharedPreferences(getApplicationContext());
				String accessTokenString = sharedPreferences.getString(
						ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN, "");
				Log.v(LOG, "access token " + accessTokenString);
				String accessTokenSecret = sharedPreferences.getString(
						ConstantValues.PREFERENCE_TWITTER_OAUTH_TOKEN_SECRET,
						"");
				Log.v(LOG, "access token " + accessTokenSecret);

				if (!StringUtil.isNullOrWhitespace(accessTokenString)
						&& !StringUtil.isNullOrWhitespace(accessTokenSecret)) {
					AccessToken accessToken = new AccessToken(
							accessTokenString, accessTokenSecret);

					twitter4j.Status status = TwitterUtil.getInstance()
							.getTwitterFactory().getInstance(accessToken)
							.updateStatus(params[0]);
					return true;
				}

			} catch (TwitterException e) {
				e.printStackTrace();
			}
			return false;

		}
	}

	// post to facebook
	private void postToFacebook(String status) {
		AsyncFacebookRunner mAsyncFbRunner = new AsyncFacebookRunner(mFacebook);

		Bundle params = new Bundle();

		params.putString("message", status);

		mAsyncFbRunner.request("me/feed", params, "POST",
				new WallPostListener());
	}

	// facebook post
	private final class WallPostListener extends BaseRequestListener {
		public void onComplete(final String response) {
			mRunOnUi.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(MainActivity.this, "Posted Successfully",
							Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

}
