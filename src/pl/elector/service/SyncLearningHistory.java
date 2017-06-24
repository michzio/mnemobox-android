package pl.elector.service;

import java.util.ArrayList;
import java.util.List;

import pl.elector.database.LearningHistoryProvider;
import pl.electoroffline.CustomHttpClient;
import pl.electoroffline.GetHistoryFromXML;
import pl.electoroffline.Preferences;
import pl.electoroffline.R;
import pl.electoroffline.SettingsFragment;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

public class SyncLearningHistory {
	
	private String syncLearningHistoryURL; 
	private String getLearningHistoryURL; 
	
	private String email; 
	private String pass; 
	private int profileId; 
	
	private Context context;
	
	public SyncLearningHistory(Context context, int profileId, String email, String  pass) { 
		
		this.context = context;
		this.profileId = profileId; 
		this.email = email;
		this.pass = pass;
		
		loadURLs();
	}
	
	public SyncLearningHistory(Context context, int profileId, String email, String pass, String nativeCode, String foreignCode) { 
		
		this.context = context;
		this.profileId = profileId; 
		this.email = email;
		this.pass = pass;
		
		loadURLs(nativeCode, foreignCode); 
	}
	
	public void start() { 
		synchronizeLearningHistory();
	}
	
	private void loadURLs() { 
		String nativeCode = Preferences.getAccountPreferences(context)
					.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower)); 
		
	    String foreignCode = Preferences.getAccountPreferences(context)
					.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower));
		loadURLs(nativeCode, foreignCode); 
	}
	
	private void loadURLs(String nativeCode, String foreignCode) {
	
		syncLearningHistoryURL = context.getString(R.string.synclearninghistory_url, nativeCode, foreignCode);
		getLearningHistoryURL = context.getString(R.string.history_url, nativeCode, foreignCode); 
	}
	
	/**
	 * Helper method used to synchronize learning history items with web server
	 */
	private void synchronizeLearningHistory() {
		
		// 1) send not_synced items in learningHistoryTable to web server database
		//    using JSON and web service script: syncLearningHistory.php?from=pl&to=en&email=xxx&pass=sha1(xxx)
		int responseCode = sendLearningHistoryDataToServer();
		Log.w(SyncLearningHistory.class.getName(), "Learning history synchronization response code: " + responseCode);
		
		// 2) get learning history items from web service making request to:
		//    userHistory.xml.php?from=pl&to=en&email=xxx&pass=sha1(xxx) 
		getLearningHistoryURL = getLearningHistoryURL + "&email=" + email + "&pass=" + pass; 
		Log.w(SyncLearningHistory.class.getName(), "Learning history URL: " + getLearningHistoryURL);
				
		GetHistoryFromXML learningHistoryReader = GetHistoryFromXML.getHistoryReader(getLearningHistoryURL);
				
		if(learningHistoryReader != null) {
			insertLearningHistoryRows(learningHistoryReader); 
		}
	}
	
	/**
	 * Helper method used to insert each learning history item 
	 * retrieved from XML web service into local SQLite database. 
	 */
	private void insertLearningHistoryRows(GetHistoryFromXML learningHistoryReader) {
		
		if(learningHistoryReader == null) return; 
		
		for(LearningHistoryItem item : learningHistoryReader) { 
			
			Log.w(SyncLearningHistory.class.getName(), "Learning history: " + item.getProfileId()  
					+ ", " + item.getWordsetId() + ", " + item.getModeId() + ", " + item.getWordsetTypeId() 
					+ ", " + item.getBadAnswers() + "," + item.getGoodAnswers() + ", " + item.getImprovement() 
					+ ", " + item.getHits() + ", " + item.getLastAccessDate()); 
			
			ContentValues values = new ContentValues(); 
			values.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_PROFILE_ID, item.getProfileId());
			values.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_WORDSET_ID, item.getWordsetId());
			values.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_MODE_ID, item.getModeId()); 
			values.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_WORDSET_TYPE_ID, item.getWordsetTypeId());
			values.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_BAD_ANSWERS, item.getBadAnswers()); 
			values.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_GOOD_ANSWERS, item.getGoodAnswers()); 
			values.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_IMPROVEMENT, item.getImprovement()); 
			values.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_HITS, item.getHits()); 
			values.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_LAST_ACCESS_DATE, item.getLastAccessDate()); 
			values.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_NOT_SYNCED, 0); 
			
			// insert new or update existing learning history row
			Uri INSERT_OR_UPDATE_HISTORY_URI = Uri.parse(LearningHistoryProvider.CONTENT_URI + "/insert_or_update");
			Uri insertedItemUri = context.getContentResolver().insert(INSERT_OR_UPDATE_HISTORY_URI, values);
			Log.w(SyncLearningHistory.class.getName(), 
					"Inserting/Updating learning history row under: " + insertedItemUri.toString());
	
		}
	}

	/**
	 * Helper method that gets not_synced learning history items 
	 * from LearningHistoryProvider (SQLite database) and sends them 
	 * to web server database using JSON to envelop data and HTTP POST 
	 * to send this JSON. 
	 * web service script: syncLearningHistory.php?from=pl&to=en&email=xxx&pass=sha1(xxx) + JSON (learning history rows)
	 * @return
	 */
	private int sendLearningHistoryDataToServer() {
		
		// Select not synchronized learning history items from learningHistoryTable limited to current profileId 
		Uri PROFILE_NOT_SYNCED_LEARNING_HISTORY_URI 
				= Uri.parse(LearningHistoryProvider.CONTENT_URI + "/user/" + profileId + "/not_synced");
		String[] projection = null; // all columns
		String selection = null;
		String[] selectionArgs = null;
				
		// Querying for learning history items
		Cursor cursor = context.getContentResolver().query(PROFILE_NOT_SYNCED_LEARNING_HISTORY_URI, 
		  										   projection, selection, selectionArgs, null);
		
		// Creating list of LearningHistoryItem instances used to serialize them to JSON format. 
		List<LearningHistoryItem> items = new ArrayList<LearningHistoryItem>();
				
		while(cursor.moveToNext()) { 
			
			LearningHistoryItem item = new LearningHistoryItem(
					cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_PROFILE_ID)),
					cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_WORDSET_ID)),
					cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_MODE_ID)),
					cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_WORDSET_TYPE_ID)),
					cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_BAD_ANSWERS)),
					cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_GOOD_ANSWERS)), 
					cursor.getFloat(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_IMPROVEMENT)),
					cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_HITS)),
					cursor.getString(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_LAST_ACCESS_DATE))
					);
			items.add(item); 
		}
		
		cursor.close(); 
		
		Gson gson = new Gson();
		String json = gson.toJson(items); 
		Log.d(SyncLearningHistory.class.getName(), "Learning history items JSON: "+ json); 
		 
		try {
			String url = syncLearningHistoryURL + "&email=" + email + "&pass=" + pass; 
			String responseText = CustomHttpClient.executeHttpJsonPost(url, json).trim();
			Log.w(SyncLearningHistory.class.getName(), "Learning history response text:|" + responseText + "|." );
			if(responseText.equals("1"))
				return 1; // Successful request 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
