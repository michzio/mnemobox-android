package pl.electoroffline;

import java.util.HashMap;

import pl.elector.database.PostItProvider;
import pl.elector.service.PostItsLoader;
import pl.elector.service.PostItsSaver;
import pl.elector.service.PostItsSaver.PostItObject;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class WordDetailsActivity extends DrawerActivity implements PostItsSaver.Callbacks  { 
	
	public static final String KEY_WORD_ID = "KEY_WORD_ID"; 
	public static final String KEY_WORD_OBJECT = "KEY_WORD_OBJECT";
	
	private WordDetailsFragment wordDetailsFragment; 
	
	@Override
	protected void onCreateDrawerActivity(Bundle savedInstanceState) {
		
		// adding initial fragment using Fragment Transaction
        FragmentManager fragmentManager =  getSupportFragmentManager(); 
        wordDetailsFragment = (WordDetailsFragment) fragmentManager.findFragmentByTag(WordDetailsFragment.TAG);
        
        // IMPORTANT TO RETAIN CURRENT FRAGMENT ON SCREEN WHILE ex. ROTATING DEVICE
        if(wordDetailsFragment == null) { 
	        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        wordDetailsFragment = new WordDetailsFragment();
	        fragmentTransaction.replace(R.id.main_content_frame, wordDetailsFragment, WordDetailsFragment.TAG); 
	        // fragmentTransaction.addToBackStack("wordDetailsFragmentBack"); 
	        fragmentTransaction.commit();
        }
		
	}
	
	
	@Override
	protected int getRightDrawerMenuButtonId() {
		
		return R.id.detailsPostItsBtn;
	}
	@Override
	protected void drawerMenuItemClicked(long id) {
		super.drawerMenuItemClicked(id);
	} 
	
	@Override 
	protected void configureRightDrawer() {
		
		super.configureRightDrawer();
		
		int wordId = getIntent().getExtras().getInt(WordDetailsActivity.KEY_WORD_ID, -1);
		// populating right drawer expandable list view with post its for current word.
		SimpleCursorTreeAdapter adapter = new PostItsCursorAdapter(this, wordId,
									R.layout.postit_row,  //groupLayout
									new String[] { PostItProvider.PostItTable.COLUMN_TEXT,
												   PostItProvider.PostItTable.COLUMN_AUTHOR_FIRST_NAME,
												   PostItProvider.PostItTable.COLUMN_AUTHOR_LAST_NAME }, 
									new int[] { R.id.postText, R.id.authorFristName, R.id.authorLastName }, 
									android.R.layout.simple_expandable_list_item_1, // childLayout
									new String[] {  },  // there will be no child items for post its
									new int[] { });     // there will be no child item for post its
		
		configureRightHeaderView();
		rightDrawerList.setAdapter(adapter);
		//rightDrawerList.setOnChildClickListener(this);
    	rightDrawerList.setOnGroupClickListener(this);
    	
	}
	
	private void configureRightHeaderView() { 
    	
    	if(postItHeaderView == null) {
	    	postItHeaderView = getLayoutInflater().inflate(R.layout.user_postit_header, null);
			rightDrawerList.addHeaderView(postItHeaderView); 
			postItHeaderView.findViewById(R.id.editPostItBtn).setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					onUserPostItEditClicked( postItHeaderView );
				}
				
			});
			postItHeaderView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					onUserPostItEditClicked(v);
				}
			});
			postItHeaderView.setTag(-1); 
    	}
    	
	
	}
	
	private View postItHeaderView = null; 
	
	private void onCurrentUserPostItFound(Cursor cursor) {
		
		Log.w(WordDetailsActivity.class.getName(), "onCurrentUserPostItFound() called...");
		if(cursor != null && cursor.getCount() > 0) { 
			
			int postItId = cursor.getInt(cursor.getColumnIndexOrThrow(PostItProvider.PostItTable.COLUMN_POST_IT_ID));
			String postText = cursor.getString(cursor.getColumnIndexOrThrow(PostItProvider.PostItTable.COLUMN_TEXT));
			String firstName = cursor.getString(cursor.getColumnIndexOrThrow(PostItProvider.PostItTable.COLUMN_AUTHOR_FIRST_NAME));
			String lastName = cursor.getString(cursor.getColumnIndexOrThrow(PostItProvider.PostItTable.COLUMN_AUTHOR_LAST_NAME));
			
			Log.w(WordDetailsActivity.class.getName(), "Current user post it is: " + postText); 
			
			if(postItHeaderView == null) { 
				postItHeaderView = getLayoutInflater().inflate(R.layout.user_postit_header, null);
				rightDrawerList.addHeaderView(postItHeaderView);
				postItHeaderView.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						onUserPostItEditClicked(v);
					}
				});
			}
			
			TextView postTextView = (TextView) postItHeaderView.findViewById(R.id.postText); 
			TextView firstNameTextView = (TextView) postItHeaderView.findViewById(R.id.authorFristName);
			TextView lastNameTextView = (TextView) postItHeaderView.findViewById(R.id.authorLastName);
			// ImageView editPostItImg = (ImageView) postItHeaderView.findViewById(R.id.editPostItImg);
			
			postTextView.setVisibility(View.VISIBLE);
			postItHeaderView.findViewById(R.id.editPostItBtn).setVisibility(View.GONE);
			postItHeaderView.findViewById(R.id.authorLayout).setVisibility(View.VISIBLE);
			
			postTextView.setText(postText);
			firstNameTextView.setText(firstName);
			lastNameTextView.setText(lastName);
			postItHeaderView.setTag(postItId); 
	
		}
	}
	
	/**
	 * Event handler called when user click to add/edit his post it for current word
	 * @param view
	 */
	private void onUserPostItEditClicked(View v) { 
		Log.w(WordDetailsActivity.class.getName(), "User clicked to edit post it for current word...");
		
		if(!NetworkUtilities.haveNetworkConnection(this)) { 
			promptTurnOnNetworkToEditPostIt(v);
		} else { 
			showPostItEditionDialog(v); 
		}
	}
	
	/**
	 * Method used to display special AlertDialog with EditText field 
	 * where user can add/edit his post it for current word.
	 * @param view
	 */
	private void showPostItEditionDialog(View v) {
		
		// accessing postItId if has been set previously
		// final int postItId = (Integer) v.getTag(); 
		
		View postEditionView = null; 
		postEditionView = getLayoutInflater().inflate(R.layout.postit_edition, null); 
		// setting current post text if exists in edition field
		final EditText editionField = (EditText) postEditionView.findViewById(R.id.postEditionField); 
		editionField.setText( ((TextView) v.findViewById(R.id.postText)).getText()  );
		
		AlertDialog dialog = new AlertDialog.Builder(this)
			.setView(postEditionView)
			.setCancelable(true)
			.setPositiveButton(R.string.save_button,  new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.w(WordDetailsActivity.class.getName(), "Edited Post It saving...");
					
					int wordId = getIntent().getExtras().getInt(WordDetailsActivity.KEY_WORD_ID); 
					PostItObject postIt = new PostItObject(wordId, editionField.getText().toString() ); 
					
					new PostItsSaver(WordDetailsActivity.this).execute(postIt); 
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.w(WordDetailsActivity.class.getName(), "Post It Edition canceled...");
				}
			}).create();
			
		dialog.show();
	}
	
	/**
	 * Method prompt user whether he want to turn on network connection
	 * in order to edit post it. 
	 * @param v
	 */
	private void promptTurnOnNetworkToEditPostIt(final View v) {
		
		AlertDialog dialog = new AlertDialog.Builder(this)
			.setMessage(R.string.no_network_dialog_message)
			.setCancelable(false)
			.setPositiveButton(R.string.connect_button,  new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				// Connect to Internet 
				WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				wifiManager.setWifiEnabled(true);
				
				int i = 0; 
				while(!NetworkUtilities.haveNetworkConnection(WordDetailsActivity.this) && i < 10 )
				{
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				// If successfully connected with WIFI or mobile network 
				boolean connected = NetworkUtilities.haveNetworkConnection(WordDetailsActivity.this);
				if(connected) {
						showPostItEditionDialog(v);  
				} else { 
					// When couldn't connect to the Internet user cannot edit his post it text.
					Log.w(WordDetailsActivity.class.getName(), "Trying to connect to the Internet failed!");
					Toast.makeText(WordDetailsActivity.this, getString(R.string.trying_to_connect_failed), Toast.LENGTH_SHORT).show();
				}
				
			}
		})
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Toast.makeText(WordDetailsActivity.this, 
								getString(R.string.cannot_add_post_toast), 
									Toast.LENGTH_SHORT).show();
			}
		})
		.create();

		dialog.show();
	}
	
	private View selectedDrawerItemView = null;
	private long selectedDrawerItemId = -1;
	
	@Override
	public boolean onGroupClick(ExpandableListView parent, View v,
				int groupPosition, long id) {
		
			if(parent == rightDrawerList) {
				if(selectedDrawerItemView != null) { 
					selectedDrawerItemView.setBackgroundResource(R.drawable.drawer_item_shape2);
		        }
				
		        v.setBackgroundResource(R.drawable.drawer_item_shape_pressed2);
		        selectedDrawerItemView = v;
		        selectedDrawerItemId = id; 
	        
		        postItDrawerItemClicked(id);
			}
			return true;
	}
	
	private void postItDrawerItemClicked(long id) {
		Log.w(WordDetailsActivity.class.getName(), "Post It Item Clicked on the drawer with id: " + id); 
		
	}


	@Override 
	public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getItemId() == getRightDrawerMenuButtonId()) {
        	/** if(!drawerLayout.isDrawerOpen(rightDrawerList))
        	 *	loadPostIts();
        	 */
        }
    	
		return super.onOptionsItemSelected(item);
    }
	
	@Override 
	public void onRightDrawerOpened(View drawerView) 
	{
		loadPostIts();
	}
	
	private void loadPostIts() { 
		
		Log.w(WordDetailsActivity.class.getName(),"Loading post its from online web service...");
		int wordId = getIntent().getExtras().getInt(WordDetailsActivity.KEY_WORD_ID, -1);
		new PostItsLoader(this).execute(wordId);
	}
	
	
	private class PostItsCursorAdapter extends SimpleCursorTreeAdapter 
									   implements LoaderManager.LoaderCallbacks<Cursor> {
		
		private static final int POST_ITS_LOADER_ID = -0x01;
		private Context context;
		private int wordId = -1;
		private int profileId = 0;
		
		protected final HashMap<Integer, Integer> groupMap;
		
		/**
		 * Cursor not provided as second argument to avoid querying on main thread.
		 */
		@SuppressLint("UseSparseArrays")
		public PostItsCursorAdapter(Context context, int wordId,
									int groupLayout, String[] groupFrom, int[] groupTo, 
									int childLayout, String[] childFrom, int[] childTo)
		{
			// calling SimpleCursorTreeAdapter constructor
			super(context, null, groupLayout, groupFrom, groupTo, childLayout, childFrom, childTo);
			
			// storing context object as attribute
			this.context = context;
			this.wordId = wordId; 
			this.profileId = Preferences.getInt(context, Preferences.KEY_PROFILE_ID, 0);
			
			groupMap = new HashMap<Integer, Integer>();
			
			Log.w(PostItsCursorAdapter.class.getName(), "Constructing ListView Adapter for word id: " + wordId); 
			
			// initializing or restarting cursor loader for current cursor tree adapter
			Loader<Cursor> cursorLoader = getSupportLoaderManager().getLoader(POST_ITS_LOADER_ID);
			if (cursorLoader != null && !cursorLoader.isReset()) {
				 getSupportLoaderManager().restartLoader(POST_ITS_LOADER_ID, null, this);
			} else {
				 getSupportLoaderManager().initLoader(POST_ITS_LOADER_ID, null, this);
			}
			
		}

		/**
		 * Method implements logic to get the children items cursor on the basis of selected group.
		 */
		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			
			int groupPosition = groupCursor.getPosition();
			int groupId = groupCursor.getInt(groupCursor.getColumnIndex(PostItProvider.PostItTable.COLUMN_POST_IT_ID));
			 
			Log.w(PostItsCursorAdapter.class.getName(), "getChildrenCursor() for groupPos " + groupPosition);
			Log.w(PostItsCursorAdapter.class.getName(), "getChildrenCursor() for groupId " + groupId);
			
			groupMap.put(groupId, groupPosition);
			 
			Loader<Cursor> cursorLoader = getSupportLoaderManager().getLoader(groupId);
			if (cursorLoader != null && !cursorLoader.isReset()) {
				getSupportLoaderManager().restartLoader(groupId, null, this);
			} else {
				getSupportLoaderManager().initLoader(groupId, null, this);
			}
			
			return null;
		}
		
		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
			super.bindGroupView(view, context, cursor, isExpanded);
			
			// searching for post it added by current user 
			int authorId = cursor.getInt(cursor.getColumnIndex(PostItProvider.PostItTable.COLUMN_AUTHOR_ID));
			if(authorId == profileId) { 
				Log.w(WordDetailsActivity.class.getName(), "Autor id: " + authorId + " == " + profileId + " :profileId.");
				// post it added by current user has been found. 
				// set this post it as list view header
				onCurrentUserPostItFound(cursor);
			}
			
		}
		
		@SuppressWarnings("unused")
		public HashMap<Integer, Integer> getGroupMap() {
			 return groupMap;
		 }

		/**
		 * Implementation of LoaderManager.LoaderCallbacks methods
		 */
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			
			Log.w(PostItsCursorAdapter.class.getName(), 
					"onCreateLoader for loader id: " + id);
			
			CursorLoader cursorLoader = null; 
			
			// CursorLoader is used to construct the new query 
			String[] projection = { PostItProvider.PostItTable.COLUMN_POST_IT_ID, 
    								PostItProvider.PostItTable.COLUMN_AUTHOR_ID,
    								PostItProvider.PostItTable.COLUMN_AUTHOR_FIRST_NAME,
    								PostItProvider.PostItTable.COLUMN_AUTHOR_LAST_NAME,
    								PostItProvider.PostItTable.COLUMN_TEXT
								  };
    						
			String where = null; 
			String[] whereArgs = null; 
			String sortOrder = null; 
			
			/**
			 * In most advanced case with groups/children 
			 * there will be id = -1 to load group items 
			 * and id >= 0 (groupId) to load child items for each group 
			 * Now we have only groups and load post it items for currently
			 * displayed word details into them. 
			 * For each selected group item we load cursor that contains only this selected post it item. 
			 */
			switch(id) { 
				case POST_ITS_LOADER_ID: 
					
					// Content URI for retrieving all post_it rows for current word. 
					Uri WORD_POST_ITS_URI = Uri.parse(PostItProvider.CONTENT_URI + "/word/" + wordId);
					
					// Create and return the new CursorLoader
					cursorLoader = new CursorLoader(context, WORD_POST_ITS_URI, projection, where, whereArgs, sortOrder); 
					break; 
				default: 
					
					// id != POST_ITS_LOADER_ID  -> id is groupId (post_it id) for currently selected group (post_it item) 
					
					// Content URI for retrieving post_it row for current post_it id
					Uri POST_IT_URI = Uri.parse(PostItProvider.CONTENT_URI + "/" + id);
					
					// Create and return the new CursorLoader
					cursorLoader =  new CursorLoader(context, POST_IT_URI, projection, where, whereArgs, sortOrder); 
					break;
			}
			
			return cursorLoader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
			Log.w(PostItsCursorAdapter.class.getName(), "onLoadFinished() for loader id:  " + cursorLoader.getId());
			
			/**
			 * In most advanced case with groups/children 
			 * there will be id = -1 to load group items 
			 * and id >= 0 (groupId) to load child items for each group 
			 * Now we have only groups and load post it items for currently
			 * displayed word details into them. 
			 * For each selected group item we load cursor that contains only this selected post it item.
			 */
			switch(cursorLoader.getId()) {
			
				case POST_ITS_LOADER_ID:
					
					Log.w(PostItsCursorAdapter.class.getName(), "postItsCursor.getCount(): " + cursor.getCount());
					setGroupCursor(cursor);
					
					break; 
				default: 
					// id != POST_ITS_LOADER_ID  -> id is groupId (post_it id) for currently selected group (post_it item) 
					if(!cursor.isClosed()) { 
						Log.w(PostItsCursorAdapter.class.getName(), "postItsCursor.getCount(): " + cursor.getCount());
						try { 
							int groupPosition = groupMap.get(cursorLoader.getId());
							Log.w(PostItsCursorAdapter.class.getName(), "onLoadFinished() for groupPositon:  " + groupPosition);
							setChildrenCursor(groupPosition, cursor);
						} catch(NullPointerException e) { 
							 Log.w(PostItsCursorAdapter.class.getName(),
									 "Adapter expired, try again on the next query: "
									 + e.getMessage());
						 }
						
					}
					break; 
			}		
			
		}

		/**
		 * Method called just before the cursor is about to be closed.
		 */
		@Override
		public void onLoaderReset(Loader<Cursor> cursorLoader) {
			Log.w(PostItsCursorAdapter.class.getName(), "onLoaderReset() for loader id: " + cursorLoader.getId());
			
			/**
			 * In most advanced case with groups/children 
			 * there will be id = -1 for group items cursor loader
			 * and id >= 0 (groupId) for child items cursor loader for each group 
			 * Now we have only groups and load post it items for currently
			 * displayed word details into them. 
			 * For each selected group item we load cursor that contains only this selected post it item.
			 */
			switch(cursorLoader.getId()) {
			
				case POST_ITS_LOADER_ID:
					// we have reset on post it items cursor loader (groups loader)
					setGroupCursor(null);
	
				default:
					// id != POST_ITS_LOADER_ID  -> id is groupId (post_it id) for currently selected group (post_it item) 
					try { 
						int groupPosition = groupMap.get(cursorLoader.getId());
						setChildrenCursor(groupPosition, null);
						Log.w(PostItsCursorAdapter.class.getName(), "onLoaderReset() for groupPositon:  " + groupPosition);
					} catch(NullPointerException e) { 
						Log.w(PostItsCursorAdapter.class.getName(), "Adapter expired, try again on the next query: "
								 + e.getMessage());
					}
					break;
				
			}
		
		}
	}


	@Override
	public void onPostItSaved() {
		loadPostIts();
	}


	@Override
	public void onPostItSavingError() {
		Toast.makeText(this, R.string.error_while_saving_post, Toast.LENGTH_SHORT).show();
	}
	
}