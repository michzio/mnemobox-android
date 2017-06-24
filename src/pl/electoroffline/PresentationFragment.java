package pl.electoroffline;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PresentationFragment extends LearningFragment {

	private View view; 
	private TextView foreignWordTextView; 
	private TextView nativeWordTextView;
	private TextView transcriptionTextView;
	private ImageView wordImageView; 
	private ImageView audioButton; 
	private ImageButton badButton; 
	private ImageButton normalButton; 
	private ImageButton goodButton;
	
	@Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		
		view = inflater.inflate(R.layout.presentation, container, false);
		
		loadLayout();
		setWord();
		
		return view; 
	}
	
	private void loadLayout() {
		
		foreignWordTextView = (TextView) view.findViewById(R.id.presentationWordEN);
        nativeWordTextView = (TextView) view.findViewById(R.id.presentationWordPL);
        transcriptionTextView = (TextView) view.findViewById(R.id.presentationWordTrans);
        wordImageView = (ImageView) view.findViewById(R.id.presentationWordImage);
        
        /** 
	        DisplayMetrics displaymetrics = new DisplayMetrics();
	        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
	        int width = displaymetrics.widthPixels;
	        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
	                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        float scale = getActivity().getResources().getDisplayMetrics().density;
	        int px50 = (int) (50*scale + 0.5f); 
	        layoutParams.leftMargin = width-px50;
	        int px30 = (int) (30*scale + 0.5f); 
	        layoutParams.topMargin = px30;
        */
        audioButton = (ImageView) view.findViewById(R.id.presentationAudioBtn);
        // audioButton.setLayoutParams(layoutParams);
        audioButton.setOnClickListener(this);
        
        badButton = (ImageButton) view.findViewById(R.id.badBtn);
        badButton.setOnClickListener(this);
        normalButton = (ImageButton) view.findViewById(R.id.normalBtn);
        normalButton.setOnClickListener(this);
        goodButton = (ImageButton) view.findViewById(R.id.goodBtn);
        goodButton.setOnClickListener(this);
	}
	
	private void setWord() {
		
		Log.w(PresentationFragment.class.getName(), "Setting fragment with word: " + foreignWord + " on learning pager.");
		
		foreignWordTextView.setText(foreignWord);
		nativeWordTextView.setText(nativeWord);
		transcription = transcription.replaceAll("��", "'");
		transcription = transcription.replaceAll("��", ",").replaceAll("��", ","); 
		
		if(DrawerActivity.isRTL())  {
	       	SpannableStringBuilder styledTranscription = new SpannableStringBuilder(transcription);
	       	styledTranscription.setSpan (
	       			new BackgroundColorSpan(getActivity().getResources().getColor(R.color.lightBlue)), 0, transcription.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	       	transcriptionTextView.setText(styledTranscription);
	    } else { 
	    	transcriptionTextView.setText(transcription);
	    }
		
		// adjust font size
		if(foreignWord.length() > 20) {
			foreignWordTextView.setTextSize(18);
		    transcriptionTextView.setTextSize(13);
		    nativeWordTextView.setTextSize(15);
		 } else {
		    foreignWordTextView.setTextSize(28);
		    transcriptionTextView.setTextSize(20);
		    nativeWordTextView.setTextSize(25);
		 }
		 
		 learningListener.loadWordImage(wordImageView, wordId);
		 learningListener.playRecording();
	}
	
	@Override
    public void onClick(View view) { 
		super.onClick(view);
		
		if(view.getId() == R.id.badBtn) { 
        	
        	learningListener.traceForgottenWord(wordId, Personalization.Mood.BAD);
        	learningListener.addToForgottenDrawerList(wordId); 
            learningListener.loadNextWord();

        } else if(view.getId() == R.id.normalBtn) {
        	
        	learningListener.traceForgottenWord(wordId, Personalization.Mood.NEUTRAL);
            learningListener.loadNextWord();
            
        } else if(view.getId() == R.id.goodBtn) {
        	
        	learningListener.traceForgottenWord(wordId, Personalization.Mood.GOOD);
            learningListener.traceLearnedWord(wordId); 
            learningListener.loadNextWord();
        }
		learningListener.playRecording();
	}

	@Override
	protected void onFragmentVisible() {
		
			 Log.w(PresentationFragment.class.getName(), foreignWord + " fragment is visible.");
			 // recording could be played here: learningListener.playRecording(wordId);
	}

	@Override
	protected void onFragmentHidden() {
		 Log.w(PresentationFragment.class.getName(), foreignWord + " fragment lost visibility.");
		
	}
	
}
