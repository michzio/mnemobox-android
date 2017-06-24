/**
 * @date 23.09.2014
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
public class PostItProvider extends ContentProvider {
	
	// defining ContentProvider's URI address 
	private static final String AUTHORITY = "pl.elector.provider.PostItProvider";
	private static final String BASE_PATH = "postits";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	
	// defining a UriMatcher to differentiate between different URI requests: 
	// for all elements, subset of rows for given word or author
	// and a single row.
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2; 
	private static final int ROWS_FOR_WORD = 3; 
	private static final int ROWS_FOR_AUTHOR = 4; 
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending 
	// in 'postits' represents a request for all postit items
	// and 'postits/[rowId]' represents a single row,
	// and 'postits/[wordId]' represents request for all postits for given word
	// and 'postits/[authorId]' represents request for all postits for given author
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/word/#", ROWS_FOR_WORD); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/author/#", ROWS_FOR_AUTHOR);
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database.
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows (including rows for given word or author)
	// and a single row
	private static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.postits";
	private static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.postits";

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single postIt item or set of postIts for given word or author
	 * or all rows depending on URI address.
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is a row URI limit the deletion to specified row
		// else if this is a word URI limit the deletion to specified word rows
		// else if this is a author URI limit the deletion to specified author rows
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1);
				selection = PostItTable.COLUMN_POST_IT_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			case ROWS_FOR_WORD: 
				String wordID = uri.getPathSegments().get(2);
				selection = PostItTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			case ROWS_FOR_AUTHOR: 
				String authorID = uri.getPathSegments().get(2);
				selection = PostItTable.COLUMN_AUTHOR_ID + "=" + authorID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			default:
				break;
		}
		
		// To return the number of deleted items, you must specify 
		// a where clause. To delete all rows and return a value, pass in "1"
		if(selection == null)
			selection = "1"; 
		
		// Execute the deletion
		int deleteCount = db.delete(PostItTable.TABLE_POST_IT, selection, selectionArgs);
		
		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return correct MIME type, depending on 
	 * the query type: set of rows (including all rows) or a single row.
	 */
	@Override
	public synchronized String getType(Uri uri) {
		// For given query's Content URI we return suitable MIME type.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				return CONTENT_ITEM_MIME_TYPE; 
			case ROWS_FOR_WORD:
			case ROWS_FOR_AUTHOR: 
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
		// ContentValues object, you must use the null column hack 
		// parameter to specify the name of the column that can be set to null. 
		String nullColumnHack = null; 
		
		long id = -1; 
		// checking whether Content URI address is suitable 
		switch( uriMatcher.match(uri))
		{
			case ALLROWS:
				// insert the values into the table
				id = db.insert(PostItTable.TABLE_POST_IT, nullColumnHack, values);
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
		// effectively defer creating and opening database 
		// until it's required
		databaseHelper = new DatabaseSQLiteOpenHelper(getContext()); 
		// returns true if the provider was successfully loaded
		return true; 
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 * This method enables you to perform queries on the underlying data source 
	 * (SQLite database) using ContentProvider. UriMatcher object is used to differentiate 
	 * queries for all rows, subset of rows for given word or author and a single row. 
	 * SQLite Query Builder is used as a helper object for performing row-based, 
	 * word-based and author-based queries. 
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
		queryBuilder.setTables(PostItTable.TABLE_POST_IT); 
		
		// If this is a single row query add postIt id to the base query 
		// else if this is a query for subset of rows for given word add word id to the base query 
		// else if this is a query for subset of rows for given author add author id to the base query 
		switch( uriMatcher.match(uri))
		{
			case ALLROWS: break; 
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				queryBuilder.appendWhere(PostItTable.COLUMN_POST_IT_ID + "=" + rowID);
				break;
			case ROWS_FOR_WORD:
				String wordID = uri.getPathSegments().get(2);
				queryBuilder.appendWhere(PostItTable.COLUMN_WORD_ID + "=" + wordID); 
				break; 
			case ROWS_FOR_AUTHOR: 
				String authorID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(PostItTable.COLUMN_AUTHOR_ID + "=" + authorID); 
				break; 
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder);
		
		cursor.setNotificationUri(getContext().getContentResolver(), CONTENT_URI);
		
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
		
		// If this is an update of single row modify selection argument to indicate 
		// that row, else if this is an update of a set of rows for given word or
		// author modify selection argument to indicate that rows. 
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = PostItTable.COLUMN_POST_IT_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			case ROWS_FOR_WORD:
				String wordID = uri.getPathSegments().get(2); 
				selection = PostItTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break;
			case ROWS_FOR_AUTHOR: 
				String authorID = uri.getPathSegments().get(2); 
				selection = PostItTable.COLUMN_AUTHOR_ID + "=" + authorID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			default: break;
		}
		
		// Perform the update.
		int updateCount = db.update(PostItTable.TABLE_POST_IT, values, selection, selectionArgs);
		
		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null);
		
		return updateCount;
	}
	
	public static class PostItTable {
		
		// Database Table 
		public static final String TABLE_POST_IT = "postItTable"; 
		public static final String COLUMN_POST_IT_ID = "_id"; 
		public static final String COLUMN_AUTHOR_ID = "authorId"; 
		public static final String COLUMN_AUTHOR_FIRST_NAME = "authorFirstName";
		public static final String COLUMN_AUTHOR_LAST_NAME = "authorLastName";
		public static final String COLUMN_WORD_ID = "wordId"; 
		public static final String COLUMN_TEXT = "postText"; 
		public static final String COLUMN_LANG = "langDirection"; 
		
		// Database Table creation SQL Statement
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_POST_IT 
				+ " ("
				+ COLUMN_POST_IT_ID + " integer primary key autoincrement not null, "
				+ COLUMN_AUTHOR_ID + " integer not null, "
				+ COLUMN_AUTHOR_FIRST_NAME + " text not null, "
				+ COLUMN_AUTHOR_LAST_NAME + " text not null, "
				+ COLUMN_WORD_ID + " integer not null, "
				+ COLUMN_TEXT + " text not null, "
				+ COLUMN_LANG + " text not null, "
				+ " foreign key (" + COLUMN_AUTHOR_ID + ") references "
				+ ProfileProvider.ProfileTable.TABLE_PROFILE
				+ "(" + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + ")"
				+ " on update cascade on delete cascade "
				+ " foreign key (" + COLUMN_WORD_ID + ") references "
				+ WordProvider.WordTable.TABLE_WORD
				+ "(" + WordProvider.WordTable.COLUMN_WORD_ID + ")"
				+ " on update cascade on delete cascade "
				+ ");";
		
		// TRIGGERS: 
		// 1) insert trigger on postIt table, checking profile exists 
		private static final String AUTHOR_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_POST_IT + "_" + COLUMN_AUTHOR_ID + " "
				+ "before insert on " + TABLE_POST_IT + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_POST_IT
									+ " violates foreign key constraint') "
					+ "where (select " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
							+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " = new." + COLUMN_AUTHOR_ID + ") is null;"
				+ " end;";
				
		// 2) update trigger on postIt table, checking new profile exists 
		private static final String AUTHOR_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_POST_IT + "_" + COLUMN_AUTHOR_ID + " "
				+ "before update on " + TABLE_POST_IT + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_POST_IT
									+ " violates foreign key constraint') "
					+ "where (select " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
							+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " = new." + COLUMN_AUTHOR_ID + ") is null;"
			    + " end;"; 
		
		// 3) delete trigger on profile table, cascade delete on postIt table
		private static final String AUTHOR_DELETE_TRIGGER_CREATE  = "create trigger fkd_"
				+ TABLE_POST_IT + "_" + COLUMN_AUTHOR_ID + " "
				+ "before delete on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin "
					+ "delete from " + TABLE_POST_IT 
						+ " where " + COLUMN_AUTHOR_ID
						+ " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + "; "
				+ " end;";
				
		// 4) update trigger on profile table, cascade update on postIt table
		private static final String AUTHOR_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_POST_IT + "_" + COLUMN_AUTHOR_ID + " "
				+ "after update on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin " 
					+ "update " + TABLE_POST_IT + " set " + COLUMN_AUTHOR_ID 
					+ " = new." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
					+ " where " + COLUMN_AUTHOR_ID + " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
					+ "; "
				+ "end;";
				
		// 5) insert trigger on postIt table, checking word exists 
		private static final String WORD_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_POST_IT + "_" + COLUMN_WORD_ID + " "
				+ "before insert on " + TABLE_POST_IT + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_POST_IT 
								+ " violates foreign key constraint') "
					+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID
							+ " from " + WordProvider.WordTable.TABLE_WORD
							+ " where " + WordProvider.WordTable.COLUMN_WORD_ID
							+ " = new." + COLUMN_WORD_ID + ") is null;"
				+ " end;";
		
		// 6) update trigger on postIt table, checking new word exists
		private static final String WORD_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_POST_IT + "_" + COLUMN_WORD_ID + " "
				+ "before update on " + TABLE_POST_IT + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_POST_IT
								+ " violates foreign key constraint') "
					+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID
							+ " from " + WordProvider.WordTable.TABLE_WORD
							+ " where " + WordProvider.WordTable.COLUMN_WORD_ID
							+ " = new." + COLUMN_WORD_ID + ") is null;"
				+ " end;";
		
		// 7) delete trigger on word table, cascade delete on postIt table
		private static final String WORD_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_POST_IT + "_" + COLUMN_WORD_ID + " "
				+ "before delete on " + WordProvider.WordTable.TABLE_WORD + " "
				+ "for each row begin "
					+ "delete from " + TABLE_POST_IT 
						+ " where " + COLUMN_WORD_ID 
						+ " = old." + WordProvider.WordTable.COLUMN_WORD_ID + "; "
				+ " end;";
				
		// 8) update trigger on word table, cascade delete on postIt table
		private static final String WORD_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_POST_IT + "_" + COLUMN_WORD_ID + " "
				+ "after update on " + WordProvider.WordTable.TABLE_WORD + " "
				+ "for each row begin "
					+ "update " + TABLE_POST_IT + " set " + COLUMN_WORD_ID
					+ " = new." + WordProvider.WordTable.COLUMN_WORD_ID
					+ " where " + COLUMN_WORD_ID + " = old." + WordProvider.WordTable.COLUMN_WORD_ID
					+ "; "
				+ "end;";
		
		// called when no database exists in disk and the SQLiteOpenHelper 
		// class needs to create a new one.
		public static void onCreate(SQLiteDatabase database)
		{
			//PostIt table creation in database 
			database.execSQL(TABLE_CREATE);
			
			/*  DEPRACATED: post_its can be loaded without word/author profile stored locally!
			    database.execSQL(AUTHOR_INSERT_TRIGGER_CREATE);
				database.execSQL(WORD_INSERT_TRIGGER_CREATE);
				database.execSQL(AUTHOR_UPDATE_TRIGGER_CREATE);
				database.execSQL(WORD_UPDATE_TRIGGER_CREATE);
				database.execSQL(AUTHOR_DELETE_TRIGGER_CREATE);
				database.execSQL(WORD_DELETE_TRIGGER_CREATE);
				database.execSQL(AUTHOR_PARENT_UPDATE_TRIGGER_CREATE);
				database.execSQL(WORD_PARENT_UPDATE_TRIGGER_CREATE); 
			*/
			
		}
		
		// called when there is a database version mismatch meaning that the version 
		// of the database on disk needs to be upgraded to the current version.
		public static void onUpgrade(SQLiteDatabase database, int oldVersion,
										int newVersion) 
		{
			// Log the version upgrade 
			Log.w(PostItTable.class.getName(),
					"Upgrading database PostIt table from version " + oldVersion 
					+ " to " + newVersion + ", which will destroy all old data");
			// Upgrade the existing database to conform to the new version.
			// Multiple previous versions can be handled by comparing oldVersion
			// and newVersion values.
			
			// Upgrade database by adding new version of PostIt table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_POST_IT);
			/*  DEPRACATED: post_its can be loaded without word/author profile stored locally!
				database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_POST_IT + "_" + COLUMN_AUTHOR_ID );
				database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_POST_IT + "_" + COLUMN_WORD_ID);
				database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_POST_IT + "_" + COLUMN_AUTHOR_ID); 
				database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_POST_IT + "_" + COLUMN_WORD_ID); 
				database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_POST_IT + "_" + COLUMN_AUTHOR_ID); 
				database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_POST_IT + "_" + COLUMN_WORD_ID); 
				database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_POST_IT + "_" + COLUMN_AUTHOR_ID); 
				database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_POST_IT + "_" + COLUMN_WORD_ID); 
			*/
			onCreate(database); 
		}
	}

}
