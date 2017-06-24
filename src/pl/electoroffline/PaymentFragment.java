package pl.electoroffline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import billing_util.IabHelper;
import billing_util.IabResult;
import billing_util.Inventory;
import billing_util.Purchase;
import billing_util.SkuDetails;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class PaymentFragment extends Fragment {

	public static final String TAG = "PAYMENT_FRAGMENT_TAG"; 
	private static final String BASE64_ENCODED_PUBLICK_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhy5O3gk30UGUMZ4xSqjI6bt6yPt9EsxxSkhTElbtZcAQ+dcukRoc08rAyU+8fuVWrL6su+tf24ePrOXX5CqO9UXd5p2zkNz8iWn5zanlaeDEJB2TchZNIwc6E7R6z9JRrG1m2wyoWb8M2RW6YDi9XcAXl82Z19YUbfgcYWvmkuFpNXntCrTZXyyOuVLm3isK02nd41KHEmu10fpjtC0v+K1bhG1sQWCW1nkXDfK2p3h+om/HZp1WdBciF+g/dPC9hjowdrHHU/46sd0X53DYQx4oGwuXM12IAYApXwifzEEsCbGPnxWktONy+UE9Bxc0azy2XzFMCN/vgNrCToimvwIDAQAB";
	
	private View view; 
	private IabHelper iabHelper; 
	
	 // (arbitrary) request code for the purchase flow
    private static final int RC_REQUEST = 10001;
	
    private static final String SKU_DONATE_LEVEL_3 = "donate_lvl_3"; 
	private static final String SKU_DONATE_LEVEL_2 = "donate_lvl_2"; 
	private static final String SKU_TURN_OFF_ADS = "donate_lvl_1"; 
	
	private static final List<String> SKU_list; 
	static { 
		SKU_list = new ArrayList<String>(); 
		SKU_list.add(SKU_TURN_OFF_ADS);
		SKU_list.add(SKU_DONATE_LEVEL_2);
		SKU_list.add(SKU_DONATE_LEVEL_3);
	}
	
	private Spinner spinner;
	private PurchaseItemAdapter purchaseItemAdapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		view = inflater.inflate(R.layout.payment, container, false); 
		
		int profileId = Preferences.getInt(getActivity(), Preferences.KEY_PROFILE_ID, 0); 
		if(profileId > 0) { 
			if(NetworkUtilities.haveNetworkConnection(getActivity())) { 
				 // there is internet connectivity, it is possible to purchase items
				 setUpPurchaseItemsSpinner();
				 loadPurchaseSystem();
				 updateUi(); 
			} else { 
				 // there's no internet connection, prompt to connect 
				 promptToConnectWithInternet(); 
			}
		} else { 
			new User(getActivity()).showPromptToLogIn(); 
		}
		return view;
	}
	
	@Override 
	public void onResume()
	{
		super.onResume(); 
		onFragmentResume(); 
	}
	
	private void onFragmentResume() { 
		ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar(); 
		actionBar.setTitle("Purchase Items"); 
		actionBar.setSubtitle(null);
	}
	
	/**
	 * Initialization of Purchase Items Spinner control. 
	 */
	private void setUpPurchaseItemsSpinner() { 
		spinner = (Spinner) view.findViewById(R.id.purchase_items_spinner);
		purchaseItemAdapter = new PurchaseItemAdapter(getActivity(), new ArrayList<PurchaseItem>());
		spinner.setAdapter(purchaseItemAdapter);
		 
		Log.d(TAG, "Purchase Items Spinner has been set up."); 
	}
	
	/**
	 * Loading info about purchase items 
	 */
	private void loadPurchaseItemsIntoSpinner(Inventory inventory) { 
		
		
		for(String SKU : SKU_list) { 
			SkuDetails skuDetails =  inventory.getSkuDetails(SKU);
			
			if(skuDetails != null) { 
				PurchaseItem purchaseItem = new PurchaseItem(skuDetails.getSku(), 
															 skuDetails.getTitle().substring(0, skuDetails.getTitle().indexOf(" (")), 
															 skuDetails.getPrice(), 
															 skuDetails.getDescription());
				Log.d(TAG, skuDetails.toString());
				purchaseItemAdapter.add(purchaseItem);
			} else { 
				Log.d(TAG, "No skuDetails found for SKU: " + SKU); 
			}
		}
		
		// attaching onClick event listener to purchase button
		Button purchaseButton = (Button) view.findViewById(R.id.purchase_button);
		purchaseButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onBuyItemButtonClicked(v);
			}
		});
		
	}
	
	/**
	 * This function set ups purchase system.
	 * Which enable buying new items and 
	 * viewing history of current purchases. 
	 */
	private void loadPurchaseSystem() { 
		
		iabHelper = new IabHelper(getActivity(), BASE64_ENCODED_PUBLICK_KEY);
		iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			
			@Override
			public void onIabSetupFinished(IabResult result) {
				if(!result.isSuccess()) { 
					// Oh noes, there was a problem. 
					Log.d(TAG, "Problem setting up In-app Billing: " + result);
				}
				
				// Have we been disposed of in the meantime? If so, quit.
                if (iabHelper == null) return;
                
				// Hooray, IAB is fully set up! Now, let's get an inventory of stuff we own.
                iabHelper.queryInventoryAsync(true, SKU_list, iabGotInventoryListener);
			}
		});
	}
	
	// Listener that's called when we finish querying the items and subscriptions we own
	IabHelper.QueryInventoryFinishedListener iabGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		
		@Override
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			Log.d(TAG, "Query inventory finished.");
			
			// Have we been disposed of in the meantime?
			if(iabHelper == null) return; 
			
			// Is it a failure?
			if(result.isFailure()) { 
				complain("Failed to query inventory: " + result); 
				return; 
			}
			
			Log.d(TAG, "Query inventory was successfull."); 
			
			/*
			 * Load items descriptions into spinner control. 
			 */
			loadPurchaseItemsIntoSpinner(inventory); 
			
			/*
			 * Check for items we own. Notice that for each purchase, we check 
			 * the developer payload to see if it's correct! See 
			 * verifyDeveloperPayload(). 
			 */
			
			// Check for purchase delivery -- if we own purchase this should be marked in database/phone storage 
			for(String SKU : SKU_list) { 
				Purchase purchase = inventory.getPurchase(SKU); 
				if (purchase != null && verifyDeveloperPayload(purchase)) { 
					Log.d(TAG, "We have '" +  SKU + "' item. Consuming it."); 
					iabHelper.consumeAsync(inventory.getPurchase(SKU), iabConsumeFinishedListener);
					return; 
				}
			}
		
			updateUi(); 
			setWaitScreen(false); 
			Log.d(TAG, "Initial inventory query finished; enabling main UI"); 
			
		}
	};
	
	// User clicked the Donate button with specific SKU set. 
	public void onBuyItemButtonClicked(View v) { 
		
		Log.d(TAG, "Buy item button clicked."); 
		
		// launch the item purchase UI flow. 
		// we will be notified of completion via iabPurchaseFinishedListener
		setWaitScreen(true); 
		Log.d(TAG, "Launching purches flow for item."); 
		
		/*
		 * For security, generate payload here for verification.
		 * See the comments on verifyDeveloperPayload() for more info. 
		 */
		String payload = ""; 
		
		PurchaseItem selectedPurchaseItem = (PurchaseItem) spinner.getSelectedItem(); 
		
		iabHelper.launchPurchaseFlow(getActivity(), selectedPurchaseItem.getSKU(), RC_REQUEST,
				iabPurchaseFinishedListener, payload);
		
	}
	
	/**
	 * Allow the IabHelper to process an onActivityResult if it can
	 * 
	 * @param requestCode The request code
	 * @param resultCode The result code
	 * @param data The data
	 * 
	 * @return true if the IABHelper handled the result, else false
	 */

	public boolean checkIabHelperHandleActivityResult(int requestCode, int resultCode, Intent data)
	{
		// Pass on the activity result to the helper for handling 
	    return (iabHelper != null) && iabHelper.handleActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
	    if (!checkIabHelperHandleActivityResult(requestCode, resultCode, data))
	    {
	        super.onActivityResult(requestCode, resultCode, data);
	    }
	}
	
	
	/**
	 * Verifies the developer payload of a purchase.
	 * @param purchase
	 */
	boolean verifyDeveloperPayload(Purchase purchase) 
	{
		String payload = purchase.getDeveloperPayload(); 
		
		/*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

		return true; 
	}
	
	/**
	 *  callback for when a purchase is finished
	 */
	IabHelper.OnIabPurchaseFinishedListener iabPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		
		@Override
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase); 
			
			// if we were disposed of in the meantime, quit. 
			if(iabHelper == null) return; 
			
			if(result.isFailure()) { 
				complain("Error purchasing: " + result); 
				setWaitScreen(false); 
				return; 
			}
			if(!verifyDeveloperPayload(purchase)) { 
				complain("Error purchasing. Authenticity verification failed.");
				setWaitScreen(false); 
				return; 
			}
			
			Log.d(TAG, "Purchase successful."); 
			
			for(String SKU : SKU_list) { 
				
				if(purchase.getSku().equals(SKU)) { 
					Log.d(TAG, "Purchase is '" + SKU + "'. Starting consumption.");
					iabHelper.consumeAsync(purchase, iabConsumeFinishedListener);
				}
			}
		
		}
	};
	
	/**
	 * Called when consumption is complete
	 */
	IabHelper.OnConsumeFinishedListener iabConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
		
		@Override
		public void onConsumeFinished(Purchase purchase, IabResult result) {
			Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);
			
			// if we were disposed of in the meantime, quit. 
			if(iabHelper == null) return; 
			
			if(result.isSuccess()) {
				// successfully consumed purchase item
				persistPurchasedItem(purchase);
			} else { 
				complain("Error while consuming: " + result); 
			}
			
			updateUi();
			setWaitScreen(false); 
			Log.d(TAG, "End consumption flow."); 
		}
			
	};
	
	private void persistPurchasedItem(Purchase purchase) {
		
		Log.d(TAG, "Persisting purchased item in shared preferences and server side."); 
		
		if(purchase.getSku().equals(SKU_TURN_OFF_ADS)) {
			// successfully consumed, so we apply the effects of the SKU_TURN_OFF_ADS item
			// in our app logic: storing ads switching and unlocking elector.pl 
		} else if(purchase.getSku().equals(SKU_DONATE_LEVEL_2)) { 
			// successfully consumed, so we apply the effects of the SKU_DONATE_LEVEL_2 item
			// in our app logic: storing ads switching and unlocking elector.pl 
		} else if (purchase.getSku().equals(SKU_DONATE_LEVEL_3)) { 
			// successfully consumed, so we apply the effects of the SKU_DONATE_LEVEL3 item 
			// in our app logic: storing ads switching and unlocking elector.pl
		}
		
		// here we use the same persistent logic for all items that can be purchased
		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime(); 
		calendar.add(Calendar.YEAR, 1);
		Date nextYear = calendar.getTime(); 
		
		Log.d(TAG, "Persisting Expiration Date: "+ nextYear.toString());
		
		// set expiration date for ads free & paid up account services as one year ahead from now
		String emailAddress = Preferences.getString(getActivity(), Preferences.KEY_EMAIL, null);
		if(emailAddress == null) { 
			Log.d(TAG, "Email address not found in default SharedPreferences. Could not persist purchase item."); 
			return; 
		}
		Preferences preferences = new Preferences(getActivity(), emailAddress);
		preferences.putDate(Preferences.KEY_TURN_OFF_ADS_EXPIRATION_DATE, nextYear); 
		preferences.putDate(Preferences.KEY_PAID_UP_ACCOUNT_EXPIRATION_DATE, nextYear); 
		
		// deprecated: (in past expiration date has been stored in default SharedPreferences)
		// Preferences.putDate(getActivity(), Preferences.KEY_TURN_OFF_ADS_EXPIRATION_DATE, nextYear); 
		// Preferences.putDate(getActivity(), Preferences.KEY_PAID_UP_ACCOUNT_EXPIRATION_DATE, nextYear);
		
		Log.d(TAG, "GET request to activate full elector.pl access."); 
		// storing in server side database info about paid up access to elector.pl system
		Payments.activateFullAccess(getActivity()); 
	}
	
	/**
	 * Updating user interface after successful purchase item consumption
	 */
	private void updateUi() { 
		
		String emailAddress = Preferences.getString(getActivity(), Preferences.KEY_EMAIL, null);
		if(emailAddress == null) { 
				Log.d(TAG, "Email address not found in default SharedPreferences. Could not retrieve expiration dates."); 
				return; 
		}
		Preferences preferences = new Preferences(getActivity(), emailAddress); 
		Date adsFreeDate = preferences.getDate(Preferences.KEY_TURN_OFF_ADS_EXPIRATION_DATE, new Date(0L)); 
		Date paidUpAccountDate = preferences.getDate(Preferences.KEY_PAID_UP_ACCOUNT_EXPIRATION_DATE, new Date(0L)); 
		
		// deprecated: (in past expiration date has been stored in default SharedPreferences)
		// Date adsFreeDate = Preferences.getDate(getActivity(), Preferences.KEY_TURN_OFF_ADS_EXPIRATION_DATE, new Date(0L));
		// Date paidUpAccountDate = Preferences.getDate(getActivity(), Preferences.KEY_PAID_UP_ACCOUNT_EXPIRATION_DATE, new Date(0L));  
		
		Log.d(TAG, "Updating UI, adsFreeDate: " + adsFreeDate.toString() + ", paidUpAccountDate: " + paidUpAccountDate.toString());
		
		setDateOnTextView(R.id.ads_free_valid_thru_date, adsFreeDate); 
		setDateOnTextView(R.id.paid_up_valid_thru_date, paidUpAccountDate); 
		
		checkPaidUpAccountExpirationDateOnServerSideAsync(onPaidUpDateResponseListener);
	}
	
	private void setDateOnTextView(int resId, Date date) { 
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
		
		TextView dateTextView = (TextView) view.findViewById(resId);
		if(date.getTime() == 0L) {
			dateTextView.setText(R.string.service_no_available); 
		} else { 
			dateTextView.setText(sdf.format(date)); 
		}
	}
	
	private void checkPaidUpAccountExpirationDateOnServerSideAsync(
						final PaymentFragment.OnPaidUpDateResponseListener callback) {
		
		final Handler handler = new Handler();
		
		(new Thread(new Runnable() {

			@Override
			public void run() {
				
				// check paid up account expiration date on server-side - user could bought account via www
				String email = Preferences.getString(getActivity(), Preferences.KEY_EMAIL, ""); 
				String pass = Preferences.getString(getActivity(), Preferences.KEY_SHA1_PASSWORD, "");
				String url = getString(R.string.check_paid_up_valid_thru_url, email, pass);
				Log.d(PaymentFragment.class.getName(), "Check paid up valid thru url: " + url); 
				
				Date serverSideDate = null; 
				try {
					String result = CustomHttpClient.executeHttpGet(url);
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					serverSideDate = format.parse(result);
					if(serverSideDate.getTime() != 0L) { 
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(serverSideDate);
						calendar.add(Calendar.YEAR, 1);
						serverSideDate = calendar.getTime(); 
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				Log.d(TAG, "Server side date: " + serverSideDate); 
				
				final Date serverSideDate_f = serverSideDate;
				
				if(callback != null) { 
					 handler.post(new Runnable() {
	                     public void run() {
	                         callback.onPaidUpDateResponseListener(serverSideDate_f);
	                     }
	                 });
				}
			} 
			
		})).start();
	}
	
	/**
	 * Callback that notifies when the paid up account expiration date response is returned from server REST API.
	 */
	private interface OnPaidUpDateResponseListener {
		
		public void onPaidUpDateResponseListener(Date serverSideExpirationDate); 
	}
	
	PaymentFragment.OnPaidUpDateResponseListener onPaidUpDateResponseListener = new PaymentFragment.OnPaidUpDateResponseListener()
	{
		
		@Override
		public void onPaidUpDateResponseListener(Date serverSideDate) {
			
			String emailAddress = Preferences.getString(getActivity(), Preferences.KEY_EMAIL, null);
			if(emailAddress == null) { 
					Log.d(TAG, "Email address not found in default SharedPreferences. Could not retrieve expiration dates."); 
					return; 
			}
			Preferences preferences = new Preferences(getActivity(), emailAddress); 
			Date adsFreeDate = preferences.getDate(Preferences.KEY_TURN_OFF_ADS_EXPIRATION_DATE, new Date(0L)); 
			Date paidUpAccountDate = preferences.getDate(Preferences.KEY_PAID_UP_ACCOUNT_EXPIRATION_DATE, new Date(0L));
			
			// deprecated: (in past expiration date has been sotred in default SharedPreferences)
			// Date adsFreeDate = Preferences.getDate(getActivity(), Preferences.KEY_TURN_OFF_ADS_EXPIRATION_DATE, new Date(0L));
			// Date paidUpAccountDate = Preferences.getDate(getActivity(), Preferences.KEY_PAID_UP_ACCOUNT_EXPIRATION_DATE, new Date(0L));  
			
			// check whether paid up account valid thru date on server side is after date in shared preferences
			if(serverSideDate != null && serverSideDate.after(paidUpAccountDate)) {
				preferences.putDate(Preferences.KEY_TURN_OFF_ADS_EXPIRATION_DATE, serverSideDate); 
				preferences.putDate(Preferences.KEY_PAID_UP_ACCOUNT_EXPIRATION_DATE, serverSideDate);
				adsFreeDate = preferences.getDate(Preferences.KEY_TURN_OFF_ADS_EXPIRATION_DATE, new Date(0L));
				paidUpAccountDate = preferences.getDate(Preferences.KEY_PAID_UP_ACCOUNT_EXPIRATION_DATE, new Date(0L));
			}
			
			setDateOnTextView(R.id.ads_free_valid_thru_date, adsFreeDate); 
			setDateOnTextView(R.id.paid_up_valid_thru_date, paidUpAccountDate);
		}
	};
	
	/**
	 * Enables or disables the "please wait" screen.
	 */
	private void setWaitScreen(boolean set) {
		view.findViewById(R.id.screen_main).setVisibility( set ? View.GONE : View.VISIBLE); 
		view.findViewById(R.id.screen_wait).setVisibility( set ? View.VISIBLE : View.GONE); 
	}
	
	private void complain(String message) {
	        Log.e(TAG, "**** TrivialDrive Error: " + message);
	        alert("Error: " + message);
	}
	
	private void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }
	
	@Override 
	public void onDestroy() { 
		 super.onDestroy();
		 
		 if (iabHelper != null) iabHelper.dispose();
		 iabHelper = null;
	}
	
	
	private void promptToConnectWithInternet() { 
		AlertDialog dialog = new AlertDialog.Builder(getActivity())
					.setMessage(R.string.no_network_dialog_message)
					.setCancelable(false)
					.setPositiveButton(R.string.connect_now, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
						
							// Connect to Internet 
							WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE); 
							wifiManager.setWifiEnabled(true); 
							
							int i = 0; 
							while(!NetworkUtilities.haveNetworkConnection(getActivity()) && i<10 ) 
							{
								try { 
									Thread.sleep(1000);
								} catch(InterruptedException e) { 
									e.printStackTrace(); 
								}
							}
							
							// If successfully connected with WIFI or mobile network 
							boolean connected = NetworkUtilities.haveNetworkConnection(getActivity());
							if(connected) { 
								loadPurchaseSystem(); 
							}
						}
					})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							getActivity().finish();
						}
					})
					.create(); 
		
		dialog.show(); 
	}
	
	private class PurchaseItem  {
		
		private String SKU; 
		private String name; 
		private String price; 
		private String description; 
		
		public PurchaseItem(String SKU, String name) { 
			this.SKU = SKU; 
			this.name = name; 
		}
		
		public PurchaseItem(String SKU, String name, String price, String description) { 
			
			this(SKU, name); 
			
			this.price = price; 
			this.description = description; 
		}
		
		public void setName(String name) { 
			this.name = name; 
		}
		
		public void setPrice(String price) {
			this.price = price;
		}
		
		public void setDescription(String desc) { 
			this.description = desc; 
		}
		
		public String getSKU() { 
			return this.SKU; 
		}
		
		public String getName() { 
			return this.name; 
		}
		
		public String getDescription() { 
			return this.description; 
		}
		
		public String getPrice() { 
			return this.price; 
		}
		
		public String toString() { 
			return this.name; 
		}
	}
	
	private class PurchaseItemAdapter extends ArrayAdapter<PurchaseItem> { 
		
		private final ActionBarActivity context; 
		private final List<PurchaseItem> itemsList; 
		
		public PurchaseItemAdapter(Context context, List<PurchaseItem> purchaseItemsList) { 
			// call through to ArrayAdapter implementation 
			super(context,  R.layout.purchase_item_row, R.id.purchase_item_name , purchaseItemsList);
			
			this.context = (ActionBarActivity) context;
			this.itemsList = purchaseItemsList; 
		}
		
		@Override public View getDropDownView(int position, View convertView, ViewGroup parent) { 
			return getView(position, convertView, parent); 
		}
		
	   class ViewHolder { 
			protected TextView item_name; 
			protected TextView item_desc; 
			protected TextView item_price; 
			protected ImageView item_icon; 
		}
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
			
			View view = null; 
			// inflate a new row if one isn't recycled
			if(convertView == null) { 
				
				LayoutInflater inflator = context.getLayoutInflater(); 
				view = inflator.inflate(R.layout.purchase_item_row, null); 
				final ViewHolder viewHolder = new ViewHolder(); 
				viewHolder.item_name = (TextView) view.findViewById(R.id.purchase_item_name);
				viewHolder.item_desc = (TextView) view.findViewById(R.id.purchase_item_desc); 
				viewHolder.item_price = (TextView) view.findViewById(R.id.purchase_item_price); 
				viewHolder.item_icon = (ImageView) view.findViewById(R.id.purchase_item_icon); 
				view.setTag(viewHolder); 
			} else { 
				view = convertView; 
			}
			
			PurchaseItem item = getItem(position); 
			
			ViewHolder holder = (ViewHolder) view.getTag(); 
			holder.item_name.setText(item.getName());
			holder.item_desc.setText(item.getDescription()); 
			holder.item_price.setText(item.getPrice()); 
			holder.item_icon.setBackgroundResource(R.drawable.dollar); 
						
			return view;
		}
	}
}
