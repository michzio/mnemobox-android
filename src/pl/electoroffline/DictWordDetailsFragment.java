package pl.electoroffline;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class DictWordDetailsFragment extends Fragment 
	implements MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnPreparedListener  {
	
	public static final String TAG = "DICT_WORD_DETAILS_FRAGMENT_TAG"; 
	
	private View view; 
	private RelativeLayout wordDetailsView; 
	
	private MediaPlayer mediaPlayer;
	private float scale; 
	
	@Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		
		view = inflater.inflate(R.layout.dict_word_details, container, false);
	  
        wordDetailsView = (RelativeLayout) view.findViewById(R.id.dictWordDetailsView);
        scale = getResources().getDisplayMetrics().density;
        
        attachListeners();
        showDetailsView(); 
        
		return view;
	}
	
	private void attachListeners() {
	
		view.findViewById(R.id.dictAudioBtn).setOnClickListener(new View.OnClickListener() {
	    
				@Override
				public void onClick(View v) {
					playRecordingClick(v);	
				}
			});
		
	}
	
	
	private void showDetailsView() { 
	   
	   // accessing child views
       TextView tvworden = (TextView) view.findViewById(R.id.dictWordEN); 
       TextView tvtrans = (TextView) view.findViewById(R.id.dictWordTrans); 
       TextView tvwordpl = (TextView) view.findViewById(R.id.dictWordPL);
       ImageView ivimage = (ImageView) view.findViewById(R.id.dictWordImage); 
       ImageView audioButton = (ImageView) view.findViewById(R.id.dictAudioBtn);
       
       // audio button layoutParams 
       DisplayMetrics displaymetrics = new DisplayMetrics();
       getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
       int width = displaymetrics.widthPixels;
       
       RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                 RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
       int pxOffset = (int) (50*scale + 0.5f); 
       int pxMarginTop = (int) (30*scale + 0.5f); 
       layoutParams.leftMargin = width-pxOffset;
       layoutParams.topMargin = pxMarginTop;
       // setting audio button layout
       audioButton.setLayoutParams(layoutParams);
       
       // setting word texts and image 
       tvworden.setText(getArguments().getString(WordObject.KEY_FOREIGN_WORD));
       tvtrans.setText(getArguments().getString(WordObject.KEY_TRANSCRIPTION).replaceAll("��", "'").replaceAll("��", ",").replaceAll("��", ",")); 
       tvwordpl.setText(getArguments().getString(WordObject.KEY_NATIVE_WORD));
       if(getArguments().getString(WordObject.KEY_IMAGE) != null )
    	   	UrlImageViewHelper.setUrlDrawable(ivimage, 
                  getString(R.string.images_url) + getArguments().getString(WordObject.KEY_IMAGE));
       
       //play recording
       playRecording();
    }
	
	
	public void playRecordingClick(View v) {       
	       playRecording(); 
	}
	  
	private void playRecording() { 
	         mediaPlayer = new MediaPlayer();     
	         mediaPlayer.setOnBufferingUpdateListener(this);
	         mediaPlayer.setOnCompletionListener(this);
	         String audioUrl = getString(R.string.recordings_url) + getArguments().getString(WordObject.KEY_RECORDING);
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
