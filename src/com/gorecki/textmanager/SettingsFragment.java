package com.gorecki.textmanager;

import org.json.JSONObject;
import org.json.JSONTokener;

import twitter4j.auth.RequestToken;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.SessionStore;

public class SettingsFragment extends PreferenceFragment

{
	// Preference twitter login;
	private Preference myPreferenceTwitter;
	// Preference facebook login
	private Preference myPreferenceFacebook;
	// log constant for debugging
	static final String LOG = "SettingsFragment";
	
	// constants
	private Facebook mFacebook;
	private static final String[] PERMISSIONS = new String[] {"publish_stream", "read_stream", "offline_access"};
	private static final String APP_ID = "APP_ID";
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.settings);
		
		// facebook init
		mFacebook = new Facebook(APP_ID);
		SessionStore.restore(mFacebook, getActivity());
		
		// twitter preference click
		myPreferenceTwitter = findPreference("twitter");
        myPreferenceTwitter.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference arg0) {
            	twitterLoginClick();
                return false;
            }
        });
        
        // facebook preference click
        myPreferenceFacebook = findPreference("facebook");
        myPreferenceFacebook.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference arg0) {
            	mFacebook.authorize(getActivity(), PERMISSIONS, -1, new FbLoginDialogListener());
                return false;
            }
        });
        

	}
	
	// twitter asynctask to authenticate user
	class TwitterAuthenticateTask extends AsyncTask<String, String, RequestToken> {

        @Override
        protected void onPostExecute(RequestToken requestToken) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL()));
            startActivity(intent);
        }

        @Override
        protected RequestToken doInBackground(String... params) {
            return TwitterUtil.getInstance().getRequestToken();
        }
    }
	
	// login twitter user
	public void twitterLoginClick()
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        if (!sharedPreferences.getBoolean(ConstantValues.PREFERENCE_TWITTER_IS_LOGGED_IN,false))
        {
            new TwitterAuthenticateTask().execute();
        }
        else
        {
        	startActivity(new Intent(getActivity().getApplicationContext(), MainActivity.class));
        }
	}
	
    // login facebook user
    private final class FbLoginDialogListener implements DialogListener {
        public void onComplete(Bundle values) {
            SessionStore.save(mFacebook, getActivity());
			startActivity(new Intent(getActivity().getApplicationContext(), MainActivity.class));
        }

        public void onFacebookError(FacebookError error) {
           Toast.makeText(getActivity().getApplicationContext(), "Facebook connection failed", Toast.LENGTH_SHORT).show();
           
        }
        
        public void onError(DialogError error) {
        	Toast.makeText(getActivity().getApplicationContext(), "Facebook connection failed", Toast.LENGTH_SHORT).show(); 
        	
        }

        public void onCancel() {
        	
        }
    }
	
    // get facebook user credentials
	private Handler mFbHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			if (msg.what == 0) {
				String username = (String) msg.obj;
		        username = (username.equals("")) ? "No Name" : username;
		            
		        SessionStore.saveName(username, getActivity());
		        
		         
		        Toast.makeText(getActivity().getApplicationContext(), "Connected to Facebook as " + username, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getActivity().getApplicationContext(), "Connected to Facebook", Toast.LENGTH_SHORT).show();
			}
		}
	};
	
	// alert user if facebook login was successful
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			if (msg.what == 1) {
				Toast.makeText(getActivity().getApplicationContext(), "Facebook logout failed", Toast.LENGTH_SHORT).show();
			} else {	        	   
				Toast.makeText(getActivity().getApplicationContext(), "Disconnected from Facebook", Toast.LENGTH_SHORT).show();
			}
		}
	};
}