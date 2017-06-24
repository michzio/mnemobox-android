package pl.electoroffline;

import pl.electoroffline.R;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import pl.elector.database.WordsetType;
import pl.elector.service.SyncPersonalizationService;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Images;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public abstract class WordsListFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener,
OnCompletionListener, OnBufferingUpdateListener, OnPreparedListener, SwipeInterface {
	
	/**
	 * ABSTRACT METHODS
	 */
	abstract String getHeader();
    abstract void loadWordsFromDatabase();
    abstract void traceCurrentWordToDelete(int wordId);
    abstract void traceCheckedWordsToDelete();
    abstract WordsetType getWordsetType();
    abstract boolean isSyncAskEnabled();
    protected boolean areAllWordsInDatabase = true; 
	
	protected static final int DETAILS_ITEM_ID = 333;
	protected static final int DELETE_ITEM_ID = 222;
	protected static final int COPY_FOREIGN_ITEM_ID = 444;
	protected static final int COPY_NATIVE_ITEM_ID = 555;
	protected static final int COPY_TRANSCRIPTION_ITEM_ID = 666;
	protected static final int SHARE_ITEM_ID = 777;
	
	protected View view; 
	protected ActivitySwipeDetector swipeDetector; 
	protected Object actionMode;
	protected View toDeleteWordView; 
	protected Integer selectedWordId;
	protected WordObject selectedWord; 
	
	protected ScrollView scrollview;
    protected LinearLayout layout;
    protected float scale;
    
    protected MediaPlayer mediaPlayer;
    protected LinkedHashMap<Integer, String> recordingPaths;
    protected LinkedHashMap<Integer, byte[]> imagesData; 
    protected static final String IMAGE_FROM_BLOB = "IMAGE_FROM_BLOB"; 
    
    protected int profileId; 
    
    protected boolean isSyncing = false; 
    
    protected boolean inDeletionMode = false; 
    protected ArrayList<Integer> checkedWords = new ArrayList<Integer>();
    protected ArrayList<String> wordIds = new ArrayList<String>();
    
    @Override
   	public void onActivityCreated(Bundle savedInstanceState)
   	{
   		super.onActivityCreated(savedInstanceState);
   		
   	}
    
    @Override 
    public void onDestroy() {
    	ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
    	actionBar.setCustomView(null); 
    	super.onDestroy();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
    	((DrawerActivity) getActivity()).drawerMenuAdapter.notifyDataSetChanged();
    	
        view = inflater.inflate(R.layout.wordslist, container, false);
        
        createLayout(); 
        
        profileId = Preferences.getInt(getActivity(), Preferences.KEY_PROFILE_ID, 0);
        recordingPaths = new LinkedHashMap<Integer, String>();
        imagesData = new LinkedHashMap<Integer, byte[]>();
       
        // asking user to launch words personalization synchronization process
        if(isSyncAskEnabled())
        	askUserToSyncWords(); 
        
        swipeDetector = new ActivitySwipeDetector(this);
        Toast.makeText(getActivity(), R.string.swipe_left_to_remove_item, Toast.LENGTH_SHORT).show();
        
        // method loading words from local SQL database 
        loadWordsFromDatabase(); 
        
        addScrollViewListener();
        
        return view; 
    }
    
   protected void askUserToSyncWords() {
    	
    	AlertDialog dialog = new AlertDialog.Builder(getActivity())
		.setMessage(R.string.want_to_sync_personalizations)
		.setCancelable(false)
		.setPositiveButton(R.string.yes_sync_now_button,  new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Personalization p = new Personalization(getActivity());
		        p.synchronize();
		        if(NetworkUtilities.haveNetworkConnection(getActivity()))
		        	showProgressDialog();
			}
		})
		.setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// do nothing 
			}
		})
		.create();

    	dialog.show();
    }
   
   protected void showProgressDialog() {
	   	isSyncing = true; 
	   	
	   	ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
	   	actionBar.setCustomView(R.layout.progressbar); 
   	
   }
   
    protected void createLayout() 
    {
   		// ActionBar modification 
   		ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
   		actionBar.setTitle(getHeader());
   		actionBar.setSubtitle(null);
   		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP 
   								| ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
   		setRetainInstance(true);
   		setHasOptionsMenu(true);
   		getActivity().supportInvalidateOptionsMenu();
   		
   	
   		// Fragment modification 
   		scale = this.getResources().getDisplayMetrics().density;
   		scrollview = (ScrollView) view.findViewById(R.id.wordsListScrollView);
   		layout = new LinearLayout(getActivity());  
   		layout.setOrientation(LinearLayout.VERTICAL);
   		int px10 = (int) (10*scale + 0.5f); 
        layout.setPadding(px10, px10, px10, px10);
        layout.setScrollContainer(true);
        scrollview.addView(layout);
       
    }
    
    /**
     * Method deletes current list of words and reload it again
     */
    private void reloadWordsFromDatabase() 
    {
    	Log.w(WordsListFragment.class.getName(), "Reload words list from database.");
    	layout.removeAllViews();
    	recordingPaths.clear(); 
    	loadWordsFromDatabase(); 
    	// after reloading words set options menu and selections to default state
    	selectedWordId = null;
    	getActivity().supportInvalidateOptionsMenu();
    }
    
    protected void makeButton(String foreignWord, String nativeWord, String transcription, int wordId) { 
        
       	LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
       	View wordItemView =  inflater.inflate(R.layout.word_item, null); 
       	
       	TextView foreignWordView = (TextView) wordItemView.findViewById(R.id.foreignWord);
       	foreignWordView.setText(foreignWord);
       	TextView transcriptionView = (TextView) wordItemView.findViewById(R.id.transcription);
       	transcription = transcription.replaceAll("��", "'").replaceAll("��", ",");
	    if(DrawerActivity.isRTL())  {
	       	SpannableStringBuilder styledTranscription = new SpannableStringBuilder(transcription);
	       	styledTranscription.setSpan (
	       			new BackgroundColorSpan(getActivity().getResources().getColor(R.color.lightBlue)), 0, transcription.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	       	transcriptionView.setText(styledTranscription);
	    } else { 
	       	transcriptionView.setText(transcription);
	    }
       	TextView nativeWordView = (TextView) wordItemView.findViewById(R.id.nativeWord); 
       	nativeWordView.setText(nativeWord); 
       	wordItemView.setId(wordId);
       	wordItemView.setTag(wordId); 
       	// building array list of word ids
       	wordIds.add(String.valueOf(wordId)); 
       	
       	// adjusting audio button 
       	ImageView audioButton = (ImageView) wordItemView.findViewById(R.id.audioBtn);
        
        /**
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
	        int width = displaymetrics.widthPixels;
	        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
	                     RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        int px = (int) (1*scale + 0.5f); 
	        layoutParams.leftMargin = width-px*75;
	        layoutParams.topMargin = px*10;
	        audioButton.setLayoutParams(layoutParams);
         */
       
        audioButton.setTag(wordId);
        audioButton.setOnClickListener(new View.OnClickListener() {
        	
    		@Override
    		public void onClick(View v) {
    			playRecording(v.getTag());	
    		}
    	});
        CheckBox checkBox = (CheckBox) wordItemView.findViewById(R.id.wordCheckbox);
        /**
	        layoutParams = new RelativeLayout.LayoutParams(
	                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        layoutParams.leftMargin = width-px*80;
	        layoutParams.topMargin = px*40;
	        checkBox.setLayoutParams(layoutParams);
        */
        checkBox.setOnCheckedChangeListener(this);
        wordItemView.setOnClickListener(this);
        wordItemView.setOnTouchListener(new View.OnTouchListener() {  
             @Override
             public boolean onTouch(View v, MotionEvent event) {
                 //gesture detector to detect swipe.
            	 swipeDetector.onTouch(v, event);
                 return false;//always return true to consume event
             }
         });
        registerForContextMenu(wordItemView);
        wordItemView.setOnLongClickListener( new View.OnLongClickListener() {
    		
    		@Override
    		public boolean onLongClick(View v) {
    			Log.w(LearnedWordsFragment.class.getName(), "Long press...");
    			getActivity().openContextMenu(v);
    			return true;
    		}
    	});
            
       	layout.addView(wordItemView);
       	
   }
    
    /** 
     * Helper function that makes paths online/local to recordings. 
     * @param wordId
     * @param recordingName
     */
    protected void makeAudio(int wordId, String recordingName) { 
           
   	 if(Preferences.getBoolean(getActivity(), Preferences.KEY_PREFER_TO_DOWNLOAD_AUDIO, true))
   	 {
   		 // user prefers to download audio and play it from local storage 
   		 File path = new File(FileUtilities.getExternalFilesDir(getActivity()), "recordings"); 
   		 String dirPath = path.getAbsolutePath();
   		 String recordingPath = dirPath + File.separator + wordId + recordingName;
   		 File recordingFile = new File(recordingPath); 
   		 if(recordingFile.exists()) { 
   			 recordingPaths.put(wordId, recordingPath);
   			 return; 
   		 } 
   	 }
   	 
   	// else if user prefers to use online stored audio files or local recording file doesn't exists
   	String dirPath = getResources().getString(R.string.recordings_url).replaceAll("&amp;", "&");
   	String recordingPath = dirPath + recordingName;
    Log.w(WordsListFragment.class.getName(), recordingPath); 
   	recordingPaths.put(wordId, recordingPath); 
    }
    
    /**
     * DEPRECATED: 
     * in order to use lazy image loading
    protected void makeImageView(int wordId, Bitmap imageBitmap) { 
        
   	 View v = layout.findViewById(wordId); 
   	 ImageView wordImageView = (ImageView) v.findViewById(R.id.wordImage); 
    	 
    	 // setting image on ImageView
    	 if(imageBitmap != null)
    		 wordImageView.setImageBitmap(BitmapUtilities.fitSize(imageBitmap, (int) (200*scale + 0.5f), (int) (100*scale + 0.5f)));
    	 else 
    		 wordImageView.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
    } */
    
    protected void makeImageView(int wordId, String imageName) { 
    	View v = layout.findViewById(wordId); 
    	ImageView wordImageView = (ImageView) v.findViewById(R.id.wordImage); 
    	
    	if(imageName != null && !imageName.equals("")) { 
   
    		//UrlImageViewHelper.setUrlDrawable(wordImageView, getResources().getString(R.string.images_url) + imageName);
    		wordImageView.setTag(imageName);
    	} else { 
    		wordImageView.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
    	}
    }
    
    @Override
    public void onClick(View view) {  
   	
	   	selectedWordId = (Integer) view.getTag();
	   	selectWord(view); 
	   	
	   	getActivity().supportInvalidateOptionsMenu();
	    playRecording(view.getTag());
       
    }
    
    private void selectWord(View v) {
   	 int wordId = (Integer) v.getTag();
   	 String foreignWord = (String) ((TextView)v.findViewById(R.id.foreignWord)).getText();
   	 String nativeWord = (String) ((TextView)v.findViewById(R.id.nativeWord)).getText();
   	 String transcription = ((TextView)v.findViewById(R.id.transcription)).getText().toString();
 
   	 Bitmap imageBitmap = null; 
   	 try { 
   		 imageBitmap = ((BitmapDrawable) ((ImageView)v.findViewById(R.id.wordImage)).getDrawable()).getBitmap();
   	 } catch(ClassCastException ex) {
   		 
   	 } catch(NullPointerException ex) { 
   		 // when there isn't image set on word item
   	 }
   	 
   	 selectedWord = new WordObject();
   	 selectedWord.setWordId(wordId);
   	 selectedWord.setForeignWord(foreignWord);
   	 selectedWord.setNativeWord(nativeWord);
   	 selectedWord.setTranscription(transcription);
   	 selectedWord.setImageBitmap(imageBitmap);
    }
    
    private void showWordDetails() { 
   	 
   	 if(selectedWordId != null) {
   		 Intent detailsIntent = new Intent(getActivity(), WordDetailsActivity.class);
   		 detailsIntent.putExtra(WordDetailsActivity.KEY_WORD_ID, (int) selectedWordId);
         startActivity(detailsIntent);
    	} else {
    		Toast.makeText(getActivity(), R.string.no_word_selected, Toast.LENGTH_SHORT).show();
    	}
    }
    
    private void playRecording(Object wid) {    
   	    mediaPlayer = new MediaPlayer();     
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
        
        int intWordId = (Integer) wid; 
        
        String audioUrl = /*getResources().getString(R.string.recordings_url) +*/ recordingPaths.get(intWordId);
        //Toast.makeText(this, audioUrl,
                             //  Toast.LENGTH_SHORT).show();
        
        boolean isAudioOnline = audioUrl.substring(0, 4).equals("http");
        try {
       	 if(isAudioOnline) { 
             	 mediaPlayer.setDataSource(audioUrl);
             	 mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepareAsync();
             } else {
             	FileInputStream fis = new FileInputStream(audioUrl);
             	mediaPlayer.setDataSource(fis.getFD());
             	//mediaPlayer.setOnErrorListener(this);
             	fis.close();
             	mediaPlayer.prepare();
             	
             }
             // you must call this method after setup the datasource in setDataSource method. 
             // After calling prepare() the instance of MediaPlayer starts load data from URL to internal buffer. 
        } catch (Exception e) {
             e.printStackTrace();
        }

        if(!mediaPlayer.isPlaying()){
           mediaPlayer.start();
            //buttonPlayPause.setImageResource(R.drawable.button_pause);
        }else {
           mediaPlayer.pause();
           //buttonPlayPause.setImageResource(R.drawable.button_play);
        }
    }
    
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
    
    /**
     * BroadcastReceiver to receive info about finished 
     * personalization synchronization process. It enable 
     * to reload words after finished synchronization.
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			Log.w(WordsListFragment.class.getName(), "Personalization Synced Broadcast Received in WordsListFragment.");
			reloadWordsFromDatabase(); 
			Toast.makeText(getActivity(), R.string.words_reloaded_after_syncing, Toast.LENGTH_SHORT).show();
			hideProgressDialog();
		} 
   
    };
    
    protected void hideProgressDialog() {
    	isSyncing = false; 
		ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
    	actionBar.setCustomView(null); 
    }
    
    @Override 
    public void onResume() {
    	super.onResume(); 	
    	// register the broadcast receiver that receives personalizations synchronization finished notification
    	getActivity().registerReceiver(receiver, new IntentFilter(SyncPersonalizationService.PERSONALIZATION_SYNCED_BROADCAST)); 
    	
    	// setting header while resuming app
    	ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
    	actionBar.setTitle(getHeader());
    	actionBar.setSubtitle(null);
    }
    
    @Override 
    public void onPause() {
    	super.onPause(); 
    	Log.w(WordsListFragment.class.getName(), "WordsListFragment.onPause() called...");
    	// unregister the broadcast receiver that receives personalizations synchronization finished notification
    	getActivity().unregisterReceiver(receiver);
    }
    
	@Override
	public void bottom2top(View v) {
		// ...
	}

	@Override
	public void left2right(View v) {
		// ...
	}

	@Override
	public void right2left(final View v) {
		Log.w(WordsListFragment.class.getName(), "Swipe right to left."); 
		Animation translateAnimation = AnimationUtils.loadAnimation(getActivity(),R.anim.horizontal_translation);
		translateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
            	
            	if(actionMode != null || isSyncing) return; 
            	
            	 // hide current item view 
            	 v.setVisibility(View.GONE);
            	 toDeleteWordView = v; 
            	 
            	 // start action mode using defined ActionMode.Callback 
            	 actionMode = ((ActionBarActivity) WordsListFragment.this.getActivity())
            			 			.startSupportActionMode(deleteActionModeCallback);
            	 // when deleting word disable details button
            	 selectedWordId = null; selectedWord = null; 
            	 getActivity().supportInvalidateOptionsMenu();
            	 
            	 /*// display action view with delete confirmation prompt on action bar
            	 actionBar.setCustomView(R.layout.delete_item_actionview);
            	 
            	// ActionBar custom view's buttons event handlers
            	 actionBar.getCustomView().findViewById(R.id.deleteItemActionView)
            	 						  .setOnClickListener( new View.OnClickListener() {
												@Override
												public void onClick(View v) {
													Toast.makeText(getActivity(), "Item deleted!", Toast.LENGTH_SHORT).show();
												} 
            	 						  });
            	 */
            	 /**
            	  *  DEPRECATED: if you want to delete immediately use this:
	              *	 layout.post(new Runnable() {
				  *		@Override
				  *		public void run() {
				  * 		layout.removeView(v);			 
				  *		} 
	              *	 }); 
            	 **/
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
		v.startAnimation(translateAnimation);
	}

	@Override
	public void top2bottom(View v) {
		//...
	}
	
	private ActionMode.Callback deleteActionModeCallback = new ActionMode.Callback() {
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.delete_item_menu, menu);
			return true;
		}
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			actionMode = null;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			
			switch(item.getItemId())
			{
				case R.id.cancelDeletion: 
					if(inDeletionMode) { 
						// WHEN IN MULTIPLE DELETION MODE 
						hideItemCheckboxes(true);
					} else { 
						// WHEN SINGLE SWIPE DELETION
						// return word's item view to screen
						toDeleteWordView.setVisibility(View.VISIBLE); 
						toDeleteWordView = null;
					}
					mode.finish();
					return true; 
				case R.id.confirmDeletion: 
					if(inDeletionMode) { 
						// WHEN IN MULTIPLE DELETION MODE 
						traceCheckedWordsToDelete();
						for(Integer wordId : checkedWords)
							wordIds.remove(String.valueOf(wordId));
						hideItemCheckboxes(true);
						
					} else { 
						// WHEN SINGLE SWIPE DELETION
						// user confirmed word's item deletion
						// 1) delete current word from local database and add to not_synced table 
						Log.w(WordsListFragment.class.getName(), "Tracing current word: " + toDeleteWordView.getTag() + " as to delete.");
						traceCurrentWordToDelete((Integer) toDeleteWordView.getTag());
						wordIds.remove(String.valueOf((Integer) toDeleteWordView.getTag()));
						
						// 2) remove corresponding view from layout
						layout.removeView(toDeleteWordView);
						toDeleteWordView = null;
					}
					mode.finish();
					return true;
					
				default:
					return false; 
			}
		}
	};
	
	public void backPressed() {
		if(toDeleteWordView != null) { 
			// return word's item view to screen
			toDeleteWordView.setVisibility(View.VISIBLE); 
			toDeleteWordView = null;
		}
		
		if(inDeletionMode) {
			hideItemCheckboxes(true);
		}
	}
	
	@Override 
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) 
	{	
		// Find the actionbar's menuItem to add overflow menu sub_menu 
        MenuItem overflowMenuItem = menu.findItem(R.id.menu_overflow);
        // Inflating the sub_menu menu this way, will add its menu items 
        // to the empty SubMenu you created in the XML
        inflater.inflate(R.menu.learning_method_actions, overflowMenuItem.getSubMenu());
        
        inflater.inflate(R.menu.item_details_menu, menu);
		
		// menu.findItem(R.id.menu_logout).setVisible(false);
		menu.findItem(R.id.taskNotificationsBtn).setVisible(false);
		menu.findItem(R.id.itemDetails).setEnabled(false).setVisible(false);
		menu.findItem(R.id.deleteItem).setEnabled(false).setVisible(false);
		
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId())
		{
			case R.id.actionFlashCards:
				startLearningMethod(FlashCardsActivity.class);
				return true; 
			case R.id.actionSimpleRepetition:
				startLearningMethod(SimpleRepetitionActivity.class);
				return true; 
			case R.id.actionPresentation: 
				startLearningMethod(PresentationActivity.class);
				return true; 
			case R.id.actionRepetition: 
				startLearningMethod(RepetitionActivity.class);
				return true; 
			case R.id.actionSpeaking: 
				startLearningMethod(SpeakingActivity.class);
				return true; 
			case R.id.actionListening:
				startLearningMethod(ListeningActivity.class);
				return true; 
			case R.id.actionChoosing:
				startLearningMethod(ChoosingActivity.class);
				return true; 
			case R.id.actionCartons:
				startLearningMethod(CartonsActivity.class);
				return true;
			case R.id.itemDetails:
				Log.w(WordsListFragment.class.getName(), "Word Details: " + selectedWordId + ".");
				showWordDetails();
				return true; 
			case R.id.deleteItem: 
				switchDeletionMode();
				return true;
			default: 
				break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	protected void startLearningMethod(Class<?> learningMethodClass) 
	{
		Intent learningMethodIntent = new Intent(getActivity(), learningMethodClass);
		learningMethodIntent.putExtra(WordsetsListActivity.SELECTED_WORDSET, 0);

		int profileId = Preferences.getInt(getActivity(), Preferences.KEY_PROFILE_ID, 0);
		if(getWordsetType() == WordsetType.FORGOTTEN_WORDSET && profileId == 0 && !areAllWordsInDatabase) {
			learningMethodIntent.putExtra(WordsetType.KEY_TYPE, WordsetType.SELECTED_WORDS);
			learningMethodIntent.putExtra(WordsetActivityOld.KEY_SELECTED_WORD_IDS, wordIds);
		} else { 
			learningMethodIntent.putExtra(WordsetType.KEY_TYPE, getWordsetType());
		}
     	startActivity(learningMethodIntent);
	}
	
	@Override 
	public void onPrepareOptionsMenu(Menu menu)
	{
		menu.findItem(R.id.taskNotificationsBtn).setVisible(false);
		// menu.findItem(R.id.menu_logout).setVisible(false);
		
		if(selectedWordId != null && !isSyncing) { 
			menu.findItem(R.id.itemDetails).setEnabled(true).setVisible(true);
			menu.findItem(R.id.deleteItem).setEnabled(true).setVisible(true);
		} else { 
			menu.findItem(R.id.itemDetails).setEnabled(false).setVisible(false);
			menu.findItem(R.id.deleteItem).setEnabled(false).setVisible(false);
		}
	}
	
	/**
	 * This is helper method that switches on deletion mode
	 * where user can select to delete items and confirm their deletion or not!
	 */
	private void switchDeletionMode() {
		
		// initialize deletion mode
		inDeletionMode = true; 
		clearItemCheckboxes();
		
		// add check boxes to all word items, and select check box for current word item
		showItemCheckboxes();
		
		// start action mode using defined ActionMode.Callback 
		actionMode = ((ActionBarActivity) getActivity())
   			 					.startSupportActionMode(deleteActionModeCallback);
   	 	// when deleting word disable details button
   	 	selectedWordId = null; selectedWord = null; 
   	 	getActivity().supportInvalidateOptionsMenu();
	}
	
	/**
	 * This is helper method that shows check boxes on each word item
	 * and select check box for currently selected item. User can then selects
	 * other item he want to delete.
	 */
	private void showItemCheckboxes() {
		
		// iterating over all word items in scroll view layout
		for(int i=0; i < layout.getChildCount(); i++) { 
			 View v = layout.getChildAt(i);
			 if(v instanceof RelativeLayout) { 
				 Log.w(WordsListFragment.class.getName(), "This is " + i + "th word item.");
				 CheckBox itemCheckBox= (CheckBox) v.findViewById(R.id.wordCheckbox);
				 itemCheckBox.setVisibility(View.VISIBLE);
				 if(v.isFocused()) { 
					 itemCheckBox.setChecked(true);
				 }
			 } else { 
				 Log.w(WordsListFragment.class.getName(), "Child view no " + i + " isn't word item.");
			 }
	    	 
		}
	}
	
	/**
	 *  Helper method that clears all word items check boxes selections
	 */
	private void clearItemCheckboxes() { 
		
		checkedWords.clear();
		
		// iterating over all word items in scroll view layout
		for(int i=0; i < layout.getChildCount(); i++) { 
			 View v = layout.getChildAt(i);
			 if(v instanceof RelativeLayout) { 
				 Log.w(WordsListFragment.class.getName(), "This is " + i + "th word item.");
				 CheckBox itemCheckBox= (CheckBox) v.findViewById(R.id.wordCheckbox);
				 itemCheckBox.setChecked(false); 
			} else { 
				Log.w(WordsListFragment.class.getName(), "Child view no " + i + " isn't word item.");
			}
		}
	}
	
	/**
	 *  Helper method that for each word item turns off its check box 
	 */
	private void hideItemCheckboxes(boolean clear) { 
		inDeletionMode = false; 
		
		// iterating over all word items in scroll view layout
		for(int i=0; i < layout.getChildCount(); i++) { 
				View v = layout.getChildAt(i);
				if(v instanceof RelativeLayout) { 						 
					 Log.w(WordsListFragment.class.getName(), "This is " + i + "th word item.");
					 CheckBox itemCheckBox= (CheckBox) v.findViewById(R.id.wordCheckbox);
					 if(clear) itemCheckBox.setChecked(false);
					 itemCheckBox.setVisibility(View.GONE); 
				} else { 
					Log.w(WordsListFragment.class.getName(), "Child view no " + i + " isn't word item.");
				}
		}
		
		if(clear)
			checkedWords.clear();
	}
	
	/**
	 * Handler that creates context menu that is shown after long click on word item.
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		  
		 super.onCreateContextMenu(menu, v, menuInfo);
	         
	     menu.setHeaderTitle("Word: " + ((TextView)v.findViewById(R.id.foreignWord)).getText());

	     menu.add(Menu.NONE, DETAILS_ITEM_ID, Menu.NONE, getString(R.string.details_action));
	     menu.add(Menu.NONE, SHARE_ITEM_ID, Menu.NONE, "Share"); 
	     menu.add(Menu.NONE, COPY_FOREIGN_ITEM_ID, Menu.NONE, "Copy " + ((TextView)v.findViewById(R.id.foreignWord)).getText() );
	     menu.add(Menu.NONE, COPY_NATIVE_ITEM_ID, Menu.NONE, "Copy " + ((TextView)v.findViewById(R.id.nativeWord)).getText() );
	     menu.add(Menu.NONE, COPY_TRANSCRIPTION_ITEM_ID, Menu.NONE, "Copy " + ((TextView)v.findViewById(R.id.transcription)).getText() );
	     menu.add(Menu.NONE, DELETE_ITEM_ID, Menu.NONE, "Delete");
	}
	
	@Override
	  public boolean onContextItemSelected(MenuItem item) {
		  Log.w(WordsListFragment.class.getName(), "Context menu wordID: " + selectedWordId); 
	      // menu handling code here
		  switch (item.getItemId()) {
		    case DETAILS_ITEM_ID:
		    	showWordDetails();
		        return true;
		        
		    case DELETE_ITEM_ID:
		    	// 1) trace current word as to delete
		    	Log.w(WordsListFragment.class.getName(), "Tracing current word: " + selectedWordId + " as to delete.");
		    	traceCurrentWordToDelete(selectedWordId);
				
				// 2) remove corresponding view from layout
				layout.removeView(layout.findViewById(selectedWordId));
				selectedWordId = null;
				selectedWord = null;
		        return true;
		        
		    case SHARE_ITEM_ID: 
		    	shareWord(selectedWordId); 
		    	return true; 
		    	
		    case COPY_FOREIGN_ITEM_ID: 
		    	CopyUtility.copyText(getActivity(), selectedWord.getForeignWord());
		    	return true; 
		    	
		    case COPY_NATIVE_ITEM_ID: 
		    	CopyUtility.copyText(getActivity(), selectedWord.getNativeWord());
		    	return true; 
		    	
		    case COPY_TRANSCRIPTION_ITEM_ID: 
		    	CopyUtility.copyText(getActivity(), selectedWord.getTranscription());
		    	return true; 
		    	
		    default:
		        return super.onContextItemSelected(item);
		   }
	  }
	
	private void shareWord(int wordId) {
		  
	    Intent shareCaptionIntent = new Intent(Intent.ACTION_SEND);

	    //set photo
	    if(selectedWord.getImageBitmap() != null) { 
		    String bitmapPath = Images.Media.insertImage(getActivity().getContentResolver(), 
		    									selectedWord.getImageBitmap(), selectedWord.getForeignWord(), null);
		    Uri bitmapUri = Uri.parse(bitmapPath);
		    shareCaptionIntent.setType("*/*");
		    shareCaptionIntent.setData(bitmapUri);
		    shareCaptionIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
	    }

	    //set caption
	    shareCaptionIntent.putExtra(Intent.EXTRA_TITLE, selectedWord.getForeignWord() + " - " + selectedWord.getNativeWord());
	    String shareText = selectedWord.getForeignWord() + " - " + selectedWord.getNativeWord() + "\n" + "link: http://mnemobox.com/dict/" + selectedWord.getForeignWord();
	    shareCaptionIntent.putExtra(Intent.EXTRA_TEXT, shareText);
	    shareCaptionIntent.putExtra(Intent.EXTRA_SUBJECT, selectedWord.getForeignWord() + " - " + selectedWord.getNativeWord());
	    
	    startActivity(Intent.createChooser(shareCaptionIntent,"Share"));
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		
		// getting access to word item and assigned to it word id
		Integer wordId = ((RelativeLayout) buttonView.getParent()).getId();
		if(isChecked) { 
			Log.w(WordsListFragment.class.getName(), "Word item with id: " + wordId + " has been checked.");
			checkedWords.add(wordId);
		} else { 
			Log.w(WordsListFragment.class.getName(), "Word item with id: " + wordId + " has been unchecked.");
			checkedWords.remove(wordId);
		}	
	}
	
	
	/**
	 * Detecting scroll view scrolling stop and reloading visible word item views
	 *
	 */
	public interface OnScrollStoppedListener {
	    public void onScrollStopped();
	}
	
	private OnScrollStoppedListener onScrollStoppedListener;
	private int scrollY = 0; 
	private int taskDelay = 200;
	private Runnable scrollerTask;
	private Handler handler = new Handler(); 
	
	private void startScrollerTask() {
		scrollY = scrollview.getScrollY();
		handler.removeCallbacksAndMessages(null);
	    handler.postDelayed(scrollerTask, taskDelay);
	}
	
	protected void addScrollViewListener() { 
		
		scrollerTask = new Runnable() {

	        public void run() {

	            int newScrollY = scrollview.getScrollY(); 
	            if(scrollY - newScrollY == 0) {

	                if(onScrollStoppedListener != null){
	                    onScrollStoppedListener.onScrollStopped();
	                } 
	            } /* else {
	                    scrollY = scrollview.getScrollY();
	                    scrollview.postDelayed(scrollerTask, taskDelay);
	                	Log.w(WordsListFragment.class.getName(), "scrollerTask scrolling...");
	            } */
	        }
		};
		
		setOnScrollStoppedListener(new OnScrollStoppedListener() {

			@Override
			public void onScrollStopped() {
				
				if(!isAdded()) return;
				
				int firstVisible = -1; 
				int lastVisible = -1; 
				
				for(int i=0; i< layout.getChildCount(); i++) { 
					Rect scrollBounds = new Rect();
	    			scrollview.getHitRect(scrollBounds);
					if (layout.getChildAt(i).getLocalVisibleRect(scrollBounds)) {
						if(firstVisible == -1) firstVisible = i; 
						else lastVisible = i; 
					} else {
						ImageView wordImage = (ImageView) layout.getChildAt(i).findViewById(R.id.wordImage);
		    			if(wordImage.getTag() == null) continue;  // skip database loaded images
	    			    // wordItemView is not within the visible window
	    				Drawable drawable = wordImage.getDrawable();
	    				if (drawable instanceof BitmapDrawable) {
	    				    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
	    				    Bitmap bitmap = bitmapDrawable.getBitmap();
	    				    if(bitmap != null) bitmap.recycle();
	    				}
	    				wordImage.setImageBitmap(null); 
	    			}
				}
				
				int startVisible = (firstVisible - 5) > 0 ? (firstVisible -5) : 0;
				int endVisible = (lastVisible + 5) < layout.getChildCount() ? (lastVisible + 5) : layout.getChildCount(); 
		        
		    	for(int i=startVisible; i< endVisible; i++) { 
		    		ImageView wordImage = (ImageView) layout.getChildAt(i).findViewById(R.id.wordImage);
	    			if(wordImage.getTag() == null) continue;  // skip database loaded images
	    			
		    		// wordItemView is within the visible window
		    		Log.w(WordsListFragment.class.getName(), "Word image on screen: " + (String) wordImage.getTag());
		    		
		    		if(wordImage.getTag().equals(IMAGE_FROM_BLOB)) { 
		    			// load image from HashMap byte[] array
		    			Integer wordId = (Integer) ((ViewGroup) wordImage.getParent()).getTag();
		    			Log.w(WordsListFragment.class.getName(), "Loading local image for word with id: " + wordId); 
		    			byte[] image = imagesData.get(wordId);
		    			Bitmap imageBitmap = null;
		    			if(image != null) { 
		    				imageBitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
		    			}
		    			wordImage.setImageBitmap(imageBitmap);
		    			
		    		} else { 
		    			// load image asynchronously from web service 
		    			UrlImageViewHelper.setUrlDrawable(wordImage, 
		    					getResources().getString(R.string.images_url) + (String) wordImage.getTag());
		    		}
		    	}
			} 
			
		});
		
		
		scrollview.getViewTreeObserver().addOnScrollChangedListener(new OnScrollChangedListener() {

		    @Override
		    public void onScrollChanged() {
		    	
		    	startScrollerTask();
		    }
		});
	
	}
	
	private void setOnScrollStoppedListener(OnScrollStoppedListener listener) 
	{
		onScrollStoppedListener = listener; 
	}
	
	@Override
	public void onDestroyView() {
		
		handler.removeCallbacksAndMessages(null);
		
		for(int i=0; i< layout.getChildCount(); i++) { 
			ImageView wordImage = (ImageView) layout.getChildAt(i).findViewById(R.id.wordImage);
			if(wordImage.getTag() == null) continue;  // skip database loaded images
		    // wordItemView is not within the visible window
			Drawable drawable = wordImage.getDrawable();
			if (drawable instanceof BitmapDrawable) {
			    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			    Bitmap bitmap = bitmapDrawable.getBitmap();
			    if(bitmap != null) bitmap.recycle();
			}
			wordImage.setImageBitmap(null); 
		}
		
		super.onDestroyView();
		
	}
	
}
