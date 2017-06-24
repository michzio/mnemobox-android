/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import pl.elector.database.ForgottenProvider;
import pl.elector.database.WordProvider;
import pl.elector.database.WordsetType;
import pl.electoroffline.Personalization.Mood;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;
/**
 *
 * @author Michał Ziobro 
 */
public class ListOfWordsFragment extends WordsListFragment implements WordsetWordsAccessor.Callbacks {
	
	public static final String TAG = "LIST_OF_WORDS_FRAGMENT_TAG"; 
	public static final String WORDSET_TYPE = "KEY_WORDSET_TYPE";
	private String HEADER = "List Of Words"; 
	
	protected int wordsetId;
	protected WordsetType wordsetType = WordsetType.SYSTEM_WORDSET;
	
	// containers with collection of WORDSET words
	protected LinkedHashMap<Integer, String> enWords;
	protected LinkedHashMap<Integer, String> plWords;
	protected LinkedHashMap<Integer, String> transcriptions;
	protected LinkedHashMap<Integer, String> audios; 
	protected LinkedHashMap<Integer, String> imagePaths;
	protected boolean areImageDataAvailable; 
	protected ArrayList<Integer> widCollection;
	protected ArrayList<String> selectedWordIds = new ArrayList<String>(); // contains selected ids of words
	
	@Override 
	protected String getHeader() {
		return getString(R.string.list_of_words_header); 
	}
	
	@Override
	protected WordsetType getWordsetType() { 
		return wordsetType; 
	}
	
	@Override
	protected void traceCurrentWordToDelete(int wordId) { 
		// do nothing 
	}
	
	@Override
	protected void traceCheckedWordsToDelete() {
		// do nothing
	}
	
	
	@Override
	public void right2left(final View v) {
		// block swipe
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
    	
		// get WORDSET identifier and type from Intent's Bundle 
		wordsetId = getActivity().getIntent().getExtras().getInt(WordsetsListActivity.SELECTED_WORDSET); 
		wordsetType = (WordsetType) getActivity().getIntent().getExtras().getSerializable(WordsetType.KEY_TYPE);
		if(wordsetType == null) wordsetType = WordsetType.SYSTEM_WORDSET;
		
		WordsetWordsAccessor wordsAccessor;
		if(wordsetType == WordsetType.SYSTEM_WORDSET ) { 
			wordsAccessor = new WordsetWordsAccessor(getActivity(), wordsetId, wordsetType, false);
		} else if(wordsetType == WordsetType.SELECTED_WORDS) { 
			wordsAccessor = new WordsetWordsAccessor(getActivity(), wordsetId, wordsetType, false);
			selectedWordIds = getActivity().getIntent().getStringArrayListExtra(WordsetActivityOld.KEY_SELECTED_WORD_IDS);
			wordsAccessor.setSelectedWordIds(selectedWordIds);
		} else { 
			wordsAccessor = new WordsetWordsAccessor(getActivity(), wordsetId, wordsetType, false);
		}
		wordsAccessor.setCallbacksListener(this);
		wordsAccessor.load();
    	
    }
	
	/**
     * WordsetWordsAccessor.Callbacks interface method called when WordsetWordsAccessor
     * object finishes WORDSET WORDS details loading process. Now you can display words.
     */
	@Override
    public void onWordsLoadFinished(WordsetWordsAccessor wordsAccessor) {
      	
      		Log.w(WordsetWordsAccessor.Callbacks.class.getName(), 
      					"Words loading finished! (" + wordsAccessor.getWordIds().size() + ")");
      		
      		// setting local attributes with collections of data loaded by words accessory
    	    enWords = wordsAccessor.getForeignWords(); 
    	    plWords = wordsAccessor.getNativeWords();
    	    transcriptions = wordsAccessor.getTranscriptions(); 
    	    audios = wordsAccessor.getRecordingNames();
    	    imagePaths = wordsAccessor.getImagePaths();
    	    imagesData = wordsAccessor.getImageData(); 
    	    areImageDataAvailable = (imagesData == null) ? false : true;
    	    widCollection = wordsAccessor.getWordIds();    	    
    	        
    	    // start learning 
    	    displayWordsList();
    }
	
	private void displayWordsList() {
		
		for(Integer wordId : widCollection) { 
			makeButton(enWords.get(wordId), plWords.get(wordId), transcriptions.get(wordId), wordId);
			makeAudio(wordId, audios.get(wordId)); 
			if(areImageDataAvailable) { 
				makeImageView(wordId, IMAGE_FROM_BLOB);
			} else { 
				makeImageView(wordId, imagePaths.get(wordId));
			}
		}
	}
	
	public boolean allowBackPressed() {
		return true; 
	}

	@Override
	boolean isSyncAskEnabled() {
		return false;
	}
	
	@Override
	protected void startLearningMethod(Class<?> learningMethodClass) 
	{
		Intent learningMethodIntent = new Intent(getActivity(), learningMethodClass);
		
		learningMethodIntent.putExtra(WordsetsListActivity.SELECTED_WORDSET, wordsetId);

		if(getWordsetType() == WordsetType.SYSTEM_WORDSET) { 
			learningMethodIntent.putExtra(WordsetType.KEY_TYPE, WordsetType.SYSTEM_WORDSET);
		} else if(getWordsetType() == WordsetType.SELECTED_WORDS) { 
			learningMethodIntent.putExtra(WordsetType.KEY_TYPE, WordsetType.SELECTED_WORDS);
			learningMethodIntent.putExtra(WordsetActivityOld.KEY_SELECTED_WORD_IDS, selectedWordIds);
		}
		
     	startActivity(learningMethodIntent);
	}
	
	@Override 
	public void onPrepareOptionsMenu(Menu menu)
	{
		menu.findItem(R.id.taskNotificationsBtn).setVisible(false);
		// menu.findItem(R.id.menu_logout).setVisible(false);
		menu.findItem(R.id.deleteItem).setEnabled(false).setVisible(false);
		
		if(selectedWordId != null && !isSyncing) { 
			menu.findItem(R.id.itemDetails).setEnabled(true).setVisible(true);
		} else {
			menu.findItem(R.id.itemDetails).setEnabled(false).setVisible(false);
		}
	}
	
	
	/**
	 * Handler that creates context menu that is shown after long click on word item.
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		  
		 super.onCreateContextMenu(menu, v, menuInfo);
	        
	     menu.removeItem(DELETE_ITEM_ID);
	}
	
   
}
