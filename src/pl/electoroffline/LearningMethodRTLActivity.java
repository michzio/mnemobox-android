package pl.electoroffline;

import java.util.Collections;

import android.support.v4.view.ViewPager;
import android.util.Log;

public abstract class LearningMethodRTLActivity extends LearningMethodActivity {

	
	abstract protected boolean isUsingViewPager();

	abstract protected Class<?> getLearningFragmentClass();
	
	@Override 
	protected void startLearning() { 
		
		if(widCollection != null) { 
			Collections.shuffle(widCollection); 
		    index = widCollection.size();
		    currWid = widCollection.get(widCollection.size()-1); // important! presetting current wordId on first word in newly loaded collection
		    // learning method helper collections
	        resetWordQuestionsAsked();
	        resetWordsAnswers();
	        
			learningMethodPagerAdapter.notifyDataSetChanged();
			
			loadNextWord();
			addAds();
		}
	}
	
	/**
	 * Method goes to the next word 
	 */
	public void loadNextWord() {
		 //hideChallangeElements();
		 if( (index - 1) > -1) { 
			 Log.w(LearningMethodActivity.class.getName(), "Current word index is: " + (index-1));
			 if(index > widCollection.size()) { 
				 // for first page in viewpager
				 learningMethodPager.setCurrentItem(widCollection.size()-1);
				 currWid = (Integer) widCollection.get(widCollection.size()-1);
				 index = widCollection.size()-1; 
			 } else {
				 // for next page in viewpager
				 learningMethodPager.setCurrentItem(index-1);
				 currWid = (Integer) widCollection.get(index); 
			 }
		 } else { 
	         finishLearning(); 
	     }
	}
	
	@Override 
	public void onSwipeOutAtEnd() { 
		super.onSwipeOutAtStart(); 
	}
	
	@Override 
	public void onSwipeOutAtStart() { 
		super.onSwipeOutAtEnd(); 
	}
}
