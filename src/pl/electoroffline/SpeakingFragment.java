package pl.electoroffline;

import java.util.ArrayList;
import java.util.Locale;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

public class SpeakingFragment extends LearningFragment {
	
	private static final String TAG = "SpeakingFragment.Recording";

	private View view; 
	private TextView foreignWordTextView; 
	private TextView nativeWordTextView;
	private TextView transcriptionTextView;
	private ImageView wordImageView; 
	private ImageView audioButton; 
	private ImageButton  checkInputButton; 
	private ImageButton nextWordButton; 
	private Button recordVoiceButton; 
    private Button recordVoiceOnButton;
    private Button answerResultOnButton; 
    private ImageView answerCheckedImageView;
    
    private RelativeLayout textInputScreen; 
    private RelativeLayout checkAnswerScreen; 
    
    private boolean uncovered = false; 
    
    // speech recognition properties
    public ArrayList<String> voiceResults; 
    private double answerAccuracy;
    
    @Override 
 	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
 	{
 		view = inflater.inflate(R.layout.speaking, container, false);
 		
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
    		answerResultOnButton.setVisibility(View.GONE);
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
        checkAnswerScreen = (RelativeLayout) view.findViewById(R.id.learning_screen_check_answer_footer);
        textInputScreen = (RelativeLayout) view.findViewById(R.id.learning_screen_inputs_footer);
        
        recordVoiceButton = (Button) view.findViewById(R.id.recordVoiceBtn);
        recordVoiceButton.setOnClickListener(this);
        recordVoiceOnButton = (Button) view.findViewById(R.id.recordVoiceOnBtn);
        answerResultOnButton = (Button) view.findViewById(R.id.answerResultOnBtn);
       
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
             skipAnswer(); 
        } else if(view.getId() == R.id.nextWordBtn) {  	
             learningListener.loadNextWord();      
        } else if( view.getId() == R.id.recordVoiceBtn) { 
             speechRecognising();
        }
     }
    
    protected void checkAnswer() {
		learningListener.playRecording();
		verifyAnswer();
		uncoverChallangeElements(); 
	}
    
    protected void verifyAnswer() { 
        
       int ansPercent = (int) (answerAccuracy*100); 
       String answerRate = String.valueOf(ansPercent);
          
       if(answerAccuracy > 0.5) {  
        	answerCheckedImageView.setBackgroundResource(R.drawable.good);
        	answerResultOnButton.setText(answerRate); // !!!
        	learningListener.incrementGoodAns();
        	learningListener.traceForgottenWord(wordId, Personalization.Mood.NEUTRAL);
        } else { 
        	answerCheckedImageView.setBackgroundResource(R.drawable.bad);
        	answerResultOnButton.setText(answerRate); // !!!
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
        
        recordVoiceButton.setText(""); // !!!
        
        uncovered = false; 
        
        //nativeWordTextView.setTextSize(25); ?
    }
    
    protected void skipAnswer() { 
        answerAccuracy = 0; 
        learningListener.playRecording();
        verifyAnswer();
        uncoverChallangeElements();
    } 
    
    
    /*** SPEECH RECOGNISING API IMPLEMENTATION ! ***/
    public void speechRecognising() { 
    	
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
            "pl.electoroffline");
        
        SpeechRecognizer recognizer = SpeechRecognizer
        		 						.createSpeechRecognizer(getActivity().getApplicationContext());
        
        RecognitionListener listener = new RecognitionListener() {
        	
		        @Override
		        public void onResults(Bundle results) {
		            
		            voiceResults = results
		                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		            if (voiceResults == null) {
		                Log.e(TAG, "No voice results");
		            } else {
		                Log.d(TAG, "Printing matches: ");
		                int n = 0; 
		                String wordPattern = foreignWord.trim();
		                for (String match : voiceResults) {
		                    Log.d(TAG, match);
		                    if(wordPattern.equalsIgnoreCase(match)) { 
		                        break; 
		                    }
		                    //Toast.makeText(SpeakingActivity.this, match, Toast.LENGTH_SHORT).show();
		                    n++; 
		                }
		                calculateAnswerAccuracy(n, voiceResults.size());
		                
		            }
		            checkAnswer();
		        }

		        @Override
		        public void onReadyForSpeech(Bundle params) {
		            Log.d(TAG, "Ready for speech");
		            recordVoiceButton.setVisibility(View.GONE);
		            recordVoiceOnButton.setVisibility(View.VISIBLE);
		        }

		        @Override
		        public void onError(int error) {
		            Log.d(TAG,
		                    "Error listening for speech: " + error);
		        }
		
		        @Override
		        public void onBeginningOfSpeech() {
		            Log.d(TAG, "Speech starting");
		            
		        }

		        @Override
		        public void onBufferReceived(byte[] buffer) {
		
		        }

		        @Override
		        public void onEndOfSpeech() {
		            recordVoiceButton.setVisibility(View.VISIBLE);
		            recordVoiceOnButton.setVisibility(View.GONE);
		        }

		        @Override
		        public void onEvent(int eventType, Bundle params) {
		
		        }

		        @Override
		        public void onPartialResults(Bundle partialResults) {
		
		        }

		        @Override
		        public void onRmsChanged(float rmsdB) {
		
		        }
        };
        
        recognizer.setRecognitionListener(listener);
        recognizer.startListening(intent);
    }
    
    /**
     * Helper method that calculates voice answer accuracy
     */
    private void calculateAnswerAccuracy(int n, int length) { 
        double range = (1.0)/length; 
        double accuracy = 1.0 - range*n; 
        answerAccuracy = accuracy; 
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
    		answerResultOnButton.setVisibility(View.GONE);
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
		answerResultOnButton.setVisibility(View.GONE);
    }
}
