/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.util.Locale;

import pl.elector.database.LearningHistoryProvider.Mode;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
 
/**
 *
 * @author Michał Ziobro
 */
public class RepetitionActivity extends LearningMethodActivity {
     
    @Override
    public void onCreate(Bundle savedInstanceState){
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
		return RepetitionFragment.class;
	}
	
	@Override 
	protected Mode getLearningMode() { 
		return Mode.ANDROID_REPETITION; 
	}
   
    @Override
    protected void createLayout() { 	
    	super.createLayout();

        // setting up action bar title
        ActionBar actionBar =  getSupportActionBar();
        actionBar.setTitle(getString(R.string.repetition));
        
    }
  
 
    /**
     * try  { 
     *   InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
     *   wordsListObject = new GetWordsListFromXML(is); 
     *   try { 
     *   	is.close();
     *   } catch(java.io.IOException e) { } 
     * } catch (Exception e) { }
     * if(wordsListObject instanceof GetWordsListFromXML) { 
     *  	loadWords(); 
     *	}
     */
    
    /**
     * private void loadWords() { 
     *    enWords = this.wordsListObject.getENWords();
     *   plWords = this.wordsListObject.getPLWords();
     *   images = this.wordsListObject.getImages();
     *   transcriptions = this.wordsListObject.getTranscriptions();
     *   audios = this.wordsListObject.getAudios();
 	 *
     *   widCollection = new ArrayList<Integer>(enWords.keySet());
     * }
     */
    
    /**
     *  private void impressForgotten() { 
     *       serializedForgotten += currWid + ",1;";
     * 	}
     */
}
