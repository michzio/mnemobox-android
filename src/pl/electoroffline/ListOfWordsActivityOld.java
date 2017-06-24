/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;
import java.io.InputStream;
import java.util.LinkedHashMap;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

/**
 *
 * @author Michał Ziobro
 */
public class ListOfWordsActivityOld extends Activity implements View.OnClickListener, 
        OnCompletionListener, OnBufferingUpdateListener, OnPreparedListener {
    
    GetWordsListFromXML wordsListObject; 
    String url;
    ScrollView scrollview;
    LinearLayout layout;
    private int wordsetId;
    private MediaPlayer mediaPlayer;
    private LinkedHashMap<Integer, String> audios;
    float scale; 
    
     @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listofwords);
        scale = this.getResources().getDisplayMetrics().density;
    
        wordsetId = getIntent().getIntExtra(WordsetsListActivity.SELECTED_WORDSET, 0);
        //url = this.getString(R.string.getwordset_url).replaceAll("&amp;", "&");
        //url += ListOfWordsActivity.wordsetId;
        url = "wordset" + wordsetId;
            Resources res = getResources();
            System.out.println(url); 
            int wordset = res.getIdentifier(url, "raw", getPackageName());
         try  { 
            InputStream is = this.getResources().openRawResource(wordset);
            wordsListObject = new GetWordsListFromXML(is); 
            try { 
            is.close();
            } catch(java.io.IOException e) { } 
        } catch (Exception e) { }
        
        if(wordsListObject instanceof GetWordsListFromXML) { 
            generateListOfWords();
           
        }
        
    }
    
     private void generateListOfWords() { 
         scrollview = (ScrollView) findViewById(R.id.listOfWordsScrollView);
         layout = new LinearLayout(this);
         LinkedHashMap<Integer, String> enWords = this.wordsListObject.getENWords();
         LinkedHashMap<Integer, String> plWords = this.wordsListObject.getPLWords();
         LinkedHashMap<Integer, String> images = this.wordsListObject.getImages();
         LinkedHashMap<Integer, String> transcriptions = this.wordsListObject.getTranscriptions();
         audios = this.wordsListObject.getAudios();
         if(enWords instanceof LinkedHashMap  ) {
         for(int wid : enWords.keySet() ) {
            makeButton(enWords.get(wid), plWords.get(wid), transcriptions.get(wid), wid);
            makeAudio(audios.get(wid), wid); 
            makeImageView(images.get(wid), wid); 
            
          }
         }
        layout.setOrientation(LinearLayout.VERTICAL);
        int pxPadding = (int) (10*scale + 0.5f);
        layout.setPadding(pxPadding,  pxPadding,  pxPadding,  pxPadding);
        layout.setScrollContainer(true);
        scrollview.addView(layout);
       
     }
     
     @SuppressWarnings("deprecation")
	private void makeButton(String enWord, String plWord, String transcription, int wid) { 
        String trans = transcription.replaceAll("ˈ", "'");
        trans = trans.replaceAll("̩", ","); 
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
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = displaymetrics.widthPixels;
       // button.setWidth(width-90);
        int pxHeight = (int) (80*scale + 0.5f); 
        int pxPadding = (int) (10*scale + 0.5f);
        button.setHeight(pxHeight);
        button.setGravity(Gravity.LEFT);
        button.setPadding(0,pxPadding,0,0);
        button.setId(wid);
       
         
        button.setTextSize(16);
        button.setTextColor(Color.BLACK);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                  RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = pxPadding;
       
        button.setTag(wid);
        button.setOnClickListener(this);
        
        
        buttonsLine.addView(button, layoutParams);
        ImageView audioButton = new ImageView(this);
       
        audioButton.setPadding(0,pxPadding,0,0);
        audioButton.setTag(wid);
        audioButton.setOnClickListener(this);
        audioButton.setImageResource(R.drawable.audio_button);
        RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(
                  RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        int pxOffset = pxHeight; // pxHeight used only because have the same 80 px conversion to dp
        int marginLeft = width-pxOffset; 
        layoutParams2.setMargins(marginLeft, 0, pxPadding, 0);
        buttonsLine.addView(audioButton, layoutParams2);
        
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams3.setMargins(pxPadding, 0, pxPadding, 0);
        layout.addView(buttonsLine,layoutParams3);
       
        
        
     }
       private void makeImageView(String image, int wid) { 
        
        ImageView imgview = new ImageView(this);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        // int width = displaymetrics.widthPixels;
        imgview.setId(wid);
        UrlImageViewHelper.setUrlDrawable(imgview, getResources().getString(R.string.images_url) + image);
       // imgview.setVisibility(View.GONE);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int pxMargin = (int) (10*scale + 0.5f);
        layoutParams.setMargins(pxMargin, 0, pxMargin, pxMargin);
        layout.addView(imgview, layoutParams);
       }
       
       private void makeAudio(String audio, int wid) { 
            
       }
     
     @Override
    public void onClick(View view) {  
        
        //showWordDetails(view.getTag());
        playRecording(view.getTag());
    }
    
     private void showWordDetails(Object wid) { 
         //for example showing image for given button
         
     }
     
     private void playRecording(Object wid) { 
           mediaPlayer = new MediaPlayer();     
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnCompletionListener(this);
         int intWordId = (Integer) wid; 
         String audioUrl = getResources().getString(R.string.recordings_url) + audios.get(intWordId);
         //Toast.makeText(this, audioUrl,
                              //  Toast.LENGTH_SHORT).show();
         /** ImageButton onClick event handler. Method which start/pause mediaplayer playing */
            try {
                //FileInputStream fis = new FileInputStream(audioUrl);
                mediaPlayer.setDataSource(audioUrl);
                //mediaPlayer.prepare(); 
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.prepareAsync();
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
}
