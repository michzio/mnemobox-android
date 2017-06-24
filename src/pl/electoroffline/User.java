/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import pl.elector.database.ProfileProvider;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;


/**
 * Type that represents User and 
 * implements helper methods to access
 * User account like: log in user, 
 * check user availablity, sign up user etc.
 * @author Michał Ziobro 
 */
public class User {
	
	private User.LogInCallbacks logInCallbacks; 
	
	private int profileId; 
    private String email; 
    private String passwordSHA1; 
    private boolean loggedIn = false; 
    private Context context; 
    // String verifyingUrl = "http://www.mnemobox.com/webservices/loginService.php?";
    //params are: email=xxx&pass=SHA1(xxx)
   
    // callback interface to inform about events of log in operation
    public interface LogInCallbacks {
    	public static final int ERROR_NO_INTERNET_CONNTECTION = 1; 
    	public void onLogInFinished(boolean isLoggedIn);
    	public void onLogInError(int errorCode); 
    }
    
    User(Context context) { 
        //it is needed by SharedPreferences
        this.context = context; 
        //create User object 
        //first check whether there are stored information to authorize user in elector.pl
        loggedIn = checkWhetherUserIsLoggedIn(); 
    }
    
    User(Context context, String email, String pass) { 
        this.context = context; 
        this.email = email; 
        try { 
            this.passwordSHA1 = User.SHA1(pass); 
        } catch(NoSuchAlgorithmException e) {
            //do something
        } catch(UnsupportedEncodingException e) { 
            //do something
        }
    }
    
    /** Method used to set callback object for log in events */
    public void setLogInCallbacks(User.LogInCallbacks callbacks) {
    	logInCallbacks = callbacks; 
    }
    
    public boolean isLoggedIn() { 
        return loggedIn; 
    }
    public void redirectToLogingPageActivity() { 
         
         Intent mainIntent = new Intent(context, LogInActivity.class);
         context.startActivity(mainIntent);
    }
    private boolean checkWhetherUserIsLoggedIn() { 
    	 profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
         email = Preferences.getString(context, Preferences.KEY_EMAIL, "");
         passwordSHA1 = Preferences.getString(context, Preferences.KEY_SHA1_PASSWORD, "");
         if(profileId > 0 && !email.equals("") && !passwordSHA1.equals("") ) {
             return true;
         }
         return false; 
    }
    
    /**
     * Method that verifies user account data: email address, sha1password 
     * using verification URL address via web service. Checks whether there 
     * is network connection available, if so then make GET request and 
     * parse return value to integer which is profile id or 0. 
     * Next if user has been logged in then we set up profile account in 
     * local database profile table (needs to download additional informations).
     * Next if there are anonymous user not synced personalizations ask client
     * to port this personalization to currently logged in user account and synced them.
     * After verification process end onLogInFinished() handler is called with passed in 
     * flag that can be set to true if user has been successfully logged in or false on failure.
     * @return
     */
    public boolean verifyUser() { 
        String response = "0"; 
        
        String verifyingUrl = context.getString(R.string.login_url, email, passwordSHA1);
        Log.d(User.class.getName(), "Login url: " + verifyingUrl);
       
        if(NetworkUtilities.haveNetworkConnection(context) ) { 
            try {
                response = CustomHttpClient.executeHttpGet(verifyingUrl);

            } catch(Exception e) {
                e.printStackTrace();
            }
            
            Log.w(User.class.getName(), "User verification response is:" + response.trim());
            profileId = Integer.parseInt(response.trim());

           /* Toast.makeText(context, response,
                                Toast.LENGTH_SHORT).show(); */
           if(profileId > 0) {
            	
	        	// 1.) save current logged in user data in SharedPreferences 
	           	replaceProfileInPreferences(context, profileId, email, passwordSHA1);
            	loggedIn = true;
            	
            	// 2.) insert to local database profile data and try to sync personalizations
            	new Thread(new Runnable() { 
                	public void run() { 
                		setUpProfileAccountLocally();
                	}
            	}).start();
            	
                return true; 
            } 
         } else { 
            Toast.makeText(context, R.string.internet_lost,
                                Toast.LENGTH_SHORT).show();
            if(logInCallbacks != null)
            	logInCallbacks.onLogInError(LogInCallbacks.ERROR_NO_INTERNET_CONNTECTION); 
         }
        
         if(logInCallbacks != null)
    		logInCallbacks.onLogInFinished(loggedIn);
         return false;
    }
    
    /**
     * Helper method used to set up profile account 
     * in local database and if client wants to port
     * anonymous user personalizations with new account
     */
    private void setUpProfileAccountLocally() 
    {   
        // 1.) insert new profile into database (used for further log in operation without Internet connection)
	    insertProfileToDB();
	    
	    // 2.) check if there are any existing words personalizations for Anonymous (profile_id = 0) user 
        Personalization p = new Personalization(context); 
        if(p.checkAnonymousPersonalizationsAvailable()) {
        	Log.w(User.class.getName(), "Anonymous personalizations are available..."); 
        	// 4.) ask user if he want to port this personalization to new account 
        	p.showPromptToPortAnonymousPersonalizations(logInCallbacks);
        } else  {
        	// else there are no personalizations for Anonymous user to be ported to new logged in Account
        	if(logInCallbacks != null)
        		((Activity) context).runOnUiThread(new Runnable() {

					@Override
					public void run() {
						logInCallbacks.onLogInFinished(loggedIn);
					} 
        		});
        }
        
    }
    
    public static void replaceProfileInPreferences(Context context, int profileId, String email, String passwordSHA1) {
    	Log.w(User.class.getName(), "Replacing profile account in shared preferences... " + profileId); 
    	
    	Preferences.putInt(context, Preferences.KEY_PROFILE_ID, profileId);
        Preferences.putString(context, Preferences.KEY_EMAIL, email);
        Preferences.putString(context, Preferences.KEY_SHA1_PASSWORD, passwordSHA1);
        Preferences.putDate(context, Preferences.KEY_TURN_OFF_ADS_EXPIRATION_DATE, new Date(0L)); 
        Preferences.putDate(context, Preferences.KEY_PAID_UP_ACCOUNT_EXPIRATION_DATE, new Date(0L));
    }
    
    /**
     * Helper method that inserts new logged in profile into database 
     */
    private void insertProfileToDB()
    {
    	Log.w(User.class.getName(), "Inserting new profile into local database.... " + profileId); 
    	
    	// loading additional information from online web service...
    	GetUserinfoFromXML userinfo = GetUserinfoFromXML.getMyProfileInfo(context);
    	
    	ContentValues values = new ContentValues(); 
    	values.put(ProfileProvider.ProfileTable.COLUMN_PROFILE_ID, profileId);
    	values.put(ProfileProvider.ProfileTable.COLUMN_EMAIL, email);
    	values.put(ProfileProvider.ProfileTable.COLUMN_SHA1PASS, passwordSHA1);
    	values.put(ProfileProvider.ProfileTable.COLUMN_LANG, "pl-en");  // this is fixed and has only application scope 
    	if(userinfo != null) {
    		 values.put(ProfileProvider.ProfileTable.COLUMN_FIRST_NAME, userinfo.firstName);
    		 values.put(ProfileProvider.ProfileTable.COLUMN_LAST_NAME, userinfo.lastName);
    		 values.put(ProfileProvider.ProfileTable.COLUMN_LAST_WORDSET_NAME, userinfo.lastWordset);
    		 values.put(ProfileProvider.ProfileTable.COLUMN_LAST_WORDSET_ID, Integer.valueOf(userinfo.lastWordsetId));
    		 values.put(ProfileProvider.ProfileTable.COLUMN_AGE, Integer.valueOf(userinfo.userAge));
    		 values.put(ProfileProvider.ProfileTable.COLUMN_CITY, userinfo.city);
    		 values.put(ProfileProvider.ProfileTable.COLUMN_PHONE, userinfo.phone);
    		 values.put(ProfileProvider.ProfileTable.COLUMN_SKYPE, userinfo.skype);
    		 values.put(ProfileProvider.ProfileTable.COLUMN_IMAGE_PATH, userinfo.userImage); 
    		 
    		 String urlPath = context.getResources().getString(R.string.avatars_url);
    		 values.put(ProfileProvider.ProfileTable.COLUMN_IMAGE, 
    				 				BitmapUtilities.getImageBlob(context, urlPath, userinfo.userImage));
    		 
    	}
    	
    	// new profile is inserted when it doesn't exists in local SQL database 
    	if(!checkProfileExists()) {
	    	Uri insertedItemUri = context.getContentResolver().insert(ProfileProvider.CONTENT_URI, values);
	    	Log.w(User.class.getName(), "Inserted new profile under: " + insertedItemUri);
	    	
    	} else { 
    		Log.w(User.class.getName(), "Profile already exists in database with id: " + profileId);
    		// we only need to update data such as email, pass or language to current values
    		Uri UPDATE_PROFILE_URI = Uri.parse(ProfileProvider.CONTENT_URI + "/" + profileId);
    		/* int updateCount = */ context.getContentResolver().update(UPDATE_PROFILE_URI, values, null, null);
    	}
    }
    
    /**
     * Helper method that checks whether profile row for current profile id already exists in local database 
     * ex. user is logging again on the same phone with this data and its account is already stored locally
     * @return
     */
    private boolean checkProfileExists() {
    	
    	Log.w(User.class.getName(), "Checking user profile exists in local DB, profile id: " + profileId); 
    	
    	Uri PROFILE_CONTENT_URI = Uri.parse(ProfileProvider.CONTENT_URI + "/" + profileId); 
    	String[] projection = { ProfileProvider.ProfileTable.COLUMN_PROFILE_ID };
    	Cursor cursor = context.getContentResolver().query(PROFILE_CONTENT_URI, projection, null, null, null);
    	
    	if(cursor.getCount() == 1) { 
    		cursor.close();
    		return true;
    	}
    	
    	cursor.close(); 
    	return false; 
    }
  
    public boolean verifyUser(String newEmail, String newPassword) { 
        email = newEmail; 
        try { 
            passwordSHA1 = User.SHA1(newPassword); 
        } catch(NoSuchAlgorithmException e) {
            //do something
        } catch(UnsupportedEncodingException e) { 
            //do something
        }
        return verifyUser();
    }
    
    public boolean logOut() { 
      
    	if(profileId > 0) { 
    		// delete current profile from local database profileTable
    		Uri DELETE_PROFILE_URI = Uri.parse(ProfileProvider.CONTENT_URI + "/" + profileId);
    		context.getContentResolver().delete(DELETE_PROFILE_URI, null, null);
    	}
    	
       Preferences.putInt(context, Preferences.KEY_PROFILE_ID, 0);
       Preferences.putString(context, Preferences.KEY_EMAIL, "");
       Preferences.putString(context, Preferences.KEY_SHA1_PASSWORD, "");
       loggedIn = false; 
       redirectToLogingPageActivity();
       return true;
    }
   
   public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException  { 
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    byte[] sha1hash = new byte[40];
    md.update(text.getBytes("utf-8"), 0, text.length());
    sha1hash = md.digest();
    return convertToHex(sha1hash);
  } 
   private static String convertToHex(byte[] data) { 
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < data.length; i++) { 
        int halfbyte = (data[i] >>> 4) & 0x0F;
        int two_halfs = 0;
        do { 
            if ((0 <= halfbyte) && (halfbyte <= 9)) 
                buf.append((char) ('0' + halfbyte));
            else 
                buf.append((char) ('a' + (halfbyte - 10)));
            halfbyte = data[i] & 0x0F;
        } while(two_halfs++ < 1);
    } 
    return buf.toString();
} 
   
   
   public void showPromptToLogIn() 
   {
		 AlertDialog dialog = new AlertDialog.Builder(context)
		.setMessage(R.string.sign_in_to_sync)
		.setCancelable(true)
		.setPositiveButton(R.string.log_in,  new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				Intent logInIntent = new Intent(context, LogInActivity.class);
				context.startActivity(logInIntent);
				
			}
		})
		.setNegativeButton(R.string.signup, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent signUpIntent = new Intent(context, SignUpActivity.class);
				context.startActivity(signUpIntent);
			}
		})
		.create();

		dialog.show();
   }
   
}
