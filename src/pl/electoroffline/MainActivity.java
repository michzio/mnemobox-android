package pl.electoroffline;

import info.semsamot.actionbarrtlizer.ActionBarRtlizer;
import info.semsamot.actionbarrtlizer.RtlizeEverything;

import java.util.ArrayList;

import pl.elector.service.WordsLoaderService;
import pl.electoroffline.R;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

public class MainActivity extends DrawerActivity
{	
	
	private MainPagerFragment mainPagerFragment;
	
    private User user; 
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        // adding initial fragment using Fragment Transaction
        FragmentManager fragmentManager =  getSupportFragmentManager(); 
        mainPagerFragment = (MainPagerFragment) fragmentManager.findFragmentByTag(MainPagerFragment.TAG);
        
        // IMPORTANT TO RETAIN CURRENT FRAGMENT ON SCREEN WHILE ex. ROTATING DEVICE
        if(mainPagerFragment == null) { 
	        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        mainPagerFragment = new MainPagerFragment();
	        fragmentTransaction.replace(R.id.main_content_frame, mainPagerFragment, MainPagerFragment.TAG); 
	        fragmentTransaction.addToBackStack("mainPagerFragmentBack"); 
	        fragmentTransaction.commit();
        }
        
    	user = new User( MainActivity.this);
    	if(!user.isLoggedIn()) { 
    		Toast.makeText(this, R.string.anonymous_user_toast, Toast.LENGTH_LONG).show(); 
    	}
    	
    	// when restarting Activity passing that we have new intent to analyzes
    	onNewIntent(getIntent());
        
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
        	FragmentManager fragmentManager =  getSupportFragmentManager(); 
        	Fragment fragment = fragmentManager.findFragmentById(R.id.main_content_frame); 
        	
        	Log.w(MainActivity.class.getName(), "Key Event Back current fragment is: " + fragment.getTag() + ".");
        	
        	if(fragment.getTag().equals(LearnedWordsFragment.TAG))
        	{	
        		((LearnedWordsFragment) fragment).backPressed();

        	} else if(fragment.getTag().equals(RememberMeFragment.TAG))
        	{
        		((RememberMeFragment) fragment).backPressed(); 
        		
        	} else if(fragment.getTag().equals(ForgottenFragment.TAG))
        	{
        		((ForgottenFragment) fragment).backPressed();
        	} else if(fragment.getTag().equals(MainPagerFragment.TAG)) {
        		finish();
        		return true; 
        	}
        	
        }
        return super.dispatchKeyEvent(event);
    }
    
    
    @Override
    public void onBackPressed() {
    	
    	FragmentManager fragmentManager =  getSupportFragmentManager(); 
    	Fragment fragment = fragmentManager.findFragmentById(R.id.main_content_frame); 
    	
    	Log.w(MainActivity.class.getName(), "Back Pressed current fragment is: " + fragment.getTag() + ".");
    	
    	if(fragment.getTag().equals(ForgottenFragment.TAG)) {
    		if(!((ForgottenFragment) fragment).allowBackPressed())
    			return; 
    	}
    	
    	drawerMenuAdapter.notifyDataSetChanged();
    	
    	super.onBackPressed();
    }
    
     // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        
        // Find the actionbar's menuItem to add overflow menu sub_menu 
        MenuItem overflowMenuItem = menu.findItem(R.id.menu_overflow);
        // Inflating the sub_menu menu this way, will add its menu items 
        // to the empty SubMenu you created in the xml
        getMenuInflater().inflate(R.menu.main_menu_content, overflowMenuItem.getSubMenu());
       
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override 
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        return super.onPrepareOptionsMenu(menu);
    }
 
    /**
     * Event Handling for Individual menu item selected
     * Identify single menu item by it's id
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	
        switch (item.getItemId())
        {
	        case R.id.menu_logout:
	            logoutUser();
	            return true;
	        case R.id.menu_donate: 
	        	Intent profileIntent = new Intent(MainActivity.this, PaymentActivity.class);
                startActivity(profileIntent); 
	        	return true; 
	        case R.id.menu_settings:
	        	Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
	        	startActivityForResult(settingsIntent, SettingsActivity.MAIN_ACTIVITY_REQUEST_CODE); 
	            return true;
	        default:
	            break; 
        }
        
        return super.onOptionsItemSelected(item);
    } 
    
    private void logoutUser() { 
        if(user.logOut()) { 
            Toast.makeText(MainActivity.this, R.string.main_singing_out,
                            Toast.LENGTH_SHORT).show();
        }
    }


	@Override
	public boolean onGroupClick(ExpandableListView parent, View v,
			int groupPosition, long id) {
		
		return super.onGroupClick(parent, v, groupPosition, id);
	} 

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		
		return super.onChildClick(parent, v, groupPosition, childPosition, id);
	}
	
	@Override
	protected void drawerMenuItemClicked(long id) {
		
		/* DEPRECATED: earlier used in each switch->case!
		if(selectedDrawerItemView != null) { 
			selectedDrawerItemView.setBackgroundResource(R.drawable.drawer_item_shape2);
			selectedDrawerItemView = null; 
        }*/
		
		switch((int) id) {
			
			case 0: // vocabulary
				break;
			case 1: // dictionary 
				break; 
			case 2: // profile info 
				break;
			case 3: // learned words 
			{
				/** deprecated - fragment changed to activity
				FragmentManager fragmentManager = getSupportFragmentManager(); 
                fragmentManager.beginTransaction()
                			   .add(R.id.main_content_frame, new LearnedWordsFragment(), LearnedWordsFragment.TAG)
                			   .addToBackStack("learnedWordsFragmentBack")
                			   .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
                			   .commit();
                drawerLayout.closeDrawers();
                */
                break; 
			}
			case 4: // forgotten words 
			{ 
				/** deprecated - fragment changed to activity
				FragmentManager fragmentManager =getSupportFragmentManager(); 
                fragmentManager.beginTransaction()
                			   .add(R.id.main_content_frame, new ForgottenFragment(),ForgottenFragment.TAG)
              				   .addToBackStack("forgottenFragmentBack")
                			   .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
                			   .commit();
                drawerLayout.closeDrawers();
                */
                
                break;
			}
			case 5: // remember me words 
			{ 
				/** deprecated - fragment changed to activity
				FragmentManager fragmentManager = getSupportFragmentManager(); 
                fragmentManager.beginTransaction()
                			   .add(R.id.main_content_frame, new RememberMeFragment(), RememberMeFragment.TAG)
                			   .addToBackStack("rememberMeFragmentBack")
                			   .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
                			   .commit();
                drawerLayout.closeDrawers();
                */
				
                break;
			}
			case 6: // user wordsets
				break; 
			case 7: // learning history 
				break; 
			case 8: // langwish playlist
				break; 
			default: 
				break;
		}
		
		super.drawerMenuItemClicked(id);
	}

	@Override
	protected void onCreateDrawerActivity(Bundle savedInstanceState) {
		
	}

	@Override
	protected int getRightDrawerMenuButtonId() {
		
		return R.id.taskNotificationsBtn;
	}
	
	@Override 
	public void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		
		// updating Intent
		setIntent(intent); 
		
		// called when pending intent from Notification relaunch Activity 
		// or when user moves back from Learning Activity after selecting 
		// selected (ex. forgotten) words synchronization on Dialog prompt.
		if(getIntent().getBooleanExtra(WordsetWordsAccessor.KEY_START_SELECTED_WORDS_SYNCHRONIZATION, false)) 
		{ 	 
			// user wants to automatically start synchronization process!
			startSelectedWordsSynchronization(); 
		}
	}
	
	/**
     * Helper method used to start selected words synchronization service.
     * Used when new intent is received with special extra flag set to TRUE.
     * ex. Intent from Dialog in learning Activity like PresentationActivity. 
     */
	private void startSelectedWordsSynchronization()
	{
		// launching Service that will load data associated with selected words
		// such as: words, words details, images, and if user wants audio recordings
		Intent serviceIntent = new Intent(MainActivity.this, WordsLoaderService.class);
		// 0 means special (not system) wordset
		serviceIntent.putExtra(WordsLoaderService.DOWNLOADED_WORDSET_ID, 0); 
		serviceIntent.putExtra(WordsLoaderService.WORDSET_TITLE, "Synchronizing words...");
		
		//setting array list of word ids of selected words to download 
		ArrayList<String> wordIds = getIntent().getStringArrayListExtra(WordsetWordsAccessor.KEY_WORDS_TO_SYNC);
		serviceIntent.putExtra(WordsLoaderService.WORDS_TO_SYNC, wordIds);
		startService(serviceIntent);
		//showProgressDialog();
		Log.w(WordsetActivity.class.getName(), "WordsLoaderService has been started...");
	}
	
	private void showProgressDialog() { 
		// create new progress dialog 
		progressDialog = new ProgressDialog(this); 
		// setting the progress dialog to display a horizontal progress bar 
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		// setting the dialog title to 'Downloading words data...'
		progressDialog.setTitle("Downloading words data..."); 
		// setting the dialog message to 'Initializing download...'
		progressDialog.setMessage("Initializing downlod...");
		// this dialog can't be canceled by pressing the back key
		progressDialog.setCancelable(true);
		progressDialog.setCanceledOnTouchOutside(false);
		// this dialog isn't indeterminate
		progressDialog.setIndeterminate(false); 
		// setting the initial max number of items 
		progressDialog.setMax(100); 
		// setting the current progress to zero 
		progressDialog.setProgress(0); 
		// display the progress dialog
		progressDialog.show(); 
}
	
	private ProgressDialog progressDialog = null;
	
	/**
     * Broadcast Receiver to receive info about selected words synchronization
     * progress. This informations are uset to display download progress 
     * on dialog window above Main Activity. 
     */
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			Bundle bundle = intent.getExtras(); 
			if(bundle != null) { 
				
				int wordsSize = bundle.getInt(WordsLoaderService.WORDS_SIZE); 
				int wordsCounter = bundle.getInt(WordsLoaderService.WORDS_COUNT);
				
				Log.w(WordsetActivity.class.getName(), "Broadcast received! Downloaded " + wordsCounter + "/" + wordsSize + " words.");
				
				if(progressDialog == null) {
					showProgressDialog(); 
				}
				if(wordsCounter < wordsSize) { 
					progressDialog.setMax(wordsSize);
					progressDialog.setProgress(wordsCounter); 
					progressDialog.setMessage("Downloaded " + wordsCounter + "/" + wordsSize + " words.");
				} else { 
					progressDialog.setMax(wordsSize);
					progressDialog.setProgress(wordsCounter); 
					progressDialog.setMessage("Download completed!");
					progressDialog.setCancelable(true);
					progressDialog.setCanceledOnTouchOutside(true); 
				}
			}
		}
	};
	
	@Override 
    public void onResume() {
    	super.onResume(); 		
    	// register the broadcast receiver that receives words loader notifications
    	registerReceiver(receiver, new IntentFilter(WordsLoaderService.SELECTED_WORDS_LOADER_BROADCAST));
	}
	
	 @Override 
	 public void onPause() {
	    super.onPause(); 
	    // unregiser the broadcast receiver that receives words loader notifications
	    unregisterReceiver(receiver); 
	 }
	 
	 @Override
	 public void onActivityResult(int requestCode, int resultCode, Intent data) {

		    if (requestCode == SettingsActivity.MAIN_ACTIVITY_REQUEST_CODE) {
		         if(resultCode == RESULT_OK){
			          boolean menuLangChanged = data.getBooleanExtra(SettingsActivity.KEY_MENU_LANG_CHANGED, false);
			          
			          Log.d(MainActivity.class.getName(), "Menu language change detected in Main Activity: " + menuLangChanged); 
			          // restart activity to enforce language change
			          if(menuLangChanged) { 
			        	  Intent restartIntent = getIntent();
			      	      finish();
			      	      startActivity(restartIntent);
			          }
		         }
		    }
	}
}
