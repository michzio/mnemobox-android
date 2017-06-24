package pl.elector.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import pl.elector.database.LearnedWordsNotSyncedProvider;
import pl.elector.database.LearnedWordsProvider;
import pl.elector.service.WordsLoaderService.WordsDownloader;
import pl.electoroffline.CustomHttpClient;
import pl.electoroffline.GetWordsListFromXML;
import pl.electoroffline.Preferences;
import pl.electoroffline.R;
import pl.electoroffline.SettingsFragment;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

public class SyncLearnedWords {
	
	private String syncLearnedURL; 
	private String getLearnedURL; 
	
	private Context context; 
	private int profileId; 
	private String email; 
	private String pass; 
	
	public SyncLearnedWords(Context context, int profileId, String email, String pass) { 
			
		this.context = context; 
		this.profileId = profileId; 
		this.email = email; 
		this.pass = pass; 
	
		loadURLs(); 
	}
	
	public SyncLearnedWords(Context context, int profileId, String email, String pass, String nativeCode, String foreignCode) { 
		
		this.context = context; 
		this.profileId = profileId; 
		this.email = email;
		this.pass = pass; 
		
		loadURLs(nativeCode, foreignCode); 
	}
	
	public void start() { 
		synchronizeLearnedWords(); 
	}

	private void loadURLs() { 
		String nativeCode = Preferences.getAccountPreferences(context)
				.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower)); 
		String foreignCode = Preferences.getAccountPreferences(context)
				.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower));
		
		loadURLs(nativeCode, foreignCode); 
	}
	
	private void loadURLs(String nativeCode, String foreignCode) { 
		syncLearnedURL = context.getString(R.string.synclearned_url);
		getLearnedURL = context.getString(R.string.getlearnedwordset_url, nativeCode, foreignCode, profileId);
	}

	/**
	 * Helper method used to perform learned words synchronization 
	 * with web server database.
	 */
	private void synchronizeLearnedWords() {
		
		// 1) synchronize data saved in learnedWordsNotSyncedTable with web server by making POST request
		//    to web service script: syncLearnedWords.php?from=pl&to=en&email=xxx&pass=sha1(xxx)
		String sentData = sendLearnedWordsDataToServer();
		Log.w(SyncLearnedWords.class.getName(), "Learned words synchronization sent data: " + sentData);
		// if request is processed correctly than there are returned serialized sent data 
		if(sentData != null) {
				// 2) delete rows from learnedNotSyncedTable
				deleteLearnedNotSyncedRows(sentData);		
		}
		
		// 3) get learned words from web service making request to:
		//    userLearnedWords.php?from=pl&to=en&email=xxx&pass=sha1(xxx) 
		Log.w(SyncLearnedWords.class.getName(), "Learned words URL: " + getLearnedURL);
		GetWordsListFromXML learnedWordsReader = 
				GetWordsListFromXML.getWordsListReader(context, getLearnedURL );	
		if(learnedWordsReader != null && learnedWordsReader.getSize() > 0) {
			
			// download all learned words details and update it in wordTable (don't delete, only insert or update!)
			new WordsLoaderService.WordsDownloader(context, learnedWordsReader).downlaod();
			
			// 4) before inserting new learned rows, delete old ones from learnedTable
			deleteLearnedRows();
					
			// 5) insert retrieved learned rows into learnedTable 
			insertLearnedRows(learnedWordsReader); 
		}
		
		 if(sentData == null) { 
			// if learned words hasn't been synchronized correctly 
			// we don't delete learnedNotSynced rows as it will be used 
			// for further synchronization try. 
						
			// we only need to complement existing learnedTable 
			// with words from learnedNotSyncedTable that haven't 
			// been inserted due to lack of words details: 
						
			// 6) get wordIds from learnedNotSynced that doesn't exists 
			//    in learnedTable and has set toDelete = FALSE 
						
			Uri NOT_EXISTING_LEARNED_URI =  Uri.parse(LearnedWordsNotSyncedProvider.CONTENT_URI + "/not_existing/" + profileId); 
			Cursor cursor = context.getContentResolver().query(NOT_EXISTING_LEARNED_URI, null, null, null, null); 
						
			while(cursor.moveToNext()) { 
							
				// download new word from web server with current wordId
				int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_WORD_ID));
							
				if(!WordsDownloader.checkWordExists(context, wordId)) 
				{
					if(!WordsDownloader.downloadWordDetails(context, wordId)) 
							continue; 
				}
							
				// insert new row into learnedTable 
				insertLearnedRow(wordId); 
			}
		}		
	}

	private Uri insertLearnedRow(int wordId) {
		ContentValues values = new ContentValues(); 
		values.put(LearnedWordsProvider.LearnedTable.COLUMN_PROFILE_ID, profileId); 
		values.put(LearnedWordsProvider.LearnedTable.COLUMN_WORD_ID, wordId); 
		
		Uri insertedItemUri = context.getContentResolver().insert(LearnedWordsProvider.CONTENT_URI, values);
		
		Log.w(SyncLearnedWords.class.getName(), 
			 "Learned row inserted under: " + insertedItemUri + " for (" + profileId + "," + wordId + ").");
		
		return insertedItemUri; 
		
	}

	/**
	 * Helper method that sends learned words data saved 
	 * in learnedNotSyncedTable to web server in order
	 * to synchronize it. Synchronization is performed 
	 * using POST request on:
	 * syncLearnedWords.php?from=pl&to=en&email=xxx&pass=sha1(xxx)
	 * passing data as @serialData parameter: 
	 * wid1+,wid2+,wid3+;wid4-,wid5-,wid6-
	 * @return serialData or null
	 */
	private String sendLearnedWordsDataToServer() {
		
		// select learned words data not to delete from learnedNotSyncedTable limited to current profileId 
		Uri PROFILE_LEARNED_NOT_SYNCED_URI = Uri.parse(LearnedWordsNotSyncedProvider.CONTENT_URI + "/profile/" + profileId);
		String[] projection = { LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_WORD_ID };
		String selection = LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_TO_DELETE + "= ?";
		String[] selectionArgs = { "0" };
		
		// querying for learned words to add
		Cursor cursor = context.getContentResolver().query(PROFILE_LEARNED_NOT_SYNCED_URI, 
  												projection, selection, selectionArgs, null);
		
		StringBuilder sb = new StringBuilder();
		
		// adding learned wordIds to add 
		while(cursor.moveToNext()) 
		{
			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(
								LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_WORD_ID));
			sb.append(wordId);
			sb.append(","); 
		}
		
		cursor.close(); 
		
		// trimming last "," character
		if (sb.length() > 0) {
				sb.setLength(sb.length() - 1);
		}
				
		sb.append(";");
		
		// querying for learned words to delete
		selectionArgs = new String[] { "1" }; 
		cursor = context.getContentResolver().query(PROFILE_LEARNED_NOT_SYNCED_URI, 
											projection, selection, selectionArgs, null);
		
		while(cursor.moveToNext()) 
		{
			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(
								LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_WORD_ID));
			sb.append(wordId);
			sb.append(","); 
		}
		
		// trimming last "," character
		if (sb.length() > 0) {
			   sb.setLength(sb.length() - 1);
		}
		
		String serialData = sb.toString(); 
		
		if(serialData.length() > 0) { 
			
			String nativeCode = Preferences.getAccountPreferences(context)
					.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower));
			String foreignCode = Preferences.getAccountPreferences(context)
					.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower));
			
			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(5); 
			params.add(new BasicNameValuePair("from", nativeCode));
			params.add(new BasicNameValuePair("to", foreignCode));
			params.add(new BasicNameValuePair("email", email));
			params.add(new BasicNameValuePair("pass", pass)); 
			params.add(new BasicNameValuePair("serialData", serialData)); 
			
			try {
				String responseText = CustomHttpClient.executeHttpPost(syncLearnedURL, params).substring(0, 1);
				Log.w(SyncLearnedWords.class.getName(), "Learned response text:|" + responseText + "|." );
				if(responseText.equals("1"))
					return serialData; // Successful request 
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// else some error occurred
		return null;
	}
	
	private boolean deleteLearnedNotSyncedRows(String toDeleteSerial) { 
		
		// converting string with serialized data to map of data used as deletion conditions (Where args) 
		Map<Integer, Integer> toDeleteMap = new HashMap<Integer, Integer>(); 
		
		// split serial data into two groups: 
		// 1 - with learned words to add
		// 2 - with learned words to delete
		String[] groups = toDeleteSerial.split(";", 2); 
		
		if(groups.length > 0) { 
			// to add learned wordIds (while synchronizing), 
			// in mapping there is flag toDelete set to 0 
			for(String wordId : groups[0].split(",")) {
				toDeleteMap.put(Integer.valueOf(wordId), 0); 
			}
		}
		
		if(groups.length > 1) { 
			// to delete learned wordIds (while synchronizing), 
			// in mapping there is flag toDelete set to 1 
			for(String wordId : groups[1].split(",")) { 
				toDeleteMap.put(Integer.valueOf(wordId), 1); 
			}
		}
		
		return deleteLearnedNotSyncedRows(toDeleteMap); 
	}
	
	/**
	 * Helper method used after successful online synchronization of learned
	 * words to delete learned not_synced rows that have already been synchronized. 
	 */
	private boolean deleteLearnedNotSyncedRows(Map<Integer, Integer> toDeleteMap) { 
		
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		ContentProviderOperation operation; 
		
		for(Entry<Integer, Integer> entry : toDeleteMap.entrySet()) { 
			Integer wordId = entry.getKey(); 
			Integer toDeleteFlag = entry.getValue(); 
			
			Uri PROFILE_WORD_LEARNED_NOT_SYNCED_URI = 
					Uri.parse(LearnedWordsNotSyncedProvider.CONTENT_URI + "/profile/" + profileId
							+ "/word/" + wordId);
			String selection = LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_TO_DELETE + "= ?"; 
			String[] selectionArgs = new String[] { String.valueOf(toDeleteFlag) }; 
			
			operation = ContentProviderOperation.newDelete(PROFILE_WORD_LEARNED_NOT_SYNCED_URI)
												.withSelection(selection, selectionArgs)
												.build(); 
			operations.add(operation); 
		}
		
		try { 
			ContentProviderResult[] results; 
			results = context.getContentResolver()
							 .applyBatch(LearnedWordsNotSyncedProvider.AUTHORITY, operations);
			if(results != null && results.length > 0) { 
				Log.w(SyncLearnedWords.class.getName(), "Deleted learned not synced rows: " + results.length); 
				return true; 
			}
		} catch(RemoteException e) { 
			e.printStackTrace(); 
		} catch(OperationApplicationException e) { 
			e.printStackTrace(); 
		}
		
		Log.w(SyncLearnedWords.class.getName(), "Some error occured while deleting learned not synced rows."); 
		
		return false; 
	}

	/**
	 * Simple old helper method that deletes all rows in learned not_synced table
	 * for current profileId. It is not taking into account any external conditions. 
	 * It can delete rows added in the meantime of synchronization process, leading 
	 * to missing some personalizations. 
	 * @return
	 */
	private boolean deleteAllLearnedNotSyncedRows() {
		
		Uri PROFILE_LEARNED_NOT_SYNCED_URI = 
				Uri.parse(LearnedWordsNotSyncedProvider.CONTENT_URI + "/profile/" + profileId);
		
		int deleteCount = context.getContentResolver().delete(PROFILE_LEARNED_NOT_SYNCED_URI, null, null);
		
		if(deleteCount > 0)
			return true; 
		
		return false; 
	}

	/**
	 * Helper method used after successful retrieving synchronized learned
	 * words from online web service and before inserting them to 
	 * learnedWordsTable to delete learned entries
	 * for current profile from learnedWordsTable.
	 */
	private boolean deleteLearnedRows() {
		
		Uri PROFILE_LEARNED_URI = Uri.parse(LearnedWordsProvider.CONTENT_URI + "/profile/" + profileId);
		
		int deleteCount = context.getContentResolver().delete(PROFILE_LEARNED_URI, null, null);
		
		if(deleteCount > 0)
			return true; 
		
		return false; 
		
	}

	/**
	 * Helper method that bulk inserts learned words read from parsed XML 
	 * that we get from online web service into the SQLite database. 
	 */
	private void insertLearnedRows(GetWordsListFromXML learnedWordsReader) {
		
		if(learnedWordsReader == null) 
			return; 
		
		ContentValues[] valuesCollection = new ContentValues[learnedWordsReader.getSize()];
		
		for(int i=0; i < learnedWordsReader.getSize(); i++) { 
			
			Integer wordId = learnedWordsReader.getWordIds().get(i); 
			
			ContentValues values = new ContentValues(); 
			values.put(LearnedWordsProvider.LearnedTable.COLUMN_PROFILE_ID, profileId);
			values.put(LearnedWordsProvider.LearnedTable.COLUMN_WORD_ID, wordId);
			
			valuesCollection[i] = values;
			
			/* Uri insertedItemUri = getContentResolver().insert(LearnedWordsProvider.CONTENT_URI, values);
			   Log.w(SyncLearnedWords.class.getName(), "Inserting learned row: " 
					+ wordId + "," + profileId  + " under: " + insertedItemUri.toString()); */
		}
		
		int insertedCount = context.getContentResolver().bulkInsert(LearnedWordsProvider.CONTENT_URI, valuesCollection);
		Log.w(SyncLearnedWords.class.getName(), "Bulk Inserted " + insertedCount + " learned rows to sqlite database."); 
		
	}
}
