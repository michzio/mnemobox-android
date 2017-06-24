package pl.electoroffline;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class FlashCardsFragment extends LearningFragment {
	
	private View view; 
	private TextView foreignWordTextView; 
	private TextView nativeWordTextView;
	private TextView transcriptionTextView;
	private ImageView wordImageView; 
	private ImageView audioButton; 
	private Button reloadCardButton; 
	private Button badButton; 
	private Button normalButton; 
	private Button goodButton; 
	
	private boolean uncovered = false; 

	@Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		
		view = inflater.inflate(R.layout.flashcards, container, false);
		
		loadLayout();
		setWord();
		
		return view; 
	}	
	
	private void loadLayout() {
		
		foreignWordTextView = (TextView) view.findViewById(R.id.presentationWordEN);
        nativeWordTextView = (TextView) view.findViewById(R.id.presentationWordPL);
        transcriptionTextView = (TextView) view.findViewById(R.id.presentationWordTrans);
        wordImageView = (ImageView) view.findViewById(R.id.presentationWordImage);
        audioButton = (ImageView) view.findViewById(R.id.presentationAudioBtn);
        audioButton.setOnClickListener(this);
        reloadCardButton = (Button) view.findViewById(R.id.reloadCardBtn);
        reloadCardButton.setOnClickListener(this);
         
        badButton = (Button) view.findViewById(R.id.badBtn);
        badButton.setOnClickListener(this);
        normalButton = (Button) view.findViewById(R.id.normalBtn);
        normalButton.setOnClickListener(this);
        goodButton = (Button) view.findViewById(R.id.goodBtn);
        goodButton.setOnClickListener(this);
	}
	
	private void setWord() {
		
		foreignWordTextView.setText(foreignWord);
		nativeWordTextView.setText(nativeWord);
		transcription = transcription.replaceAll("��", "'");
		transcription = transcription.replaceAll("��", ",").replaceAll("��", ","); 
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
            
        } else if(view.getId() == R.id.reloadCardBtn) { 
        	uncoverFlashCard(view);
        }
	}
	
	 public void uncoverFlashCard(View v) 
	 {
	        if(uncovered) { 
	            foreignWordTextView.setVisibility(View.GONE);
	            transcriptionTextView.setVisibility(View.GONE);
	            nativeWordTextView.setVisibility(View.VISIBLE);
	            uncovered = false; 
	        } else { 
	            foreignWordTextView.setVisibility(View.VISIBLE);
	            transcriptionTextView.setVisibility(View.VISIBLE);
	            nativeWordTextView.setVisibility(View.GONE);
	            uncovered = true;
	            learningListener.playRecording(wordId);
	        }
	 }

	@Override
	protected void onFragmentVisible() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onFragmentHidden() {
		// TODO Auto-generated method stub
		
	}
	
}
