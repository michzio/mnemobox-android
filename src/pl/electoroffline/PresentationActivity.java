/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import pl.elector.database.LearningHistoryProvider.Mode;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;


/**
 * @author Michał Ziobro
 */
public class PresentationActivity extends LearningMethodActivity {
    
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
		return PresentationFragment.class;
	}
	
	@Override
	protected Mode getLearningMode() { 
		return Mode.ANDROID_PRESENTATION;
	}
    
    @Override 
    protected void createLayout() { 
    	
    	super.createLayout();
        
        // setting up action bar title
        ActionBar actionBar =  getSupportActionBar();
        actionBar.setTitle(getString(R.string.presentation));    
    }
    
    
    /**
     * DEPRECATED - loading from online web service, now we use WordsetWordsAccessor object to load words
     * try  { 
     *   InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
     *   wordsListObject = new GetWordsListFromXML(is); 
     *   try { 
     *   is.close();
     *   } catch(java.io.IOException e) { } 
     *  } catch (Exception e) { }
     * 
     *  if(wordsListObject instanceof GetWordsListFromXML) { 
     *      loadWords(); 
     *  }
     */
    
    /**
     * DEPRECATED
     * private LinkedHashMap<Integer, Integer> forgotten;
     * private String serializedForgotten = "";
     */
    
    /**
     * private void loadWords() { 
     *   enWords = this.wordsListObject.getENWords();
     *   plWords = this.wordsListObject.getPLWords();
     *   images = this.wordsListObject.getImages();
     *   transcriptions = this.wordsListObject.getTranscriptions();
     *   audios = this.wordsListObject.getAudios();
 	 *
     *   widCollection = new ArrayList<Integer>(enWords.keySet());
     * }
     */
    
    /**
     * DEPRECATED FORGOTTEN TRACING MECHANISM 
     * private void impressForgotten(Mood mood) { 
     *    if(mood == Mood.BAD) { 
     *       //forgotten.put(currWid, 2);
     *       serializedForgotten += currWid + ",2;";
     *   } else if(mood == Mood.NORMAL) { 
     *       //forgotten.put(currWid, 1);
     *       serializedForgotten += currWid + ",1;";
     *   } else if(mood == Mood.GOOD) { 
     *       //forgotten.put(currWid, 0);
     *       serializedForgotten += currWid + ",0;";
     *   }
     * }
     * 
     */
}
