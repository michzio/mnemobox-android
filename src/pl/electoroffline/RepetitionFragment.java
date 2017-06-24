package pl.electoroffline;

import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RepetitionFragment extends LearningFragment {

	private View view; 
	private TextView foreignWordTextView; 
	private TextView nativeWordTextView;
	private TextView transcriptionTextView;
	private ImageView wordImageView; 
	private ImageView audioButton; 
	private ImageButton  checkInputButton; 
	private ImageButton nextWordButton; 
	private EditText inputTranslationEditText; 
	private ImageView answerCheckedImageView;
	
	private RelativeLayout textInputScreen; 
    private RelativeLayout checkAnswerScreen; 
    
    private boolean uncovered = false; 
    
    @Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		view = inflater.inflate(R.layout.repetition, container, false);
		
		loadLayout();
		hideChallangeElements();
		setWord();
		checkWordAlreadyAnswered();
		
		return view; 
	}
    
    @Override
    public void onDestroy() {
    	// Fragment will be destroyed and wordItemView will be not within the visible page
		Drawable drawable = wordImageView.getDrawable();
		if (drawable instanceof BitmapDrawable) {
		    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
		    Bitmap bitmap = bitmapDrawable.getBitmap();
		    if(bitmap != null) bitmap.recycle();
		}
		wordImageView.setImageBitmap(null);
		Log.w(ChoosingFragment.class.getName(), "Bitmap has been recycled in Fragment.onDestroy().");
    	super.onDestroy();
    }
    
    private void checkWordAlreadyAnswered() 
    {
    	if(wordAnswered) { 
    		
    		if(wordAnsweredSuccessfully) { 
    			answerCheckedImageView.setBackgroundResource(R.drawable.good);
    		} else {
    			answerCheckedImageView.setBackgroundResource(R.drawable.bad);
    		}
    		uncoverChallangeElements();
    	}
    }
    
    private void loadLayout() {
		
		foreignWordTextView = (TextView) view.findViewById(R.id.presentationWordEN);
        nativeWordTextView = (TextView) view.findViewById(R.id.presentationWordPL);
        transcriptionTextView = (TextView) view.findViewById(R.id.presentationWordTrans);
        wordImageView = (ImageView) view.findViewById(R.id.presentationWordImage);
        
        /** deprecated 
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
        
        checkInputButton = (ImageButton) view.findViewById(R.id.checkInputBtn);
        checkInputButton.setOnClickListener(this);
        nextWordButton = (ImageButton) view.findViewById(R.id.nextWordBtn);
        nextWordButton.setOnClickListener(this);
        
        answerCheckedImageView = (ImageView) view.findViewById(R.id.odpytywanieViewAnswer); 
        checkAnswerScreen = (RelativeLayout) view.findViewById(R.id.odpytywanieCheckAnswerScreen);
        textInputScreen = (RelativeLayout) view.findViewById(R.id.odpytywanieTextInputScreen);
        inputTranslationEditText = (EditText) view.findViewById(R.id.odpytywanieEditText); 
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
    }
    
    @Override
    public void onClick(View view) { 
		super.onClick(view);
		 if(view.getId() == R.id.checkInputBtn) {  	
	           checkAnswer(); 
	     } else if(view.getId() == R.id.nextWordBtn) {    	
	           inputTranslationEditText.setText("");
	           learningListener.loadNextWord(); 
	    }
	}
    
    protected void checkAnswer() {
		learningListener.playRecording();
		verifyAnswer();
		uncoverChallangeElements(); 
	}
    
    protected void verifyAnswer() { 
        
    	String answer = inputTranslationEditText.getText().toString();
	    answer = answer.trim().toLowerCase(Locale.ENGLISH);
	    String wordPattern = foreignWord.trim().toLowerCase(Locale.ENGLISH);  
        if(wordPattern.equalsIgnoreCase(answer)) { 
        	answerCheckedImageView.setBackgroundResource(R.drawable.good);
        	learningListener.incrementGoodAns();
        	learningListener.traceForgottenWord(wordId, Personalization.Mood.NEUTRAL);
        } else { 
        	answerCheckedImageView.setBackgroundResource(R.drawable.bad);
        	learningListener.incrementBadAns();
        	learningListener.traceForgottenWord(wordId, Personalization.Mood.BAD); 
	        learningListener.addToForgottenDrawerList(wordId); 
        }
    }
    
    protected void uncoverChallangeElements() { 
        textInputScreen.setVisibility(View.GONE);
        checkAnswerScreen.setVisibility(View.VISIBLE);
        foreignWordTextView.setVisibility(View.VISIBLE);
        audioButton.setVisibility(View.VISIBLE); 
        transcriptionTextView.setVisibility(View.VISIBLE);
        
        uncovered = true; 
        
        //nativeWordTextView.setTextSize(14); ?
    }
    
    protected void hideChallangeElements() { 	
        foreignWordTextView.setVisibility(View.GONE);
        audioButton.setVisibility(View.GONE); 
        transcriptionTextView.setVisibility(View.GONE);
        checkAnswerScreen.setVisibility(View.GONE);
        textInputScreen.setVisibility(View.VISIBLE);  
        
        uncovered = false; 
        
        //nativeWordTextView.setTextSize(25); ?
    }
    /**
     * if answer hasn't been checked (uncovered) yet (uncover it and check it now!)
     */
    @Override
    public void onSwipedForward() { 
    	super.onSwipedForward();
    	if(!learningListener.checkCurrentWordAnswered()) {
    		Log.w(ChoosingFragment.class.getName(), "User hasn't answerd question about: " + foreignWord); 
    		verifyAnswer();
    		uncoverChallangeElements();
    	}
    }
    
    /**
     * if current word answer has been already aksed/answered 
	 * uncover it without checking.
     */
    @Override
    public void onSwipingForward() { 
    	super.onSwipingForward();
    	
    	Log.w(ChoosingFragment.class.getName(), "New Fragment display word: "+ foreignWord); 
    	Log.w(ChoosingFragment.class.getName(), "ChoosingFragment.onSwipingForward() " + foreignWord + ", " +  hashCode());
    	
    	if(learningListener.checkCurrentWordAnswered()) { 
    		
    		if(learningListener.currentWordAnsweredSuccessfully()) { 
    			answerCheckedImageView.setBackgroundResource(R.drawable.good);
    		} else {
    			answerCheckedImageView.setBackgroundResource(R.drawable.bad);
    		}
    		uncoverChallangeElements();
    	}
    }
    
   /** 
    * uncover word answer without checking it again.
    */
    @Override
    public void onSwipingBackward() { 
    	super.onSwipingBackward();
    	
    	Log.w(ChoosingFragment.class.getName(), "New Fragment display word: "+ foreignWord); 
    	Log.w(ChoosingFragment.class.getName(), "ChoosingFragment.onSwipingBackground() " + foreignWord + ", " +  hashCode());
    	
    	if(learningListener.currentWordAnsweredSuccessfully()) { 
			answerCheckedImageView.setBackgroundResource(R.drawable.good);
		} else {
			answerCheckedImageView.setBackgroundResource(R.drawable.bad);
		}
		uncoverChallangeElements();
    }
    
}
