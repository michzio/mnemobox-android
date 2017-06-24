package pl.elector.service;

import java.io.InputStream;

import pl.elector.database.PostItProvider;
import pl.electoroffline.CustomHttpClient;
import pl.electoroffline.GetWordPostItsFromXML;
import pl.electoroffline.Preferences;
import pl.electoroffline.R;
import pl.electoroffline.SettingsFragment;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class PostItsLoader extends AsyncTask<Integer, Integer, Boolean> {
	
	private Context context; 
	private int wordId; 
	
	public PostItsLoader(Context context) {
		this.context = context; 
	}

	/**
	 * Loading post its for given word id from online web service
	 */
	@Override
	protected Boolean doInBackground(Integer... params) {
		
		if(params.length != 1) return false; 
		
		String nativeCode = Preferences.getAccountPreferences(context)
				.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, context.getString(R.string.native_code_lower));
		String foreignCode = Preferences.getAccountPreferences(context)
				.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, context.getString(R.string.foreign_code_lower));

		wordId = params[0]; 
		String email = Preferences.getString(context, Preferences.KEY_EMAIL, null);
		String pass = Preferences.getString(context, Preferences.KEY_SHA1_PASSWORD, null);
		
		String url = context.getString(R.string.word_postits_url, nativeCode, foreignCode, wordId, email, pass);
		Log.d(PostItsLoader.class.getName(), "Word post_its url: " + url); 
		
		GetWordPostItsFromXML postIts = null;
		
		try  { 
	        InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
	        postIts = new GetWordPostItsFromXML(is); 
	        try { 
	          is.close();
	        } catch(java.io.IOException e) { return false; } 
	    } catch(Exception e) { return false; }
		 	
	    return insertPostItsToDB(postIts);
		
	}
	
	/**
	 * Helper method that inserts or updates post it objects loaded from online web service to database.
	 * @param postIts - object with data loaded from web service.
	 * @return boolean value where true indicates correct post it insertion to database 
	 * 		   and false indicates that some error has happened.
	 */
	private Boolean insertPostItsToDB(GetWordPostItsFromXML postIts) {
		
		Boolean result = true; 
		
		for(Integer pid : postIts.postText().keySet()) {
			
			Log.w(PostItsLoader.class.getName(), "Inserting post it with id: " + pid); 
			
			if(checkPostItInDB(pid)) { 
				result = updatePostIt(pid, postIts.authorId().get(pid), postIts.authorFirstName().get(pid), 
							 postIts.authorLastName().get(pid), wordId, postIts.postText().get(pid),
							 postIts.from().get(pid) + "-" + postIts.to().get(pid));
			} else { 
				result = insertPostIt(pid, postIts.authorId().get(pid), postIts.authorFirstName().get(pid), 
						 postIts.authorLastName().get(pid), wordId, postIts.postText().get(pid),
						 postIts.from().get(pid) + "-" + postIts.to().get(pid));
			}
		}
		
		return result; 
	}

	/**
	 * Helper method that inserts new post it into local database
	 * @param pid - post it identifier
	 * @param authorId
	 * @param authorFirstName
	 * @param authorLastName
	 * @param wordId
	 * @param postText
	 * @param langDirection
	 * @return true/false indicating correctness of insertion to database
	 */
	private Boolean insertPostIt(Integer pid, Integer authorId, String authorFirstName,
			String authorLastName, int wordId, String postText, String langDirection) {
		
		ContentValues values = new ContentValues(); 
		values.put(PostItProvider.PostItTable.COLUMN_POST_IT_ID, pid);
		values.put(PostItProvider.PostItTable.COLUMN_AUTHOR_ID, authorId);
		values.put(PostItProvider.PostItTable.COLUMN_AUTHOR_FIRST_NAME, authorFirstName);
		values.put(PostItProvider.PostItTable.COLUMN_AUTHOR_LAST_NAME, authorLastName);
		values.put(PostItProvider.PostItTable.COLUMN_WORD_ID, wordId);
		values.put(PostItProvider.PostItTable.COLUMN_TEXT, postText);
		values.put(PostItProvider.PostItTable.COLUMN_LANG, langDirection);
		
		Uri insertedItemUri = context.getContentResolver().insert(PostItProvider.CONTENT_URI, values);
		
		if(insertedItemUri != null)
			return true;
		
		return false; 
	}

	/**
	 * Helper method updates existing post it in local database based on passed post it id (pid)
	 * @param pid
	 * @param authorId
	 * @param authorFirstName
	 * @param authorLastName
	 * @param wordId
	 * @param postText
	 * @param langDirection
	 * @return true/false indicating correctness of update in database
	 */
	private Boolean updatePostIt(Integer pid, Integer authorId, String authorFirstName,
			String authorLastName, int wordId, String postText, String langDirection) {
		
		Uri POST_IT_URI = Uri.parse(PostItProvider.CONTENT_URI + "/" + pid);
		
		ContentValues values = new ContentValues(); 
		values.put(PostItProvider.PostItTable.COLUMN_AUTHOR_ID, authorId);
		values.put(PostItProvider.PostItTable.COLUMN_AUTHOR_FIRST_NAME, authorFirstName);
		values.put(PostItProvider.PostItTable.COLUMN_AUTHOR_LAST_NAME, authorLastName);
		values.put(PostItProvider.PostItTable.COLUMN_WORD_ID, wordId);
		values.put(PostItProvider.PostItTable.COLUMN_TEXT, postText);
		values.put(PostItProvider.PostItTable.COLUMN_LANG, langDirection);
		
		int updateCount = context.getContentResolver().update(POST_IT_URI, values, null, null);
		if(updateCount == 1)
			return true; 
		
		return false;
	}

	/**
	 * Helper method that checks whether in local database exists post it item with passed in post it id (pid).
	 * @param pid
	 * @return true/false indicating whether post it with passed in pid exists in local database
	 */
	private boolean checkPostItInDB(Integer pid) {
		
		Uri POST_IT_URI = Uri.parse(PostItProvider.CONTENT_URI + "/" + pid);
		String[] projection = {
								PostItProvider.PostItTable.COLUMN_POST_IT_ID
							  };
		
		Cursor cursor = context.getContentResolver().query(POST_IT_URI, projection, null, null, null);
		
		if(cursor.getCount() == 1) {
			cursor.close(); 
			return true;
		}
		
		cursor.close();
		return false;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		
		if(result) { 
			Toast.makeText(context, R.string.posts_synced_toast, Toast.LENGTH_LONG).show();
		} else { 
			Toast.makeText(context, R.string.error_while_syncing_posts, Toast.LENGTH_LONG).show();
		}
	}

}
