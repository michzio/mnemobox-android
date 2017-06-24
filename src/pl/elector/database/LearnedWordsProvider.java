/**
 * @date 06.10.2014
 */
package pl.elector.database;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * @author Michał Ziobro
 *
 */
public class LearnedWordsProvider extends ContentProvider {
	
	// defining ContentProvider's URI address
	private static final String AUTHORITY = "pl.elector.provider.LearnedWordsProvider"; 
	private static final String BASE_PATH = "learned_words"; 
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	
	// defining a UriMatcher to differentiate between different URI requests:
	// 1) for all elements 
	// 2) subset of rows for given profile id 
	// 3) subset of rows for given profile id and wordset_id 
	// 4) single row for given profile and word id 
	// 5) single row for specific learned id 
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2; 
	private static final int ROWS_FOR_PROFILE = 3; 
	private static final int ROWS_FOR_PROFILE_AND_WORDSET = 4; 
	private static final int ROW_FOR_PROFILE_AND_WORD = 5; 
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending 
	// in 'learned_words' represents a request for all learned items
	// and 'learned_words/[rowId]' represents a single row,
	// and 'learned_words/profile/[profileId]/word/[wordId]' represents a single row for given profile and word id
	// and 'learned_words/profile/[profileId]' represents request for subset of rows for given profile id,
	// and 'learned_words/profile/[profileId]/wordset/[wordsetId]' represents request for subset of rowsfor given profile id and wordset id
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#/word/#", ROW_FOR_PROFILE_AND_WORD);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#", ROWS_FOR_PROFILE);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#/wordset/#", ROWS_FOR_PROFILE_AND_WORDSET);
	}
	
	// reference to SQLiteOpenHelper class instance
	// used to construct the underlying database
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows (including rows for given profile and wordset)
	// and a single row. 
	private static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.learned_words"; 
	private static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.learned_words"; 
	
	// projection map for learned words join query 
	private static final Map<String, String> learnedWordsColumnMap; 
	
	static {
		learnedWordsColumnMap = new HashMap<String, String>(); 
		// "wordsetWordsTable.wordsetId" => "wordsetWordsTable.wordsetId AS wordsetId"
		learnedWordsColumnMap.put(WordsetWordsProvider.WordsetWordsTable.addPrefix(WordsetWordsProvider.WordsetWordsTable.COLUMN_WORDSET_ID),
											 WordsetWordsProvider.WordsetWordsTable.addPrefix(WordsetWordsProvider.WordsetWordsTable.COLUMN_WORDSET_ID) + " AS " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORDSET_ID );
		// "wordsetWordsTable.wordId" => "wordsetWordsTable.wordId AS wordId" !!!
		learnedWordsColumnMap.put(WordsetWordsProvider.WordsetWordsTable.addPrefix(WordsetWordsProvider.WordsetWordsTable.COLUMN_WORD_ID),
				 WordsetWordsProvider.WordsetWordsTable.addPrefix(WordsetWordsProvider.WordsetWordsTable.COLUMN_WORD_ID) + " AS " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORD_ID);
		// "learnedTable._id" => "learnedTable._id AS _id"
		learnedWordsColumnMap.put(LearnedTable.addPrefix(LearnedTable.COLUMN_LEARNED_ID), 
				 LearnedTable.addPrefix(LearnedTable.COLUMN_LEARNED_ID) + " AS " + LearnedTable.COLUMN_LEARNED_ID);
		// "learnedTable.profileId" => "learnedTable.profileId AS profileId"
		learnedWordsColumnMap.put(LearnedTable.addPrefix(LearnedTable.COLUMN_PROFILE_ID),
				 LearnedTable.addPrefix(LearnedTable.COLUMN_PROFILE_ID) + " AS " + LearnedTable.COLUMN_PROFILE_ID);
		// "learnedTable.wordId" => "learnedTable.wordId AS learnedWordId" !!! BE CAREFUL
		learnedWordsColumnMap.put(LearnedTable.addPrefix(LearnedTable.COLUMN_WORD_ID),
				 LearnedTable.addPrefix(LearnedTable.COLUMN_WORD_ID) + " AS learnedWordId");
	}
	
	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single learned word item for given learned id 
	 * or for given profile id and word id, or subset of learned word items
	 * for given profile id or for given profile id and wordset id 
	 * or rows for provided selection argument depending on the URI address.
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is a single row URI limit the deletion to specified row: 
		// 1) given learned id or 2) given profile id and word id 
		// else if this is a rows for profile URI then limit the deletion to specified profile id 
		// else if this is a rows for profile and wordset URI then limit the deletion to specified profile id 
		// and word Ids IN the result of SELECT query for word Ids for given wordset Id on wordset_words table 
		// else this is all rows deletion then use passed in selection and selectionArgs parameters. 
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: {
				String learnedID = uri.getPathSegments().get(1); 
				selection = LearnedTable.COLUMN_LEARNED_ID + "=" + learnedID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROW_FOR_PROFILE_AND_WORD: {
				String profileID = uri.getPathSegments().get(2); 
				String wordID = uri.getPathSegments().get(4); 
				selection = LearnedTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND " + LearnedTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE: {
				String profileID = uri.getPathSegments().get(2); 
				selection = LearnedTable.COLUMN_PROFILE_ID + "=" + profileID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE_AND_WORDSET: {
				String profileID = uri.getPathSegments().get(2); 
				String wordsetID = uri.getPathSegments().get(4); 
				selection = LearnedTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND " + LearnedTable.COLUMN_WORD_ID + " IN ("
									+ " SELECT " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORD_ID
									+ " FROM " + WordsetWordsProvider.WordsetWordsTable.TABLE_WORDSET_WORDS
									+ " WHERE " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORDSET_ID
									+ " = " + wordsetID + " )"
						+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "");
				break; 
			}
			default: break; 		
		}
		
		// To return the number of deleted item, you must specify 
		// a where clause. To delete all rows and return a value, pass in "1". 
		if(selection == null)
			selection = "1"; 
		
		// Execute the deletion. 
		int deleteCount = db.delete(LearnedTable.TABLE_LEARNED, selection, selectionArgs);
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return correct MIME type depending on the query type: 
	 * single row, all rows, subset of rows for given profile or profile and wordset.
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
			case ROWS_FOR_PROFILE_AND_WORDSET: 
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
		switch( uriMatcher.match(uri) ) 
		{
			case ALLROWS: 
				// insert the values into the table
				id = db.insert(LearnedTable.TABLE_LEARNED, nullColumnHack, values); 
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
	
	@Override
	public synchronized int bulkInsert(Uri uri, ContentValues[] values) { 
		
		int count = 0; 
		
		switch(uriMatcher.match(uri)) { 
			case ALLROWS: 
				count = doBulkInsertOptimised(uri, values); 
				break;
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		return count; 
	}
	
	private int doBulkInsertOptimised(Uri uri, ContentValues[] values) { 
		
		final String INSERT_QUERY = "INSERT INTO " + LearnedTable.TABLE_LEARNED + " ("
				+ LearnedTable.COLUMN_PROFILE_ID + ", " + LearnedTable.COLUMN_WORD_ID
				+ ") VALUES (?,?)"; 
		final SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		final SQLiteStatement stmt = db.compileStatement(INSERT_QUERY); 
		
		int count = 0; 
		
		db.beginTransaction(); 
		try { 
			for(int i=0; i < values.length; i++) { 
				stmt.clearBindings(); 
				stmt.bindString(1, values[i].getAsString(LearnedTable.COLUMN_PROFILE_ID));
				stmt.bindString(2, values[i].getAsString(LearnedTable.COLUMN_WORD_ID)); 
				long id = stmt.executeInsert(); 
				if(id > -1) count++; 
			}
			db.setTransactionSuccessful(); 
		} finally { 
			db.endTransaction(); 
		}
		
		getContext().getContentResolver().notifyChange(uri, null); 
		return count; 
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
	 * This method enables you to perform queries on the underlying data source 
	 * (SQLite database) using ContentProvider. UriMatcher object is used to differentiate 
	 * queries for single row, row for given profile id and word id, for subset of rows 
	 * for given profile id or profile id and wordset id (join query) 
	 * and queries for all rows. SQLite Query Builder is used as a helper object for 
	 * performing row-based, profile-based, word-based and wordset-based queries. 
	 * @projection - ACCEPTS ONLY FULLY QUALIFIED COLUMN NAMES! ex. table_name.column_name
	 * 				for ROWS_FOR_PROFILE_AND_WORDSET content URI address queries. 
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
				queryBuilder.setTables(LearnedTable.TABLE_LEARNED);
				// getting learnedID for current query and setting where clause 
				String learnedID = uri.getPathSegments().get(1); 
				queryBuilder.appendWhere(LearnedTable.COLUMN_LEARNED_ID + "=" + learnedID);
				break; 
			}
			case ROW_FOR_PROFILE_AND_WORD: 
			{
				queryBuilder.setTables(LearnedTable.TABLE_LEARNED);
				// getting profileID and wordID for current query and setting where clause
				String profileID = uri.getPathSegments().get(2); 
				String wordID = uri.getPathSegments().get(4); 
				queryBuilder.appendWhere(LearnedTable.COLUMN_PROFILE_ID + "=" + profileID
									+ " AND " + LearnedTable.COLUMN_WORD_ID + "=" + wordID);
				break; 
			}
			case ROWS_FOR_PROFILE: 
			{
				queryBuilder.setTables(LearnedTable.TABLE_LEARNED);
				// getting profileID for current query and setting where clause 
				String profileID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(LearnedTable.COLUMN_PROFILE_ID + "=" + profileID);
				break; 
			}
			case ROWS_FOR_PROFILE_AND_WORDSET: 
			{
				// creating inner joined learned and wordset_words tables
				StringBuilder sb = new StringBuilder(); 
				sb.append(LearnedTable.TABLE_LEARNED);
				sb.append(" INNER JOIN "); 
				sb.append(WordsetWordsProvider.WordsetWordsTable.TABLE_WORDSET_WORDS);
				sb.append(" ON ("); 
				// appending column name prefixed with table name
				sb.append(LearnedTable.addPrefix(LearnedTable.COLUMN_WORD_ID));
				sb.append(" = "); 
				// appending column name prefixed with table name
				sb.append(WordsetWordsProvider.WordsetWordsTable.addPrefix(WordsetWordsProvider.WordsetWordsTable.COLUMN_WORD_ID));
				sb.append(")");
				
				String joinTables = sb.toString(); 
				// setting joined tables and projection map 
				queryBuilder.setTables(joinTables);
				queryBuilder.setProjectionMap(learnedWordsColumnMap);
				
				// getting profileID and wordsetID for current query and setting where clause
				String profileID = uri.getPathSegments().get(2); 
				String wordsetID = uri.getPathSegments().get(4); 
				final String COLUMN_PROFILE_ID = LearnedTable.addPrefix(LearnedTable.COLUMN_PROFILE_ID);
				final String COLUMN_WORDSET_ID = WordsetWordsProvider.WordsetWordsTable.addPrefix(WordsetWordsProvider.WordsetWordsTable.COLUMN_WORDSET_ID);
				queryBuilder.appendWhere(COLUMN_PROFILE_ID + "=" + profileID
								+ " AND " + COLUMN_WORDSET_ID + "=" + wordsetID);
				break; 
			}
			case ALLROWS: 
			{
				queryBuilder.setTables(LearnedTable.TABLE_LEARNED);
				break; 
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
	public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		
		// If this is an update of single row, modify selection argument
		// to indicate that row, else if this is update of a set of rows 
		// for given profile or for given profile and wordset also 
		// modify selection argument appropriately to indicate that rows
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: {
				String learnedID = uri.getPathSegments().get(1); 
				selection = LearnedTable.COLUMN_LEARNED_ID + "=" + learnedID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROW_FOR_PROFILE_AND_WORD: {
				String profileID = uri.getPathSegments().get(2); 
				String wordID = uri.getPathSegments().get(4); 
				selection = LearnedTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND " + LearnedTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE: {
				String profileID = uri.getPathSegments().get(2); 
				selection = LearnedTable.COLUMN_PROFILE_ID + "=" + profileID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE_AND_WORDSET: {
				String profileID = uri.getPathSegments().get(2); 
				String wordsetID = uri.getPathSegments().get(4); 
				selection = LearnedTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND " + LearnedTable.COLUMN_WORD_ID + " IN ("
									+ " SELECT " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORD_ID
									+ " FROM " + WordsetWordsProvider.WordsetWordsTable.TABLE_WORDSET_WORDS
									+ " WHERE " + WordsetWordsProvider.WordsetWordsTable.COLUMN_WORDSET_ID
									+ " = " + wordsetID + " )"
						+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ")" : "");
				break; 
			}
			default: break; 		
		}
		
		// Perform the update.
		int updateCount = db.update(LearnedTable.TABLE_LEARNED, 
									values, selection, selectionArgs);
		
		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null);
		
		return updateCount;
	}
	
	public static class LearnedTable {
		
		// Database Table 
		public static final String TABLE_LEARNED = "learnedTable"; 
		public static final String COLUMN_LEARNED_ID = "_id"; // primary key
		public static final String COLUMN_PROFILE_ID = "profileId"; // foreign key
		public static final String COLUMN_WORD_ID = "wordId"; // foreign key
		
		// Database Table creation SQL Statement 
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_LEARNED 
				+ " ("
				+ COLUMN_LEARNED_ID + " integer primary key autoincrement, "
				+ COLUMN_PROFILE_ID + " integer not null default 0, " // 0 - Anonymous user 
				+ COLUMN_WORD_ID + " integer not null, "
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
		// 1) insert trigger on learned table, checks if corresponding profile exists
		private static final String PROFILE_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_LEARNED + "_" + COLUMN_PROFILE_ID + " "
				+ "before insert on " + TABLE_LEARNED + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_LEARNED
									+ " violates foreign key constraint') "
					+ "where new." + COLUMN_PROFILE_ID + "!= 0 AND (select " 
							+ ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
							+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " = new." + COLUMN_PROFILE_ID + ") is null;"
				+ " end;";
		
		// 2) insert trigger on learned table, checks if corresponding word exists
		private static final String WORD_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_LEARNED + "_" + COLUMN_WORD_ID + " "
				+ "before insert on " + TABLE_LEARNED + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_LEARNED
									+ " violates foreign key constraint') "
					+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID 
							+ " from " + WordProvider.WordTable.TABLE_WORD
							+ " where " + WordProvider.WordTable.COLUMN_WORD_ID
							+ " = new." + COLUMN_WORD_ID + ") is null;"
				+ " end;";
		
		// 3) update trigger on learned table, checks if new profile exists
		private static final String PROFILE_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_LEARNED + "_" + COLUMN_PROFILE_ID + " "
				+ "before update on " + TABLE_LEARNED + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_LEARNED
									+ " violates foreign key constraint') "
					+ "where new." + COLUMN_PROFILE_ID + "!= 0 AND (select " 
							+ ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
							+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " = new." + COLUMN_PROFILE_ID + ") is null;"
				+ " end;";
		
		// 4) update trigger on learned table, checks if new word exists
		private static final String WORD_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_LEARNED + "_" + COLUMN_WORD_ID + " "
				+ "before update on " + TABLE_LEARNED + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_LEARNED
									+ " violates foreign key constraint') "
					+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID
							+ " from " + WordProvider.WordTable.TABLE_WORD
							+ " where " + WordProvider.WordTable.COLUMN_WORD_ID
							+ " = new." + COLUMN_WORD_ID  + ") is null; "
				+ " end;";
		
		// 5) delete trigger on profile table, cascade deletes corresponding learned words
		private static final String PROFILE_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_LEARNED + "_" + COLUMN_PROFILE_ID + " "
				+ "before delete on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin "
					+ "delete from " + TABLE_LEARNED
						+ " where " + COLUMN_PROFILE_ID
						+ " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + ";"
			    + " end;"; 
		
		// 6) delete trigger on word table, cascade deletes corresponding learned words
		private static final String WORD_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_LEARNED + "_" + COLUMN_WORD_ID + " "
				+ "before delete on " + WordProvider.WordTable.TABLE_WORD + " "
				+ "for each row begin "
					+ "delete from " + TABLE_LEARNED
						+ " where " + COLUMN_WORD_ID 
						+ " = old." + WordProvider.WordTable.COLUMN_WORD_ID + ";"
				+ " end;";
		
		// 7) update trigger on profile table, cascade updates corresponding learned words
		private static final String PROFILE_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_LEARNED + "_" + COLUMN_PROFILE_ID + " "
				+ "after update on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin "
					+ "update " + TABLE_LEARNED + " set " + COLUMN_PROFILE_ID 
					+ " = new." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
					+ " where " + COLUMN_PROFILE_ID + " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
					+ "; "
				+ "end;";
		
		// 8) update trigger on word table, cascade updates corresponding learned words
		private static final String WORD_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_LEARNED + "_" + COLUMN_WORD_ID + " "
				+ "after update on " + WordProvider.WordTable.TABLE_WORD + " "
				+ "for each row begin "
					+ "update " + TABLE_LEARNED + " set " + COLUMN_WORD_ID 
					+ " = new." + WordProvider.WordTable.COLUMN_WORD_ID
					+ " where " + COLUMN_WORD_ID + " = old." + WordProvider.WordTable.COLUMN_WORD_ID
					+ "; "
				+ "end;";
		
		// called when no database exists in disk and the SQLiteOpenHelper 
	    // class needs to create a new one.
		public static void onCreate(SQLiteDatabase database)
		{
			// Learned table creation in database (with additional triggers)
			database.execSQL(TABLE_CREATE);
			database.execSQL(PROFILE_INSERT_TRIGGER_CREATE);
			database.execSQL(WORD_INSERT_TRIGGER_CREATE); 
			database.execSQL(PROFILE_UPDATE_TRIGGER_CREATE);
			database.execSQL(WORD_UPDATE_TRIGGER_CREATE); 
			database.execSQL(PROFILE_DELETE_TRIGGER_CREATE);
			database.execSQL(WORD_DELETE_TRIGGER_CREATE); 
			database.execSQL(PROFILE_PARENT_UPDATE_TRIGGER_CREATE);
			database.execSQL(WORD_PARENT_UPDATE_TRIGGER_CREATE); 
		}
		
		// called when there is a database version mismatch meaning that the version
		// of the database on disk needs to be upgraded to the current version. 
		public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
		{
			// Log the version upgrade 
			Log.w(LearnedTable.class.getName(), 
					"Upgrading database Learned table from version " + oldVersion 
					+ " to " + newVersion + ", which will destroy all old data.");
			
			// Upgrading the existing database to conform to the new version.
			// Multiple previous versions can be handled by comparing oldVersion 
			// and newVersion values. 
			
			// Upgrade database by adding new version of Learned table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_LEARNED); 
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_LEARNED + "_" + COLUMN_PROFILE_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_LEARNED + "_" + COLUMN_WORD_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_LEARNED + "_" + COLUMN_PROFILE_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_LEARNED + "_" + COLUMN_WORD_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_LEARNED + "_"+ COLUMN_PROFILE_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_LEARNED + "_" + COLUMN_WORD_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_LEARNED + "_" + COLUMN_PROFILE_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_LEARNED + "_" + COLUMN_WORD_ID);
			onCreate(database); 
		}
		
		public static String addPrefix(String columnName)
		{
			return TABLE_LEARNED + "." + columnName; 
		}
	}
	
	

}
