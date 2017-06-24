/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;
import java.util.ArrayList;

import pl.elector.database.ForgottenProvider;
import pl.elector.database.WordProvider;
import pl.elector.database.WordsetType;
import pl.electoroffline.Personalization.Mood;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.widget.Toast;
/**
 *
 * @author Michał Ziobro 
 */
public class ForgottenFragment extends WordsListFragment {
	
	public static final String TAG = "FORGOTTEN_FRAGMENT_TAG"; 
	private String HEADER = "Forgotten Words"; 
	private boolean notAskedToSyncWords = true;
	
	private WordsLoader wordsLoader; 
	
	protected boolean isSyncAskEnabled()
	{
		return false;
	}
	
	@Override 
	protected String getHeader() {
		return getString(R.string.forgotten_header); 
	}
	
	@Override
	protected WordsetType getWordsetType() { 
		return WordsetType.FORGOTTEN_WORDSET; 
	}
	
	@Override
	protected void traceCurrentWordToDelete(int wordId) { 
		Personalization p = new Personalization(getActivity()); 
		p.traceForgottenWord(wordId, Mood.GOOD); // Mood.GOOD trigger word deletion from forgottenTable!
	}
	
	@Override
	protected void traceCheckedWordsToDelete() {
		Personalization p = new Personalization(getActivity()); 
		for(int wordId : checkedWords) { 
			p.traceForgottenWord(wordId, Mood.GOOD); // Mood.GOOD trigger word deletion from forgottenTable!
			//remove corresponding view from layout
			layout.removeView(layout.findViewById(wordId)); 
		}
	}
    
	/**
     * Helper method that loads word_ids for current user 
     * (also anonymous user if user is not logged in) 
     * from forgottenTable and next associated words details
     * from local storage and online web service if they don't exist 
     * in local database. 
     */
	@Override
    protected void loadWordsFromDatabase() {
    	
    	// loading word_ids from SQL database 
    	Uri FORGOTTEN_FOR_PROFILE_URI = Uri.parse(ForgottenProvider.CONTENT_URI + "/profile/" + profileId); 
    	String[] projection = { ForgottenProvider.ForgottenTable.COLUMN_WORD_ID }; 
    	Cursor cursor = getActivity().getContentResolver().query(FORGOTTEN_FOR_PROFILE_URI, projection, null, null, null); 
    	
    	// if cursor is not null constructing IN selection argument for query on word table 
    	if(cursor.getCount() > 0) { 
    		
    		ArrayList<String> wordIds = new ArrayList<String>(); //forgotten wordIds list 
    		StringBuilder sb = new StringBuilder(); 
    		sb.append("("); 
    		
    		while(cursor.moveToNext()) { 
    			
    			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(ForgottenProvider.ForgottenTable.COLUMN_WORD_ID));
    			wordIds.add(String.valueOf(wordId));
    			sb.append("?,"); 
    		}
    		
    		if(sb.length() > 1) {
    			sb.setLength(sb.length()-1); 
    		}
    		
    		sb.append(")"); 
    		
    		cursor.close(); 
    		
    		// Querying word table for words with wordIds in selection argument
    		projection = null;
    		String selection = WordProvider.WordTable.COLUMN_WORD_ID + " IN " + sb.toString(); 
    		String[] selectionArgs = wordIds.toArray(new String[] {});
    		 
    		cursor = getActivity().getContentResolver().query(WordProvider.CONTENT_URI, null, selection, selectionArgs, null); 
    		
    		while(cursor.moveToNext()) {
    			
    			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_WORD_ID));
    			String foreignWord = cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_FOREIGN_WORD));
    			String nativeWord = cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_NATIVE_WORD));
    			String transcription = cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_TRANSCRIPTION));
    			String recordingName = cursor.getString(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_RECORDING));
    			byte[] image = cursor.getBlob(cursor.getColumnIndexOrThrow(WordProvider.WordTable.COLUMN_IMAGE));
    			if(image == null) image = new byte[] {};
    			
    			imagesData.put(wordId, image); 
    			
    			makeButton(foreignWord, nativeWord, transcription, wordId);
    			makeAudio(wordId, recordingName); 
    			makeImageView(wordId, IMAGE_FROM_BLOB);
    			
    			// delete current word's word_id from ArrayList 
    			wordIds.remove(String.valueOf(wordId));
    		}
    		
    		// Checking if there are word_ids for which word details aren't stored locally 
    		if(wordIds.size() > 0 ) { 
    			areAllWordsInDatabase = false; 
    			// Remaining word details need to be loaded from online web service
    			if(!NetworkUtilities.haveNetworkConnection(getActivity())) { 
    				// if there isn't network connection ask user if he want to connect with it
    				askToConnectWithInternetToLoadWords(wordIds);
    			} else { 
    				loadRemainingWordsFromWebService(wordIds);
    			}
    		} else { 
    			// all forgotten words has been loaded from local database
    			Toast.makeText(getActivity(), getString(R.string.all_forgotten_words_loaded), Toast.LENGTH_SHORT).show();
    		}
    	} else { 
    		// there is no forgotten words stored locally, 
    		// local database potentially needs previous synchronization
    		Toast.makeText(getActivity(), getString(R.string.no_forgotten_words_found), Toast.LENGTH_LONG).show(); 
    		return;
    	}
    }
	
	/**
	 * Helper method that loads for remaining word_ids its word details.
	 * from online web service. 
	 * @param wordIds
	 */
	@SuppressWarnings("unchecked")
	private void loadRemainingWordsFromWebService(ArrayList<String> wordIds) 
	{
		Toast.makeText(getActivity(), getString(R.string.some_forgotten_loading_toast), Toast.LENGTH_SHORT).show();
		wordsLoader = (WordsLoader) new WordsLoader().execute(wordIds); 
		showProgressDialog();
	}
	
	/**
	 * Helper method that ask the user to connect with Internet in order to
	 * load remaining forgotten words from online web service.
	 * @param wordIds
	 */
	private void askToConnectWithInternetToLoadWords(final ArrayList<String> wordIds)
	{
		AlertDialog dialog = new AlertDialog.Builder(getActivity())
		.setMessage(getString(R.string.ask_to_connect_forgotten_words, wordIds.size()) )
		.setCancelable(false)
		.setPositiveButton(R.string.connect_now,  new DialogInterface.OnClickListener() {
			
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
					loadRemainingWordsFromWebService(wordIds);
				} else { 
					// When couldn't connect to the Internet run in emergency mode
					Log.w(ForgottenFragment.class.getName(), "Trying to connect to the Internet failed! Emergency Mode.");
					Toast.makeText(getActivity(), getString(R.string.forgotten_words_connection_failed, wordIds.size()), Toast.LENGTH_LONG).show();
				}
			}
		})
		.setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
					Toast.makeText(getActivity(), getString(R.string.forgotten_words_no_button, wordIds.size()), Toast.LENGTH_LONG).show();
			}
		})
		.create();

    	dialog.show();
	}
	
	
	private class WordsLoader extends AsyncTask<ArrayList<String>, Integer, Boolean> 
	{
		@Override
		protected void onPreExecute(){
			
		}

		@Override
		protected Boolean doInBackground(ArrayList<String>... params) {
			
			int count = params[0].size();
			
			String nativeCode = Preferences.getAccountPreferences(getActivity())
					.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, getString(R.string.native_code_lower));
			String foreignCode = Preferences.getAccountPreferences(getActivity())
					.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, getString(R.string.foreign_code_lower));
			
			//getting wordsIds comma separated list from array list
			String wordIds = params[0].toString().replaceAll(" ", "").replace("[","").replace("]", "");
			Log.w(ForgottenFragment.class.getName(), "Words ids: " + wordIds );
			
			// creating URL to get all remaining forgotten words based on wordIds list 
			String getSelectedWordsURL = getString(R.string.getselectedwords_url, nativeCode, foreignCode, wordIds);
			Log.w(ForgottenFragment.class.getName(), "Selected words URL: " + getSelectedWordsURL); 
			
			// querying online web service to get word details for passed in wordsIds
			GetWordsListFromXML selectedWordsReader = 
					GetWordsListFromXML.getWordsListReader(getActivity(), getSelectedWordsURL);
		
			if(selectedWordsReader == null) 
				return false; 
				
			int i = 0;
			for(; i < selectedWordsReader.getSize(); i++) { 
				
				Log.w(WordsLoader.class.getName(), 
						"Loading word details for word with id: " + selectedWordsReader.getWordIds().get(i)  + ".");
				
				// loading word details for current wordId 
				WordObject word = selectedWordsReader.getWordObject(i); 
				
				// add loaded word to forgotten words list 
				if(word != null && word.getForeignWord() != null) {
					// word.getImageBitmap(); // preloading image bitmap from web service on background thread
					onWordLoaded(word);
				} else 
					continue; 
				
				// updating progress bar
				i++;
				publishProgress((int) ((i / (float) count) * 100));
			}
			
			if(i == count)
				return true; // all words has been loaded
			else 
				return false; // some words hasn't been loaded or other error occured
		}
		
		 protected void onProgressUpdate(Integer... progress) {
	         setProgressPercent(progress[0]);
	     }

	     protected void onPostExecute(Boolean result) {
			 hideProgressDialog();
			
	    	 if(result) 
	    		 Toast.makeText(getActivity(), getString(R.string.all_forgotten_words_loaded), Toast.LENGTH_SHORT).show();
	    	 else 
	    		 Toast.makeText(getActivity(), getString(R.string.error_forgotten_words_loading), Toast.LENGTH_SHORT).show();
	    	 
	    	 if(notAskedToSyncWords) { 
	    		 askUserToSyncWords();
	    		 notAskedToSyncWords = false; 
	    	 }
	    	 
	     }
		
	}
	
	/**
	 * Helper method that updates progress percent on ActionBar
	 * @param progress
	 */
	private void setProgressPercent(int progress) 
	{
		// ... 
	}
	
	/**
	 * Handler that receives as argument newly loaded
	 * from web service word object and places it on 
	 * the screen. 
	 * @param word
	 */
	private void onWordLoaded(final WordObject word) 
	{
		
		getActivity().runOnUiThread( new Runnable() {

			@Override
			public void run() {
				makeButton(word.getForeignWord(), word.getNativeWord(), word.getTranscription(), word.getWordId());
				makeAudio(word.getWordId(), word.getRecording());
				makeImageView(word.getWordId(), word.getImages().get(0));
				
			} 
		});
	}
	
	public boolean allowBackPressed() {
		
		if(wordsLoader != null && wordsLoader.getStatus() == Status.RUNNING) { 
			Toast.makeText(getActivity(), getString(R.string.wait_words_loading), Toast.LENGTH_SHORT).show();
			return false;
		}
			
		return true; 
	}
	
    /*
     private void makeButton(String enWord, String plWord, String transcription, int wid, int weight) { 
        String trans = transcription.replaceAll("��", "'");
        trans = trans.replaceAll("��", ","); 
        RelativeLayout buttonsLine = new RelativeLayout(this);
        Button button = new Button(this);
       
        button.setText(
                Html.fromHtml("<font color='#AB1E35'>"+ enWord + "</font><br/>"
                + "<small><font color='#6699FF' bgcolor'#F5F9FA'>" + trans + "</font></small><br />"
                + "<small>"+ plWord +"</small>"));
       
        Resources resources = this.getResources();
        //Drawable drawable = resources.getDrawable(R.drawable.buttonColor);
        Drawable drawable = resources.getDrawable(R.drawable.button_shape);
        button.setBackgroundDrawable(drawable);
        Display display=getWindowManager().getDefaultDisplay();
        int width=display.getWidth();
       // button.setWidth(width-90);
        int pxHeight = (int) (scale*80 + 0.5f); 
        button.setHeight(pxHeight);
        button.setGravity(Gravity.LEFT);
        int pxPadding = (int) (scale*10 + 0.5f);
        button.setPadding(0,pxPadding,0,0);
        button.setId(wid);
       
         
        button.setTextSize(16);
        button.setTextColor(Color.BLACK);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                  RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        int pxMargin = (int) (scale*10 + 0.5f); 
        layoutParams.leftMargin = pxMargin;
       
        button.setTag(wid);
        button.setOnClickListener(this);
        
        
        buttonsLine.addView(button, layoutParams);
        ImageView audioButton = new ImageView(this);
       
        audioButton.setPadding(0,pxMargin,0,0);
        audioButton.setTag(wid);
        audioButton.setOnClickListener(this);
        audioButton.setImageResource(R.drawable.audio_button);
        RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(
                  RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        int pxOffset = (int) (80*scale + 0.5f); 
        int marginLeft = width-pxOffset; 
        layoutParams2.setMargins(marginLeft, 0, pxMargin, 0);
        buttonsLine.addView(audioButton, layoutParams2);
        
        TextView weightStatus = new TextView(this);
        weightStatus.setText(String.valueOf(weight));
        weightStatus.setTextColor(Color.WHITE);
        int pxOne = (int) (1*scale + 0.5f); 
        weightStatus.setPadding(2*pxOne, pxOne, 2*pxOne, pxOne);
        weightStatus.setBackgroundColor(Color.parseColor("#AB1E35"));
        int weightImg = 10 * weight;
        RelativeLayout.LayoutParams layoutParams4 = new RelativeLayout.LayoutParams(
                  weightImg, RelativeLayout.LayoutParams.WRAP_CONTENT); 
        int px40 = (int) (40*scale + 0.5f); 
        layoutParams4.setMargins(marginLeft, px40, 0, 0);
        buttonsLine.addView(weightStatus, layoutParams4);
        
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams3.setMargins(pxMargin, 0, pxMargin, 0);
        layout.addView(buttonsLine,layoutParams3);
        
     }
     */
   
}
