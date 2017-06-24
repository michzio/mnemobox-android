package pl.elector.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import pl.electoroffline.CustomHttpClient;
import pl.electoroffline.Preferences;
import pl.electoroffline.R;
import pl.electoroffline.SettingsFragment;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class PostItsSaver extends AsyncTask<PostItsSaver.PostItObject, Integer, Boolean> {
	
	public interface Callbacks { 
		public void onPostItSaved();
		public void onPostItSavingError(); 
		
	}
	
	public static class PostItObject { 
		private Integer wordId; 
		private String postText; 
		
		public PostItObject(Integer wordId, String postText) { 
			this.wordId = wordId;
			this.postText = postText; 
		}
		
		public Integer wordId() { 
			return wordId; 
		}
		
		public String postText() { 
			return postText; 
		}
	}
	
	private Context context; 
	
	public PostItsSaver(Context context) {
		this.context = context; 
	}

	/**
	 * Saving post it objects in online database for current user 
	 */
	@Override
	protected Boolean doInBackground(PostItObject... params) {
		
		if(params.length < 1) return false; 
		
		StringBuilder sb = new StringBuilder(); 
		
		for(PostItObject postIt : params) { 
			sb.append(postIt.wordId()); 
			sb.append(",");
			try {
				sb.append(URLEncoder.encode(postIt.postText(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return false; 
			} 
			sb.append(";");
		}
		
		
		String nativeCode = Preferences.getAccountPreferences(context)
				.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower));
		String foreignCode = Preferences.getAccountPreferences(context)
				.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower));
		String serialData = sb.toString(); 
		String email = Preferences.getString(context, Preferences.KEY_EMAIL, "");
		String pass = Preferences.getString(context, Preferences.KEY_SHA1_PASSWORD, "");
		
		if(serialData.equals("") || email.equals("")|| pass.equals("")) return false; 
		
		// concatenating web service URL
		String url = context.getString(R.string.save_postit_url, nativeCode, foreignCode, serialData, email, pass);
			   url = url.replaceAll(" ", "%20"); // replacing blank spaces
		Log.w(PostItsSaver.class.getName(), "Saving post it GET URL: " + url); 
		
		String result = "0";
		try {
			result = CustomHttpClient.executeHttpGet(url);
		} catch (Exception e) {
			e.printStackTrace();
			return false; 
		}
		Log.w(PostItsSaver.class.getName(), "Post it GET request result: " + result); 
		
		if(Integer.valueOf(result.replaceAll("\\s+","")) == 1) { 
			return true; 
		} else { 
			return false; 
		}
		
	}

	@Override
	protected void onPostExecute(Boolean result) {
		
		if(result) { 
			Toast.makeText(context, R.string.posts_saved_toast, Toast.LENGTH_LONG).show();
			((Callbacks) context).onPostItSaved(); 
		} else { 
			Toast.makeText(context, R.string.error_while_saving_posts, Toast.LENGTH_LONG).show();
			((Callbacks) context).onPostItSavingError();
		}
	}

}
