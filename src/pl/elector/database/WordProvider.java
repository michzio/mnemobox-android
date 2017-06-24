/**
 * @date 11.09.2014
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
public class WordProvider extends ContentProvider {
	
	// defining ContentProvider's URI address 
	private static final String AUTHORITY = "pl.elector.provider.WordProvider"; 
	private static final String BASE_PATH = "words"; 
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + 
				AUTHORITY + "/" + BASE_PATH);
	
	// defining a UriMatcher to differentiate between different URI requests: 
	// for all words or a single word 
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2; 
	private static final int ROWS_FOR_WORDSET = 3; 
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending 
	// in 'words' represents a request for all word items and 
	// 'words/[wordId]' represents a single word (row) request.
	static { 
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW );
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/wordset/#", ROWS_FOR_WORDSET);
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database. 
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows (words) and single row (word)
	public static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.words"; 
	public static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.words"; 
	

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single word item or set of words based on selection argument depending on
	 * uri address. 
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		
		// If this is a row URI, limit the deletion to specified row 
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); // word ID
				selection = WordTable.COLUMN_WORD_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND (" 
						+ selection + ")" : ""); 
				break; 
			case ROWS_FOR_WORDSET: 
				String wordsetID = uri.getPathSegments().get(2); // wordset ID
				selection = WordTable.COLUMN_WORD_ID + " IN ("
						  + " SELECT " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORD_ID
						  + " FROM " + WordsetWordsProvider.WordsetWordsTable.TABLE_WORDSET_WORDS
						  + " WHERE " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORDSET_ID
						  + " = " + wordsetID + ")"
						  + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : ""); 
				break; 
			default: break; 
		} 
		
		// To return the number of deleted items, you must specify
		// a where clause. To delete all rows and return a value, pass in "1". 
		if(selection == null)
			selection = "1"; 
		
		// Execute the deletion. 
		int deleteCount = db.delete(WordTable.TABLE_WORD, selection, selectionArgs);
		
		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return deleteCount; 
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return the correct MIME type, depending 
	 * on the query type: all rows or a single row. 
	 */
	@Override
	public synchronized String getType(Uri uri) {
		
		// For a given query's Content URI we return suitable MIME type.
		switch(uriMatcher.match(uri))
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
		// Open a read/write database by passing in an empty 
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
				id = db.insert(WordTable.TABLE_WORD, nullColumnHack, values);
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
		// creating instance of SQLiteOpenHelper that effectively
		// defer creating and opening a database until it's required
		databaseHelper = new DatabaseSQLiteOpenHelper(getContext()); 
		
		//returns true if the provider was successfully loaded
		return true;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 * This method enables you to perform queries on the underlying data
	 * source (SQLite database) using ContentProvider. UriMatcher object is used
	 * to differentiate queries for all rows and a single row. 
	 * SQLite Query Builder is used as a helper object for performing row-based queries. 
	 */
	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		
		// Open the underlying database
		SQLiteDatabase db; 
		try { 
			db = databaseHelper.getWritableDatabase(); 
		} catch(SQLiteException ex)
		{
			db = databaseHelper.getReadableDatabase(); 
		}
		
		// Replace this with valid SQL statements if necessary. 
		String groupBy = null; 
		String having = null; 
		
		// Using SQLiteQueryBuilder instead of query() method
		// in order to simplify database query construction
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder(); 
		// Specifying the table on which to perform query
		queryBuilder.setTables(WordTable.TABLE_WORD); 
		
		// If this is a single row query add word ID to the base query 
		switch(uriMatcher.match(uri))
		{
			case SINGLE_ROW:
				String rowID = uri.getPathSegments().get(1); //or uri.getLastPathSegment()
				queryBuilder.appendWhere(WordTable.COLUMN_WORD_ID + "=" + rowID); 
				break;
			case ROWS_FOR_WORDSET: 
				String wordsetID = uri.getPathSegments().get(2); // wordset ID
				queryBuilder.appendWhere( WordTable.COLUMN_WORD_ID + " IN ("
						  + " SELECT " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORD_ID
						  + " FROM " + WordsetWordsProvider.WordsetWordsTable.TABLE_WORDSET_WORDS
						  + " WHERE " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORDSET_ID
						  + " = " + wordsetID + " )" ); 
				break;
			case ALLROWS: 
				break;
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder);
		
		//returning the result set Cursor
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
		// to indicate that row.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = WordTable.COLUMN_WORD_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection)? " AND ("
						+ selection + ")" : ""); 
				break; 
			case ROWS_FOR_WORDSET: 
				String wordsetID = uri.getPathSegments().get(2); // wordset ID
				selection = WordTable.COLUMN_WORD_ID + " IN ("
						  + " SELECT " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORD_ID
						  + " FROM " + WordsetWordsProvider.WordsetWordsTable.TABLE_WORDSET_WORDS
						  + " WHERE " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORDSET_ID
						  + " = " + wordsetID + " )"
				+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "");
				break;
			default: 
				break; 
		}
		
		// Perform the update. 
		int updateCount = db.update(WordTable.TABLE_WORD, values, selection, selectionArgs);
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null);
	
		return updateCount;
	}
	
	public static class WordTable {
		
		// Database Table 
		public static final String TABLE_WORD = "wordTable"; 
		public static final String COLUMN_WORD_ID = "_id"; //translation ID 
		public static final String COLUMN_FOREIGN_ARTICLE = "foreignArticle"; 
		public static final String COLUMN_FOREIGN_WORD = "foreignWord"; 
		public static final String COLUMN_NATIVE_ARTICLE = "nativeArticle"; 
		public static final String COLUMN_NATIVE_WORD = "nativeWord"; 
		public static final String COLUMN_TRANSCRIPTION = "transcription"; 
		public static final String COLUMN_RECORDING = "recording";
		public static final String COLUMN_IMAGE = "image";	
		
		// Database Table creation SQL Statement
		private static final String TABLE_CREATE = "create table "
				+ TABLE_WORD 
				+ " ("
				+ COLUMN_WORD_ID + " integer primary key autoincrement, "
				+ COLUMN_FOREIGN_ARTICLE + " text not null default '', "
				+ COLUMN_FOREIGN_WORD + " text not null, "
				+ COLUMN_NATIVE_ARTICLE + " text not null default '', "
				+ COLUMN_NATIVE_WORD + " text not null, "
				+ COLUMN_TRANSCRIPTION + " text not null, "
				+ COLUMN_RECORDING + " text default null, "
				+ COLUMN_IMAGE + " blob default null)"; 
		
		// called when no database exists in disk and the SQLiteOpenHelper
		// class needs to create a new one. 
		public static void onCreate(SQLiteDatabase database)
		{
			//Word table creation in database.
			database.execSQL(TABLE_CREATE); 
		}
		
		// called when there is a database version mismatch meaning that 
		// the version of the database on disk needs to be upgraded 
		// to the current version.
		public static void onUpgrade(SQLiteDatabase database, 
									int oldVersion, int newVersion) 
		{
			// Log the version upgrade 
			Log.w(WordTable.class.getName(), 
				  "Upgrading database Word table from version " + oldVersion 
				  + " to " + newVersion + ", which will destroy all old data");
			// Upgrade the existing database to conform to the new version. 
			// Multiple previous versions can be handled by comparing oldVersion
			// and newVersion values. 
			
			// Upgrade database by adding new version of Wordset tabel?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_WORD);
			onCreate(database); 
			
		}
		
		public static String addPrefix(String columnName)
		{
			return TABLE_WORD + "." + columnName; 
		}
	}

}
