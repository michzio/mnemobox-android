/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;
import pl.elector.database.WordsetProvider;
import pl.elector.database.WordsetType;
import pl.elector.service.WordsLoaderService;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 *
 * @author Michał Ziobro
 * @date modified 11.10.2014
 */
public class WordsetActivityOld extends DrawerActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    
	private static final int WORDSET_INFO_LOADER = 0x01;
	// Intent extra key that is used to pass in boolean value indicating to start words synchronization process
	public static final String START_WORDS_SYNCHRONIZATION = "START_WORDS_SYNCHRONIZATION"; 
	// Wordset information Bundle keys 
	public static final String KEY_WORDSET_FOREIGN_NAME = "KEY_WORDSET_FOREIGN_NAME"; 
	public static final String KEY_WORDSET_NATIVE_NAME = "KEY_WORDSET_NATIVE_NAME"; 
	public static final String KEY_WORDSET_LEVEL = "KEY_WORDSET_LEVEL"; 
	public static final String KEY_WORDSET_DESCRIPTION = "KEY_WORDSET_DESCRIPTION"; 
	public static final String KEY_SELECTED_WORD_IDS = "KEY_SELECTED_WORD_IDS"; 
	
	 private int wordsetId; 
     private String wordsetForeignName;
     private String wordsetNativeName; 
     private String wordsetLevel; 
     private String wordsetDescription;
     private boolean paidUp = false;
     private int intMoney = 0; 
     private GetUserinfoFromXML userinfo; 
     
 	@Override
 	protected void onCreateDrawerActivity(Bundle savedInstanceState) {
 		
 		 setContentView(R.layout.wordset_drawer);
 		// get from database wordset information for current wordset Id and fill view with them
         loadWordsetInfoFromDatabase();
         
         /*if(!checkUserWordsetAccess()) { 
	         ScrollView wordsetScrollView = (ScrollView) findViewById(R.id.wordsetScrollView);
	         wordsetScrollView.setVisibility(View.GONE); 
     	} else { 
	         loadLearningMethodsIcons();
	         attachOnClickEvents(); 
     	}*/
         loadLearningMethodsIcons();
         attachOnClickEvents();
         // when restarting Activity passing that we have new intent to analyzes
         onNewIntent(getIntent());
         AppRater.app_launched(this);
 	}
     
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
    }
    
    /**
     * Helper method used to start words synchronization service.
     * Used after manual clicking Synchronize Words button or 
     * when new intent received with special extra flag set to TRUE.
     * ex. Intent from Dialog in learning Activity like PresentationActivity. 
     */
	private void startWordsetWordsSynchronization()
	{
		// launching Service that will load data associated with wordset
		// such as: words, words details, images, and if user wants audio recordings
		Intent serviceIntent = new Intent(WordsetActivityOld.this, WordsLoaderService.class);
		serviceIntent.putExtra(WordsLoaderService.DOWNLOADED_WORDSET_ID, wordsetId); 
		serviceIntent.putExtra(WordsLoaderService.WORDSET_TITLE, wordsetForeignName);
		startService(serviceIntent); 
		showProgressDialog();
		Log.w(WordsetActivityOld.class.getName(), "WordsLoaderService has been started...");
	}
    
    /**
     * Method is attaching onClick event listeners to button 
     * on learning method list. This event listeners launch 
     * new Activity for given learning method, passing in wordsetId 
     * as Intent extras.
     */
    private void attachOnClickEvents() { 
    	
    	// Synchronize Words button on click listener 
    	Button syncWordsBtn = (Button) findViewById(R.id.synchronizeWords);
    	
    	syncWordsBtn.setOnClickListener( new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				if(NetworkUtilities.haveNetworkConnection(WordsetActivityOld.this)) { 
					startWordsetWordsSynchronization();
				} else { 
					Toast.makeText(WordsetActivityOld.this, R.string.connect_internet_to_download_words, Toast.LENGTH_LONG).show(); 
					Log.w(WordsetActivityOld.class.getName(), "Can not synchronize words! There is not internet connection."); 
				}
				   
			}
    		 
    	});
    	
    	// List of Words learning method on click listener
        Button listOfWordsBtn = (Button) findViewById(R.id.listOfWordsBtn);
        
        listOfWordsBtn.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View view) {
            	// Creating new explicit Intent to start ListOfWordsActivity
            	// and filling its extras with current wordsetId.
            	startLearningMethodActivity(ListOfWordsActivity.class);   
                
            }
        });
        
        // FlashCards learning method on click listener
        Button flashcardsBtn = (Button) findViewById(R.id.flashcardsBtn);
        
        flashcardsBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View view) {
            	// Creating new explicit Intent to start FlashCardsActivity 
            	// and filling its extras with current wordsetId.
            	startLearningMethodActivity(FlashCardsActivity.class);

            }
        });
         
        // SimpleRepetition learning method on click listener
        Button simpleRepetitionBtn = (Button) findViewById(R.id.simpleRepetitionBtn);
        
        simpleRepetitionBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View view) {
            	// Creating new explicit Intent to start SimpleRepetitionActivity
            	// and filling its extras with current wordsetId.
            	startLearningMethodActivity(SimpleRepetitionActivity.class);
  
            }
        });
        
        // Presentation learning method on click listener 
        Button presentationBtn = (Button) findViewById(R.id.presentationBtn);
        
        presentationBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				// Creating new explicit Intent to start PresentationActivity 
				// and filling its extras with current wordsetId. 
				startLearningMethodActivity(PresentationActivity.class);
			}
        });
        
        // Repetition learning method on click listener 
        Button repetitionBtn = (Button) findViewById(R.id.repetitionBtn);
        
        repetitionBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				// Creating new explicit Intent to start RepetitionActivity 
				// and filling its extras with current wordsetId. 
				startLearningMethodActivity(RepetitionActivity.class);

			}
        });
        
        // Speaking learning method on click listener 
        Button speakingBtn = (Button) findViewById(R.id.speakingBtn);
        
        speakingBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				// Creating new explicit Intent to start SpeakingActivity 
				// and filling its extras with current wordsetId. 
				startLearningMethodActivity(SpeakingActivity.class);
			}
        });   
        
        // Listening learning method on click listener 
        Button listeningBtn = (Button) findViewById(R.id.listeningBtn);
        
        listeningBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				// Creating new explicit Intent to start ListeningActivity 
				// and filling its extras with current wordsetId. 
				startLearningMethodActivity(ListeningActivity.class);
			}
        });
        
        // Choosing learning method on click listener 
        Button choosingBtn = (Button) findViewById(R.id.choosingBtn);
        
        choosingBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				// Creating new explicit Intent to start ChoosingActivity 
				// and filling its extras with current wordsetId. 
				startLearningMethodActivity(ChoosingActivity.class);
			}
        });
        
        // Cartons learning method on click listener 
        Button cartonsBtn = (Button) findViewById(R.id.cartonsBtn);
        
        cartonsBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View view) {
				// Creating new explicit Intent to start CartonsActivity 
				// and filling its extras with current wordsetId. 
				startLearningMethodActivity(CartonsActivity.class);
			}
        });
    }
    
    
    /**
     * Helper method that wraps logic for starting suitable Learning Activity
     * using explicit intent and passing to it wordset id , wordset type, and selected words array
     */
    private void startLearningMethodActivity(Class<?> learningMethodClass)
    {
		Intent intent = new Intent(WordsetActivityOld.this, learningMethodClass);
		intent.putExtra(WordsetsListActivity.SELECTED_WORDSET, wordsetId);
		
		if(wordsAdapter.wordsHasBeenSelected()) { 
			intent.putExtra(WordsetType.KEY_TYPE, WordsetType.SELECTED_WORDS);
			intent.putExtra(WordsetActivityOld.KEY_SELECTED_WORD_IDS, wordsAdapter.getSelectedWordIds());
		} else { 
			intent.putExtra(WordsetType.KEY_TYPE, WordsetType.SYSTEM_WORDSET);
		}
		
		startActivity(intent);
    }
    
    /**
     * This is method that fills user interface learning methods list with 
     * icons that depicts each method.
     */
    private void loadLearningMethodsIcons() { 
        
        ImageView listOfWordsIV = (ImageView) findViewById(R.id.listOfWordsIV);
        listOfWordsIV.setImageResource(R.drawable.pen);
        
        ImageView flashCardsIV = (ImageView) findViewById(R.id.flashcardsIV);
        flashCardsIV.setImageResource(R.drawable.presentation); // ! should be changed to new icon
        
        ImageView simpleRepetitionIV = (ImageView) findViewById(R.id.simpleRepetitionIV);
        simpleRepetitionIV.setImageResource(R.drawable.repetition); // ! should be changed to new icon
        
        ImageView presentationIV = (ImageView) findViewById(R.id.presentationIV);
        presentationIV.setImageResource(R.drawable.presentation);
        
        ImageView repetitionIV = (ImageView) findViewById(R.id.repetitionIV);
        repetitionIV.setImageResource(R.drawable.repetition);
        
        ImageView speakingIV = (ImageView) findViewById(R.id.speakingIV); 
        speakingIV.setImageResource(R.drawable.speaking); 
        
        ImageView listeningIV = (ImageView) findViewById(R.id.listeningIV);
        listeningIV.setImageResource(R.drawable.listening); 
        
        ImageView choosingIV = (ImageView) findViewById(R.id.choosingIV);
        choosingIV.setImageResource(R.drawable.choosing);
        
        ImageView cartonsIV = (ImageView) findViewById(R.id.cartonsIV);
        cartonsIV.setImageResource(R.drawable.cartons);
    }
    
    /**
     * This method is used to load wordset information:
     * foreignName, nativeName, level, desc from SQLiteDatabase.
     */
    private void loadWordsetInfoFromDatabase() {
    	
    	getSupportLoaderManager().initLoader(WORDSET_INFO_LOADER, null, this);
    }
    
    /**
     * Implementation of LoaderManager.LaderCallbacks<Cursor> interface
     */
    
    // This method creates CursorLoader object after initLoader() call
    // @id - parameter is used to create different Loaders
    // @args - parameter is a Bundle with additinal arguments for constructed Loader
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    	
    	CursorLoader cursorLoader = null;
    	
    	switch(id) {
    		
    		case WORDSET_INFO_LOADER: 
    			
    			// CursorLoader is used to construct a new query
    			String[] projection = { WordsetProvider.WordsetTable.COLUMN_WORDSET_ID, 
						WordsetProvider.WordsetTable.COLUMN_CATEGORY_ID,
						WordsetProvider.WordsetTable.COLUMN_WORDSET_FOREIGN_NAME,
						WordsetProvider.WordsetTable.COLUMN_WORDSET_NATIVE_NAME,
						WordsetProvider.WordsetTable.COLUMN_WORDSET_LEVEL,
						WordsetProvider.WordsetTable.COLUMN_WORDSET_ABOUT,
						WordsetProvider.WordsetTable.COLUMN_IS_AUDIO_STORED_LOCALLY
					  };
    			
    			String where = null; 
				String[] whereArgs = null; 
				String sortOrder = null; 
				
				// constructing query URI pointing to current wordset in database 
				wordsetId = getIntent().getIntExtra(WordsetsListActivity.SELECTED_WORDSET, 0);
				Uri queryUri = Uri.parse(WordsetProvider.CONTENT_URI + "/" + wordsetId); 
				
				// Create an return the new CursorLoader
				cursorLoader = new CursorLoader(this, queryUri, projection, where, whereArgs, sortOrder);
				
    		break; 
    	}
    	
    	return cursorLoader;
    }

    // Method called when data are loaded from data source 
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

    	// checking whether Cursor contains wordset 
        // if it's size id 0, there are no wordset with given wordsetId in database
    	if(cursor.moveToFirst())
		{
				wordsetId = cursor.getInt(
									cursor.getColumnIndexOrThrow(
												WordsetProvider.WordsetTable.COLUMN_WORDSET_ID));
				wordsetForeignName = cursor.getString(
											cursor.getColumnIndexOrThrow(
												WordsetProvider.WordsetTable.COLUMN_WORDSET_FOREIGN_NAME));
				wordsetNativeName = cursor.getString(
											cursor.getColumnIndexOrThrow(
												WordsetProvider.WordsetTable.COLUMN_WORDSET_NATIVE_NAME));
				wordsetLevel = cursor.getString(
										cursor.getColumnIndexOrThrow(
												WordsetProvider.WordsetTable.COLUMN_WORDSET_LEVEL));
				wordsetDescription = cursor.getString(
										cursor.getColumnIndexOrThrow(
												WordsetProvider.WordsetTable.COLUMN_WORDSET_ABOUT));
				fillViewWithWordsetInformations();
				
    	} else {
    		// try to load wordsets list from online web service 
    		Log.w(WordsetsListActivity.class.getName(), 
    			   "Database doesn't contain given wordset, redirecting user to wordsets list.");
    		getSupportLoaderManager().destroyLoader(WORDSET_INFO_LOADER);
    		// emergency mode! loading wordset info from XML resource 
    		loadWordsetInfoFromXMLResources();
    	}
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }
    
    /**
     * Helper method used when App is running in
     * emergency mode. Loads wordset information
     * from XML resources. 
     */
    private void loadWordsetInfoFromXMLResources()
    {
    	// getting wordset info values loaded earlier from XML
    	// and passed in Intent Bundle
    	wordsetId = getIntent().getExtras().getInt(WordsetsListActivity.SELECTED_WORDSET);
    	wordsetForeignName = getIntent().getExtras().getString(WordsetActivityOld.KEY_WORDSET_FOREIGN_NAME);
    	wordsetNativeName = getIntent().getExtras().getString(WordsetActivityOld.KEY_WORDSET_NATIVE_NAME);
    	wordsetLevel = getIntent().getExtras().getString(WordsetActivityOld.KEY_WORDSET_LEVEL);
    	wordsetDescription = getIntent().getExtras().getString(WordsetActivityOld.KEY_WORDSET_DESCRIPTION);
    	fillViewWithWordsetInformations();
    }
    
    private void fillViewWithWordsetInformations() { 
        TextView enTitleTextView = (TextView) findViewById(R.id.wordsetTitleEN);
        enTitleTextView.setText(wordsetForeignName);
        TextView plTitleTextView = (TextView) findViewById(R.id.wordsetTitlePL);
        plTitleTextView.setText(wordsetNativeName);
        TextView descTextView = (TextView) findViewById(R.id.wordsetDescription);
        descTextView.setText(wordsetDescription);
        TextView levelTextView = (TextView) findViewById(R.id.wordsetLevel);
        levelTextView.setText(getResources().getString(R.string.wordset_level) + wordsetLevel);
        
    }
    
    @SuppressWarnings("unused")
	private boolean checkUserWordsetAccess() { 
        String userId = Preferences.getString(this, "userId", "");
        String webServiceUrl = getResources().getString(R.string.check_wordset_access_url).replaceAll("&amp;", "&");
        webServiceUrl += userId + "&wid=" + wordsetId;
        String response = "0";
        if(NetworkUtilities.haveNetworkConnection(this)) { 
            try {
                response = CustomHttpClient.executeHttpGet(webServiceUrl);
               
            } catch(Exception e) {
                e.printStackTrace();
               /* Toast.makeText(this, e.getMessage(),
                                Toast.LENGTH_SHORT).show();*/
            }
            int resultInt = Integer.parseInt(response.substring(0, 1));
        
            if(resultInt == 1) {
                return true; 
            } 
        } else { 
            Toast.makeText(this, R.string.internet_lost,
                                Toast.LENGTH_SHORT).show();
        }
         userinfo =  GetUserinfoFromXML.getMyProfileInfo(this);
         if(userinfo.paidupAccount) return true; 
        return true; //always users have access to this free appp
    }
    
    
    /**
     * Broadcast Receiver to receive info about wordset synchronization
     * progress. This informations are uset to display download progress 
     * on dialog window above Wordset Activity. 
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			Bundle bundle = intent.getExtras(); 
			if(bundle != null) { 
				// int wid = bundle.getInt(WordsLoaderService.DOWNLOADED_WORDSET_ID); 
				int wordsSize = bundle.getInt(WordsLoaderService.WORDS_SIZE); 
				int wordsCounter = bundle.getInt(WordsLoaderService.WORDS_COUNT); 
				
				Log.w(WordsetActivityOld.class.getName(), "Broadcast received! Downloaded " + wordsCounter + "/" + wordsSize + " words.");
				
				if(progressDialog == null) {
					showProgressDialog(); 
				}
				if(wordsCounter < wordsSize) { 
					progressDialog.setMax(wordsSize);
					progressDialog.setProgress(wordsCounter); 
					progressDialog.setMessage("Downloaded " + wordsCounter + "/" + wordsSize + " words.");
				} else { 
					progressDialog.setMax(wordsSize);
					progressDialog.setProgress(wordsCounter); 
					progressDialog.setMessage("Download completed!");
					progressDialog.setCancelable(true);
					progressDialog.setCanceledOnTouchOutside(true); 
				}
			}
			
		}
    	
    };
    
    @Override 
    public void onResume() {
    	super.onResume(); 	
    	// register the broadcast receiver that receives words loader notifications
    	registerReceiver(receiver, new IntentFilter(WordsLoaderService.WORDS_LOADER_BROADCAST)); 
    	ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.english);
		actionBar.setSubtitle(R.string.domain_name);
    }
    
    @Override 
    public void onPause() {
    	super.onPause(); 
    	// unregiser the broadcast receiver that receives words loader notifications
    	unregisterReceiver(receiver); 
    }
    
    private ProgressDialog progressDialog = null; 
	
	private void showProgressDialog() { 
			// create new progress dialog 
			progressDialog = new ProgressDialog(WordsetActivityOld.this); 
			// setting the progress dialog to display a horizontal progress bar 
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			// setting the dialog title to 'Downloading words data...'
			progressDialog.setTitle("Downloading words data..."); 
			// setting the dialog message to 'Initializing download...'
			progressDialog.setMessage("Initializing downlod...");
			// this dialog can't be canceled by pressing the back key
			progressDialog.setCancelable(true);
			progressDialog.setCanceledOnTouchOutside(false); 
			// this dialog isn't indeterminate
			progressDialog.setIndeterminate(false); 
			// setting the initial max number of items 
			progressDialog.setMax(100); 
			// setting the current progress to zero 
			progressDialog.setProgress(0); 
			// display the progress dialog
			progressDialog.show(); 
	}
		
	@Override 
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		// updating Intent
		setIntent(intent); 
		
		// called when pending intent from Notification relaunch Activity 
		// or when user moves back from Learning Activity after selecting 
		// wordset words synchronization on Dialog prompt.
		
		if(getIntent().getExtras().getBoolean(WordsetActivityOld.START_WORDS_SYNCHRONIZATION, false)) 
		{
			getSupportLoaderManager().restartLoader(WORDSET_INFO_LOADER, null, this); 
			
			// user wants to automatically start synchronization process!
			startWordsetWordsSynchronization(); 
		}
	}

	@Override
	protected int getRightDrawerMenuButtonId() {
		
		return R.id.selectWordsBtn;
	}
	
	@Override 
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = this.getMenuInflater(); 
		inflater.inflate(R.menu.wordset_menu, menu); 
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override 
	public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == getRightDrawerMenuButtonId()) {

        }
    	
		return super.onOptionsItemSelected(item);
    }
	
	
	private WordsetWordsAdapterOld wordsAdapter; 
	
	@Override 
	protected void configureRightDrawer() {
		
		super.configureRightDrawer();
		
		// populating right drawer expandable list view with word items for current wordset.
		wordsAdapter = new WordsetWordsAdapterOld(this, wordsetId);
		configureRightHeaderView();
		rightDrawerList.setAdapter(wordsAdapter);
		rightDrawerList.setOnGroupClickListener(this);
	}

	/**
	 * Method used to display header view on right drawer 
	 * with buttons to select all/deselect all words 
	 */
	private void configureRightHeaderView() {
			
		View view = getLayoutInflater().inflate(R.layout.checkbox_item, null); 
		CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
		checkbox.setChecked(true); 
		checkbox.setText(R.string.select_all); 
		checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton view, boolean isChecked) {
				
				// CheckBox checkbox = (CheckBox) view;
				
				if(isChecked) { 
					// select all words
					wordsAdapter.selectAllWords(); 
				} else { 
					// unselect all words
					wordsAdapter.unselectAllWords(); 
				}
			} 
			
		});
		rightDrawerList.addHeaderView(view); 
	}
	
	@Override
	public void onDestroy() 
	{
		Log.w(WordsetActivityOld.class.getName(), " onDestroy() WordsetActivity..."); 
		wordsAdapter.clearDataSet();
		this.rightDrawerList.setAdapter((ExpandableListAdapter) null); 
		super.onDestroy();
	}
   
}

