/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;

import pl.elector.database.WordsetProvider;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;


/**
 *
 * @author Michał Ziobro
 */
public class WordsetsListActivity extends DrawerActivity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {
    
	private static final int WORDSETS_LIST_LOADER = 0x02;
	public static final String SELECTED_WORDSET = "SELECTED_WORDSET"; 
	public static final String SELECTED_LEVEL = "SELECTED_LEVEL";
	
	// default URL to web service, loaded from string resource
	private String url;
	
    private int category_id;
    ScrollView scrollview;
    RelativeLayout layout; 
    private GetWordsetsFromXML wordsetsObject;
    private int marginTop = 0; 
    private LinkedHashMap<Integer, String> wordsetsDescriptions;
    private LinkedHashMap<Integer, String> wordsetsENTitles;
    private LinkedHashMap<Integer, String> wordsetsPLTitles;
    private LinkedHashMap<Integer, String> wordsetsLevels;
    float scale;
    
    @Override
	protected void onCreateDrawerActivity(Bundle savedInstanceState) {

    	 setContentView(R.layout.wordscategories_drawer);
         
         // creating basic, empty layout
         createLayout(); 
         // filling layout with wordsets list 
         loadWordsetsList(); 
         
         Log.w(WordsetsListActivity.class.getName(), "Loading Wordsets List...");
		
	}
    
    @Override 
    public void onResume()
    {
    	super.onResume();
    	
    	ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(getString(R.string.themes));
		actionBar.setSubtitle(null);
		
		getSupportLoaderManager().restartLoader(WORDSETS_LIST_LOADER, null, this);
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }
    
    private void createLayout()
    {
    	scale = getResources().getDisplayMetrics().density;	
    	scrollview = (ScrollView) findViewById(R.id.categoryListScrollView);
        layout = new RelativeLayout(this);
       
        layout.setPadding(10, 10, 10, 10);
        layout.setScrollContainer(true);
        // Drawable bigben = getResources().getDrawable(R.drawable.bigben);
        // layout.setBackgroundDrawable(bigben);
        scrollview.addView(layout);
    }
    
    private void loadWordsetsList() { 
    	
    	// checking if there is network connectivity via WIFI or mobile data 
    	// and checking whether user prefer to load data from web service or stored in locale database
    	if(NetworkUtilities.haveNetworkConnection(this) 
    			&& Preferences.getBoolean(this, Preferences.KEY_PREFER_ONLINE_DATA, false) ) {
    			// loading data (Wordsets List) from online web service
    			loadWordsetsFromWebService(); 
    	} else { 
    		
    		// try load data (Wordsets List) from local data source (SQLiteDatabse)
    		loadWordsetsFromDatabase(); 
    		
    	}
    	
    }
    
    /**
     * This method loads wordsets list from local database (SQLiteDatabase).
     * To load data asynchronously we are using CursorLoader object. 
     * If there are no data in database we will call method to load wordsets list from web service.
     */
    private void loadWordsetsFromDatabase() 
    {
    	getSupportLoaderManager().initLoader(WORDSETS_LIST_LOADER, null, this);
    }
    
    /**
     * Implementation of LoaderManager.LoaderCallbacs<Cursor> interface methods:
     */
    
    // This method is used to create CursorLoader after the initLoader() call
    // @id parameter will be used to create and return different Loaders
    // @args is a Bundle that contains additional arguments for constructing Loaders
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		
		CursorLoader cursorLoader = null; 
		
		switch(id) {
			case WORDSETS_LIST_LOADER: 
				
				// CursorLoader is used to construct the new query 
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
				Uri queryUri = WordsetProvider.CONTENT_URI; 
				
				// determining selection clause to limit loaded wordsets 
				String selectedLevel = getIntent().getExtras().getString(SELECTED_LEVEL);
				if(selectedLevel != null) { 
					// wordsets for given level
				    where = WordsetProvider.WordsetTable.COLUMN_WORDSET_LEVEL + "= ?";
				    whereArgs = new String[] { selectedLevel.toUpperCase(Locale.ENGLISH) };
				    sortOrder = WordsetProvider.WordsetTable.COLUMN_WORDSET_FOREIGN_NAME +  " COLLATE NOCASE ASC";
				     
				} else { 
				    // wordsets for given categoryId
					category_id = getIntent().getIntExtra(WordsetCategoriesActivity.SELECTED_WORDSET_CATEGORY, 0); 
					
					queryUri = Uri.parse(queryUri + "/category/" + category_id); 
					sortOrder = WordsetProvider.WordsetTable.COLUMN_WORDSET_LEVEL + " ASC, "
							  + WordsetProvider.WordsetTable.COLUMN_WORDSET_FOREIGN_NAME + " COLLATE NOCASE ASC"; 
				}
				
				// Create an return the new CursorLoader
				cursorLoader = new CursorLoader(this, queryUri, projection, where, whereArgs, sortOrder); 
				break;
		}
		
		return cursorLoader;
	}
	
	// Method called when new data are loaded from data source 
	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		
		marginTop = 0;
		layout.removeAllViewsInLayout();
		
		// checking whether Cursor contains loaded wordsets
		// if it's size id 0, there are no wordsets saved in database 
		if(cursor.getCount() > 0) {
			
			wordsetsENTitles = new LinkedHashMap<Integer, String>();
			wordsetsPLTitles = new LinkedHashMap<Integer, String>();
			wordsetsLevels = new LinkedHashMap<Integer, String>();
			wordsetsDescriptions = new LinkedHashMap<Integer, String>();
			
			// iterate over the cursor rows. 
			while(cursor.moveToNext())
			{
				int wid = cursor.getInt(
								cursor.getColumnIndexOrThrow(
												WordsetProvider.WordsetTable.COLUMN_WORDSET_ID));
				String foreignName = cursor.getString(
											cursor.getColumnIndexOrThrow(
												WordsetProvider.WordsetTable.COLUMN_WORDSET_FOREIGN_NAME));
				String nativeName = cursor.getString(
											cursor.getColumnIndexOrThrow(
												WordsetProvider.WordsetTable.COLUMN_WORDSET_NATIVE_NAME));
				String level = cursor.getString(
										cursor.getColumnIndexOrThrow(
												WordsetProvider.WordsetTable.COLUMN_WORDSET_LEVEL));
				String description = cursor.getString(
											 	cursor.getColumnIndexOrThrow(
											 			WordsetProvider.WordsetTable.COLUMN_WORDSET_ABOUT));
				
				// adding new wordset details to collections 
				wordsetsENTitles.put(wid, foreignName);
				wordsetsPLTitles.put(wid, nativeName);
				wordsetsLevels.put(wid, level);
				wordsetsDescriptions.put(wid, description );
				
				// creating new button in the list of current wordset
				makeButton(foreignName, nativeName, wid);
       		 	makeLevelButton(level);
       		 	marginTop += (int) ( 80 * scale + 0.5f);
			}
			
		} else { 
			// try to load wordsets list from online web service 
			Log.w(WordsetsListActivity.class.getName(), 
					"Database doesn't contain wordsets, trying to load wordsets from online web service.");
			getSupportLoaderManager().destroyLoader(WORDSETS_LIST_LOADER);
			
			if( NetworkUtilities.haveNetworkConnection(WordsetsListActivity.this)) {
				Toast.makeText(this, R.string.database_does_not_contain_wordsets,
                        Toast.LENGTH_SHORT).show();
				loadWordsetsFromWebService(); 
			} else { 
				// running App in emergency mode using static XML files on disk
				Toast.makeText(this, R.string.database_does_not_contain_wordsets_emergancy_mode,
                        Toast.LENGTH_SHORT).show();
				loadWordsetsFromXMLResources(); 
			}
		}
		
	}
	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {

	}
	
	/**
	 * This method loads wordsets from online web service.
	 */
	private void loadWordsetsFromWebService() {
	
		if( NetworkUtilities.haveNetworkConnection(this) ) { 
		
			String nativeCode = Preferences.getAccountPreferences(this)
					.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, getString(R.string.native_code_lower));
			String foreignCode = Preferences.getAccountPreferences(this)
					.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, getString(R.string.foreign_code_lower)); 
			
			url = getString(R.string.getwordsetslist_url, nativeCode, foreignCode);
			Log.d(WordsetsListActivity.class.getName(), "Wordsets list url: " + url); 
			
			/* deprecated! - limiting wordsets list by category id or level is moved to reloading from database 
			String fullUrl;
			
			if(wordsetsByLevel) { 
	            fullUrl = url + "level=" + WordsetsListActivity.level;
	            WordsetsListActivity.wordsetsByLevel = false; 
			} else { 
	            // old code: this.category_id = WordsetCategoriesActivity.category_id;
	        	category_id = getIntent().getIntExtra(WordsetCategoriesActivity.SELECTED_WORDSET_CATEGORY, 0); 
	        	
	            fullUrl = url + "cid=" + category_id;
			} */
	    
			try  { 
				InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGetOrThrow(url);
				wordsetsObject = new GetWordsetsFromXML(is); 
				try { 
					is.close();
				} catch(java.io.IOException e) { } 
			} catch (Exception e) {
				loadWordsetsFromXMLResources(); return; 
			}
        
			if(wordsetsObject instanceof GetWordsetsFromXML) { 
				Log.w(WordsetsListActivity.class.getName(),
						"Wordsets list loaded from web service, saving it to database and reloading list form database.");
				saveWordsetsInDatabase();
				loadWordsetsFromDatabase();
	            //generateListOfWordsets(); - deprecated! now wordsets are firstly saved in SQLite DB and next are reloaded from it
	        }
		} else { 
			Log.w(WordsetsListActivity.class.getName(),
					"Wordsets hasn't been loaded from web service. Check internet connectivity.");
			Toast.makeText(this, R.string.wordsets_list_internet_lost,
								Toast.LENGTH_SHORT).show();
			// PROMPT TO RECONNECT WITH INTERNET?
			promptTurnOnNetworkToOnlineAccess();
			
		}
	}
	
	/**
	 * Helper method that prompts user to turn on network connection in order to access wordsets online 
	 */
	private void promptTurnOnNetworkToOnlineAccess()
	{
		// create dialog that prompts user if he wants 
		// to access wordsets online in order for him to
		// turn on network connection. 
		AlertDialog dialog = new AlertDialog.Builder(this)
							.setMessage(R.string.no_network_dialog_message)
							.setCancelable(false)
							.setPositiveButton(R.string.connect_button,  new DialogInterface.OnClickListener() {
										
								@Override
								public void onClick(DialogInterface dialog, int which) {
											
									// Connect to Internet 
									WifiManager wifiManager = (WifiManager) WordsetsListActivity.this.getSystemService(Context.WIFI_SERVICE);
									wifiManager.setWifiEnabled(true);
											
									int i = 0; 
									while(!NetworkUtilities.haveNetworkConnection(WordsetsListActivity.this) && i < 10 )
									{
											try {
												Thread.sleep(1000);
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
									}
											
									// If successfully connected with WIFI or mobile network 
									boolean connected = NetworkUtilities.haveNetworkConnection(WordsetsListActivity.this);
									if(connected) {
											loadWordsetsFromWebService(); 
									} else { 
											// When couldn't connect to the internet and load wordsets list from web service, run it in emergency mode
											Log.w(WordsetsListActivity.class.getName(), "Trying to connect to the Internet failed! Emergency Mode.");
											Toast.makeText(WordsetsListActivity.this, R.string.trying_to_connect_failed_emergancy, Toast.LENGTH_SHORT).show();
											loadWordsetsFromXMLResources(); 
									}
											
								}
							})
							.setNegativeButton(R.string.emergancy_mode, new DialogInterface.OnClickListener() {
										
									@Override
									public void onClick(DialogInterface dialog, int which) {
										// load wordsets list in emergancy mode (static xml files)
										loadWordsetsFromXMLResources(); 
									}
							})
							.create();
		
				dialog.show(); 
	}
	
	/**
	 * Helper method used when running App in emergency 
     * mode, when there is no wordsets details in database and 
     * the Internet connection isn't available. 
	 */
	private void loadWordsetsFromXMLResources() 
	{
		// construct name of XML resource stored in raw resources directory
		String xmlName = "wordsetslist";
		
		String selectedLevel = getIntent().getExtras().getString(SELECTED_LEVEL);
		if(selectedLevel != null) { 
            xmlName += selectedLevel.toLowerCase(Locale.ENGLISH);
		} else {     
        	category_id = getIntent().getIntExtra(WordsetCategoriesActivity.SELECTED_WORDSET_CATEGORY, 0); 
            xmlName += category_id;
		}
		
		Log.w(WordsetsListActivity.class.getName(), "Loading wordsets list from xml: " + xmlName + "."); 
   
    	// and get its resource identifier 
    	int xmlResourceId = getResources().getIdentifier(xmlName, "raw", getPackageName());
    	
    	try  { 
    		InputStream is = getResources().openRawResource(xmlResourceId);
    		wordsetsObject = new GetWordsetsFromXML(is); 
    		try { 
    			is.close();
    		} catch(java.io.IOException e) { } 
    	} catch (Exception e) { }
         
    	if(wordsetsObject instanceof GetWordsetsFromXML) { 
			Log.w(WordsetsListActivity.class.getName(),
					"Wordsets list loaded from XML resources.");
			// saveWordsetsInDatabase();
			// loadWordsetsFromDatabase();
            generateListOfWordsets();
        }
	}
	
	/**
	 * This method is used to store wordsets loaded from web service in local database
	 * to enable off-line access to it while user will be doing future requests.
	 * All database operations are done using ContentProviders.
	 */
	private void saveWordsetsInDatabase()
	{
		// 1) getting data loaded from web service
		HashMap<Integer,String> foreignName = wordsetsObject.getWordsetsENTitles();
		HashMap<Integer,String> nativeName = wordsetsObject.getWordsetsPLTitles();
		HashMap<Integer,String> level = wordsetsObject.getWordsetsLevels();
		HashMap<Integer, String> description = wordsetsObject.getWordsetsDescriptions();
		HashMap<Integer, String> categoryId = wordsetsObject.getWordsetsCategoryIds();
		
		if(!foreignName.isEmpty()) { 
			
			// 2) deleting old data (wordset entries) from database
			/* int deletedCount = */ getContentResolver().delete(WordsetProvider.CONTENT_URI, null, null);
			
			// 3) inserting new data (wordset entries) into database
			for(int wordsetId : foreignName.keySet()) {
				
				// creating new row of values to insert. 
				ContentValues newValues = new ContentValues(); 
				
				// assign values for each row.
				newValues.put(WordsetProvider.WordsetTable.COLUMN_WORDSET_ID, wordsetId);
				newValues.put(WordsetProvider.WordsetTable.COLUMN_CATEGORY_ID, Integer.valueOf(categoryId.get(wordsetId)));
				newValues.put(WordsetProvider.WordsetTable.COLUMN_WORDSET_FOREIGN_NAME, foreignName.get(wordsetId));
				newValues.put(WordsetProvider.WordsetTable.COLUMN_WORDSET_NATIVE_NAME, nativeName.get(wordsetId));
				newValues.put(WordsetProvider.WordsetTable.COLUMN_WORDSET_LEVEL, level.get(wordsetId));
				newValues.put(WordsetProvider.WordsetTable.COLUMN_WORDSET_ABOUT, description.get(wordsetId));
				// by default insert that audio recording hasn't been downloaded yet and aren't stored on disk
				// this will be changed only when the user synchronizes wordset words with preference to download 
				// audio recordings set to true in application preferences
				newValues.put(WordsetProvider.WordsetTable.COLUMN_IS_AUDIO_STORED_LOCALLY, 0);
				
				Log.w(WordsetsListActivity.class.getName(), 
						"INSERTING: " + wordsetId + " " + foreignName.get(wordsetId) + " - " + nativeName.get(wordsetId) ) ;
				
				// get the Content Resolver and insert new row (wordset entry) into database
				getContentResolver().insert(WordsetProvider.CONTENT_URI, newValues);
			}
		} else { 
			Log.w(WordsetsListActivity.class.getName(), "Loaded from web service wordsets list is empty!");
		}
	}
	
	/**
	 * This method is used to load wordsets form raw XML data source 
	 */
	@SuppressWarnings("unused")
	private void loadWordsetsFromRawXML() {
		
		 String fullUrl; 
		    
		 String selectedLevel = getIntent().getExtras().getString(SELECTED_LEVEL);
		 if(selectedLevel != null) {          
		    	fullUrl = "wordsetslist" + selectedLevel.toLowerCase(Locale.ENGLISH);
		    	
		 } else { 
		        // category_id = WordsetCategoriesActivity.category_id;
		    	category_id = getIntent().getIntExtra(WordsetCategoriesActivity.SELECTED_WORDSET_CATEGORY, 0); 
		        	
		        fullUrl = "wordsetslist" + category_id;
		 }
		 
		 System.out.println(fullUrl);
		 
		 try  { 
           
        	Resources res = getResources();
        	 
        	int wordsetsList = res.getIdentifier(fullUrl, "raw", getPackageName());
        	InputStream is = this.getResources().openRawResource(wordsetsList);
        	
            wordsetsObject = new GetWordsetsFromXML(is); 
            try { 
            	is.close();
            } catch(java.io.IOException e) { } 
            
        } catch (Exception e) { }
        
        if(wordsetsObject instanceof GetWordsetsFromXML) { 
            generateListOfWordsets();
        }
	}
    
     private void generateListOfWordsets() { 
        
          wordsetsENTitles = wordsetsObject.getWordsetsENTitles();
          wordsetsPLTitles = wordsetsObject.getWordsetsPLTitles();
          wordsetsLevels = wordsetsObject.getWordsetsLevels();
          wordsetsDescriptions = wordsetsObject.getWordsetsDescriptions();
          
         if(wordsetsENTitles instanceof LinkedHashMap  ) {
        	 
        	 for(int wid : wordsetsENTitles.keySet() ) {
        		 makeButton(wordsetsENTitles.get(wid), wordsetsPLTitles.get(wid), wid);
        		 makeLevelButton(wordsetsLevels.get(wid));
        		 marginTop += (int) ( 80 * scale + 0.5f);
        	 }
        	
         }        
    }
    
     @SuppressWarnings("deprecation")
	private void makeButton(String ENTitle, String PLTitle, int wid) { 
        
       
        int pxHeight = (int) ( 80 * scale + 0.5f);
        int pxOffset = (int) ( 20 * scale + 0.5f);
        int pxPadding = (int) (10 * scale + 0.5f);
        int pxMarginLeft = (int) (85 * scale + 0.5f);
        int pxMarginRight = (int) (10* scale + 0.5f);
        Button button = new Button(this);
       
        button.setText(
                Html.fromHtml("<font color='#AB1E35'>"+ ENTitle + "</font><br/>"
                + "<small>"+ PLTitle +"</small>"));
       
        Resources resources = this.getResources();
        //Drawable drawable = resources.getDrawable(R.drawable.buttonColor);
        Drawable drawable = resources.getDrawable(R.drawable.button_shape);
        button.setBackgroundDrawable(drawable);
        
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = displaymetrics.widthPixels;
        
        button.setWidth(width-pxOffset);
        button.setHeight(pxHeight);
        button.setGravity(Gravity.LEFT);
        button.setPadding(0,pxPadding,0,0);
        
        
        button.setTextSize(16);
        button.setTextColor(Color.BLACK);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                  RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(pxMarginLeft, marginTop, pxMarginRight, 0);
        
        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);  
        buttonParams.addRule(RelativeLayout.CENTER_IN_PARENT);
       // button.setLayoutParams(buttonParams);
        
        button.setTag(wid);
        button.setOnClickListener(this);
        layout.addView(button, layoutParams);
        
     }
     
      @SuppressWarnings("deprecation")
	private void makeLevelButton(String level) { 
        
        Button button = new Button(this);
        int pxHeight = (int) ( 80 * scale + 0.5f);
        int pxWidth = (int) ( 70 * scale + 0.5f);
        int pxPadding = (int) (5 * scale + 0.5f);
        int pxMarginLeft = (int) (10 * scale + 0.5f);
        
        button.setText(
                Html.fromHtml("<font color='#AB1E35'>"+ level + "</font>"));
       
        Resources resources = this.getResources();
        //Drawable drawable = resources.getDrawable(R.drawable.buttonColor);
        Drawable drawable = resources.getDrawable(R.drawable.button_shape);
        button.setBackgroundDrawable(drawable);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        // int width = displaymetrics.widthPixels;
        button.setWidth(pxWidth);
        button.setHeight(pxHeight);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0,pxPadding,0,0);
        
        
        button.setTextSize(25);
        button.setTextColor(Color.BLACK);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                  RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = pxMarginLeft;
        layoutParams.topMargin = marginTop;
        
        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);  
        buttonParams.addRule(RelativeLayout.CENTER_IN_PARENT);
       // button.setLayoutParams(buttonParams);
        
        button.setTag(level);
        button.setOnClickListener(this);
        layout.addView(button, layoutParams);
        
     }
    
      @Override
    public void onClick(View view) {  
         try { 
          int wid = (Integer) view.getTag(); 
          selectWordset(wid);
         } catch(ClassCastException e) { 
            try { 
             String lvl = (String) view.getTag(); 
             Log.w(WordsetsListActivity.class.getName(), "Selected wordsets level: " + lvl + "."); 
             selectWordsetListByLevel(lvl);
            } catch(ClassCastException e2) { 
                Toast.makeText(this, R.string.unexpected_error,
                                Toast.LENGTH_SHORT).show();
            }
         }
    }
   
   /**
    * Helper method that is called by onClick event handler when user 
    * select given wordset on list. This method is starting new Activity 
    * using explicit Intent. It puts into this Intent wordsetId of wordset to show.  
    */
   private  void selectWordset(int wordsetId) { 
    	  
    	  Log.w(WordsetsListActivity.class.getName(), "Select wordset with id: " + wordsetId);
    	  
    	  // creating new explicit Intent and putting wordsetId into it. 
    	  Intent wordsetIntent = new Intent(WordsetsListActivity.this, WordsetActivity.class); 
    	  wordsetIntent.putExtra(SELECTED_WORDSET, wordsetId);
    	  wordsetIntent.putExtra(WordsetActivity.KEY_WORDSET_FOREIGN_NAME, wordsetsENTitles.get(wordsetId));
    	  wordsetIntent.putExtra(WordsetActivity.KEY_WORDSET_NATIVE_NAME, wordsetsPLTitles.get(wordsetId));
    	  wordsetIntent.putExtra(WordsetActivity.KEY_WORDSET_LEVEL, wordsetsLevels.get(wordsetId));
    	  wordsetIntent.putExtra(WordsetActivity.KEY_WORDSET_DESCRIPTION, wordsetsDescriptions.get(wordsetId));
    	  
    	  startActivity(wordsetIntent);
      }
   
      public void selectWordsetListByLevel(String lvl) { 
         
    	  Intent levelIntent = new Intent(this, WordsetsListActivity.class); 
    	  levelIntent.putExtra(SELECTED_LEVEL, lvl); 
    	  
          startActivity(levelIntent);
      }

	@Override
	protected int getRightDrawerMenuButtonId() {
		
		return R.id.taskNotificationsBtn;
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
		return super.onOptionsItemSelected(item);
    }
}
