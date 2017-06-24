/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * @author Michał Ziobro 
 */
public class WordsetCategoriesActivity extends DrawerActivity {
	
	public static final String SELECTED_WORDSET_CATEGORY = "SELECTED_WORDSET_CATEGORY"; 
	
	private WordsetCategoriesFragment wordsetCategoriesFragment;
	private User user;
    
     /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);  
    }
    
	@Override 
    public void onResume() 
    {
    	super.onResume();
    	
    }

	@Override
	protected void onCreateDrawerActivity(Bundle savedInstanceState) {
		
		setContentView(R.layout.main_drawer);
        
		// adding initial fragment using Fragment Transaction
        FragmentManager fragmentManager =  getSupportFragmentManager(); 
        wordsetCategoriesFragment = (WordsetCategoriesFragment) fragmentManager.findFragmentByTag(WordsetCategoriesFragment.TAG);
        
        // IMPORTANT TO RETAIN CURRENT FRAGMENT ON SCREEN WHILE ex. ROTATING DEVICE
        if(wordsetCategoriesFragment == null) { 
	        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        wordsetCategoriesFragment = new WordsetCategoriesFragment();
	        fragmentTransaction.replace(R.id.main_content_frame, wordsetCategoriesFragment, WordsetCategoriesFragment.TAG); 
	        
	        fragmentTransaction.commit();
        }
        
        user = new User(this);
    	if(!user.isLoggedIn()) { 
    		
    	} 
        
	}
    
   
	@Override
	protected int getRightDrawerMenuButtonId() {
		
		return R.id.taskNotificationsBtn;
	}

	// Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	
        return super.onCreateOptionsMenu(menu);
    }
 
    /**
     * Event Handling for Individual menu item selected
     * Identify single menu item by it's id
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	 if(item.getItemId() == getRightDrawerMenuButtonId()) {
    		 // can be implemented right drawer icon click
    	 }
        return super.onOptionsItemSelected(item);
    } 
    
    @SuppressWarnings("unused")
	private void logoutUser() { 
        if(user.logOut()) { 
            Toast.makeText(WordsetCategoriesActivity.this, R.string.main_singing_out,
                            Toast.LENGTH_SHORT).show();
        }
    }
}
