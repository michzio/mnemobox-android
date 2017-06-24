/*
 * @last modified date 25.10.2014
 */
package pl.electoroffline;
import java.util.ArrayList;

import pl.elector.database.RememberMeProvider;
import pl.elector.database.WordProvider;
import pl.elector.database.WordsetType;
import android.database.Cursor;
import android.net.Uri;

/**
 *
 * @author Michał Ziobro 
 */
public class RememberMeFragment extends WordsListFragment {
	
	public static final String TAG = "REMEMBER_ME_FRAGMENT_TAG";
  
	protected boolean isSyncAskEnabled() 
	{
		return true; 
	}
	
    /**
     * Helper method that loads words for current user 
     * (also anonymous user if user is not logged in) 
     * from rememberMeTable. 
     */
	@Override
    protected void loadWordsFromDatabase() {
    	
    	// loading words from SQL database 
    	Uri REMEMBER_ME_FOR_PROFILE_URI = Uri.parse(RememberMeProvider.CONTENT_URI + "/profile/" + profileId); 
    	String[] projection = { RememberMeProvider.RememberMeTable.COLUMN_WORD_ID }; 
    	Cursor cursor = getActivity().getContentResolver().query(REMEMBER_ME_FOR_PROFILE_URI, projection, null, null, null); 
    	
    	// if cursor is not null constructing IN selection argument for query on word table 
    	if(cursor.getCount() > 0) { 
    		
    		ArrayList<String> wordIds = new ArrayList<String>(); //rememberMe wordIds list 
    		StringBuilder sb = new StringBuilder(); 
    		sb.append("("); 
    		
    		while(cursor.moveToNext()) { 
    			
    			int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(RememberMeProvider.RememberMeTable.COLUMN_WORD_ID));
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
    		}
    		
    	} else { 
    		// there is no remember me words stored locally 
    		return;
    	}
    }
    
	@Override
	protected void traceCurrentWordToDelete(int wordId) { 
		Personalization p = new Personalization(getActivity()); 
		p.traceRememberMeWord(wordId, true); 
	}
	
	@Override
	protected void traceCheckedWordsToDelete() {
		Personalization p = new Personalization(getActivity()); 
		for(int wordId : checkedWords) { 
			p.traceRememberMeWord(wordId, true);
			//remove corresponding view from layout
			layout.removeView(layout.findViewById(wordId)); 
		}
	}
	
	@Override
	protected WordsetType getWordsetType() { 
		return WordsetType.REMEMBER_ME_WORDSET; 
	}
	
	@Override
	protected String getHeader() {
		return getString(R.string.rememberme_header); 
	}

}