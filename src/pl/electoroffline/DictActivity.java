/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import info.semsamot.actionbarrtlizer.ActionBarRtlizer;
import info.semsamot.actionbarrtlizer.RtlizeEverything;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;


/**
 *
 * @author Michał Ziobro 
 */
public class DictActivity extends DrawerActivity implements View.OnClickListener {
	
	public static final String TAG = "DICT_FRAGMENT_TAG"; 
	
	private ActionBarDrawerToggle drawerToggle;
	private ListView drawerList; 
	DrawerLayout drawerLayout; 
	
    //String[] suggestions = new String[] {}; 
    private AutoCompleteTextView actvDev;
    private ArrayAdapter<String> adapter;
    private Handler handler;
    private String lang; 
    private  ArrayList<String> currentTIDS; 
    private ArrayList<String> suggestionsResult;
    
    private ImageView searchBtn; 
    private HashMap<Integer, WordObject> wordsDetails; 
   
    private int currWid;
    private float scale; 
    
    // preventing auto completion when search button clicked
    private Object lock = new Object();
    private Timer t = new Timer();
    private DictSearchTask searchTask; 
    
    private DictResultListFragment resultListFragment; 
    private DictWordDetailsFragment wordDetailsFragment;
    
    @Override
	protected void onCreateDrawerActivity(Bundle savedInstanceState) {
		
    	setContentView(R.layout.dict_drawer);
    	createLayout();
        attachListeners();
        
        suggestionsResult = new ArrayList<String>();
        currentTIDS = new ArrayList<String>();
       
        setUpSearchAutoComplete();
 
        addDictResultListFragment();
    	
	}
    
    /*
    // Called when the activity is first created. 
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        setContentView(R.layout.dict_drawer);
        
        createLayout();
        attachListeners();
        configureLeftDrawer();
        
        suggestionsResult = new ArrayList<String>();
        currentTIDS = new ArrayList<String>();
       
        setUpSearchAutoComplete();
       
        addDictResultListFragment();
    }
    */
    
    private void setUpSearchAutoComplete() {
        	adapter = 
                new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line);
      
      
              actvDev = (AutoCompleteTextView) findViewById(R.id.dictSuggestions);
              actvDev.setThreshold(1);
              actvDev.setAdapter(adapter);
              actvDev.addTextChangedListener(new TextWatcher() {
                  
                  private Editable editable; 
                  private boolean shouldAutoComplete = true; 
                  TimerTask scanTask;
                  Handler handler = new Handler();
                   
                  @Override
                  public void onTextChanged(CharSequence s, int start, int before, int count) {
                       
                       shouldAutoComplete = true;
                       for (int position = 0; position < adapter.getCount(); position++) {
                          if (adapter.getItem(position).equalsIgnoreCase(s.toString())) {
                              shouldAutoComplete = false;
                              break;
                          }
                       }

                   }
                  @Override
                  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                  }

                  @Override
                  public void afterTextChanged(Editable s) {
                      editable = s; 
                      if (shouldAutoComplete) {
                          scanTask = new TimerTask() {
                                      public void run() {
                                          handler.post(new Runnable() {
                                                   public void run() {
                                                           retrieveSuggestions(editable.toString());
                                                          }
                                              });
                                          }};
                          t.cancel();
                          t.purge();
                          t = new Timer(); 
                          t.schedule(scanTask, 1500); 
                          if(searchTask != null && searchTask.getStatus() == AsyncTask.Status.RUNNING) {
                        	  searchTask.cancel(true); 
                          }
                      }
                  }
              });
              
              actvDev.setOnEditorActionListener(new OnEditorActionListener() {
                  @Override
                  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    	  searchBtnClick(v);	
                          return true;
                      }
                      return false;
                  }
              });

    }
    
    /**
     * Helper method that adds DictResultListFragment to fragment container 
     */
    private void addDictResultListFragment() {
    		
    		FragmentManager fragmentManager =  getSupportFragmentManager(); 
    		resultListFragment = (DictResultListFragment) fragmentManager.findFragmentByTag(DictResultListFragment.TAG);
    		
    		if(resultListFragment == null) { 
	    	 	FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		        resultListFragment = new DictResultListFragment();
		        fragmentTransaction.replace(R.id.dict_content_frame, resultListFragment, DictResultListFragment.TAG); 
		        //fragmentTransaction.addToBackStack("dictResultListFragmentBack"); 
		        fragmentTransaction.commit();
    		}
    }
    
    @Override 
    public void onResume() 
    {
    	super.onResume();
    	
    }
    
    private void createLayout() {
    	scale = this.getResources().getDisplayMetrics().density;
    	 
    	// ActionBar modification 
   		ActionBar actionBar = getSupportActionBar();
   		actionBar.setTitle(getString(R.string.dictionary));
   		actionBar.setSubtitle(null);
   		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP 
   								| ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
    }
    
    /*
    private void configureLeftDrawer() 
    {
    	drawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout); 
    	drawerList = (ListView) findViewById(R.id.main_left_drawer); 
    	//drawerList.setAdapter(); 
    	//drawerList.setOnItemClickListener(listener)
    	drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, 
    											R.drawable.ic_drawer, 
    											R.string.main_drawer_open,
    											R.string.main_drawer_close) {
    		
    		// Called when a drawer has settled in a completely closed state. 
    		public void onDrawerClosed(View view) {
    			
    		}
    		
    		// Called when a drawer has settled in a completely open state. 
    		public void onDrawerOpen(View drawerView) {
    			
    		}
    	};
    	
    	// set the drawer toggle as the Drawer Listener 
    	drawerLayout.setDrawerListener(drawerToggle);
    	
    	getSupportActionBar().setDisplayHomeAsUpEnabled(true); 
    	getSupportActionBar().setHomeButtonEnabled(true); 
    }
    
    public class DrawerItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// TO DO: Drawer Item click listener 
		} 
    	
    }
    
    @Override 
    protected void onPostCreate(Bundle savedInstanceState) {
    	super.onPostCreate(savedInstanceState);
    	// Sync the toggle state after onRestoreInstanceState has occurred.
    	drawerToggle.syncState();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
    	super.onConfigurationChanged(newConfig);
    	drawerToggle.onConfigurationChanged(newConfig);
    }*/
    
    @Override 
	public boolean onCreateOptionsMenu(Menu menu) 
	{
    	MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.dict_menu, menu);
		
		String foreignCode = Preferences.getAccountPreferences(this)
				  	.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, getString(R.string.foreign_code_lower)); 
		String nativeCode = Preferences.getAccountPreferences(this)
					.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, getString(R.string.foreign_code_lower)); 
	    menu.findItem(R.id.dictForeignBtn).setTitle(LanguageNameMapping.getResourceId(foreignCode)); 
		menu.findItem(R.id.dictNativeBtn).setTitle(LanguageNameMapping.getResourceId(nativeCode)); 
		
		return super.onCreateOptionsMenu(menu);
	}
    
    private MenuItem foreignItem; 
    private MenuItem nativeItem; 
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
    	// pass the event to ActionBarDrawerToggle, 
    	// if it returns true, then it has handled 
    	// the app icon touch event. 
    	if(drawerToggle.onOptionsItemSelected(item)) {
    		return true; 
    	}
    	*/
    	
		switch(item.getItemId())
		{
			case R.id.dictForeignBtn: {
				if(isRTL()) setForeignLangRTL(item);
				else setForeignLang(item); 
				return true; 
			}
			case R.id.dictNativeBtn: {
				if(isRTL()) setNativeLangRTL(item);
				else setNativeLang(item); 
				return true; 
			}
			default: 
				break;
		}
		
		return super.onOptionsItemSelected(item);
	}
    
    private void attachListeners() {
	    findViewById(R.id.searchWordBtn).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				searchBtnClick(v);	
			}
		});
	 
    }
    
    public void searchBtnClick(View v) { 
         
    	t.cancel();
        t.purge();
    	searchTask = (DictSearchTask) new DictSearchTask().execute();
     
    	// hiding soft keyboard
    	InputMethodManager imm = (InputMethodManager)getSystemService(
    	      Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(actvDev.getWindowToken(), 0);
        
    }
    
    private class DictSearchTask extends AsyncTask<Void, Integer, Boolean> {
    	
    	private boolean cancelled = false; 

		@SuppressLint("UseSparseArrays")
		@Override
		protected Boolean doInBackground(Void... params) {
			
				wordsDetails = new HashMap<Integer, WordObject>(); 
				
			 	String keyword = actvDev.getText().toString().trim(); 
		        int idx = suggestionsResult.indexOf(keyword);
		        if(idx == -1) { 
		            //the result was not found in suggestions 
		        	Log.w(DictSearchTask.class.getName(), "Keyword not found in suggestions arraylist!"); 
		            retrieveWordsFromService(keyword);
		            idx = suggestionsResult.indexOf(keyword);
		        }
		       
		        Log.w(DictSearchTask.class.getName(), "Checking whether keyword exists again in DictSearchTask.");
		        if(idx > -1 && idx < currentTIDS.size()) { 
		            String translationIds = currentTIDS.get(idx);
		            String[] tids = translationIds.split(",");
		            for(String strTid : tids) { 
		            	if(cancelled) return false; 
		                int tid = Integer.parseInt(strTid); 
		                getWordDetails(tid); 
		            }
		            return true;
		            
		        } else { 
		        	
		        	Log.w(DictActivity.class.getName(), "keyword index in DictSearchTask: " + idx); 
		        	runOnUiThread(new Runnable () {

						@Override
						public void run() {
							Toast.makeText(DictActivity.this,
				                       getString(R.string.dict_not_found), Toast.LENGTH_LONG).show();
							
						} 
		        	});
		            
		        	if(!cancelled && currentTIDS.size() > 0) {
			            for(String translationIds : currentTIDS) {
			            	if(cancelled) return false; 
			                String[] tids = translationIds.split(",");
			                for(String strTid : tids) { 
			                    int tid = Integer.parseInt(strTid); 
			                    getWordDetails(tid); 
			                }
			            }
			            return true; 
		        	}
		           
		            return false;
		        }
		}
		
		 private void getWordDetails(int tid) { 
		        WordObject wo = new WordObject(tid, getResources().getString(R.string.native_code_lower), getResources().getString(R.string.foreign_code_lower), DictActivity.this); 
		        wordsDetails.put(tid, wo); 
		 }
		 
		 @Override
		 protected void onCancelled() {
		        cancelled = true;
		        Log.w(DictSearchTask.class.getName(), "Search Task cancelled!");
		 }
		 @Override
		 protected void onProgressUpdate(Integer... progress) {
			 
	     }
		 
		 @Override
	     protected void onPostExecute(Boolean result) {
			
	    	 if(result) { 
	    		 
	    		FragmentManager fragmentManager = getSupportFragmentManager(); 
	    		wordDetailsFragment = (DictWordDetailsFragment) fragmentManager.findFragmentByTag(DictWordDetailsFragment.TAG);

	    		if(wordDetailsFragment != null) { 
	    			fragmentManager.beginTransaction()
	    				.remove(wordDetailsFragment).show(resultListFragment).commit();
	    			wordDetailsFragment = null; 
	    			fragmentManager.popBackStack();
	    			
	    		}
				resultListFragment.displayWordsInScrollView(wordsDetails);
	    	 } else { 
	    		Toast.makeText(DictActivity.this, getString(R.string.error_while_searching_word), Toast.LENGTH_SHORT).show();
	    	 }
	    }
    	
    }
    
    /*
    private void displayWordsInScrollView() { 
       
       //dictResultList.setPadding(10, 10, 10, 10);
       // dictResultList.setScrollContainer(true);
       // scrollview.addView(dictResultList);
        Toast.makeText(DictActivity.this,
	      "displaying results...", Toast.LENGTH_LONG).show();
       // wordDetailsView.setVisibility(View.GONE);
       // scrollview.setVisibility(View.VISIBLE); 
    } */
    
    
    private void retrieveWordsFromService(String word) { 
      synchronized(lock) { 
         suggestionsResult.clear();
         currentTIDS.clear(); 
         GetLookupWordsFromXML lookupObj; 
         if(word.length() != 0 ) { 
        	
        	String foreignCode = Preferences.getAccountPreferences(this)
											.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, 
													   getResources().getString(R.string.foreign_code_lower));
        	String nativeCode = Preferences.getAccountPreferences(this)
        									.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE,
        											   getResources().getString(R.string.native_code_lower));
            
            String url = getString(R.string.lookup_word_url, nativeCode, foreignCode, word).replaceAll("&amp;", "&"); 
            if(lang instanceof String) { 
                url += "&lang=" + lang; 
            }
            
            Log.d(DictActivity.class.getName(), "Word: " + word + ", Lookup world url: " + url); 
            
            try  { 
                InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
                lookupObj = new GetLookupWordsFromXML(is);  
                // if(lookupObj.getLookupWords().size())
                suggestionsResult = lookupObj.getLookupWords();
                currentTIDS = lookupObj.getTranslationsIds(); 
                try { 
                  is.close();
                } catch(java.io.IOException e) { } 
            } catch (Exception e) { }
        }
        Log.w(DictActivity.class.getName(), "Words suggestions retrieved: " + currentTIDS.size());
      }
    }
    
    public void retrieveSuggestions(String word) { 
         retrieveWordsFromService(word);
         adapter = 
               new ArrayAdapter<String>(this,  android.R.layout.select_dialog_item, suggestionsResult);
         
         actvDev.setAdapter(adapter); 
         adapter.notifyDataSetChanged();
         Toast.makeText(this, getString(R.string.dict_fetching_msg), Toast.LENGTH_LONG).show();
   }
    
      @Override
    public void onClick(View view) {  
    	  
         try { 
        	 currWid = (Integer) view.getTag();
        	 addDictWordDetailsFragment(wordsDetails.get(currWid));
            
         } catch(ClassCastException e) { 
           
         }
   }
   
   private void addDictWordDetailsFragment(WordObject wo) {
	    FragmentManager fragmentManager =  getSupportFragmentManager(); 
		wordDetailsFragment = (DictWordDetailsFragment) fragmentManager.findFragmentByTag(DictWordDetailsFragment.TAG);
		
		
		if(wordDetailsFragment == null) { 
			Log.w(DictActivity.class.getName(), "There is no wordDetailsFragment");
   	 		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        wordDetailsFragment = new DictWordDetailsFragment();
	        wordDetailsFragment.setArguments(getWordBundle(wo)); 
	        fragmentTransaction.hide(resultListFragment);
	        fragmentTransaction.add(R.id.dict_content_frame, wordDetailsFragment, DictWordDetailsFragment.TAG); 
	        fragmentTransaction.addToBackStack("dictWordDetailsFragmentBack"); 
	        fragmentTransaction.commit();
		}
		else { 
			Log.w(DictActivity.class.getName(), "There exists wordDetailsFragment"); 
		}
	
   }
   
   private Bundle getWordBundle(WordObject wo) {
	   Bundle bundle = new Bundle(6); 
	   bundle.putInt(WordObject.KEY_WORD_ID , wo.getWordId()); 
	   bundle.putString(WordObject.KEY_FOREIGN_WORD, wo.getForeignWord());
	   bundle.putString(WordObject.KEY_NATIVE_WORD, wo.getNativeWord());
	   bundle.putString(WordObject.KEY_TRANSCRIPTION,  wo.getTranscription()); 
	   bundle.putString(WordObject.KEY_RECORDING, wo.getRecording());
	   if(wo.getImages().size() > 0)
		   bundle.putString(WordObject.KEY_IMAGE, wo.getImages().get(0));
	   else 
		   bundle.putString(WordObject.KEY_IMAGE, null); 
	   
	   return bundle; 
   }
   
   private void setNativeLang(MenuItem item) { 
       /**
        * DEPRECATED: 
        * Button enbtn = (Button) view.findViewById(R.id.englishBtn);
        * enbtn.setTextColor(Color.parseColor("#333333"));
        * Button plbtn = (Button) v; 
        * plbtn.setTextColor(Color.parseColor("#AB1E35"));
        **/
	   nativeItem = item; 
	   Log.w(DictActivity.class.getName(), "Menu item: " + nativeItem.getTitle() + " clicked.");
	   MenuItemCompat.setActionView(nativeItem, R.layout.dict_native_txtview);
	   
	   MenuItemCompat.expandActionView(nativeItem);
	   if(foreignItem != null) {
			MenuItemCompat.collapseActionView(foreignItem);
			MenuItemCompat.setActionView(foreignItem, null);
	   }
	   lang = "Native";
	   
	   String nativeCode = Preferences.getAccountPreferences(this)
	   			  		 .getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, 
	   			  				    getResources().getString(R.string.native_code_lower));
	   
	   TextView nativeTextView = (TextView) MenuItemCompat.getActionView(nativeItem).findViewById(R.id.dictNativeTxtView); 
	   nativeTextView.setText(getString(LanguageNameMapping.getResourceId(nativeCode)).toUpperCase()); 
   }
   
   private void setForeignLang(MenuItem item) { 
       /** 
        * DEPRECATED: 
        * Button plbtn = (Button) view.findViewById(R.id.polishBtn);
       	* plbtn.setTextColor(Color.parseColor("#333333"));
        * Button enbtn = (Button) v; 
        * enbtn.setTextColor(Color.parseColor("#AB1E35"));
        **/
	    foreignItem = item; 
	    Log.w(DictActivity.class.getName(), "Menu item: " + foreignItem.getTitle() + " clicked.");
	    MenuItemCompat.setActionView(foreignItem, R.layout.dict_foreign_txtview);
	   	
		MenuItemCompat.expandActionView(foreignItem);
		if(nativeItem != null) {
			MenuItemCompat.collapseActionView(nativeItem);
			MenuItemCompat.setActionView(nativeItem, null);
		}
		
		lang = "Foreing"; 
		
		String foreignCode = Preferences.getAccountPreferences(this)
						  		.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE,
								     getResources().getString(R.string.foreign_code_lower)); 
		
		
		TextView foreignTextView = (TextView) MenuItemCompat.getActionView(foreignItem).findViewById(R.id.dictForeignTxtView); 
	   	foreignTextView.setText(getString(LanguageNameMapping.getResourceId(foreignCode)).toUpperCase()); 
   }
   
   private void setForeignLangRTL(MenuItem item) {
	  
	   foreignItem = item; 
	   Log.w(DictActivity.class.getName(), "Menu item: " + foreignItem.getTitle() + " clicked.");
	   
	   foreignItem.setEnabled(false); 
	   
	   if(nativeItem != null)  
		   nativeItem.setEnabled(true); 
	   
	   lang = "Foreign";
   }
   
   private void setNativeLangRTL(MenuItem item) { 
	   
	   nativeItem = item; 
	   Log.w(DictActivity.class.getName(), "Menu item: " + nativeItem.getTitle() + " clicked.");
	   
	   nativeItem.setEnabled(false); 
	   
	   if(foreignItem != null) 
		   foreignItem.setEnabled(true); 
	   
	   lang = "Native";
   }
	
	@Override
	protected int getRightDrawerMenuButtonId() {
		
		return R.id.dictStorageBtn;
	}
	
	@Override
	protected void drawerMenuItemClicked(long id) {
		
		super.drawerMenuItemClicked(id);
	}
 
}


