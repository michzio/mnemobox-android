package pl.elector.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import pl.elector.database.ForgottenNotSyncedProvider;
import pl.elector.database.ForgottenProvider;
import pl.electoroffline.CustomHttpClient;
import pl.electoroffline.GetUserForgottenFromXML;
import pl.electoroffline.Preferences;
import pl.electoroffline.R;
import pl.electoroffline.SettingsFragment;
import pl.electoroffline.GetUserForgottenFromXML.ForgottenWord;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

public class SyncForgottenWords {

	private Context context; 
	private int profileId; 
	private String email;
	private String pass; 
	
	private String syncForgottenURL; 
	
	public SyncForgottenWords(Context context, int profileId, String email, String pass) { 
		
		this.context = context; 
		this.profileId = profileId;
		this.email = email; 
		this.pass = pass; 
		
		loadURLs(); 
		
	}
	
	public void start() { 
		synchronizeForgottenWords(); 
	}
	
	private void loadURLs() { 
		syncForgottenURL = context.getString(R.string.syncforgotten_url);
	}
	
	private void synchronizeForgottenWords() {
		
		// 1) using data saved in forgottenNotSyncedTable sync it making POST request 
		//    to web service script: syncForgottenWords.php?from=pl&to=en&email=xxx&pass=sha1(xxx)
		String sentData = sendForgottenDataToServer(); 
		Log.w(SyncForgottenWords.class.getName(), "Forgotten words synchronization sent data: " + sentData);
		// if request is processed correctly than there are returned serialized sent data
		if(sentData != null) { 
			// 2) delete rows from forgottenNotSyncedTable
			deleteForgottenNotSyncedRows(sentData); 
		}
		
		// 3) get forgotten rows from web service making request to: 
		//    userForgotten.php?from=pl&to=en&email=xxx&pass=sha1(xxx)
		GetUserForgottenFromXML forgottenReader = GetUserForgottenFromXML.getForgottenWordsReader(context); 
		Log.w(SyncForgottenWords.class.getName(), 
				"Number of retrieved forggoten words from web service: " + forgottenReader.forgottenWords.size());
		
		if(forgottenReader != null && forgottenReader.forgottenWords.size() > 0) {  
			
			// 4) before inserting new forgotten rows, delete old ones from forgottenTable
			deleteForgottenRows();
							
			//  5) insert retrieved forgotten rows into forgottenTable
			insertForgottenRows(forgottenReader.forgottenWords);
		}
		
		if(sentData == null) { 
			// 6) forgotten words hasn't been synced correctly with web server 
			// so we cannot relay on its data: 
			// we doesn't delete current forgotten_not_synced rows,
			// app will relay on current state in forgottenTable 
			// and state of forgottenNotSyncedTable will be maintain for 
			// further synchronization.
		}
		
	}
	
	/**
	 * Helper method that sends forgotten data saved 
	 * in forgottenNotSyncedTable to web server in order
	 * to synchronize it. Synchronization is performed 
	 * using POST request on:
	 * syncForgottenWords.php?from=pl&to=en&email=xxx&pass=sha1(xxx)
	 * passing data as @serialData parameter: 
	 * wid1,delta1;wid2,delta2;wid3,delta3
	 * @return responseCode or 0 if request hasn't been executed
	 */
	private String sendForgottenDataToServer() {
	
		// select forgotten data from forgottenNotSyncedTable limited to current profileId 
		Uri PROFILE_FORGOTTEN_NOT_SYNCED_URI = Uri.parse(ForgottenNotSyncedProvider.CONTENT_URI + "/profile/" + profileId);
		String[] projection = { ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_WORD_ID,
								ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_DELTA_WEIGHT };
		
		Cursor cursor = context.getContentResolver().query(PROFILE_FORGOTTEN_NOT_SYNCED_URI, 
								  					projection, null, null, null);
		if(cursor.getCount() > 0) { 
			StringBuilder sb = new StringBuilder(); 
			
			// construct @serialData param: 
			while(cursor.moveToNext()) { 
				int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(
									ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_WORD_ID));
				int deltaWeight = cursor.getInt(cursor.getColumnIndexOrThrow(
								ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_DELTA_WEIGHT));
				// concatenating serialData
				sb.append(wordId); 
				sb.append(",");
				sb.append(deltaWeight); 
				sb.append(";"); 
			}
			
			// trimming last ";" character
			if (sb.length() > 0) {
				   sb.setLength(sb.length() - 1);
			}
			
			cursor.close(); 
			
			String serialData = sb.toString(); 
			Log.w(SyncForgottenWords.class.getName(), "Frogotten serialData: " + serialData); 
			
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
					String responseText = CustomHttpClient.executeHttpPost(syncForgottenURL, params).substring(0, 1);
					Log.w(SyncForgottenWords.class.getName(), "Forgotten response text:|" + responseText + "|." );
					if(responseText.equals("1"))
						return serialData; 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}  
		}
		// else case 
		return null; 
	}
	
	private boolean deleteForgottenNotSyncedRows(String toDeleteSerial) { 
		
		// converting string with serialized data to map of data used as deletion conditions (Where args)
		Map<Integer,Integer> toDeleteMap = new HashMap<Integer,Integer>();
		
		for(String keyValue : toDeleteSerial.split(";")) {
			String[] mapping = keyValue.split(",",2); 
			toDeleteMap.put(Integer.valueOf(mapping[0]), Integer.valueOf(mapping[1])); 
		}
		
		return deleteForgottenNotSyncedRows(toDeleteMap); 
	}
	
	/**
	 * Helper method used after successful 
	 * online synchronization of forgotten 
	 * to delete forgotten not_synced rows 
	 */
	private boolean deleteForgottenNotSyncedRows(Map<Integer, Integer> toDeleteMap) {
		
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(); 
		ContentProviderOperation operation; 
		
		for(Entry<Integer, Integer> entry : toDeleteMap.entrySet() ) { 
			Integer wordId = entry.getKey(); 
			Integer delta_weight = entry.getValue(); 
			
			Uri PROFILE_WORD_FORGOTTEN_NOT_SYNCED_URI = 
					Uri.parse(ForgottenNotSyncedProvider.CONTENT_URI + "/profile/" + profileId 
							+ "/word/" + wordId); 
			String selection = ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_DELTA_WEIGHT + " = ?"; 
			String[] selectionArgs = new String[] { String.valueOf(delta_weight) };
			
			operation = ContentProviderOperation.newDelete(PROFILE_WORD_FORGOTTEN_NOT_SYNCED_URI)
												.withSelection(selection, selectionArgs)
												.build();
			operations.add(operation); 
		}
		
		try { 
			ContentProviderResult[] results; 
			results = context.getContentResolver()
							.applyBatch(ForgottenNotSyncedProvider.AUTHORITY, operations);
			
			if(results != null && results.length > 0) { 
				Log.w(SyncForgottenWords.class.getName(), "Deleted forgotten not synced rows: " + results.length); 
				return true; 
			}
		} catch(RemoteException e) { 
			e.printStackTrace(); 
		} catch(OperationApplicationException e) { 
			e.printStackTrace();
		}
		
		Log.w(SyncForgottenWords.class.getName(), "Some error occured while deleting forgotten not synced rows."); 
		
		return false; 
	}
	
	/**
	 * Simple old helper function that deletes all rows in forgotten_not_synced table.
	 * It is not taking into account any external conditions. It can delete rows added 
	 * in the meantime of synchronization process, leading to missing some personalizations.
	 * @return
	 */
	private boolean deleteAllForgottenNotSyncedRows() {
		
		Uri PROFILE_FORGOTTEN_NOT_SYNCED_URI = Uri.parse(ForgottenNotSyncedProvider.CONTENT_URI + "/profile/" + profileId);
		
		int deleteCount = context.getContentResolver().delete(PROFILE_FORGOTTEN_NOT_SYNCED_URI, null, null);
		
		if(deleteCount > 0)
			return true; 
		
		return false; 
	}
	
	/**
	 * Helper method used after successful 
	 * retrieving synchronized forgotten 
	 * words from online web service 
	 * and before inserting them to 
	 * forgottenTable to delete forgotten entries
	 * for current profile from forgottenTable.
	 */
	private boolean deleteForgottenRows() {
		
		Uri PROFILE_FORGOTTEN_URI = Uri.parse(ForgottenProvider.CONTENT_URI + "/profile/" + profileId);
		
		int deleteCount = context.getContentResolver().delete(PROFILE_FORGOTTEN_URI, null, null);
		
		if(deleteCount > 0)
			return true; 
		
		return false; 
		
	}
	
	private void insertForgottenRows(ArrayList<ForgottenWord> forgottenWords) {
		
		if(forgottenWords == null) 
			return;
		
		ContentValues[] valuesCollection = new ContentValues[forgottenWords.size()]; 
		
		for(int i=0; i<forgottenWords.size(); i++) { 
			
			ForgottenWord forgotten = forgottenWords.get(i); 
			
			ContentValues values = new ContentValues(); 
			values.put(ForgottenProvider.ForgottenTable.COLUMN_PROFILE_ID, profileId);
			values.put(ForgottenProvider.ForgottenTable.COLUMN_WORD_ID, forgotten.translationId);
			values.put(ForgottenProvider.ForgottenTable.COLUMN_WEIGHT, forgotten.weight);
			
			valuesCollection[i] = values; 
			
			/* Uri insertedItemUri = context.getContentResolver().insert(ForgottenProvider.CONTENT_URI, values);
		       Log.w(SyncForgottenWords.class.getName(), "Inserting forgotten: " 
					+ forgotten.translationId + "," + profileId + ","  + forgotten.weight + " under: "
					+ insertedItemUri.toString());  */
		}
		
		int insertedCount = context.getContentResolver().bulkInsert(ForgottenProvider.CONTENT_URI, valuesCollection); 
		Log.w(SyncForgottenWords.class.getName(), "Bulk Inserted " + insertedCount + " forgotten rows to sqlite database.");
	}
}
