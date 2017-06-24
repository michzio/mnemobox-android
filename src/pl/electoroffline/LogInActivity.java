/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 
 * last modification date: 26.10.2014
 */
package pl.electoroffline;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Log in screen with 2 x EditText to log in user 
 * (email & password input fields)
 * and 2 x button's handlers (Log In & Sign Up) 
 * @author Michał Ziobro 
 */
public class LogInActivity extends Activity implements User.LogInCallbacks {
   
	private User user; 
    private String email; 
    private String pass; 
    
    private EditText emailInput; 
    private EditText passInput; 
 
    
     /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        
        Button loginBtn = (Button)findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {  
            	// after login button has been clicked execute login operation
                executeLoginOperation();
            }
        });
        Button signupBtn = (Button)findViewById(R.id.signupBtn);
        signupBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {
            	// after sign up button has been clicked move to SignUp screen 
                executeSignupOperation();
            }
        });
        Button useAnonymouslyBtn = (Button)findViewById(R.id.useAnonymouslyBtn); 
        useAnonymouslyBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// after use anonymously button has been clicked move to Main screen
				Intent mainIntent = new Intent(LogInActivity.this, MainActivity.class);
				startActivity(mainIntent); 
				
			} 
        	
        	
        });
        
        emailInput = (EditText)findViewById(R.id.email_address);
        passInput = (EditText)findViewById(R.id.password_input);
    }
    
    /* 
     * DEPRECATED: method that enforce user to log in, blocking back button press!
     * @Override
     * public void onBackPressed() {
     *     Toast.makeText( LogInActivity.this, 
     *                       getResources().getString(R.string.login_toast),
     *                           Toast.LENGTH_SHORT).show();
     *	}
     */
    
    /**
     * Helper method that performs log in operation. 
     * Takes 2 inputs with email and pass from user. 
     * Next using User object verify email and pass 
     * using online web service. 
     */
    public void executeLoginOperation() { 
       
    	// read user input 
       
        email = emailInput.getText().toString();
        pass = passInput.getText().toString();
        
        if(user == null) {
           // Creating new User object and passing user data.
           user = new User(this, email, pass);
        }
        
        // Verifying log in data using existing User object
        user.setLogInCallbacks(this);
       
        // verify user executes expensive tasks on background thread 
        // so we need to disable edit text fields
        emailInput.setEnabled(false); 
        passInput.setEnabled(false); 
        
        user.verifyUser(email, pass);
    }
    
    @Override 
    public void onLogInFinished(boolean isLoggedIn) { 
    	
        if(isLoggedIn) { 
    	   // after user is logged in initiate words personalizations  
    	   new Personalization(this).synchronize();
    	   
    	   // and move to Main application screen
           Intent mainIntent = new Intent(LogInActivity.this, MainActivity.class);
           startActivity(mainIntent);
        } else { 
    	   Toast.makeText(LogInActivity.this, R.string.email_or_pass_incorrect, Toast.LENGTH_LONG).show();
    	   emailInput.setEnabled(true); 
           passInput.setEnabled(true);
        }
       
    }
    
    @Override 
    public void onLogInError(int errorCode) {
    	//this method is called when error occurs while logging in 
    	emailInput.setEnabled(true); 
        passInput.setEnabled(true);
    }
    
    public void executeSignupOperation() { 
         //moving to sign up activity using explicit Intent 
         Intent signupIntent = new Intent(LogInActivity.this, SignUpActivity.class);
         startActivity(signupIntent);
    }
}
