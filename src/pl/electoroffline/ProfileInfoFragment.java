package pl.electoroffline;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class ProfileInfoFragment extends Fragment {

	public static final String TAG = "PROFILE_INFO_FRAGMENT_TAG";
	
	private View view; 
	private AlertDialog dialog; 
	
	 @Override
     public void onCreate(Bundle savedInstanceState)
     {
         super.onCreate(savedInstanceState);
     }
	
	@Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		view = inflater.inflate(R.layout.profileinfo, container, false);
		int profileId = Preferences.getInt(getActivity(), Preferences.KEY_PROFILE_ID, 0);
        if(profileId > 0) { 
        	if(NetworkUtilities.haveNetworkConnection(getActivity())) { 
        		loadProfileInfoFromServerSideAsync(onProfileInfoLoadedListener);
        	} else { 
        		promptToConnectWithInternet();
        	}
        } else { 
        	new User(getActivity()).showPromptToLogIn(); 
        }
		return view;
		
	}
	
	/**
	 * Async function that makes request to server about profile info 
	 * and then returns result using Handler.post() to callback function. 
	 * @param callback
	 */
	private void loadProfileInfoFromServerSideAsync(
			final ProfileInfoFragment.OnProfileInfoLoadedListener callback) { 
		
			final Handler handler = new Handler(); 
			
			(new Thread(new Runnable() {
				@Override
				public void run() {
					// loading user information on background thread from server side 
					GetUserinfoFromXML userinfo = GetUserinfoFromXML.getMyProfileInfo(getActivity()); 
					
					final GetUserinfoFromXML userinfo_f = userinfo; 
					if(callback != null && isAdded()) { 
						
						handler.post(new Runnable() {
							@Override
							public void run() {
								callback.onProfileInfoLoadedListener(userinfo_f);
							}
						});
					}
				} 
			})).start(); 
			
		   	
		       
	}
	
	/**
	 * Callback that is notified about profile info has loaded from server side event 
	 */
	private interface OnProfileInfoLoadedListener { 
		public void onProfileInfoLoadedListener(GetUserinfoFromXML userInfo); 
	}
	
	/**
	 * Implementation of above callback interface for loading profile info from server side
	 */
	private ProfileInfoFragment.OnProfileInfoLoadedListener onProfileInfoLoadedListener 
							= new ProfileInfoFragment.OnProfileInfoLoadedListener() 
	{
								
		@Override
		public void onProfileInfoLoadedListener(GetUserinfoFromXML userinfo) {
			
			// callback function executed on UI main thread!
			if(userinfo != null) { 
			       TextView nametxt = (TextView) view.findViewById(R.id.nameText); 
			       nametxt.setText(userinfo.firstName + " " + userinfo.lastName); 
			       TextView emailtxt = (TextView) view.findViewById(R.id.emailText); 
			       emailtxt.setText(userinfo.email); 
			       ImageView image = (ImageView) view.findViewById(R.id.userPhoto); 
			       UrlImageViewHelper.setUrlDrawable(image, 
			                   getResources().getString(R.string.avatars_url) + userinfo.userImage);
			       TextView agetxt = (TextView) view.findViewById(R.id.ageText); 
			       agetxt.setText(userinfo.userAge);
			       if(!userinfo.city.equals("")) { 
			           TextView citytxt = (TextView) view.findViewById(R.id.cityText); 
			           citytxt.setText(userinfo.city);
			       }
			       if(!userinfo.gaduGadu.equals("")) { 
			           TextView ggtxt = (TextView) view.findViewById(R.id.ggText); 
			           ggtxt.setText(userinfo.gaduGadu);
			       }
			        if(!userinfo.skype.equals("")) { 
			           TextView skypetxt = (TextView) view.findViewById(R.id.skypeText); 
			           skypetxt.setText(userinfo.skype);
			       }
			       if(!userinfo.phone.equals("")) { 
			           TextView phonetxt = (TextView) view.findViewById(R.id.phoneText); 
			           phonetxt.setText(userinfo.phone);
			       }
			       TextView paiduptxt = (TextView) view.findViewById(R.id.paidupText);
			       if(userinfo.paidupAccount) { 
			           paiduptxt.setText(getResources().getString(R.string.profileinfo_fullaccess));
			           Button unlockFullAccessBtn = (Button) view.findViewById(R.id.unlockFullAccessBtn); 
			           unlockFullAccessBtn.setVisibility(View.GONE);
			       }
			       TextView leveltxt = (TextView) view.findViewById(R.id.userlevelText); 
			       leveltxt.setText(userinfo.userLevel);
			       TextView moneytxt = (TextView) view.findViewById(R.id.usermoneyText); 
			       moneytxt.setText(userinfo.userMoney);
			       
			       view.findViewById(R.id.profileinfoScrollView).setVisibility(View.VISIBLE);
			       
			 } else { 
			         Toast.makeText(getActivity(), R.string.internet_lost,
			                               Toast.LENGTH_SHORT).show();
			 }
									
		}
	};
	
	@Override
	public void onResume() 
	{
		super.onResume();
		onFragmentResume();	
	}
	
	private void onFragmentResume() {
		ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
		actionBar.setTitle(getString(R.string.profileinfo));
		actionBar.setSubtitle(null);
	}
	
 
   private void promptToConnectWithInternet() {
   	
   	 dialog = new AlertDialog.Builder(getActivity())
				.setMessage(R.string.no_network_dialog_message)
				.setCancelable(false)
				.setPositiveButton(R.string.connect_now,  new DialogInterface.OnClickListener() {
				
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						// Connect to Internet 
						WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
						wifiManager.setWifiEnabled(true);
						
						int i = 0; 
						while(!NetworkUtilities.haveNetworkConnection(getActivity()) && i < 10 )
						{
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						
						// If successfully connected with WIFI or mobile network 
						boolean connected = NetworkUtilities.haveNetworkConnection(getActivity()); 
						if(connected) {
							loadProfileInfoFromServerSideAsync(onProfileInfoLoadedListener); 
						} else { 
							dialog.dismiss();
							getActivity().finish();
						}
					}
				})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					getActivity().finish();
					
				}
			})
			.create();


   	  dialog.show();
   }
   
   @Override 
   public void onDestroy() {
	   if(dialog != null) dialog.dismiss();
	   super.onDestroy();
   }
      
}
