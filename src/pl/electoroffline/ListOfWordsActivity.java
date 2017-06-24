/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


/**
 *
 * @author Michał Ziobro
 */
public class ListOfWordsActivity extends DrawerActivity  {
    
	private ListOfWordsFragment listOfWordsFragment;
    
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
        listOfWordsFragment = (ListOfWordsFragment) fragmentManager.findFragmentByTag(ListOfWordsFragment.TAG);
        
        // IMPORTANT TO RETAIN CURRENT FRAGMENT ON SCREEN WHILE ex. ROTATING DEVICE
        if(listOfWordsFragment == null) { 
	        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        listOfWordsFragment = new ListOfWordsFragment();
	        fragmentTransaction.replace(R.id.main_content_frame, listOfWordsFragment, ListOfWordsFragment.TAG); 
	        //fragmentTransaction.addToBackStack("listOfWordsFragmentBack");
	        fragmentTransaction.commit();
        }
 		
     }

	@Override
	protected int getRightDrawerMenuButtonId() {
		return 0;
	}
	
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
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        
        return super.onOptionsItemSelected(item);
    }
    
}
