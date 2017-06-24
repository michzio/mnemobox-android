package pl.elector.service;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import pl.elector.database.LearningStatsProvider;
import pl.electoroffline.CustomHttpClient;
import pl.electoroffline.GetLearningStatisticsFromXML;
import pl.electoroffline.Preferences;
import pl.electoroffline.R;
import pl.electoroffline.SettingsFragment;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class SyncLearningStatistics {

	private String syncLearningStatisticsURL; 
	private String getLearningStatisticsURL; 
	
	private String email; 
	private String pass; 
	private int profileId; 
	
	private Context context; 
	
	public SyncLearningStatistics(Context context, int profileId, String email, String pass) { 
		
		this.context = context; 
		this.profileId = profileId; 
		this.email = email; 
		this.pass = pass; 
		
		loadURLs(); 
	}
	
	public SyncLearningStatistics(Context context, int profileId, String email, String pass, String nativeCode, String foreignCode) { 
		
		this.context = context; 
		this.profileId = profileId; 
		this.email = email; 
		this.pass = pass; 
		
		loadURLs(nativeCode, foreignCode); 
	}
	
	public void start() { 
		synchronizeLearningStatistics(); 
	}
	
	private void loadURLs() { 
		String nativeCode = Preferences.getAccountPreferences(context)
				.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower)); 
		String foreignCode = Preferences.getAccountPreferences(context)
				.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower));
		loadURLs(nativeCode, foreignCode); 
	}
	
	private void loadURLs(String nativeCode, String foreignCode) { 
		
		syncLearningStatisticsURL = context.getString(R.string.synclearningstats_url, nativeCode, foreignCode, email, pass);
		getLearningStatisticsURL = context.getString(R.string.getlearningstats_url, nativeCode, foreignCode, email, pass); 
	}
	
	/**
	 * Helper method used to synchronize learning statistics items with web server 
	 */
	private void synchronizeLearningStatistics() { 
		
		// 1) send not_synced items in learningStatsTable to web server database 
		// 	  using JSON and web service script: syncLearningStats.php?from=pl&to=en&email=xxx&pass=sha1(xxx)
		int responseCode = sendLearningStatisticsDataToServer(); 
		Log.w(SyncLearningStatistics.class.getName(), "Learning statistics synchronization response code: " + responseCode); 
		
		// 2) get learning statistics items from web service making request to:
		//    getLearningStats.xml.php?from=pl&to=en&email=xxx&pass=sha1(xxx)
		Log.w(SyncLearningStatistics.class.getName(), "Learning statistics URL: " + getLearningStatisticsURL); 
		GetLearningStatisticsFromXML learningStatisticsReader = 
				GetLearningStatisticsFromXML.getLearningStatisticsReader(getLearningStatisticsURL); 
		
		if(learningStatisticsReader != null) 
			insertLearningStatisticsRows(learningStatisticsReader); 
	}

	private void insertLearningStatisticsRows(
			GetLearningStatisticsFromXML learningStatisticsReader) {
		  
		if(learningStatisticsReader == null) return; 
		
		for(LearningStatisticsItem item : learningStatisticsReader) { 
			
			Log.w(SyncLearningStatistics.class.getName(), "Learning statistics: " 
					+ item.getProfileId() + ", " + item.getAccessDate() + ", " 
					+ item.getBadAnswers() + ", " + item.getGoodAnswers()); 
			
			ContentValues values = new ContentValues(); 
			values.put(LearningStatsProvider.LearningStatsTable.COLUMN_PROFILE_ID, item.getProfileId());
			values.put(LearningStatsProvider.LearningStatsTable.COLUMN_ACCESS_DATE,  item.getAccessDate()); 
			values.put(LearningStatsProvider.LearningStatsTable.COLUMN_BAD_ANSWERS, item.getBadAnswers()); 
			values.put(LearningStatsProvider.LearningStatsTable.COLUMN_GOOD_ANSWERS, item.getGoodAnswers()); 
			values.put(LearningStatsProvider.LearningStatsTable.COLUMN_NOT_SYNCED, 0); 
			
			// insert new or update existing learning statistics row
			Uri INSERT_OR_UPDATE_STATS_URI = Uri.parse(LearningStatsProvider.CONTENT_URI + "/insert_or_update"); 
			Uri insertedItemUri = context.getContentResolver().insert(INSERT_OR_UPDATE_STATS_URI, values);
			Log.w(SyncLearningStatistics.class.getName(), 
					"Inserting/Updating learning statistics row under: " + insertedItemUri.toString());
		}
	}

	/**
	 * Helper method that gets not_synced learning statistics items 
	 * from LearningStatsProvider (SQLite database) and sends them 
	 * to web server database using JSON to envelop data and HTTP POST 
	 * to send this JSON. 
	 * web service script: syncLearningStats.php?from=pl&to=en&email=xxx&pass=sha1(xxx) + JSON (learning statistics rows)
	 * @return
	 */
	private int sendLearningStatisticsDataToServer() {
		
		// Select not synchronized learning statistics items from LearningStatsTable limited to current profileId 
		Uri PROFILE_NOT_SYNCED_LEARNING_STATS_URI 
					= Uri.parse(LearningStatsProvider.CONTENT_URI + "/profile/" + profileId + "/not_synced");  
		String[] projection = null; // all columns 
		String selection = null; 
		String[] selectionArgs = null;
		
		// Querying for learning statistics items 
		Cursor cursor = context.getContentResolver().query(PROFILE_NOT_SYNCED_LEARNING_STATS_URI,
													projection, selection, selectionArgs, null); 
		
		// Creating list of LearningStatisticsItem instances used to serialize them to JSON format. 
		List<LearningStatisticsItem> items = new ArrayList<LearningStatisticsItem>();
		
		while(cursor.moveToNext()) { 
			
			LearningStatisticsItem item = new LearningStatisticsItem(
					cursor.getInt(cursor.getColumnIndexOrThrow(LearningStatsProvider.LearningStatsTable.COLUMN_PROFILE_ID)),
					cursor.getString(cursor.getColumnIndexOrThrow(LearningStatsProvider.LearningStatsTable.COLUMN_ACCESS_DATE)),
					cursor.getInt(cursor.getColumnIndexOrThrow(LearningStatsProvider.LearningStatsTable.COLUMN_BAD_ANSWERS)), 
					cursor.getInt(cursor.getColumnIndexOrThrow(LearningStatsProvider.LearningStatsTable.COLUMN_GOOD_ANSWERS))
					); 
			items.add(item);
		}
		
		cursor.close(); 
		
		Gson gson = new Gson(); 
		String json = gson.toJson(items); 
		Log.d(SyncLearningStatistics.class.getName(), "Learning statistics items JSON: " + json); 
		
		try { 
			String responseText = CustomHttpClient.executeHttpJsonPost(syncLearningStatisticsURL, json).trim();
			Log.w(SyncLearningStatistics.class.getName(), "Learning statistics response text:|" + responseText + "|.");
			if(responseText.equals("1"))
				return 1; // Successful request
		} catch(Exception e) { 
			e.printStackTrace(); 
		}
		return 0;
	}
}
