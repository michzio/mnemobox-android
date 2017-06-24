package pl.electoroffline;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
 
import java.util.HashMap;
import java.util.Locale;

public class ElectorApplication extends Application {
	
	 private Locale locale = null; 
	 
	 @Override
	 public void onConfigurationChanged(Configuration newConfig) {  
		 super.onConfigurationChanged(newConfig); 
		 
		 newConfig = new Configuration(newConfig); 
		 if(locale != null) { 
			 newConfig.locale = locale; 
			 Locale.setDefault(locale); 
			 getBaseContext().getResources().updateConfiguration(newConfig, 
					 			getBaseContext().getResources().getDisplayMetrics());
		 }
	 }
	 
	 @Override 
	 public void onCreate() { 
		 super.onCreate(); 
		 
		 // retrieve persisted menu language settings
		 String userAccountEmail = Preferences.getString(this, Preferences.KEY_EMAIL, "anonymous");
		 Preferences preferences = new Preferences(this, userAccountEmail);  
		 // user account email used as preferences file name
		 String lang = preferences.getString(SettingsFragment.KEY_MENU_LANGUAGE_PREFERENCE, null);
		 
		 updateLocaleLanguage(lang);
		 
	 }
	 
	 public void updateLocaleLanguage(String lang) {
		 Log.d(ElectorApplication.class.getName(), "Currently set menu language: " + lang); 
		 
		// set up configuration if retrieved lang and it is different 
		Configuration config = getBaseContext().getResources().getConfiguration();
		config = new Configuration(config); 
		
		if(lang != null && !config.locale.getLanguage().equals(locale)) {
		 
			 	locale = new Locale(lang); 
			 	Locale.setDefault(locale);
			 	config.locale = locale; 
			 	getBaseContext().getResources().updateConfiguration(config, 
			 					getBaseContext().getResources().getDisplayMetrics());
		}
	 }

	/**
	   * Enum used to identify the tracker that needs to be used for tracking.
	   *
	   * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
	   * storing them all in Application object helps ensure that they are created only once per
	   * application instance.
	   */
	  public enum TrackerName {
	    APP_TRACKER, // Tracker used only in this app.
	    GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
	    ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
	  }
	  
	  HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

	  // The following line should be changed to include the correct property id.
	  private static final String PROPERTY_ID = "UA-41399245-1";
	  public static int GENERAL_TRACKER = 0;
	
	  
	  public ElectorApplication() {
			super();
	  }
	
	  
	  synchronized Tracker getTracker(TrackerName trackerId) {
		    
		  if (!mTrackers.containsKey(trackerId)) {

		      GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
		      
		      Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(R.xml.app_tracker)
		              : (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(PROPERTY_ID)
		              :  null; //analytics.newTracker(R.xml.ecommerce_tracker);
		      t.enableAdvertisingIdCollection(true);
		      mTrackers.put(trackerId, t);

		    }
		    
		    return mTrackers.get(trackerId);
	 }
}
