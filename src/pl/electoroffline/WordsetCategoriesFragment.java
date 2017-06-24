/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.InputStream;
import java.util.HashMap;

import pl.electoroffline.R;
import pl.elector.database.WordsetCategoryProvider;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

/**
 * @author Michał Ziobro 
 */
public class WordsetCategoriesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {
	
    private static final int WORDSET_CATEGORIES_LOADER = 0x01;
    
	private GetCategoriesFromXML categoryObject; 
    private String url;
    private ScrollView scrollview;
    private LinearLayout layout;
    //public static int category_id;
    float scale;
    
    public static final String TAG = "WORDSET_CATEGORIES_INFO_FRAGMENT_TAG";
	
	private View view; 
	
	@Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{		
		view = inflater.inflate(R.layout.wordscategories, container, false); 
		
        // creating basic, empty layout 
        createLayout();
        // filling layout with wordset categories
        loadWordsetCategories();  
        
        return view;
		
	}
    
    private void createLayout() 
    {
    	scale = getResources().getDisplayMetrics().density;
        scrollview = (ScrollView) view.findViewById(R.id.categoryListScrollView);
        layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pxPadding = (int) (10*scale +0.5f);
        layout.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);
        layout.setScrollContainer(true);
        scrollview.addView(layout);
       
    	setHasOptionsMenu(true);
    	
    }
    
    @Override
	public void onResume() 
	{
		super.onResume();
		onFragmentResume();	
	}
	
	private void onFragmentResume() {
		ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
		actionBar.setTitle(getString(R.string.categories));
		actionBar.setSubtitle(null);
		
		getActivity().getSupportLoaderManager().restartLoader(WORDSET_CATEGORIES_LOADER, null, this);
	}
    
    private void loadWordsetCategories() {
    	
    	// loading from XML: InputStream is = this.getResources().openRawResource(R.raw.get_categories);
    	
    	// checking if there is network connectivity via WIFI or mobile data 
    	// and checking whether user prefer to load data from web service or stored in local database
    	if(NetworkUtilities.haveNetworkConnection(getActivity())
    			&& Preferences.getBoolean(getActivity(), Preferences.KEY_PREFER_ONLINE_DATA, false) ) {
    			// loading data (WordsetCategories) from online web service 
    			loadWordsetCategoriesFromWebService(); 
    	} else {
    		
    		// try load data (WordsetCategories) from local data source (SQLiteDatabase)
    		loadWordsetCategoriesFromDatabase(); 
    	}
    	
    }
    /**
     * This method loads wordset categories from local database (SQLiteDatabase).
     * To load data asynchronously we are using CursorLoader object. 
     * If there are no data in database we will call method to load categories from web service.
     */
    private void loadWordsetCategoriesFromDatabase()
    {
    	getActivity().getSupportLoaderManager().initLoader(WORDSET_CATEGORIES_LOADER, null, this);
    }
    
    /**
     * Implementation of LoaderManager.LoaderCallbacks<Cursor> interface methods:
     */
    
    // This method is used to create CursorLoader after the initLoader() call 
    // @id parameter will be used to create and return different Loaders
    // @args is a Bundle that contains additional arguments for constructing Loaders
    @Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    	
    	CursorLoader cursorLoader = null; 
    	
		switch(id) {
			case WORDSET_CATEGORIES_LOADER: 
			
				// CursorLoader is used to construct the new query 
				String[] projection = { WordsetCategoryProvider.WordsetCategoryTable.COLUMN_ID, 
	    							WordsetCategoryProvider.WordsetCategoryTable.COLUMN_CATEGORY_FOREIGN_NAME,
	    							WordsetCategoryProvider.WordsetCategoryTable.COLUMN_CATEGORY_NATIVE_NAME
	    						   };
				String where = null; 
				String[] whereArgs = null; 
				String sortOrder = null; 
	    	
				// Query Content URI for retrieving all category rows
				Uri queryUri = WordsetCategoryProvider.CONTENT_URI;
	    	
				// Create and return the new CursorLoader
				cursorLoader = new CursorLoader(getActivity(), queryUri, projection, where, whereArgs, sortOrder); 
				break; 
			
		}
    	
		return cursorLoader;
	}

    // Method called when new data are loaded from data source 
	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		
		Log.w(WordsetCategoriesFragment.class.getName(), "onLoadFinished()");
		layout.removeAllViewsInLayout();
		
		// checking whether Cursor contains loaded categories 
		// if it's size is 0, there are no categories saved in database 
		if(cursor.getCount() > 0) {
			
			Log.w(WordsetCategoriesFragment.class.getName(), "Cursor object count: " + cursor.getCount());
			
			// iterate over the cursor rows. 
			while(cursor.moveToNext())
			{
				int cid = cursor.getInt( 
									cursor.getColumnIndexOrThrow(
												WordsetCategoryProvider.WordsetCategoryTable.COLUMN_ID ) );
				String foreignName = cursor.getString( 
										cursor.getColumnIndexOrThrow(
												WordsetCategoryProvider.WordsetCategoryTable.COLUMN_CATEGORY_FOREIGN_NAME ) );
				String nativeName = cursor.getString(
										cursor.getColumnIndexOrThrow(
												WordsetCategoryProvider.WordsetCategoryTable.COLUMN_CATEGORY_NATIVE_NAME ) );
				
				// creating new button in the list for current worset category
				makeButton(foreignName, nativeName, cid);
				
			}
			
		} else { 
			// try to load wordset categories from online web service
			Log.w(WordsetCategoriesFragment.class.getName(), 
					"Database doesn't contain wordset categories, trying to load wordset categories from online web service.");
			getActivity().getSupportLoaderManager().destroyLoader(WORDSET_CATEGORIES_LOADER);
			
			if( NetworkUtilities.haveNetworkConnection(getActivity())) {
				Toast.makeText(getActivity(), R.string.database_does_not_contain_categories, Toast.LENGTH_SHORT).show();
				loadWordsetCategoriesFromWebService();
			} else { 
				// running app in emergency mode using static XML files on disk
				Toast.makeText(getActivity(), R.string.database_does_not_contain_categories_emergancy_mode,
                        Toast.LENGTH_SHORT).show();
				loadWordsetCategoriesFromXMLResources(); 
			}
		}
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {
		
	} 
	
    /**
     * This method loads wordset categories from online web service.
     * Method calls function to generate list of wordset categories on screen.
     */
    private void loadWordsetCategoriesFromWebService()
    {
    	if( NetworkUtilities.haveNetworkConnection(getActivity())) { 
    	
    		String nativeCode = Preferences.getAccountPreferences(getActivity())
    				.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, getString(R.string.native_code_lower));
    		String foreignCode = Preferences.getAccountPreferences(getActivity())
    				.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, getString(R.string.foreign_code_lower));
    		
    		url = getString(R.string.getcategories_url, nativeCode, foreignCode);
    		Log.d(WordsetCategoriesFragment.class.getName(), "Get categories url: " + url);
    		
    		try  { 
    			InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGetOrThrow(url);
    			categoryObject = new GetCategoriesFromXML(is); 
    			try { 
    				is.close();
    			} catch(java.io.IOException e) {  } 
    		} catch (Exception e) { 
    			loadWordsetCategoriesFromXMLResources(); return; 
    		}
         
    		if(categoryObject instanceof GetCategoriesFromXML) { 
    			Log.w(WordsetCategoriesFragment.class.getName(),
    					"Categories loaded from web service, generating list of category buttons.");
    			generateListOfCategories();
    			saveWordsetCategoriesInDatabase(); 
    		}
    	} else { 
        	 Log.w(WordsetCategoriesFragment.class.getName(),
        			 "Categories hasn't been loaded from web service. Check internet connectivity.");
             Toast.makeText(getActivity(), R.string.category_internet_lost,
                                 Toast.LENGTH_SHORT).show();
             // PROMPT TO RECONNECT WITH INTERNET?
             promptTurnOnNetworkToOnlineAccess(); 
        }
    }
    
    /**
	 * Helper method that prompts user to turn on network connection in order to access wordset categories online 
	 */
	private void promptTurnOnNetworkToOnlineAccess()
	{
		// create dialog that prompts user if he wants 
		// to access wordset categories online in order for him to
		// turn on network connection. 
		AlertDialog dialog = new AlertDialog.Builder(getActivity())
							.setMessage(R.string.no_network_dialog_message)
							.setCancelable(false)
							.setPositiveButton(R.string.connect_button,  new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									
									// Connect to Internet 
									WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
									wifiManager.setWifiEnabled(true);
									
									int i = 0; 
									while(!NetworkUtilities.haveNetworkConnection(getActivity()) && i < 10 )
									{
										try {
											Thread.sleep(1000);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}
									
									// If successfully connected with WIFI or mobile network 
									boolean connected = NetworkUtilities.haveNetworkConnection(getActivity());
									if(connected) {
										loadWordsetCategoriesFromWebService(); 
									} else { 
										// When couldn't connect to the Internet and load wordset categories from web service, run it in emergency mode
										Log.w(WordsetCategoriesFragment.class.getName(), "Trying to connect to the Internet failed! Emergency Mode.");
										Toast.makeText(getActivity(), getString(R.string.trying_to_connect_failed_emergancy), Toast.LENGTH_SHORT).show();
										loadWordsetCategoriesFromXMLResources(); 
									}
									
								}
							})
							.setNegativeButton(R.string.emergancy_mode, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// load wordsets categories in emergency mode (static xml files)
									loadWordsetCategoriesFromXMLResources(); 
								}
							})
							.create();
		dialog.show(); 
	}
	
    
    /**
     * Helper method used when running App in emergency 
     * mode, when there is no words in database and 
     * the Internet connection isn't available. 
     */
    private void loadWordsetCategoriesFromXMLResources()
    {
    	// construct name of XML resource stored in raw resources directory
    	String xmlName = "get_categories";
    	// and get its resource identifier 
    	int xmlResourceId = getResources().getIdentifier(xmlName, "raw", getActivity().getPackageName());
    		
    	try  { 
    		InputStream is = getResources().openRawResource(xmlResourceId);
    		categoryObject = new GetCategoriesFromXML(is); 
    		try { 
    			is.close();
    		} catch(java.io.IOException e) { } 
    	} catch (Exception e) { }
         
    	if(categoryObject instanceof GetCategoriesFromXML) { 
    		Log.w(WordsetCategoriesFragment.class.getName(),
    				"Categories loaded from XML resource, generating list of category buttons.");
    		generateListOfCategories();
    		// saveWordsetCategoriesInDatabase(); 
    	}
    }
    
    /**
     * This method is used to store categories loaded from web service in local database 
     * to enable off-line access to it while user will be doing future requests.
     * All database operations are done using ContenProviders
     */
    private void saveWordsetCategoriesInDatabase()
    {
    	// 1) getting data loaded from web service
    	HashMap<Integer, String> foreignName = categoryObject.getCategoryENTitles(); 
    	HashMap<Integer, String> nativeName = categoryObject.getCategoryPLTitles(); 
    	
    	if(!foreignName.isEmpty()) { 
    		// 2) deleting old data (wordset categories) from database
    		/* int deletedCount = */ getActivity().getContentResolver().delete(WordsetCategoryProvider.CONTENT_URI, null, null); 
    		
    		// 3) inserting new data (wordset categories) into database 
    		for(int categoryId : foreignName.keySet() ) {
    			
    				// creating new row of values to insert. 
    				ContentValues newValues = new ContentValues(); 
    				
    				// assign values for each row. 
    				newValues.put(WordsetCategoryProvider.WordsetCategoryTable.COLUMN_ID, categoryId);
    				newValues.put(WordsetCategoryProvider.WordsetCategoryTable.COLUMN_CATEGORY_FOREIGN_NAME, foreignName.get(categoryId));
    				newValues.put(WordsetCategoryProvider.WordsetCategoryTable.COLUMN_CATEGORY_NATIVE_NAME, nativeName.get(categoryId)); 
    				
    				// get the Content Resolver and insert new row (wordset category) into database 
    				getActivity().getContentResolver().insert(WordsetCategoryProvider.CONTENT_URI, newValues);
    		}
    		
    	}
    	
    }
    
    private void generateListOfCategories() { 
 
         HashMap<Integer, String> categoryENTitle = categoryObject.getCategoryENTitles();
         HashMap<Integer, String> categoryPLTitle = categoryObject.getCategoryPLTitles();
         if(categoryENTitle instanceof HashMap  ) {
         for(int cid : categoryENTitle.keySet() ) {
            makeButton(categoryENTitle.get(cid), categoryPLTitle.get(cid), cid);
          }
         }
        
        
    }
    
     @SuppressWarnings("deprecation")
	private void makeButton(String ENTitle, String PLTitle, int cid) { 
        
    	Log.w(WordsetCategoriesActivity.class.getName(), "Making category button: " + ENTitle); 
    	 
        Button button = new Button(getActivity());
        int pxHeight = (int) (80*scale + 0.5f);
        int pxOffset = (int) (80*scale + 0.5f); 
        int pxPadding = (int) (10*scale + 0.5f);
        button.setText(
                Html.fromHtml("<font color='#AB1E35'>"+ ENTitle + "</font><br/>"
                + "<small>"+ PLTitle +"</small>"));
       
        Resources resources = this.getResources();
        //Drawable drawable = resources.getDrawable(R.drawable.buttonColor);
        Drawable drawable = resources.getDrawable(R.drawable.button_shape);
        button.setBackgroundDrawable(drawable);
        
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = displaymetrics.widthPixels;
       
        button.setWidth(width-pxOffset);
        button.setHeight(pxHeight);
        button.setGravity(Gravity.LEFT);
        button.setPadding(0,pxPadding,0,0);
        button.setId(cid);
       
        
        button.setTextSize(16);
        button.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(pxPadding, 0, pxPadding, 0);
        button.setTag(cid);
        button.setOnClickListener(this);
        
        layout.addView(button, layoutParams);
       
        
        
     }
    @Override
    public void onClick(View view) {  
        
        selectCategory(view.getTag());
    }
    
    private void selectCategory(Object cid) { 
       int intCid = (Integer) cid;
      // Button button = (Button) findViewById(intCid);
      // button.setBackgroundColor(Color.GREEN);
       
      //WordsetCategoriesFragment.category_id = intCid;
      
      Intent wordsetsIntent = new Intent(getActivity(), WordsetsListActivity.class);
      wordsetsIntent.putExtra(WordsetCategoriesActivity.SELECTED_WORDSET_CATEGORY, intCid);
      
      startActivity(wordsetsIntent);
    }
    
      
     // Initiating Menu XML file (menu.xml)
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
    	inflater.inflate(R.menu.main_menu, menu);
    	
    	// Find the actionbar's menuItem to add overflow menu sub_menu 
        MenuItem overflowMenuItem = menu.findItem(R.id.menu_overflow);
        // Inflating the sub_menu menu this way, will add its menu items 
        // to the empty SubMenu you created in the XML
        inflater.inflate(R.menu.wordcategories_menu_content, overflowMenuItem.getSubMenu()); 
        
        super.onCreateOptionsMenu(menu, inflater);
    }
 
    /**
     * Event Handling for Individual menu item selected
     * Identify single menu item by it's id
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.selectByLevelMenuBtn:
            startActivity(new Intent(getActivity(), SelectByLevelActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
