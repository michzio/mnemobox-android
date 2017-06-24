package pl.electoroffline;

import pl.electoroffline.R;
import pl.electoroffline.Personalization.Mood;

import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.koushikdutta.urlimageviewhelper.UrlImageViewCallback;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import pl.elector.database.LearningHistoryProvider;
import pl.elector.database.LearningHistoryProvider.Mode;
import pl.elector.database.WordsetType;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

abstract public class LearningMethodActivity extends DrawerActivity implements WordsetWordsAccessor.Callbacks, LearningFragment.LearningListener, OnClickListener,
ViewPager.OnPageChangeListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
CustomViewPager.OnSwipeOutListener
{
	// ADMOB FULLSCREEN AD
	private InterstitialAd interstitial;
	
	public static final String WORDSET_TYPE = "KEY_WORDSET_TYPE";
	protected int wordsetId;
	protected WordsetType wordsetType; 
	protected Mode learningMode; 
	
	// containers with collection of WORDSET words
	protected LinkedHashMap<Integer, String> enWords;
    protected LinkedHashMap<Integer, String> plWords;
    protected LinkedHashMap<Integer, String> transcriptions;
    protected LinkedHashMap<Integer, String> audios;
    protected LinkedHashMap<Integer, String> imagePaths;
    protected LinkedHashMap<Integer, byte[]> imageData;
    protected boolean areImageDataAvailable; 
    protected ArrayList<Integer> widCollection;

    
    // learning method variables
    protected int currWid; 
    protected int goodAns = 0;
    protected int badAns = 0;
    protected LinkedHashMap<Integer, Integer> wordAnswers;
    protected ArrayList<Boolean> wordQuestionAsked;  
    
    protected MediaPlayer mediaPlayer;
    protected int index = -1; 
   
    protected RelativeLayout endScreen;
    private Button reloadButton;
    private Button comebackButton;
    
    // object with logic to trace learning progress 
    protected Personalization personalization;
    
    // helper variables 
    protected float scale;
    
    protected LearningMethodPagerAdapter learningMethodPagerAdapter;
    protected CustomViewPager learningMethodPager; 
    
    /**
     * DEPRECATED: 
     *  protected LinkedHashMap<Integer, Integer> forgotten;
     *  protected String serializedForgotten = "";
     */
    
    /**
     * Method called when activity is first created
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        learningMode = getLearningMode(); 
    }
    
    abstract Mode getLearningMode(); 
	
	@Override
	protected void onCreateDrawerActivity(Bundle savedInstanceState) {
		
		Preferences preferences = new Preferences(this, Preferences.getString(this, Preferences.KEY_EMAIL, "anonymous")); 
		Date adsFreeValidThru = preferences.getDate(Preferences.KEY_TURN_OFF_ADS_EXPIRATION_DATE, new Date(0L));
		if((new Date()).after(adsFreeValidThru)) { 
			Log.d("LearningMethodActivity", new Date() + " vs " + adsFreeValidThru); 
			createAdmobAd();
		}
		
		// get WORDSET identifier and type from Intent's Bundle 
		wordsetId = getIntent().getExtras().getInt(WordsetsListActivity.SELECTED_WORDSET); 
		wordsetType = (WordsetType) getIntent().getExtras().getSerializable(WordsetType.KEY_TYPE);
		if(wordsetType == null) wordsetType = WordsetType.SYSTEM_WORDSET;
		
		WordsetWordsAccessor wordsAccessor;
		if(wordsetType == WordsetType.SYSTEM_WORDSET ) { 
			wordsAccessor = new WordsetWordsAccessor(this, wordsetId, wordsetType, true);
		} else if(wordsetType == WordsetType.SELECTED_WORDS) { 
			wordsAccessor = new WordsetWordsAccessor(this, wordsetId, wordsetType, true);
			ArrayList<String> selectedWordIds = getIntent().getStringArrayListExtra(WordsetActivity.KEY_SELECTED_WORD_IDS);
			wordsAccessor.setSelectedWordIds(selectedWordIds);
		} else { 
			wordsAccessor = new WordsetWordsAccessor(this, wordsetId, wordsetType, false);
		}
		wordsAccessor.load();
		
		personalization = new Personalization(this); 
		
		createLayout();
	}
	
	/**
	 * Helper method used to create Admob ads 
	 * that will be displayed in application.
	 */
	private void createAdmobAd()
	{
		// Prepare the Interstitial Ad
        interstitial = new InterstitialAd(LearningMethodActivity.this);
        // Insert the Ad Unit ID
        interstitial.setAdUnitId("ca-app-pub-2929935550094094/5458197178");
        
        // Request for Ads
        AdRequest adRequest = new AdRequest.Builder()
 
        // Add a test device to show Test Ads
         //.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
         //.addTestDevice("CC5F2C72DF2B356BBF0DA198")
                .build();
        
        // Load ads into Interstitial Ads
        interstitial.loadAd(adRequest);
        
        // Prepare an Interstitial Ad Listener
        interstitial.setAdListener(new AdListener() {
            public void onAdLoaded() {
                // Call displayInterstitial() function
                displayInterstitial();
            }
        });
       
	}
	
	/**
	 * Method called when Interestitial Admob Ad has been loaded
	 */
	public void displayInterstitial() {
        // If Ads are loaded, show Interstitial else show nothing.
        if (interstitial.isLoaded()) {
            interstitial.show();
        }
    }
	
	/**
     * WordsetWordsAccessor.Callbacks interface method called when WordsetWordsAccessor
     * object finishes WORDSET WORDS details loading process. Now you can start learning method.
     */
	@Override
    public void onWordsLoadFinished(WordsetWordsAccessor wordsAccessor) {
      	
      		Log.w(WordsetWordsAccessor.Callbacks.class.getName(), 
      					"Words loading finished! (" + wordsAccessor.getWordIds().size() + ")");
      		
      		// setting local attributes with collections of data loaded by words accessory
    	    enWords = wordsAccessor.getForeignWords(); 
    	    plWords = wordsAccessor.getNativeWords();
    	    transcriptions = wordsAccessor.getTranscriptions(); 
    	    audios = wordsAccessor.getRecordingPaths();
    	    imagePaths = wordsAccessor.getImagePaths();
    	    imageData = wordsAccessor.getImageData(); 
    	    areImageDataAvailable = (imageData == null) ? false : true;
    	    widCollection = wordsAccessor.getWordIds();    	    
    	        
    	    // start learning 
    	    startLearning();
    }
	
	/**
	 * Method shuffle WORD_IDS collection and load first word
	 */
	protected void startLearning() { 
		
		if(widCollection != null) { 
	        Collections.shuffle(widCollection); 
	        index = -1;
	        currWid = widCollection.get(0); // important! presetting current wordId on first word in newly loaded collection
	        // learning method helper collections
	        resetWordQuestionsAsked();
	        resetWordsAnswers();
	        
	        learningMethodPagerAdapter.notifyDataSetChanged();
	        
	        loadNextWord();    
	        addAds();
		}
	 }
	
	protected void resetWordQuestionsAsked()  {
		
		if(wordQuestionAsked == null)
			wordQuestionAsked = new ArrayList<Boolean>(widCollection.size());
		else 
			wordQuestionAsked.clear();
			
		
		for(int i=0; i< widCollection.size(); i++) 
			wordQuestionAsked.add(false);
	}
	
	protected void resetWordsAnswers() { 
		
		if(wordAnswers == null)
			wordAnswers = new LinkedHashMap<Integer, Integer>();
		
		wordAnswers.clear(); 
	}
	
	/**
	 * Helper method used to add ads on the screen
	 */
	protected void addAds() {
		return; 
	}
	
	
	/**
	 * Method goes to the next word 
	 */
	public void loadNextWord() {
		 //hideChallangeElements();
		 if( (index + 1) < widCollection.size()) { 
			 Log.w(LearningMethodActivity.class.getName(), "Current word index is: " + (index+1));
			 if(index < 0) { 
				 // for first page in viewpager
				 learningMethodPager.setCurrentItem(0);
				 currWid = (Integer) widCollection.get(0);
				 index = 0; 
			 } else {
				 // for next page in viewpager
				 currWid = (Integer) widCollection.get(index+1); // ? 
				 learningMethodPager.setCurrentItem(index+1);
				 
			 }
		 } else { 
	         finishLearning(); 
	     }
	}
	
	//abstract protected void hideChallangeElements();
	//abstract protected void displayCurrentWord();
	
	/**
	 * Method checks user answer to current word 
	 */
	protected void checkAnswer() {
	     playRecording();
	     // verifyAnswer();
	     // uncoverChallangeElements();      
	}
	
	//abstract protected void verifyAnswer();
	//abstract protected void uncoverChallangeElements();
	
	protected void finishLearning() { 
	    	
		/**
	     * DEPRECATED
	     * Statistics.storeForgotten(serializedForgotten, this);
	     * Statistics.traceHistory(wordsetId, 0, 0, "prezentacja", this);
	     */ 
		
		 // save results of current learning in learning history and learning statistics.
		 personalization.traceLearningHistoryAndStatistics(wordsetId, learningMode, wordsetType, badAns, goodAns, learningHistoryTracerListener);
	    	
	     learningMethodPager.setVisibility(View.GONE);   
	     endScreen.setVisibility(View.VISIBLE);
	     
	}
	
	Personalization.OnTracerCompletedListener learningHistoryTracerListener = new Personalization.OnTracerCompletedListener() {
		
		@Override
		public void onTracerCompleted() {
			
			Log.d(LearningMethodActivity.class.getName(), "Learning history and statistics has been saved in local database..."); 
			
			 // after each learning application tries to synchronize learning personalities 
		     personalization.synchronize();
		}
	};
	
	/**
	 * Helper method used to create learning method layout
	 */
	protected void createLayout() {
		scale = this.getResources().getDisplayMetrics().density;
		if(isUsingViewPager()) { 
			setContentView(R.layout.learning_method_drawer); 
			
			// ViewPager and its adapters use support library
	        // fragments, so use getSupportFragmentManager.
			learningMethodPagerAdapter =  new LearningMethodPagerAdapter(getSupportFragmentManager(), getLearningFragmentClass());
			learningMethodPager = (CustomViewPager) findViewById(R.id.learningMethodPager);
			learningMethodPager.setAdapter(learningMethodPagerAdapter);
			learningMethodPager.setCurrentItem(0);
			learningMethodPager.setOnPageChangeListener(this); 
			learningMethodPager.setOnSwipeOutListener(this);
			
			// load learning method end screen views
			endScreen = (RelativeLayout) findViewById(R.id.endOfPresentation);
	        reloadButton = (Button) findViewById(R.id.reloadPresentation);
	        reloadButton.setOnClickListener(this);
	        comebackButton = (Button) findViewById(R.id.comebackPresentation);
	        comebackButton.setOnClickListener(this);
		}
	}
	
	abstract protected boolean isUsingViewPager();
	abstract protected Class<?> getLearningFragmentClass();
	
	
	public void playRecording() 
	{
		Log.w(LearningMethodActivity.class.getName(), "Current word id: " + currWid); 
		playRecording(currWid); 
	}
	
   /**
	* Method used to play recording of current word
	*/
	public void playRecording(Integer wordId) { 
		if(mediaPlayer != null) mediaPlayer.release(); // important! to not get audio resources out of memory!
        mediaPlayer = new MediaPlayer();     
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
    
        String audioUrl = /* getResources().getString(R.string.recordings_url) + */ audios.get(wordId);
        // Toast.makeText(this, audioUrl, Toast.LENGTH_SHORT).show();
        
        if(audioUrl.startsWith(WordsetWordsAccessor.CHECK_RECORDING_AVAILABILITY)) { 
        	String audioName = audioUrl.replaceFirst(WordsetWordsAccessor.CHECK_RECORDING_AVAILABILITY, "");
        	
        	audioUrl = generateAudioURL(wordId, audioName);
        	audios.put(wordId, audioUrl); // replace for future recording url for current word id
        	
        }
        
        boolean isAudioOnline = audioUrl.substring(0, 4).equals("http");
       /** ImageButton onClick event handler. Method which start/pause media player playing */
        try {
        	if(isAudioOnline) { 
              	mediaPlayer.setDataSource(audioUrl);
              	mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepareAsync();
              } else {
              	FileInputStream fis = new FileInputStream(audioUrl);
              	mediaPlayer.setDataSource(fis.getFD());
              	mediaPlayer.setOnErrorListener(this);
              	mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
              	fis.close();
              	mediaPlayer.prepare();
              	
              }
               // you must call this method after setup the data source in setDataSource method. 
               // After calling prepare() the instance of MediaPlayer starts load data from URL to internal buffer. 
        } catch (Exception e) {
           // e.printStackTrace();
        }

	    if(!mediaPlayer.isPlaying()){
	        mediaPlayer.start();
	         //buttonPlayPause.setImageResource(R.drawable.button_pause);
	    }else {
	        mediaPlayer.pause();
	         //buttonPlayPause.setImageResource(R.drawable.button_play);
	    }
	}
	
	protected String generateAudioURL(int wordId, String recordingName) { 
        
	   	 if(Preferences.getBoolean(this, Preferences.KEY_PREFER_TO_DOWNLOAD_AUDIO, true))
	   	 {
	   		 // user prefers to download audio and play it from local storage 
	   		 File path = new File(FileUtilities.getExternalFilesDir(this), "recordings"); 
	   		 String dirPath = path.getAbsolutePath();
	   		 String recordingPath = dirPath + File.separator + wordId + recordingName;
	   		 File recordingFile = new File(recordingPath); 
	   		 if(recordingFile.exists()) { 
	   			 return recordingPath; 
	   		 } 
	   	 }
	   	 
	   	// else if user prefers to use online stored audio files or local recording file doesn't exists
	   	String dirPath = getResources().getString(R.string.recordings_url).replaceAll("&amp;", "&");
	   	String recordingPath = dirPath + recordingName;
	    Log.w(LearningMethodActivity.class.getName(), recordingPath); 
	    return recordingPath; 
	}
	
	/**
	 * Media player handlers
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		/** MediaPlayer onCompletion event handler. Method 
		 * which calls then song playing is complete*/
		//buttonPlayPause.setImageResource(R.drawable.button_play);
	}
	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		/** Method which updates the SeekBar secondary progress by 
		 * current song loading from URL position*/
		 //seekBarProgress.setSecondaryProgress(percent);
	}
	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();
	}
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
	   	Toast.makeText(this, "Error " + what, Toast.LENGTH_LONG).show();
	    return false;
	}
    
   /**
    * Handler used to populate options menu
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.learning_method_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    /**
     * Handler used to modify options menu on invalidate menu called
     */
    @Override 
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        return super.onPrepareOptionsMenu(menu);
    }
 
    /**
     * Event Handling for individual menu item selected
     * Identify single menu item by it's id
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        switch (item.getItemId())
        {
	        case R.id.menu_details:
	            showWordDetails(currWid);
	            return true;
	 
	        case R.id.menu_share:  
	            if(currWid != 0) { 
	            	Statistics.shareWithFriends(currWid, enWords.get(currWid), plWords.get(currWid), this);
	            } else { 
	            	Toast.makeText(this, getResources().getString(R.string.no_word_to_share), Toast.LENGTH_SHORT).show();  
	            }
	            return true;
	            
	        case R.id.menu_remember:
	           
	            if(currWid != 0) { 
	            	personalization.traceRememberMeWord(currWid); 
	            } else { 
	            	Toast.makeText(this, getResources().getString(R.string.no_word_to_remember), Toast.LENGTH_SHORT).show();  
	            }
	            return true;
	        case R.id.menu_learned_word:
	        	 if(currWid != 0) { 
		            	personalization.traceLearnedWord(currWid); 
		            } else { 
		            	Toast.makeText(this, getResources().getString(R.string.no_word_to_learned), Toast.LENGTH_SHORT).show();  
		            }
	        	return true; 
	        case R.id.raport_an_error:
	        	raportAnError(currWid); 
	        	return true; 
	        default:
	            break; 
        }
        
        return super.onOptionsItemSelected(item);
    } 
    
    private void showWordDetails(int wordId)
    {
    	 if(wordId != 0) { 
         	Intent detailsIntent = new Intent(this, WordDetailsActivity.class);
       		detailsIntent.putExtra(WordDetailsActivity.KEY_WORD_ID, wordId);
             startActivity(detailsIntent);
         } else { 
         	Toast.makeText(this, getResources().getString(R.string.no_word_to_show_details), Toast.LENGTH_SHORT).show();  
         }
    }
    
    private void raportAnError(int wordId)
    {
    	String foreignCode = Preferences.getAccountPreferences(this)
    									.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, 
    											   getResources().getString(R.string.foreign_code_lower));
    	final String errorURL = getString(R.string.trace_error_url, foreignCode, wordId).replaceAll("&amp;", "&");
    	Log.d(LearningMethodActivity.class.getName(), "Error url: " + errorURL); 
    	
    	// create dialog that prompts user if he want to 
		// SYNC words now or prefer ONLINE access
		AlertDialog dialog = new AlertDialog.Builder(this)
							.setMessage(getString(R.string.report_an_error_message, enWords.get((Integer.valueOf(wordId))) ))
							.setCancelable(false)
							.setPositiveButton(R.string.report_an_error_button,  new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// Handle ONLINE wordset words access
									
									// checking if there is network available
									if(NetworkUtilities.haveNetworkConnection(LearningMethodActivity.this))
									{
										try {
											CustomHttpClient.executeHttpGet(errorURL);
										} catch (Exception e) {
											e.printStackTrace();
											Toast.makeText(LearningMethodActivity.this, 
													R.string.reporting_error_occured_toast, Toast.LENGTH_SHORT).show();
										}
									} else {
										Toast.makeText(LearningMethodActivity.this, R.string.internet_lost, Toast.LENGTH_SHORT).show();
									}
									
								}
							})
							.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// do nothing
								}
							})
							.create();
	
		dialog.show();
    }

    /**
     * Drawer Expandable List View (right or left) Group item clicked 
     */
	@Override
	public boolean onGroupClick(ExpandableListView parent, View v,
			int groupPosition, long id) {
		
		return super.onGroupClick(parent, v, groupPosition, id);
	} 

	/**
	 * Drawer Expandable List View (right or left) Child item clicked
	 */
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		
		return super.onChildClick(parent, v, groupPosition, childPosition, id);
	}
	
	@Override
	protected void drawerMenuItemClicked(long id) {
		
		switch((int) id) {
			
			case 0: // vocabulary
				break;
			case 1: // dictionary 
				break; 
			case 2: // profile info 
				break;
			case 3: // learned words 
			{
				FragmentManager fragmentManager = getSupportFragmentManager(); 
                fragmentManager.beginTransaction()
                			   .add(R.id.main_content_frame, new LearnedWordsFragment(), LearnedWordsFragment.TAG)
                			   .addToBackStack("learnedWordsFragmentBack")
                			   .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
                			   .commit();
                drawerLayout.closeDrawers();
                return; 
			}
			case 4: // forgotten words 
			{ 
				FragmentManager fragmentManager =getSupportFragmentManager(); 
                fragmentManager.beginTransaction()
                			   .add(R.id.main_content_frame, new ForgottenFragment(),ForgottenFragment.TAG)
              				   .addToBackStack("forgottenFragmentBack")
                			   .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
                			   .commit();
                drawerLayout.closeDrawers();
                return;
			}
			case 5: // remember me words 
			{ 
				FragmentManager fragmentManager = getSupportFragmentManager(); 
                fragmentManager.beginTransaction()
                			   .add(R.id.main_content_frame, new RememberMeFragment(), RememberMeFragment.TAG)
                			   .addToBackStack("rememberMeFragmentBack")
                			   .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
                			   .commit();
                drawerLayout.closeDrawers();
                return; 
			}
			case 6: // user wordsets
				break; 
			case 7: // learning history 
				break; 
			case 8: // langwish playlist
				break; 
			default: 
				break;
		}
		
		super.drawerMenuItemClicked(id);
	}

	@Override
	protected int getRightDrawerMenuButtonId() {
		
		return R.id.menu_forgotten_words;
	}
	
	private ForgottenWordsAdapter forgottenAdapter;
	
	@Override 
	protected void configureRightDrawer() {
		
		super.configureRightDrawer();
		
		// populating right drawer expandable list view with words forgotten by user while learning.
		forgottenAdapter = new ForgottenWordsAdapter(this);
		configureRightHeaderView();
		rightDrawerList.setAdapter(forgottenAdapter);
		//rightDrawerList.setOnGroupClickListener(this);
	}
	
	public void addToForgottenDrawerList(Integer wordId)
	{
		forgottenAdapter.addForgottenWord(wordId);
	}
	
	@Override
	public void traceForgottenWord(Integer wordId, Mood mood) {
		personalization.traceForgottenWord(wordId, mood);
	}

	@Override
	public void traceLearnedWord(Integer wordId) {
		personalization.traceLearnedWord(wordId); 
	}
	
	/**
	 * Method used to display header view on right drawer 
	 */
	private void configureRightHeaderView() {
		
	}
	
	private class ForgottenWordsAdapter extends BaseExpandableListAdapter {
		
		private LayoutInflater inflater; 
		private Activity activity; 
		
		private LinkedHashSet<Integer> forgottenWordIds; 
		
		public ForgottenWordsAdapter(Activity a) 
		{
			activity = a; 
			inflater = a.getLayoutInflater(); 
			
			forgottenWordIds = new LinkedHashSet<Integer>(); 
		}
		
		public void addForgottenWord(Integer wordId)
		{
			this.forgottenWordIds.add(wordId);
			
			// reload expandable list view
			notifyDataSetChanged();
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			
			return new ArrayList<Integer>(forgottenWordIds).get(groupPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			
			return new ArrayList<Integer>(forgottenWordIds).get(groupPosition);
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			
			return 0;
		}

		@Override
		public Object getGroup(int groupPosition) {
			
			return new ArrayList<Integer>(forgottenWordIds).get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			
			return forgottenWordIds.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
		
			return new ArrayList<Integer>(forgottenWordIds).get(groupPosition);
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			
			final Integer groupWordId = (Integer) getGroup(groupPosition);
			
			TextView textView = null; 
			
			if(convertView == null) {
				convertView = inflater.inflate(R.layout.textview_item, null);
			}
			
			textView= (TextView) convertView.findViewById(R.id.textview);
			textView.setText(enWords.get(groupWordId) + " - " + plWords.get(groupWordId));
			convertView.setTag(groupWordId);
			
			convertView.setOnClickListener( new OnClickListener() {

				@Override
				public void onClick(View v) {
					Log.w(ForgottenWordsAdapter.class.getName(), "User selected word: " + enWords.get((Integer)v.getTag()));
					showWordDetails((Integer)v.getTag());
				}
				
			});
			
			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			
			return true;
		} 
		
		
	}
	
	public class LearningMethodPagerAdapter extends FragmentStatePagerAdapter {
		 
		private final Class<?> learningFragmentClass; 
		
		private Map<Integer, WeakReference<LearningFragment>> mWeakFragmentMap 
												= new HashMap<Integer, WeakReference<LearningFragment>>();

		public LearningMethodPagerAdapter(FragmentManager fm, Class<?> learningFragmentClass) {
			super(fm);
			
			this.learningFragmentClass = learningFragmentClass; 
			
		}
		
		@Override
		public int getItemPosition(Object object)
		{
			return FragmentPagerAdapter.POSITION_NONE;
			
		}

		@Override
		public Fragment getItem(int position) {
			
				Integer wordId = LearningMethodActivity.this.widCollection.get(position);
				Log.w(LearningMethodPagerAdapter.class.getName(), "Creating Fragment for word: " + wordId); 
				
				LearningFragment fragment = (LearningFragment) Fragment.instantiate(LearningMethodActivity.this, learningFragmentClass.getName());
				fragment.setWordId(wordId);
				fragment.setForeignWord(LearningMethodActivity.this.enWords.get(wordId));
				fragment.setNativeWord(LearningMethodActivity.this.plWords.get(wordId));
				fragment.setTranscription(LearningMethodActivity.this.transcriptions.get(wordId));
				fragment.setWordAnswered(LearningMethodActivity.this.wordQuestionAsked.get(position));
				Integer wordAnsCount = LearningMethodActivity.this.wordAnswers.get(wordId);
				fragment.setWordAnsweredSuccessfully( (wordAnsCount != null && wordAnsCount > 0) ? true : false);
				fragment.setLearningListener(LearningMethodActivity.this);
				
				WeakReference<LearningFragment> weakFragment = mWeakFragmentMap.get(position);
		        if (weakFragment != null) {
		            weakFragment.clear();
		        }
		        mWeakFragmentMap.put(position, new WeakReference<LearningFragment>(fragment));
			
			return fragment;
		}
		
		
		
		public LearningFragment getFragmentAt(int position) { 
			
			 WeakReference<LearningFragment> weakFragment = mWeakFragmentMap.get(position);
		     if (weakFragment != null && weakFragment.get() != null) {
		            return weakFragment.get();
		     } else { 
		            return null;
		     } 
		}

		@Override
		public int getCount() {
			if(LearningMethodActivity.this.widCollection != null) { 
				//Log.w(LearningMethodPagerAdapter.class.getName(), "Learning Method Pager Adapter detect: " + 
				//		LearningMethodActivity.this.widCollection.size() + " words. ");
					
				return LearningMethodActivity.this.widCollection.size();
			} else 
				return 0; 
		}

	}
	
	@Override
	public void onPageScrollStateChanged(int arg0) {
		
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

	}

	@Override
	public void onPageSelected(int position) {
		
		Log.w(ViewPager.OnPageChangeListener.class.getName(), "OnPageChangeListener.onPageSelected() called: " + index + "=>" + position);
		
		LearningFragment previousFragment = learningMethodPagerAdapter.getFragmentAt(index);
		LearningFragment nextFragment = learningMethodPagerAdapter.getFragmentAt(position);
		
		
		if(previousFragment != null) previousFragment.setVisible(false);
		if(nextFragment != null) nextFragment.setVisible(true);
		
		if(position > index) {
			Log.w(LearningMethodActivity.class.getName(), "Swipe Forward detected!");
			
			if(previousFragment != null) previousFragment.onSwipedForward();
			currWid = widCollection.get(position); 
			index = position; 
			if(nextFragment != null) nextFragment.onSwipingForward();
		} else if(position < index){ 
			Log.w(LearningMethodActivity.class.getName(), "Swipe Backward detected!");
			
			if(previousFragment != null) previousFragment.onSwipedBackward();
			currWid = widCollection.get(position); 
			index = position; 
			if(nextFragment != null) nextFragment.onSwipingBackward();
		} else { 
			Log.w(LearningMethodActivity.class.getName(), "onPageSelected() - page index doesn't change.");
		}
		
		
		
	}
	
	@Override
	public void onClick(View view) {
		
		if(view.getId() == R.id.reloadPresentation) {   	
            endScreen.setVisibility(View.GONE);
            learningMethodPager.setVisibility(View.VISIBLE);
            startLearning();
     
        } else if( view.getId() == R.id.comebackPresentation) { 
             finish();
        }
		
	}
	
	@Override
	public void onSwipeOutAtEnd() {
		Log.w(LearningMethodActivity.class.getName(), "Swipe Pager Out Screen!");
		 finishLearning();
	}
	
	@Override
	public void onSwipeOutAtStart() { 
		return; 
	}
	
	@Override
	public void loadWordImage(ImageView wordImageView, Integer wordId) {
		if(areImageDataAvailable) { 
			DisplayMetrics displaymetrics = new DisplayMetrics();
		    getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		    int width = displaymetrics.widthPixels;
		    
			byte[] image = imageData.get(wordId); 
			Bitmap bitmap = null; 
			if(image != null) {
				bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
				
				wordImageView.setImageBitmap(BitmapUtilities.fitSize(bitmap, width, 0));
			} else { 
				wordImageView.setImageBitmap(BitmapUtilities.fitSize(bitmap, width, 0)); 
				wordImageView.setVisibility(View.GONE); 
			}
		
		} else { 
	        
	        UrlImageViewHelper.setUrlDrawable(wordImageView,
	        				getString(R.string.images_url) + imagePaths.get(wordId),
	        				urlImageViewCallback);
		}
	}
	
	private UrlImageViewCallback urlImageViewCallback = new UrlImageViewCallback() {

		@Override
		public void onLoaded(ImageView imageView, Drawable loadedDrawable,
				String url, boolean loadedFromCache) {
				
				if(loadedDrawable != null) { 
					Log.d(LearningMethodActivity.class.getName(), "UrlImageView has found image..."); 
				} else { 
					Log.d(LearningMethodActivity.class.getName(), "UrlImageView hasn't found image...");
					imageView.setVisibility(View.GONE); 
				}
		} 
		
	}; 
	
	
	@Override 
	public void incrementGoodAns() 
	{
		Log.w(LearningMethodActivity.class.getName(), "Incrementing good answers count for word: " + enWords.get(currWid) );
		goodAns++;
		wordQuestionAsked.set(index, true);
		Integer currAnsCount = wordAnswers.get(currWid);
		if(currAnsCount == null) 
			currAnsCount = 0; 
		wordAnswers.put(currWid, ++currAnsCount);
	}
	
	@Override
	public void incrementBadAns()
	{
		Log.w(LearningMethodActivity.class.getName(), "Incrementing bad answers count for word: " + enWords.get(currWid));
		badAns++;
		wordQuestionAsked.set(index, true);
		Integer currAnsCount = wordAnswers.get(currWid);
		if(currAnsCount == null) 
			currAnsCount = 0;
		wordAnswers.put(currWid, --currAnsCount);
	}
	
	@Override
	public HashMap<Integer, String> getForeignWords() {
		return enWords;
	}
	@Override
	public ArrayList<Integer> getWordIds() { 
		return widCollection;
	}
	
	public boolean checkCurrentWordAnswered() {
		return wordQuestionAsked.get(index);
	}
	public boolean currentWordAnsweredSuccessfully() {
		Integer currAnsCount = wordAnswers.get(currWid); 
		if(currAnsCount != null && currAnsCount > 0)
			return true; 
		else 
			return false; 
	}
	
	
}
