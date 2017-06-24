package pl.electoroffline;

import info.semsamot.actionbarrtlizer.ActionBarRtlizer;
import info.semsamot.actionbarrtlizer.RtlizeEverything;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup;

public class SettingsActivity extends ActionBarActivity {
    
	private static final String TAG = "SETTINGS_ACTIVITY_TAG"; 
	
	public static final int MAIN_ACTIVITY_REQUEST_CODE = 1; 
	public static final String KEY_MENU_LANG_CHANGED = "KEY_MENU_LANG_CHANGED";
	
	private SettingsFragment settingsFragment;
	private User user; 
	
	/**
	 * Called when the Activity is first created 
	 */
	@Override 
	public void onCreate(Bundle savedInstanceState) { 
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings); 
		
		// adding initial fragment using Fragment Transaction 
		FragmentManager fragmentManager = getSupportFragmentManager(); 
		settingsFragment = (SettingsFragment) fragmentManager.findFragmentByTag(SettingsFragment.TAG); 
		
		// IMPORTANT TO RETAIN CURRENT FRAGMENT ON SCREEN WHILE ex. ROTATING DEVICE 
		if(settingsFragment == null) { 
			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction(); 
			settingsFragment = new SettingsFragment(); 
			fragmentTransaction.replace(R.id.settings_frame, settingsFragment, SettingsFragment.TAG); 
			
			fragmentTransaction.commit(); 
		}
		
		user = new User(this); 
		if(!user.isLoggedIn()) { 
			// do some action while user isn't logged in...
		}
	}
	
	@Override 
	public void onResume() { 
		super.onResume(); 
	}
	
	protected void restartActivity() {
		
	    Intent restartIntent = getIntent();
	    // set result intent that will be returned to Main Activity on back button press
	    Intent resultIntent = new Intent();
	    resultIntent.putExtra(KEY_MENU_LANG_CHANGED, true); 
	    setResult(RESULT_OK, resultIntent);
	    finish();
	    
	    startActivity(restartIntent);
	    
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
    	
		if(DrawerActivity.isRTL()) { 
			Log.d(DrawerActivity.class.getName(), "Configuring RTL action bar..."); 
	        // RTLizing ActionBar and it's children.
	        ActionBarRtlizer rtlizer = new ActionBarRtlizer(this);
	 
	        RtlizeEverything.rtlize(rtlizer.getActionBarView());
	 
	       if (rtlizer.getHomeViewContainer() instanceof ViewGroup) {
	            RtlizeEverything.rtlize((ViewGroup) rtlizer.getHomeViewContainer());
	        }
	 
	        ViewGroup homeView = (ViewGroup) rtlizer.getHomeView();
	        RtlizeEverything.rtlize(homeView);
	        rtlizer.flipActionBarUpIconIfAvailable(homeView);
	        
	        RtlizeEverything.rtlize((ViewGroup)rtlizer.getActionMenuView());
		}
		return super.onCreateOptionsMenu(menu);
	}
	
}
