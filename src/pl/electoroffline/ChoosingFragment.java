package pl.electoroffline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ChoosingFragment extends LearningFragment {

	private View view; 
	private TextView foreignWordTextView; 
	private TextView nativeWordTextView;
	private TextView transcriptionTextView;
	private ImageView wordImageView; 
	private ImageView audioButton;
	private Button nextWordButton; 
	private ImageView answerCheckedImageView;
	
	private LinearLayout textInputScreen; // ! here is LinearLayout instead of RelativeLayout 
    private RelativeLayout checkAnswerScreen;
    
    private Button answer1; 
    private Button answer2; 
    private Button answer3; 
    private Button answer4; 
    
    private ArrayList<Integer> optionsIds; 
    private int idxOfGoodAns;
    private int lastAnswerId = -1; 
    
    private boolean uncovered = false;
    
    @Override 
   	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
   	{
    	
    	Log.w(ChoosingFragment.class.getName(), "Creating ChoosingFragment: " + foreignWord + ", " +  hashCode()); 
   		view = inflater.inflate(R.layout.choosing, container, false);
   		
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
        audioButton = (ImageView) view.findViewById(R.id.presentationAudioBtn);
        audioButton.setLayoutParams(layoutParams);
        audioButton.setOnClickListener(this);
        
        nextWordButton = (Button) view.findViewById(R.id.nextWordBtn);
        nextWordButton.setOnClickListener(this);
        
        answerCheckedImageView = (ImageView) view.findViewById(R.id.odpytywanieViewAnswer); 
        checkAnswerScreen = (RelativeLayout) view.findViewById(R.id.odpytywanieCheckAnswerScreen);
        textInputScreen = (LinearLayout) view.findViewById(R.id.odpytywanieTextInputScreen);
        
        // creating options to select from
        answer1 = (Button) view.findViewById(R.id.answer1);
        answer1.setOnClickListener(this);
        answer2 = (Button) view.findViewById(R.id.answer2);
        answer2.setOnClickListener(this);
        answer3 = (Button) view.findViewById(R.id.answer3);
        answer3.setOnClickListener(this);
        answer4 = (Button) view.findViewById(R.id.answer4);
        answer4.setOnClickListener(this);
        
        optionsIds = new ArrayList<Integer>();
        optionsIds.add(R.id.answer1);
        optionsIds.add(R.id.answer2);
        optionsIds.add(R.id.answer3);
        optionsIds.add(R.id.answer4);
    }
    
    private void setWord() {
		
  		Log.w(PresentationFragment.class.getName(), "Setting fragment with word: " + foreignWord + " on learning pager.");
  		
  		foreignWordTextView.setText(foreignWord);
		nativeWordTextView.setText(nativeWord);
		transcription = transcription.replaceAll("��", "'").replaceAll("��", ",").replaceAll("��", ","); 
		transcriptionTextView.setText(transcription);
		
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
		
		loadAnswerOptions();
    }
    
    @Override
    public void onClick(View view) { 
		super.onClick(view);
		
		if(view.getId() == R.id.nextWordBtn) {    	        
	          learningListener.loadNextWord(); 
	    } else if(view.getId() == R.id.answer1 ) { 
	          lastAnswerId = R.id.answer1;
	          checkAnswer();    
	    } else if(view.getId() == R.id.answer2 ) {     	
	          lastAnswerId = R.id.answer2;
	          checkAnswer();  
	    } else if(view.getId() == R.id.answer3) {        
	          lastAnswerId = R.id.answer3;
	          checkAnswer();   
	    } else if(view.getId() == R.id.answer4) {        
	          lastAnswerId = R.id.answer4;
	          checkAnswer();   
	    }
	}
    
    protected void checkAnswer() {
		learningListener.playRecording();
		verifyAnswer();
		uncoverChallangeElements(); 
	}
    
    protected void verifyAnswer() { 
        
    	if(optionsIds.get(idxOfGoodAns) == lastAnswerId) { 
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
        
        nativeWordTextView.setVisibility(View.VISIBLE); 
        wordImageView.setVisibility(View.VISIBLE);
        
        uncovered = true; 
    }
    
    protected void hideChallangeElements() { 	
        foreignWordTextView.setVisibility(View.GONE);
        audioButton.setVisibility(View.GONE); 
        transcriptionTextView.setVisibility(View.GONE);
        checkAnswerScreen.setVisibility(View.GONE);
        textInputScreen.setVisibility(View.VISIBLE);  
        
        wordImageView.setVisibility(View.GONE);
        nativeWordTextView.setVisibility(View.VISIBLE); 
        
        uncovered = false; 
    }
    
    private void loadAnswerOptions() { 
    	
        ArrayList<Integer>  used = new ArrayList<Integer>();
        idxOfGoodAns =  (int) (Math.random() *4);
        int resId = optionsIds.get(idxOfGoodAns);
        
        Button ansGood = (Button) view.findViewById(resId);
        ansGood.setText(foreignWord); 
        
        // get wordIds collection & foreign words collection
        HashMap<Integer, String> foreignWords = learningListener.getForeignWords();
        ArrayList<Integer> wordIds = learningListener.getWordIds(); 
        
        int maxInt = foreignWords.size();
        used.add(wordId);
        
        if(wordIds.size() < 4) {
        	finishLearning();
        	return; 
        }
        
        for(int i = 0; i<4; i++) { 
            if(i == idxOfGoodAns)  { 
                continue;
            } else { 
                Button ansBad = (Button) view.findViewById(optionsIds.get(i));  
                int idx = (int) (Math.random() * maxInt);  
                while(used.contains(wordIds.get(idx))) { 
                    idx = (int) (Math.random() * maxInt); 
                }
                used.add(wordIds.get(idx));
                String randomBadWord = foreignWords.get(wordIds.get(idx));
               
                ansBad.setText(randomBadWord);  
            }
        }
    }
    
    private void finishLearning() 
    {
    	Toast.makeText(getActivity(), "Wordset contains less than 4 words, cannot generate learning method!", Toast.LENGTH_LONG).show();
    	getActivity().finish();
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
