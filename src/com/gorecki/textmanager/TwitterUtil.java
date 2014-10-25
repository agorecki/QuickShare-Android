package com.gorecki.textmanager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public final class TwitterUtil {

	// declare constants
	private RequestToken requestToken = null;
	private TwitterFactory twitterFactory = null;
	private Twitter twitter;
	boolean authenticated;
	public Activity activity;
	
	public TwitterUtil() {

		// configuration builder holds twitter consumer key and consumer secret
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.setOAuthConsumerKey(ConstantValues.TWITTER_CONSUMER_KEY);
		configurationBuilder.setOAuthConsumerSecret(ConstantValues.TWITTER_CONSUMER_SECRET);
		Configuration configuration = configurationBuilder.build();
		twitterFactory = new TwitterFactory(configuration);
		twitter = twitterFactory.getInstance();
		
	}

	// get twitterfactory
	public TwitterFactory getTwitterFactory() {
		return twitterFactory;
	}

	// set twitterfactory
	public void setTwitterFactory(AccessToken accessToken) {
		twitter = twitterFactory.getInstance(accessToken);
	}

	// get twitter
	public Twitter getTwitter() {
		return twitter;
	}

	// request access token
	public RequestToken getRequestToken() {
		if (requestToken == null) {
			try {
				requestToken = twitterFactory.getInstance().getOAuthRequestToken(ConstantValues.TWITTER_CALLBACK_URL);
				authenticated = true;
			} catch (TwitterException e) {
				e.printStackTrace(); // To change body of catch statement use
										// File | Settings | File Templates.
			}
		}
		return requestToken;
	}

	// declare instance
	static TwitterUtil instance = new TwitterUtil();

	// get twitterutil instance
	public static TwitterUtil getInstance() {
		return instance;
	}

	// reset instance
	public void reset() {
		instance = new TwitterUtil();
	}
}
