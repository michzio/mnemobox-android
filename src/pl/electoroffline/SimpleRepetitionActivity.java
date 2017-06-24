/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import pl.elector.database.LearningHistoryProvider.Mode;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 *
 * @author Michał Ziobro
 */
public class SimpleRepetitionActivity extends LearningMethodActivity {
    
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
		return SimpleRepetitionFragment.class;
	}
	
	@Override 
	protected Mode getLearningMode() { 
		return Mode.ANDROID_SIMPLE_REPETITION; 
	}
   
   @Override 
   protected void createLayout() { 	   
	   super.createLayout();
	   
        // setting up action bar title
        ActionBar actionBar =  getSupportActionBar();
        actionBar.setTitle(getString(R.string.simplerepetition));  
    }
	
    /**
     *  DEPRECATED loading from XML resource, now we use WordsetWordsAccessor object to load words
     *  //url = getResources().getString(R.string.getwordset_url);
     *	//url += OdpytywanieActivity.wordsetId;
     *	
     *	url = "wordset" + SimpleRepetitionActivity.wordsetId;
     *   Resources res = getResources();
     *   System.out.println(url); 
     *   int wordset = res.getIdentifier(url, "raw", getPackageName());
     *	try  { 
     *   InputStream is = this.getResources().openRawResource(wordset);
     *   wordsListObject = new GetWordsListFromXML(is); 
     *   try { 
     *   is.close();
     *   } catch(java.io.IOException e) { } 
     *	} catch (Exception e) { }
     *
     *	if(wordsListObject instanceof GetWordsListFromXML) { 
     *   loadWords(); 
     * }
     */
	
	/**
     *  DEPRECATED: used when words details where loaded from GetWordsListFromXML object
     *  
     *  private void loadWords() { 
     *    enWords = this.wordsListObject.getENWords();
     *    plWords = this.wordsListObject.getPLWords();
     *    images = this.wordsListObject.getImages();
     *    transcriptions = this.wordsListObject.getTranscriptions();
     *    audios = this.wordsListObject.getAudios();
 	 *
     *    widCollection = new ArrayList<Integer>(enWords.keySet());
     *   }
     */
	
	/**
     * DEPRECATED: 
     *  private void impressForgotten() { 
     *      serializedForgotten += currWid + ",1;";
     *  }
     */  
}
