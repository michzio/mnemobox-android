/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import pl.elector.database.LearningHistoryProvider.Mode;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

/**
 *
 * @author Michał Ziobro
 */
public class ListeningActivity extends LearningMethodActivity {

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void onCreateDrawerActivity(Bundle savedInstanceState) {
        super.onCreateDrawerActivity(savedInstanceState);
    }
    
	@Override
	protected boolean isUsingViewPager() {	
		return true;
	}

	@Override
	protected Class<?> getLearningFragmentClass() {	
		return ListeningFragment.class;
	}
	
	@Override
	protected Mode getLearningMode() { 
		return Mode.ANDROID_LISTENING; 
	}
      
    @Override
    protected void createLayout() {     	
    	super.createLayout();

        // setting up action bar title
        ActionBar actionBar =  getSupportActionBar();
        actionBar.setTitle(getString(R.string.listening));
    }

    /**
     * url = this.getString(R.string.getwordset_url).replaceAll("&amp;", "&");
     * url += ListOfWordsActivity.wordsetId;
     * try  { 
     *    InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
     *    wordsListObject = new GetWordsListFromXML(is); 
     *    try { 
     *    is.close();
     *    } catch(java.io.IOException e) { } 
     * } catch (Exception e) { }
     * if(wordsListObject instanceof GetWordsListFromXML) { 
     *   loadWords(); 
     * }
     */
    
    /**
     * private void loadWords() { 
     *    enWords = this.wordsListObject.getENWords();
     *    plWords = this.wordsListObject.getPLWords();
     *    images = this.wordsListObject.getImages();
     *    transcriptions = this.wordsListObject.getTranscriptions();
     *    audios = this.wordsListObject.getAudios();
     *
     *    widCollection = new ArrayList<Integer>(enWords.keySet());
     * }
     */
    
    /**
     * DEPRECATED:
     *  private void impressForgotten() {  
     *       serializedForgotten += currWid + ",1;";    
     *  }
     */
}
