package pl.electoroffline;

import com.facebook.widget.LikeView;
import com.facebook.Settings;

import info.semsamot.actionbarrtlizer.ActionBarRtlizer;
import info.semsamot.actionbarrtlizer.RtlizeEverything;

import java.util.ArrayList;
import java.util.Locale;

import pl.elector.database.ProfileProvider;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public abstract class DrawerActivity extends TrackedActionBarActivity implements OnChildClickListener, OnGroupClickListener {

	protected ActionBarDrawerToggle drawerToggle;
	protected ExpandableListView leftDrawerList; 
	protected ExpandableListView rightDrawerList; 
	protected DrawerLayout drawerLayout;
	protected DrawerMenuAdapter drawerMenuAdapter;
	
	protected View newUserHeader; 
	
	abstract protected void onCreateDrawerActivity(Bundle savedInstanceState); 
	abstract protected int getRightDrawerMenuButtonId();
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	 super.onCreate(savedInstanceState);
    	 setContentView(R.layout.main_drawer);
    	 
    	// forceRTLIfSupported(); // only from API 17+
    	 
    	 onCreateDrawerActivity(savedInstanceState);
    	 
    	// configuring Left & Right Drawer  
     	 configureLeftDrawer();
     	 configureRightDrawer(); 
     	
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void forceRTLIfSupported()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
        	Log.d(DrawerActivity.class.getName(), "SDK 17+, supporting View.LAYOUT_DIRECTION_LOCALE"); 
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        }
    }
    
    @Override 
    public void onResume()
    {
    	super.onResume();
    	
    	// checking if user is currently logged in and loading profile data if so 
    	if(Preferences.getInt(this, Preferences.KEY_PROFILE_ID, 0) > 0) { 
    		loadProfileAccount( Preferences.getString(this, Preferences.KEY_EMAIL, ""), false);
    	}
    	
    	drawerMenuAdapter.notifyDataSetChanged();
    }

    protected void configureLeftDrawer() 
    {
    	drawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);
    	leftDrawerList = (ExpandableListView) findViewById(R.id.main_left_drawer);
    	
    	addNewUserHeader(leftDrawerList);
    	addFacebookLikeButtonFooter(leftDrawerList);
    	
    	drawerMenuAdapter = new DrawerMenuAdapter(this, R.raw.left_drawer_menu); 
    	leftDrawerList.setAdapter(drawerMenuAdapter); 
    	leftDrawerList.setOnChildClickListener(this);
    	leftDrawerList.setOnGroupClickListener(this); 
    	for(int i=0; i < drawerMenuAdapter.getGroupCount(); i++)
    			leftDrawerList.expandGroup(i);

    	drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, 
    											//R.drawable.ic_drawer, 
    											R.string.main_drawer_open,
    											R.string.main_drawer_close) {
    		
    		/** Called when a drawer has settled in a completely closed state. */
    		public void onDrawerClosed(View drawerView) {
    			 if(drawerView.equals(leftDrawerList)) {
                     //getSupportActionBar().setTitle(getTitle());
                     supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                     drawerToggle.syncState();
                     Log.w(ActionBarDrawerToggle.class.getName(), "Left Drawer Closed!");
                     onLeftDrawerClosed(drawerView);
                 }  else if(drawerView.equals(rightDrawerList)) { 
                 	drawerToggle.syncState();
                 	Log.w(ActionBarDrawerToggle.class.getName(), "Right Drawer Closed!");
                 	onRightDrawerClosed(drawerView);
                 }
    		}
    		
    		/** Called when a drawer has settled in a completely open state. */
    		public void onDrawerOpened(View drawerView) {
    			if(drawerView.equals(leftDrawerList)) {
                    //getSupportActionBar().setTitle(getString(R.string.app_name));
                    supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                    drawerToggle.syncState();
                    Log.w(ActionBarDrawerToggle.class.getName(), "Left Drawer Opened!");
                    onLeftDrawerOpened(drawerView);
                } else if(drawerView.equals(rightDrawerList)) { 
                	drawerToggle.syncState();
                	Log.w(ActionBarDrawerToggle.class.getName(), "Right Drawer Opened!");
                	onRightDrawerOpened(drawerView);
                }
    		}
    	};
    	
    	// set the drawer toggle as the Drawer Listener 
    	drawerLayout.setDrawerListener(drawerToggle);
    	
    	getSupportActionBar().setDisplayHomeAsUpEnabled(true); 
    	getSupportActionBar().setHomeButtonEnabled(true); 
    	
    }
    
    View likeButtonFooter = null;
    
    private void addFacebookLikeButtonFooter(ExpandableListView drawerList) {
		if(likeButtonFooter == null)
		{
			likeButtonFooter = getLayoutInflater().inflate(R.layout.likebutton, null); 
		}
		
		// To initialize Facebook SDK in your app
        Settings.sdkInitialize(this);
        // Get LikeView button
        LikeView likeView = (LikeView) likeButtonFooter.findViewById(R.id.like_view);
        // Set the object for which you want to get likes from your users (Photo, Link or even your FB Fan page)
        likeView.setObjectId("https://www.facebook.com/electorpl");
        // Set foreground color for Like count text
        likeView.setForegroundColor(Color.BLACK);
        likeView.setLikeViewStyle( LikeView.Style.STANDARD);
        likeView.setAuxiliaryViewPosition(LikeView.AuxiliaryViewPosition.INLINE);
        likeView.setHorizontalAlignment(LikeView.HorizontalAlignment.LEFT);
        
        drawerList.addFooterView(likeButtonFooter);
		
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LikeView.handleOnActivityResult(this, requestCode, resultCode, data);
        Log.i(DrawerActivity.class.getName(), "OnActivityResult...");
    }
    
	private void addNewUserHeader(ExpandableListView drawerList) 
    {
    	if(newUserHeader == null) 
    		newUserHeader = getLayoutInflater().inflate(R.layout.new_user_header, null);
    	drawerList.addHeaderView(newUserHeader);
    	newUserHeader.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showChooseAccountDialog();
			}
		});
    	
    	/** DEPRECARED: move to onResume method 
    	 *	// checking if user is currently logged in and loading profile data if so 
    	 *	if(Preferences.getInt(this, Preferences.KEY_PROFILE_ID, 0) > 0) { 
    	 *		onChosenAccount( Preferences.getString(this, Preferences.KEY_EMAIL, ""));
    	 *	}
    	 **/
    }
    
    String chosenAccount = null;
    
	private void showChooseAccountDialog() 
    {
    	Log.w(DrawerActivity.class.getName(), "User clicked to show 'Change User Account Dialog'.");
    	
    	final ArrayList<String> accountList = new ArrayList<String>();
    	
    	// accessing all profile accounts stored locally
    	Cursor cursor = getContentResolver().query(ProfileProvider.CONTENT_URI, null, null, null, null); 
    	
    	while(cursor.moveToNext()) { 
    		String email = cursor.getString(cursor.getColumnIndexOrThrow(ProfileProvider.ProfileTable.COLUMN_EMAIL));
    		accountList.add(email); 
    	}
    	
    	accountList.add(getResources().getString(R.string.add_account)); 
    	
    	final AlertDialog dialog = new AlertDialog.Builder(this)
    			.setTitle(R.string.choose_account)
    			.setSingleChoiceItems(accountList.toArray(new String[] {}), -1, 
    							new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										
										Log.w(AlertDialog.class.getName(), "User selected account in position: " + which + "."); 
										((AlertDialog)dialog).getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
										
										chosenAccount = accountList.get(which);
									}
								})
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Log.w(AlertDialog.class.getName(), "User clicked positive button on Choose account dialog.");
						
						if(chosenAccount == null) return; 
						
						if(chosenAccount.equals(getResources().getString(R.string.add_account))) {
							Log.w(AlertDialog.class.getName(), "User wants to sign into new account."); 
							Intent loginIntent = new Intent(DrawerActivity.this, LogInActivity.class);
							startActivity(loginIntent); 
						} else { 
							Log.w(AlertDialog.class.getName(), "User has chosen different account with email: " + chosenAccount);
							onChosenAccount(chosenAccount); 
						}
						
					}
				})
				.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Log.w(AlertDialog.class.getName(), "User clicked negative button on Choose account dialog.");
						
					}
				})
    			.create();
 
    	dialog.show();
    	dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
    }
	
	private void onChosenAccount(String email) { 
		Log.d(DrawerActivity.class.getName(), "user has chosen a new account..."); 
		loadProfileAccount(email, true); 
		
	}
    
    /**
     * Handler called when user has chosen a new account on alert dialog
     * @param email
     */
    private void loadProfileAccount(String email, boolean replaceProfileInPreferences) 
    {
    	
    	String selection = ProfileProvider.ProfileTable.COLUMN_EMAIL + " =  ? ";
    	String[] selectionArgs = new String[] { email };
    	Cursor cursor = getContentResolver().query(ProfileProvider.CONTENT_URI, null,selection , selectionArgs, null);
    	
    	if(cursor.getCount() == 1) { 
    		cursor.moveToFirst();
    		
    		int profileId = cursor.getInt(cursor.getColumnIndexOrThrow(ProfileProvider.ProfileTable.COLUMN_PROFILE_ID));
    		String newEmail = cursor.getString(cursor.getColumnIndexOrThrow(ProfileProvider.ProfileTable.COLUMN_EMAIL));
    		String sha1Password = cursor.getString(cursor.getColumnIndexOrThrow(ProfileProvider.ProfileTable.COLUMN_SHA1PASS));
    		
    		if(replaceProfileInPreferences) { 
    			User.replaceProfileInPreferences(this, profileId, newEmail,sha1Password); 
    		}
    		
    		// change User header view on drawer list 
    		String firstName = cursor.getString(cursor.getColumnIndexOrThrow(ProfileProvider.ProfileTable.COLUMN_FIRST_NAME));
    		String lastName = cursor.getString(cursor.getColumnIndexOrThrow(ProfileProvider.ProfileTable.COLUMN_LAST_NAME));
    		// TO DO: cursor.getBlob(cursor.getColumnIndexOrThrow(ProfileProvider.ProfileTable.COLUMN_IMAGE));
    		// currently database is storing only name of image file on web server
    		TextView emailView = ( (TextView) newUserHeader.findViewById(R.id.user_email));
    		TextView nameView = ( (TextView) newUserHeader.findViewById(R.id.user_name));
    		// ImageView avatarView = ( (ImageView) newUserHeader.findViewById(R.id.user_icon));
    		emailView.setText(newEmail); 
    	 
    		if(firstName.length() > 0 || lastName.length() > 0) { 
    			nameView.setText(firstName + " " + lastName);
    		} else { 
    			nameView.setText(R.string.no_name); 
    		}
    		//avatarView.setImageBitmap(); TO DO! 
    		
    		newUserHeader.findViewById(R.id.new_user_account).setVisibility(View.GONE);
    		newUserHeader.findViewById(R.id.user_account_info).setVisibility(View.VISIBLE); 
    		
    	} else { 
    		Toast.makeText(this, getString(R.string.error_while_switching_accounts), Toast.LENGTH_SHORT).show();
    	}
    }
    
    
    public class DrawerItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			//Toast.makeText(DrawerActivity.this, "DrawerItemClickListener", Toast.LENGTH_SHORT).show();
		} 
    	
    }
   
    @Override 
    protected void onPostCreate(Bundle savedInstanceState) {
    	super.onPostCreate(savedInstanceState);
    	// Sync the toggle state after onRestoreInstanceState has occurred.
    	drawerToggle.syncState();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
    	super.onConfigurationChanged(newConfig);
    	drawerToggle.onConfigurationChanged(newConfig);
    }
    
    public static boolean isRTL() {
        return isRTL(Locale.getDefault());
    }

    public static boolean isRTL(Locale locale) {
        final int directionality = Character.getDirectionality(locale.getDisplayName().charAt(0));
        return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
               directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }
    
  
    protected void configureRightDrawer() {
    	
    	rightDrawerList = (ExpandableListView) findViewById(R.id.main_right_drawer);
    }
    
    private boolean isRTLActionBarEnabled = false; 
    
    public void setRTLActionBarEnabled(boolean flag) {
    	isRTLActionBarEnabled = true;
    }
    
    /**
     * Uses ActionBarRTLizer library to support 
     * action bar in right-to-left configuration 
     * ex. arabic locales (SDK 7+ solution) 
     */
    protected void supportRTLActionBar() { 
    	if (isRTL() && !isRTLActionBarEnabled) {  //in Right To Left layout
    		
    		Log.d(DrawerActivity.class.getName(), "Configuring RTL action bar..."); 
    		isRTLActionBarEnabled = true;
	    	
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
	     
    	} else if(isRTL()) { 
    		 ActionBarRtlizer rtlizer = new ActionBarRtlizer(this);
    		 RtlizeEverything.rtlize((ViewGroup)rtlizer.getActionMenuView());
    	}
    }
    
    protected Menu mainMenu; 
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	
    	if (keyCode == KeyEvent.KEYCODE_MENU) {
    		Log.d(DrawerActivity.class.getName(), "Hardware Menu button touched."); 
    		mainMenu.performIdentifierAction(R.id.menu_overflow, 0);
            return true;
        }
    	
        return super.onKeyUp(keyCode, event);
    }
    
    // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    { 
    	mainMenu = menu; 
    	
    	supportRTLActionBar();
        return super.onCreateOptionsMenu(menu);
    }
    
    
    @Override 
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	// If the navigation drawer is open, hide action items related to the content view
        for(int i = 0; i< menu.size(); i++)
            menu.getItem(i).setVisible(!drawerLayout.isDrawerOpen(leftDrawerList));

        return super.onPrepareOptionsMenu(menu);
    }
    
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	if(isRTL()) { //right-to-left languages
    		
	        if(item.getItemId() == getRightDrawerMenuButtonId())
	        {
	        	if(drawerLayout.isDrawerOpen(leftDrawerList))
                    drawerLayout.closeDrawer(leftDrawerList);
        	   
	        	if(drawerLayout.isDrawerOpen(rightDrawerList))
	        		drawerLayout.closeDrawer(rightDrawerList);
	        	else 
	        		drawerLayout.openDrawer(rightDrawerList);
        	   
        	    return true; 
    		
	        } else if(item.getItemId() == android.R.id.home) {
    			 
    			if(drawerLayout.isDrawerOpen(rightDrawerList))
                    drawerLayout.closeDrawer(rightDrawerList);
        	   
	        	if(drawerLayout.isDrawerOpen(leftDrawerList))
	        		drawerLayout.closeDrawer(leftDrawerList);
	        	else 
	        		drawerLayout.openDrawer(leftDrawerList);
        	   
        	    return true; 
    		}
    	} else { 
    	
	        if(item.getItemId() == android.R.id.home)
	        {
		        	// pass the event to ActionBarDrawerToggle, 
		           	// if it returns true, then it has handled 
		           	// the app icon touch event.
	        	   if(drawerToggle.onOptionsItemSelected(item)) {        		   
	        		   if(drawerLayout.isDrawerOpen(rightDrawerList))
	                       drawerLayout.closeDrawer(rightDrawerList);
	        	   }
	        	   return true; 
	        } else if(item.getItemId() == getRightDrawerMenuButtonId()) {
		        	// pass the event to ActionBarDrawerToggle, 
		           	// if it returns true, then it has handled 
		           	// the app icon touch event.
	        	   drawerToggle.onOptionsItemSelected(item);     		   
	        	   if(drawerLayout.isDrawerOpen(leftDrawerList))
	                    drawerLayout.closeDrawer(leftDrawerList);
	        	   
	        	   if(drawerLayout.isDrawerOpen(rightDrawerList))
	        		   drawerLayout.closeDrawer(rightDrawerList);
	        	   else 
	        		   drawerLayout.openDrawer(rightDrawerList);
	        	   
	        	   return true; 
	        } 
    	}
        
        return super.onOptionsItemSelected(item);
    } 
    
    @Override
	public boolean onGroupClick(ExpandableListView parent, View v,
			int groupPosition, long id) {
		return true;
	}
    
    protected View selectedDrawerItemView = null;
	protected long selectedDrawerItemId = -1; 
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		
		if(parent == leftDrawerList) {
			/*
			if(selectedDrawerItemView != null) { 
				selectedDrawerItemView.setBackgroundResource(R.drawable.drawer_item_shape2);
	        }*/
			
	        //v.setBackgroundResource(R.drawable.drawer_item_shape_pressed2);
	        selectedDrawerItemView = v;
	        selectedDrawerItemId = id; 
        
	        drawerMenuItemClicked(id);
		}
		
		return true;
	}
	
	
	protected void drawerMenuItemClicked(long id) {
		
		switch((int) id) {
			
			case 0: // vocabulary
				Intent vocabularyIntent = new Intent(this, WordsetCategoriesActivity.class);
				startActivity(vocabularyIntent); 
				
				break;
			case 1: // dictionary 
				Intent dictIntent = new Intent(this, DictActivity.class);
				startActivity(dictIntent); 
				break; 
			case 2: // profile info 
				Intent profileIntent = new Intent(this, ProfileInfoActivity.class);
				startActivity(profileIntent); 
				break; 
			case 3: // learned words 
			{
				Intent learnedWordsIntent = new Intent(this, LearnedWordsActivity.class);
				startActivity(learnedWordsIntent); 
                break;
			}
			case 4: // forgotten words 
			{ 
				Intent forgottenIntent = new Intent(this,ForgottenActivity.class);
				startActivity(forgottenIntent); 
                break;
			}
			case 5: // remember me words 
			{ 
				Intent rememberMeIntent = new Intent(this, RememberMeActivity.class);
				startActivity(rememberMeIntent); 
                break;
			}
			case 6: // user wordsets
				
				break; 
			case 7: // learning history 
				Intent learningHistoryIntent = new Intent(this, HistoryActivity.class); 
				startActivity(learningHistoryIntent); 
				break; 
			case 8: 
				  String playlist_id = "PL7KxEIIDcPaWeAim0Rr7ICpiDG1CunE4t";
				  Uri uri = Uri.parse("http://www.youtube.com/playlist?list=" + playlist_id);
				  Intent i = new Intent(Intent.ACTION_VIEW);
				  i.setData(uri);
				  i.setPackage("com.google.android.youtube");
				  startActivity(i);
				break; 
			default: 
				break;
		}
	}
	
	
	/* Left & Right Drawer Listeners */ 
	public void onLeftDrawerOpened(View drawerView) {
		
	}
	
	public void onRightDrawerOpened(View drawerView) {
	
	}
	
	public void onLeftDrawerClosed(View drawerView) {
		
	}
	
	public void onRightDrawerClosed(View drawerView) {
		
	}
	
	@Override
	public void onDestroy() {
	      super.onDestroy();
	      ExpandableListAdapter adapter = null;
	      this.leftDrawerList.setAdapter(adapter);
	}
	
}
