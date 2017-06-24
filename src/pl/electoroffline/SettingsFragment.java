package pl.electoroffline;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import pl.elector.database.DatabaseSQLiteOpenHelper;
import pl.elector.database.ForgottenNotSyncedProvider;
import pl.elector.database.ForgottenProvider;
import pl.elector.database.LearnedWordsNotSyncedProvider;
import pl.elector.database.LearnedWordsProvider;
import pl.elector.database.PostItProvider;
import pl.elector.database.RememberMeNotSyncedProvider;
import pl.elector.database.RememberMeProvider;
import pl.elector.database.SentenceProvider;
import pl.elector.database.SolutionContentProvider;
import pl.elector.database.SolutionProvider;
import pl.elector.database.TaskCategoryProvider;
import pl.elector.database.TaskProvider;
import pl.elector.database.UserWordsetProvider;
import pl.elector.database.UserWordsetWordsProvider;
import pl.elector.database.WordProvider;
import pl.elector.database.WordsetCategoryProvider;
import pl.elector.database.WordsetProvider;
import pl.elector.database.WordsetWordsProvider;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	public static final String TAG = "SETTINGS_FRAGMENT_TAG"; 
	private static final String KEY_SOUND_VOLUME_PREFERENCE = "KEY_SOUND_VOLUME_PREFERENCE"; 
	public static final String KEY_MENU_LANGUAGE_PREFERENCE = "KEY_MENU_LANGUAGE_PREFERENCE"; 
	public static final String KEY_NATIVE_LANGUAGE_PREFERENCE = "KEY_NATIVE_LANGUAGE_PREFERENCE"; 
	public static final String KEY_FOREIGN_LANGUAGE_PREFERENCE = "KEY_FOREIGN_LANGUAGE_PREFERENCE"; // not implemented in PreferenceScreen
	
	private SoundVolumePreference soundVolumePreference; 
	private VolumeChangeObserver volumeChangeObserver; 
	
	@Override 
	public void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		
		// configuring SharedPreferences to use custom filename (account-specific) to store Preferences,
		// default (when user is logged out) is 'anonymous' filename
		PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(
        		Preferences.getString(getActivity(), Preferences.KEY_EMAIL, "anonymous"));
        preferenceManager.setSharedPreferencesMode(Context.MODE_PRIVATE);
        
        // loading preferences from XML file (layout of PreferenceScreens)
		addPreferencesFromResource(R.xml.userpreferences); 
		
		soundVolumePreference = 
				 (SoundVolumePreference) getPreferenceScreen().findPreference(KEY_SOUND_VOLUME_PREFERENCE);
		
		Preference nativeLangPreference = findPreference(KEY_NATIVE_LANGUAGE_PREFERENCE); 
		nativeLangPreference.setOnPreferenceChangeListener(onPreferenceChangeListener); 
	
	}
	
	private Preference.OnPreferenceChangeListener onPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
		
		@Override
		public boolean onPreferenceChange(final Preference preference, final Object newValue) {
			
			if(preference.getKey().equals(KEY_NATIVE_LANGUAGE_PREFERENCE)) {  
				
				// create dialog that prompts user if he want to 
				// change native language, which will clear current database state
				AlertDialog dialog = new AlertDialog.Builder(getActivity())
							.setMessage(R.string.native_language_change_alert)
							.setCancelable(false)
							.setPositiveButton(R.string.change_native_button, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									
									// replace state of the Preference with the new value 
									((ListPreference) preference).setValue((String)newValue); 
								
									// clear current database state 
									clearDatabaseState(); 
								}
							})
							.setNegativeButton(R.string.cancel_native_change_button, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									
									// just cancel the operation and do nothing 
									return; 
								}
							})
							.create(); 
				
				dialog.show();
				
				
				return false;
			}
			
			// true to update the state of the Preference with the new value
            // in case you want to disallow use return false
			return true; 
		}
	};
	
	@Override
	public void onResume() {
		super.onResume(); 
		
		ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
		actionBar.setTitle(R.string.settings);
		
		// register VolumeChangeObserver
		volumeChangeObserver = new VolumeChangeObserver(getActivity(), 
														new Handler(), 
														AudioManager.STREAM_MUSIC, 
														soundVolumePreference);
		getActivity().getApplicationContext().getContentResolver()
							.registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, volumeChangeObserver);
		Log.d(SettingsFragment.class.getName(), "VolumeChangeObserver has been registered.");
		
		 // set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this); 
	}
	
	@Override
	public void onPause() { 
		super.onPause();
		
		// unregister VolumeChangeObserver
		getActivity().getApplicationContext().getContentResolver()
							.unregisterContentObserver(volumeChangeObserver);
		Log.d(SettingsFragment.class.getName(), "VolumeChangeObserver has been unregistered.");
		
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this); 
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	      
		if(key.equals(KEY_MENU_LANGUAGE_PREFERENCE)) { 
			String menuLanguage = sharedPreferences.getString(KEY_MENU_LANGUAGE_PREFERENCE, "en"); 
			ElectorApplication app = (ElectorApplication) getActivity().getApplication();
			app.updateLocaleLanguage(menuLanguage);
			
			
			((SettingsActivity) getActivity()).restartActivity(); 
			
		} else if(key.equals(KEY_NATIVE_LANGUAGE_PREFERENCE)) { 
			
			// handled in onPreferenceChangeListener
		}
		
		
	}
	
	private void clearDatabaseState() { 
		/** 
		 *  // Conventional method to clear given table:  	
		 *	// db.delete(String tableName, String whereClause, String[] whereArgs);
		 *	// If whereClause is null, it will delete all rows.
		 *	DatabaseSQLiteOpenHelper dbHelper = new DatabaseSQLiteOpenHelper(getActivity());
		 *	SQLiteDatabase db = dbHelper.getWritableDatabase();	
		 *	db.delete(TABLE_NAME, null, null); 
		 */	
		
		// clearing database while changing native language configuration 
		getActivity().getContentResolver().delete(ForgottenNotSyncedProvider.CONTENT_URI, null, null);
		getActivity().getContentResolver().delete(ForgottenProvider.CONTENT_URI, null, null);
		getActivity().getContentResolver().delete(LearnedWordsNotSyncedProvider.CONTENT_URI, null, null);
		getActivity().getContentResolver().delete(LearnedWordsProvider.CONTENT_URI, null, null);
		getActivity().getContentResolver().delete(PostItProvider.CONTENT_URI, null, null); 
		getActivity().getContentResolver().delete(RememberMeNotSyncedProvider.CONTENT_URI, null, null);
		getActivity().getContentResolver().delete(RememberMeProvider.CONTENT_URI, null, null); 
		getActivity().getContentResolver().delete(SentenceProvider.CONTENT_URI, null, null); 
		getActivity().getContentResolver().delete(SolutionContentProvider.CONTENT_URI, null, null); 
		getActivity().getContentResolver().delete(SolutionProvider.CONTENT_URI, null, null); 
		getActivity().getContentResolver().delete(TaskCategoryProvider.CONTENT_URI, null, null); 
		getActivity().getContentResolver().delete(TaskProvider.CONTENT_URI, null, null); 
		getActivity().getContentResolver().delete(UserWordsetProvider.CONTENT_URI, null, null); 
		getActivity().getContentResolver().delete(UserWordsetWordsProvider.CONTENT_URI, null, null); 
		getActivity().getContentResolver().delete(WordProvider.CONTENT_URI, null, null); 
		getActivity().getContentResolver().delete(WordsetCategoryProvider.CONTENT_URI, null, null);
		getActivity().getContentResolver().delete(WordsetProvider.CONTENT_URI, null, null); 
		getActivity().getContentResolver().delete(WordsetWordsProvider.CONTENT_URI, null, null); 
	}
	
}
