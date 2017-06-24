package pl.electoroffline;


import pl.electoroffline.R;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivityOld extends ActionBarActivity implements ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupClickListener  /*implements /* ActionBar.TabListener /*, SwipeInterface */
{
	private ActionBarDrawerToggle drawerToggle;
	private ExpandableListView leftDrawerList; 
	private ExpandableListView rightDrawerList; 
	private DrawerLayout drawerLayout;
	private DrawerMenuAdapter drawerMenuAdapter;
	
	private MainPagerFragment mainPagerFragment;
	
    private User user; 
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        setContentView(R.layout.main_drawer); 
        
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
        
    	user = new User( MainActivityOld.this);
    	if(!user.isLoggedIn()) { 
    		Toast.makeText(this, R.string.anonymous_user_toast, Toast.LENGTH_LONG).show(); 
    	} 
        
    	// configuring Left & Right Drawer  
    	configureLeftDrawer();
    	configureRightDrawer(); 
    	
        
        /** 
         *  // This code check if user is logged in, 
         *  // else redirect him to SignIn Activity
         * 	user = new User( MainActivity.this);
         *	if(!user.isLoggedIn()) { 
         *   	user.redirectToLogingPageActivity();
         *  }
         **/
       /* 
        buttonEvents();
     
        ActivitySwipeDetector swipe = new ActivitySwipeDetector(this);
        RelativeLayout swipe_layout = (RelativeLayout) findViewById(R.id.mainSwipeLayout);
        swipe_layout.setOnTouchListener(swipe); 
        */
        
    }
    
    
    private void configureLeftDrawer() 
    {
    	drawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);
    	leftDrawerList = (ExpandableListView) findViewById(R.id.main_left_drawer); 
    	
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
                 }
    		}
    		
    		/** Called when a drawer has settled in a completely open state. */
    		@SuppressWarnings("unused")
			public void onDrawerOpen(View drawerView) {
    			if(drawerView.equals(leftDrawerList)) {
                    //getSupportActionBar().setTitle(getString(R.string.app_name));
                    supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                    drawerToggle.syncState();
                }         
    		}
    	};
    	
    	// set the drawer toggle as the Drawer Listener 
    	drawerLayout.setDrawerListener(drawerToggle);
    	
    	getSupportActionBar().setDisplayHomeAsUpEnabled(true); 
    	getSupportActionBar().setHomeButtonEnabled(true); 
    }
    
    public class DrawerItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			//Toast.makeText(MainActivityOld.this, "DrawerItemClickListener", Toast.LENGTH_SHORT).show();
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
    
  
    public void configureRightDrawer() {
    	rightDrawerList = (ExpandableListView) findViewById(R.id.main_right_drawer);
    	
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
        	FragmentManager fragmentManager =  getSupportFragmentManager(); 
        	Fragment fragment = fragmentManager.findFragmentById(R.id.main_content_frame); 
        	
        	Log.w(MainActivityOld.class.getName(), "Key Event Back current fragment is: " + fragment.getTag() + ".");
        	
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
    	
    	Log.w(MainActivityOld.class.getName(), "Back Pressed current fragment is: " + fragment.getTag() + ".");
    	
    	if(fragment.getTag().equals(ForgottenFragment.TAG)) {
    		if(!((ForgottenFragment) fragment).allowBackPressed())
    			return; 
    	}
    	
    	super.onBackPressed();
    }
    
    /*
    private void buttonEvents() { 
        
        Button vocabularyBtn = (Button)findViewById(R.id.vocabularyBtn);
        // Do this for each view added to the grid
        vocabularyBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
            Intent vocabularyIntent = new Intent(MainActivity.this, WordsetCategoriesActivity.class);
            startActivity(vocabularyIntent);   
                
            }
        });
        Button dictBtn = (Button)findViewById(R.id.dictBtn);
        dictBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
               Intent mainIntent = new Intent().setClass(MainActivity.this, DictActivity.class);
                startActivity(mainIntent); 
                
            }
        });
          Button taskBtn = (Button)findViewById(R.id.taskBtn);
         taskBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
              Intent mainIntent = new Intent().setClass(MainActivity.this, TaskActivity.class);
                startActivity(mainIntent);   
                
            }
        });
         Button profileBtn = (Button)findViewById(R.id.profileBtn);
         profileBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
               Intent mainIntent = new Intent().setClass(MainActivity.this, ProfileMainActivity.class);
                startActivity(mainIntent); 
                
            }
        });
        Button forgottenBtn = (Button)findViewById(R.id.forgottenBtn);
         forgottenBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
               Intent mainIntent = new Intent().setClass(MainActivity.this, ForgottenActivity.class);
                startActivity(mainIntent);   
                
            }
        });
         Button toRememberBtn = (Button)findViewById(R.id.toRememberBtn);
         toRememberBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
                Intent mainIntent = new Intent().setClass(MainActivity.this, RememberMeActivity.class);
                startActivity(mainIntent);  
            }
        });
    }*/
    
    /**
     * DEPRECATED:
    @Override
    public void left2right(View v) { 
        startActivity(new Intent(this, MoreAppsActivity.class));
    }
    @Override 
    public void right2left(View v) { 
         startActivity(new Intent(this, MoreAppsActivity.class));
    }
    @Override 
    public void top2bottom(View v) { 
        
    }
    @Override 
    public void bottom2top(View v) { 
        
    }
    */
    
     // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu_content, menu);
        menuInflater.inflate(R.menu.main_menu, menu);
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
 
    /**
     * Event Handling for Individual menu item selected
     * Identify single menu item by it's id
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	
        switch (item.getItemId())
        {
       
           case android.R.id.home: 
	        	// pass the event to ActionBarDrawerToggle, 
	           	// if it returns true, then it has handled 
	           	// the app icon touch event.
        	   if(drawerToggle.onOptionsItemSelected(item)) {        		   
        		   if(drawerLayout.isDrawerOpen(rightDrawerList))
                       drawerLayout.closeDrawer(rightDrawerList);
        	   }
        	   return true; 
           case R.id.taskNotificationsBtn:
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
	        case R.id.menu_logout:
	            logoutUser();
	            return true;
	        default:
	            break; 
        }
        
        return super.onOptionsItemSelected(item);
    } 
    
    private void logoutUser() { 
        if(user.logOut()) { 
            Toast.makeText(MainActivityOld.this, R.string.main_singing_out,
                            Toast.LENGTH_SHORT).show();
        }
    }


	@Override
	public boolean onGroupClick(ExpandableListView parent, View v,
			int groupPosition, long id) {
		return true;
	}

	private View selectedDrawerItemView = null;
	private long selectedDrawerItemId = -1; 

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		
		/*
		DrawerMenuItem menuItem = (DrawerMenuItem) drawerMenuAdapter.getChild(groupPosition, childPosition);
		
		Toast.makeText(this, "Drawer list item: " 
								+ menuItem.title() + " clicked with id: " + id, Toast.LENGTH_LONG).show();
		v.setBackgroundColor(getResources()
                .getColor(R.drawable.buttonColor));
		int index = parent.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
		parent.setItemChecked(index, true);
		*/
		if(parent == leftDrawerList) {
			if(selectedDrawerItemView != null) { 
				selectedDrawerItemView.setBackgroundResource(R.drawable.drawer_item_shape2);
	        }
			
	        v.setBackgroundResource(R.drawable.drawer_item_shape_pressed2);
	        selectedDrawerItemView = v;
	        selectedDrawerItemId = id; 
        
	        drawerMenuItemClicked(id);
		}
		
		return true;
	}
	
	private void drawerMenuItemClicked(long id) {
		
		switch((int) id) {
			
			case 0: // vocabulary
				Intent vocabularyIntent = new Intent(this, WordsetCategoriesActivity.class);
				startActivity(vocabularyIntent); 
				if(selectedDrawerItemView != null) { 
					selectedDrawerItemView.setBackgroundResource(R.drawable.drawer_item_shape2);
					selectedDrawerItemView = null; 
		        }
				break;
			case 1: // dictionary 
				Intent dictIntent = new Intent(this, DictActivity.class);
				startActivity(dictIntent); 
				if(selectedDrawerItemView != null) { 
					selectedDrawerItemView.setBackgroundResource(R.drawable.drawer_item_shape2);
					selectedDrawerItemView = null; 
		        }
				break; 
			case 2: // profile info 
				Intent profileIntent = new Intent(this, ProfileInfoActivity.class);
				startActivity(profileIntent); 
				if(selectedDrawerItemView != null) { 
					selectedDrawerItemView.setBackgroundResource(R.drawable.drawer_item_shape2);
					selectedDrawerItemView = null; 
		        }
			case 3: // learned words 
			{
				
				FragmentManager fragmentManager = getSupportFragmentManager(); 
                fragmentManager.beginTransaction()
                			   .add(R.id.main_content_frame, new LearnedWordsFragment(), LearnedWordsFragment.TAG)
                			   .addToBackStack("learnedWordsFragmentBack")
                			   .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
                			   .commit();
                drawerLayout.closeDrawers();
                break;
			}
			case 4: // forgotten words 
			{ 
				
				FragmentManager fragmentManager =getSupportFragmentManager(); 
                fragmentManager.beginTransaction()
                			   .add(R.id.main_content_frame, new ForgottenFragment(),ForgottenFragment.TAG)
              				   .addToBackStack("forgottenFragmentBack")
                			   .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
                			   .commit();
                drawerLayout.closeDrawers();
                break;
			}
			case 5: // remember me words 
			{ 
				
				FragmentManager fragmentManager = getSupportFragmentManager(); 
                fragmentManager.beginTransaction()
                			   .add(R.id.main_content_frame, new RememberMeFragment(), RememberMeFragment.TAG)
                			   .addToBackStack("rememberMeFragmentBack")
                			   .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
                			   .commit();
                drawerLayout.closeDrawers();
                break;
			}
			case 6: // user wordsets
				
				break; 
			case 7: // learning history 
				
				break; 
			case 8: 
				  String playlist_id = "PL7KxEIIDcPaWeAim0Rr7ICpiDG1CunE4t";
				  Uri uri = Uri.parse("http://www.youtube.com/playlist?list=" + playlist_id);
				  Intent i = new Intent(Intent.ACTION_VIEW);
				  i.setData(uri);
				  i.setPackage("com.google.android.youtube");
				  startActivity(i);
				  if(selectedDrawerItemView != null) { 
						selectedDrawerItemView.setBackgroundResource(R.drawable.drawer_item_shape2);
						selectedDrawerItemView = null; 
			        }
				break; 
			default: 
				break;
		}
	}


    /**
	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		
	}
	*/
}
