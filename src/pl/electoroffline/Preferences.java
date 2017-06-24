package pl.electoroffline;

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Preferences {
	
	public static final String KEY_PREFER_ONLINE_DATA = "KEY_PREFERE_ONLINE_DATA"; 
	public static final String KEY_PREFER_TO_DOWNLOAD_AUDIO = "KEY_PREFER_TO_DOWNLOAD_AUDIO";
	public static final String KEY_LAST_PERSONALIZATION_SYNCHRONIZATION = "KEY_LAST_PERSONALIZATION_SYNCHRONIZATION";
	public static final String KEY_PROFILE_ID = "KEY_PROFILE_ID";
	public static final String KEY_EMAIL = "KEY_EMAIL"; 
	public static final String KEY_SHA1_PASSWORD = "KEY_SHA1_PASSWORD";  
	public static final String KEY_TURN_OFF_ADS_EXPIRATION_DATE = "KEY_TURN_OFF_ADS_EXPIRATION_DATE"; 
	public static final String KEY_PAID_UP_ACCOUNT_EXPIRATION_DATE = "KEY_PAID_UP_ACCOUNT_EXPIRATION_DATE"; 
	
	private String filename; 
	private Context context;
	
	public Preferences(Context context, String filename) { 
		this.filename = filename; 
		this.context = context; 
	}
	
	/**
	 * Factory method that creates Preferences object for currently used user account (email specific)
	 */
	public static Preferences getAccountPreferences(Context context) {
		String emailAddress = Preferences.getString(context, Preferences.KEY_EMAIL, "anonymous");
		Preferences prefs = new Preferences(context, emailAddress); 
		return prefs; 
	}
	
	public void putString(String key, String value) 
	{
		SharedPreferences sharedPreferences = context.getSharedPreferences(filename, Context.MODE_PRIVATE); 
		SharedPreferences.Editor editor = sharedPreferences.edit(); 
		editor.putString(key, value); 
		editor.commit(); 
	}

	public static void putString(Context context, String key, String value) 
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	    SharedPreferences.Editor editor = sharedPreferences.edit();
	    editor.putString(key, value);
	    editor.commit();
	}
	
	public String getString(String key, String defaultValue) 
	{
		SharedPreferences sharedPreferences =  context.getSharedPreferences(filename, Context.MODE_PRIVATE); 
		String value = sharedPreferences.getString(key, defaultValue); 
		return value; 
	}
	
	public static String getString(Context context, String key, String defaultValue)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	    String value = sharedPreferences.getString(key, defaultValue);
	    return value;
	}
	
	public void putBoolean(String key, boolean value) 
	{
		SharedPreferences sharedPreferences = context.getSharedPreferences(filename, Context.MODE_PRIVATE); 
		SharedPreferences.Editor editor = sharedPreferences.edit(); 
		editor.putBoolean(key, value);
		editor.commit();
	}
	
	public static void putBoolean(Context context, String key, boolean value)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context); 
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}
	
	public boolean getBoolean(String key, boolean defaultValue)
	{
		SharedPreferences sharedPreferences = context.getSharedPreferences(filename,  Context.MODE_PRIVATE); 
		boolean value = sharedPreferences.getBoolean(key, defaultValue); 
		return value; 
	}
	
	public static boolean getBoolean(Context context, String key, boolean defaultValue)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		boolean value = sharedPreferences.getBoolean(key, defaultValue);
		return value;
	}
	
	public void putLong(String key, long value)
	{
		SharedPreferences sharedPreferences = context.getSharedPreferences(filename, Context.MODE_PRIVATE); 
		SharedPreferences.Editor editor = sharedPreferences.edit(); 
		editor.putLong(key, value);
		editor.commit();
	}
	
	// long can be used to store Data & Time using timestamp value
	public static void putLong(Context context, String key, long value)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putLong(key, value);
		editor.commit();
	}
	
	public long getLong(String key, long defaultValue) 
	{
		SharedPreferences sharedPreferences = context.getSharedPreferences(filename, Context.MODE_PRIVATE); 
		long value = sharedPreferences.getLong(key, defaultValue); 
		return value; 
	}
	
	public static long getLong(Context context, String key, long defaultValue)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		long value = sharedPreferences.getLong(key, defaultValue);
		return value; 
	}
	
	public void putInt(String key, int value)
	{
		SharedPreferences sharedPreferences = context.getSharedPreferences(filename, Context.MODE_PRIVATE); 
		SharedPreferences.Editor editor = sharedPreferences.edit(); 
		editor.putInt(key, value); 
		editor.commit(); 
	}
	
	public static void putInt(Context context, String key, int value)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPreferences.edit(); 
		editor.putInt(key, value);
		editor.commit(); 
	}
	
	public int getInt(String key, int defaultValue)
	{
		SharedPreferences sharedPreferences = context.getSharedPreferences(filename, Context.MODE_PRIVATE); 
		int value = sharedPreferences.getInt(key, defaultValue); 
		return value; 
	}
	
	public static int getInt(Context context, String key, int defaultValue)
	{
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		int value = sharedPreferences.getInt(key, defaultValue);
		return value;
	}
	
	public void putDate(String key, Date value) 
	{
		SharedPreferences sharedPreferences = context.getSharedPreferences(filename, Context.MODE_PRIVATE); 
		SharedPreferences.Editor editor = sharedPreferences.edit();
		Log.d("PREFERENCES", "Putting date: " + String.valueOf(value.getTime()));
		editor.putLong(key, value.getTime()); 
		editor.commit(); 
	}
	
	public static void putDate(Context context, String key, Date value) { 
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		Log.d("PREFERENCES", "Putting date: " + String.valueOf(value.getTime())); 
		editor.putLong(key, value.getTime());
		editor.commit(); 
	}
	
	public Date getDate(String key, Date defaultDate)
	{
		SharedPreferences sharedPreferences = context.getSharedPreferences(filename, Context.MODE_PRIVATE); 
		long miliseconds = sharedPreferences.getLong(key, defaultDate.getTime());
		Log.d("PREFERENCES", "Getting date: " + String.valueOf(miliseconds)); 
		return new Date(miliseconds); 
	}
	
	public static Date getDate(Context context, String key, Date defaultDate) { 
	    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	    long miliseconds = sharedPreferences.getLong(key, defaultDate.getTime()); 
	    Log.d("PREFERENCES", "Getting date: " + String.valueOf(miliseconds)); 
	    return new Date(miliseconds); 
	}
}
