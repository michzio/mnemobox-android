package pl.electoroffline;

import pl.elector.database.LearningHistoryProvider.Mode;
import android.support.v7.app.ActionBar;

public class PresentationRTLActivity extends LearningMethodRTLActivity {

	@Override
	protected boolean isUsingViewPager() {
		return true;
	}

	@Override
	protected Class<?> getLearningFragmentClass() {
		return PresentationRTLFragment.class;
	}
	
	@Override
	protected Mode getLearningMode() { 
		return Mode.ANDROID_PRESENTATION; 
	}
	
	@Override
	protected void createLayout() { 	
	   super.createLayout();

        // setting up action bar title
        ActionBar actionBar =  getSupportActionBar();
        actionBar.setTitle(getString(R.string.presentation));
    }

}
