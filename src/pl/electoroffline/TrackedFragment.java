package pl.electoroffline;

import com.google.android.gms.analytics.GoogleAnalytics;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class TrackedFragment extends Fragment {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		//Get a Tracker (should auto-report)
        ((ElectorApplication) getActivity().getApplication()).getTracker(ElectorApplication.TrackerName.APP_TRACKER);
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		//Get an Analytics tracker to report app starts &amp; uncaught exceptions etc.
		GoogleAnalytics.getInstance(getActivity()).reportActivityStart(getActivity());

	}
	
	public void onStop() 
	{
		//Stop the analytics tracking
		GoogleAnalytics.getInstance(getActivity()).reportActivityStop(getActivity());

		super.onStop();
	}
}
