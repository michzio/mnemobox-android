/**
 * 
 */
package pl.electoroffline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import pl.elector.database.ForgottenNotSyncedProvider;
import pl.elector.database.ForgottenProvider;
import pl.elector.database.LearnedWordsNotSyncedProvider;
import pl.elector.database.LearnedWordsProvider;
import pl.elector.database.LearningHistoryProvider;
import pl.elector.database.LearningHistoryProvider.Mode;
import pl.elector.database.LearningStatsProvider;
import pl.elector.database.RememberMeNotSyncedProvider;
import pl.elector.database.RememberMeProvider;
import pl.elector.database.WordsetType;
import pl.elector.service.SyncPersonalizationService;
import pl.elector.service.WordsLoaderService.WordsDownloader;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Michał Ziobro 
 * @date 17.10.2014
 * 
 * This class is used to personalize wordsets while learning. 
 * 4 main things done by this object: 
 * 1) tracing and saving forgotten words, remember_me words and learned words
 * 	  in local database tables(forgotten, remember_me, learned) and its changes 
 * 	  in corresponding local not_synced table versions
 * 2) logic related to this personalization saving process depending on 
 * 	  current user state (logged in/anonymous), Internet status etc. 
 * 3) initialization of synchronization of aggregated changes in not_synced tables
 *    using special Services, Broadcast Receivers (Internet connection availability)
 *    and downloading synchronized data from server's database 
 * 4) loading personalization before learning process for WordsetWordsAccesor object 
 *    which later process this data suitably
 *
 */

public class Personalization {
	
	// forgotten words state 
	public enum Mood { 
        BAD, // when user completely doesn't know current word -> ForgottenWord ( delta = +1)
        NEUTRAL, // when user know current word in average level -> ForgottenWord (delta = -1) 
        GOOD // when user know current word greatly => add to LearnedWords, delete from ForgottenWords (delta = -10)
    }

	private Context context; 
	private Personalization.Callbacks callbacks; 
	
	public interface Callbacks {
    	
    	public void onWordsPersonalizationFinished(); 
    }
	
	// constructor 
	public Personalization(Context ctx) 
	{
		context = ctx; 
	}
	
	// method used to set object implementing interface Personalization.Callbacks
	public void setCallbacks(Personalization.Callbacks callbacks) 
	{
		this.callbacks = callbacks; 
	}
	
	/**
	 * This method checks if there are any forgotten, remember_me not_synced or learned not_synced
	 * words personalization available for anonymous user that can be ported to new profile account.
	 * @return
	 */
	public boolean checkAnonymousPersonalizationsAvailable() {
		
		Log.w(Personalization.class.getName(), "Checking anonymous personalizations are available...");
		
		Uri ANONYMOUS_FORGOTTEN_URI = Uri.parse(ForgottenProvider.CONTENT_URI + "/profile/0"); 
		String[] projection = { ForgottenProvider.ForgottenTable.COLUMN_FORGOTTEN_ID };
		
		Cursor cursor = context.getContentResolver().query(ANONYMOUS_FORGOTTEN_URI, projection, null, null, null);
		
		if(cursor.getCount() >0) 
			return true; 
		
		Uri ANONYMOUS_REMEMBER_ME_URI = Uri.parse(RememberMeNotSyncedProvider.CONTENT_URI + "/profile/0"); 
		projection = new String[] { RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_REMEMBER_ME_NOT_SYNCED_ID };
		
		cursor = context.getContentResolver().query(ANONYMOUS_REMEMBER_ME_URI, projection, null, null, null); 
		
		if(cursor.getCount() > 0) 
			return true; 
		
		Uri ANONYMOUS_LEARNED_URI = Uri.parse(LearnedWordsNotSyncedProvider.CONTENT_URI + "/profile/0"); 
		projection = new String[] { LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_LEARNED_WORDS_NOT_SYNCED_ID };
		
		cursor = context.getContentResolver().query(ANONYMOUS_LEARNED_URI, projection, null, null, null); 
		
		if(cursor.getCount() > 0)
			return true; 
		
		return false; 
	}
	

	// this methods use current profile_id stored in SharedPreferenced as User identifier 
	// it is tracing forgotten word based on mood value:
	// BAD =  current forgotten value +1 
	// NEUTRAL = current forgotten value -1 
	// GOOD = current forgotten value -10 (means delete given 
	//        row from forgottenTable, in not_synced table cause deletion 
	//        also on the web server after synchronization process
	public void traceForgottenWord(int wordId, Mood mood) 
	{
		// saving forgotten in forgottenTable & forgottenNotSyncedTable
		// using background thread
		new Thread(new ForgottenWordTracer(wordId, mood)).start();
	}
	
	// method used to trace word as learned by user 
	public void traceLearnedWord(int wordId) {
		this.traceLearnedWord(wordId, false); 
	}
	
	// overloaded method that takes in as a second parameter toDelete flag
	// indicating whether word is marked as to delete 
	public void traceLearnedWord(int wordId, boolean toDelete) {
		
		// saving learned word in learnedTable & learnedNotSyncedTable
		// using background thread
		new Thread(new LearnedWordTracer(wordId, toDelete)).start(); 
	}
	
	// method used to trace word to remember 
	public void traceRememberMeWord(int wordId) { 
		this.traceRememberMeWord(wordId, false);
	}
	
	public void traceRememberMeWord(int wordId, boolean toDelete) {
		
		// saving remember_me word in rememberMeTable & rememberMeNotSyncedTable
		// using background thread
		new Thread(new RememberMeWordTracer(wordId, toDelete)).start(); 
	}
	
	// method used to initialize synchronization process 
	public void synchronize() {
		
		// Initialize synchronization of personalizations with web server
		// using PersonalizationSyncService
		if(!NetworkUtilities.haveNetworkConnection(context))
		{
			Toast.makeText(context, R.string.cannot_sync_personalizations, Toast.LENGTH_SHORT).show(); 
			return; 
		}
		
		int profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
		if(profileId == 0) {
			new User(context).showPromptToLogIn(); 
			return; 
		}
		
		// launching Service that will sync data associated with personalizations
		// such as: forgotten words, remember_me words, learned words, download word's details 
		//          learning history, learning stats
		Intent syncServiceIntent = new Intent(context, SyncPersonalizationService.class);
		context.startService(syncServiceIntent); 
		Log.w(Personalization.class.getName(), "SyncPersonalizationService has been started...");
	}
	
	/**
	 * Method used to load words personalization for given wordIds 
	 * passed in ArrayList<Integer> by REFERENCE! 
	 * so any modifications done in this method on this ArrayList 
	 * will be reflected in ArrayList in method calling. 
	 * @param wordIds
	 */
	// usually used by WordsetWordsAccessor object
	public void loadWordsPersonalizations(final ArrayList<Integer> wordIds) {
		
		// Loading personalizations for given wordIds, i.e.
		// forgotten wordIds and weights, learned wordIds using background thread
		// from local database 
		
		/***
		 * ((Activity) context).runOnUiThread( new Runnable()  {
		 *
		 *	@Override
		 *	public void run() {
		 *		Toast.makeText(context, "Personalizowanie słówek....", Toast.LENGTH_SHORT).show();
		 *	} 
		 * 	
		 * });
		 ***/
		
		new Thread( new Runnable() {

			@Override
			public void run() {
				
				// 1) delete learned word_ids from ArrayList to skip this words while learning
				deleteLearnedWordsFrom(wordIds);
				
				// 2) multiply forgotten word_ids in ArrayList to improve learning of forgotten words
				multiplyForgottenWordsIn(wordIds); 
				
				// notify Personalization.Callbacks about finished words personalization process. 
				Personalization.this.callbacks.onWordsPersonalizationFinished(); 
			}
			
		}).start(); 
	}
	
	/**
	 * Helper method that multiplies word_ids 
	 * of forgotten words that should be shown 
	 * more often when learning to improve 
	 * their remembrance. 
	 * @param wordIds
	 */
	private void multiplyForgottenWordsIn(ArrayList<Integer> wordIds)
	{
		String wordIdsINSelection = asIN(wordIds); 
		int profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
		
		Uri FORGOTTEN_FOR_PROFILE_URI = Uri.parse(ForgottenProvider.CONTENT_URI + "/profile/" + profileId);
		String[] projection = { ForgottenProvider.ForgottenTable.COLUMN_WORD_ID, 
							    ForgottenProvider.ForgottenTable.COLUMN_WEIGHT };
		String selection = ForgottenProvider.ForgottenTable.COLUMN_WORD_ID + " IN " + wordIdsINSelection;
		String[] selectionArgs = new String[wordIds.size()]; 
		for(int i = 0; i< wordIds.size(); i++)  {
			selectionArgs[i] = "" + wordIds.get(i); 
		}
		
		Cursor cursor = context.getContentResolver().query(FORGOTTEN_FOR_PROFILE_URI, projection, selection, selectionArgs, null);
		
		while(cursor.moveToNext()) {
			int forgottenWordId = cursor.getInt(cursor.getColumnIndexOrThrow(ForgottenProvider.ForgottenTable.COLUMN_WORD_ID));
			int weight = cursor.getInt(cursor.getColumnIndexOrThrow(ForgottenProvider.ForgottenTable.COLUMN_WEIGHT));
			for(int i=0; i<weight; i++) { 
				wordIds.add( Integer.valueOf(forgottenWordId) );
			}
		}
	}
	
	/**
	 * Helper method that deletes word_ids of words 
	 * that should be skipped while learning 
	 * from ArrayList passed as only argument. 
	 * @param wordIds
	 */
	private void deleteLearnedWordsFrom(ArrayList<Integer> wordIds)
	{
		String wordIdsINSelection = asIN(wordIds); 
		int profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
		
		Uri LEARNED_FOR_PROFILE_URI = Uri.parse(LearnedWordsProvider.CONTENT_URI + "/profile/" + profileId);
		String[] projection = { LearnedWordsProvider.LearnedTable.COLUMN_WORD_ID };
		String selection = LearnedWordsProvider.LearnedTable.COLUMN_WORD_ID + " IN " + wordIdsINSelection;
		String[] selectionArgs = new String[wordIds.size()]; 
		for(int i = 0; i< wordIds.size(); i++)  {
			selectionArgs[i] = "" + wordIds.get(i); 
		}
		
		Cursor cursor = context.getContentResolver().query(LEARNED_FOR_PROFILE_URI, projection, selection, selectionArgs, null);
		
		while(cursor.moveToNext()) {
			int learnedWordId = cursor.getInt(cursor.getColumnIndexOrThrow(LearnedWordsProvider.LearnedTable.COLUMN_WORD_ID));
			wordIds.remove(Integer.valueOf(learnedWordId));
		}
	}
	
	/**
	 * Helper method that join wordIds List's
	 * items separated by commas as String
	 * inside parentheses.
	 * @param wordIds
	 * @return
	 */
	private String asIN(ArrayList<Integer> wordIds) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("("); 
		
		for(@SuppressWarnings("unused") Integer wordId : wordIds) 
		{
			//sb.append(wordId); 
			sb.append("?,"); 
		}
		
		// trimming last "," character
		if (sb.length() > 1) {
			 sb.setLength(sb.length() - 1);
		}
		
		sb.append(")"); 
		
		return sb.toString();
	}
	
	/**
	 * This class implements Runnable interface 
	 * and can be used to execute forgotten word tracing 
	 * logic in background thread.
	 */
	private class ForgottenWordTracer implements Runnable 
	{
		private int wordId; 
		private Mood mood; 
		private int profileId; // 0 - Anonymous
		
		// construct 
		public ForgottenWordTracer(int wordId, Mood mood)
		{
			this.wordId = wordId;
			this.mood = mood; 
		}
		
		@Override
		public void run() {
			
			// 1). getting user's profile identifier, if user is not logged in 
			//     we get default values 0, which means Anonymous user 
			profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
			
			
			// 2.) selecting suitable logic to save word as forgotten depending on Mood state
			switch(mood) 
			{
				case BAD:
					// change weight = weight + 1
					increaseForgottenWeight(); 
					saveForgottenChangesAsNotSynced(1);
					break; 
				case NEUTRAL: 
					// change weight = weight - 1
					decreaseForgottenWeight(); 
					saveForgottenChangesAsNotSynced(-1);
					break;
				case GOOD:
					// delete forgotten, weight = weight - 10
					deleteForgotten();
					saveForgottenChangesAsNotSynced(-10); 
					break; 
				default: 
					throw new IllegalArgumentException(); 
			}
			
			
		}
		
		/**
		 * Method used to update/insert row for (profileId, wordId) 
		 * with weight increased by +1 in forgottenTable.
		 */
		private void increaseForgottenWeight() {
			
			// 1.) select current row for (profileId, wordId) 
			Cursor cursor = selectForgottenRow(profileId, wordId);
			
			// 2.) if there isn't any such row 
			if(cursor.getCount() == 0)
			{
				// 2.a) INSERT new one with weight = 1
				insertForgottenRow(profileId, wordId, 1); 
				
			} else if(cursor.getCount() == 1)
			{
				// 2.b) else if weight < 4 UPDATE its value by +1 
				cursor.moveToFirst(); 
				int forgottenID = cursor.getInt(cursor.getColumnIndexOrThrow(ForgottenProvider.ForgottenTable.COLUMN_FORGOTTEN_ID));
				int weight = cursor.getInt(cursor.getColumnIndexOrThrow(ForgottenProvider.ForgottenTable.COLUMN_WEIGHT));
				
				if(weight < 4) { 
					
					updateForgottenRow(forgottenID, (weight+1) );
					
				} else {
					Log.w(ForgottenWordTracer.class.getName(), "Forgotten word has currently max value 4, update isn't needed.");
				}
 				
			} else { 
				Log.w(ForgottenWordTracer.class.getName(), 
					 "Error while selecting row from forgottenTable for given profile and word id.");
				cursor.close();
				return; 
			} 	
			
			cursor.close();
			
		}
		
		/**
		 * Method used to delete/update row for (profileId, wordId) 
		 * with weight decreased by (-1) from/in forgottenTable.
		 */
		private void decreaseForgottenWeight() {
		
			// 1) select current row for (profileId, wordId)
			Cursor cursor = selectForgottenRow(profileId, wordId); 
			
			// 2) if there isn't any such row do nothing
			//    else: 
			if(cursor.getCount() == 1) 
			{
				// 3) depending on weight value 
				cursor.moveToFirst();
				int forgottenID = cursor.getInt(cursor.getColumnIndexOrThrow(ForgottenProvider.ForgottenTable.COLUMN_FORGOTTEN_ID));
				int weight = cursor.getInt(cursor.getColumnIndexOrThrow(ForgottenProvider.ForgottenTable.COLUMN_WEIGHT));
				
				if(weight > 1) { 
					// 3a) if it is greater than 1, UPDATE it by -1 
					updateForgottenRow(forgottenID, (weight-1) );
					
				} else { 
					// 3b) else DELETE this row FROM forgottenTable
					final Uri DELETE_FORGOTTEN_CONTENT_URI = Uri.parse(ForgottenProvider.CONTENT_URI + "/" + forgottenID);
					int deleteCount = context.getContentResolver().delete(DELETE_FORGOTTEN_CONTENT_URI, null, null); 
					
					if(deleteCount == 1) 
						Log.w(ForgottenWordTracer.class.getName(), "Forgotten row: (" + profileId +"," + wordId + ") deleted properly."); 
					else 
						Log.w(ForgottenWordTracer.class.getName(), "An error occured while deleting row from forgottenTable.");
				}
			
			} else if(cursor.getCount() > 1) {
				Log.w(ForgottenWordTracer.class.getName(), 
					  "An error occured while selecting row from forgottenTable for given profile and word id.");
			}
			cursor.close(); 
			
		}

		/**
		 * Method used to delete row for (profileId, wordId) 
		 * from forgottenTable.
		 */
		private void deleteForgotten() {
			
			// when user selected Mood.GOOD or add word to learned words (Mood.GOOD) 
			// corresponding row in forgottenTable must be deleted (and change saved 
			// in forgottenNotSyncedTable is -10, done in saveForgottenChangesAsNotSynced() ) 
			Uri DELETE_FORGOTTEN_CONTENT_URI = Uri.parse(ForgottenProvider.CONTENT_URI 
					  							+ "/profile/" + profileId
					  							+ "/word/" + wordId);
			
			int deleteCount = context.getContentResolver().delete(DELETE_FORGOTTEN_CONTENT_URI, null, null);
			
			if(deleteCount == 1)
				Log.w(ForgottenWordTracer.class.getName(), "Forgotten row: (" + profileId + "," + wordId + ") deleted properly.");
			else 
				Log.w(ForgottenWordTracer.class.getName(), "Forgotten row to delete hasn't been found."); 
			
		}

		/**
		 * Method used to save change on forgottenTable
		 * in special forgottenNotSyncedTable, which will be used 
		 * while synchronizing personalization with online web server.
		 * @param deltaWeight - is current change of weight
		 */
		private void saveForgottenChangesAsNotSynced(int deltaWeight) {
			
			// 1) selecting row for given profileId and wordId 
			Cursor cursor = selectForgottenNotSyncedRow(profileId, wordId);
			
			/** 
			 * DEPRECATED: 
			 * if(cursor == null) {
			 *   Log.w(Personalization.class.getName(), "Cursor is null!!!"); 
			 *	 return; 
			 *	}
			 */
			
			// 2) if such row doesn't exists in forgottenNotSynced table 
			if(cursor.getCount() == 0) {
				
				// 2a) INSER a new row with current deltaWeight value
				insertForgottenNotSyncedRow(profileId, wordId, deltaWeight); 
			} else if(cursor.getCount() == 1) {
				cursor.moveToFirst(); 
				int rowID = cursor.getInt(cursor.getColumnIndexOrThrow(
						ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_FORGOTTEN_NOT_SYNCED_ID));
				int aggregatedDeltaWeight = cursor.getInt(cursor.getColumnIndexOrThrow(
						ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_DELTA_WEIGHT));
				
				// 2b) else if such row exists, 
				//     UPDATE it setting aggregatedDeltaWeight += deltaWeight
				updateForgottenNotSyncedRow(rowID, (aggregatedDeltaWeight + deltaWeight) ); 
				
			} else { 
				Log.w(ForgottenWordTracer.class.getName(), 
					  "An error occured while selecting row from forgottenNotSyncedTable for given profile and word id.");
			}
			
			cursor.close(); 
			
		}
		
		/**
		 * Helper method used by decreaseForgottenWeight(),
		 * and increaseForgottenWeight() to return corresponding forgotten row. 
		 * @param profileId
		 * @param wordId
		 * @return
		 */
		private Cursor selectForgottenRow(int profileId, int wordId)
		{
			final Uri FORGOTTEN_CONTENT_URI = Uri.parse(ForgottenProvider.CONTENT_URI 
					  + "/profile/" + profileId
					  + "/word/" + wordId);
			String[] projection = { ForgottenProvider.ForgottenTable.COLUMN_FORGOTTEN_ID,
									ForgottenProvider.ForgottenTable.COLUMN_WEIGHT };

			Cursor cursor = context.getContentResolver().query(FORGOTTEN_CONTENT_URI, projection, null, null, null);
			
			return cursor; 
		}
		
		private Cursor selectForgottenNotSyncedRow(int profileId, int wordId)
		{
			final Uri FORGOTTEN_NOT_SYNCED_CONTENT_URI = Uri.parse(ForgottenNotSyncedProvider.CONTENT_URI 
					  + "/profile/" + profileId
					  + "/word/" + wordId);
			String[] projection = { ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_FORGOTTEN_NOT_SYNCED_ID,
									ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_DELTA_WEIGHT };
			
			Cursor cursor = context.getContentResolver()
								   .query(FORGOTTEN_NOT_SYNCED_CONTENT_URI, projection, null, null, null);
			
			return cursor; 
		}

		private boolean updateForgottenRow(int forgottenID, int updatedWeight) 
		{
			final Uri FORGOTTEN_UPDATE_CONTENT_URI = Uri.parse(ForgottenProvider.CONTENT_URI + "/" + forgottenID); 
			
			ContentValues updatedValues = new ContentValues(); 
			updatedValues.put(ForgottenProvider.ForgottenTable.COLUMN_WEIGHT, updatedWeight);
			
			int updatedCount = 
				context.getContentResolver().update(FORGOTTEN_UPDATE_CONTENT_URI, updatedValues, null, null);
			
			if(updatedCount == 1) { 
				Log.w(ForgottenWordTracer.class.getName(), "Frogotten word (" + profileId + "," + wordId + ") updated successfuly to value: " + updatedWeight + ".");
				return true; 
			} else { 
				Log.w(ForgottenWordTracer.class.getName(), "An error has occured while updating forgotten word ("+ profileId + "," + wordId + ").");
				return false; 
			} 
		}
		
		private boolean updateForgottenNotSyncedRow(int forgottenNotSyncedID, int aggregatedDeltaWeight)
		{
			final Uri FORGOTTEN_NOT_SYNCED_UPDATE_CONTENT_URI = 
					Uri.parse(ForgottenNotSyncedProvider.CONTENT_URI + "/" + forgottenNotSyncedID); 
			
			ContentValues updatedValues = new ContentValues(); 
			updatedValues.put(ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_DELTA_WEIGHT, aggregatedDeltaWeight);
			
			int updatedCount = 
					context.getContentResolver().update(FORGOTTEN_NOT_SYNCED_UPDATE_CONTENT_URI, updatedValues, null, null);
			
			if(updatedCount == 1) { 
				Log.w(ForgottenWordTracer.class.getName(), "Frogotten not synced row (" + profileId + "," + wordId + ") updated successfuly to value: " + aggregatedDeltaWeight + ".");
				return true; 
			} else { 
				Log.w(ForgottenWordTracer.class.getName(), "An error has occured while updating forgotten not synced row ("+ profileId + "," + wordId + ").");
				return false; 
			} 
		}
		
		private Uri insertForgottenRow(int profileId, int wordId, int weight)
		{
			ContentValues forgottenValues = new ContentValues(); 
			forgottenValues.put(ForgottenProvider.ForgottenTable.COLUMN_PROFILE_ID, profileId);
			forgottenValues.put(ForgottenProvider.ForgottenTable.COLUMN_WORD_ID, wordId);
			forgottenValues.put(ForgottenProvider.ForgottenTable.COLUMN_WEIGHT, weight); 
			
			Uri insertedItemUri = 
					context.getContentResolver().insert(ForgottenProvider.CONTENT_URI, forgottenValues);
			
			Log.w(ForgottenWordTracer.class.getName(), "Inserted new forgotten word under: " 
					+ insertedItemUri.toString() + " with values: (" + profileId + "," + wordId + "," + weight + ").");
			
			return insertedItemUri;
		}
		
		private Uri insertForgottenNotSyncedRow(int profileId, int wordId, int deltaWeight)
		{
			ContentValues values = new ContentValues(); 
			values.put(ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_PROFILE_ID, profileId); 
			values.put(ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_WORD_ID, wordId); 
			values.put(ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_DELTA_WEIGHT, deltaWeight); 
			
			Uri insertedItemUri = context.getContentResolver().insert(ForgottenNotSyncedProvider.CONTENT_URI, values);
			
			Log.w(ForgottenWordTracer.class.getName(), "Inserted new forgotten not synced row under: " 
					+ insertedItemUri.toString() + " with values: (" + profileId + "," + wordId + "," + deltaWeight + ").");
			
			return insertedItemUri; 
		}
		
	}
	
	/**
	 * This class implements Runnable interface 
	 * and can be used to execute rememberMe word tracing 
	 * logic in background thread.
	 */
	private class RememberMeWordTracer implements Runnable 
	{
		private int wordId; 
		private boolean toDelete; 
		private int profileId; 
		
		public RememberMeWordTracer(int wordId, boolean toDelete) {
			this.wordId = wordId; 
			this.toDelete = toDelete;
		}

		@Override
		public void run() {
			
			// 1). getting user's profile identifier, if user is not logged in 
			//     we get default values 0, which means Anonymous user 
			profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
			
			// 2). selecting suitable logic depending on remember me is to delete or not
			if(toDelete) { 
				deleteRememberMeRow();
			} else { 
				saveRememberMeRow();
			}
			
			// 3). saving changes in remember_me not_synced table for further synchronization
			saveRememberMeChangesAsNotSynced();
		}

		/**
		 * Method deletes row for wordId 
		 * from rememberMeTable.
		 * @param wordId
		 */
		private boolean deleteRememberMeRow() {
			
			Uri DELETE_REMEMBER_ME_CONTENT_URI = Uri.parse(RememberMeProvider.CONTENT_URI 
															+ "/profile/" + profileId
															+ "/word/" + wordId);
			
			int deleteCount = context.getContentResolver().delete(DELETE_REMEMBER_ME_CONTENT_URI, null, null);
			
			if(deleteCount == 1) { 
				Log.w(RememberMeWordTracer.class.getName(), 
					   "Remember Me row: (" + profileId + "," + wordId + ") has been deleted from rememberMeTable properly.");
				return true; 
			} else { 
				Log.w(RememberMeWordTracer.class.getName(), 
					   "Some error occured while trying to delete rememberMe row (" + profileId + "," + wordId + ") or row hasn't existed.");
				return false; 
			}
		}
		
		/**
		 * Method used to save (INSERT if not exists) 
		 * remember me row in rememberMeTable for given wordId and profileId
		 */
		private void saveRememberMeRow() {
			
			// 1). try SELECT remember me row for given wordId and profileId
			if(!checkRememberMeRowExists())
			{
				// if rememeber_me row doesn't exists in rememberMeTable we must save it in database
				
				// 2). check whether in wordTable exists word with wordId (rememberMeWord must be stored locally)
				if(!WordsDownloader.checkWordExists(context, wordId)) { 
					
					// if given word doesn't exists, download word details, image and recording and save it locally
					if(NetworkUtilities.haveNetworkConnection(context)) {
						if(!WordsDownloader.downloadWordDetails(context, wordId))
							return;
					} else { 
						// if there isn't Internet connection word details can not be download 
						// so remember_me row in table rememberMeTable can not be added instantly 
						// (It will be added for not existing words during scheduled personalizations 
						//  online synchronization)
						return;
					}
				}
				// 3) insert remember_me row to rememberMeTable for current wordId and profileId 
				insertRememberMeRow(); 
			}
			
		}

		/**
		 * Helper method that checks whether rememberMe row for current 
		 * wordId and profileId exists in rememberMeTable
		 * @return
		 */
		private boolean checkRememberMeRowExists() {
			
			Uri SELECT_REMEMBER_ME_ROW_CONTENT_URI = Uri.parse(RememberMeProvider.CONTENT_URI 
															  + "/profile/" + profileId
															  + "/word/" + wordId);
			
			String[] projection = { RememberMeProvider.RememberMeTable.COLUMN_REMEMBER_ME_ID };
			
			Cursor cursor = context.getContentResolver()
								   .query(SELECT_REMEMBER_ME_ROW_CONTENT_URI, projection, null, null, null); 
			
			if(cursor.getCount() == 1) { 
				Log.w(RememberMeWordTracer.class.getName(), "Remember me row exists for (" + profileId + "," + wordId + ").");
				cursor.close();
				return true; 
			} else { 
				Log.w(RememberMeWordTracer.class.getName(), "Remember me row doesn't exists for (" + profileId + "," + wordId + ") or error has occured.");
				cursor.close();
				return false;
			}
			
		}
		
		
		/**
		 * Helper method used to INSERT new 
		 * row into rememberMe table. 
		 */
		private Uri insertRememberMeRow() {
		
			
			ContentValues values = new ContentValues(); 
			values.put(RememberMeProvider.RememberMeTable.COLUMN_PROFILE_ID, profileId); 
			values.put(RememberMeProvider.RememberMeTable.COLUMN_WORD_ID, wordId); 
			
			Uri insertedItemUri = context.getContentResolver().insert(RememberMeProvider.CONTENT_URI, values);
			
			Log.w(RememberMeWordTracer.class.getName(), 
				 "Remember me row inserted under: " + insertedItemUri + " for (" + profileId + "," + wordId + ").");
			
			return insertedItemUri; 
			
		}
		
		/**
		 * Method used to save change in remember_me in 
		 * special rememberMeNotSyncedTable that is used 
		 * while synchronizing with online web server. 
		 */
		private void saveRememberMeChangesAsNotSynced() {
			
			Cursor cursor = selectRememberMeNotSyncedRow(profileId, wordId); 
			
			// 1) checking whether corresponding row for profileId and wordId exists
			if(cursor.getCount() == 0) { 
				
					// when doesn't exists, insert new one for profileId and wordId with current toDelete value
					insertRememberMeNotSyncedRow(profileId, wordId, toDelete); 
			} else if(cursor.getCount() == 1) { 
					// when row exists, update existing one with current toDelete value
					cursor.moveToFirst();
				    int rememberMeNotSyncedID = cursor.getInt(cursor.getColumnIndexOrThrow(
				    			RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_REMEMBER_ME_NOT_SYNCED_ID));
					updateRememberMeNotSyncedRow(rememberMeNotSyncedID, toDelete); 
			} else { 
				Log.w(RememberMeWordTracer.class.getName(), 
					  "Error while selecting row from rememberMeNotSyncedTable for given profile and word id.");
				cursor.close();
				return; 
			}
			cursor.close(); 
		}
	

		private Cursor selectRememberMeNotSyncedRow(int profileId, int wordId) {
			
			final Uri REMEMBER_ME_NOT_SYNCED_CONTENT_URI = Uri.parse(RememberMeNotSyncedProvider.CONTENT_URI 
					  												+ "/profile/" + profileId
					  												+ "/word/" + wordId);
			String[] projection = { RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_REMEMBER_ME_NOT_SYNCED_ID };
			
			Cursor cursor = context.getContentResolver()
					   			   .query(REMEMBER_ME_NOT_SYNCED_CONTENT_URI, projection, null, null, null);
			
			return cursor;
		}
		
		/**
		 * Method used to INSERT new row into rememberMeNotSyncedTable
		 * which will be used while synchronizing rememberMe words with web server.
		 * @param profileId
		 * @param wordId
		 * @param toDelete
		 */
		private Uri insertRememberMeNotSyncedRow(int profileId, int wordId,
				boolean toDelete) {
			
			ContentValues values = new ContentValues(); 
			values.put(RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_PROFILE_ID, profileId);
			values.put(RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_WORD_ID, wordId); 
			values.put(RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_TO_DELETE, toDelete); 
			
			Uri insertedItemUri = context.getContentResolver().insert(RememberMeNotSyncedProvider.CONTENT_URI, values);
			
			Log.w(RememberMeWordTracer.class.getName(), 
					 "Remember me not_synced row inserted under: " + insertedItemUri + " for (" + profileId + "," + wordId + ").");
			
			return insertedItemUri; 
		}
		
		/**
		 * Method used to UPDATE existing row in rememberMeNotSyncedTable
		 * for current rememberMeNotSyncedID (profileID, wordID) with new toDelete value.
		 * @param rememberMeNotSyncedID
		 * @param toDelete
		 */
		private boolean updateRememberMeNotSyncedRow(int rememberMeNotSyncedID,
				boolean toDelete) {
			
			ContentValues updatedValues = new ContentValues();
			updatedValues.put(RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_TO_DELETE, toDelete); 
			
			Uri UPDATE_REMEMBER_ME_NOT_SYNCED_CONTENT_URI = 
					Uri.parse(RememberMeNotSyncedProvider.CONTENT_URI + "/" + rememberMeNotSyncedID); 
			
			int updatedCount = context.getContentResolver()
									  .update(UPDATE_REMEMBER_ME_NOT_SYNCED_CONTENT_URI, updatedValues, null, null);
			if(updatedCount == 1) {
				Log.w(RememberMeWordTracer.class.getName(), "Remember me not_synced row updated properly."); 
				return true;
			} else { 
				Log.w(RememberMeWordTracer.class.getName(), "An error occured while updating rememberMeNotSyncedTable row.");
				return false; 
			}
		}

	}
	
	/**
	 * This class implements Runnable interface 
	 * and can be used to execute learned word tracing 
	 * logic in background thread.
	 */
	private class LearnedWordTracer implements Runnable 
	{
		private int wordId; 
		private boolean toDelete; 
		private int profileId; 
		
		public LearnedWordTracer(int wordId, boolean toDelete) {
			this.wordId = wordId; 
			this.toDelete = toDelete;
		}

		@Override
		public void run() {
			
			// 1). getting user's profile identifier, if user is not logged in 
			//     we get default values 0, which means Anonymous user 
			profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
			
			// 2). selecting suitable logic depending on learned is to delete or not
			if(toDelete) { 
				deleteLearnedRow();
			} else { 
				saveLearnedRow();
			}
			
			// 3). saving changes in learned word not_synced table for further synchronization
			saveLearnedChangesAsNotSynced();
		}

		/**
		 * Method deletes row for wordId and profileId
		 * from learnedWordsTable.
		 * @param wordId
		 */
		private boolean deleteLearnedRow() {
			
			Uri DELETE_LEARNED_CONTENT_URI = Uri.parse(LearnedWordsProvider.CONTENT_URI 
															+ "/profile/" + profileId
															+ "/word/" + wordId);
			
			int deleteCount = context.getContentResolver().delete(DELETE_LEARNED_CONTENT_URI, null, null);
			
			if(deleteCount == 1) { 
				Log.w(LearnedWordTracer.class.getName(), 
					   "Learned row: (" + profileId + "," + wordId + ") has been deleted from learnedWordsTable properly.");
				return true; 
			} else { 
				Log.w(LearnedWordTracer.class.getName(), 
					   "Some error occured while trying to delete learned row (" + profileId + "," + wordId + ") or row hasn't existed.");
				return false; 
			}
		}
		
		/**
		 * Method used to save (INSERT if not exists) 
		 * learned row in learnedWordsTable for given wordId and profileId
		 */
		private void saveLearnedRow() {
			
			// 1). try SELECT learned row for given wordId and profileId
			if(!checkLearnedRowExists())
			{
				// if learned row doesn't exists in learnedWordsTable we must save it in database
				
				// 2). check whether in wordTable exists word with wordId (learnedWord must be stored locally)
				if(!WordsDownloader.checkWordExists(context, wordId)) { 
					
					// if given word doesn't exists, download word details, image and recording and save it locally
					if(NetworkUtilities.haveNetworkConnection(context)) {
						if(!WordsDownloader.downloadWordDetails(context, wordId))
							return;
					} else { 
						// if there isn't Internet connection word details can not be download 
						// so learned row in table learnedWordsTable can not be added instantly 
						// (It will be added for not existing words during scheduled personalizations 
						//  online synchronization)
						return;
					}
				}
				// 3) insert learned row to learnedWordsTable for current wordId and profileId 
				insertLearnedRow(); 
			}
			
		}

		/**
		 * Helper method that checks whether learned row for current 
		 * wordId and profileId exists in learnedWordsTable
		 * @return
		 */
		private boolean checkLearnedRowExists() {
			
			Uri SELECT_LEARNED_ROW_CONTENT_URI = Uri.parse(LearnedWordsProvider.CONTENT_URI 
															  + "/profile/" + profileId
															  + "/word/" + wordId);
			
			String[] projection = { LearnedWordsProvider.LearnedTable.COLUMN_LEARNED_ID };
			
			Cursor cursor = context.getContentResolver()
								   .query(SELECT_LEARNED_ROW_CONTENT_URI, projection, null, null, null); 
			
			if(cursor.getCount() == 1) { 
				Log.w(LearnedWordTracer.class.getName(), "Learned row exists for (" + profileId + "," + wordId + ").");
				cursor.close();
				return true; 
			} else { 
				Log.w(LearnedWordTracer.class.getName(), "Learned row doesn't exists for (" + profileId + "," + wordId + ") or error has occured.");
				cursor.close(); 
				return false;
			}
			
		}
		
		/**
		 * Helper method used to INSERT new 
		 * row into learned table. 
		 */
		private Uri insertLearnedRow() {
		
			
			ContentValues values = new ContentValues(); 
			values.put(LearnedWordsProvider.LearnedTable.COLUMN_PROFILE_ID, profileId); 
			values.put(LearnedWordsProvider.LearnedTable.COLUMN_WORD_ID, wordId); 
			
			Uri insertedItemUri = context.getContentResolver().insert(LearnedWordsProvider.CONTENT_URI, values);
			
			Log.w(LearnedWordTracer.class.getName(), 
				 "Learned row inserted under: " + insertedItemUri + " for (" + profileId + "," + wordId + ").");
			
			return insertedItemUri; 
			
		}
		
		/**
		 * Method used to save change in learned in 
		 * special learnedWordsNotSyncedTable that is used 
		 * while synchronizing with online web server. 
		 */
		private void saveLearnedChangesAsNotSynced() {
			
			Cursor cursor = selectLearnedNotSyncedRow(profileId, wordId); 
			
			// 1) checking whether corresponding row for profileId and wordId exists
			if(cursor.getCount() == 0) { 
				
					// when doesn't exists, insert new one for profileId and wordId with current toDelete value
					insertLearnedNotSyncedRow(profileId, wordId, toDelete); 
			} else if(cursor.getCount() == 1) { 
					// when row exists, update existing one with current toDelete value
					cursor.moveToFirst();
				    int learnedNotSyncedID = cursor.getInt(cursor.getColumnIndexOrThrow(
				    			LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_LEARNED_WORDS_NOT_SYNCED_ID));
					updateLearnedNotSyncedRow(learnedNotSyncedID, toDelete); 
			} else { 
				Log.w(LearnedWordTracer.class.getName(), 
					  "Error while selecting row from learnedWordsNotSyncedTable for given profile and word id.");
				cursor.close();
				return; 
			}
			cursor.close();
		}
	

		private Cursor selectLearnedNotSyncedRow(int profileId, int wordId) {
			
			final Uri LEARNED_NOT_SYNCED_CONTENT_URI = Uri.parse(LearnedWordsNotSyncedProvider.CONTENT_URI 
					  												+ "/profile/" + profileId
					  												+ "/word/" + wordId);
			
			String[] projection = { LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_LEARNED_WORDS_NOT_SYNCED_ID };
			
			Cursor cursor = context.getContentResolver()
					   			   .query(LEARNED_NOT_SYNCED_CONTENT_URI, projection, null, null, null);
			
			return cursor;
		}
		
		/**
		 * Method used to INSERT new row into learnedWordsNotSyncedTable
		 * which will be used while synchronizing learned words with web server.
		 * @param profileId
		 * @param wordId
		 * @param toDelete
		 */
		private Uri insertLearnedNotSyncedRow(int profileId, int wordId, boolean toDelete) {
			
			ContentValues values = new ContentValues(); 
			values.put(LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_PROFILE_ID, profileId);
			values.put(LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_WORD_ID, wordId); 
			values.put(LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_TO_DELETE, toDelete); 
			
			Uri insertedItemUri = context.getContentResolver().insert(LearnedWordsNotSyncedProvider.CONTENT_URI, values);
			
			Log.w(LearnedWordTracer.class.getName(), 
					 "Learned not_synced row inserted under: " + insertedItemUri + " for (" + profileId + "," + wordId + ").");
			
			return insertedItemUri; 
		}
		
		/**
		 * Method used to UPDATE existing row in learnedNotSyncedTable
		 * for current learnedWordsNotSyncedID (profileID, wordID) with new toDelete value.
		 * @param learnedNotSyncedID
		 * @param toDelete
		 */
		private boolean updateLearnedNotSyncedRow(int learnedNotSyncedID, boolean toDelete) {
			
			ContentValues updatedValues = new ContentValues();
			updatedValues.put(LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_TO_DELETE, toDelete); 
			
			Uri UPDATE_LEARNED_NOT_SYNCED_CONTENT_URI = 
					Uri.parse(LearnedWordsNotSyncedProvider.CONTENT_URI + "/" + learnedNotSyncedID); 
			
			int updatedCount = context.getContentResolver()
									  .update(UPDATE_LEARNED_NOT_SYNCED_CONTENT_URI, updatedValues, null, null);
			if(updatedCount == 1) {
				Log.w(LearnedWordTracer.class.getName(), "Learned not_synced row updated properly."); 
				return true;
			} else { 
				Log.w(LearnedWordTracer.class.getName(), "An error occured while updating learnedWordsNotSyncedTable row.");
				return false; 
			}
		}

	}
	
	/**
	 * Method used to ask user whether he want to port 
	 * existing anonymous user words personalization 
	 * to his new profile account.
	 */
	
	public void showPromptToPortAnonymousPersonalizations()
	{
		showPromptToPortAnonymousPersonalizations(null); 
	}
	public void showPromptToPortAnonymousPersonalizations(final User.LogInCallbacks logInCallbacks)
	{
		Log.w(Personalization.class.getName(), "Showing prompt to ask user to port anonymous personalization..."); 
		
		 final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context)
		 .setMessage(R.string.port_existing_personalizations)
	     .setCancelable(false)
		 .setPositiveButton(R.string.yes_button,  new DialogInterface.OnClickListener() {
					
				@Override
				public void onClick(DialogInterface dialog, int which) {
						
					portAnonymousPersonalizationsToNewAccount();
					
					if(logInCallbacks != null) { 
						int profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
						if(profileId > 0)
							logInCallbacks.onLogInFinished(true);
						else 
							logInCallbacks.onLogInFinished(false); 
					}
						
					
				}
			})
		 .setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
					
				@Override			
				public void onClick(DialogInterface dialog, int which) {
					
					//skip words personalization porting operation
					if(logInCallbacks != null) { 
						int profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
						if(profileId > 0)
							logInCallbacks.onLogInFinished(true);
						else 
						    logInCallbacks.onLogInFinished(false); 
							
					}
					return; 
				}
			});
		 
		 ((Activity) context).runOnUiThread(new Runnable() {

				@Override
				public void run() {
					 AlertDialog dialog = dialogBuilder.create();
					 Log.w(Personalization.class.getName(), 
							 "Prompting user to port his current personalizations from anonymous account.");
					 dialog.show();
				}
		 });
	}
	
	/**
	 * Helper method that ports existing anonymous personalizations 
	 * to new account (which profile id is stored in SharedPreferences).
	 * This methods is updating forgotten, forgotten_not_synced, 
	 * remember_me, remember_me_not_synced, learned, learned_not_synced
	 * tables where profileId = 0 setting profilId = newProfileId.
	 */
	private boolean portAnonymousPersonalizationsToNewAccount() { 
		
		int profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
		if(profileId == 0) 
			return false; 
		
		boolean success = false;
		
		// 1) updating forgotten table rows with profileId=0 -> profileId = newProfileId
		ContentValues values = new ContentValues(); 
		values.put(ForgottenProvider.ForgottenTable.COLUMN_PROFILE_ID, profileId); 
		
		Uri FORGOTTEN_FOR_PROFILE_URI = Uri.parse(ForgottenProvider.CONTENT_URI + "/profile/0"); 
		
		int updateCount = context.getContentResolver().update(FORGOTTEN_FOR_PROFILE_URI, values, null, null); 
		if(updateCount > 0) 
			success = true; 
		
		// 2) updating forgotten_not_synced table rows with profileId=0 -> profileId = newProfileId
		values = new ContentValues(); 
		values.put(ForgottenNotSyncedProvider.ForgottenNotSyncedTable.COLUMN_PROFILE_ID, profileId);
		
		Uri FORGOTTEN_NOT_SYNCED_FOR_PROFILE_URI = Uri.parse(ForgottenNotSyncedProvider.CONTENT_URI + "/profile/0"); 
		
		updateCount = context.getContentResolver().update(FORGOTTEN_NOT_SYNCED_FOR_PROFILE_URI, values, null, null); 
		if(updateCount > 0)
			success = true; 
		
		// 3) updating remember_me table rows with profileId=0 -> profileId = newProfileId
		values = new ContentValues(); 
		values.put(RememberMeProvider.RememberMeTable.COLUMN_PROFILE_ID, profileId); 
		
		Uri REMEMBER_ME_FOR_PROFILE_URI = Uri.parse(RememberMeProvider.CONTENT_URI + "/profile/0");
		
		updateCount = context.getContentResolver().update(REMEMBER_ME_FOR_PROFILE_URI, values, null, null); 
		if(updateCount > 0) 
			success = true; 
		
		// 4) updating remember_me_not_synced table rows with profileId=0 -> profileId = newProfileId 
		values = new ContentValues(); 
		values.put(RememberMeNotSyncedProvider.RememberMeNotSyncedTable.COLUMN_PROFILE_ID, profileId); 
		
		Uri REMEMBER_ME_NOT_SYNCED_FOR_PROFILE_URI = Uri.parse(RememberMeNotSyncedProvider.CONTENT_URI + "/profile/0"); 
		
		updateCount = context.getContentResolver().update(REMEMBER_ME_NOT_SYNCED_FOR_PROFILE_URI, values, null, null); 
		if(updateCount > 0)
			success = true; 
		
		// 5) updating learned table rows with profileId=0 -> profileId = newProfileId
		values = new ContentValues(); 
		values.put(LearnedWordsProvider.LearnedTable.COLUMN_PROFILE_ID, profileId); 
		
		Uri LEARNED_FOR_PROFILE_URI = Uri.parse(LearnedWordsProvider.CONTENT_URI + "/profile/0");
		
		updateCount = context.getContentResolver().update(LEARNED_FOR_PROFILE_URI, values, null, null); 
		if(updateCount > 0)
			success = true;
		
		// 6) updating learned_not_synced table rows with profileId=0 -> profileId = newProfileId
		values = new ContentValues(); 
		values.put(LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.COLUMN_PROFILE_ID, profileId); 
		
		Uri LEARNED_NOT_SYNCED_FOR_PROFILE_URI = Uri.parse(LearnedWordsNotSyncedProvider.CONTENT_URI + "/profile/0");
		
		updateCount = context.getContentResolver().update(LEARNED_NOT_SYNCED_FOR_PROFILE_URI, values, null, null); 
		if(updateCount > 0)
			success = true; 
		
		return success; 
	}

	/**
	 * This method is used to persist in SQLite database information about 
	 * last learning session. This enables to trace learning history and its statistics. 
	 * @param wordsetId - identifier of learned wordset 
	 * @param learningMode - ex. flash cards, presentation, repetition,...
	 * @param wordsetType - system, user, selected, remember_me,...
	 * @param badAns - number of bad answers in last learning session
	 * @param goodAns - number of good answers in last learning session
	 */
	public void traceLearningHistoryAndStatistics(int wordsetId, Mode learningMode, WordsetType wordsetType,
												  int badAns, int goodAns, OnTracerCompletedListener callback) {
		
		// saving learning history in learninHistoryTable & statistics in learningStatsTable
		// using background thread
		new Thread(new LearningHistoryAndStatisticsTracer(wordsetId, learningMode, wordsetType, badAns, goodAns, callback)).start();
	}
	
	/**
	 * Callback interface onTracerCompletedListerner used to notify 
	 * invoker about tracing task completion.
	 */
	public interface OnTracerCompletedListener { 
		public void onTracerCompleted(); 
	}
	
	/**
	 * This class implements Runnable interface 
	 * and can be used to execute learning history 
	 * and statistics tracing logic in background thread.
	 */
	public class LearningHistoryAndStatisticsTracer implements Runnable 
	{
		
		private int profileId; // 0 - Anonymous

		private int wordsetId;
		private Mode learningMode; 
		private WordsetType wordsetType; 
		private int badAns; 
		private int goodAns; 
		
		private Handler handler; 
		private OnTracerCompletedListener callback; 
		
		// construct 
		public LearningHistoryAndStatisticsTracer(int wordsetId, Mode learningMode, WordsetType wordsetType, int badAns, int goodAns,
												  OnTracerCompletedListener callback)
		{
			this.wordsetId = wordsetId;
			this.learningMode = learningMode; 
			this.wordsetType = wordsetType; 
			this.badAns = badAns; 
			this.goodAns = goodAns; 
			
			this.handler =  new Handler(); // should be on main UI Thread initialized 
			this.callback = callback; 
		}

		@Override
		public void run() {
			
			// 1). getting user's profile identifier, if user is not logged in 
			//     we get default value 0, which means Anonymous user 
			profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
			
			// 2). inserting or updating learningHistoryTable row based on current informations
			Cursor cursor = selectLearningHistoryRowFor(profileId, wordsetId, learningMode, wordsetType); 
			
			if(cursor != null && cursor.getCount() == 1) { 
				updateLearningHistoryRow(cursor, badAns, goodAns);
			} else { 
				insertLearningHistoryRow(profileId, wordsetId, learningMode, wordsetType, badAns, goodAns);
			}
			
			// 3). persisting statistics in database 
			insertLearningStatsRow(profileId, badAns, goodAns); 
			
			// invoke callback function on Tracer's invoker thread (usually UI main thread)
			handler.post(new Runnable() {
				@Override
				public void run() {
					 if(callback != null) callback.onTracerCompleted(); 	
				}}); 
			
		}

		/**
		 * Helper method used to retrieve learning history item from SQLite database 
		 * associated with given parameters. It will be used to differentiate whether
		 * we need to insert new learning history row or update existing one. 
		 * @param profileId
		 * @param wordsetId
		 * @param learningMode
		 * @param wordsetType
		 * @return Cursor (object with selected learning history row) or null 
		 */
		private Cursor selectLearningHistoryRowFor(int profileId, int wordsetId, Mode learningMode, WordsetType wordsetType) {
			
			final Uri LEARNING_HISTORY_CONTENT_URI = Uri.parse(LearningHistoryProvider.CONTENT_URI 
													+ "/user/" + profileId 
													+ "/wordset/" + wordsetId
													+ "/mode/" + learningMode.id()
													+ "/wordset_type/" + wordsetType.id());
			
			String[] projection = { LearningHistoryProvider.LearningHistoryTable.COLUMN_LEARNING_HISTORY_ID,
									LearningHistoryProvider.LearningHistoryTable.COLUMN_BAD_ANSWERS, 
									LearningHistoryProvider.LearningHistoryTable.COLUMN_GOOD_ANSWERS, 
									LearningHistoryProvider.LearningHistoryTable.COLUMN_IMPROVEMENT, 
									LearningHistoryProvider.LearningHistoryTable.COLUMN_HITS};

			Cursor cursor = context.getContentResolver().query(LEARNING_HISTORY_CONTENT_URI, projection, null, null, null);
			
			return cursor;
		}
		
		
		/**
		 * Helper method used to insert new learning history row into SQLite database. 
		 * @param profileId
		 * @param wordsetId
		 * @param learningMode
		 * @param wordsetType
		 * @param badAns
		 * @param goodAns
		 * @return URI (path of newly inserted learning history item) or null 
		 */
		private Uri insertLearningHistoryRow(int profileId, int wordsetId,
						Mode learningMode, WordsetType wordsetType, int badAns, int goodAns) {
			
			ContentValues learningHistoryValues = new ContentValues(); 
			learningHistoryValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_PROFILE_ID, profileId);
			learningHistoryValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_WORDSET_ID, wordsetId); 
			learningHistoryValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_MODE_ID, learningMode.id()); 
			learningHistoryValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_WORDSET_TYPE_ID, wordsetType.id()); 
			learningHistoryValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_BAD_ANSWERS, badAns); 
			learningHistoryValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_GOOD_ANSWERS, goodAns);
			
			// calculating improvement based on bad and good answers ratio
			float ratio = 0; 
			if( (badAns + goodAns) > 0) { 
				ratio = (float) goodAns/(badAns + goodAns) *100;
			}
			float improvement = RoundFloat.round( ratio , 2);
			
			learningHistoryValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_IMPROVEMENT, improvement);
			learningHistoryValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_HITS, 1);
			String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()); 
			learningHistoryValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_LAST_ACCESS_DATE, currentDateTime); 
			learningHistoryValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_NOT_SYNCED, 1); 
			
			Uri insertedItemUri = 
					context.getContentResolver().insert(LearningHistoryProvider.CONTENT_URI, learningHistoryValues);
			
			Log.w(LearningHistoryAndStatisticsTracer.class.getName(), "Inserted new learning history row under: " 
					+ insertedItemUri.toString() + " with values: (" + profileId + "," + wordsetId + "," + learningMode.id() 
					+ "," + wordsetType.id() + "," + badAns + "," + goodAns + "," + improvement + "," + 1 + "," + currentDateTime + "," + 1 + ").");
			
			return insertedItemUri;
		}
		
		/**
		 * Helper method used to update existing learning history row in SQLite database. 
		 * @param cursor - contains existing learning history row which will be updated 
		 * @param badAns
		 * @param goodAns
		 * @return true/false depending on success or failure of updating the learning history row
		 */
		private boolean updateLearningHistoryRow(Cursor cursor, int badAns, int goodAns) {
			
			if(cursor != null && cursor.moveToFirst()) { 
				
				// get learning history information
				int learningHistoryID = cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_LEARNING_HISTORY_ID));
				int currBadAns = cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_BAD_ANSWERS));
				int currGoodAns = cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_GOOD_ANSWERS)); 
				float improvement = cursor.getFloat(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_IMPROVEMENT)); 
				int hits = cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryProvider.LearningHistoryTable.COLUMN_HITS)); 
				
				
				// calculate improvement between current learning history and new learning session
				float currRatio = 0 ; 
				if( (currBadAns + currGoodAns) > 0 ) { 
					currRatio = (float) currGoodAns/(currBadAns + currGoodAns) * 100; 
				}
				
				float newRatio = 0; 
				if( (badAns + goodAns) > 0) { 
					newRatio = (float) goodAns/(badAns + goodAns) *100;
				}
				
				float newImprovement = RoundFloat.round( (newRatio - currRatio), 2);
				
				// increase number of hits (learning sessions) by one (+1)
				hits++; 
				
				// update current learning history row using new values
				return updateLearningHistoryRow(learningHistoryID, badAns, goodAns, newImprovement, hits);
				
			} else { 
				return false; 
			}
		}
		
		/**
		 * Helper method used to update existing learning history row using learning history id and fix data 
		 * @param learningHistoryID
		 * @param badAns
		 * @param goodAns
		 * @param improvement
		 * @param hits
		 * @return true/false depending on success or failure of update operation
		 */
		private boolean updateLearningHistoryRow(int learningHistoryID, int badAns, int goodAns, float improvement, int hits) { 
			
			final Uri LEARNING_HISTORY_UPDATE_CONTENT_URI = Uri.parse(LearningHistoryProvider.CONTENT_URI + "/" + learningHistoryID); 
			
			ContentValues updatedValues = new ContentValues(); 
			updatedValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_BAD_ANSWERS, badAns);
			updatedValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_GOOD_ANSWERS, goodAns); 
			updatedValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_IMPROVEMENT, improvement); 
			updatedValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_HITS, hits); 
			String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()); 
			updatedValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_LAST_ACCESS_DATE, currentDateTime);
			updatedValues.put(LearningHistoryProvider.LearningHistoryTable.COLUMN_NOT_SYNCED, 1); 
			
			int updatedCount = 
					context.getContentResolver().update(LEARNING_HISTORY_UPDATE_CONTENT_URI, updatedValues, null, null);
			
			if(updatedCount == 1) { 
				Log.w(LearningHistoryAndStatisticsTracer.class.getName(), 
						"Learning History (" + learningHistoryID + ") updated successfuly to values: " 
								+ badAns + ", " + goodAns + ", " + improvement + ", " + hits + ", " + currentDateTime + ".");
				return true; 
			} else { 
				Log.w(LearningHistoryAndStatisticsTracer.class.getName(), 
						"An error has occured while updating learning history ("+ learningHistoryID + ").");
				return false; 
			} 
		}
		
		/**
		 * Helper method used to insert (persist) learning statistics in SQLite database.
		 * @param profileId
		 * @param badAns
		 * @param goodAns
		 * @return URI (path to inserted learning statistics row) or null
		 */
		private Uri insertLearningStatsRow(int profileId, int badAns, int goodAns) {
			
			ContentValues learningStatsValues = new ContentValues(); 
			learningStatsValues.put(LearningStatsProvider.LearningStatsTable.COLUMN_PROFILE_ID, profileId);
			learningStatsValues.put(LearningStatsProvider.LearningStatsTable.COLUMN_BAD_ANSWERS, badAns); 
			learningStatsValues.put(LearningStatsProvider.LearningStatsTable.COLUMN_GOOD_ANSWERS, goodAns); 
			String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()); 
			learningStatsValues.put(LearningStatsProvider.LearningStatsTable.COLUMN_ACCESS_DATE, currentDateTime);
			learningStatsValues.put(LearningStatsProvider.LearningStatsTable.COLUMN_NOT_SYNCED, 1); 
			
			Uri insertedItemUri = 
					context.getContentResolver().insert(LearningStatsProvider.CONTENT_URI, learningStatsValues);
			
			Log.w(LearningHistoryAndStatisticsTracer.class.getName(), "Inserted new learning stats row under: " 
					+ insertedItemUri.toString() + " with values: (" + profileId + "," + badAns + "," + goodAns + "," + currentDateTime + ").");
			
			return insertedItemUri; 
		}
	}
	

}
