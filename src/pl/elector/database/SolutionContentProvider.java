/**
 * @date 17.09.2014
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
public class SolutionContentProvider extends ContentProvider {
	
	// defining ContentProvider's URI address
	private static final String AUTHORITY = "pl.elector.provider.SolutionContentProvider";
	private static final String BASE_PATH = "solution_contents";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH); 
	
	// defining a UriMatcher to differentiate between different URI requests: 
	// for all elements or a single row 
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2;
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending
	// in 'solution_contents' represents a request for all solution content items
	// and 'solution_contents/[rowId]' represents a single row
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW);
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database.
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows and a single row. 
	public static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.solution_contents"; 
	public static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.solution_contents"; 

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single solution content item or all rows (client can specify 
	 * additional selection argument) depending on URI address.
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is a row URI limit the deletion to specified row.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = SolutionContentTable.COLUMN_SOLUTION_CONTENT_ID + "=" + rowID
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
		int deleteCount = db.delete(SolutionContentTable.TABLE_SOLUTION_CONTENT,
									selection, selectionArgs);
		
		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null);
		
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return the correct MIME type, depending on 
	 * the query type: all rows or a single row.
	 */
	@Override
	public synchronized String getType(Uri uri) {
		// For given query's Content URI we return 
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
		// Content Values object you must use to the null column hack 
		// parameter to specify the name of the column that can be set to null. 
		String nullColumnHack = null; 
		
		long id = -1; 
		//checking whether Content URI address is suitable 
		switch(uriMatcher.match(uri))
		{
			case ALLROWS:
				// insert the values into the table 
				id = db.insert(SolutionContentTable.TABLE_SOLUTION_CONTENT,
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
		// effectively defer creating and opening a database 
		// until it's required
		databaseHelper = new DatabaseSQLiteOpenHelper(getContext());
		//returns true if the provider was successfully loaded
		return true;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 * This method enables you to perform queries on the underlying data source
	 * (SQLite database) using ContentProvider. UriMatcher object is used to differentiate queries
	 * for all rows and a single row.
	 * SQLite Query Builder is used as a helper object for performing row-based queries.
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
		
		// Replace this with valid SQL statements if necessary. 
		String groupBy = null; 
		String having = null; 
		
		// Using SQLiteQueryBuilder instead of query() method 
		// in order to simplify database query construction
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder(); 
		// Specifying the table on which to perform query 
		queryBuilder.setTables(SolutionContentTable.TABLE_SOLUTION_CONTENT);
		
		// If this is a single row query add solution content ID to the base query 
		switch( uriMatcher.match(uri))
		{
			case ALLROWS: break; 
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				queryBuilder.appendWhere(SolutionContentTable.TABLE_SOLUTION_CONTENT + "=" + rowID);
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
	public synchronized int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		
		// If this is an update of single row modify selection argument
		// to indicate that row
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = SolutionContentTable.COLUMN_SOLUTION_CONTENT_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
						;
				break;
			default: 
				break; 
		}
		
		// Perform the update.
		int updateCount = db.update(SolutionContentTable.TABLE_SOLUTION_CONTENT,
									values, selection, selectionArgs);
		
		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null);
		
		return updateCount;
	}

	public static class SolutionContentTable {
		
		// Database Table 
		public static final String TABLE_SOLUTION_CONTENT = "solutionContentTable"; 
		public static final String COLUMN_SOLUTION_CONTENT_ID = "_id"; //primary key
		public static final String COLUMN_SOLUTION_CONTENT = "solutionContent"; 
		
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_SOLUTION_CONTENT 
				+ " ("
				+ COLUMN_SOLUTION_CONTENT_ID + " integer not null primary key autoincrement, "
				+ COLUMN_SOLUTION_CONTENT + " text default null"
				+ " );";
		
		// called when no database exists in disk and the SQLiteOpenHelper 
		// class needs to create a new one. 
		public static void onCreate(SQLiteDatabase database) {
			// Solution Content table creation in database 
			database.execSQL(TABLE_CREATE);
		}
		
		// called when there is a database version mismatch meaning that the 
		// version of the database on disk needs to be upgraded to the current version.
		public static void onUpgrade(SQLiteDatabase database, int oldVersion,
									int newVersion)
		{
			// Log the version upgrade 
			Log.w(SolutionContentTable.class.getName(),
					"Upgrading database SolutionContent table from version " + oldVersion
					+ " to " + newVersion + ", which will destroy all old data");
			// Upgrade the existing database to conform to the new version.
			// Multiple previous versions can be handled by comparing oldVersion 
			// and newVersion values. 
			
			// Upgrade database by adding new version of SolutionContent table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_SOLUTION_CONTENT); 
			onCreate(database); 
		}

	}
}
