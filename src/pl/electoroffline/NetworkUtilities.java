/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 *
 * @author Michał Ziobro
 */
public class NetworkUtilities {
  
  public static boolean haveNetworkConnection(Context ctx) {
    boolean haveConnectedWifi = false;
    boolean haveConnectedMobile = false;

    ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo[] netInfo = cm.getAllNetworkInfo();
    for (NetworkInfo ni : netInfo) {
        if (ni.getTypeName().equalsIgnoreCase("WIFI"))
            if (ni.isConnected())
                haveConnectedWifi = true;
        if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
            if (ni.isConnected())
                haveConnectedMobile = true;
    }
    return haveConnectedWifi || haveConnectedMobile;
  }
  
  /**
   * This method is used to download file store at url 
   * and return it as byte[] array. Can be used to download 
   * images as byte[] array.
   * @param url - points to file (image) in internt resources.
   * @return
   */
  public static byte[] downloadFromURL(String stringURL) {
	  
	   final int INITIAL_BUFFER_SIZE = 50; 
	  
	    try { 
	    	 URL url = new URL(stringURL); 
	    	 URLConnection urlConnection = url.openConnection();
	    	 
	    	 InputStream is = urlConnection.getInputStream(); 
	    	 BufferedInputStream bis = new BufferedInputStream(is);
	    	
	    	 // ByteArrayBuffer has set only initial buffer size 
	    	 // that will be resized automatically each time it reaches its limit
	    	 ByteArrayBuffer baf  = new ByteArrayBuffer(INITIAL_BUFFER_SIZE);
	    	 
	    	 int currByte = bis.read(); 
	    	 while( currByte != -1 ) 
	    	 {
	    		 baf.append((byte) currByte); 
	    		 currByte = bis.read();
	    	 }
	    	
	    	 return baf.toByteArray(); 
	    	 
	    } catch(Exception e) {
	    	Log.d(NetworkUtilities.class.getName(), "Error while downloading file: " + stringURL); 
	    }
	  	
	  	return null;
	  
  }
}
