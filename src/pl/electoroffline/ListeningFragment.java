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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ListeningFragment extends LearningFragment {

	private View view; 
	private TextView foreignWordTextView; 
	private TextView nativeWordTextView;
	private TextView transcriptionTextView;
	private ImageView wordImageView; 
	private ImageView audioButton; 
	private ImageButton  checkInputButton; 
	private ImageButton nextWordButton; 
	private EditText foreignInputEditText;
    private EditText nativeInputEditText; 
    private ImageView answerCheckedImageView;
    
    private LinearLayout textInputScreen; 
    private RelativeLayout checkAnswerScreen;
    
    private Button hintButton; 
    private TextView hintWord;
    private ImageButton hintAudioButton; 
    
    private LinearLayout wordHeader;
    
    private boolean uncovered = false; 
    
    @Override 
  	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  	{
  		view = inflater.inflate(R.layout.listening, container, false);
  		
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
        
        audioButton = (ImageView) view.findViewById(R.id.presentationAudioBtn);
        audioButton.setOnClickListener(this);
        // setSmallAudioButton(); - DEPRECATED
        
        checkInputButton = (ImageButton) view.findViewById(R.id.checkInputBtn);
        checkInputButton.setOnClickListener(this);
        nextWordButton = (ImageButton) view.findViewById(R.id.nextWordBtn);
        nextWordButton.setOnClickListener(this);
        
        answerCheckedImageView = (ImageView) view.findViewById(R.id.odpytywanieViewAnswer); 
        checkAnswerScreen = (RelativeLayout) view.findViewById(R.id.odpytywanieCheckAnswerScreen);
        textInputScreen = (LinearLayout) view.findViewById(R.id.odpytywanieTextInputScreen);
       
        hintButton = (Button) view.findViewById(R.id.hintBtn);
        hintButton.setOnClickListener(this);
        hintWord = (TextView) view.findViewById(R.id.hintWord);
        
        hintAudioButton = (ImageButton) view.findViewById(R.id.hintAudioBtn); 
        hintAudioButton.setOnClickListener(this); 
        
        wordHeader = (LinearLayout) view.findViewById(R.id.learning_screen_word_header); 
        
        foreignInputEditText = (EditText) view.findViewById(R.id.dyktandoENEditText);
        nativeInputEditText = (EditText) view.findViewById(R.id.dyktandoPLEditText);
          
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
	/**	if(foreignWord.length() > 20) {
			foreignWordTextView.setTextSize(18);
			transcriptionTextView.setTextSize(13);
			nativeWordTextView.setTextSize(15);
		} else {
			foreignWordTextView.setTextSize(28);
			transcriptionTextView.setTextSize(20);
			nativeWordTextView.setTextSize(25);
		}
	 **/	
		learningListener.loadWordImage(wordImageView, wordId);
		learningListener.playRecording();
    }
    
    @Override
    public void onClick(View view) { 
		super.onClick(view);
		 if(view.getId() == R.id.checkInputBtn) {  	
	         checkAnswer(); 
	     } else if(view.getId() == R.id.nextWordBtn) {    	
	    	 foreignInputEditText.setText("");
	         nativeInputEditText.setText("");
	         learningListener.loadNextWord(); 
	     } else if(view.getId() == R.id.hintBtn ) { 
             showHint(); 
         } else if(view.getId() == R.id.hintAudioBtn) { 
        	 learningListener.playRecording(wordId);
         }
	}
    
    protected void checkAnswer() {
		learningListener.playRecording();
		verifyAnswer();
		uncoverChallangeElements(); 
	}
    
   protected void verifyAnswer() {       
    	String foreignAnswer = foreignInputEditText.getText().toString().trim().toLowerCase(Locale.ENGLISH);
	    String nativeAnswer = nativeInputEditText.getText().toString().trim().toLowerCase(new Locale("pl","PL"));
	    String foreignWordPattern = foreignWord.trim().toLowerCase(Locale.ENGLISH); 
        String nativeWordPattern = nativeWord.trim().toLowerCase(new Locale("pl","PL"));
	     
        if(foreignWordPattern.equalsIgnoreCase(foreignAnswer) && nativeWordPattern.equalsIgnoreCase(nativeAnswer)) { 
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
      
       wordHeader.setVisibility(View.VISIBLE); 
       audioButton.setVisibility(View.VISIBLE);
      
       wordImageView.setVisibility(View.VISIBLE);
    
       foreignInputEditText.setText("");
       nativeInputEditText.setText("");
       
       // setSmallAudioButton();  // DEPRECATED
       
       uncovered = true; 
   }
   
   protected void hideChallangeElements() { 	
       
       audioButton.setVisibility(View.GONE);
       wordHeader.setVisibility(View.GONE);
       wordImageView.setVisibility(View.GONE);
       checkAnswerScreen.setVisibility(View.GONE);
       
       textInputScreen.setVisibility(View.VISIBLE);
       
       hintWord.setVisibility(View.GONE);
       hintButton.setVisibility(View.VISIBLE);
       
       // setBigAudioButton(); // DEPRECATED
       hintAudioButton.setVisibility(View.VISIBLE);
       
       uncovered = false; 
   }
   
   private void showHint() {
       hintButton.setVisibility(View.GONE);
       transcription = transcription.replaceAll("��", "'").replaceAll("��", ",").replaceAll("��", ","); 
       hintWord.setText(transcription);
       hintWord.setVisibility(View.VISIBLE); 
   }
   
   /*** - DEPRECATED 
	   private void setSmallAudioButton() {
			 // get screen width
		     DisplayMetrics displaymetrics = new DisplayMetrics();
		     getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		     int width = displaymetrics.widthPixels;
		     
		     // calculate px size adjusted to device density
		     float scale = getActivity().getResources().getDisplayMetrics().density;
		     int px = (int) (1*scale + 0.5f); 
		     
		     // create layout for audio button
		     RelativeLayout.LayoutParams audiobtnlayoutParams = new RelativeLayout.LayoutParams(
		               RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		     audiobtnlayoutParams.leftMargin = width-px*50;
		     audiobtnlayoutParams.topMargin = px*30;
		     
		     // change audio button
		     audioButton.setBackgroundResource(R.drawable.audio_button);
		     audioButton.setLayoutParams(audiobtnlayoutParams); 
	   }
    ***/
  
   /*** - DEPRECATED
	   private void setBigAudioButton() {    
		   // calculate px size adjusted to device density 
		   float scale = getActivity().getResources().getDisplayMetrics().density;
		   int px = (int) (1*scale + 0.5f); 
		   
		   // create layout for audio button
	       RelativeLayout.LayoutParams audiobtnlayoutParams = new RelativeLayout.LayoutParams(
	                 px*50, px*50);
	       audiobtnlayoutParams.leftMargin = px*50;
	       audiobtnlayoutParams.topMargin = px*35;
	       
	       // change audio button
	       audioButton.setLayoutParams(audiobtnlayoutParams);
	       audioButton.setBackgroundResource(R.drawable.listening);   
	   }
	 ***/
   
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
