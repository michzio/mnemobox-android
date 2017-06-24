package pl.elector.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import pl.elector.database.SentenceProvider;
import pl.elector.database.WordProvider;
import pl.elector.database.WordsetProvider;
import pl.elector.database.WordsetWordsProvider;
import pl.electoroffline.CustomHttpClient;
import pl.electoroffline.FileUtilities;
import pl.electoroffline.GetWordDetailsFromXML;
import pl.electoroffline.GetWordsListFromXML;
import pl.electoroffline.MainActivity;
import pl.electoroffline.NetworkUtilities;
import pl.electoroffline.Personalization;
import pl.electoroffline.Preferences;
import pl.electoroffline.R;
import pl.electoroffline.SettingsFragment;
import pl.electoroffline.WordsetActivity;
import pl.electoroffline.WordsetsListActivity;
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

public class WordsLoaderService extends IntentService {
	
	public static final String DOWNLOADED_WORDSET_ID = "DOWNLOADED_WORDSET_ID";
	public static final String WORDSET_TITLE = "WORDSET_TITLE"; 
	public static final String WORDS_SIZE = "WORDS_SIZE"; 
	public static final String WORDS_COUNT = "WORDS_COUNT"; 
	public static final String WORDS_LOADER_BROADCAST = "pl.elector.action.WORDS_LOADER_BROADCAST"; 
	public static final String SELECTED_WORDS_LOADER_BROADCAST = "pl.elector.action.SELECTED_WORDS_LOADER_BROADCAST"; 
	private static final int WORDS_LOADER_PENDING_INTENT_REQUEST_CODE = 0; 
	private static final int WORDSET_LOADER_SERVICE = 0; 
	
	public static final String WORDS_TO_SYNC = "WORDS_TO_SYNC"; 
	
	private String url = "http://www.mnemobox.com/webservices/getwordset.php?type=systemwordset&from=pl&to=en&wordset=";
	
	private int wordsetId; 
	private String wordsetTitle; 
	private Handler handler;
	private NotificationCompat.Builder notificationBuilder; 
	private NotificationManager notificationManager;
	
	/**
	 * constructor that passes name parameter to the super class
	 */
	public WordsLoaderService() {
		super("WordsLoaderService"); 
	}
	
	public WordsLoaderService(String name) {
		super(name); 
	}
	
	/**
	 * Actions performed when the service is created
	 */
	@Override 
	public void onCreate() {
		super.onCreate(); 
		
		handler = new Handler();
		
		// getting NotificationManager and building Notification using NotificationBuilder 
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationBuilder = new NotificationCompat.Builder(this); 
		notificationBuilder.setSmallIcon(R.drawable.favicon)
						   .setTicker("Downloading Words...")
						   .setWhen(System.currentTimeMillis())
						   .setContentTitle("Progress")
						   .setProgress(100,0,false)
						   .setOngoing(true);
						 //.setContentIntent(pendingIntent)
		
		// get web service URL address from string resources
		// url = this.getResources().getString(R.string.);
	}
	
	/**
	 * This handler occurs on the background thread.
	 * Here should be performed time consuming tasks.
	 * Each Intent supplied to this IntentService 
	 * will be processed consecutively. 
	 * When all incoming Intents have been processed the
	 * the Service will terminate itself. 
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		
		// getting wordsetId from intent extras
		wordsetId = intent.getIntExtra(DOWNLOADED_WORDSET_ID, 0);
		wordsetTitle = intent.getStringExtra(WORDSET_TITLE);
		
		if(wordsetId == 0) { 
			Log.w(WordsLoaderService.class.getName(), "wordsetId number: 0 -> only selected words loader!");
			synchronizeSelectedWords(intent); 
			return; 
		}
		
		
		// creating Pending Intent to start Wordset Activity when notification is clicked
		Intent notificationIntent = new Intent(WordsLoaderService.this, WordsetActivity.class);
		notificationIntent.putExtra(WordsetsListActivity.SELECTED_WORDSET, wordsetId);
		PendingIntent contentIntent = PendingIntent.getActivity(WordsLoaderService.this,
																WORDS_LOADER_PENDING_INTENT_REQUEST_CODE, 
																notificationIntent,
																PendingIntent.FLAG_UPDATE_CURRENT);
		
		// setting up notification
		notificationBuilder.setTicker(getString(R.string.downloading_notification_title, wordsetTitle))
						   .setContentTitle(wordsetTitle)
						   .setContentText(getString(R.string.downloading_in_progress))
						   .setContentIntent(contentIntent);
		notificationManager.notify(WORDSET_LOADER_SERVICE+wordsetId, notificationBuilder.build());
		
		
		// calling method which perform downloading wordset words in background
		loadWordsetWordsFromWebService(); 
		
	}
	
	private ArrayList<String> selectedWordIds; 
	
	/**
	 * Method used to setting up synchronization of only selected words. 
	 * @param intent
	 */
	private void synchronizeSelectedWords(Intent intent)
	{
		// creating Pending Intent to start Main Activity when notification is clicked. 
		Intent notificationIntent = new Intent(WordsLoaderService.this, MainActivity.class); 
		// notificationIntent.putExtra(name, value)
		PendingIntent contentIntent = PendingIntent.getActivity(WordsLoaderService.this, 
																WORDS_LOADER_PENDING_INTENT_REQUEST_CODE, 
																notificationIntent, 
																PendingIntent.FLAG_UPDATE_CURRENT);
		
		// getting array list of word ids of selected words to download
		selectedWordIds = intent.getStringArrayListExtra(WordsLoaderService.WORDS_TO_SYNC);
		
		// setting up notification
		notificationBuilder.setTicker("Downloading " + selectedWordIds.size() + " words...")
						   .setContentTitle("Downloading " + selectedWordIds.size() + " words...")
						   .setContentText("in progress...")
						   .setContentIntent(contentIntent);
		notificationManager.notify(WORDSET_LOADER_SERVICE+wordsetId, notificationBuilder.build());
		
		// calling method which perform downloading selected words in background
		loadSelectedWordsFromWebService(); 
	}
	
	/**
	 * method loading words for selected word ids 
	 */
	private void loadSelectedWordsFromWebService() 
	{
		Log.w(WordsLoaderService.class.getName(), "Loading selected words...");
		
		String nativeCode = Preferences.getAccountPreferences(this)
				.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, getString(R.string.native_code_lower));
		String foreignCode = Preferences.getAccountPreferences(this)
				.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, getString(R.string.foreign_code_lower)); 
		
		// constructing url and appedning wordIds
		url = getString(R.string.getselectedwords_url, nativeCode, foreignCode, getCommaSeparatedWordIds(selectedWordIds)); 
		Log.w(WordsLoaderService.class.getName(), "Selected words URL: " + url); 
		
		GetWordsListFromXML wordsListObject  = null; 
		try { 
			InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url); 
			wordsListObject = new GetWordsListFromXML(is); 
			try { 
	            is.close();
	        } catch(java.io.IOException e) { } 
		}  catch (Exception e) { }
		
		Log.d(WordsLoaderService.class.getName(), "Words List object: " + wordsListObject.toString());
		
		new WordsDownloader(this, wordsListObject).download(true, notificationBuilder);
		
		// When the loop is finished, updates the notification
        notificationBuilder.setContentText(getString(R.string.download_complete))
        				   // Removes the progress bar
                	       .setProgress(0,0,false)
                	       .setOngoing(false);
        
        notificationManager.notify(WORDSET_LOADER_SERVICE+wordsetId, notificationBuilder.build());
	}
	
	/**
	 * Helper method that returns comma separated wordIds based on array list -> can be REFACTORIZED
	 * @param wordIds
	 * @return
	 */
	private String getCommaSeparatedWordIds(ArrayList<String> wordIds)
	{
		if(wordIds == null) 
			return ""; 
		
		String result = wordIds.toString().replaceAll(" ", "").replace("[","").replace("]", ""); 
		return result; 
	}

	
	/**
	 * method loading words for wordset with current wordset id 
	 */
	private void loadWordsetWordsFromWebService()
	{
		Log.w(WordsLoaderService.class.getName(), "Loading words for wordset with id: " + wordsetId);
		
		// constructing URL 
		String nativeCode = Preferences.getAccountPreferences(this)
								.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, getString(R.string.native_code_lower));
		String foreignCode = Preferences.getAccountPreferences(this)
								.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, getString(R.string.foreign_code_lower));
		url = getString(R.string.getsystemwordset_url, nativeCode, foreignCode, wordsetId); 
		
		Log.d(WordsLoaderService.class.getName(), "Words loading from url: " + url); 
		
		GetWordsListFromXML wordsListObject  = null; 
		try { 
			InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url); 
			wordsListObject = new GetWordsListFromXML(is); 
			try { 
	            is.close();
	        } catch(java.io.IOException e) { } 
		}  catch (Exception e) { }
		
		/**
		 * DEPRECATED - replaced by WordsDownloader object and method WordsDownloader.download()!
		 *
		// getting maps (key=>value) of words details as wordId => value
		HashMap<Integer, String> foreignArticles = wordsListObject.getENArticles();
		HashMap<Integer, String> foreignWords = wordsListObject.getENWords(); 
		HashMap<Integer, String> nativeArticles = wordsListObject.getPLArticles(); 
		HashMap<Integer, String> nativeWords = wordsListObject.getPLWords(); 
		HashMap<Integer, String> images = wordsListObject.getImages();
		HashMap<Integer, String> transcriptions = wordsListObject.getTranscriptions(); 
		HashMap<Integer, String> audios = wordsListObject.getAudios(); 
		// HashMap containing HashMaps of sentences for given words. Each sentence consist of ArrayList 
		// of foreign sentence (idx 0), native sentence (idx 1), sentence recording (idx 2) 
		LinkedHashMap<Integer, LinkedHashMap<Integer, ArrayList<String>>> sentences = wordsListObject.getSentences();
		
		if(foreignWords.isEmpty()) { 
			handler.post(new Runnable() {
			    public void run() {
			    	Toast.makeText(WordsLoaderService.this, 
			    					"An error encounter while downloading words... ", 3000);
			    }
			}); 
			return;
		}
		
		// deleting old entries in Word table. It also deletes bindings in Wordset_Words table
		// and Sentences related to Word entries from Sentence table using DELETE_TRIGGERs
		Uri DELETE_WORDSET_WORDS_URI = Uri.parse(WordProvider.CONTENT_URI + "/wordset/" + wordsetId);
		int deleteCount = getContentResolver().delete(DELETE_WORDSET_WORDS_URI, null, null); 
		Log.w(WordsLoaderService.class.getName(), "Number of deleted items: " + deleteCount); 
		/// TO DO: 
		// NEED TO BE CHANGED ON INSERT/UPDATE instead of DELETE to maintain consistancy with rememberMe, learned words
		// TO DO!
		
		// number of words used to calculate download progress displayed on notification
		int wordsSize = foreignWords.entrySet().size(); 
		int wordsCounter = 0;
		// sending broadcast intent to parent Activity 
		sendProgressBroadcast(context, wordsCounter, wordsSize);
		
		// iterate over words in set of words 
		for( int wordId : foreignWords.keySet()) { 
			
			Log.w(WordsLoaderService.class.getName(), "Inserting word: " + wordId + ", "
				  + foreignArticles.get(wordId) + ", " + foreignWords.get(wordId) + ", "
				  + nativeArticles.get(wordId)  +  ", " + nativeWords.get(wordId) + ", "
				  + images.get(wordId) + ", " + transcriptions.get(wordId) + ", "
				  + audios.get(wordId) + ".");
			
			// wrapping Word details into ContentValues object 
			ContentValues wordValues = new ContentValues(); 
			wordValues.put(WordProvider.WordTable.COLUMN_WORD_ID, wordId);
			wordValues.put(WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE, foreignArticles.get(wordId));
			wordValues.put(WordProvider.WordTable.COLUMN_FOREIGN_WORD, foreignWords.get(wordId));
			wordValues.put(WordProvider.WordTable.COLUMN_NATIVE_ARTICLE, nativeArticles.get(wordId));
			wordValues.put(WordProvider.WordTable.COLUMN_NATIVE_WORD, nativeWords.get(wordId));
			wordValues.put(WordProvider.WordTable.COLUMN_TRANSCRIPTION, transcriptions.get(wordId));
			wordValues.put(WordProvider.WordTable.COLUMN_IMAGE, getImageBlob(this, images.get(wordId)));
			wordValues.put(WordProvider.WordTable.COLUMN_RECORDING, audios.get(wordId)); 
			
			getContentResolver().insert(WordProvider.CONTENT_URI, wordValues); 
			
			// for each inserted Word entity into Word table 
			// there must be also inserted binding between word and wordset 
			// to which it belongs into Wordset_Words table 
			ContentValues wordsetWordBindingValues = new ContentValues(); 
			wordsetWordBindingValues.put(WordsetWordsProvider.WordsetWordsTable.COLUMN_WORDSET_ID, wordsetId); 
			wordsetWordBindingValues.put(WordsetWordsProvider.WordsetWordsTable.COLUMN_WORD_ID, wordId);
			
			getContentResolver().insert(WordsetWordsProvider.CONTENT_URI, wordsetWordBindingValues);
			Log.w(WordsLoaderService.class.getName(), "Inserting wordset word binding: (" + wordsetId + "," + wordId + ")");
			
			// Inserting to Sentence table sentences related with current word
			// Sentences number is indeterminate so we must loop through its HashMap all elements
			for(int sentenceId : sentences.get(wordId).keySet()) {
				
				String sentenceForeign = sentences.get(wordId).get(sentenceId).get(0); 
				String sentenceNative = sentences.get(wordId).get(sentenceId).get(1);
				String sentenceRecording = sentences.get(wordId).get(sentenceId).get(2);
				
				Log.w(WordsLoaderService.class.getName(), "Inserting sentence: " + sentenceId + ", "
						+ sentenceForeign + ", "+ sentenceNative + ", " + sentenceRecording + "."); 
				
				// wrapping Sentence info into ContentValues object 
				ContentValues sentenceValues = new ContentValues(); 
				sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_SENTENCE_ID, sentenceId);
				sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_WORD_ID, wordId); 
				sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_FOREIGN_SENTENCE, sentenceForeign); 
				sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_NATIVE_SENTENCE, sentenceNative); 
				
				getContentResolver().insert(SentenceProvider.CONTENT_URI, sentenceValues); 
				
				// HERE CAN BE ADDED ADDITIONAL SENTENCE RECORDING DOWNLOAD PROCEDURE !
			}
			
			// Checking whether user wants to download audio recordings with words
			if(Preferences.getBoolean(this, Preferences.KEY_PREFER_TO_DOWNLOAD_AUDIO, true)) {
				   
				// 1) downloading words details, images and audio files
				Log.w(WordsLoaderService.class.getName(), "Downloading audio recording: " + audios.get(wordId));
				
				// getting text part of recording file with audio format extension
				String fileName = audios.get(wordId).substring(audios.get(wordId).lastIndexOf("/") + 1); 
			    // files will be saved with additional wordsetId and wordId to make harder stealing recordings
				downloadRecording(this, audios.get(wordId), wordId + fileName);
			}
			
			// updating download progress on ongoing notification
			wordsCounter++; 
			
			notificationBuilder.setProgress(wordsSize, wordsCounter, false)
							    .setContentText("Downloading in progress " + wordsCounter + "/" + wordsSize + ".");
			notificationManager.notify(WORDSET_LOADER_SERVICE+wordsetId, notificationBuilder.build());
			
			// sending broadcast intent to parent Activity
	        sendProgressBroadcast(context, wordsCounter, wordsSize);
		}
		**/
		
		new WordsDownloader(this, wordsListObject, wordsetId).download(true, notificationBuilder);
		
		ContentValues updateWordsetValues = new ContentValues();
		final Uri UPDATE_WORDSET_URI = Uri.parse(WordsetProvider.CONTENT_URI + "/" + wordsetId); 
		// If user wants to store audio files locally we update row in Wordset table 
		if(Preferences.getBoolean(this, Preferences.KEY_PREFER_TO_DOWNLOAD_AUDIO, true)) {
			updateWordsetValues.put(WordsetProvider.WordsetTable.COLUMN_IS_AUDIO_STORED_LOCALLY, 1);
		} else { 
			updateWordsetValues.put(WordsetProvider.WordsetTable.COLUMN_IS_AUDIO_STORED_LOCALLY, 0);
		}
		
		// updating Wordset table COLUMN_IS_AUDIO_STORED_LOCALLY
		getContentResolver().update(UPDATE_WORDSET_URI, updateWordsetValues, null, null);
		
		// When the loop is finished, updates the notification
        notificationBuilder.setContentText(getString(R.string.download_complete))
        				   // Removes the progress bar
                	       .setProgress(0,0,false)
                	       .setOngoing(false);
        
        notificationManager.notify(WORDSET_LOADER_SERVICE+wordsetId, notificationBuilder.build());
        
        
	
	}
	
	public static byte[] getImageBlob(Context context, String imageName) {
		
		if(imageName.equals("")) {
			Log.w(WordsLoaderService.class.getName(), "Skipping image, url empty.");
			return null; 
		}
		
		// concatenating URL path to image file on web server
		String imageURL = context.getResources().getString(R.string.images_url) + imageName;
		
		Log.w(WordsLoaderService.class.getName(), "Downloading image: " + imageURL); 
		
		// downloading image as byte array
		byte[] imageBuffer = NetworkUtilities.downloadFromURL(imageURL); 
	     
		return imageBuffer; 
	}
	
	public static void downloadRecording(Context ctx, String recordingName, String localRecordingName)
	{
		if(recordingName.equals("")) {
			Log.w(WordsLoaderService.class.getName(), "Skipping recording download, empty url.");
		} 
		
		// concatenating URL path to recording file on web server
		String recordingURL = ctx.getResources().getString(R.string.recordings_url) + recordingName; 
		
		Log.w(WordsLoaderService.class.getName(), "Downloading recording: " + recordingURL); 
		
		// downloading recording as byte array 
		byte[] recordingBuffer = NetworkUtilities.downloadFromURL(recordingURL);
		
		// checking whether external storage is mounted
		if(FileUtilities.isExternalStorageWritable()) {
			File directory = new File(FileUtilities.getExternalFilesDir(ctx), "recordings");
		    if (!directory.isDirectory()) {
		        Log.e(WordsLoaderService.class.getName(), "Directory has not been created yet.");
		        if(!directory.mkdirs()) {
		        	 Log.e(WordsLoaderService.class.getName(), "Error while creating 'recording' directory.");
		        	 return;
		        }
		    }
		    
		    // creating recording file on external storage
		    File file = new File(directory, localRecordingName);
		    try { 
		    	file.createNewFile();
		    	FileOutputStream fos = new FileOutputStream(file);
		    	fos.write(recordingBuffer);
		    	fos.flush();
		    	fos.close();
		    } catch(FileNotFoundException ex) {
		    	Log.w(WordsLoaderService.class.getName(), "FileNotFound Exception while saving recording to external storage."); 
		    } catch(IOException ex) { 
		    	Log.w(WordsLoaderService.class.getName(), "IO Exception while saving recording to external storage."); 
		    } finally {
		    	// do nothing?
		    }

		}
	}
	
	/**
	 * Method sends Broadcast Intent to WordsetActivity Receiver with info about download progress. 
	 * @param wordsCounter - currently downloaded number of words
	 * @param wordsSize - number of words to download 
	 */
	private static void sendProgressBroadcast(Context context, int wordsetId, int wordsCounter, int wordsSize) 
	{
		Intent broadcastIntent;
		
		if(wordsetId > 0) {  
			broadcastIntent = new Intent(WORDS_LOADER_BROADCAST);
		} else { 
			broadcastIntent = new Intent(SELECTED_WORDS_LOADER_BROADCAST); 
		}
		
		broadcastIntent.putExtra(DOWNLOADED_WORDSET_ID, wordsetId); 
		broadcastIntent.putExtra(WORDS_COUNT, wordsCounter); 
		broadcastIntent.putExtra(WORDS_SIZE, wordsSize); 
		context.sendBroadcast(broadcastIntent); 
	}

	public static class WordsDownloader 
	{
		GetWordsListFromXML wordsListReader; 
		Context context;
		int wordsetId;
		
		public WordsDownloader(Context ctx, GetWordsListFromXML reader) {
			this(ctx, reader, 0);   
		}
		
		public WordsDownloader(Context ctx, GetWordsListFromXML reader, int wordsetId) {
			wordsListReader = reader; 
			context = ctx;
			this.wordsetId = wordsetId; 
		}
		
		public void downlaod() { 
			download(false, null); 
		}
		
		public void download(boolean sendProgress, NotificationCompat.Builder notificationBuilder) {
			
			// getting maps (key=>value) of words details as wordId => value
			HashMap<Integer, String> foreignArticles = wordsListReader.getENArticles();
			HashMap<Integer, String> foreignWords = wordsListReader.getENWords(); 
			HashMap<Integer, String> nativeArticles = wordsListReader.getPLArticles(); 
			HashMap<Integer, String> nativeWords = wordsListReader.getPLWords(); 
			HashMap<Integer, String> images = wordsListReader.getImages();
			HashMap<Integer, String> transcriptions = wordsListReader.getTranscriptions(); 
			HashMap<Integer, String> audios = wordsListReader.getAudios();
			// HashMap containing HashMaps of sentences for given words. Each sentence consist of ArrayList 
			// of foreign sentence (idx 0), native sentence (idx 1), sentence recording (idx 2) 
			LinkedHashMap<Integer, LinkedHashMap<Integer, ArrayList<String>>> sentences = wordsListReader.getSentences();
			
			// number of words used to calculate download progress displayed on notification
			int wordsSize = foreignWords.entrySet().size(); 
			int wordsCounter = 0;
			if(sendProgress)
				// sending broadcast intent to parent Activity 
				sendProgressBroadcast(context, wordsetId, wordsCounter, wordsSize);
			
			// iterate over words in set of words 
			for( int wordId : foreignWords.keySet()) { 
				
				Log.w(WordsLoaderService.class.getName(), "Inserting/Updating word: " + wordId + ", "
					  + foreignArticles.get(wordId) + ", " + foreignWords.get(wordId) + ", "
					  + nativeArticles.get(wordId)  +  ", " + nativeWords.get(wordId) + ", "
					  + images.get(wordId) + ", " + transcriptions.get(wordId) + ", "
					  + audios.get(wordId) + ".");
				
				// wrapping Word details into ContentValues object 
				ContentValues wordValues = new ContentValues(); 
				wordValues.put(WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE, foreignArticles.get(wordId));
				wordValues.put(WordProvider.WordTable.COLUMN_FOREIGN_WORD, foreignWords.get(wordId));
				wordValues.put(WordProvider.WordTable.COLUMN_NATIVE_ARTICLE, nativeArticles.get(wordId));
				wordValues.put(WordProvider.WordTable.COLUMN_NATIVE_WORD, nativeWords.get(wordId));
				wordValues.put(WordProvider.WordTable.COLUMN_TRANSCRIPTION, transcriptions.get(wordId));
				wordValues.put(WordProvider.WordTable.COLUMN_IMAGE, getImageBlob(context, images.get(wordId)));
				wordValues.put(WordProvider.WordTable.COLUMN_RECORDING, audios.get(wordId));
				
				if(checkWordExists(context, wordId)) { 
					// UPDATE current row in Word table 
					Uri WORD_CONTENT_URI = Uri.parse(WordProvider.CONTENT_URI + "/" + wordId);
					context.getContentResolver().update(WORD_CONTENT_URI, wordValues, null, null);
					
				} else { 
					// INESRT brand new row into Word table 
					wordValues.put(WordProvider.WordTable.COLUMN_WORD_ID, wordId);
					context.getContentResolver().insert(WordProvider.CONTENT_URI, wordValues);
				}
				
				if(wordsetId > 0) 
					insertWordsetWordBinding(wordId); 
				
				// inserting or updating sentences related with current word
				addSentences(wordId, sentences.get(wordId)); 
				
				// Checking whether user wants to download audio recordings with words
				if(Preferences.getBoolean(context, Preferences.KEY_PREFER_TO_DOWNLOAD_AUDIO, true)) {
					   
					// 1) downloading words details, images and audio files
					Log.w(WordsDownloader.class.getName(), "Downloading audio recording: " + audios.get(wordId));
					
					// getting text part of recording file with audio format extension
					String fileName = audios.get(wordId).substring(audios.get(wordId).lastIndexOf("/") + 1); 
				    // files will be saved with additional wordId to make harder stealing recordings
					downloadRecording(context, audios.get(wordId), wordId + fileName);
				}
				
				// updating download progress on ongoing notification
				wordsCounter++; 
				
				if(notificationBuilder != null) 
				{
					notificationBuilder.setProgress(wordsSize, wordsCounter, false)
				    				   .setContentText(context.getString(R.string.downloading_in_proress_count, wordsCounter, wordsSize));
					
					NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
					notificationManager.notify(WORDSET_LOADER_SERVICE+wordsetId, notificationBuilder.build());
				}
				
				// sending broadcast intent to parent Activity
				if(sendProgress)
					sendProgressBroadcast(context, wordsetId, wordsCounter, wordsSize);
			}
		}
		
		/**
		 * Method inserts wordset_word binding if such doesn't exists.
		 * @param wordId
		 */
		private void insertWordsetWordBinding(int wordId) 
		{
			// for each inserted Word entity into Word table 
			// there must be also inserted binding between word and wordset 
			// to which it belongs into Wordset_Words table 
			
			Uri SELECT_WORDSET_WORD = Uri.parse(WordsetWordsProvider.CONTENT_URI + "/" + wordsetId + "/" + wordId);
			String[] projection = null; 
			
			Cursor cursor = context.getContentResolver().query(SELECT_WORDSET_WORD, projection, null, null, null); 
			
			if( cursor.moveToNext() ) {
				cursor.close();
				return; 
			}
			
			// else we need to insert wordset word binding into wordsetWordsTable
			ContentValues wordsetWordBindingValues = new ContentValues(); 
			wordsetWordBindingValues.put(WordsetWordsProvider.WordsetWordsTable.COLUMN_WORDSET_ID, wordsetId); 
			wordsetWordBindingValues.put(WordsetWordsProvider.WordsetWordsTable.COLUMN_WORD_ID, wordId);
						
			Uri insertedItemUri = context.getContentResolver().insert(WordsetWordsProvider.CONTENT_URI, wordsetWordBindingValues);
			Log.w(WordsLoaderService.WordsDownloader.class.getName(), 
					"Inserting wordset word binding: (" + wordsetId + "," + wordId + ") under: " + insertedItemUri);
			
			
		}
		
		/**
		 * Helper method that insert/update 
		 * sentences for current word with wordId
		 * @param wordId
		 * @param sentences
		 */
		private void addSentences(int wordId, 
				LinkedHashMap<Integer, ArrayList<String>> sentences) {
		
			
			// Inserting to Sentence table sentences related with current word
			// Sentences number is indeterminate so we must loop through its HashMap all elements
			for(int sentenceId : sentences.keySet()) {
				
				String sentenceForeign = sentences.get(sentenceId).get(0); 
				String sentenceNative = sentences.get(sentenceId).get(1);
				String sentenceRecording = sentences.get(sentenceId).get(2);
				
				Log.w(WordsLoaderService.class.getName(), "Inserting/Updating sentence: " + sentenceId + ", "
						+ sentenceForeign + ", "+ sentenceNative + ", " + sentenceRecording + ".");
				
				// wrapping Sentence info into ContentValues object 
				ContentValues sentenceValues = new ContentValues(); 
				sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_WORD_ID, wordId); 
				sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_FOREIGN_SENTENCE, sentenceForeign); 
				sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_NATIVE_SENTENCE, sentenceNative); 
				
				if(checkSentenceExists(context, sentenceId)) { 
					Uri SENTENCE_CONTENT_URI = Uri.parse(SentenceProvider.CONTENT_URI + "/" + sentenceId); 
					context.getContentResolver().update(SENTENCE_CONTENT_URI, sentenceValues, null, null);
				} else { 
					sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_SENTENCE_ID, sentenceId);
					context.getContentResolver().insert(SentenceProvider.CONTENT_URI, sentenceValues);
				}
				
				// HERE CAN BE ADDED ADDITIONAL SENTENCE RECORDING DOWNLOAD PROCEDURE !
			}
			
		}

		/**
		 * Helper method that based on sentenceId 
		 * checks whether given sentence exists in DB.
		 * @param sentenceId
		 * @return
		 */
		public static boolean checkSentenceExists(Context context, int sentenceId) {
			
			Uri SENTENCE_CONTENT_URI = Uri.parse(SentenceProvider.CONTENT_URI + "/" + sentenceId); 
			String[] projection = { SentenceProvider.SentenceTable.COLUMN_SENTENCE_ID };
			Cursor cursor = context.getContentResolver().query(SENTENCE_CONTENT_URI, projection, null, null, null);
			
			if(cursor.moveToFirst()) {
				cursor.close();
				return true; 
			}
			cursor.close();
			return false;
		}
	
		/**
		 * Helper method that checks whether word for current wordId 
		 * exists in wordTable
		 * @return
		 */
		public static boolean checkWordExists(Context context, int wordId) {
			Uri WORD_CONTENT_URI = Uri.parse(WordProvider.CONTENT_URI + "/" + wordId); 
			String[] projection = { WordProvider.WordTable.COLUMN_WORD_ID };
			Cursor cursor = context.getContentResolver().query(WORD_CONTENT_URI, projection, null, null, null);
			
			if(cursor.moveToFirst()) {
				cursor.close();
				return true; 
			}
			cursor.close();
			return false;
		}
		
		/**
		 * Helper method that download remember me/learned word details 
		 * (as each such word must be stored locally with audio and image).
		 */
		public static boolean downloadWordDetails(Context context, int wordId) {
			
			String nativeCode = Preferences.getAccountPreferences(context)
					.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower));
			String foreignCode = Preferences.getAccountPreferences(context)
					.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower)); 
			
			// Get URL address of XML web service 
			String url = context.getString(R.string.gettranslation_url, nativeCode, foreignCode, wordId);
			Log.d(WordsLoaderService.class.getName(), "Get translation url: " + url); 
			
			try {
				// retrieve XML as InputStream and parse it with GetWordDetailsFromXML object
				InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
				GetWordDetailsFromXML wordDetailsObject = new GetWordDetailsFromXML(is); 
				try { 
					is.close();
				} catch(IOException e) { }
			
				String imageName = ""; 
			    if(wordDetailsObject.images.size() > 0)
			    	imageName = wordDetailsObject.images.values().toArray(new String[] {})[0];
			    
				// inserting new row into Word table 
				Log.w(Personalization.class.getName(), "Inserting word: " + wordId + ", "
					  + wordDetailsObject.enArticle + ", " + wordDetailsObject.enWord + ", "
					  + wordDetailsObject.plArticle  +  ", " + wordDetailsObject.plWord + ", "
					  + imageName + ", " + wordDetailsObject.transcription + ", "
					  + wordDetailsObject.recording + ".");
			
				// wrapping Word details into ContentValues object 
				ContentValues wordValues = new ContentValues(); 
				wordValues.put(WordProvider.WordTable.COLUMN_WORD_ID, wordId); 
				wordValues.put(WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE, wordDetailsObject.enArticle);
				wordValues.put(WordProvider.WordTable.COLUMN_FOREIGN_WORD, wordDetailsObject.enWord);
				wordValues.put(WordProvider.WordTable.COLUMN_NATIVE_ARTICLE, wordDetailsObject.plArticle);
				wordValues.put(WordProvider.WordTable.COLUMN_NATIVE_WORD, wordDetailsObject.plWord);
				wordValues.put(WordProvider.WordTable.COLUMN_TRANSCRIPTION, wordDetailsObject.transcription);
				wordValues.put(WordProvider.WordTable.COLUMN_IMAGE, getImageBlob(context, imageName));
				wordValues.put(WordProvider.WordTable.COLUMN_RECORDING, wordDetailsObject.recording); 
			
				Uri insertedItemUri = context.getContentResolver().insert(WordProvider.CONTENT_URI, wordValues); 
				
				Log.w(Personalization.class.getName(), "Inserted word uri: " + insertedItemUri); 
				
				if(insertedItemUri == null) return false;
				
				// Inserting to Sentence table sentences related with current word
				// Sentences number is indeterminate so we must loop through its HashMap all elements
				for(int sentenceId : wordDetailsObject.enSentences.keySet()) 
				{
					String sentenceForeign = wordDetailsObject.enSentences.get(sentenceId); 
					String sentenceNative = wordDetailsObject.plSentences.get(sentenceId);
					String sentenceRecording = wordDetailsObject.sentRecordings.get(sentenceId);
					
					Log.w(Personalization.class.getName(), "Inserting sentence: " + sentenceId + ", "
							+ sentenceForeign + ", "+ sentenceNative + ", " + sentenceRecording + "."); 
					
					// wrapping Sentence info into ContentValues object 
					ContentValues sentenceValues = new ContentValues(); 
					sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_SENTENCE_ID, sentenceId);
					sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_WORD_ID, wordId); 
					sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_FOREIGN_SENTENCE, sentenceForeign); 
					sentenceValues.put(SentenceProvider.SentenceTable.COLUMN_NATIVE_SENTENCE, sentenceNative); 
					
					Uri insertedSentenceUri = context.getContentResolver().insert(SentenceProvider.CONTENT_URI, sentenceValues); 
					
					Log.w(Personalization.class.getName(), "Inserted sentence uri: " + insertedSentenceUri); 
					// HERE CAN BE ADDED ADDITIONAL SENTENCE RECORDING DOWNLOAD PROCEDURE !
				}
				
				// Checking whether user wants to download audio recordings with words
				if(Preferences.getBoolean(context, Preferences.KEY_PREFER_TO_DOWNLOAD_AUDIO, true)) {
					   
					// 1) downloading words details, images and audio files
					Log.w(Personalization.class.getName(), "Downloading audio recording: " + wordDetailsObject.recording);
					
					// getting text part of recording file with audio format extenstion
					String fileName = wordDetailsObject.recording.substring(wordDetailsObject.recording.lastIndexOf("/") + 1); 
				    // files will be saved with additional wordsetId and wordId to make harder stealing recordings
					WordsLoaderService.downloadRecording(context, wordDetailsObject.recording, wordId + fileName);
				}
			
				return true; 
				
			} catch (Exception e) {
				e.printStackTrace();
				return false; 
			}
			
		}
		
	}
}
