package pl.electoroffline;

import java.util.ArrayList;
import java.util.HashMap;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;

public abstract class LearningFragment extends Fragment implements View.OnClickListener  {
	
	public interface LearningListener { 
		public void playRecording(Integer wordId);
		public void playRecording();
		public void traceForgottenWord(Integer wordId, Personalization.Mood mood);
		public void addToForgottenDrawerList(Integer wordId); 
		public void traceLearnedWord(Integer wordId);
		public void loadNextWord();
		public void loadWordImage(ImageView imageView, Integer wordId);
		public void incrementGoodAns();
		public void incrementBadAns();
		public HashMap<Integer, String> getForeignWords();
		public ArrayList<Integer> getWordIds();
		public boolean checkCurrentWordAnswered();
		public boolean currentWordAnsweredSuccessfully();
	}
	
	protected LearningListener learningListener;
	
	protected Integer wordId; 
	protected String foreignWord;
	protected String nativeWord; 
	protected String transcription;
	
	protected boolean wordAnswered = false; 
	protected boolean wordAnsweredSuccessfully = false; 
	
	protected boolean isVisible = false;
	
	protected void onFragmentVisible() {}
	protected void onFragmentHidden() {} 
	
	public void setWordId(Integer wordId) {
		this.wordId = wordId; 
	}

	public void setForeignWord(String foreignWord) {
		this.foreignWord = foreignWord; 
	}
	
	public void setNativeWord(String nativeWord) { 
		this.nativeWord = nativeWord;
	}
	
	public void setTranscription(String transcription) {
		this.transcription = transcription; 
	}
	
	public void setLearningListener(LearningListener listener) 
	{
		learningListener = listener;
	}
	
	public void setWordAnswered(boolean flag) {
		wordAnswered = flag;
	}
	
	public void setWordAnsweredSuccessfully(boolean flag) 
	{
		wordAnsweredSuccessfully = flag; 
	}
	
	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible; 
		if(this.isVisible) { 
			onFragmentVisible();
		} else { 
			onFragmentHidden();
		}
	}
	
	@Override
    public void onClick(View view) { 
		
		if(view.getId() == R.id.presentationAudioBtn) {        	
           learningListener.playRecording(wordId);
        }
	}
	
	/***
	 * Event handler called when current Fragment has been
	 * swiped, and user has moved to next Fragment in forward 
	 * direction. Current Fragment is hiding on the screen. 
	 * Do here things like: 
	 * ex.1) if answer hasn't been checked (uncovered) yet (uncover it and check it now!)
	 */
	public void onSwipedForward() { }
	/***
	 * Event handler called when current Fragment has been 
	 * swiped to the screen now (is showing on the screen) 
	 * after swipe movement in forward direction. 
	 * Do here things like: 
	 * ex.1) if current word answer has been already aksed/answerd 
	 *       uncover it without checking. 
	 */
	public void onSwipingForward() { }
	
	/***
	 * Event handler called when current Fragment has been
	 * swiped, and user has moved to next Fragment in backward
	 * direction (i.e. previous Fragment is showin on the screen). 
	 * Current Fragment is hiding on the screen. 
	 * Do here things like: 
	 * ex.1) nothing special needed
	 */
	public void onSwipedBackward() { }
	
	/***
	 * Event handler called when current Fragment has been 
	 * swiped to the screen now (is showing on the screen) 
	 * after swipe movement in backward direction. 
	 * Do here things like: 
	 * ex.1) uncover word answer without checking it again.
	 */
	public void onSwipingBackward() { }
}
