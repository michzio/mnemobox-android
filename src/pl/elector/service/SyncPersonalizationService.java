/**
 * 
 */
package pl.elector.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.Gson;

import pl.elector.database.ForgottenNotSyncedProvider;
import pl.elector.database.ForgottenProvider;
import pl.elector.database.LearnedWordsNotSyncedProvider;
import pl.elector.database.LearnedWordsProvider;
import pl.elector.database.LearningHistoryProvider;
import pl.elector.database.RememberMeNotSyncedProvider;
import pl.elector.database.RememberMeProvider;
import pl.elector.service.WordsLoaderService.WordsDownloader;
import pl.electoroffline.CustomHttpClient;
import pl.electoroffline.GetHistoryFromXML;
import pl.electoroffline.GetUserForgottenFromXML;
import pl.electoroffline.GetUserForgottenFromXML.ForgottenWord;
import pl.electoroffline.GetWordsListFromXML;
import pl.electoroffline.MainActivity;
import pl.electoroffline.NetworkUtilities;
import pl.electoroffline.Preferences;
import pl.electoroffline.R;
import pl.electoroffline.SettingsFragment;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * @author Michał Ziobro
 *
 */
public class SyncPersonalizationService extends IntentService {
	
	public static final String PERSONALIZATION_SYNCED_BROADCAST = "pl.elector.action.PERSONALIZATION_SYNCED_BROADCAST"; 
	public static final int NOTIFICATION_ID = 1; 

	
	// Handler enables posting task back to main UI thread
	private Handler handler; 
	
	// Notificatin objects used to inform user about online synchronization
	private NotificationCompat.Builder notificationBuilder; 
	private NotificationManager notificationManager;
	
	private int profileId; 
	private String email; 
	private String pass; 
	private String nativeCode;
	private String foreignCode; 
	
	/**
	 * constructor that passes name parameter to the superclass 
	 */
	public SyncPersonalizationService() {
		super("SyncPersonalizationService");
	}
	
	public SyncPersonalizationService(String name) {
		super(name);		
	}
	
	/**
	 * Actions performed when the service is created
	 */
	@Override
	public void onCreate() {
		
		super.onCreate(); 
		
		nativeCode = Preferences.getAccountPreferences(this)
				.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, getString(R.string.native_code_lower)); 
		foreignCode = Preferences.getAccountPreferences(this)
				.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, getString(R.string.foreign_code_lower));
		
		profileId = Preferences.getInt(this, Preferences.KEY_PROFILE_ID, 0);
		
		handler = new Handler(); 
		
		// getting NotificationManager and building Notification using 
		// NotificationBuilder
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationBuilder = new NotificationCompat.Builder(this);
		notificationBuilder.setSmallIcon(R.drawable.favicon)
		   				   .setTicker("Syncing words personalizations...")
		   				   .setWhen(System.currentTimeMillis())
		   				   .setContentTitle(getString(R.string.syncing_personalizations))
		   				   .setProgress(0, 0, true) // operation of indeterminate length
		   				   .setOngoing(true);
		
		Intent notificationIntent = new Intent(this, MainActivity.class);  
	    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,   
	            PendingIntent.FLAG_UPDATE_CURRENT);  
	    notificationBuilder.setContentIntent(contentIntent);  

	}

	/** (non-Javadoc)
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 * This handler occurs on the background thread. 
	 * Here should be performed time consuming tasks. 
	 * Each Intent supplied to this IntentSevice 
	 * will be processed consecutively. 
	 * When all incoming Intents have been processed the Service will terminate itself. 
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		
		Log.w(SyncPersonalizationService.class.getName(), "Executing Sync Service..."); 
		
		if(!NetworkUtilities.haveNetworkConnection(this))
		{
			Log.w(SyncPersonalizationService.class.getName(), "No network connection, personalizations cannot be synced."); 
			stopSelf(); 
			return; 
		}
		
		if(profileId == 0) {
			Log.w(SyncPersonalizationService.class.getName(), "User is not logged in (profileId = 0), personalization cannot be synced.");
			stopSelf();
			return; 
		}
		email = Preferences.getString(this, Preferences.KEY_EMAIL, ""); 
		pass = Preferences.getString(this, Preferences.KEY_SHA1_PASSWORD, ""); 
		if(email.equals("") || pass.equals("")) { 
			Log.w(SyncPersonalizationService.class.getName(), "User has incorrect email or pass saved in SharedPreferences, personalization cannot be synced.");
			stopSelf();
			return;
		}
		
		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build()); 
		
		Log.w(SyncPersonalizationService.class.getName(), "After Internet connection & Profile availability checked in Sync Service...");
		
		// 1) synchronize personalizations related with forgotten words
		setNotificationTitle(R.string.forgotten_words_syncing);
		(new SyncForgottenWords(this, profileId, email, pass)).start(); 
		
		// 2) synchronize personalizations related with rememberMe words 
		setNotificationTitle(R.string.remember_me_words_syncing);
		(new SyncRememberMeWords(this, profileId, email, pass, nativeCode, foreignCode)).start(); 
		
		// 3) synchronize personalizations related with learned words
		setNotificationTitle(R.string.learned_words_syncing);
		(new SyncLearnedWords(this, profileId, email, pass, nativeCode, foreignCode)).start();
		
		// 4) synchronize learning history
		setNotificationTitle(R.string.learning_history_syncing);
		(new SyncLearningHistory(this, profileId, email, pass, nativeCode, foreignCode)).start();
		
		// 5) synchronize learning statistics 
		setNotificationTitle(R.string.learning_statistics_syncing);
		(new SyncLearningStatistics(this, profileId, email, pass, nativeCode, foreignCode)).start();
		
		notificationBuilder.setContentTitle(getString(R.string.personalizations_synced)).setOngoing(false).setProgress(0,0,false);
		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build()); 
		
		sendPersonalizationSyncedBroadcast(); 
	}

	private void setNotificationTitle(int resId) { 
		notificationBuilder.setContentTitle(getString(resId));
		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
	}
	
	private void sendPersonalizationSyncedBroadcast() 
	{
		Intent broadcastIntent = new Intent(PERSONALIZATION_SYNCED_BROADCAST); 
		sendBroadcast(broadcastIntent); 
	}

	private void synchronizeLearningStatistics() {
		// TODO Auto-generated method stub
		
	}
	
}
