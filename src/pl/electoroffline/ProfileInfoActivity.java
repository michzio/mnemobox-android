/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import pl.electoroffline.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;


/**
 *
 * @author Michał Ziobro
 */
public class ProfileInfoActivity extends DrawerActivity {
    
	//private ActionBarDrawerToggle drawerToggle;
	//private ListView drawerList; 
	DrawerLayout drawerLayout; 
	
	private ProfileInfoFragment profileInfoFragment;
	private User user; 
	
	@Override
    protected void onCreateDrawerActivity(Bundle savedInstanceState) {
			
			setContentView(R.layout.main_drawer); 

			// adding initial fragment using Fragment Transaction
	        FragmentManager fragmentManager =  getSupportFragmentManager(); 
	        profileInfoFragment = (ProfileInfoFragment) fragmentManager.findFragmentByTag(ProfileInfoFragment.TAG);
	        
	        // IMPORTANT TO RETAIN CURRENT FRAGMENT ON SCREEN WHILE ex. ROTATING DEVICE
	        if(profileInfoFragment == null) { 
		        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		        profileInfoFragment = new ProfileInfoFragment();
		        fragmentTransaction.replace(R.id.main_content_frame, profileInfoFragment, MainFragment.TAG); 
		        
		        fragmentTransaction.commit();
	        }
	        
	        user = new User(this);
	    	if(!user.isLoggedIn()) { 
	    		
	    	} 
	    	
	}
	
	@Override 
    public void onResume() 
    {
    	super.onResume();
    	
    }
	/*
	private void configureLeftDrawer() 
    {
    	drawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout); 
    	drawerList = (ListView) findViewById(R.id.main_left_drawer); 
    	//drawerList.setAdapter(); 
    	//drawerList.setOnItemClickListener(listener)
    	drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, 
    											R.drawable.ic_drawer, 
    											R.string.main_drawer_open,
    											R.string.main_drawer_close) {
    		
    		// Called when a drawer has settled in a completely closed state. 
    		public void onDrawerClosed(View view) {
    			
    		}
    		
    		// Called when a drawer has settled in a completely open state. 
    		public void onDrawerOpen(View drawerView) {
    			
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
			// TO DO: Drawer Item click listener 
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
    */
    
    // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
 
    /**
     * Event Handling for Individual menu item selected
     * Identify single menu item by it's id
     **/
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	/*
    	// pass the event to ActionBarDrawerToggle, 
    	// if it returns true, then it has handled 
    	// the app icon touch event. 
    	if(drawerToggle.onOptionsItemSelected(item)) {
    		return true; 
    	}
    	*/
    	
        switch (item.getItemId())
        {
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
            Toast.makeText(ProfileInfoActivity.this, R.string.main_singing_out,
                            Toast.LENGTH_SHORT).show();
        }
    }

	@Override
	protected int getRightDrawerMenuButtonId() {
		
		return R.id.taskNotificationsBtn;
	}
	
	public void donateApp(View v) { 
		startActivity(new Intent(this, PaymentActivity.class)); 
	}
    
}
