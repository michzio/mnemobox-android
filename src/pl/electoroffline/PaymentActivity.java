package pl.electoroffline;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * The aim of this activity is to provide in-app 
 * billing item purchases in this application. 
 * Example item that the user can buy is:
 * - 1-year turning off ads. (Google unmanaged item)
 * This is managed only by app and elector website itself.
 * @author michzio
 *
 */
public class PaymentActivity extends DrawerActivity  {
	
	private static final String TAG = "PAYMENT_ACTIVITY_TAG"; 
	// private DrawerLayout drawerLayout;
	
	private PaymentFragment paymentFragment;
	private User user; 

	@Override
	protected void onCreateDrawerActivity(Bundle savedInstanceState) {
		
		setContentView(R.layout.main_drawer);
		
		// adding initial fragment using Fragment Transaction 
		FragmentManager fragmentManager = getSupportFragmentManager(); 
		paymentFragment = (PaymentFragment) fragmentManager.findFragmentByTag(PaymentFragment.TAG); 
		
		// IMPORTANT TO RETAIN CURRENT FRAGMENT ON SCREEN WHILE ex. ROTATING DEVICE 
		if(paymentFragment == null) { 
				FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction(); 
				paymentFragment = new PaymentFragment(); 
				fragmentTransaction.replace(R.id.main_content_frame, paymentFragment, MainFragment.TAG);
				
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
	
	// Initiating Menu XML file (menu.xml) 
	@Override 
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater menuInflater = getMenuInflater(); 
		//menuInflater.inflate(R.menu.some_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	/**
	 * Event Handling for individual menu item selected 
	 * Identifying single menu item by it's id. 
	 */
	@Override 
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()) { 
		
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
			Toast.makeText(PaymentActivity.this, R.string.main_singing_out, Toast.LENGTH_SHORT).show(); 
		}
	}

	@Override
	protected int getRightDrawerMenuButtonId() {

		return 0;
	}
	 
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
	    boolean handled = false;

	    // The following is a hack to ensure that the InAppPurchasesFragment receives
	    // its onActivityResult call.
	    //
	    // For more information on this issue, read here:
	    //
	    // http://stackoverflow.com/questions/14131171/calling-startintentsenderforresult-from-fragment-android-billing-v3
	    //
	    // Note: If Google ever fixes the issue with startIntentSenderForResult() and
	    // starts forwarding on the onActivityResult to the fragment automatically, we
	    // should future-proof this code so it will still work.
	    //
	    // If we don't do anything and always call super.onActivityResult, we risk 
	    // having the billing fragment's onActivityResult called more than once for
	    // the same result.
	    //
	    // To accomplish this, we create a method called checkIabHelperHandleActivityResult
	    // in the billing fragment that returns a boolean indicating whether the result was 
	    // handled or not.  We would just call Fragment's onActivityResult method, except 
	    // its return value is void.
	    //
	    // Then call this new method in the billing fragment here and only call 
	    // super.onActivityResult if the billing fragment didn't handle it.
	    
	    Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data+ ")");

	    if (paymentFragment != null)
	    {
	        handled = paymentFragment.checkIabHelperHandleActivityResult(requestCode, resultCode, data);
	    }

	    if (!handled)
	    {
	        super.onActivityResult(requestCode, resultCode, data);
	    }
	}

}
