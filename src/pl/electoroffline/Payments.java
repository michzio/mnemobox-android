/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.util.LinkedHashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
/**
 *
 * @author bochzio
 */
public class Payments {
    
    // private static String fullAccessURL = "http://www.mnemobox.com/webservices/fullAccess.php?from=en&to=pl&";
    // private static String addMnemonsURL = "http://www.mnemobox.com/webservices/addMnemons.php?";
    // private static String unlockURL = "http://www.mnemobox.com/webservices/tryUnlock.php?from=en&to=pl&";
    
    private static LinkedHashMap<String, Integer> wordsetCost
            = new LinkedHashMap<String, Integer>() {}; 
   
   /* It takes mnemons amount in kilo! for example: 100 000 = 100 kilo, i.e. mnemonsAmount = 100 */
    public static void addMnemons(int mnemonsAmount, Context context) { 
    	
        String email =  Preferences.getString(context, Preferences.KEY_EMAIL, ""); 
        String pass = Preferences.getString(context, Preferences.KEY_SHA1_PASSWORD, "");
        
        String addMnemonsURL = context.getString(R.string.addmnemones_url, email, pass, mnemonsAmount);
        Log.d(Payments.class.getName(), "Add mnemons url: " + addMnemonsURL); 
    
        try { 
            /*String response =*/ CustomHttpClient.executeHttpGet(addMnemonsURL);
         } catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.payments_error_mnemoney,
                                Toast.LENGTH_SHORT).show();
         }
    }
    
    public static void activateFullAccess(Context context) { 
    	
    	String nativeCode = Preferences.getAccountPreferences(context)
    			.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower));
    	String foreignCode = Preferences.getAccountPreferences(context)
    			.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower));
        String email = Preferences.getString(context, Preferences.KEY_EMAIL, "");
        String pass = Preferences.getString(context, Preferences.KEY_SHA1_PASSWORD, "");
        
        String fullAccessURL = context.getString(R.string.fullaccess_url, nativeCode, foreignCode, email, pass);
        Log.d(Payments.class.getName(), "Full access url: "+ fullAccessURL); 
        
        try { 
        	/*String response = */ CustomHttpClient.executeHttpGet(fullAccessURL);
         } catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.payments_error_fullaccess,
                                Toast.LENGTH_SHORT).show();
         }
    }
    
   public static LinkedHashMap<String, Integer> getWordsetCosts() { 
       wordsetCost.put("A1", 1000); 
       wordsetCost.put("A2", 1500);
       wordsetCost.put("B1", 2000); 
       wordsetCost.put("B2", 2500); 
       wordsetCost.put("C1", 3000); 
       wordsetCost.put("C2", 3500); 
       return wordsetCost; 
   }
  
   public static boolean tryUnlockWordset(int cost, int wordsetId, String level, Context context) { 
	   
	    String nativeCode = Preferences.getAccountPreferences(context)
	    		.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower));
	    String foreignCode = Preferences.getAccountPreferences(context)
	    		.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower));
        String email = Preferences.getString(context, Preferences.KEY_EMAIL, "");
        String pass = Preferences.getString(context, Preferences.KEY_SHA1_PASSWORD, "");
        
        String unlockURL = context.getString(R.string.tryunlock_url, nativeCode, foreignCode, email, pass, level, wordsetId, cost);
        Log.d(Payments.class.getName(), "Try unlock url: " + unlockURL); 
                    
         try { 
            String response = CustomHttpClient.executeHttpGet(unlockURL);
            if(Integer.parseInt(
                response.substring(0,1)) == 1) return true;  
            Toast.makeText(context, response,
                                Toast.LENGTH_SHORT).show();
         } catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.payments_error_tryunlock ,
                                Toast.LENGTH_SHORT).show();
         }
       return false;
   }
}
