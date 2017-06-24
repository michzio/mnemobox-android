/**
 * @date 07.10.2014
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
public class UserWordsetProvider extends ContentProvider {
	
	// defining ContentProvider's URI address 
	private static final String AUTHORITY = "pl.elector.provider.UserWordsetProvider"; 
	private static final String BASE_PATH = "user_wordsets"; 
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	
	// defining a UriMatcher to differentiate between different URI requests: 
	// for all elements or a single row 
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2; 
	private static final int ROWS_FOR_USER = 3; 
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending 
	// in 'user_wordsets' represents a request for all user_wordset items
	// and 'user_wordsets/[rowId]' represents a single row, 
	// and 'user_wordsets/user/[userId]' represents request for all user_wordsets for given user (profile id)
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/user/#", ROWS_FOR_USER);
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database. 
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows (including rows for given user) 
	// and a single row 
	public static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.user_wordsets"; 
	public static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.user_wordsets";

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single user_wordset item or set of user_wordsets 
	 * for given user id or all rows depending on URI address. 
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is a row URI, limit the deletion to specified row
		// else if this is a user URI, limit the deletion to specified rows 
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = UserWordsetTable.COLUMN_USER_WORDSET_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			case ROWS_FOR_USER: 
				String userID = uri.getPathSegments().get(2); 
				selection = UserWordsetTable.COLUMN_USER_ID + "=" + userID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			default: 
				break; 
		}
		
		// To return the number of deleted items, you must specify 
		// a where clause. To delete all rows and return a value, pass in "1". 
		if(selection == null)
			selection = "1"; 
		
		// Execute the deletion.
		int deleteCount = db.delete(UserWordsetTable.TABLE_USER_WORDSET, 
									selection, selectionArgs);
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return the correct MIME type, 
	 * depending on the query type: set of rows (including all rows) or a single row. 
	 */
	@Override
	public synchronized String getType(Uri uri) {
		
		// For a given query's Content URI we return 
		// suitable MIME type. 
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				return CONTENT_ITEM_MIME_TYPE; 
			case ROWS_FOR_USER: 
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
				id = db.insert(UserWordsetTable.TABLE_USER_WORDSET, 
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
		// effectively defer creation and opening of database. 
		databaseHelper = new DatabaseSQLiteOpenHelper(getContext()); 
		// returns true if the provider was successfully loaded
		return true;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 * This method enables you to perform queries on the underlying data source
	 * (SQLite database) using ContentProvider. UriMatcher object is used to 
	 * differentiate queries for all rows, subset of rows for given user and
	 * a single row. SQLite Query Builder is used as a helper object for performing
	 * row-based and user-based queries. 
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
		queryBuilder.setTables(UserWordsetTable.TABLE_USER_WORDSET); 
		
		// If this is a single row query append user_wordset ID to the base query 
		// else if this is a query for subset of rows for given user append 
		// user ID to the base query. 
		switch( uriMatcher.match(uri))
		{
			case ALLROWS: break; 
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); //or uri.getLastPathSegment()
				queryBuilder.appendWhere(UserWordsetTable.COLUMN_USER_WORDSET_ID + "=" + rowID);
				break;
			case ROWS_FOR_USER: 
				String userID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(UserWordsetTable.COLUMN_USER_ID + "=" + userID);
				break; 
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder);
		
		// returning the result set Cursor
		return cursor;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		
		// If this is an update of single row, modify selection argument 
		// to indicate that row, else if this is an update of a set of rows 
		// for given user id modify selection argument to indicate that user.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = UserWordsetTable.COLUMN_USER_WORDSET_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			case ROWS_FOR_USER:
				String userID = uri.getPathSegments().get(2); 
				selection = UserWordsetTable.COLUMN_USER_ID + "=" + userID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			default: break; 
		}
		
		// Perform the update. 
		int updateCount = db.update(UserWordsetTable.TABLE_USER_WORDSET,
									values, selection, selectionArgs);
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null);
		
		return updateCount;
	}
	
	public static class UserWordsetTable {
		
		// Database Table 
		public static final String TABLE_USER_WORDSET = "userWordsetTable"; 
		public static final String COLUMN_USER_WORDSET_ID = "_id"; // primary key
		public static final String COLUMN_USER_ID = "userId"; // foreign key -> profileId
		public static final String COLUMN_WORDSET_FOREIGN_NAME = "wordsetForeignName"; 
		public static final String COLUMN_WORDSET_NATIVE_NAME = "wordsetNativeName";
		public static final String COLUMN_WORDSET_ABOUT = "wordsetDescription"; 
		
		// Database Table creation SQL Statement
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_USER_WORDSET
				+ " ("
				+ COLUMN_USER_WORDSET_ID + " integer primary key autoincrement, "
				+ COLUMN_USER_ID + " integer not null, "
				+ COLUMN_WORDSET_FOREIGN_NAME + " text not null, "
				+ COLUMN_WORDSET_NATIVE_NAME + " text not null, "
				+ COLUMN_WORDSET_ABOUT + " text not null, "
				+ " foreign key(" + COLUMN_USER_ID + ") references "
				+ ProfileProvider.ProfileTable.TABLE_PROFILE
				+ "(" + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + ")"
				+ " on update cascade on delete cascade"
				+ " );";
		
		// TRIGGERS: 
		// 1) insert trigger on user_wordset table, checks if corresponding user (profile) exists
		private static final String INSERT_TRIGGER_CREATE = "create trigger fki_" 
				+ TABLE_USER_WORDSET + "_" + COLUMN_USER_ID + " "
				+ "before insert on " + TABLE_USER_WORDSET + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_USER_WORDSET 
								+ " violates foreign key constraint') "
					+ "where (select " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
							+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " = new." + COLUMN_USER_ID + ") is null;"
				+ " end;"; 
				
		// 2) update trigger on user_wordset table, checks if new user (profile) exists
		private static final String UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_USER_WORDSET + "_" + COLUMN_USER_ID + " "
				+ "before update on " + TABLE_USER_WORDSET + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_USER_WORDSET
								+ " violates foreign key constraint') "
					+ "where (select " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
							+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " = new." + COLUMN_USER_ID + ") is null;"
				+ "end;";
		
		// 3) delete trigger on profile table, cascade deletes corresponding user_wordset rows
		private static final String DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_USER_WORDSET + "_" + COLUMN_USER_ID + " "
				+ "before delete on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin "
					+ "delete from " + TABLE_USER_WORDSET 
					+ " where " + COLUMN_USER_ID 
					+ " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + "; "
				+ "end;" ;
		
		// 4) update trigger on profile table, cascade updates corresponding user_wordset rows
		private static final String PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_USER_WORDSET + "_" + COLUMN_USER_ID + " "
				+ "after update on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin " 
					+ "update " + TABLE_USER_WORDSET + " set " + COLUMN_USER_ID 
						+ " = new." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
						+ " where " + COLUMN_USER_ID 
						+ " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
						+ "; "
				+ "end;";
		
		// called when no database exists in disk and the SQLiteOpenHelper 
		// class needs to create a new one. 
		public static void onCreate(SQLiteDatabase database) {
			// UserWordset table creation in database 
			database.execSQL(TABLE_CREATE);
			database.execSQL(INSERT_TRIGGER_CREATE);
			database.execSQL(UPDATE_TRIGGER_CREATE);
			database.execSQL(DELETE_TRIGGER_CREATE);
			database.execSQL(PARENT_UPDATE_TRIGGER_CREATE);
		}
		
		// called when there is a database version mismatch meaning that 
		// the version of the database on disk needs to be upgraded to the current 
		// version. 
		public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
		{
			// Log the version upgrade 
			Log.w(UserWordsetTable.class.getName(),
					"Upgrading database UserWordset table from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			// Upgrade the existing database to conform to the new version.
			// Multiple previous versions can be handled by comparing oldVersion
			// and newVersion values. 
			
			// Upgrade database by adding new version of UserWordset table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_WORDSET); 
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_USER_WORDSET + "_" + COLUMN_USER_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_USER_WORDSET + "_" + COLUMN_USER_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_USER_WORDSET + "_" + COLUMN_USER_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_USER_WORDSET + "_" + COLUMN_USER_ID); 
			onCreate(database); 
		}
		public static String addPrefix(String columnName)
		{
			return TABLE_USER_WORDSET + "." + columnName; 
		}
		
	}

}
