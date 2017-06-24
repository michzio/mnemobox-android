/**
 * @date 16.10.2014
 */
package pl.elector.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * @author Michał Ziobro
 *
 */
public class RememberMeNotSyncedProvider extends ContentProvider {
	
	// defining ContentProvider's URI address 
	public static final String AUTHORITY = "pl.elector.provider.RememberMeNotSyncedProvider"; 
	private static final String BASE_PATH = "remember_me_words_not_synced";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH); 
	
	// defining a UriMatcher to differentiate between different URI requests: 
	// 1) for all elements
	// 2) subset of rows for given profile id 
	// 3) single row for given profile and word id 
	// 4) single row for specific remember_me id
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2; 
	private static final int ROWS_FOR_PROFILE = 3;  
	private static final int ROW_FOR_PROFILE_AND_WORD = 4;
	private static final int ROWS_FOR_NOT_EXISTING_WORDS = 5;  
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending
	// in 'remember_me_words_not_synced' represents a request for all items
	// and 'remember_me_words_not_synced/[rowId]' represents a single row, 
	// and 'remember_me_words_not_synced/profile/[profileId]/word/[wordId]' represents a single row for given profile and word id
	// and 'remember_me_words_not_synced/profile/[profileId]' represents request for subset of rows for given profile
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#/word/#", ROW_FOR_PROFILE_AND_WORD); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#", ROWS_FOR_PROFILE);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/not_existing/#", ROWS_FOR_NOT_EXISTING_WORDS);
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database.
	private DatabaseSQLiteOpenHelper databaseHelper;
	
	// defining the MIME types for all rows (including rows for given profile)
	// and a single row. 
	private static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.remember_me_words_not_synced"; 
	private static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.remember_me_words_not_synced"; 
		

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
				
		// Limit the deletion to specified row or subset of rows based on URI.
		switch(uriMatcher.match(uri))
		{
			case SINGLE_ROW: {
				String rowID = uri.getPathSegments().get(1); 
				selection = RememberMeNotSyncedTable.COLUMN_REMEMBER_ME_NOT_SYNCED_ID + "=" + rowID
					+ (!TextUtils.isEmpty(selection) ? " AND ("
					+ selection + ")" : ""); 
				break;
			}
			case ROW_FOR_PROFILE_AND_WORD: {
				String profileID = uri.getPathSegments().get(2);
				String wordID = uri.getPathSegments().get(4); 
				selection = RememberMeNotSyncedTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND " + RememberMeNotSyncedTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break;
			}
			case ROWS_FOR_PROFILE: {
				String profileID = uri.getPathSegments().get(2); 
				selection = RememberMeNotSyncedTable.COLUMN_PROFILE_ID + "=" + profileID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			}
			case ROWS_FOR_NOT_EXISTING_WORDS:
			{
				// Unimplemented!
				throw new IllegalArgumentException(); 
			}
			default: break; 
		}
		
		// To return the number of deleted items, you must specify 
		// a where clause. To delete all rows and return a value, pass in "1".
		if(selection == null)
			selection = "1"; 
		
		// Execute the deletion. 
		int deleteCount = db.delete(RememberMeNotSyncedTable.TABLE_REMEMBER_ME_NOT_SYNCED, 
										selection, selectionArgs);
				
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
				
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return correct MIME type depending on the query type:
	 * single row, all rows, subset of rows for given profile.
	 */
	@Override
	public synchronized String getType(Uri uri) {
		
		// For a given query's Content URI we return suitable MIME type.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
			case ROW_FOR_PROFILE_AND_WORD: 
				return CONTENT_ITEM_MIME_TYPE; 
			case ALLROWS:
			case ROWS_FOR_PROFILE:
			case ROWS_FOR_NOT_EXISTING_WORDS:
				return CONTENT_MIME_TYPE; 
			default: 
				throw new IllegalArgumentException("Unsupported URI: " + uri); 
		}
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
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
		switch( uriMatcher.match(uri))
		{
			case ALLROWS: 
				// insert the values into the table 
				id = db.insert(RememberMeNotSyncedTable.TABLE_REMEMBER_ME_NOT_SYNCED,
								nullColumnHack, values); 
				break; 
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
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

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public synchronized boolean onCreate() {
		// creating instance of SQLiteOpenHelper that 
		// effectively defer creating and opening database 
		// until it's required. 
		databaseHelper = new DatabaseSQLiteOpenHelper(getContext()); 
		// returns true if the provider was successfully loaded
		return true;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 */
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
			case SINGLE_ROW: 
			{
				queryBuilder.setTables(RememberMeNotSyncedTable.TABLE_REMEMBER_ME_NOT_SYNCED); 
				// getting rowID for current query and setting where clause
				String rowID = uri.getPathSegments().get(1); 
				queryBuilder.appendWhere(RememberMeNotSyncedTable.COLUMN_REMEMBER_ME_NOT_SYNCED_ID + "=" + rowID); 
				break; 
			}
			case ROW_FOR_PROFILE_AND_WORD: 
			{
				queryBuilder.setTables(RememberMeNotSyncedTable.TABLE_REMEMBER_ME_NOT_SYNCED);
				// getting profileID and wordID for current query and setting where clause
				String profileID = uri.getPathSegments().get(2); 
				String wordID = uri.getPathSegments().get(4); 
				queryBuilder.appendWhere(RememberMeNotSyncedTable.COLUMN_PROFILE_ID + "=" + profileID
										+ " AND " + RememberMeNotSyncedTable.COLUMN_WORD_ID + "=" + wordID);
				break; 
			}
			case ROWS_FOR_PROFILE: 
			{
				queryBuilder.setTables(RememberMeNotSyncedTable.TABLE_REMEMBER_ME_NOT_SYNCED); 
				// getting profileID for current query and setting where clause 
				String profileID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(RememberMeNotSyncedTable.COLUMN_PROFILE_ID + "=" + profileID);
				break; 
			}
			case ALLROWS: 
			{
				queryBuilder.setTables(RememberMeNotSyncedTable.TABLE_REMEMBER_ME_NOT_SYNCED); 
				break; 
			}
			case ROWS_FOR_NOT_EXISTING_WORDS:
			{
				String profileID = uri.getPathSegments().get(2); 
				String selectionSQL = "SELECT * FROM " + RememberMeNotSyncedTable.TABLE_REMEMBER_ME_NOT_SYNCED
									  + " AS NS "
									  + " WHERE NS." + RememberMeNotSyncedTable.COLUMN_PROFILE_ID + "= ? "
									  + " AND NS." + RememberMeNotSyncedTable.COLUMN_TO_DELETE + " = 0"
									  + " AND (SELECT RM." + RememberMeProvider.RememberMeTable.COLUMN_REMEMBER_ME_ID 
									  + " FROM " + RememberMeProvider.RememberMeTable.TABLE_REMEMBER_ME
									  + " AS RM "
									  + " WHERE RM." + RememberMeProvider.RememberMeTable.COLUMN_PROFILE_ID + " = ? "
									  + " AND RM." + RememberMeProvider.RememberMeTable.COLUMN_WORD_ID  
									  + " = NS." + RememberMeNotSyncedTable.COLUMN_WORD_ID + " LIMIT 1) IS NULL;";
				String[] whereArgs = new String[] { profileID, profileID, };
				
				Log.w(RememberMeNotSyncedProvider.class.getName(), "Executing query: " + selectionSQL); 
				Cursor cursor = db.rawQuery(selectionSQL, whereArgs); 
				return cursor;
			}
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder);
		
		// return the result set Cursor 
		return cursor;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// Modify selection argument to indicate updated row or rows
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: {
				String rowID = uri.getPathSegments().get(1); 
				selection = RememberMeNotSyncedTable.COLUMN_REMEMBER_ME_NOT_SYNCED_ID + "=" + rowID
					+ (!TextUtils.isEmpty(selection) ? " AND ("
					+ selection + ")" : ""); 
				break;
			}
			case ROW_FOR_PROFILE_AND_WORD: {
				String profileID = uri.getPathSegments().get(2);
				String wordID = uri.getPathSegments().get(4); 
				selection = RememberMeNotSyncedTable.COLUMN_PROFILE_ID + "=" + profileID
					+ " AND " + RememberMeNotSyncedTable.COLUMN_WORD_ID + "=" + wordID
					+ (!TextUtils.isEmpty(selection) ? " AND ("
					+ selection + ")" : ""); 
				break;
			}
			case ROWS_FOR_PROFILE: {
				String profileID = uri.getPathSegments().get(2); 
				selection = RememberMeNotSyncedTable.COLUMN_PROFILE_ID + "=" + profileID
					+ (!TextUtils.isEmpty(selection) ? " AND ("
					+ selection + ")" : "");
				break;
			}
			case ROWS_FOR_NOT_EXISTING_WORDS:
			{
				// Unimplemented!
				throw new IllegalArgumentException(); 
			}
			default: break; 
		}
		
		// Perform the update. 
		int updateCount = db.update(RememberMeNotSyncedTable.TABLE_REMEMBER_ME_NOT_SYNCED, 
										values, selection, selectionArgs);
				
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
				
		return updateCount;
	}
	
	public static class RememberMeNotSyncedTable {
		
		// Database Table 
		public static final String TABLE_REMEMBER_ME_NOT_SYNCED = "rememberMeNotSyncedTable";
		public static final String COLUMN_REMEMBER_ME_NOT_SYNCED_ID = "_id"; 
		public static final String COLUMN_PROFILE_ID = "profileId"; // foreign key 
		public static final String COLUMN_WORD_ID = "wordId"; // foreign key 
		public static final String COLUMN_TO_DELETE = "toDelete"; // boolean
		// remember Me word even if words are loaded from online web server must be 
		// stored in words table before!
		
		//Database Table creation SQL Statement 
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_REMEMBER_ME_NOT_SYNCED
				+ " ("
				+ COLUMN_REMEMBER_ME_NOT_SYNCED_ID + " integer primary key autoincrement not null, "
				+ COLUMN_PROFILE_ID + " integer not null default 0, " // 0 - Anonymous user 
				+ COLUMN_WORD_ID + " integer not null, "
				+ COLUMN_TO_DELETE + " integer not null, "
				+ " foreign key(" + COLUMN_PROFILE_ID + ") references "
				+ ProfileProvider.ProfileTable.TABLE_PROFILE
				+ "(" + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + ")"
				+ " on update cascade on delete cascade"
				+ " foreign key(" + COLUMN_WORD_ID + ") references "
				+ WordProvider.WordTable.TABLE_WORD
				+ "(" + WordProvider.WordTable.COLUMN_WORD_ID + ")"
				+ " on update cascade on delete cascade"
				+ " unique(" + COLUMN_PROFILE_ID + ", " + COLUMN_WORD_ID + ") "
				+ " on conflict replace "
				+ " );";
		
		// TRIGGERS: 
		// 1) insert trigger on remember_me_not_synced table, checks if corresponding profile exists
		private static final String PROFILE_INSERT_TRIGGER_CREATE = "create trigger fki_"
					+ TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_PROFILE_ID + " "
					+ "before insert on " + TABLE_REMEMBER_ME_NOT_SYNCED + " "
					+ "for each row begin "
							+ "select raise(rollback, 'insert on table " + TABLE_REMEMBER_ME_NOT_SYNCED
											+ " violates foreign key constraint') "
							+ "where new." + COLUMN_PROFILE_ID + "!= 0 AND (select " // 0 - Anonymous user 
									+ ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
									+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
									+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
									+ " = new." + COLUMN_PROFILE_ID + ") is null;"
						+ " end;";
		
		// 2) insert trigger on remember_me_not_synced table, checks if corresponding word exists 
		private static final String WORD_INSERT_TRIGGER_CREATE = "create trigger fki_"
					+ TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_WORD_ID + " "
					+ "before insert on " + TABLE_REMEMBER_ME_NOT_SYNCED + " "
					+ "for each row begin "
							+ "select raise(rollback, 'insert on table " + TABLE_REMEMBER_ME_NOT_SYNCED
											+ " violates foreign key constraint') "
							+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID 
									+ " from " + WordProvider.WordTable.TABLE_WORD
									+ " where " + WordProvider.WordTable.COLUMN_WORD_ID
									+ " = new." + COLUMN_WORD_ID + ") is null;"
					+ " end;";
		
		// 3) update trigger on remember_me_not_synced table, checks if new profile exists
		private static final String PROFILE_UPDATE_TRIGGER_CREATE = "create trigger fku_"
					+ TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_PROFILE_ID + " "
					+ "before update on " + TABLE_REMEMBER_ME_NOT_SYNCED + " "
					+ "for each row begin "
							+ "select raise(rollback, 'update on table " + TABLE_REMEMBER_ME_NOT_SYNCED
											+ " violates foreign key constraint') "
							+ "where new." + COLUMN_PROFILE_ID + "!= 0 AND (select " 
									+ ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
									+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
									+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
									+ " = new." + COLUMN_PROFILE_ID + ") is null;"
						+ " end;";
		
		// 4) update trigger on remember_me_not_synced table, checks if new word exists
		private static final String WORD_UPDATE_TRIGGER_CREATE = "create trigger fku_"
					+ TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_WORD_ID + " "
					+ "before update on " + TABLE_REMEMBER_ME_NOT_SYNCED + " "
					+ "for each row begin "
							+ "select raise(rollback, 'update on table " + TABLE_REMEMBER_ME_NOT_SYNCED
											+ " violates foreign key constraint') "
							+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID
									+ " from " + WordProvider.WordTable.TABLE_WORD
									+ " where " + WordProvider.WordTable.COLUMN_WORD_ID
									+ " = new." + COLUMN_WORD_ID  + ") is null; "
					+ " end;";
		// 5) delete trigger on profile table, cascade deletes corresponding rememeber_me_not_synced words
		private static final String PROFILE_DELETE_TRIGGER_CREATE = "create trigger fkd_"
					+ TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_PROFILE_ID + " "
					+ "before delete on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
					+ "for each row begin "
							+ "delete from " + TABLE_REMEMBER_ME_NOT_SYNCED
								+ " where " + COLUMN_PROFILE_ID
								+ " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + ";"
					+ " end;"; 
		
		// 6) delete trigger on word table, cascade deletes corresponding remember_me_not_synced words
		private static final String WORD_DELETE_TRIGGER_CREATE = "create trigger fkd_"
					+ TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_WORD_ID + " "
					+ "before delete on " + WordProvider.WordTable.TABLE_WORD + " "
					+ "for each row begin "
							+ "delete from " + TABLE_REMEMBER_ME_NOT_SYNCED
								+ " where " + COLUMN_WORD_ID 
								+ " = old." + WordProvider.WordTable.COLUMN_WORD_ID + ";"
					+ " end;";
		
		// 7) update trigger on profile table, cascade updates corresponding remember_me words
		private static final String PROFILE_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
					+ TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_PROFILE_ID + " "
					+ "after update on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
					+ "for each row begin "
							+ "update " + TABLE_REMEMBER_ME_NOT_SYNCED + " set " + COLUMN_PROFILE_ID 
							+ " = new." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " where " + COLUMN_PROFILE_ID + " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ "; "
					+ "end;";
		
		// 8) update trigger on word table, cascade updates corresponding rememeber_me words
		private static final String WORD_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
					+ TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_WORD_ID + " "
					+ "after update on " + WordProvider.WordTable.TABLE_WORD + " "
					+ "for each row begin "
							+ "update " + TABLE_REMEMBER_ME_NOT_SYNCED + " set " + COLUMN_WORD_ID 
							+ " = new." + WordProvider.WordTable.COLUMN_WORD_ID
							+ " where " + COLUMN_WORD_ID + " = old." + WordProvider.WordTable.COLUMN_WORD_ID
							+ "; "
					+ "end;";
		
		// called when no database exists in disk and the SQLiteOpenHelper 
		// class needs to create a new one.
		public static void onCreate(SQLiteDatabase database)
		{
				// RememberMeNotSynced table creation in database (with additional triggers)
				database.execSQL(TABLE_CREATE);
				database.execSQL(PROFILE_INSERT_TRIGGER_CREATE);
				//database.execSQL(WORD_INSERT_TRIGGER_CREATE); 
				database.execSQL(PROFILE_UPDATE_TRIGGER_CREATE);
				//database.execSQL(WORD_UPDATE_TRIGGER_CREATE); 
				database.execSQL(PROFILE_DELETE_TRIGGER_CREATE);
				//database.execSQL(WORD_DELETE_TRIGGER_CREATE); 
				database.execSQL(PROFILE_PARENT_UPDATE_TRIGGER_CREATE);
				//database.execSQL(WORD_PARENT_UPDATE_TRIGGER_CREATE); 
		}
		
		// called when there is a database version mismatch meaning that the version
		// of the database on disk needs to be upgraded to the current version. 
		public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
		{
				// Log the version upgrade 
				Log.w(RememberMeNotSyncedTable.class.getName(), 
						"Upgrading database RememberMeNotSynced table from version " + oldVersion 
						+ " to " + newVersion + ", which will destroy all old data.");
					
				// Upgrading the existing database to conform to the new version.
				// Multiple previous versions can be handled by comparing oldVersion 
				// and newVersion values. 
					
				// Upgrade database by adding new version of RememberMeNotSynced table?
				database.execSQL("DROP TABLE IF EXISTS " + TABLE_REMEMBER_ME_NOT_SYNCED); 
				database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_PROFILE_ID);
				//database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_WORD_ID);
				database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_PROFILE_ID); 
				//database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_WORD_ID); 
				database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_REMEMBER_ME_NOT_SYNCED + "_"+ COLUMN_PROFILE_ID); 
				//database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_WORD_ID); 
				database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_PROFILE_ID);
				//database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_REMEMBER_ME_NOT_SYNCED + "_" + COLUMN_WORD_ID);
				onCreate(database); 
		}
	
	}

}
