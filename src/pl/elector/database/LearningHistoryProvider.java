package pl.elector.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class LearningHistoryProvider extends ContentProvider {
	
	// defining ContentProvider's URI address
	private static final String AUTHORITY = "pl.elector.provider.LearningHistoryProvider"; 
	private static final String BASE_PATH = "learning_history"; 
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH); 
	
	// defining a UriMatcher to differentiate between different URI requests: 
	// for all elements, subset of rows for given wordset+user or mode+user (learning_method+user) 
	// and subset of rows for given user id
	// and a single row (for current user id and given wordset id + mode id ) 
	// or single row (using learning_history_id) 
	// also defining special URI to get all not_synced rows for given user 
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2; 
	private static final int ROW_FOR_USER_AND_WORDSET_AND_MODE_AND_TYPE = 3;  // single row (unique index) 
	private static final int ROWS_FOR_USER = 4; 
	private static final int ROWS_FOR_USER_AND_WORDSET = 5; 
	private static final int ROWS_FOR_USER_AND_MODE = 6; 
	private static final int ROWS_FOR_USER_AND_TYPE = 8; 
	private static final int ROWS_FOR_USER_NOT_SYNCED = 9; 
	private static final int ROW_INSERT_OR_UPDATE = 10; // used only to insert/update row, in other cases throws Exception
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending
	// in 'learning_history' represents a request for all learning history items 
	// and 'learning_history/[rowId]' represents a single row 
	// and 'learning_history/user/[uid]/wordset/[setid]/mode/[modeid]' represents a single row for given wordset id and mode id and type id
	// and 'learning_history/user/[uid]' represents subset of rows for given user id 
	// and 'learning_history/user/[uid]/wordset/[setid]' represent subset of rows for given user id and wordset id 
	// and 'learning_history/user/[uid]/mode/[modeid]' represents subset of rows for given user id and mode id 
	// and 'learning_history/user/[uid]/wordset_type/[type_id]' represents subset of rows for given user id and wordset type id
	// and 'learning_history/user/[uid]/not_synced' represents rows for given user id that haven't been synced yet
	static { 
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/user/#/wordset/#/mode/#/wordset_type/#", ROW_FOR_USER_AND_WORDSET_AND_MODE_AND_TYPE);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/user/#", ROWS_FOR_USER);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/user/#/wordset/#", ROWS_FOR_USER_AND_WORDSET);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/user/#/mode/#", ROWS_FOR_USER_AND_MODE);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/user/#/wordset_type/#", ROWS_FOR_USER_AND_TYPE);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/user/#/not_synced",ROWS_FOR_USER_NOT_SYNCED);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/insert_or_update", ROW_INSERT_OR_UPDATE); 
	}
	
	// reference to SQLiteOpenHelper class instance
	// used to construct the underlying database
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows (including rows for given user or user and wordset/mode) 
	// and for single rows.
	private static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "vnd.elector.learning_history"; 
	private static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "vnd.elector.learning_history";

	/**
	 * This method deletes single learning history item or set of items for given user 
	 * or user and wordset/mode or all rows depending on URI address. 
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		
		// If this is a row URI limit the deletion to specified row 
		// else if this is user URI limit the deletion to specified user rows
		// else if this is user and wordset URI limit the deletion to specified user and wordset combination rows 
		// else if this is user and mode URI limit the deletion to sepcified user and mode combination rows 
		switch( uriMatcher.match(uri) ) {
			
			case SINGLE_ROW: { 
				String rowID = uri.getPathSegments().get(1);
				selection = LearningHistoryTable.COLUMN_LEARNING_HISTORY_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROW_FOR_USER_AND_WORDSET_AND_MODE_AND_TYPE: {
				String userID = uri.getPathSegments().get(2); 
				String wordsetID = uri.getPathSegments().get(4);
				String modeID = uri.getPathSegments().get(6); 
				String wordsetTypeID = uri.getPathSegments().get(8);
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + userID
						+ " AND " + LearningHistoryTable.COLUMN_WORDSET_ID + "=" + wordsetID
						+ " AND " + LearningHistoryTable.COLUMN_MODE_ID + "=" + modeID 
						+ " AND " + LearningHistoryTable.COLUMN_WORDSET_TYPE_ID + "=" + wordsetTypeID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 	
				break;
			}
			case ROWS_FOR_USER: {
				String userID = uri.getPathSegments().get(2); 
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + userID 
						+ (!TextUtils.isEmpty(selection)? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_USER_AND_WORDSET: {
				String userID = uri.getPathSegments().get(2); 
				String wordsetID = uri.getPathSegments().get(4); 
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + userID 
						+ " AND " + LearningHistoryTable.COLUMN_WORDSET_ID + "=" + wordsetID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_USER_AND_MODE: { 
				String userID = uri.getPathSegments().get(2); 
				String modeID = uri.getPathSegments().get(4);
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + userID 
						+ " AND " + LearningHistoryTable.COLUMN_MODE_ID + "=" + modeID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_USER_AND_TYPE: { 
				String userID = uri.getPathSegments().get(2); 
				String wordsetTypeID = uri.getPathSegments().get(4); 
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + userID 
						+ " AND " + LearningHistoryTable.COLUMN_WORDSET_TYPE_ID + "=" + wordsetTypeID
						+ (!TextUtils.isEmpty(selection) ? " AND (" 
						+ selection + ")" : "");
				break;
			}
			case ROWS_FOR_USER_NOT_SYNCED: { 
				String userID = uri.getPathSegments().get(2); 
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + userID 
						+ " AND " + LearningHistoryTable.COLUMN_NOT_SYNCED + "= 1"
						+ (!TextUtils.isEmpty(selection) ? " AND ("
					    + selection + ")" : "");
				break;
			}
			case ROW_INSERT_OR_UPDATE:
				throw new IllegalArgumentException("Unsupported URI for deletion: " + uri); 
			default: 
				break;
		}
		
		// To return the number of deleted items, you must specify 
		// a where clause. To delete all rows and return a value, pass in "1". 
		if(selection == null) 
			selection = "1"; 
		
		// Execute the deletion. 
		int deleteCount = db.delete(LearningHistoryTable.TABLE_LEARNING_HISTORY, selection, selectionArgs); 
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return deleteCount;
	}

	/**
	 * This method is used to return correct MIME type depending on the query type: 
	 * single row, row for user and wordset and mode, all rows, subset of rows for given combination. 
	 */
	@Override
	public synchronized String getType(Uri uri) {
		
		// For a given query's Content URI we return a suitable MIME type.
		switch(uriMatcher.match(uri)) { 
			
			case SINGLE_ROW: 
			case ROW_FOR_USER_AND_WORDSET_AND_MODE_AND_TYPE: 
			case ROW_INSERT_OR_UPDATE:
				return CONTENT_ITEM_MIME_TYPE; 
			case ROWS_FOR_USER:
			case ROWS_FOR_USER_AND_WORDSET: 
			case ROWS_FOR_USER_AND_MODE: 
			case ROWS_FOR_USER_AND_TYPE: 
			case ROWS_FOR_USER_NOT_SYNCED: 
			case ALLROWS:
				return CONTENT_MIME_TYPE; 
			default: 
				throw new IllegalArgumentException("Unsupported URI: " + uri); 
		}
	}

	/**
	 * Transaction method used to insert a new row into database (represented by Content Values) 
	 */
	@Override
	public synchronized Uri insert(Uri uri, ContentValues values) {
		
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// To add empty rows to your database by passing in an empty 
		// Content Values object, you must use the null column hack 
		// parameter to specify the name of the column that can be set to null. 
		String nullColumnHack = null; 
		
		long id = -1; 
		// checking whether Content URI address is suitable 
		switch( uriMatcher.match(uri)) { 
		
			case ALLROWS: {
				// insert the values into the table
				id = db.insert(LearningHistoryTable.TABLE_LEARNING_HISTORY, nullColumnHack, values);
				break; 
			}
			case ROW_INSERT_OR_UPDATE: { 
				id = insertOrUpdate(db, nullColumnHack, values);
				break;
			}
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		Log.d(LearningHistoryProvider.class.getName(), "Inserted/updateg learning history id: "+ id); 
		
		if( id > -1 )
		{
			// construct and return the URI of the newly inserted row.
			Uri insertedItemUri = ContentUris.withAppendedId(CONTENT_URI, id);
			
			// notify any observers of the change in the data set.
			getContext().getContentResolver().notifyChange(insertedItemUri, null);
			
			return insertedItemUri;
		} else 
			return null;
		
	}
	
	/**
	 * Helper method used to do insert or update operation on learning history table 
	 * depending on whether it already contains or not corresponding row.
	 */
	private long insertOrUpdate(SQLiteDatabase db, String nullColumnHack, ContentValues values ) throws SQLException { 
		long id = -1; 
		// insert new row or if already exists update it
		// there is unique key on (profile_id, wordset_id, mode_id, type_id) columns
		try { 
			id = db.insertOrThrow(LearningHistoryTable.TABLE_LEARNING_HISTORY, nullColumnHack, values);
		} catch(SQLiteConstraintException e) { 
			Log.d(LearningHistoryProvider.class.getName(), "Updating learning history row..."); 
			// row with such unique index already exists in the table, update it
			Uri updateUri = Uri.parse(CONTENT_URI 
							+ "/user/" + values.getAsInteger(LearningHistoryTable.COLUMN_PROFILE_ID)
							+ "/wordset/" + values.getAsInteger(LearningHistoryTable.COLUMN_WORDSET_ID) 
							+ "/mode/" + values.getAsInteger(LearningHistoryTable.COLUMN_MODE_ID)
							+ "/wordset_type/" + values.getAsInteger(LearningHistoryTable.COLUMN_WORDSET_TYPE_ID)); 
			int updateCount = update(updateUri, values, null, null); 
			if(updateCount == 0 ) { 
				Log.d(LearningHistoryProvider.class.getName(), "Error while updating learnign history row."); 
				throw e; 
			} else {  // if row successfully updated than query for learning_history_id of updated row 
				Log.d(LearningHistoryProvider.class.getName(), "Querying learning history id..."); 
				Cursor cursor = query(updateUri, new String[] { LearningHistoryTable.COLUMN_LEARNING_HISTORY_ID } , null, null, null);
				
				if(cursor != null && cursor.getCount() == 1) { 
					cursor.moveToFirst(); 
					id = cursor.getInt(cursor.getColumnIndexOrThrow(LearningHistoryTable.COLUMN_LEARNING_HISTORY_ID)); 
				} else { 
					Log.d(LearningHistoryProvider.class.getName(), "Error while selecting learning history id."); 
					throw e; 
				}
			}
		}
		return id; 
	}

	@Override
	public synchronized boolean onCreate() {
		// creating instance of SQLiteOpenHelper that 
		// effectively defer creating and opening database 
		// until it's required. 
		databaseHelper = new DatabaseSQLiteOpenHelper(getContext()); 
		// returns true if the provider was successfully loaded 
		return true;
	}

	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection, 
			String[] selectionArgs, String sortOrder) {
		
		// Open the underlying database
		SQLiteDatabase db; 
		try { 
			db = databaseHelper.getWritableDatabase(); 
		} catch(SQLiteException ex) { 
			db = databaseHelper.getReadableDatabase(); 
		}
		
		// Replace this with valid SQL statements if necessary 
		String groupBy = null; 
		String having = null; 
		
		// Using SQLiteQueryBuilder instead of query() method
		// in order to simplify database query construction
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder(); 
		
		switch( uriMatcher.match(uri) ) 
		{ 
			case SINGLE_ROW: { 
				queryBuilder.setTables(LearningHistoryTable.TABLE_LEARNING_HISTORY); 
				// getting rowID for current query and setting where clause
				String rowID = uri.getPathSegments().get(1); 
				queryBuilder.appendWhere(LearningHistoryTable.COLUMN_LEARNING_HISTORY_ID + "=" + rowID); 
				break; 
			} 
			case ROW_FOR_USER_AND_WORDSET_AND_MODE_AND_TYPE: {
				queryBuilder.setTables(LearningHistoryTable.TABLE_LEARNING_HISTORY); 
				// getting profileID and wordsetID and modeID and wordsetTypeID for current query and setting where clause
				String profileID = uri.getPathSegments().get(2); 
				String wordsetID = uri.getPathSegments().get(4); 
				String modeID = uri.getPathSegments().get(6); 
				String wordsetTypeID = uri.getPathSegments().get(8); 
				queryBuilder.appendWhere(LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID 
										+ " AND " + LearningHistoryTable.COLUMN_WORDSET_ID + "=" + wordsetID 
										+ " AND " + LearningHistoryTable.COLUMN_MODE_ID + "=" + modeID
										+ " AND " + LearningHistoryTable.COLUMN_WORDSET_TYPE_ID + "=" + wordsetTypeID); 
				break; 
			}
			case ROWS_FOR_USER: { 
				queryBuilder.setTables(LearningHistoryTable.TABLE_LEARNING_HISTORY); 
				// getting profileID for current query and setting where clause
				String profileID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID); 
				break;
			} 
			case ROWS_FOR_USER_AND_WORDSET: { 
				queryBuilder.setTables(LearningHistoryTable.TABLE_LEARNING_HISTORY); 
				// getting profileID and wordsetID for current query and setting where clause 
				String profileID = uri.getPathSegments().get(2); 
				String wordsetID = uri.getPathSegments().get(4);
				queryBuilder.appendWhere(LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND " + LearningHistoryTable.COLUMN_WORDSET_ID + "=" + wordsetID); 
				break; 
			}
			case ROWS_FOR_USER_AND_MODE: { 
				queryBuilder.setTables(LearningHistoryTable.TABLE_LEARNING_HISTORY);
				// getting profileID and modeID for current query and setting where clause
				String profileID = uri.getPathSegments().get(2); 
				String modeID = uri.getPathSegments().get(4); 
				queryBuilder.appendWhere(LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND " + LearningHistoryTable.COLUMN_MODE_ID + "=" + modeID); 
				break;
			}
			case ROWS_FOR_USER_AND_TYPE: { 
				queryBuilder.setTables(LearningHistoryTable.TABLE_LEARNING_HISTORY); 
				// getting profileID and wordsetTypeID for current query and setting where clause 
				String profileID = uri.getPathSegments().get(2); 
				String wordsetTypeID = uri.getPathSegments().get(4); 
				queryBuilder.appendWhere(LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND " + LearningHistoryTable.COLUMN_WORDSET_TYPE_ID + "=" + wordsetTypeID); 
				break; 
			}
			case ROWS_FOR_USER_NOT_SYNCED: { 
				queryBuilder.setTables(LearningHistoryTable.TABLE_LEARNING_HISTORY); 
				// getting profileID for current query and setting where clause 
				String profileID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND " + LearningHistoryTable.COLUMN_NOT_SYNCED + "= 1");
				break; 
			}
			case ALLROWS: { 
				queryBuilder.setTables(LearningHistoryTable.TABLE_LEARNING_HISTORY); 
				break; 
			}
			case ROW_INSERT_OR_UPDATE: 
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder);
		
		// return the result set Cursor 
		return cursor;
	}
	
	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transcription.
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		
		// Modify selection argument to indicate updated row or rows 
		switch( uriMatcher.match(uri) ) 
		{ 
			case SINGLE_ROW: { 
				String rowID = uri.getPathSegments().get(1); 
				selection = LearningHistoryTable.COLUMN_LEARNING_HISTORY_ID + "=" + rowID 
						+ (!TextUtils.isEmpty(selection) ? " AND (" 
						+ selection + ")" : ""); 
				break; 
			}
			case ROW_FOR_USER_AND_WORDSET_AND_MODE_AND_TYPE: {
				String profileID = uri.getPathSegments().get(2); 
				String wordsetID = uri.getPathSegments().get(4); 
				String modeID = uri.getPathSegments().get(6); 
				String wordsetTypeID = uri.getPathSegments().get(8); 
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND " + LearningHistoryTable.COLUMN_WORDSET_ID + "=" + wordsetID
						+ " AND " + LearningHistoryTable.COLUMN_MODE_ID + "=" + modeID
						+ " AND " + LearningHistoryTable.COLUMN_WORDSET_TYPE_ID + "=" + wordsetTypeID
						+ (!TextUtils.isEmpty(selection) ? " AND (" 
						+ selection + ")" : ""); 
				break; 
			}
			case ROWS_FOR_USER: { 
				String profileID = uri.getPathSegments().get(2); 
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ (!TextUtils.isEmpty(selection) ? " AND (" 
						+ selection + ")" : ""); 
				break; 
			}
			case ROWS_FOR_USER_AND_WORDSET: { 
				String profileID = uri.getPathSegments().get(2); 
				String wordsetID = uri.getPathSegments().get(4); 
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND " + LearningHistoryTable.COLUMN_WORDSET_ID + "=" + wordsetID 
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			}
			case ROWS_FOR_USER_AND_MODE: { 
				String profileID = uri.getPathSegments().get(2); 
				String modeID = uri.getPathSegments().get(4); 
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND " + LearningHistoryTable.COLUMN_MODE_ID + "=" + modeID 
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_USER_AND_TYPE: { 
				String profileID = uri.getPathSegments().get(2); 
				String wordsetTypeID = uri.getPathSegments().get(4); 
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND " + LearningHistoryTable.COLUMN_WORDSET_TYPE_ID + "=" + wordsetTypeID
					    + (!TextUtils.isEmpty(selection) ? " AND ("
					    + selection + ")" : "");
				break; 
			}
			case ROWS_FOR_USER_NOT_SYNCED: { 
				String profileID = uri.getPathSegments().get(2); 
				selection = LearningHistoryTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND " + LearningHistoryTable.COLUMN_NOT_SYNCED + "= 1"  
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			}
			case ROW_INSERT_OR_UPDATE: { 
				throw new IllegalArgumentException();
			}
			default: break;  
		}
		
		// Perform the update. 
		int updateCount = db.update(LearningHistoryTable.TABLE_LEARNING_HISTORY, 
									values, selection, selectionArgs);
		
		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null); 
	
		return updateCount;
	}
	
	public static class LearningHistoryTable { 
		
		// Database table 
		public static final String TABLE_LEARNING_HISTORY = "learningHistoryTable"; 
		// learning history id in local SQLite table isn't compatible with server side database  
		// so cannot be copied (exported/imported) between this two database systems! 
		public static final String COLUMN_LEARNING_HISTORY_ID = "_id";  // INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
		public static final String COLUMN_PROFILE_ID = "profileId";  // INTEGER NOT NULL
		public static final String COLUMN_WORDSET_ID = "wordsetId"; // INTEGER NOT NULL
		public static final String COLUMN_MODE_ID = "modeId";  // INTEGER NOT NULL
		public static final String COLUMN_WORDSET_TYPE_ID = "wordsetTypeId"; // INTEGER NOT NULL
		public static final String COLUMN_BAD_ANSWERS = "badAnswers";  // INTEGER NOT NULL
		public static final String COLUMN_GOOD_ANSWERS = "goodAnswers"; // INTEGER NOT NULL
		public static final String COLUMN_IMPROVEMENT = "improvement"; // FLOAT NOT NULL
		public static final String COLUMN_HITS = "hits"; // FLOAT NOT NULL
		public static final String COLUMN_LAST_ACCESS_DATE = "lastAccessDate"; // TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
		public static final String COLUMN_NOT_SYNCED = "notSynced"; //BOOLEAN NOT NULL (NUMERIC, 0 - synced, 1 - not_synced)
		
		// Database Table creation SQL Statement 
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_LEARNING_HISTORY 
				+ " ("
				+ COLUMN_LEARNING_HISTORY_ID + " integer primary key autoincrement not null, "
				+ COLUMN_PROFILE_ID + " integer not null default 0, " // 0 - Anonymous user
				+ COLUMN_WORDSET_ID + " integer not null, "
				+ COLUMN_MODE_ID + " integer not null, "
				+ COLUMN_WORDSET_TYPE_ID + " integer not null, "
				+ COLUMN_BAD_ANSWERS + " integer not null, "
				+ COLUMN_GOOD_ANSWERS + " integer not null, "
				+ COLUMN_IMPROVEMENT + " float not null, "
				+ COLUMN_HITS + " integer not null, "
				+ COLUMN_LAST_ACCESS_DATE + " datetime default current_timestamp not null, "
				+ COLUMN_NOT_SYNCED + " boolean not null, "
				+ " unique(" + COLUMN_PROFILE_ID + ", " + COLUMN_WORDSET_ID + ", " 
							 + COLUMN_MODE_ID + ", " + COLUMN_WORDSET_TYPE_ID + ") on conflict abort, "
				+ " foreign key (" + COLUMN_PROFILE_ID + ") references "
				+ ProfileProvider.ProfileTable.TABLE_PROFILE
				+ "(" + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + ")"
				+ " on update cascade on delete cascade "
				+ " foreign key (" + COLUMN_WORDSET_ID + ") references "
				+ WordsetProvider.WordsetTable.TABLE_WORDSET
				+ "(" + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID + ")"
				+ " on update cascade on delete no action "
				+ ")";
		
		// TRIGGERS: 
		// 1) insert trigger on learning_history table checks if corresponding profile exists 
		private static final String PROFILE_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_LEARNING_HISTORY + "_" + COLUMN_PROFILE_ID + " "
				+ "before insert on " + TABLE_LEARNING_HISTORY + " "
				+ "for each row begin "
						+ "select raise(rollback, 'insert on table " + TABLE_LEARNING_HISTORY
										+ " violates foreign key constraint') "
						+ "where new." + COLUMN_PROFILE_ID + "!= 0 AND (select " // 0 - Anonymous user 
								+ ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
								+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
								+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
								+ " = new." + COLUMN_PROFILE_ID + ") is null;"
					+ " end;";
		
		// 2) update trigger on learning_history table, checks if new profile exists
		private static final String PROFILE_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_LEARNING_HISTORY + "_" + COLUMN_PROFILE_ID + " "
				+ "before update on " + TABLE_LEARNING_HISTORY + " "
				+ "for each row begin "
						+ "select raise(rollback, 'update on table " + TABLE_LEARNING_HISTORY
										+ " violates foreign key constraint') "
						+ "where new." + COLUMN_PROFILE_ID + "!= 0 AND (select " 
								+ ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
								+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
								+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
								+ " = new." + COLUMN_PROFILE_ID + ") is null;"
					+ " end;";
		
		// 3) delete trigger on profile table, cascade deletes corresponding learning_history rows
		private static final String PROFILE_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_LEARNING_HISTORY + "_" + COLUMN_PROFILE_ID + " "
				+ "before delete on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin "
						+ "delete from " + TABLE_LEARNING_HISTORY
							+ " where " + COLUMN_PROFILE_ID
							+ " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + ";"
				+ " end;"; 
		
		// 4) update trigger on profile table, cascade updates corresponding learning_history rows
		private static final String PROFILE_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_LEARNING_HISTORY + "_" + COLUMN_PROFILE_ID + " "
				+ "after update on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin "
						+ "update " + TABLE_LEARNING_HISTORY + " set " + COLUMN_PROFILE_ID 
						+ " = new." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
						+ " where " + COLUMN_PROFILE_ID + " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
						+ "; "
				+ "end;"; 
		
		// called when no database exists in disk and the SQLiteOpenHelper 
		// class needs to create a new one.
		public static void onCreate(SQLiteDatabase database)
		{
			// LearningHistory table creation in database (with additional triggers)
			Log.d(LearningHistoryTable.class.getName(), "Creating database LearningHistory table."); 
			database.execSQL(TABLE_CREATE);
			database.execSQL(PROFILE_INSERT_TRIGGER_CREATE);
			database.execSQL(PROFILE_UPDATE_TRIGGER_CREATE); 
			database.execSQL(PROFILE_DELETE_TRIGGER_CREATE);
			database.execSQL(PROFILE_PARENT_UPDATE_TRIGGER_CREATE);
		}
		
		// called when there is a database version mismatch meaning that the version
		// of the database on disk needs to be upgraded to the current version. 
		public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
		{
			// Log the version upgrade 
			Log.w(LearningHistoryTable.class.getName(), 
					"Upgrading database LearningHistory table from version " + oldVersion 
					+ " to " + newVersion + ", which will destroy all old data.");
			
			// Upgrading the existing database to conform to the new version.
			// Multiple previous versions can be handled by comparing oldVersion 
			// and newVersion values.
			
			// Upgrade database by adding new version of LearningHistory table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_LEARNING_HISTORY); 
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_LEARNING_HISTORY + "_" + COLUMN_PROFILE_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_LEARNING_HISTORY + "_" + COLUMN_PROFILE_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_LEARNING_HISTORY + "_"+ COLUMN_PROFILE_ID);  
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_LEARNING_HISTORY + "_" + COLUMN_PROFILE_ID);
			onCreate(database); 
			
		}
	}
	
	/** 
	 * Defining different available modes and their mappings to modeIds
	 * used while storing learning history items in database. 
	 */
	public static enum Mode {
		WEB_CROSSWORD(1), 
		WEB_MATCHER(2), 
		WEB_LISTENING(3), 
		WEB_MEMORY_GAME(4), 
		WEB_REPETITION(5), 
		WEB_PRESENTATION(6), 
		WEB_SNAKE(7), 
		WEB_SORTING(8), 
		WEB_GALLOW(9), 
		WEB_SCRABBLE(10), 
		WEB_FILLING_GAPS(11), 
		WEB_SELECTION(12), 
		ANDROID_FLASH_CARDS(101), 
		ANDROID_SIMPLE_REPETITION(102),
		ANDROID_PRESENTATION(103),
		ANDROID_REPETITION(104), 
		ANDROID_SPEAKING(105), 
		ANDROID_LISTENING(106), 
		ANDROID_SELECTION(107), 
		ANDROID_MEMORY_GAME(108);
		
		// instance variable for storing corresponding modeId 
		private int modeID; 
		
		// enumeration item constructor 
		Mode(int modeID) throws OutOfRangeException { 
			if( modeID < 0 ) 
				throw new OutOfRangeException("Mode identifier must be higher than zero."); 
			// set for current mode item its identifier value
			this.modeID = modeID; 
		}
		
		public int id() { 
			return modeID; 
		}
		
		public static Mode name(int modeID) { 
			for(Mode mode : values()) { 
				if(mode.compare(modeID)) return mode;
			}
			return null; 
		}
		
		public boolean compare(int modeID) { 
			if(this.modeID == modeID) 
				return true; 
			else 
				return false; 
		}
		
		// defining OutOfRangeException class
		static class OutOfRangeException extends ExceptionInInitializerError { 
			
			private String msg; 
			
			OutOfRangeException(String msg) { 
				this.msg = msg; 
			}
			
			@Override
			public String getMessage() { 
				return msg; 
			}
		}
	}; 

}
