/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.OpenRequest;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Sign up screen that enables to create a new profile account.
 * @author Michał Ziobro
 */
public class SignUpActivity extends Activity {
	
	private static final String KEY_USE_FACEBOOK_DATA = "KEY_USE_FACEBOOK_DATA"; 
	private static final String KEY_FIRST_NAME = "KEY_FIRST_NAME"; 
	private static final String KEY_LAST_NAME = "KEY_LAST_NAME"; 
	private static final String KEY_EMAIL_ADDRESS = "KEY_EMAIL_ADDRESS"; 
    
	private boolean useFacebookData = false; 
	private String firstName;
    private String lastName;
    private String email; 
    private String pass; 
    private String pass2;
    
    // Facebook Graph API permissions (to get email details) 
    private static final List<String> PERMISSIONS = new ArrayList<String>(); 
    static {
    	PERMISSIONS.add("email");
    }
    
    private static final int REAUTH_ACTIVITY_CODE = 100;
    private String fbAccessToken;
    private UiLifecycleHelper uiHelper;
    
    private Session.StatusCallback statusCallback = new Session.StatusCallback() {
        @Override
        public void call(final Session session, final SessionState state, final Exception exception) {
        	Log.d(SignUpActivity.class.getName(), "In Session.StatusCallback..."); 
            onSessionStateChange(session, state, exception);
        }
    };
    
    private void onSessionStateChange(final Session session, SessionState state, Exception exception) {
    	Log.d(SignUpActivity.class.getName(), "In onSessionStateChange..." + session); 
        if (session != null && session.isOpened()) {
        	fbAccessToken = session.getAccessToken();
            // Get the user's data.
            makeFacebookRequest(session);
        }
    }
    
    static void showHashKey(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getString(R.string.app_package), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.i("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
                }
        } catch (NameNotFoundException e) {
        } catch (NoSuchAlgorithmException e) {
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
        
       /** if (requestCode == REAUTH_ACTIVITY_CODE) {
        	Log.d(SignUpActivity.class.getName(), "In onActivityResult..."); 
        	uiHelper.onActivityResult(requestCode, resultCode, data);
        } **/
    }
    
    private void onClickLogin() {
        Session session = Session.getActiveSession();
        if (session != null) {
          if (!session.isOpened() && !session.isClosed()) {
            session.openForRead(new Session.OpenRequest(this).setPermissions(Arrays.asList("public_profile", "email", "user_birthday", "user_hometown")).setCallback(statusCallback));
          } else {
            Session.openActiveSession(this, true, statusCallback);
          }
        }
     }
    
    /**
     * Logout From Facebook 
     */
    public static void callFacebookLogout(Context context) {
        Session session = Session.getActiveSession();
        if (session != null) {

            if (!session.isClosed()) {
                session.closeAndClearTokenInformation();
                //clear your preferences if saved
            }
        } else {

            session = new Session(context);
            Session.setActiveSession(session);

            session.closeAndClearTokenInformation();
                //clear your preferences if saved

        }

    }
    
    
     /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);
        
        showHashKey(getApplicationContext());
    
        uiHelper = new UiLifecycleHelper(this, null); //statusCallback
        uiHelper.onCreate(savedInstanceState);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        		prefillSignUpFormWithPhoneOwnerProfile(); 
        }
              
        Button signupBtn = (Button)findViewById(R.id.signupBtn);
        signupBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            
                boolean signedup = executeSignupOperation();
                
                if(signedup) { 
                   
                    Toast.makeText( SignUpActivity.this, 
                            		R.string.signup_confirmation,
                            		Toast.LENGTH_SHORT).show();
                    Intent mainIntent = new Intent().setClass(SignUpActivity.this, LogInActivity.class);
                    startActivity(mainIntent);
                 }
            }
        });
        
        Button signupWithFacebookBtn = (Button) findViewById(R.id.signupWithFacebookBtn); 
        signupWithFacebookBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				Log.d(SignUpActivity.class.getName(), "Sign up with facebook cliked button!");
				onClickLogin();
			} 
        	
        });
   
    }
    
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH) 
    private void prefillSignUpFormWithPhoneOwnerProfile() {

    	Cursor c = getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
    	int count = c.getCount();
    	String[] columnNames = c.getColumnNames();
    	boolean b = c.moveToFirst();
    	int position = c.getPosition();
    	if (count == 1 && position == 0) {
    		
    		/**
    	    for (int j = 0; j < columnNames.length; j++) {
    	        String columnName = columnNames[j];
    	        String columnValue = c.getString(c.getColumnIndex(columnName));
    	        
    	        Log.d(SignUpActivity.class.getName(), "Reading phone owner profile: " + columnName + " -> " + columnValue);
    	    }
    	    **/
    		
    		String displayName = c.getString(c.getColumnIndex("display_name_alt")); 
    		String[] owner_names = displayName.split(", "); 
    		if(owner_names.length == 2) { 
    			((EditText) findViewById(R.id.first_name)).setText(owner_names[1]); 
    			((EditText) findViewById(R.id.last_name)).setText(owner_names[0]); 
    			
    		} else if(owner_names.length == 1) { 
    			((EditText) findViewById(R.id.first_name)).setText(owner_names[0]);
    		}
    	}
    	c.close();
		
      	((EditText) findViewById(R.id.email_address)).setText( UserEmailFetcher.getEmail(this)); 
	}

	private  void makeFacebookRequest(final Session session) {
    	
    	Log.d(SignUpActivity.class.getName(), "Making Facebook Request..."); 
    	Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
			
			@Override
			public void onCompleted(GraphUser user, Response response) {
				// If the response if successful
				if( session == Session.getActiveSession()) { 
					if(user != null) { 
						
						String fbId = user.getId();
						String name = user.getName(); 
						String firstName = user.getFirstName();
						String lastName = user.getLastName();
						String gender = ""; 
						if(user.asMap().get("gender") != null)
							gender = user.asMap().get("gender").toString();
						String email = ""; 
						if(user.asMap().get("email") != null)
							email = user.asMap().get("email").toString();
		                String birthday = user.getBirthday(); 
		                String hometown = "";
		                if(user.asMap().get("hometown") != null) 
		                	hometown = user.asMap().get("hometown").toString(); 
						
	                    
						Log.d(SignUpActivity.class.getName(),"Email: " + email 
															+ ", Name: "+ name 
															+ ", First Name: " + firstName 
															+ ", Last name: " + lastName
															+ ", Gender: " + gender 
															+ ", Birthday: " + birthday 
															+ ", Hometown: " + hometown); 
						
						callFacebookLogout(SignUpActivity.this); 
						
						// pre fill sign up form
						SignUpActivity.this.firstName = firstName; 
						SignUpActivity.this.lastName = lastName; 
						SignUpActivity.this.email = email;
						
						SignUpActivity.this.useFacebookData = true; 
						
						updateViewForSignUpWithFacebook();
					}
				}
				if (response.getError() != null) {
		                // Handle errors, will do so later.
		        }
				
			}
		});
    	request.executeAsync();
    }
    
    public void updateViewForSignUpWithFacebook()  { 
    	
    	Log.d(SignUpActivity.class.getName(), "Updating view for sign up with facebook!"); 
    	
    	findViewById(R.id.first_name).setVisibility(View.GONE); 
    	findViewById(R.id.last_name).setVisibility(View.GONE); 
    	if(email != null && email.length() > 0) { 
    		findViewById(R.id.sign_up_email_text).setVisibility(View.GONE); 
    		findViewById(R.id.email_address).setVisibility(View.GONE); 
    	}
    	findViewById(R.id.signupWithFacebookBtn).setVisibility(View.GONE); 
    	findViewById(R.id.sign_up_first_name_text).setVisibility(View.GONE); 
    	findViewById(R.id.sign_up_last_name_text).setVisibility(View.GONE); 
    	((TextView)findViewById(R.id.sign_up_header_text)).setText(R.string.sign_up_with_fb_header);
    	((TextView)findViewById(R.id.sign_up_password_text)).setText(R.string.new_password); 
    	((Button)findViewById(R.id.signupBtn)).setBackgroundResource(R.drawable.facebook_button);
    }
    
    
    public boolean executeSignupOperation() { 
        
    	if(!useFacebookData) { 
	        EditText firstNameInput = (EditText)findViewById(R.id.first_name);
	        firstName = firstNameInput.getText().toString();
	        
	        EditText lastNameInput = (EditText)findViewById(R.id.last_name);
	        lastName = lastNameInput.getText().toString();
	        
	        EditText emailInput = (EditText)findViewById(R.id.email_address);
	        email = emailInput.getText().toString();
    	}
       
        EditText passInput = (EditText)findViewById(R.id.password_input);
        pass = passInput.getText().toString();
        
        EditText passInput2 = (EditText)findViewById(R.id.password_input2);
        pass2 = passInput2.getText().toString();
        
        Pattern p = Pattern.compile("[\\p{L}'][ \\p{L}'-]*[\\p{L}]", Pattern.UNICODE_CASE);
        
        if(!pass.equals(pass2)) {
             Toast.makeText(SignUpActivity.this, R.string.passwords_do_not_match,
                            Toast.LENGTH_SHORT).show();
            return false; 
           
       } else if(!pass.matches("(\\S){4,20}") ) {
    	   Toast.makeText(SignUpActivity.this, R.string.password_not_correct,
                            Toast.LENGTH_SHORT).show(); 
    	   return false; 
       } else if(!email.matches(".+@.+\\.[a-z]+")) { 
           Toast.makeText(SignUpActivity.this, R.string.email_not_valid,
                            Toast.LENGTH_SHORT).show();
           return false; 
           
       } else if(!p.matcher(firstName).find()) { 
    	   Toast.makeText(SignUpActivity.this, R.string.name_fields_required,
                   Toast.LENGTH_SHORT).show();  
    	   return false;
       } else if(!p.matcher(lastName).find()) { 
            Toast.makeText(SignUpActivity.this, R.string.name_fields_required,
                            Toast.LENGTH_SHORT).show();   
            return false; 
       } else { 
          //signup user 
          return sendSignUpDataToServer(); 
            
       }
       
      
    }
    
    private boolean sendSignUpDataToServer() { 
    	
        String url = getString(R.string.register_user_url, email, pass, firstName, lastName);
        Log.d(SignUpActivity.class.getName(), "Sign up url: " + url); 
        
        boolean signedup = false; 
        try { 
        	String response = CustomHttpClient.executeHttpGet(url.replace(" ", "%20"));
        	Log.d(SignUpActivity.class.getName(), "GET response: " + response); 
        	Integer resCode = Integer.valueOf(response.trim()); 
        	if(resCode > 0) { 
        		signedup = true; 
        	} else { 
        		Toast.makeText(this, R.string.error_while_saving_account,
                        Toast.LENGTH_SHORT).show();
        		signedup = false; 
        	}
         } catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.error_while_saving_account,
                                Toast.LENGTH_SHORT).show();
                signedup = false;
         }
        
        return signedup;
       
   }
    
    @Override
    public void onResume() {
        super.onResume();
     // Main activity is launched and user session is not null,
     // but the session state change notification not be triggered.

     Session session = Session.getActiveSession();
     if (session != null && (session.isOpened() || session.isClosed()) ) {
             onSessionStateChange(session, session.getState(), null);
     }
     uiHelper.onResume();
    }

    @Override
    public void onPause() {
            super.onPause();
          uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is 
        // killed and restarted. 
        outState.putBoolean(KEY_USE_FACEBOOK_DATA, useFacebookData); 
        outState.putString(KEY_FIRST_NAME, firstName); 
        outState.putString(KEY_LAST_NAME, lastName); 
        outState.putString(KEY_EMAIL_ADDRESS, email); 
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {	
    	super.onRestoreInstanceState(savedInstanceState);
    	// Restore UI state from the savedInstanceState. 
    	// This bundle has also been passed to onCreate.
    	useFacebookData = savedInstanceState.getBoolean(KEY_USE_FACEBOOK_DATA, false); 
    	
    	firstName = (String) savedInstanceState.getString(KEY_FIRST_NAME);
    	if(firstName == null) firstName = "";
    	lastName = (String) savedInstanceState.getString(KEY_LAST_NAME);
    	if(lastName == null) lastName = "";
    	email = (String) savedInstanceState.getString(KEY_EMAIL_ADDRESS); 
    	if(email == null) email = ""; 
    	
    	if(useFacebookData) { 
    		updateViewForSignUpWithFacebook();
    	}
    }
   
}
