/**
 * @date 15.09.2014
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
 */
public class ProfileProvider extends ContentProvider {
	
	// defining ContentProvider's URI address 
	private static final String AUTHORITY = "pl.elector.provider.ProfileProvider"; 
	private static final String BASE_PATH = "profiles";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH); 
	
	// defining a UriMatcher to differentiate between different URI requests:
	// for all elements or a single row
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2; 
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where  an URI ending
	// in 'profiles' represents a request for all profile items 
	// and 'profiles/[rowId]' represents a single row.
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW);
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database.
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows and a single row
	public static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.profiles"; 
	public static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.profiles"; 
	

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single profile item or all profiles (additional selection argument) 
	 * depending on URI address.
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction 
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		
		// If this is a row URI, limit the deletion to specified row
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW:
				String rowID = uri.getPathSegments().get(1); 
				selection = ProfileTable.COLUMN_PROFILE_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			default: break; 
		}
		
		// To return the number of deleted items, you must specify 
		// a where clause. To delete all rows and return a value, pass in "1".
		if(selection == null)
			selection = "1"; 
		
		// Execute the deletion. 
		int deleteCount = db.delete(ProfileTable.TABLE_PROFILE, selection, selectionArgs);
		
		// notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null);
		
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return the correct MIME type depending on 
	 * the query type: all rows or a single row.
	 */
	@Override
	public synchronized String getType(Uri uri) {
		// For a given query's Content URI we return 
		// suitable MIME type.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				return CONTENT_ITEM_MIME_TYPE; 
			case ALLROWS: 
				return CONTENT_MIME_TYPE; 
			default: 
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
	 * Transaction method used to insert a new row into database (represented by ContentValues)
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
		switch(uriMatcher.match(uri))
		{
			case ALLROWS: 
				// insert the values into the table 
				id = db.insert(ProfileTable.TABLE_PROFILE, 
								nullColumnHack, values);
				break; 
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		if( id > -1) 
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
		// effectively defer creating and opening a database 
		// until it's required
		databaseHelper = new DatabaseSQLiteOpenHelper(getContext());
		// returns true if the provider was successfully loaded		
		return true;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 * This method enables you to perform queries on the underlying data source 
	 * (SQLite database) using ContentProvider. UriMatcher object is used to differentiate queries
	 * for all rows and a single row. SQLite Query Builder is used as a helper object 
	 * for performing row-based queries. 
	 */
	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		
		// Open the underlying database
		SQLiteDatabase db; 
		try { 
			db = databaseHelper.getWritableDatabase();
		} catch(SQLiteException ex) {
			db = databaseHelper.getReadableDatabase(); 
		}
		
		// Replace this with valid SQL statements if necessary. 
		String groupBy = null; 
		String having = null; 
		
		// Using SQLiteQueryBuilder instead of query() method 
		// in order to simplify database query construction 
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder(); 
		// Specifying the table on which to perform query 
		queryBuilder.setTables(ProfileTable.TABLE_PROFILE); 
		
		// If this is a single row query add profile ID to the base query 
		switch( uriMatcher.match(uri))
		{
			case ALLROWS: break; 
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				queryBuilder.appendWhere(ProfileTable.COLUMN_PROFILE_ID + "=" + rowID);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder);
		
		return cursor;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is an update of single row modify selection argument 
		// to indicate that row, 
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = ProfileTable.COLUMN_PROFILE_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			default: break; 
		}
		
		// Perform the update.
		int updateCount = db.update(ProfileTable.TABLE_PROFILE,
									values, selection, selectionArgs);
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null);
		
		return updateCount;
	}
	
	
	public static class ProfileTable { 
		
		// Database Table
		public static final String TABLE_PROFILE = "profileTable"; 
		public static final String COLUMN_PROFILE_ID = "_id";
		public static final String COLUMN_FIRST_NAME = "firstName"; 
		public static final String COLUMN_LAST_NAME = "lastName";
		public static final String COLUMN_EMAIL = "email"; 
		public static final String COLUMN_SHA1PASS = "sha1Pass"; 
		public static final String COLUMN_LANG = "langDirection"; 
		public static final String COLUMN_LAST_WORDSET_ID = "lastWordsetId"; 
		public static final String COLUMN_IMAGE_PATH = "imagePath"; 
		public static final String COLUMN_IMAGE = "image"; 
		public static final String COLUMN_CITY = "city"; 
		public static final String COLUMN_AGE = "age"; 
		public static final String COLUMN_SKYPE = "skype";
		public static final String COLUMN_PHONE = "phone"; 
		public static final String COLUMN_LAST_WORDSET_NAME = "lastWordsetName"; 
		
		// Database Table creation SQL Statement
		private static final String TABLE_CREATE = "create table "
				+ TABLE_PROFILE
				+ " ("
				+ COLUMN_PROFILE_ID + " integer primary key autoincrement, "
				+ COLUMN_FIRST_NAME + " text not null default '', "
				+ COLUMN_LAST_NAME + " text not null default '', "
				+ COLUMN_EMAIL + " text not null, "
				+ COLUMN_SHA1PASS + " text not null, "
				+ COLUMN_LANG + " text not null, "
				+ COLUMN_LAST_WORDSET_ID + " integer not null default 0, "
				+ COLUMN_LAST_WORDSET_NAME + " text not null default '', "
				+ COLUMN_IMAGE_PATH + " text not null default '', "
				+ COLUMN_IMAGE + " blob default null, "
				+ COLUMN_CITY + " text not null default '', " 
				+ COLUMN_AGE + " integer not null default 0, "
				+ COLUMN_SKYPE + " text not null default '', " 
				+ COLUMN_PHONE + " text not null default '' "
				+ ");";
				
				/* DEPRECATED user can learn wordsets not saved locally on phone
				+ " foreign key (" + COLUMN_LAST_WORDSET_ID + ") references "
				+ WordsetProvider.WordsetTable.TABLE_WORDSET 
				+ "(" + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID + ")"
				+ " on update cascade on delete set default "
				+ ");";
				*/
		
		// TRIGGERS: 
		// 1) insert trigger on profile table, checking wordset exists
		private static final String INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_PROFILE + "_" + COLUMN_LAST_WORDSET_ID + " "
				+ "before insert on " + TABLE_PROFILE + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_PROFILE
									+ " violates foreign key constraint') "
					+ "where new." + COLUMN_LAST_WORDSET_ID + " != 0 AND " 
							  + "(select " + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID 
							  + " from " + WordsetProvider.WordsetTable.TABLE_WORDSET
							  + " where " + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID
							  + " = new." + COLUMN_LAST_WORDSET_ID + ") is null;"
				+ " end;";
				
		// 2) update trigger on profile table, checking new wordset exists
		private static final String UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_PROFILE + "_" + COLUMN_LAST_WORDSET_ID + " "
				+ "before update on " + TABLE_PROFILE + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_PROFILE 
									+ " violates foreign key constraint') "
					+ "where new." + COLUMN_LAST_WORDSET_ID + " != 0 AND "
							  + "(select " + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID
							  + " from " + WordsetProvider.WordsetTable.TABLE_WORDSET
							  + " where " + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID
							  + " = new. " + COLUMN_LAST_WORDSET_ID + ") is null;"
				+ " end;";
				
		// 3) delete trigger on wordset table, setting default value in corresponding profiles
		private static final String DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_PROFILE + "_" + COLUMN_LAST_WORDSET_ID + " "
				+ "before delete on " + WordsetProvider.WordsetTable.TABLE_WORDSET + " "
				+ "for each row begin "
					+ "update " + TABLE_PROFILE + " set " + COLUMN_LAST_WORDSET_ID + " = 0 "
					+ "where " + COLUMN_LAST_WORDSET_ID + " = old." + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID 
					+ "; "
				+ "end;";
				
		// 4) update trigger on wordset table, cascade update on profile table 
		private static final String PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_PROFILE + "_" + COLUMN_LAST_WORDSET_ID + " "
				+ "after update on " + WordsetProvider.WordsetTable.TABLE_WORDSET + " "
				+ "for each row begin "
					+ "update " + TABLE_PROFILE + " set " + COLUMN_LAST_WORDSET_ID + " = new."
					+ WordsetProvider.WordsetTable.COLUMN_WORDSET_ID 
					+ " where " + COLUMN_LAST_WORDSET_ID + " = old." + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID
					+ "; "
				+ "end;";
		
		// called when no database exists in disk and the SQLiteOpenHelper
		// class needs to create a new one. 
		public static void onCreate(SQLiteDatabase database)
		{
			//Profile table creation in database 
			database.execSQL(TABLE_CREATE);
			/* DEPRECATED: user can learn wordsets not saved locally on the phone 
			 *	database.execSQL(INSERT_TRIGGER_CREATE);
			 *  database.execSQL(UPDATE_TRIGGER_CREATE);
			 *	database.execSQL(DELETE_TRIGGER_CREATE);
			 *	database.execSQL(PARENT_UPDATE_TRIGGER_CREATE);
			 */
		}
		
		// called when there is a database version mismatch meaning that the version
		// of the database on disk needs to be upgraded to the current version.
		public static void onUpgrade(SQLiteDatabase database, int oldVersion, 
									 int newVersion)
		{
			// Log the version upgrade 
			Log.w(ProfileTable.class.getName(),
					"Upgrading database Profile table from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			// Upgrade the existing database to conform to the new version. 
			// Multiple previous versions can be handled by comparing oldVersion 
			// and newVersion values. 
			
			// Upgrade database by adding new version of Profile table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILE); 
			/* DEPRECATED: user can learn wordsets not saved locally on the phone 
			 *	database.execSQL("DROP TRIGGER IF EXISTS fki_"+ TABLE_PROFILE + "_" + COLUMN_LAST_WORDSET_ID);
			 *	database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_PROFILE + "_" + COLUMN_LAST_WORDSET_ID); 
			 *	database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_PROFILE + "_" + COLUMN_LAST_WORDSET_ID); 
			 *	database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_PROFILE + "_" + COLUMN_LAST_WORDSET_ID); 
			 */
			onCreate(database); 
		}
		
	}

}
