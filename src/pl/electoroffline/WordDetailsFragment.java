package pl.electoroffline;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import pl.elector.database.SentenceProvider;
import pl.elector.database.WordProvider;
import pl.elector.service.WordsLoaderService;
import pl.electoroffline.WordObject.SentenceObject;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class WordDetailsFragment extends Fragment implements OnPreparedListener, OnBufferingUpdateListener, OnCompletionListener, OnErrorListener {  
	
	public static final String TAG = "WORD_DETAILS_FRAGMENT_TAG"; 
	
	private View view; 
	
	private float scale; 
	private ListView sentencesListView; 
	private SentencesAdapter adapter;
	
	private int wordId;
	private WordObject wordObject; 
	
	@Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		view = inflater.inflate(R.layout.word_details, container, false);
		
		createLayout();
		
		if(savedInstanceState != null) {
        	wordObject = (WordObject) savedInstanceState.getSerializable(WordDetailsActivity.KEY_WORD_OBJECT);
        	restoreWordObject();
        } else {
	        // load word
	        loadWord();
        }
		
		return view; 
	}
	
	/* // Called when the activity is first created. 
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_details);
        
        createLayout();
        
        if(savedInstanceState != null) {
        	wordObject = (WordObject) savedInstanceState.getSerializable(KEY_WORD_OBJECT);
        	restoreWordObject();
        } else {
	        // load word
	        loadWord();
        }
    }*/
    
    @Override 
    public void onSaveInstanceState(Bundle outState) { 
    	 super.onSaveInstanceState(outState);
    	 
    	 if(wordObject != null) { 
    		 outState.putSerializable(WordDetailsActivity.KEY_WORD_OBJECT, wordObject);
    	 }
    }
    
    private void restoreWordObject() { 
    	displayWordDetails();
    	displaySentences();
    }
    
    /**
     * Helper method that creates and adjust layout
     */
    private void createLayout() {
    	ActionBar actionBar = ((ActionBarActivity) this.getActivity()).getSupportActionBar();
   		actionBar.setTitle(getString(R.string.word_details));
   		actionBar.setSubtitle(null);
   		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_HOME_AS_UP 
   								| ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
   		
   		scale = getResources().getDisplayMetrics().density;
   		
   		// set audio button position on the screen
   		// adjustAudioButtonPosition();
   		
   		setHasOptionsMenu(true);
   		
    }
    
    /**
     * Helper method that adjusts audio button position on the top left side of the screen
     */
    private void adjustAudioButtonPosition() {
    	ImageView audioButton = (ImageView) view.findViewById(R.id.audioBtn);
    	DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = displaymetrics.widthPixels;
   		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
   		int px = (int) (1*scale + 0.5f);
   		layoutParams.leftMargin = width-px*75;
        layoutParams.topMargin = px*27;
        audioButton.setLayoutParams(layoutParams);
    }
    
    /**
     * Helper method that loads word details 
     */
    private void loadWord() {
    	
    	// accessing wordId 
    	wordId = getActivity().getIntent().getExtras().getInt(WordDetailsActivity.KEY_WORD_ID, 0);
    	
    	if(wordId == 0)  { 
    		Toast.makeText(getActivity(), R.string.word_id_bad, Toast.LENGTH_SHORT).show();
    		getActivity().finish();
    	}
    	
    	// checking whether user prefer to use ONLINE or OFFLINE access to words
    	if(Preferences.getBoolean(getActivity(), Preferences.KEY_PREFER_ONLINE_DATA, false)) 
		{
    		// user prefer online access to data 
    		// we must check whether network connection is available
    		if(NetworkUtilities.haveNetworkConnection(getActivity())) {
    			
    			// 3) load word details from web service 
    			loadWordDetailsFromWebService(); 
    			return; 
    		}
		}
    	
    	loadWordDetailsFromLocalStorage();
    	
    }

    /**
     * Helper method that load word details from local storage
     */
    private void loadWordDetailsFromLocalStorage() {
		
    	Log.w(WordDetailsFragment.class.getName(), "Loading word details from local storage...");
    	
    	Uri WORD_CONTENT_URI = Uri.parse(WordProvider.CONTENT_URI + "/" + wordId); 
    	String[] projection = { WordProvider.WordTable.COLUMN_WORD_ID, 
    							WordProvider.WordTable.COLUMN_FOREIGN_WORD,
    							WordProvider.WordTable.COLUMN_NATIVE_WORD, 
    							WordProvider.WordTable.COLUMN_TRANSCRIPTION,
    							WordProvider.WordTable.COLUMN_IMAGE,
    							WordProvider.WordTable.COLUMN_RECORDING, 
    						  }; 
    	Cursor cursor = getActivity().getContentResolver().query(WORD_CONTENT_URI, projection, null, null, null);
    	
        if(cursor.moveToFirst()) { 
        	
        	String foreignWord = cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_FOREIGN_WORD));
        	String nativeWord = cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_NATIVE_WORD));
        	String transcription = cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_TRANSCRIPTION));
        	// getting image from BLOB stored in Word table.
        	byte[] image = cursor.getBlob(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_IMAGE));
        	Bitmap imageBitmap = null; 
        	if(image != null)
        		imageBitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
        	String recording = cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_RECORDING)); 	
        	
        	// create manually constructed WordObject
        	wordObject = new WordObject(); 
        	wordObject.setWordId(wordId);
        	wordObject.setForeignWord(foreignWord);
        	wordObject.setNativeWord(nativeWord);
        	wordObject.setImageBitmap(imageBitmap);
        	wordObject.setTranscription(transcription);
        	wordObject.setRecording(recording);
        	
        	cursor.close();
        	
        	displayWordDetails();
        	
        	loadSentencesFromLocalStorage();
        	
        } else { 
        	cursor.close();
        	Toast.makeText(getActivity(), R.string.cannot_find_word_details_toast, Toast.LENGTH_SHORT).show();
        	tryLoadWordDetailsFromWebService();
        }
	}
    
    /**
     * Helper method that after loading word basic details loads its sentences
     */
    private void loadSentencesFromLocalStorage() 
    {
    	
    	Uri WORD_SENTENCES_CONTENT_URI = Uri.parse(SentenceProvider.CONTENT_URI + "/word/" + wordId);
    	String[] projection = { SentenceProvider.SentenceTable.COLUMN_SENTENCE_ID,
    							SentenceProvider.SentenceTable.COLUMN_FOREIGN_SENTENCE,
    							SentenceProvider.SentenceTable.COLUMN_NATIVE_SENTENCE,
    							SentenceProvider.SentenceTable.COLUMN_RECORDING
    						  };
    	Cursor cursor = getActivity().getContentResolver().query(WORD_SENTENCES_CONTENT_URI, projection, null, null, null);
    	
    	while(cursor.moveToNext()) { 
    		
    		int sentenceId = cursor.getInt(cursor.getColumnIndexOrThrow(SentenceProvider.SentenceTable.COLUMN_SENTENCE_ID));
    		String foreignSentence = cursor.getString(cursor.getColumnIndexOrThrow(SentenceProvider.SentenceTable.COLUMN_FOREIGN_SENTENCE));
    		String nativeSentence = cursor.getString(cursor.getColumnIndexOrThrow(SentenceProvider.SentenceTable.COLUMN_NATIVE_SENTENCE));
    		String sentenceRecording = cursor.getString(cursor.getColumnIndex(SentenceProvider.SentenceTable.COLUMN_RECORDING));
    		
    		foreignSentence = foreignSentence.replaceAll("\\\\", "");
    		nativeSentence = nativeSentence.replaceAll("\\\\", "");
    		wordObject.addSentence(sentenceId, foreignSentence, nativeSentence, sentenceRecording);
    		
    		Log.w(WordDetailsFragment.class.getName(), "Sentence: " + foreignSentence); 
    	}
    	
    	displaySentences();
    }

    /**
     * Helper method that display word's sentences in the ListView
     */
    private void displaySentences() {
    	// retrieving word's sentences
    	ArrayList<SentenceObject> sentences = wordObject.getSentences();
    	// if there isn't any sentence just return 
    	if(sentences.isEmpty()) return; 
    	
    	sentencesListView = (ListView) view.findViewById(R.id.sentencesListView); 
    	view.findViewById(R.id.noSentencesLabel).setVisibility(View.GONE);
    	sentencesListView.setVisibility(View.VISIBLE); 
    
    	adapter = new SentencesAdapter(getActivity(), sentences); 
    	sentencesListView.setAdapter(adapter);
		
	}

	/**
     * Helper method that tries load word details from online web service 
     * while they haven't been found locally.
     */
	private void tryLoadWordDetailsFromWebService() {
	
		if(NetworkUtilities.haveNetworkConnection(getActivity())) { 
			loadWordDetailsFromWebService();
		} else { 
			promptTurnOnNetworkToOnlineAccess(); 
		}
		
	}

	/**
	 *  Helper method that prompts user to turn on network connection in order to access word details online 
	 */
	private void promptTurnOnNetworkToOnlineAccess() {
		
		// create dialog that prompts user if he wants 
		// to access word details online in order for him to
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
											loadWordDetailsFromWebService();  
									} else { 
										Toast.makeText(getActivity(), getString(R.string.cannot_load_word_details_toast), Toast.LENGTH_SHORT).show();
							    		getActivity().finish();
									}
									
								}
							})
							.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									Toast.makeText(getActivity(), getString(R.string.word_details_loading_cancelled_toast), Toast.LENGTH_SHORT).show();
						    		getActivity().finish();
								}
							})
							.create();

		dialog.show();
	}

	/**
     * Helper method that loads word details from online web service
     */
	private void loadWordDetailsFromWebService() {	
		
		// NOT TESTED YET
		wordObject = new WordObject(wordId, getString(R.string.native_code_lower), getString(R.string.foreign_code_lower), getActivity());
		displayWordDetails();
		displaySentences();
	}
	
	/**
	 * Helper method that displays loaded word details on screen 
	 */
	private void displayWordDetails() { 
		// getting access to views
		TextView foreignTextView = (TextView) view.findViewById(R.id.foreignWord);
		TextView nativeTextView = (TextView) view.findViewById(R.id.nativeWord); 
		TextView transcriptionTextView = (TextView) view.findViewById(R.id.transcription); 
		ImageView wordImageView = (ImageView) view.findViewById(R.id.wordImage); 
		ImageView audioButton = (ImageView) view.findViewById(R.id.audioBtn);
		
		// setting word basics on views
		foreignTextView.setText(wordObject.getForeignWord());
		nativeTextView.setText(wordObject.getNativeWord());
		if(DrawerActivity.isRTL())  {
	       	SpannableStringBuilder styledTranscription = new SpannableStringBuilder(wordObject.getTranscription());
	       	styledTranscription.setSpan (
	       			new BackgroundColorSpan(getActivity().getResources().getColor(R.color.lightBlue)), 0, wordObject.getTranscription().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	       	transcriptionTextView.setText(styledTranscription);
	    } else { 
	    	transcriptionTextView.setText(wordObject.getTranscription());
	    }
		 
		int px = (int) (1*scale + 0.5f);
		if(wordObject.getImageBitmap() != null) 
			wordImageView.setImageBitmap(BitmapUtilities.fitSize(wordObject.getImageBitmap(), 0, 150*px));
		audioButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				playWordRecording(); 
			}

			
		});
		
		
		// uncovering hidden word basics layout 
		view.findViewById(R.id.wordBasics).setVisibility(View.VISIBLE); 
	}
	
	private void playWordRecording() {
		
		// concatenating suitable recording path
		String recordingPath = recordingPath(wordObject.getWordId(), wordObject.getRecording());
		
		MediaPlayer mediaPlayer = new MediaPlayer();     
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this); 
        
        String audioUrl = /*getResources().getString(R.string.recordings_url) +*/ recordingPath;
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
             	mediaPlayer.setOnErrorListener(this);
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
	
	/** 
     * Helper function that makes paths online/local to recordings. 
     * @param wordId
     * @param recordingName
     */
    private String recordingPath(int wordId, String recordingName) { 
           
   	 if(Preferences.getBoolean(getActivity(), Preferences.KEY_PREFER_TO_DOWNLOAD_AUDIO, true))
   	 {
   		 // user prefers to download audio and play it from local storage 
   		 File path = new File(FileUtilities.getExternalFilesDir(getActivity()), "recordings"); 
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
      Log.w(WordDetailsFragment.class.getName(), recordingPath); 
      
      return recordingPath; 
   }
	
	private void playSentenceRecording(SentenceObject sentenceObject) { 
		
	}
	
	private class SentencesAdapter extends BaseAdapter {
		
		private static final int sentenceItemPagerResource = R.layout.sentence_item_pager;
		private static final int sentenceItemResource = R.layout.sentence_item;
		
		private LayoutInflater inflater;
		private Activity activity; 
		private ArrayList<SentenceObject> sentences;
		
		class Holder { 
		    ViewPager sentencePager;
		}

		public SentencesAdapter(Activity activity, ArrayList<SentenceObject> sentences) {
			
			this.activity = activity; 
			this.sentences = sentences;
			inflater = activity.getLayoutInflater();
		}
		
		
		@Override 
		public long getItemId(int position) {
			
			SentenceObject sentenceObject = (SentenceObject) getItem(position);
			
			return sentenceObject.getSentenceId();
		}
		
		@Override 
		public boolean hasStableIds() {
			return true; 
		}
		
		@Override 
		public View getView(int position, View convertView, ViewGroup parent) {
		   Holder holder = null; 
		   SentenceObject sentenceObject = (SentenceObject) getItem(position);
			
		   if(convertView == null) { 
				
				convertView = inflater.inflate(R.layout.sentence_item_pager, null);
				holder = new Holder(); 
				holder.sentencePager = (ViewPager) convertView.findViewById(R.id.sentenceItemPager);
		        convertView.setTag(holder);
			   
		   } else { 
			   holder = (Holder) convertView.getTag();
		   }
		   
		   SentencePagerAdapter sentencePagerAdapter = new SentencePagerAdapter(sentenceObject);  
		   holder.sentencePager.setAdapter(sentencePagerAdapter);
			
			return convertView; 
		}

		@Override
		public int getCount() {
			
			return sentences.size();
		}

		@Override
		public Object getItem(int position) {
			
			return sentences.get(position);
		}
	}
		
		public class SentencePagerAdapter extends PagerAdapter {
			
			private SentenceObject sentenceObject;
			private LinearLayout sentenceLayout; 
			private TextView sentenceText; 
			private LayoutInflater inflater;
			
			public SentencePagerAdapter(SentenceObject sentenceObject) {
				this.sentenceObject = sentenceObject; 
				inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				sentenceLayout = (LinearLayout) inflater.inflate(R.layout.sentence_item, null); 
				sentenceText = (TextView) sentenceLayout.findViewById(R.id.sentenceText);
				
				Log.w(SentencePagerAdapter.class.getName(), sentenceObject.getForeignSentence());
			}

			@Override
			public int getCount() {
				
				return 2;
			}

			@Override
			public boolean isViewFromObject(View view, Object object) {
				
				return view == object;
			} 
			
			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				
				sentenceLayout = (LinearLayout) inflater.inflate(R.layout.sentence_item, null);
				sentenceText = (TextView) sentenceLayout.findViewById(R.id.sentenceText); 
				
				switch(position) {
					
					case 0: // show foreign sentence 
						sentenceText.setText(sentenceObject.getForeignSentence());
						sentenceLayout.setOnClickListener(new View.OnClickListener() {
							
							@Override
							public void onClick(View v) {
								playSentenceRecording(sentenceObject); 
							}
						});
						break; 
					case 1: // show native sentence
						sentenceText.setText(sentenceObject.getNativeSentence()); 
						sentenceLayout.setOnClickListener(null); 
						break; 
				}
				
				Log.w(SentencePagerAdapter.class.getName(), "instantiate item: " + sentenceText.getText());
				container.addView(sentenceLayout, 0);
				
				return sentenceLayout;
			}
			
			@Override
			public void destroyItem(ViewGroup container, int position, Object object) {
					container.removeView((LinearLayout)object);
			}
			
		}

		@Override
		public void onPrepared(MediaPlayer mp) {	
			mp.start();
		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onCompletion(MediaPlayer mp) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			// TODO Auto-generated method stub
			
		}
		
		@Override 
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) 
		{
			inflater.inflate(R.menu.word_details_menu, menu);
			super.onCreateOptionsMenu(menu, inflater);
			
		}
		
	MenuItem syncMenuItem = null;
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
			 
			 switch(item.getItemId())
			 {
			 	case R.id.detailsSyncBtn: 
			 		syncMenuItem = item; 
				    syncWord();
			 		return true;
			 	default:
			 		break;
			 }
			 
			 return super.onOptionsItemSelected(item);
	}
		
	private void syncWord() {  		
		    showProgressDialog();
		    new WordSyncTast().execute();
	}
	
	private boolean isSyncing = false; 
		 
	private void showProgressDialog() {
		   	isSyncing = true; 
		   	
		   	MenuItemCompat.setActionView(syncMenuItem, R.layout.progressbar);
		   	MenuItemCompat.expandActionView(syncMenuItem);
		
   }

	protected void hideProgressDialog() {
    	isSyncing = false; 
		
    	MenuItemCompat.collapseActionView(syncMenuItem);
    	MenuItemCompat.setActionView(syncMenuItem, null);
    }
	
	private class WordSyncTast extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			
			WordsLoaderService.WordsDownloader.downloadWordDetails(getActivity(), wordId);
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			hideProgressDialog();
		}
		
	}
	
}