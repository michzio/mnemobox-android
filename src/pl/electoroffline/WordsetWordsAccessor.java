/**
 * 
 */
package pl.electoroffline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import pl.elector.database.ForgottenProvider;
import pl.elector.database.LearnedWordsProvider;
import pl.elector.database.RememberMeProvider;
import pl.elector.database.UserWordsetWordsProvider;
import pl.elector.database.WordProvider;
import pl.elector.database.WordsetProvider;
import pl.elector.database.WordsetType;
import pl.elector.database.WordsetWordsProvider;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Michał Ziobro
 *
 */
public class WordsetWordsAccessor implements Personalization.Callbacks {
	
	public static final String CHECK_RECORDING_AVAILABILITY = "check_audio_availability://";
	public static final String KEY_START_SELECTED_WORDS_SYNCHRONIZATION = "KEY_START_SELECTED_WORDS_SYNCHRONIZATION"; 
	public static final String KEY_WORDS_TO_SYNC = "KEY_WORDS_TO_SYNC"; 
	public static enum ACCESS_TYPE { ONLINE, OFFLINE, DEFAULT_PROMPT, DEFAULT_NO_PROMPT }
	
	// CONSTANTS
	// URL to web service updated from string resource in constructor 
	private String url;
	private ACCESS_TYPE accessType; 
	

	// variables describing what wordset words client wants
	private final int wordsetId; 
	private final boolean wordsetPersonalize;
	private WordsetType type; 
	private ArrayList<String> selectedWordIds; 
	
	// containers storing loaded wordset words details
	private LinkedHashMap<Integer, String> foreignArticles; 
	private LinkedHashMap<Integer, String> foreignWords;
	private LinkedHashMap<Integer, String> nativeArticles; 
    private LinkedHashMap<Integer, String> nativeWords;
    private LinkedHashMap<Integer, String> transcriptions;
    private LinkedHashMap<Integer, String> recordingPaths;
    private LinkedHashMap<Integer, String> recordingNames;
    private LinkedHashMap<Integer, String> imagePaths;
    private LinkedHashMap<Integer, byte[]> imageData; 
    private ArrayList<Integer> wordIds; 
    // variable that is used to determine path to recordings:
    // - URL to online file server 
    // - URI to local external storage
    private boolean isAudioOffline; 
    
    private Object lock = new Object();
    private boolean areWordsLoaded = false; 
    private boolean areWordsPersonalized = false; 
    
    // helper variables 
    private final Context context;
    private WordsetWordsAccessor.Callbacks listener;
    
    public interface Callbacks {
    	
    	public void onWordsLoadFinished(WordsetWordsAccessor accessor); 
    }
	
	/**
	 * Constructor that creates object that enables access 
	 * to wordset words details lists for passed in wordsetId 
	 * @param wordsetId - id of wordset for which to load words details
	 */
	public WordsetWordsAccessor(Context ctx, int wordsetId) {
		//calling overloaded constructor, switching on wordset personalization
		this(ctx, wordsetId, true); 
	}
	
	/**
	 * Constructor that creates object that enables access
	 * to wordset words details lists for passed in wordsetId 
	 * and enables also to specify whether wordset should be 
	 * personalised based on forgotten words and learned words informations.
	 * @param wordsetId
	 * @param wordsetPersonalise
	 */
	public WordsetWordsAccessor(Context ctx, int wordsetId, boolean wordsetPersonalize)
	{
		this(ctx, wordsetId, WordsetType.SYSTEM_WORDSET, wordsetPersonalize); 
	}
	
	public WordsetWordsAccessor(Context ctx, int wordsetId, boolean wordsetPersonalize, ACCESS_TYPE accessType) 
	{ 
		this(ctx, wordsetId, WordsetType.SYSTEM_WORDSET, wordsetPersonalize, accessType);
	}
	
	public WordsetWordsAccessor(Context ctx, int wordsetId, WordsetType type) {
		this(ctx, wordsetId, type, true); 
	}
	
	public WordsetWordsAccessor(Context ctx, int wordsetId, WordsetType type, ACCESS_TYPE accessType)
	{
		this(ctx, wordsetId, type, true, accessType);
	}
	
	
	public WordsetWordsAccessor(Context ctx, int wordsetId, WordsetType type, boolean wordsetPersonalize)
	{
		this(ctx, wordsetId, type, wordsetPersonalize, ACCESS_TYPE.DEFAULT_PROMPT);
		
	}
	
	/**
	 * Additional constructor that enables to specify wordset type 
	 * @param ctx
	 * @param wordsetId
	 * @param type
	 * @param wordsetPersonalize
	 */
	public WordsetWordsAccessor(Context ctx, int wordsetId, WordsetType type, boolean wordsetPersonalize, ACCESS_TYPE accessType) {
			this.context = ctx;
			if(ctx instanceof WordsetWordsAccessor.Callbacks) 
				this.listener = (WordsetWordsAccessor.Callbacks) ctx; 
			
			this.wordsetId = wordsetId; 
			this.wordsetPersonalize = wordsetPersonalize; 
			this.type = type; 
			this.accessType = accessType;
			
			String wordsetType;
			switch(type) { 
				case SELECTED_WORDS:
					
					if(wordsetId > 0) { 
						wordsetType = "&type=selected";
					} else { 
						wordsetType = "&type=selected";
					}
					break; 
				case SYSTEM_WORDSET: 
					wordsetType = "&type=systemwordset";
					break; 
				case USER_WORDSET: 
					wordsetType = "&type=userwordset";
					break; 
				case REMEMBER_ME_WORDSET: 
					wordsetType = "&type=rememberme&uid=" + profileId();
					break; 
				case FORGOTTEN_WORDSET: 
					wordsetType = "&type=forgotten&uid=" + profileId();
					break; 
				case LEARNED_WORDS_WORDSET: 
					wordsetType = "&type=learned&uid=" + profileId();
					break; 
				default: 
					wordsetType = "&type=systemwordset";
					break;
			}
			
			
			String nativeCode = Preferences.getAccountPreferences(context)
										   .getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, 
												   context.getString(R.string.native_code_lower));
					
			String foreignCode = Preferences.getAccountPreferences(context)
											.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE,
												   context.getString(R.string.foreign_code_lower));
										    
			
			this.url = context.getString(R.string.getwordset_url, nativeCode, foreignCode, String.valueOf(wordsetId)) + wordsetType;
			
			Log.w(WordsetWordsAccessor.class.getName(), "Wordset URL: " + this.url);
	}
	
	private String profileId() 
	{
		int profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
		return String.valueOf(profileId); 
	}
	
	public int wordsetId() 
	{
		return wordsetId; 
	}
	
	/** 
	 * used with WordsetType.SELECTED_WORDS
	 * @param wordIds
	 */
	public void setSelectedWordIds(ArrayList<String> wordIds) 
	{
		this.selectedWordIds = wordIds; 
	}
	
	/**
	 * Method used to set callbacks listener 
	 * @param l
	 */
	public void setCallbacksListener(WordsetWordsAccessor.Callbacks l) 
	{
		this.listener = l; 
	}
	
	/**
	 * Method used to initialize wordset words loading process
	 */
	public void load() { 
		
		loadWordsetWords();
	}
	
	private synchronized void checkWordsLoaded() throws InterruptedException
	{
		// wait until words will be loaded
		while(!areWordsLoaded)
			wait(); 
	}
	
	private synchronized void checkWordsPersonalized() throws InterruptedException
	{
		if(wordsetPersonalize) 
			while(!areWordsPersonalized)
				wait(); 
	}
	
	private void loadWordsetWords() {
		// execute loading wordset words in background thread 
		new Thread( new Runnable() {
					
			@Override
			public void run() { 
						
					getWordsetWords(); 
						
					try {
						
						checkWordsLoaded(); 
						
						// if client want to personalize words 
						// it will be executed after words has
						// been loaded from data source
						personalizeWords();
							
						// if words are personalized (it is done in another thread) 
						// we must wait() for ending of this process
						checkWordsPersonalized();
							
						notifyWordsLoaded();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} 
			}
		}).start();
	}
	
	/**
	 * Helper method that personalize wordset words 
	 * ( - remove learned words 
	 *   - multiply by weight occurrences of forgotten words
	 * ) if client wants it after wordset words 
	 * has been loaded earlier.
	 * This method is executed on background thread. 
	 */
	private synchronized void personalizeWords() { 
		
		// if there is no need to personalize wordset 
		if(!wordsetPersonalize) 
			return; 
		
		// after words has been loaded load personalization for current wordIds
		Personalization p = new Personalization(context); 
		p.setCallbacks(this); 
		p.loadWordsPersonalizations(wordIds);
		
	}
	
	/**
	 * Method called by Personalization object after 
	 * words personalization has been finished on background thread.
	 */
	@Override
	public synchronized void onWordsPersonalizationFinished() {
		// mark that words has been personalized by Personalization object
		areWordsPersonalized = true; 
		notifyAll();
	}
	
	private synchronized void notifyWordsLoaded()
	{	
		
		// Words are loaded properly! Inform Learning Method Activity about this. 
		((Activity) context).runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if(listener != null)
					listener.onWordsLoadFinished(WordsetWordsAccessor.this); 
			} 
				
		});
	}
	
	/**
	 * public accessory methods
	 * @return
	 */
	public LinkedHashMap<Integer, String> getForeignArticels()
	{
		return this.foreignArticles; 
	}
	
	public LinkedHashMap<Integer, String> getForeignWords() 
	{
		return this.foreignWords; 
	}
	
	public LinkedHashMap<Integer, String> getNativeArticles()
	{
		return this.nativeArticles; 
	}
	
	public LinkedHashMap<Integer, String> getNativeWords()
	{
		return this.nativeWords;
	}
	
	public LinkedHashMap<Integer, String> getTranscriptions() 
	{
		return this.transcriptions;
	}
	
	public LinkedHashMap<Integer, String> getRecordingPaths()
	{
		return this.recordingPaths; 
	}
	
	public LinkedHashMap<Integer, String> getRecordingNames()
	{
		return this.recordingNames;
	}
	
	public LinkedHashMap<Integer, String> getImagePaths()
	{
		return this.imagePaths; 
	}
	
	public LinkedHashMap<Integer, byte[]> getImageData()
	{
		return this.imageData; 
	}
	
	public ArrayList<Integer> getWordIds()
	{
		return this.wordIds;
	}
	
	
	/**
	 * Helper method that loads wordset words details to instance variables
	 */
	private void getWordsetWords() 
	{
		// 1) Checking whether user prefer to use online or offline access to words
		if(Preferences.getBoolean(context, Preferences.KEY_PREFER_ONLINE_DATA, false) || accessType == ACCESS_TYPE.ONLINE) 
		{
			if( accessType == ACCESS_TYPE.OFFLINE) { 
				final Activity activity = (Activity) context;
				activity.runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(context, R.string.turn_off_prefer_online_prefs, Toast.LENGTH_LONG).show();
					}
				});
				return; 
			}
			
			// user prefer online access to data 
			// 2) we must check whether network connection is available
			if(NetworkUtilities.haveNetworkConnection(context)) {
				
				// 3) load all words data from web service 
				// including: words, recordings, images (we get only url addresses to them)
				// loading images and recordings will be defer to the time when they be needed
				loadWordsFromWebService(); 
				return; 
			}
			
		}
	
		// 4) user prefer OFFLINE access to data or network access isn't available
		if(accessType != ACCESS_TYPE.ONLINE) {
			loadWordsFromLocalStorage(); 
	    } else { 
	    	final Activity activity = (Activity) context;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(context, R.string.simple_online_access_isnt_possible_now, Toast.LENGTH_LONG).show();
				}
			});
	    	return;
	    }
	}
	
	
	/**
	 * Helper method used while loading words data from online web service
	 */
	private synchronized void loadWordsFromWebService()
	{
		Log.w(WordsetWordsAccessor.class.getName(), "Loading wordset words from web service...");
		
		/** 
		 *  // getting web service URL address from string resources 
		 *  url = context.getResources().getString(R.string.getwordset_url).replaceAll("&amp;", "&");
		 *  // appending wordset id at the end of URL address
		 *  url += wordsetId; 
		 */
		
		if( type == WordsetType.SELECTED_WORDS) {
			
			// append selected Word Ids to URL query string 
			url += "&words=" + getCommaSeparatedWordIds(selectedWordIds); 
		}
		
		GetWordsListFromXML wordsListObject;
		try { 
			// read online XML file into Input Stream
			InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGetOrThrow(url);
			Log.w(WordsetWordsAccessor.class.getName(), "Loading words from URL: "+ url); 
			// parse XML using special GetWordsListFromXML(InputStream) object 
			wordsListObject = new GetWordsListFromXML(is); 
			try { 
				is.close();
			} catch(IOException e) { }
			
			Log.w(WordsetWordsAccessor.class.getName(), "Number of loaded words from online: " + wordsListObject.getENWords().size());
		
			// loading words using GetWordsListFromXML  
			foreignArticles = wordsListObject.getENArticles();  
			foreignWords = wordsListObject.getENWords();
			nativeArticles = wordsListObject.getPLArticles();
			nativeWords = wordsListObject.getPLWords(); 
			transcriptions = wordsListObject.getTranscriptions(); 
			recordingNames = wordsListObject.getAudios();
	    
			// getting recordings directory URL from string resources
			String dirPath = context.getResources().getString(R.string.recordings_url).replaceAll("&amp;", "&");
	    
			recordingPaths = new LinkedHashMap<Integer, String>();
			
			// constructing path to recording files on web server 
			for(int wordId : wordsListObject.getAudios().keySet())
			{
				recordingPaths.put(wordId, dirPath + "/" + wordsListObject.getAudios().get(wordId));
			}
	    
			imagePaths = wordsListObject.getImages(); 
			imageData = null; 
			wordIds = new ArrayList<Integer>(foreignWords.keySet());
			
			Log.w(WordsetWordsAccessor.class.getName(), "Number of word Ids: " + foreignWords.keySet().size());
	    
			// wordset words has been loaded from online web service 
			Log.w(WordsetWordsAccessor.class.getName(), "Wordset words has been loaded successfully from online web service.");
			areWordsLoaded = true; 
			notifyAll(); 
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.w(WordsetWordsAccessor.class.getName(), "Wordset words hasn't been loaded from online web service!");
			
			final Activity activity = (Activity) context;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(context, R.string.problem_with_internet_connection ,Toast.LENGTH_SHORT).show();
					activity.finish();
				}
			});
			
		}
		
	}
	
	/**
	 * Helper method that convert wordIds array into comma separated string of ids
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
	 * Helper method used while user prefer to load data 
	 * from local storage or network conn isn't available.
	 * Needs some checks: 
	 * - whether data are locally available
	 * - how much data should be loaded from disk
	 */
	private void loadWordsFromLocalStorage()
	{
		Log.w(WordsetWordsAccessor.class.getName(), "Loading wordset words from local storage...");
		// 1) checking whether words data are available in OFFLINE data source (database, disk) 
		if(areWordsAvailableInDatabase(wordsetId, type))
		{
			if(type != WordsetType.SYSTEM_WORDSET) { 
				loadNotSystemWordsetWordsFromLocalStorage(); 
				return;
			}
				
			// 2) checking whether user prefer ONLINE or OFFLINE audio (default false) 
			if(Preferences.getBoolean(context, Preferences.KEY_PREFER_TO_DOWNLOAD_AUDIO, true))
			{
				// user prefer OFFLINE audio recordings
				if(isAudioStoredLocally()) {
					loadWordsFromLocalStorageAndAudioOffline(); 
				} else { 
					loadWordsFromLocalStorageAndAudioOnline(); 
				}
			} else { 
				// user prefer ONLINE  audio recordings
				loadWordsFromLocalStorageAndAudioOnline();
			}
		} else { 
			// words aren't available in local storage 
			// PROMPT: NO WORDS SYNCED
			if(accessType == ACCESS_TYPE.OFFLINE) { 
				final Activity activity = (Activity) context;
				activity.runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(context, R.string.words_not_saved_locally, Toast.LENGTH_LONG).show();
					}
				});
				return;
			} else if(accessType == ACCESS_TYPE.DEFAULT_NO_PROMPT) { 
				final Activity activity = (Activity) context;
				activity.runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(context, R.string.words_not_saved_locally_trying_online, Toast.LENGTH_LONG).show();
					}
				});
				if(NetworkUtilities.haveNetworkConnection(context)) { 
					loadWordsFromWebService();
				} else { 
					activity.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(context, R.string.loading_words_in_emergancy, Toast.LENGTH_LONG).show();
						}
					});
					loadWordsFromXMLResources();
				}
				return; 
			}
			
			promptNoWordsSynced(); 
		}
	}
	
	/**
	 * Helper method that is used to load words associated
	 * with not system wordset such as: user wordset, forgotten
	 * wordset, remember_me wordset, learned wordset, selected 
	 * words wordset when words are loaded from local database
	 * and audio will be tried to load depending on availability
	 * (not determined whether audio is local or online) 
	 */
	private void loadNotSystemWordsetWordsFromLocalStorage() {
		
		Log.w(WordsetWordsAccessor.class.getName(), "Decided to load not system wordset words from local "
													+ " storage and audio file later depending on their availability.");
		
		// getting from local database words based on wordset type
		Cursor cursor = getWordsFromLocalDatabase(type); // SYNCHRONOUS
		
		Log.w(WordsetWordsAccessor.class.getName(), "Retrieved " + cursor.getCount() + " number of not system words."); 
		
		// special path preceding recording to determine whether recording local availability is unknown 
		// and must be check previously and later if audio is unavailable try to download it from web server 
		String recordingPath = WordsetWordsAccessor.CHECK_RECORDING_AVAILABILITY; 
		
		fillWordDetailsFromCursor(cursor, recordingPath, false); // recording name not prepended with id
	}
	
	private synchronized void fillWordDetailsFromCursor(Cursor cursor, String recordingPath, boolean prependRecordingNameWithWordId)
	{
		Log.w(WordsetWordsAccessor.class.getName(), "Filling word details from cursor with: " + cursor.getCount() + " number of words.");
		foreignArticles = new LinkedHashMap<Integer, String>(); 
		foreignWords = new LinkedHashMap<Integer, String>(); 
		nativeArticles = new LinkedHashMap<Integer, String>(); 
		nativeWords = new LinkedHashMap<Integer, String>(); 
		transcriptions = new LinkedHashMap<Integer, String>(); 
		recordingPaths = new LinkedHashMap<Integer, String>(); 
		recordingNames = new LinkedHashMap<Integer, String>();
		imageData = new LinkedHashMap<Integer, byte[]>(); 
		wordIds = new ArrayList<Integer>();
		
		// moving through Cursor object to retrieve entries into HashMap containers
		while(cursor.moveToNext())
		{
			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_WORD_ID));
			foreignArticles.put(wordId, cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE)));  
			foreignWords.put(wordId, cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_FOREIGN_WORD)));
			nativeArticles.put(wordId, cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_NATIVE_ARTICLE)));
			nativeWords.put(wordId, cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_NATIVE_WORD))); 
			transcriptions.put(wordId, cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_TRANSCRIPTION)));
			
			String recordingName = cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_RECORDING));
			recordingNames.put(wordId, recordingName);
			if(prependRecordingNameWithWordId) 
				recordingPaths.put(wordId, recordingPath + "/"   
						+ wordId + recordingName.substring(recordingName.lastIndexOf("/")+1) );
			else
				recordingPaths.put(wordId, recordingPath + "/" + recordingName.substring(recordingName.lastIndexOf("/")+1));
			
			Log.w(WordsetWordsAccessor.class.getName(), recordingPaths.get(wordId));
			
			// getting image from BLOB stored in Word table.
			byte[] image = cursor.getBlob(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_IMAGE));
			if(image != null)
				imageData.put(wordId, image);
			else 
				imageData.put(wordId, null);
			
			wordIds.add(wordId);
		}
		
		// when loading words from local database we haven't URL addresses to image file stored in online web server
		imagePaths = null;
		
		areWordsLoaded = true; 
		cursor.close(); 
		notifyAll();
	}
	
	/**
	 * Helper methods that loads wordset words from 
	 * local storage (SQLite database) and audio recordings 
	 * are loaded from online server
	 */
	private void loadWordsFromLocalStorageAndAudioOnline() 
	{
		// Need to first check Internet access!
		// If no network available redirect to OFFLINE audio (possible old files persisted on disk)
		if(!NetworkUtilities.haveNetworkConnection(context)) { 
			loadWordsFromLocalStorageAndAudioOffline(); 
			return; 
		}
		
		Log.w(WordsetWordsAccessor.class.getName(), "Deciding to load wordset words from loacal" +
													" storage and audio files from online server.");
			
		// getting entries from joined WordsetWords and Word tables in SQLite database as Cursor object
		Cursor cursor = getWordsFromLocalDatabase(); //SYNCHRONOUS
		
		// getting online recordings directory URL from string resources
		String dirPath = context.getResources().getString(R.string.recordings_url).replaceAll("&amp;", "&");
		
		fillWordDetailsFromCursor(cursor, dirPath, false); // recording name not prepended with wordId
	}
	
	/**
	 * Helper method that loads wordset words from 
	 * local storage (SQLite database) and audio recordings
	 * are also loaded from disk.
	 */
	private void loadWordsFromLocalStorageAndAudioOffline()
	{
		Log.w(WordsetWordsAccessor.class.getName(), "Deciding to load wordset words from loacal" +
													" storage and audio files from disk's external storage.");
		
		// getting entries from joined WordsetWords and Word tables in SQLite database as Cursor object
		Cursor cursor = getWordsFromLocalDatabase();  //SYNCHRONOUS
		
		// getting local recordings directory path on disk
		File path = new File(FileUtilities.getExternalFilesDir(context), "recordings"); 
        String dirPath = path.getAbsolutePath();
        
        fillWordDetailsFromCursor(cursor, dirPath, true); // recording name prepended with wordId
	}
	
	/**
	 * Helper method used when loading wordset words 
	 * from locally stored SQLite database. 
	 * This method should use synchronous access to 
	 * database via Content Resolver cause learning 
	 * Activity can not be setup without loaded words
	 * @return Cursor object which points to words entries.
	 */
	private Cursor getWordsFromLocalDatabase()
	{
		
		final Uri WORDSET_WORDS_URI = Uri.parse(WordsetWordsProvider.CONTENT_URI_WORDSET_WORDS + "/" + wordsetId); 
		
		String[] projection = { 
					WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_WORD_ID),
					WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE), 
					WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_FOREIGN_WORD),
					WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_NATIVE_ARTICLE),
					WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_NATIVE_WORD), 
					WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_TRANSCRIPTION),
					WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_IMAGE), 
					WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_RECORDING),
					};
		
		Cursor cursor = context.getContentResolver().query(WORDSET_WORDS_URI, projection, null, null, null); 
		
		return cursor;
		
	}
	
	
	/**
	 * Overloaded method used to load word details for specific wordIds 
	 * depending on type of wordset passed in as parameter. It is helper 
	 * method used to service loading words for wordsets other than system wordsets.
	 */
	 private Cursor getWordsFromLocalDatabase(WordsetType type)
	 {
		 // using local wordIds array list, don't mistake with wordIds object instance property 
		 ArrayList<String> wordIds = null; 
		 switch(type) {
		 	case FORGOTTEN_WORDSET:
		 		//retrive array list of forggoten words  wordIds
		 		wordIds = getForgottenWordIds();
		 		break; 
		 	case REMEMBER_ME_WORDSET: 
		 		wordIds = getRememberMeWordIds(); 
		 		break; 
		 	case LEARNED_WORDS_WORDSET:
		 		wordIds = getLearnedWordIds(); 
		 		break;
		 	case SELECTED_WORDS:
		 		wordIds = selectedWordIds; 
		 		break;
		 	case USER_WORDSET: 
		 		// retrieve array list of user wordset wordIds for given wordsetId
		 		wordIds = getUserWordsetWordIds(wordsetId);   
		 		break; 
		 	case SYSTEM_WORDSET:
		 		// call just unparametrized version of this method 
		 		return getWordsFromLocalDatabase(); 
		 	default: 
		 		return null; 
		 }
		
		 String[] projection = { 
					WordProvider.WordTable.COLUMN_WORD_ID,
					WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE, 
					WordProvider.WordTable.COLUMN_FOREIGN_WORD,
					WordProvider.WordTable.COLUMN_NATIVE_ARTICLE,
					WordProvider.WordTable.COLUMN_NATIVE_WORD, 
					WordProvider.WordTable.COLUMN_TRANSCRIPTION,
					WordProvider.WordTable.COLUMN_IMAGE, 
					WordProvider.WordTable.COLUMN_RECORDING,
					};
		 
		 String selection = generateSelectionStringForWordIds(wordIds);
		 String[] selectionArgs = wordIds.toArray(new String[] {});
		 
		 Cursor cursor = context.getContentResolver().query(WordProvider.CONTENT_URI, projection, selection, selectionArgs, null);
		 
		 return cursor;
	 }
	
	/**
	 * Helper method used to check whether audio recordings
	 * are stored on external storage. This is checked 
	 * using ContentProvider on Wordset table.
	 * @return
	 */
	private boolean isAudioStoredLocally()
	{
		// create WORDSET_CONTENT_URI pointing to current wordset id 
		Uri WORDSET_CONTENT_URI = Uri.parse(WordsetProvider.CONTENT_URI + "/" + wordsetId); 
		
		// projection: COLUMN_IS_AUDIO_STORED_LOCALLY only needed
		String[] projection = { WordsetProvider.WordsetTable.COLUMN_IS_AUDIO_STORED_LOCALLY }; 
		
		// executing query 
		Cursor cursor = context.getContentResolver().query(WORDSET_CONTENT_URI, projection, null, null, null); 
		
		if(cursor.moveToFirst()) {
			
			int isAudioStoredLocally = cursor.getInt(cursor.getColumnIndexOrThrow(WordsetProvider.WordsetTable.COLUMN_IS_AUDIO_STORED_LOCALLY));
			cursor.close(); 
			
			if(isAudioStoredLocally == 1) {
				return true; 
			} 
			
			return false; 
		} else { 
			Log.w(WordsetWordsAccessor.class.getName(), "Wordset not found while checking is audio stored locally!");
		}
		cursor.close(); 
		return false; 
	}
	
	/**
	 * Helper method used to check if wordset words are
	 * available in local database (using ContentProvider query with COUNT(*) projection!)
	 */
	private boolean areWordsAvailableInDatabase(int wordsetId, WordsetType type)
	{
		if(wordsetId == 0)
			return areWordsAvailableInDatabase(type); 
		
		// checking whether counted number of words in given wordset is greater than 0 
		if(countWordsSavedInDatabase(wordsetId) > 0) { 
				Log.w(WordsetWordsAccessor.class.getName(), "There are some words saved in DB for wordset."); 
				return true; 
		}
		Log.w(WordsetWordsAccessor.class.getName(), "There aren't any words saved in DB for wordset."); 
		return false; 	
	}
	
	private boolean areWordsAvailableInDatabase(WordsetType type)
	{
		switch(type) { 
			case REMEMBER_ME_WORDSET:
			case LEARNED_WORDS_WORDSET: 
			case USER_WORDSET: 
				return true; // words are always available locally 
			case SELECTED_WORDS: 
				// check whether some selected words are available locally 
				// only called when wordsetId == 0, else treated as system_wordset 
				return areSelectedWordsAvailableInDatabase(); 
			case FORGOTTEN_WORDSET:
				return areForgottenWordsAvailableInDatabase();
			case SYSTEM_WORDSET: 
			default: 
				Log.w(WordsetWordsAccessor.class.getName(), "ERROR: wrong wordset type for wordsetId = 0.");
				return false; 
		}
	}
	
	/**
	 * Helper method used to check whether some selected words are saved in local 
	 * database. Words availability is checked only when wordsetId == 0, else 
	 * id wordsetId > 0 we check whether wordset words are available locally. 
	 * @return
	 */
	private boolean areSelectedWordsAvailableInDatabase() 
	{
		if(wordsetId > 0) { 
			return areWordsAvailableInDatabase(wordsetId, WordsetType.SYSTEM_WORDSET); 
		}
		// checks whether given wordIds from Intent are available locally
		if(selectedWordIds != null && selectedWordIds.size() > 0) { 
			
			if(selectedWordIds.size() == countWordsSavedInDatabaseFor(selectedWordIds))
			{
				Log.w(WordsetWordsAccessor.class.getName(), 
						"All selected words has been found as saved locally."); 
				return true; 
			}
			
		} else { 
			
			final Activity activity = (Activity) context;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(context, R.string.have_not_selected_words, Toast.LENGTH_SHORT).show();
					activity.finish();
				}
			});
			return true; 
		}
		Log.w(WordsetWordsAccessor.class.getName(), 
				"Not all selected words are saved in local database. Online access needed or synchronization."); 
		return false; 
	}
	
	
	/**
	 * Helper method used to check whether all forgotten words saved in forgottenTable
	 * are saved locally in wordTable.
	 * @return boolean value
	 */
	private boolean areForgottenWordsAvailableInDatabase() 
	{
		// implement checking whether forgotten wordIds are available locally 
		ArrayList<String> forgottenWordIds = getForgottenWordIds(); 
		
		if(forgottenWordIds.size() == countWordsSavedInDatabaseFor(forgottenWordIds))
		{
			Log.w(WordsetWordsAccessor.class.getName(), 
					"All forgotten words has been found as saved locally."); 
			return true; 
		}
		Log.w(WordsetWordsAccessor.class.getName(), 
				"Not all forgotten words are saved in local database. Online access needed or synchronization."); 
		return false; 
	}
	
	/**
	 * Helper method that returns array list with wordIds of all 
	 * forgotten words stored in forgottenTable. This can be used 
	 * to check whether all forgotten words are available locally
	 * or to load forgotten words details from wordTable.
	 * @return array list of forgotten wordIds
	 */
	private ArrayList<String> getForgottenWordIds() 
	{
		ArrayList<String> wordIds = new ArrayList<String>(); 
		
		// creating URI to ForgottenProvider used to query all forgotten words for current profile id
		Uri PROFILE_FORGOTTEN_WORDS_URI = Uri.parse(ForgottenProvider.CONTENT_URI + "/profile/" + profileId());
		// we need to select only word id column 
		String[] projection = { ForgottenProvider.ForgottenTable.COLUMN_WORD_ID };
		
		// executing query
		Cursor cursor = context.getContentResolver().query(PROFILE_FORGOTTEN_WORDS_URI, projection, null, null, null);
		
		// passing through all results
		while(cursor.moveToNext())
		{
			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(ForgottenProvider.ForgottenTable.COLUMN_WORD_ID));
			wordIds.add(String.valueOf(wordId));
		}
		
		cursor.close(); 
		
		return wordIds;	
	}
	
	private ArrayList<String> getRememberMeWordIds() 
	{
		ArrayList<String> wordIds = new ArrayList<String>(); 
		
		// Creating URI to RememberMeProvider used to query all remember_me words for current profile id 
		Uri PROFILE_REMEMBER_ME_WORDS_URI = Uri.parse(RememberMeProvider.CONTENT_URI + "/profile/" + profileId());
		// we need to select only word id column 
		String[] projection = { RememberMeProvider.RememberMeTable.COLUMN_WORD_ID }; 
		
		// executing query
		Cursor cursor = context.getContentResolver().query(PROFILE_REMEMBER_ME_WORDS_URI, projection, null, null, null);
		
		// passing through all results 
		while(cursor.moveToNext())
		{
			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(RememberMeProvider.RememberMeTable.COLUMN_WORD_ID));
			wordIds.add(String.valueOf(wordId));
		}
		
		cursor.close(); 
 		
		return wordIds; 
	}
	
	private ArrayList<String> getLearnedWordIds() 
	{
		ArrayList<String> wordIds = new ArrayList<String>(); 
		
		// Creating URI to LearnedWordsProvider used to query all learned words for current profile id 
		Uri PROFILE_LEARNED_WORDS_URI = Uri.parse(LearnedWordsProvider.CONTENT_URI + "/profile/" + profileId());
		// we need to select only word id column 
		String[] projection = { LearnedWordsProvider.LearnedTable.COLUMN_WORD_ID }; 
		
		// executing query 
		Cursor cursor = context.getContentResolver().query(PROFILE_LEARNED_WORDS_URI, projection, null, null, null); 
		
		// passing through all results 
		while(cursor.moveToNext())
		{
			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(LearnedWordsProvider.LearnedTable.COLUMN_WORD_ID));
			wordIds.add(String.valueOf(wordId)); 
		}
		
		cursor.close(); 
		
		return wordIds; 
	}
	
	private ArrayList<String> getUserWordsetWordIds(int userWordsetId)
	{
		ArrayList<String> wordIds = new ArrayList<String>(); 
		
		// Creating URI to UserWordsetWordsProvider used to query all word Ids for given user wordset word id 
		Uri  USER_WORDSET_WORDS_URI = Uri.parse(UserWordsetWordsProvider.CONTENT_URI + "/" + userWordsetId);
		// we need to select only word id column 
		String[] projection = { UserWordsetWordsProvider.UserWordsetWordsTable.COLUMN_WORD_ID }; 
		
		// executing query 
		Cursor cursor = context.getContentResolver().query(USER_WORDSET_WORDS_URI, projection, null, null, null); 
		
		// passing through all results 
		while(cursor.moveToNext())
		{
			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(UserWordsetWordsProvider.UserWordsetWordsTable.COLUMN_WORD_ID));
			wordIds.add(String.valueOf(wordId));
		}
		
		cursor.close(); 
		
		return wordIds; 
	}
	
	/**
	 * Method counts number of words details saved locally
	 * in database for passed in IDs. It can be used to compare with 
	 * total number of forgotten words stored in forgottenTable for given user
	 * or to compare with number of selected words user want to learn.  
	 * @return number of saved locally words in wordTable for given wordIds
	 */
	private int countWordsSavedInDatabaseFor(ArrayList<String> wordIds)
	{
		// we are using content URI for all rows and selection/selectionArgs to specify words to select
		String[] countProjection = { "COUNT(*) AS wordsCount" }; 
		
		String selection = generateSelectionStringForWordIds(wordIds); 
		String[] selectionArgs =  wordIds.toArray(new String[] {});  // is it ok ? 
		
 		Cursor cursor = context.getContentResolver()
								.query(WordProvider.CONTENT_URI, countProjection, selection, selectionArgs, null);
 		
 		// if returned cursor is empty there is error? 
 		if(cursor.moveToFirst()) 
 		{
 				// returning counted number of words for 
 				int count = cursor.getInt(0); 
 				cursor.close(); 
 				Log.w(WordsetWordsAccessor.class.getName(), "Words count for selected word ids is: " + count); 
 				return count; 
 		}
 		cursor.close(); 
 		return 0; 	 
	}
	
	/**
	 * Helper method used to generate selection string for passed in wordIds
	 * @param wordIds
	 * @return
	 */
	private String generateSelectionStringForWordIds(ArrayList<String> wordIds)
	{
		StringBuilder sb = new StringBuilder(); 
	
		sb.append("("); 
		// generating suitable number of wild card question marks
		for(@SuppressWarnings("unused") String wordId : wordIds) sb.append("?,"); 
		
		//trimming last comma separator and appending closing round bracket
		if(sb.length() > 1) {
			sb.setLength(sb.length()-1); 
		}
		sb.append(")"); 
		
		return  (WordProvider.WordTable.COLUMN_WORD_ID  + " IN " + sb.toString());
	}
	
	/**
	 * Helper method that counts number of words saved for given wordset in database.
	 */
	private int countWordsSavedInDatabase(int wordsetId) 
	{
		// creating URI to WordsetWordsProvider used to query all words for given wordset id
		Uri WORDSET_WORDS_CONTENT_URI = Uri.parse(WordsetWordsProvider.CONTENT_URI_WORDSET_WORDS + "/" + wordsetId);
		
		// projection: COUNT(*), used to count number of words for indicated by URI wordset
		String[] countProjection = { "COUNT(*) AS wordsCount" };
		
		// executing COUNT Query 
		Cursor cursor = context.getContentResolver()
							   .query(WORDSET_WORDS_CONTENT_URI, countProjection, null, null, null); 
		
		// if returned cursor is empty there is error? 
		if(cursor.moveToFirst()) 
		{
			// returning counted number of words in given wordset 
			int count = cursor.getInt(0); 
			cursor.close(); 
			Log.w(WordsetWordsAccessor.class.getName(), "Words count in wordset is: " + count); 
			return count; 
		}
		cursor.close(); 
		return 0; 	 
	}
	
	/**
	 * Helper method that prompts user that no words has been synchronized yet.
	 */
	private void promptNoWordsSynced()
	{
		final String message; 
		switch(type) { 
			case FORGOTTEN_WORDSET: 
				message = context.getString(R.string.forgotten_not_saved_yet);
				break; 
			case SYSTEM_WORDSET:
				message = context.getString(R.string.wordset_words_not_synced_yet);
				break;
			default: 
				message = context.getString(R.string.words_not_synced_yet);
				break;
		}
		
		((Activity) context).runOnUiThread( new Runnable() {
			
				@Override 
				public void run() {
					
					// create dialog that prompts user if he want to 
					// SYNC words now or prefer ONLINE access
					AlertDialog dialog = new AlertDialog.Builder(context)
										.setMessage(message)
										.setCancelable(false)
										.setPositiveButton(R.string.prefere_online_button,  new DialogInterface.OnClickListener() {
											
											@Override
											public void onClick(DialogInterface dialog, int which) {
												// Handle ONLINE wordset words access
												
												// checking if there is network available
												if(NetworkUtilities.haveNetworkConnection(context))
												{
													// redirect to online wordset words access
													new Thread(new Runnable() { 
														public void run() { 
															loadWordsFromWebService(); 
														}
													}).start();
												} else {
													// prompt user to turn on network in order to access words online 
													promptTurnOnNetworkToOnlineAccess(); 
												}
												
											}
										})
										.setNegativeButton(R.string.sync_now_button, new DialogInterface.OnClickListener() {
											
											@Override
											public void onClick(DialogInterface dialog, int which) {
												// Handle wordset words synchronization (redirect to WordsetActivity)
												
												// checking if ther is network available 
												if(NetworkUtilities.haveNetworkConnection(context))
												{
													// redirect to WordsetActivity in order to synchronize wordset 
													redirectToSynchronizeWordset(); 
												} else { 
													// prompt user to turn on network before synchronization 
													promptTurnOnNetworkBeforeSynchronization(); 
												}
											}
										})
										.create();
				
					dialog.show();
			}
		});
	}
	
	/**
	 * Helper method that prompts user to turn on network connection in order to access wordset words online 
	 */
	private void promptTurnOnNetworkToOnlineAccess()
	{
		final String negativeMessage; 
		
		switch(type) { 
			case SYSTEM_WORDSET: 
				negativeMessage = context.getString(R.string.emergancy_mode);
				break; 
			default: 
				negativeMessage = context.getString(R.string.cancel); 
				break; 
		}
		
(		(Activity) context).runOnUiThread( new Runnable() {
			
			@Override 
			public void run() {
				// Execute on Main UI Thread - things like updating View or other Activity UI element.
				
				// create dialog that prompts user if he wants 
				// to access wordset words online in order for him to
				// turn on network connection. 
				AlertDialog dialog = new AlertDialog.Builder(context)
									.setMessage(R.string.no_network_dialog_message)
									.setCancelable(false)
									.setPositiveButton(R.string.connect_button,  new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											
											// Connect to Internet 
											WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
											wifiManager.setWifiEnabled(true);
											
											int i = 0; 
											while(!NetworkUtilities.haveNetworkConnection(context) && i < 10 )
											{
												try {
													Thread.sleep(1000);
												} catch (InterruptedException e) {
													e.printStackTrace();
												}
											}
											
											// If successfully connected with WIFI or mobile network 
											boolean connected = NetworkUtilities.haveNetworkConnection(context);
											if(connected) {
												new Thread(new Runnable() { 
													public void run() { 
														loadWordsFromWebService(); 
													}
												}).start(); 
											} else { 
												// When couldn't connect to the internet and load words from web service, run in emergancy mode
												Log.w(WordsetWordsAccessor.class.getName(), "Trying to connect to the Internet failed! Emergency Mode.");
												Toast.makeText(context, R.string.trying_to_connect_failed_emergancy, Toast.LENGTH_SHORT).show();
												new Thread(new Runnable() { 
													public void run() { 
														if(type != WordsetType.SYSTEM_WORDSET) { 
															((Activity) context).finish(); return;
														}
														loadWordsFromXMLResources(); 
													}
												}).start(); 
											}
											
										}
									})
									.setNegativeButton(negativeMessage, new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											// learn words in emergancy mode (no sound, no images, static xml files)
											new Thread(new Runnable() { 
												public void run() { 
													if(type != WordsetType.SYSTEM_WORDSET) { 
														((Activity) context).finish(); return;
													}
													
													loadWordsFromXMLResources(); 
												}
											}).start(); 
										}
									})
									.create();
	
				dialog.show();
			}
		}); 
	}
	
	
	/**
	 * Helper method that prompts user to turn on network connection in order to synchronize words
	 */
	private void promptTurnOnNetworkBeforeSynchronization()
	{
			final String negativeMessage; 
			
			switch(type) { 
				case SYSTEM_WORDSET: 
					negativeMessage = context.getString(R.string.emergancy_mode);
					break; 
				default: 
					negativeMessage = context.getString(R.string.cancel); 
					break; 
			}
		
		((Activity) context).runOnUiThread( new Runnable() {
			
			@Override 
			public void run() {
				// Execute on Main UI Thread - things like updating View or other Activity UI element.
				
				// create dialog that prompts user if he wants 
				// to synchronize wordset words in order for him to 
				// turn on network connection.
			    AlertDialog dialog = new AlertDialog.Builder(context)
									.setMessage("No network connection! Would you like to connect with Internet?")
									.setCancelable(false)
									.setPositiveButton("Connect & Sync",  new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											
											// Connect to Internet 
											WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
											wifiManager.setWifiEnabled(true);
											
											int i = 0; 
											while(!NetworkUtilities.haveNetworkConnection(context) && i < 10 )
											{
												try {
													Thread.sleep(1000);
												} catch (InterruptedException e) {
													e.printStackTrace();
												}
											}
											
											// If successfully connected with WIFI or mobile network 
											boolean connected = NetworkUtilities.haveNetworkConnection(context); 
											if(connected) {
												redirectToSynchronizeWordset(); 
											} else { 
												// When couldn't connect to the internet and sync words run in emergancy mode
												Log.w(WordsetWordsAccessor.class.getName(), "Trying to connect to the Internet failed! Emergency Mode.");
												Toast.makeText(context, R.string.trying_to_connect_failed_emergancy, Toast.LENGTH_SHORT).show();
												new Thread(new Runnable() { 
													public void run() { 
														if(type != WordsetType.SYSTEM_WORDSET) { 
															((Activity) context).finish(); return;
														}
														
														loadWordsFromXMLResources(); 
													}
												}).start();
											}
										}
									})
									.setNegativeButton(negativeMessage , new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											// learn words in emergancy mode (no sound, no images, static xml files)
											new Thread(new Runnable() { 
												public void run() { 
													if(type != WordsetType.SYSTEM_WORDSET) { 
														((Activity) context).finish(); return;
													}
													
													loadWordsFromXMLResources();
												}
											}).start();
											
										}
									})
									.create();
				
		
					dialog.show();
			}
		});
		
	}
	
	
	/**
	 * Helper method loading wordset words from static XML resources 
	 * stored in App resources directories. This is used to support  
	 * emergency mode. 
	 * 
	 */
	private synchronized void loadWordsFromXMLResources() 
	{
		// construct name of XML resource stored in raw resources directory
		String xmlName = "wordset" + wordsetId;
		// and get its resource identifier 
		int xmlResourceId = context.getResources().getIdentifier(xmlName, "raw", context.getPackageName());
		
		// read XML file into input stream
		InputStream is = context.getResources().openRawResource(xmlResourceId);
		// parse XML file using GetWordsListFromXML(InputStream) object
		GetWordsListFromXML wordsListObject = new GetWordsListFromXML(is);
		// free InputStream
		try { 
			is.close(); 
		} catch(IOException ex) { }
		
		// loading words using GetWordsListFromXML      
        foreignArticles = wordsListObject.getENArticles();
    	foreignWords = wordsListObject.getENWords(); 
    	nativeArticles = wordsListObject.getPLArticles(); 
    	nativeWords = wordsListObject.getPLWords();
        transcriptions = wordsListObject.getTranscriptions(); 
        recordingNames = wordsListObject.getAudios();
        
        // Emergency mode is used only if the Internet connection isn't available
        // so it doesn't make sense to use URLs to recordings on the web server
        // better idea is to try to run recordings from disk (maybe some undeleted files)
        File path = new File(FileUtilities.getExternalFilesDir(context), "recordings"); 
        String dirPath = path.getAbsolutePath();
        
        recordingPaths = new LinkedHashMap<Integer, String>(); 
        for(int wordId : wordsListObject.audios.keySet()) { 
        	// constructing local recording paths: <directory>/<wordsetId>_<wordId><recordingName.mp3>
        	recordingPaths.put(wordId, dirPath + File.separator 
        						       + wordId + wordsListObject.getAudios().get(wordId)); 
        }
        // Images are not available when there is no Internet connection.
        imagePaths = wordsListObject.getImages();  
        imageData = null; 
        wordIds =  new ArrayList<Integer>(foreignWords.keySet());
        
        if( type == WordsetType.SELECTED_WORDS ) { 
        	for(Integer wordId : wordIds) { 
        		if(!selectedWordIds.contains(wordId)) {
        			wordIds.remove(wordId);
        		}
        	}
        }
        
        // wordset words has been loaded
        Log.w(WordsetWordsAccessor.class.getName(), "Wordset words has been loaded successfully from raw XML.");
        areWordsLoaded = true; 
        notifyAll(); 
      
	}
	
	/**
	 * Helper method used to redirect user to WordsetActivity 
	 * in order to synchronize wordset words. Using Intent 
	 * to start synchronization process. Current Activity must be
	 * finished before going to WordsetActivity. 
	 */
	private void redirectToSynchronizeWordset()
	{
		switch(type) { 
			
			case SYSTEM_WORDSET: 
				synchronizeSystemWordset();
				return; 
			case SELECTED_WORDS: 
				// when wordsetId > 0 then synchronize all wordset words not just selected 
				if(wordsetId > 0) { 
					synchronizeSystemWordset();
				} else { 
				// else if these are other selected words and there is wordsetId = 0 download only selected words
					synchronizeSelectedWords(); 
				}
				return; 
			// below WORDSETS are always stored in local database 
			case REMEMBER_ME_WORDSET:
			case LEARNED_WORDS_WORDSET:
			case USER_WORDSET:  
				((Activity) context).finish();
				return; 
			case FORGOTTEN_WORDSET: 
				synchronizeForgottenWordset(); 
				return; 
			default: 
				break; 
		}
		
		
	}
	
	private void synchronizeSystemWordset() 
	{
		// creating new Intent that starts Activity 
		Intent syncIntent = new Intent(context, WordsetActivity.class); 
		syncIntent.putExtra(WordsetsListActivity.SELECTED_WORDSET, wordsetId); 
		syncIntent.putExtra(WordsetActivity.START_WORDS_SYNCHRONIZATION, true); 
				
		Activity activity = (Activity) context;
		activity.finish(); 
		context.startActivity(syncIntent); 
	}
	
	private void synchronizeSelectedWords() 
	{
		synchronizeSelectedWords(selectedWordIds);
	}
	
	private void synchronizeSelectedWords(ArrayList<String> selectedWordIds) 
	{
		Log.w(WordsetWordsAccessor.class.getName(), selectedWordIds.toString());
		// creating new Intent that starts Main Activity for syncing selected words 
		Intent syncIntent = new Intent(context,MainActivity.class); 
		syncIntent.putStringArrayListExtra(WordsetWordsAccessor.KEY_WORDS_TO_SYNC, selectedWordIds); 
		syncIntent.putExtra(WordsetWordsAccessor.KEY_START_SELECTED_WORDS_SYNCHRONIZATION, true);
		
		Log.w(WordsetWordsAccessor.class.getName(), "Starting selected words synchronization..."); 
		
		Activity activity = (Activity) context;
		activity.finish(); 
		context.startActivity(syncIntent); 
	}
	
	private void synchronizeForgottenWordset() 
	{
		synchronizeSelectedWords(getForgottenWordIds());
	}
	
}
