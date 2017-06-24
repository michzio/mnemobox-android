/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;
import java.util.ArrayList;

import pl.elector.database.LearningHistoryProvider.Mode;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
//import android.speech.*;

/**
 *
 * @author Michał Ziobro
 */
@SuppressLint("NewApi")
public class SpeakingActivity extends LearningMethodActivity  {

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
		return SpeakingFragment.class;
	}
	
	@Override 
	protected Mode getLearningMode() {
		return Mode.ANDROID_SPEAKING; 
	}
 
	@Override
	protected void createLayout() { 	
	   super.createLayout();

        // setting up action bar title
        ActionBar actionBar =  getSupportActionBar();
        actionBar.setTitle(getString(R.string.speaking));
    }
    
    /**
     * DEPRECATED 
     * url = getResources().getString(R.string.getwordset_url).replaceAll("&amp;", "&");
     * url += SpeakingActivity.wordsetId;
     * try  { 
     *   InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
     *   wordsListObject = new GetWordsListFromXML(is); 
     *  try { 
     *      is.close();
     *   } catch(java.io.IOException e) { } 
     * } catch (Exception e) { }
     * if(wordsListObject instanceof GetWordsListFromXML) { 
     *   loadWords(); 
     * }
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
     *	}
     */
    
    /**
     * DEPRECATED: 
     *  private void impressForgotten() {   
     *       serializedForgotten += currWid + ",1;";
     * }
     */
}
