package pl.electoroffline;

import com.facebook.widget.LikeView;
import com.google.android.gms.analytics.GoogleAnalytics;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

public class TrackedActionBarActivity extends ActionBarActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Get a Tracker (should auto-report)
        ((ElectorApplication)getApplication()).getTracker(ElectorApplication.TrackerName.APP_TRACKER);
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		//Get an Analytics tracker to report app starts &amp; uncaught exceptions etc.
		GoogleAnalytics.getInstance(this).reportActivityStart(this);

	}
	
	public void onStop() 
	{
		
		//Stop the analytics tracking
		GoogleAnalytics.getInstance(this).reportActivityStop(this);
		super.onStop();
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TrackedActionBarActivity.class.getName(), "OnActivityResult...");
    }
}
