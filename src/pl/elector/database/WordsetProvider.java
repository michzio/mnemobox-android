/**
 *  @date 10.09.2014
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
public class WordsetProvider extends ContentProvider {
	
	// defining ContentProvider's URI address
	private static final String AUTHORITY = "pl.elector.provider.WordsetProvider"; 
	private static final String BASE_PATH = "wordsets"; 
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH); 

	// defining a UriMatcher to differentiate between different URI requests:
	// for all elements or a single row
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2;
	private static final int ROWS_FOR_CATEGORY = 3; 
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending 
	// in 'wordsets' represents a request for all wordset items
	// and 'wordsets/[rowId]' represents a single row, and 
	// 'wordsets/category/[catId]' represents request for all wordsets 
	// in given category. 
	static { 
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/category/#", ROWS_FOR_CATEGORY);
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database. 
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows (including rows for given category) and a single row
	public static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.wordsets"; 
	public static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.wordsets"; 
	
	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single wordset item or set of wordsets for given 
	 * category id or all rows depending on URI address. 
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is a row URI, limit the deletion to specified row
		// else if this is a category URI, limit the deletion to specified 
		// category rows.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW:
				String rowID = uri.getPathSegments().get(1); 
				selection = WordsetTable.COLUMN_WORDSET_ID + "=" + rowID 
						+ (!TextUtils.isEmpty(selection) ? " AND (" 
					    + selection + ")" : ""); 
				break; 
			case ROWS_FOR_CATEGORY:
				String categoryID = uri.getPathSegments().get(2); 
				selection = WordsetTable.COLUMN_CATEGORY_ID + "=" + categoryID
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
		int deleteCount = db.delete(WordsetTable.TABLE_WORDSET, 
				selection, selectionArgs);
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null);
		
		
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return the correct MIME type, depending
	 * on the query type: set of rows (including all rows) or a single row.
	 */
	@Override
	public synchronized String getType(Uri uri) {
		// For a given query's Content URI we return 
		// suitable MIME type.
		switch(uriMatcher.match(uri))
		{
			case SINGLE_ROW:
				return CONTENT_ITEM_MIME_TYPE;
			case ROWS_FOR_CATEGORY:
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
		//checking whether Content URI address is suitable 
		switch(uriMatcher.match(uri))
		{
			case ALLROWS: 
				// insert the values into the table 
				id = db.insert(WordsetTable.TABLE_WORDSET,
						nullColumnHack, values);
				break; 
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri); 
				
		}
		
		if(id > -1)
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
	 * for all rows, subset of rows for given category and a single row. 
	 * SQLite Query Builder is used as a helper object for performing row-based and category-based queries. 
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
		//Specifying the table on which to perform query
		queryBuilder.setTables(WordsetTable.TABLE_WORDSET); 
		
		// If this is a single row query add wordset ID to the base query 
		// else if this is a query for subset of rows for given category add 
		// category ID to the base query. 
		switch(uriMatcher.match(uri))
		{
			case ALLROWS: break; 
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); //or uri.getLastPathSegment()
				queryBuilder.appendWhere(WordsetTable.COLUMN_WORDSET_ID + "=" + rowID); 
				break; 
			case ROWS_FOR_CATEGORY: 
				String categoryID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(WordsetTable.COLUMN_CATEGORY_ID + "=" + categoryID); 
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
		// for given category modify selection argument to indicate that category.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = WordsetTable.COLUMN_WORDSET_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND (" 
						+ selection + ")" : "");
				break; 
			case ROWS_FOR_CATEGORY: 
				String categoryID = uri.getPathSegments().get(2); 
				selection = WordsetTable.COLUMN_CATEGORY_ID + "=" + categoryID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			default: break; 
		}
		
		// Perform the update.
		int updateCount = db.update(WordsetTable.TABLE_WORDSET,
									values, selection, selectionArgs );
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return updateCount;
	}
	
	public static class WordsetTable {
		
		// Database Table 
		public static final String TABLE_WORDSET = "wordsetTable";
		public static final String COLUMN_WORDSET_ID = "_id"; // primary key
		public static final String COLUMN_CATEGORY_ID = "categoryId"; // foreign key
		public static final String COLUMN_WORDSET_FOREIGN_NAME = "wordsetForeignName"; 
		public static final String COLUMN_WORDSET_NATIVE_NAME = "wordsetNativeName"; 
		public static final String COLUMN_WORDSET_LEVEL = "wordsetLevel"; 
		public static final String COLUMN_WORDSET_ABOUT = "wordsetDescription"; 
		public static final String COLUMN_IS_AUDIO_STORED_LOCALLY = "isAudioStoredLocally"; 
		
		// Database Table creation SQL Statement 
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_WORDSET 
				+ " ("
				+ COLUMN_WORDSET_ID + " integer primary key autoincrement, "
				+ COLUMN_CATEGORY_ID + " integer not null, "
				+ COLUMN_WORDSET_FOREIGN_NAME + " text not null, "
				+ COLUMN_WORDSET_NATIVE_NAME + " text not null, "
				+ COLUMN_WORDSET_LEVEL + " text not null, "
				+ COLUMN_WORDSET_ABOUT + " text not null, "
				+ COLUMN_IS_AUDIO_STORED_LOCALLY + " integer not null default 0, "
				+ " foreign key(" + COLUMN_CATEGORY_ID + ") references " 
				+ WordsetCategoryProvider.WordsetCategoryTable.TABLE_WORDSET_CATEGORY 
				+ "(" + WordsetCategoryProvider.WordsetCategoryTable.COLUMN_ID + ")"
				+ " on update cascade on delete cascade"
				+ " );";
		
		// INSERT TRIGGER enforces parent <-> child table integrity 
		//                you cannot insert new row into child table (wordsetTable)
		//                if corresponding row in parent table (wordsetCategoryTable) doesn't exists 
	    private static final String INSERT_TRIGGER_CREATE = "create trigger fki_" 
				+ TABLE_WORDSET + "_" + COLUMN_CATEGORY_ID + " "
				+ "before insert on " + TABLE_WORDSET + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_WORDSET + " violates foreign key constraint') "
				    + "where (select " + WordsetCategoryProvider.WordsetCategoryTable.COLUMN_ID 
				    		  + " from " + WordsetCategoryProvider.WordsetCategoryTable.TABLE_WORDSET_CATEGORY
				    		  + " where " + WordsetCategoryProvider.WordsetCategoryTable.COLUMN_ID + " = new."
				    		  + COLUMN_CATEGORY_ID + ") is null;"
				+ " end;";
		
		// UPDATE TRIGGER enforces parent <-> child table integrity 
		//                when you update row in child table (wordsetTable) 
		//                its new values should have corresponding row in parent table (wordsetCategoryTable)
		private static final String UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_WORDSET + "_" + COLUMN_CATEGORY_ID + " "
				+ "before update on " + TABLE_WORDSET + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_WORDSET + " violates foreign key constraint') "
					+ "where  (select " + WordsetCategoryProvider.WordsetCategoryTable.COLUMN_ID
							   + " from " + WordsetCategoryProvider.WordsetCategoryTable.TABLE_WORDSET_CATEGORY
							   + " where " + WordsetCategoryProvider.WordsetCategoryTable.COLUMN_ID + " = new."
							   + COLUMN_CATEGORY_ID + ") is null;"
			   + " end;";
		// DELETE TRIGGER performs cascade delete while removing row from parent table
		//                (wordsetCategoryTable), after category deletion all corresponding 
		//                rows in child table (wordsetTable) are also deleted
		private static final String DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_WORDSET + "_" + COLUMN_CATEGORY_ID + " "
				+ "before delete on " + WordsetCategoryProvider.WordsetCategoryTable.TABLE_WORDSET_CATEGORY + " "
				+ "for each row begin "
					+ "delete from " + TABLE_WORDSET + " where "
					+ COLUMN_CATEGORY_ID + " = old." + WordsetCategoryProvider.WordsetCategoryTable.COLUMN_ID + "; "
				+ "end;";
		// PARENT UPDATE TRIGGER performs integrity update on child table (wordsetTable) while 
		//                       user updates corresponding row in parent table (wordsetCategoryTable)
		private static final String PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_WORDSET + "_" + COLUMN_CATEGORY_ID + " "
				+ "after update on " + WordsetCategoryProvider.WordsetCategoryTable.TABLE_WORDSET_CATEGORY + " "
				+ "for each row begin "
					+ "update " + TABLE_WORDSET + " set " + COLUMN_CATEGORY_ID + " = new." 
					+ WordsetCategoryProvider.WordsetCategoryTable.COLUMN_ID 
					+ " where " + COLUMN_CATEGORY_ID + " = old." + WordsetCategoryProvider.WordsetCategoryTable.COLUMN_ID
					+ "; "
				+ "end;";
		
		// called when no database exists in disk and the SQLiteOpenHelper
		// class needs to create a new one. 
		public static void onCreate(SQLiteDatabase database) {
			// Wordset table creation in database 
			database.execSQL(TABLE_CREATE);
			database.execSQL(INSERT_TRIGGER_CREATE); 
			database.execSQL(UPDATE_TRIGGER_CREATE);
			database.execSQL(DELETE_TRIGGER_CREATE);
			database.execSQL(PARENT_UPDATE_TRIGGER_CREATE);
		}
		
		// called when there is a database version mismatch meaning that the version 
		// of the database on disk needs to be upgraded to the current version. 
		public static void onUpgrade(SQLiteDatabase database, int oldVersion,
				int newVersion) 
		{
			// Log the version upgrade 
			Log.w(WordsetTable.class.getName(),
				  "Upgrading database Wordset table from version " + oldVersion + " to "
				  + newVersion + ", which will destroy all old data"); 
			// Upgrade the existing database to conform to the new version. 
			// Multiple previous versions can be handled by comparing oldVersion
			// and newVersion values. 
			
			// Upgrade database by adding new version of Wordset table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_WORDSET); 
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_WORDSET + "_" + COLUMN_CATEGORY_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_WORDSET + "_" + COLUMN_CATEGORY_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_WORDSET + "_" + COLUMN_CATEGORY_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_WORDSET + "_" + COLUMN_CATEGORY_ID); 
			onCreate(database); 
		}
		
		public static String addPrefix(String columnName)
		{
			return TABLE_WORDSET + "." + columnName; 
		}
	}

}
