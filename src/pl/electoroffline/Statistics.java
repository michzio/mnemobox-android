/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
/**
 *
 * @author Michał Ziobro
 */
public class Statistics {
    
   // private static String traceHistoryURL = "http://www.mnemobox.com/webservices/traceHistory.php?";
   // private static String forgottenURL = "http://www.mnemobox.com/webservices/forgottenWords.php?";
    
    public static void traceHistory(int wordsetid, int goodAns, int badAns, String mode, Context context) { 
    	
    	String nativeCode = Preferences.getAccountPreferences(context)
    			.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower));
    	String foreignCode = Preferences.getAccountPreferences(context)
    			.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower));
        String email = Preferences.getString(context, Preferences.KEY_EMAIL, "");
        String pass = Preferences.getString(context, Preferences.KEY_SHA1_PASSWORD, "");
        
        String traceHistoryURL = context.getString(R.string.tracehistory_url, 
        		nativeCode, foreignCode, email, pass, goodAns, badAns, mode, wordsetid);
        Log.d(Statistics.class.getName(), "Trace history url: " + traceHistoryURL); 
        
        try { 
            /*String response = */ CustomHttpClient.executeHttpGet(traceHistoryURL);
         } catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.error_savinghistory,
                                Toast.LENGTH_SHORT).show();
         }
        /* Toast.makeText(context, url,
                                Toast.LENGTH_SHORT).show();*/
    }
    
    public static void rememberMe(int wid, Context context) { 
    	
    	String nativeCode = Preferences.getAccountPreferences(context)
    			.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower));
    	String foreignCode = Preferences.getAccountPreferences(context)
    			.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower));
        String email = Preferences.getString(context, Preferences.KEY_EMAIL, ""); 
        String pass = Preferences.getString(context, Preferences.KEY_SHA1_PASSWORD, "");
        
        String remembermeURL = context.getString(R.string.saverememberme_url, nativeCode, foreignCode, email, pass, wid);
        Log.d(Statistics.class.getName(), "Save rememberme url: " + remembermeURL); 
        
        try { 
        	/*String response = */ CustomHttpClient.executeHttpGet(remembermeURL);
         } catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.error_savingrememberme,
                                Toast.LENGTH_SHORT).show();
         }
        /* Toast.makeText(context, url,
                                Toast.LENGTH_SHORT).show();*/
    }
    
    public static void storeForgotten(String serialized, Context context) { 
    	
    	String nativeCode = Preferences.getAccountPreferences(context)
    			.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower));
    	String foreignCode = Preferences.getAccountPreferences(context)
    			.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower)); 
        String email = Preferences.getString(context, Preferences.KEY_EMAIL, "");
        String pass = Preferences.getString(context, Preferences.KEY_SHA1_PASSWORD, "");
        
        String forgottenURL = context.getString(R.string.saveforgotten_url, nativeCode, foreignCode, email, pass, serialized);
        Log.d(Statistics.class.getName(), "Save forgotten url: " + forgottenURL); 	
        
        try { 
        	/*String response = */ CustomHttpClient.executeHttpGet(forgottenURL);
         } catch(Exception e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.error_savingforgotten,
                                Toast.LENGTH_SHORT).show();
         }
       /* Toast.makeText(context, url,
                                Toast.LENGTH_SHORT).show();*/
    }
  
    
    public static void shareWithFriends(int wid, String plword, String enword, Context ctx) { 
       String url = ctx.getString(R.string.dict_url) + enword;
       String title = enword + " - " + plword; 
       String intro = ctx.getString(R.string.share_header);
       
       Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.share_entitle) + title );
        share.putExtra(Intent.EXTRA_TEXT, intro + "... \n" + url);
        ctx.startActivity(Intent.createChooser(share, ctx.getString(R.string.share_promo)));
    }
}
