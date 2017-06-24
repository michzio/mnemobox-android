package pl.electoroffline;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

public class HistoryFragment extends Fragment
							 implements LoaderManager.LoaderCallbacks<Cursor> {
	
	public static final String TAG = "HISTORY_FRAGMENT_TAG";
	private static final int LEARNING_HISTORY_LOADER = 0x01; 
	
	private View view;
	private ScrollView scrollView;
	
	private GetHistoryFromXML historyObjReader; 
	
	@Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		view = inflater.inflate(R.layout.learnhistory, container, false);
		scrollView = (ScrollView) view.findViewById(R.id.historyScrollView); 
		
		loadLearningHistory(); 
		
		
		int profileId = Preferences.getInt(getActivity(), Preferences.KEY_PROFILE_ID, 0);
        if(profileId > 0) { 
        	if(NetworkUtilities.haveNetworkConnection(getActivity())) { 
        		
        	} else { 
        		// promptToConnectWithInternet();
        	}
        } else { 
        	new User(getActivity()).showPromptToLogIn(); 
        }
		return view;
		
	}
	
	@Override 
	public void onResume() { 
		super.onResume(); 
		onFragmentResume(); 
	}
	
	
	private void onFragmentResume() {
		
		ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
		actionBar.setTitle(getString(R.string.learnhistory_header));
		actionBar.setSubtitle(null);
		
		getActivity().getSupportLoaderManager().restartLoader(LEARNING_HISTORY_LOADER, null, this);
	}

	/**
	 * Method loads learning history objects 
	 */
	private void loadLearningHistory() { 
		
		// checking if there is network connectivity via WIFI or mobile data
		// and checking whether user prefer to load data from web service or stored in local database
		if(NetworkUtilities.haveNetworkConnection(getActivity())
    			&& Preferences.getBoolean(getActivity(), Preferences.KEY_PREFER_ONLINE_DATA, false) ) {
			
			// loading data (Learning History Objects) from online web service 
			loadLearningHistoryFromWebService(); 
			
		} else {
    		
    		// load data (Learning History Objects) from local data source (SQLiteDatabase)
    		loadLearningHistoryFromDatabase(); 
    	}
	}

	
	/**
	 * Method that loads Learning History objects from online web service. 
	 * User must be logged in. If this is anonymous account just try to 
	 * load not synchronized, locally stored (in SQLite database) learning history objects 
	 * for anonymous account.  
	 */
	private void loadLearningHistoryFromWebService() {
		
		
	}

	/**
	 * Method that loads Learning History objects from local SQLite database.
	 * To load data asynchronously we are using CursorLoader object.
	 * If there isn't any data just show message "No learning history for current account."
	 */
	private void loadLearningHistoryFromDatabase() {
		
		getActivity().getSupportLoaderManager().initLoader(LEARNING_HISTORY_LOADER, null, this); 
	}

	/*** Implementation of LoaderManager.LoaderCallbacks<Cursor> interface methods: ***/
	
	/**
	 * This method is used to create CursorLoader after initLoader() call
	 * @param id - will be used to create and return different loaders
	 * @param args - is a Bundle that contains additional arguments for constructing Loaders
	 * @return Loader<Cursor> object 
	 */
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		
		CursorLoader cursorLoader = null; 
		
		switch(id) { 
			
			case LEARNING_HISTORY_LOADER: 
				
				// CursorLoader is used to construct the new query. 
				String[] projection = { };  // TO DO 
				String where = null; 
				String[] whereArgs = null; 
				String sortOrder = null; 
				
				// Query Content URI for retrieving all learning history rows
				Uri queryUri = null;  // TO DO 
				
				// Create and return the new CursorLoader 
				cursorLoader = new CursorLoader(getActivity(), queryUri, projection, where, whereArgs, sortOrder); 
				break; 
		}
		
		return cursorLoader;
	}
	
	/**
	 * 
	 * @param arg0
	 * @param arg1
	 */
	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		// TODO Auto-generated method stub
		
	}

	
	/**
	 * 
	 * @param arg0
	 */
	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub
		
	}
}
