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
public class SentenceProvider extends ContentProvider {

	// defining ContentProvider's URI address
	private static final String AUTHORITY = "pl.elector.provider.SentenceProvider"; 
	private static final String BASE_PATH = "sentences"; 
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH); 
	
	// defining a UriMatcher to differentiate between different URI requests:
	// for all elements, subset of sentences for given word ID and a single row. 
	private static final int ALLROWS = 1;
	private static final int SINGLE_ROW = 2; 
	private static final int ROWS_FOR_WORD = 3;
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending
	// in 'sentences' represents a request for all sentence items
	// and 'sentences/[rowId]' represents a single row, and 
	// 'sentences/word/[wordId]' represents request for all 
	// sentences for given word.
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/word/#", ROWS_FOR_WORD); 
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database.
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows (including rows for given word)
	// and a single row
	public static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.words"; 
	public static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.words"; 
	
	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single sentence item or set of sentences for given 
	 * word id or all rows depending on URI address.
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is a row URI, limit the deletion to specified row
		// else if this is a word URI, limit the deletion to specified
		// word rows. 
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW:
				String rowID = uri.getPathSegments().get(1); 
				selection = SentenceTable.COLUMN_SENTENCE_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			case ROWS_FOR_WORD:
				String wordID = uri.getPathSegments().get(2);
				selection = SentenceTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND (" 
						+ selection + ")" : ""); 
				break; 
			default:
				break; 
		}
		
		// To return the number of deleted items, you must specify
		// a where clause. To delete all rows and return a value,
		// pass in "1". 
		if(selection == null)
			selection = "1"; 
		
		// Execute the deletion. 
		int deleteCount = db.delete(SentenceTable.TABLE_SENTENCE,
									selection, selectionArgs);
		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return the correct MIME type, depending 
	 * on the query type: set of rows (including all raws) or a single row.
	 */
	@Override
	public synchronized String getType(Uri uri) {
		// For a given query's Content URI we return 
		// suitable MIME type. 
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW:
				return CONTENT_ITEM_MIME_TYPE; 
			case ROWS_FOR_WORD:
			case ALLROWS: 
				return CONTENT_MIME_TYPE; 
			default: 
				throw new IllegalArgumentException("Unsupported URI:" + uri); 
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
		switch( uriMatcher.match(uri))
		{
			case ALLROWS: 
				//insert the values into the table 
				id = db.insert(SentenceTable.TABLE_SENTENCE, 
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
		databaseHelper = new DatabaseSQLiteOpenHelper( getContext()); 
		//returns true if the provider was successfully loaded
		return true;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 * This methods enables you to perform queries on the underlying data source 
	 * (SQLite database) using ContentProvider. UriMatcher object is used to differentiate queries 
	 * for all rows, subset of rows for given word and a single row. 
	 * SQLite Query Builder is used as a helper object for performing row-based and word-based queries. 
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
		// Specify the table on which to perform query 
		queryBuilder.setTables(SentenceTable.TABLE_SENTENCE); 
		
		// If this is a single row query add sentence ID to the base query 
		// else if this is a query for subset of rows for given word add 
		// word ID to the base query 
		switch( uriMatcher.match(uri))
		{
			case ALLROWS: break; 
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); // or uri.getLastPathSegment()
				queryBuilder.appendWhere(SentenceTable.COLUMN_SENTENCE_ID + "=" + rowID); 
				break; 
			case ROWS_FOR_WORD: 
				String wordID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(SentenceTable.COLUMN_WORD_ID + "=" + wordID); 
				break; 
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		Cursor cursor = queryBuilder.query(db, projection,
				selection, selectionArgs, groupBy, having, sortOrder);
		
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
		// for given word modify selection argument to indicate that word.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
			    String rowID = uri.getPathSegments().get(1); 
			    selection = SentenceTable.COLUMN_SENTENCE_ID + "=" + rowID
			    		+ (!TextUtils.isEmpty(selection) ? " AND ("
			    		+ selection + ")" : "");
				break; 
			case ROWS_FOR_WORD: 
				String wordID = uri.getPathSegments().get(2); 
				selection = SentenceTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			default: break; 
		}
		
		// Perform the update. 
		int updateCount = db.update(SentenceTable.TABLE_SENTENCE,
									values, selection, selectionArgs);
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return updateCount;
	}
	
	public static class SentenceTable {
		
		// Database Table 
		public static final String TABLE_SENTENCE = "sentenceTable"; 
		public static final String COLUMN_SENTENCE_ID = "_id";
		public static final String COLUMN_WORD_ID = "wordId"; 
		public static final String COLUMN_FOREIGN_SENTENCE = "foreignSentence"; 
		public static final String COLUMN_NATIVE_SENTENCE = "nativeSentence"; 
		public static final String COLUMN_RECORDING = "recording"; 
		
		// Database Table creation SQL Statement
		private static final String TABLE_CREATE = "create table "
				+ TABLE_SENTENCE 
				+ " ("
				+ COLUMN_SENTENCE_ID + " integer primary key autoincrement, "
				+ COLUMN_WORD_ID + " integer not null, "
				+ COLUMN_FOREIGN_SENTENCE + " text not null, "
				+ COLUMN_NATIVE_SENTENCE + " text not null default '', "
				+ COLUMN_RECORDING + " text default null, "
				+ " foreign key(" + COLUMN_WORD_ID + ") references "
				+ WordProvider.WordTable.TABLE_WORD 
				+ "(" + WordProvider.WordTable.COLUMN_WORD_ID + ")"
				+ " on update cascade on delete cascade"
				+ " )"; 
		
		// TRIGGERS:
		// 1) insert trigger on sentence table, checking word exists 
		private static final String INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_SENTENCE + "_" + COLUMN_WORD_ID
				+ "before insert on " + TABLE_SENTENCE + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_SENTENCE 
									+ " violates foreign key constraint') "
					+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID
							  + " from " + WordProvider.WordTable.TABLE_WORD
							  + " where " + WordProvider.WordTable.COLUMN_WORD_ID 
							  + " = new." + COLUMN_WORD_ID + ") is null;"
			    + " end;"; 
				;
		// 2) update trigger on sentence table, checking word exists
		private static final String UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_SENTENCE + "_" + COLUMN_WORD_ID + " "
				+ "before update on " + TABLE_SENTENCE + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_SENTENCE
									+ " violates foreign key constraint') "
					+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID
							  + " from " + WordProvider.WordTable.TABLE_WORD 
							  + " where " + WordProvider.WordTable.COLUMN_WORD_ID
							  + " = new." + COLUMN_WORD_ID + ") is null;"
				+ " end;";
		
		// 3) delete trigger on word table, cascade delete rows in sentence table
		private static final String DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_SENTENCE + "_" + COLUMN_WORD_ID + " "
				+ "before delete on " + WordProvider.WordTable.TABLE_WORD + " "
				+ "for each row begin "
					+ "delete from " + TABLE_SENTENCE + " where "
					+ COLUMN_WORD_ID + " = old." + WordProvider.WordTable.COLUMN_WORD_ID + "; "
			    + "end;";
		
		// 4) update trigger on word table, cascade update rows in sentence table
		private static final String PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_SENTENCE + "_" + COLUMN_WORD_ID + " "
				+ "after update on " + WordProvider.WordTable.TABLE_WORD + " "
				+ "for each row begin "
					+ "update " + TABLE_SENTENCE + " set " + COLUMN_WORD_ID 
					  + " = new." + WordProvider.WordTable.COLUMN_WORD_ID
					  + " where " + COLUMN_WORD_ID + " = old." + WordProvider.WordTable.COLUMN_WORD_ID
					  + "; "
				+ "end;"; 
		
		// called when no database exists in disk and the SQLiteOpenHelper
		// class needs to create a new one. 
		public static void onCreate(SQLiteDatabase database) {
			// Sentence table creation in database 
			database.execSQL(TABLE_CREATE);
			database.execSQL(INSERT_TRIGGER_CREATE); 
			database.execSQL(UPDATE_TRIGGER_CREATE);
			database.execSQL(DELETE_TRIGGER_CREATE);
			database.execSQL(PARENT_UPDATE_TRIGGER_CREATE);
		}
		
		// called when there is a database version mismatch meaning that the 
		// version of the database on disk needs to be upgraded to the current
		// version. 
		public static void onUpgrade(SQLiteDatabase database,
									int oldVersion, int newVersion)
		{
			// Log the version upgrade 
			Log.w(SentenceTable.class.getName(), 
					"Upgrading database Sentence table from version " + oldVersion + " to " 
					+ newVersion + ", which will destroy all old data"); 
			// Upgrade the existing database to conform to the new version.
			// Multiple previous versions can be handled by comparing oldVersion
			// and newVersion values. 
			
			// Upgrade database by adding new version of Sentence table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_SENTENCE); 
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_SENTENCE + "_" + COLUMN_WORD_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_SENTENCE + "_" + COLUMN_WORD_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_SENTENCE + "_" + COLUMN_WORD_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_SENTENCE + "_" + COLUMN_WORD_ID); 
			onCreate(database); 
		}
			
	}

}
