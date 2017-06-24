package pl.electoroffline;

import pl.elector.database.LearningHistoryProvider.Mode;
import android.support.v7.app.ActionBar;

public class ListeningRTLActivity extends LearningMethodRTLActivity {

	@Override
	protected boolean isUsingViewPager() {
		return true;
	}

	@Override
	protected Class<?> getLearningFragmentClass() {
		return ListeningRTLFragment.class;
	}
	
	@Override 
	protected Mode getLearningMode() { 
		return Mode.ANDROID_LISTENING; 
	}
	
	@Override
	protected void createLayout() { 	
	   super.createLayout();

        // setting up action bar title
        ActionBar actionBar =  getSupportActionBar();
        actionBar.setTitle(getString(R.string.listening));
    }

}
