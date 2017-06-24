package pl.elector.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import pl.elector.database.RememberMeNotSyncedProvider;
import pl.elector.database.RememberMeProvider;
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

public class SyncRememberMeWords {

	private String syncRememberMeURL;
	private String getRememberMeURL; 
	
	private Context context; 
	private int profileId; 
	private String email;
	private String pass;
	
	public SyncRememberMeWords(Context context, int profileId, String email, String pass) { 
		
		this.context = context;
		this.profileId = profileId;
		this.email = email;
		this.pass = pass;
		
		loadURLs();
	}
	
	public SyncRememberMeWords(Context context, int profileId, String email, String pass, String nativeCode, String foreignCode)
	{
		this.context = context;
		this.profileId = profileId;
		this.email = email;
		this.pass = pass;
		
		loadURLs(nativeCode, foreignCode);
	}
	
	public void start() { 
		synchronizeRememberMeWords(); 
	}
	
	private void loadURLs() { 
		String nativeCode = Preferences.getAccountPreferences(context)
				.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower)); 
		String foreignCode = Preferences.getAccountPreferences(context)
				.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower));
		
		loadURLs(nativeCode, foreignCode); 
	}
	
	private void loadURLs(String nativeCode, String foreignCode) { 
		syncRememberMeURL = context.getString(R.string.syncrememberme_url); 
		getRememberMeURL = context.getString(R.string.getremembermewordset_url, nativeCode, foreignCode, profileId);
	}
	
	/**
	 * Helper method used to perform remember_me words synchronization 
	 * with web server database.
	 */
	private void synchronizeRememberMeWords() {
		
		// 1) synchronize data saved in rememberMeNotSyncedTable with web server by making POST request
		//    to web service script: syncRememberMeWords.php?from=pl&to=en&email=xxx&pass=sha1(xxx)
		String sentData = sendRememberMeDataToServer(); 
		Log.w(SyncRememberMeWords.class.getName(), "RememberMe words synchronization sent data: " + sentData);
		// if request is processed correctly than there are returned serialized sent data
		if(sentData != null) {
			// 2) delete rows from rememberMeNotSyncedTable
			deleteRememberMeNotSyncedRows(sentData);			
		}
		
		// 3) get rememberMe words from web service making request to:
		//    userRememberMeWords.php?from=pl&to=en&email=xxx&pass=sha1(xxx) 
		Log.w(SyncRememberMeWords.class.getName(), "RememberMe words from: " + getRememberMeURL + ".");
		GetWordsListFromXML rememberMeWordsReader = 
							GetWordsListFromXML.getWordsListReader(context, getRememberMeURL );
		if(rememberMeWordsReader != null && rememberMeWordsReader.getSize() > 0) { 
					
			// download all rememberMe words details and update it in wordTable (don't delete, only insert or update!)
			new WordsLoaderService.WordsDownloader(context, rememberMeWordsReader).downlaod();
						
			// 4) before inserting new rememberMe rows, delete old ones from rememberMeTable
			deleteRememberMeRows();
						
			// 5) insert retrieved rememberMe rows into rememberMeTable 
			insertRememberMeRows(rememberMeWordsReader);
		}
					
		if(sentData == null) { 
			
			Log.w(SyncRememberMeWords.class.getName(), "Checking not existing remember me words in rememberMe Table.");
			
			// if remember_me words hasn't been synchronized correctly 
			// we don't delete rememberMeNotSynced rows as it will be used 
			// for further synchronization try. 
			
			// we only need to complement existing rememberMeTable 
			// with words from rememberMeNotSyncedTable that haven't 
			// been inserted due to lack of words details: 
			
			// 6) get wordIds from rememberMeNotSynced that doesn't exists 
			//    in rememberMeTable and has set toDelete = FALSE 
			
			Uri NOT_EXISTING_REMEMBER_ME_URI =  Uri.parse(RememberMeNotSyncedProvider.CONTENT_URI + "/not_existing/" + profileId); 
			Cursor cursor = context.getContentResolver().query(NOT_EXISTING_REMEMBER_ME_URI, null, null, null, null); 
			
			while(cursor.moveToNext()) { 
				
				// download new word from web server with current wordId
				int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_WORD_ID));
				
				if(!WordsDownloader.checkWordExists(context, wordId)) 
				{
					if(!WordsDownloader.downloadWordDetails(context, wordId)) 
						continue; 
				}
				
				// insert new row into rememberMeTable 
				insertRememberMeRow(wordId); 
			}
		}
		
	}

	private Uri insertRememberMeRow(int wordId) {
		ContentValues values = new ContentValues(); 
		values.put(RememberMeProvider.RememberMeTable.COLUMN_PROFILE_ID, profileId); 
		values.put(RememberMeProvider.RememberMeTable.COLUMN_WORD_ID, wordId); 
		
		Uri insertedItemUri = context.getContentResolver().insert(RememberMeProvider.CONTENT_URI, values);
		
		Log.w(SyncRememberMeWords.class.getName(), 
			 "Remember me row inserted under: " + insertedItemUri + " for (" + profileId + "," + wordId + ").");
		
		return insertedItemUri; 
		
	}

	/**
	 * Helper method that sends rememberMe data saved 
	 * in rememberMeNotSyncedTable to web server in order
	 * to synchronize it. Synchronization is performed 
	 * using POST request on:
	 * syncRememberMeWords.php?from=pl&to=en&email=xxx&pass=sha1(xxx)
	 * passing data as @serialData parameter: 
	 * wid1+,wid2+,wid3+;wid4-,wid5-,wid6-
	 * @return serialData or null
	 */
	private String sendRememberMeDataToServer() {
		
		// select rememberMe data not to delete from rememberMeNotSyncedTable limited to current profileId 
		Uri PROFILE_REMEMBER_ME_NOT_SYNCED_URI = Uri.parse(RememberMeNotSyncedProvider.CONTENT_URI + "/profile/" + profileId);
		String[] projection = { RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_WORD_ID };
		String selection = RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_TO_DELETE + "= ?";
		String[] selectionArgs = { "0" };
	    
		// querying for rememberMe words to add
		Cursor cursor = context.getContentResolver().query(PROFILE_REMEMBER_ME_NOT_SYNCED_URI, 
										  				projection, selection, selectionArgs, null);
		StringBuilder sb = new StringBuilder();
		
		// adding remember me wordIds to add 
		while(cursor.moveToNext()) 
		{
			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(
								RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_WORD_ID));
			sb.append(wordId);
			sb.append(","); 
		}
		
		cursor.close(); 
		
		// trimming last "," character
		if (sb.length() > 0) {
			   sb.setLength(sb.length() - 1);
		}
		
		sb.append(";");
		
		// querying for rememberMe words to delete
		selectionArgs = new String[] { "1" }; 
		cursor = context.getContentResolver().query(PROFILE_REMEMBER_ME_NOT_SYNCED_URI, 
												projection, selection, selectionArgs, null);
		
		while(cursor.moveToNext()) 
		{
			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(
								RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_WORD_ID));
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
				String responseText = CustomHttpClient.executeHttpPost(syncRememberMeURL, params).substring(0, 1);
				Log.w(SyncRememberMeWords.class.getName(), "RememberMe response text:|" + responseText + "|." );
				if(responseText.equals("1"))
					return serialData; // Successful request 
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// else some error occurred
		return null;
	}

	private boolean deleteRememberMeNotSyncedRows(String toDeleteSerial) { 
		
		// converting string with serialized data to map of data used as deletion conditions (Where args)
		Map<Integer, Integer> toDeleteMap = new HashMap<Integer, Integer>(); 
		
		// split serial data into two groups: 
		// 1 - with remember_me words to add  
		// 2 - with rememeber_me words to delete
		String[] groups = toDeleteSerial.split(";", 2);  
		
		if(groups.length > 0) { 
			// to add remember_me wordIds (while synchronizing), 
			// in mapping there is flag toDelete set to 0 
			for(String wordId : groups[0].split(",")) { 
				toDeleteMap.put(Integer.valueOf(wordId), 0);  
			}
		}
		
		if(groups.length > 1 ) { 
			// to delete remember_me wordIds (while synchronizing), 
			// in mapping there is flag toDelete set to 1 
			for(String wordId : groups[1].split(",")) { 
				toDeleteMap.put(Integer.valueOf(wordId), 1); 
			}
		}
		
		return deleteRememberMeNotSyncedRows(toDeleteMap); 
	}

	/**
	 * Helper method used after successful online synchronization of rememberMe 
	 * to delete rememberMe not_synced rows that have already been synchronized. 
	 */
	private boolean deleteRememberMeNotSyncedRows(Map<Integer, Integer> toDeleteMap) { 
		
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(); 
		ContentProviderOperation operation; 
		
		for(Entry<Integer, Integer> entry : toDeleteMap.entrySet()) { 
			Integer wordId = entry.getKey(); 
			Integer toDeleteFlag = entry.getValue(); 
			
			Uri PROFILE_WORD_REMEMBER_ME_NOT_SYNCED_URI = 
					Uri.parse(RememberMeNotSyncedProvider.CONTENT_URI + "/profile/" + profileId
							+ "/word/" + wordId);
			String selection = RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_TO_DELETE + "= ?"; 
			String[] selectionArgs = new String[] { String.valueOf(toDeleteFlag) };
			
			operation = ContentProviderOperation.newDelete(PROFILE_WORD_REMEMBER_ME_NOT_SYNCED_URI)
												.withSelection(selection, selectionArgs)
												.build();
			operations.add(operation); 
		}
		
		try { 
			ContentProviderResult[] results; 
			results = context.getContentResolver()
							 .applyBatch(RememberMeNotSyncedProvider.AUTHORITY, operations);
			
			if(results != null && results.length > 0) { 
				Log.w(SyncRememberMeWords.class.getName(), "Deleted remember_me not synced rows: " + results.length); 
				return true; 
			}
		} catch(RemoteException e) { 
			e.printStackTrace(); 
		} catch(OperationApplicationException e) { 
			e.printStackTrace();
		}
		
		Log.w(SyncRememberMeWords.class.getName(), "Some error occured while deleting remember_me not synced rows."); 
		
		return false; 
	}

	/**
	 * Simple old helper function that deletes all rows in remember_me_not_synced table.
	 * It is not taking into account any external conditions. It can delete rows added 
	 * in the meantime of synchronization process, leading to missing some personalizations. 
	 * @return
	 */
	private boolean deleteAllRememberMeNotSyncedRows() {
		
		Uri PROFILE_REMEMBER_ME_NOT_SYNCED_URI = 
				Uri.parse(RememberMeNotSyncedProvider.CONTENT_URI + "/profile/" + profileId);
		
		int deleteCount = context.getContentResolver().delete(PROFILE_REMEMBER_ME_NOT_SYNCED_URI, null, null);
		
		if(deleteCount > 0)
			return true; 
		
		return false; 
		
	}

	/**
	 * Helper method used after successful retrieving synchronized remember_me
	 * words from online web service and before inserting them to 
	 * rememberMeTable to delete remember_me entries
	 * for current profile from rememberMeTable.
	 */
	private boolean deleteRememberMeRows() {
		
		Uri PROFILE_REMEMBER_ME_URI = Uri.parse(RememberMeProvider.CONTENT_URI + "/profile/" + profileId);
		
		int deleteCount = context.getContentResolver().delete(PROFILE_REMEMBER_ME_URI, null, null);
		
		if(deleteCount > 0)
			return true; 
		
		return false; 
		
	}

	private void insertRememberMeRows(GetWordsListFromXML rememberMeWordsReader) {
		
		if(rememberMeWordsReader == null) 
			return;
		
		ContentValues[] valuesCollection = new ContentValues[rememberMeWordsReader.getSize()]; 
		
		for(int i=0; i < rememberMeWordsReader.getSize(); i++) { 
			
			Integer wordId = rememberMeWordsReader.getWordIds().get(i); 
			
			ContentValues values = new ContentValues(); 
			values.put(RememberMeProvider.RememberMeTable.COLUMN_PROFILE_ID, profileId);
			values.put(RememberMeProvider.RememberMeTable.COLUMN_WORD_ID, wordId);
			
			valuesCollection[i] = values;
			
			/* Uri insertedItemUri = context.getContentResolver().insert(RememberMeProvider.CONTENT_URI, values);
			Log.w(SyncRememberMeWords.class.getName(), "Inserting rememberMe: " 
					+ wordId + "," + profileId  + " under: " + insertedItemUri.toString()); */
		}
		
		int insertedCount = context.getContentResolver().bulkInsert(RememberMeProvider.CONTENT_URI, valuesCollection);
		Log.w(SyncRememberMeWords.class.getName(), "Bulk Inserted " + insertedCount + " remember_me rows to sqlite database.");
	}

}
